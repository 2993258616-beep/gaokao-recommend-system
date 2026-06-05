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
        Map<String, List<PredictionLine>> result = new HashMap<String, List<PredictionLine>>();
        int[] henanLimits = henanLimitsByBucket(score, subjectType, schoolProvince);
        List<PredictionLine> rushCandidates = recommendBucket(score, subjectType, schoolProvince, majorName, "冲刺", henanLimits[0]);
        List<PredictionLine> stableCandidates = recommendBucket(score, subjectType, schoolProvince, majorName, "稳妥", henanLimits[1]);
        List<PredictionLine> safeCandidates = recommendBucket(score, subjectType, schoolProvince, majorName, "保底", henanLimits[2]);

        Map<String, PredictionLine> used = new LinkedHashMap<String, PredictionLine>();
        result.put("rush", polishRows(takeUniqueRows(rushCandidates, used), score, "冲刺"));
        result.put("stable", polishRows(takeUniqueRows(stableCandidates, used), score, "稳妥"));
        result.put("safe", polishRows(takeUniqueRows(safeCandidates, used), score, "保底"));
        return result;
    }

    private List<PredictionLine> recommendBucket(Integer score, String subjectType, String schoolProvince,
                                                 String majorName, String bucket, int preferredHenanCount) {
        boolean allowUndergraduate = allowUndergraduate(score, subjectType);
        boolean allowJuniorCollege = allowJuniorCollege(score, subjectType);
        boolean allRegions = !hasText(schoolProvince) || "全部地区".equals(schoolProvince);

        if (!allRegions) {
            return predictionRepository.recommend(score, subjectType, schoolProvince, majorName, bucket,
                    allowUndergraduate, allowJuniorCollege, QUERY_LIMIT);
        }

        List<PredictionLine> all = predictionRepository.recommend(score, subjectType, schoolProvince, majorName,
                bucket, allowUndergraduate, allowJuniorCollege, QUERY_LIMIT);
        List<PredictionLine> henan = predictionRepository.recommend(score, subjectType, "河南", majorName,
                bucket, allowUndergraduate, allowJuniorCollege, Math.max(preferredHenanCount + 2, 3));
        return mixHenanRows(all, henan, preferredHenanCount);
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
