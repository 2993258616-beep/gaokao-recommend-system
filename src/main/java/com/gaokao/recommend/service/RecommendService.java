package com.gaokao.recommend.service;

import com.gaokao.recommend.entity.MajorNameStat;
import com.gaokao.recommend.entity.PredictionLine;
import com.gaokao.recommend.repository.MajorRepository;
import com.gaokao.recommend.repository.PredictionRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RecommendService {
    private static final int HISTORY_UNDERGRADUATE_LINE_2025 = 471;
    private static final int PHYSICS_UNDERGRADUATE_LINE_2025 = 427;
    private static final int VISIBLE_LIMIT = 3;
    private static final int QUERY_LIMIT = 18;
    private static final int PLAN_COUNT = 3;
    private static final int NEAR_UNDERGRADUATE_MARGIN = 20;
    private static final List<String> HIGH_QUALITY_JUNIOR_COLLEGE_KEYWORDS = Collections.unmodifiableList(Arrays.asList(
            "黄河水利职业技术学院", "河南工业职业技术学院", "河南职业技术学院", "河南农业职业学院", "许昌职业技术学院",
            "郑州铁路职业技术学院", "河南经贸职业学院", "河南交通职业技术学院", "河南应用技术职业学院", "河南医学高等专科学校",
            "北京电子科技职业学院", "北京工业职业技术学院", "天津市职业大学", "天津医学高等专科学校", "天津电子信息职业技术学院",
            "石家庄铁路职业技术学院", "唐山工业职业技术学院", "山西工程职业学院", "辽宁省交通高等专科学校", "沈阳职业技术学院",
            "长春汽车工业高等专科学校", "吉林铁道职业技术学院", "黑龙江建筑职业技术学院", "哈尔滨职业技术学院",
            "上海工艺美术职业学院", "上海电子信息职业技术学院", "南京工业职业技术大学", "江苏农林职业技术学院", "常州信息职业技术学院",
            "无锡职业技术学院", "江苏经贸职业技术学院", "金华职业技术大学", "浙江金融职业学院", "杭州职业技术学院",
            "宁波职业技术学院", "温州职业技术学院", "芜湖职业技术学院", "安徽商贸职业技术学院", "福建船政交通职业学院",
            "九江职业技术学院", "江西应用技术职业学院", "山东商业职业技术学院", "淄博职业学院", "日照职业技术学院",
            "武汉职业技术学院", "武汉船舶职业技术学院", "武汉铁路职业技术学院", "长沙民政职业技术学院", "湖南铁道职业技术学院",
            "广东轻工职业技术学院", "深圳职业技术大学", "广州番禺职业技术学院", "重庆电子工程职业学院", "重庆工业职业技术学院",
            "成都航空职业技术学院", "四川交通职业技术学院", "贵州交通职业技术学院", "昆明冶金高等专科学校", "陕西工业职业技术学院",
            "杨凌职业技术学院", "西安航空职业技术学院", "兰州资源环境职业技术大学", "宁夏职业技术学院", "新疆农业职业技术学院"
    ));

    private static final List<String> ALL_SCHOOL_PROVINCES = Collections.unmodifiableList(Arrays.asList(
            "北京", "天津", "河北", "山西", "内蒙古",
            "辽宁", "吉林", "黑龙江", "上海", "江苏",
            "浙江", "安徽", "福建", "江西", "山东",
            "河南", "湖北", "湖南", "广东", "广西",
            "海南", "重庆", "四川", "贵州", "云南",
            "西藏", "陕西", "甘肃", "青海", "宁夏",
            "新疆"
    ));

    private final MajorRepository majorRepository;
    private final PredictionRepository predictionRepository;

    public RecommendService(MajorRepository majorRepository, PredictionRepository predictionRepository) {
        this.majorRepository = majorRepository;
        this.predictionRepository = predictionRepository;
    }

    public List<MajorNameStat> getMajors() {
        return majorRepository.findAll();
    }

    public List<String> getSchoolProvinces() {
        return ALL_SCHOOL_PROVINCES;
    }

    public Map<String, List<PredictionLine>> recommend(Integer score, String subjectType, String schoolProvince, String majorName) {
        return recommend(score, subjectType, schoolProvince, majorName, 0);
    }

    public Map<String, List<PredictionLine>> recommend(Integer score, String subjectType, String schoolProvince, String majorName, Integer nonce) {
        Map<String, List<PredictionLine>> result = new HashMap<String, List<PredictionLine>>();
        int[] henanLimits = henanLimitsByBucket(score, subjectType, schoolProvince);
        List<PredictionLine> rushCandidates = recommendBucket(score, subjectType, schoolProvince, majorName, "冲刺", henanLimits[0]);
        List<PredictionLine> stableCandidates = recommendBucket(score, subjectType, schoolProvince, majorName, "稳妥", henanLimits[1]);
        List<PredictionLine> safeCandidates = recommendBucket(score, subjectType, schoolProvince, majorName, "保底", henanLimits[2]);

        Map<String, PredictionLine> used = new LinkedHashMap<String, PredictionLine>();
        result.put("rush", polishRows(takeUniqueRows(varyCandidateOrder(rushCandidates, score, subjectType, schoolProvince, "冲刺", nonce), used), score, "冲刺"));
        result.put("stable", polishRows(takeUniqueRows(varyCandidateOrder(stableCandidates, score, subjectType, schoolProvince, "稳妥", nonce), used), score, "稳妥"));
        result.put("safe", polishRows(takeUniqueRows(varyCandidateOrder(safeCandidates, score, subjectType, schoolProvince, "保底", nonce), used), score, "保底"));
        return result;
    }

    private List<PredictionLine> recommendBucket(Integer score, String subjectType, String schoolProvince,
                                                 String majorName, String bucket, int preferredHenanCount) {
        boolean nearLineJuniorCollege = shouldUseQualityJuniorCollege(score, subjectType, bucket);
        boolean allowUndergraduate = allowUndergraduate(score, subjectType) && !nearLineJuniorCollege;
        boolean allowJuniorCollege = allowJuniorCollege(score, subjectType) || nearLineJuniorCollege;
        boolean allRegions = !hasText(schoolProvince) || "全部地区".equals(schoolProvince);

        if (!allRegions) {
            return queryRecommendations(score, subjectType, schoolProvince, majorName, bucket,
                    allowUndergraduate, allowJuniorCollege, nearLineJuniorCollege, QUERY_LIMIT);
        }

        List<PredictionLine> all = queryRecommendations(score, subjectType, schoolProvince, majorName,
                bucket, allowUndergraduate, allowJuniorCollege, nearLineJuniorCollege, QUERY_LIMIT);
        List<PredictionLine> henan = queryRecommendations(score, subjectType, "河南", majorName,
                bucket, allowUndergraduate, allowJuniorCollege, nearLineJuniorCollege, Math.max(preferredHenanCount + 2, 3));
        return mixHenanRows(all, henan, preferredHenanCount);
    }

    private List<PredictionLine> queryRecommendations(Integer score, String subjectType, String schoolProvince,
                                                       String majorName, String bucket, boolean allowUndergraduate,
                                                       boolean allowJuniorCollege, boolean preferQualityJuniorCollege,
                                                       int limit) {
        List<PredictionLine> first = predictionRepository.recommend(score, subjectType, schoolProvince, majorName,
                bucket, allowUndergraduate, allowJuniorCollege, preferQualityJuniorCollege, limit);
        if (!preferQualityJuniorCollege || first.size() >= VISIBLE_LIMIT) {
            return first;
        }

        LinkedHashMap<String, PredictionLine> merged = new LinkedHashMap<String, PredictionLine>();
        for (PredictionLine row : first) {
            merged.put(rowKey(row), row);
        }
        List<PredictionLine> fallback = predictionRepository.recommend(score, subjectType, schoolProvince, majorName,
                bucket, allowUndergraduate, allowJuniorCollege, false, limit);
        for (PredictionLine row : fallback) {
            merged.put(rowKey(row), row);
            if (merged.size() >= limit) {
                break;
            }
        }
        return new ArrayList<PredictionLine>(merged.values());
    }

    private List<PredictionLine> takeUniqueRows(List<PredictionLine> candidates, Map<String, PredictionLine> used) {
        List<PredictionLine> rows = new ArrayList<PredictionLine>();
        for (PredictionLine row : candidates) {
            String key = rowKey(row);
            if (used.containsKey(key)) {
                continue;
            }
            rows.add(row);
            used.put(key, row);
            if (rows.size() >= VISIBLE_LIMIT) {
                break;
            }
        }
        return rows;
    }

    private List<PredictionLine> varyCandidateOrder(List<PredictionLine> candidates, Integer score, String subjectType,
                                                    String schoolProvince, String bucket, Integer nonce) {
        int plan = nonce == null ? 0 : ((nonce % PLAN_COUNT) + PLAN_COUNT) % PLAN_COUNT;
        if (plan <= 0 || candidates.size() <= VISIBLE_LIMIT) {
            return candidates;
        }
        int windowSize = Math.min(candidates.size(), Math.max(VISIBLE_LIMIT + 3, 9));
        int offset = seededOffset(String.valueOf(score) + "|" + String.valueOf(subjectType) + "|"
                + String.valueOf(schoolProvince) + "|" + bucket + "|" + plan, windowSize);

        List<PredictionLine> varied = new ArrayList<PredictionLine>();
        for (int i = offset; i < windowSize; i++) {
            varied.add(candidates.get(i));
        }
        for (int i = 0; i < offset; i++) {
            varied.add(candidates.get(i));
        }
        for (int i = windowSize; i < candidates.size(); i++) {
            varied.add(candidates.get(i));
        }
        return varied;
    }

    private int seededOffset(String value, int size) {
        int hash = 0;
        for (int i = 0; i < value.length(); i++) {
            hash = 31 * hash + value.charAt(i);
        }
        return (hash & 0x7fffffff) % Math.max(1, size);
    }

    private int[] henanLimitsByBucket(Integer score, String subjectType, String schoolProvince) {
        if (hasText(schoolProvince) && !"全部地区".equals(schoolProvince)) {
            return new int[]{VISIBLE_LIMIT, VISIBLE_LIMIT, VISIBLE_LIMIT};
        }

        int totalLimit = totalHenanLimit(score, subjectType);
        if (totalLimit >= 6) {
            return new int[]{2, 1, 3};
        }
        if (totalLimit == 5) {
            return rotateHenanLimits(score, subjectType, new int[]{2, 1, 2}, new int[]{3, 1, 1});
        }
        return rotateHenanLimits(score, subjectType, new int[]{2, 1, 1}, new int[]{1, 1, 2});
    }

    private int totalHenanLimit(Integer score, String subjectType) {
        return 4;
    }

    private int[] rotateHenanLimits(Integer score, String subjectType, int[] first, int[] second) {
        int seed = score == null ? 0 : score;
        if ("物理".equals(subjectType)) {
            seed += 17;
        }
        return seed % 2 == 0 ? first : second;
    }

    private boolean allowUndergraduate(Integer score, String subjectType) {
        if (score == null) {
            return true;
        }
        return score >= undergraduateLine(subjectType);
    }

    private boolean allowJuniorCollege(Integer score, String subjectType) {
        if (score == null) {
            return true;
        }
        return score < undergraduateLine(subjectType);
    }

    private int undergraduateLine(String subjectType) {
        return "物理".equals(subjectType) ? PHYSICS_UNDERGRADUATE_LINE_2025 : HISTORY_UNDERGRADUATE_LINE_2025;
    }

    private boolean shouldUseQualityJuniorCollege(Integer score, String subjectType, String bucket) {
        if (score == null || "冲刺".equals(bucket)) {
            return false;
        }
        int line = undergraduateLine(subjectType);
        return score >= line && score <= line + NEAR_UNDERGRADUATE_MARGIN;
    }

    public static boolean isHighQualityJuniorCollege(PredictionLine row) {
        if (row == null || !"专科".equals(row.getSchoolLevel()) || row.getSchoolName() == null) {
            return false;
        }
        for (String keyword : HIGH_QUALITY_JUNIOR_COLLEGE_KEYWORDS) {
            if (row.getSchoolName().contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private List<PredictionLine> mixHenanRows(List<PredictionLine> all, List<PredictionLine> henan, int preferredHenanCount) {
        LinkedHashMap<String, PredictionLine> selected = new LinkedHashMap<String, PredictionLine>();
        int henanAdded = 0;
        for (PredictionLine row : henan) {
            if (henanAdded >= preferredHenanCount) {
                break;
            }
            selected.put(rowKey(row), row);
            henanAdded++;
        }
        for (PredictionLine row : all) {
            if (isHenan(row) && henanAdded >= preferredHenanCount && !selected.containsKey(rowKey(row))) {
                continue;
            }
            if (isHenan(row) && !selected.containsKey(rowKey(row))) {
                henanAdded++;
            }
            selected.put(rowKey(row), row);
            if (selected.size() >= QUERY_LIMIT) {
                break;
            }
        }
        return new ArrayList<PredictionLine>(selected.values());
    }

    private boolean isHenan(PredictionLine row) {
        return row != null && "河南".equals(row.getSchoolProvince());
    }

    private List<PredictionLine> polishRows(List<PredictionLine> rows, Integer score, String bucket) {
        for (PredictionLine row : rows) {
            polishPrediction(row, score, bucket);
        }
        return rows;
    }

    private void polishPrediction(PredictionLine row, Integer score, String bucket) {
        if (row == null || row.getPredictScore() == null) {
            return;
        }
        int base = row.getFilingScore() == null ? row.getPredictScore() : row.getFilingScore();
        int predicted = row.getPredictScore() + displayBoost(row);
        if (score != null) {
            predicted = clampToBucket(predicted, score, bucket);
        }
        int band = "本科".equals(row.getSchoolLevel()) ? 6 : 8;

        if ("本科".equals(row.getSchoolLevel())) {
            predicted = Math.max(predicted, undergraduateLine(row.getSubjectType()) + 2);
        }
        if ("专科".equals(row.getSchoolLevel())) {
            predicted = Math.min(predicted, undergraduateLine(row.getSubjectType()) + 18);
        }

        row.setPredictScore(predicted);
        row.setPredictLow(clamp(predicted - band, 0, 750));
        row.setPredictHigh(clamp(predicted + band, 0, 750));
        row.setPredictRange(row.getPredictLow() + "-" + row.getPredictHigh());
        row.setRangeFloat(predicted - base);
        row.setConfidence("按2025线预测2026");
    }

    private int displayBoost(PredictionLine row) {
        String text = String.valueOf(row.getSchoolName())
                + String.valueOf(row.getMajorGroupFull())
                + String.valueOf(row.getMajorDirection())
                + String.valueOf(row.getMajorCategory());
        int delta = "本科".equals(row.getSchoolLevel()) ? 2 : 1;
        if (containsAny(text, "临床医学", "口腔医学", "法学", "汉语言文学", "师范", "计算机", "软件", "人工智能", "大数据", "电子信息", "电气", "自动化", "护理")) {
            delta += 1;
        }
        if (isHenan(row)) {
            delta += 1;
        }
        if (containsAny(text, "中外合作", "合作办学")) {
            delta -= 1;
        }
        return clamp(delta, 0, 4);
    }

    private int clampToBucket(int predicted, int score, String bucket) {
        int low;
        int high;
        if ("冲刺".equals(bucket)) {
            low = score + 3;
            high = score + 18;
        } else if ("稳妥".equals(bucket)) {
            low = score - 7;
            high = score + 6;
        } else {
            low = score - 23;
            high = score - 8;
        }
        return clamp(predicted, Math.max(0, low), Math.min(750, high));
    }

    private boolean containsAny(String text, String... words) {
        if (text == null) {
            return false;
        }
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String rowKey(PredictionLine row) {
        return String.valueOf(row.getSchoolName()) + "|" + String.valueOf(row.getMajorGroup());
    }

    private boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }
}
