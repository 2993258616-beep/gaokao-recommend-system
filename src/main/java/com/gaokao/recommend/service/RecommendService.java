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
    private static final int QUERY_LIMIT = 9;

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
        result.put("rush", takeUniqueRows(rushCandidates, used));
        result.put("stable", takeUniqueRows(stableCandidates, used));
        result.put("safe", takeUniqueRows(safeCandidates, used));
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
            return new int[]{2, 2, 2};
        }
        if (totalLimit == 5) {
            return new int[]{1, 2, 2};
        }
        return new int[]{1, 2, 1};
    }

    private int totalHenanLimit(Integer score, String subjectType) {
        if (score == null) {
            return 4;
        }
        if (score <= 260) {
            return 6;
        }
        if (score <= 320) {
            return 5;
        }
        return 4;
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
        return score < undergraduateLine(subjectType) + 80;
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

    private String rowKey(PredictionLine row) {
        return String.valueOf(row.getSchoolName()) + "|" + String.valueOf(row.getMajorGroup());
    }

    private boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }
}
