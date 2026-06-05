package com.gaokao.recommend.repository;

import com.gaokao.recommend.entity.PredictionLine;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Repository
public class PredictionRepository {
    private static final List<String> HIGH_QUALITY_JUNIOR_COLLEGE_KEYWORDS = Arrays.asList(
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
    );

    private final JdbcTemplate jdbcTemplate;

    public PredictionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> findSchoolProvinces() {
        return jdbcTemplate.queryForList("SELECT DISTINCT school_province FROM prediction_line ORDER BY school_province", String.class);
    }

    public List<PredictionLine> recommend(Integer score, String subjectType, String schoolProvince, String majorName,
                                          String bucket, boolean allowUndergraduate, boolean allowJuniorCollege,
                                          boolean preferQualityJuniorCollege, int limit) {
        StringBuilder sql = new StringBuilder();
        List<Object> args = new ArrayList<Object>();

        sql.append("SELECT * FROM prediction_line WHERE province = ? ");
        args.add("河南");

        if (StringUtils.hasText(subjectType)) {
            sql.append(" AND subject_type = ? ");
            args.add(subjectType.trim());
        }

        if (StringUtils.hasText(schoolProvince) && !"全部地区".equals(schoolProvince)) {
            sql.append(" AND school_province = ? ");
            args.add(schoolProvince.trim());
        }

        if (StringUtils.hasText(majorName) && !"全部专业".equals(majorName) && !"全部方向".equals(majorName)) {
            sql.append(" AND (major_direction LIKE ? OR major_category LIKE ?) ");
            args.add("%" + majorName.trim() + "%");
            args.add("%" + majorName.trim() + "%");
        }

        if (!allowUndergraduate) {
            sql.append(" AND school_level <> '本科' ");
        }
        if (!allowJuniorCollege) {
            sql.append(" AND school_level <> '专科' ");
        }
        sql.append(" AND school_province <> '未识别' ");
        sql.append(" AND major_direction IS NOT NULL AND TRIM(major_direction) <> '' ");
        sql.append(" AND major_category IS NOT NULL AND TRIM(major_category) <> '' ");
        sql.append(" AND major_direction NOT LIKE '%未提供%' ");
        sql.append(" AND major_category NOT IN ('理','术','技','管理','商务','包含','未提供','专业','验技术','方向') ");
        sql.append(" AND school_name NOT LIKE '%未识别%' ");
        sql.append(" AND predict_score BETWEEN 100 AND 750 ");
        sql.append(" AND COALESCE(filing_score, predict_score) BETWEEN 100 AND 750 ");

        int undergraduateLine = undergraduateLine(subjectType);
        if (!allowUndergraduate && allowJuniorCollege) {
            sql.append(" AND school_level = '专科' ");
            sql.append(" AND COALESCE(filing_score, predict_score) <= ? ");
            args.add(undergraduateLine + 25);
            sql.append(" AND predict_score <= ? ");
            args.add(undergraduateLine + 25);
        }
        if (allowUndergraduate && !allowJuniorCollege) {
            sql.append(" AND school_level = '本科' ");
            sql.append(" AND predict_score >= ? ");
            args.add(undergraduateLine);
        }
        if (preferQualityJuniorCollege) {
            addHighQualityJuniorCollegeFilter(sql, args);
        }

        addScoreBand(sql, args, score, bucket, allowUndergraduate, allowJuniorCollege, undergraduateLine);

        if (score == null) {
            sql.append(" ORDER BY predict_score DESC, COALESCE(filing_score, predict_score) DESC ");
        } else {
            sql.append(" ORDER BY ABS(predict_score - ?) ASC, ");
            args.add(score);
            if ("冲刺".equals(bucket)) {
                sql.append("predict_score ASC, ");
            } else {
                sql.append("predict_score DESC, ");
            }
            sql.append("CASE WHEN school_province = '河南' THEN 0 ELSE 1 END, COALESCE(filing_score, predict_score) DESC ");
        }
        sql.append(" LIMIT ").append(Math.max(1, Math.min(limit, 30)));

        return jdbcTemplate.query(sql.toString(), args.toArray(), (rs, rowNum) -> map(rs));
    }

    private void addHighQualityJuniorCollegeFilter(StringBuilder sql, List<Object> args) {
        sql.append(" AND (");
        for (int i = 0; i < HIGH_QUALITY_JUNIOR_COLLEGE_KEYWORDS.size(); i++) {
            if (i > 0) {
                sql.append(" OR ");
            }
            sql.append("school_name LIKE ? ");
            args.add("%" + HIGH_QUALITY_JUNIOR_COLLEGE_KEYWORDS.get(i) + "%");
        }
        sql.append(") ");
    }

    private void addScoreBand(StringBuilder sql, List<Object> args, Integer score, String bucket,
                              boolean allowUndergraduate, boolean allowJuniorCollege, int undergraduateLine) {
        if (score == null) {
            return;
        }

        int low;
        int high;
        boolean juniorOnly = !allowUndergraduate && allowJuniorCollege;
        boolean undergraduateOnly = allowUndergraduate && !allowJuniorCollege;
        if (juniorOnly) {
            int rushWidth = score <= 320 ? 22 : 18;
            int safeWidth = score <= 320 ? 34 : 28;
            if ("冲刺".equals(bucket)) {
                low = score + 5;
                high = score + rushWidth;
            } else if ("稳妥".equals(bucket)) {
                low = score - 9;
                high = score + 7;
            } else if ("保底".equals(bucket)) {
                low = score - safeWidth;
                high = score - 10;
            } else {
                low = score - safeWidth;
                high = score + rushWidth;
            }
            high = Math.min(high, undergraduateLine + 18);
        } else if (undergraduateOnly) {
            if ("冲刺".equals(bucket)) {
                low = score + 3;
                high = score + 18;
            } else if ("稳妥".equals(bucket)) {
                low = score - 7;
                high = score + 6;
            } else if ("保底".equals(bucket)) {
                low = score - 23;
                high = score - 8;
            } else {
                low = score - 23;
                high = score + 18;
            }
            low = Math.max(low, undergraduateLine);
        } else if ("冲刺".equals(bucket)) {
            low = score + 3;
            high = score + 18;
        } else if ("稳妥".equals(bucket)) {
            low = score - 8;
            high = score + 6;
        } else if ("保底".equals(bucket)) {
            low = score - 26;
            high = score - 8;
        } else {
            low = score - 26;
            high = score + 18;
        }

        sql.append(" AND predict_score BETWEEN ? AND ? ");
        args.add(Math.max(0, low));
        args.add(Math.min(750, high));

    }

    private int undergraduateLine(String subjectType) {
        return "物理".equals(subjectType) ? 427 : 471;
    }

    private PredictionLine map(java.sql.ResultSet rs) throws java.sql.SQLException {
        PredictionLine p = new PredictionLine();
        p.setId(rs.getLong("id"));
        p.setPredictYear(rs.getInt("predict_year"));
        p.setProvince(rs.getString("province"));
        p.setSchoolCode(rs.getString("school_code"));
        p.setSchoolName(rs.getString("school_name"));
        p.setSubjectType(rs.getString("subject_type"));
        p.setMajorGroup(rs.getString("major_group"));
        p.setMajorGroupFull(rs.getString("major_group_full"));
        p.setSchoolType(rs.getString("school_type"));
        p.setSchoolLevel(rs.getString("school_level"));
        p.setSchoolNature(rs.getString("school_nature"));
        p.setPlanCount((Integer) rs.getObject("plan_count"));
        p.setFilingScore((Integer) rs.getObject("filing_score"));
        p.setFilingRank((Integer) rs.getObject("filing_rank"));
        p.setRangeFloat((Integer) rs.getObject("range_float"));
        p.setPredictScore(rs.getInt("predict_score"));
        p.setPredictLow(rs.getInt("predict_low"));
        p.setPredictHigh(rs.getInt("predict_high"));
        p.setPredictRange(rs.getString("predict_range"));
        p.setMajorDirection(rs.getString("major_direction"));
        p.setMajorCategory(rs.getString("major_category"));
        p.setSchoolProvince(rs.getString("school_province"));
        p.setConfidence(rs.getString("confidence"));
        return p;
    }
}
