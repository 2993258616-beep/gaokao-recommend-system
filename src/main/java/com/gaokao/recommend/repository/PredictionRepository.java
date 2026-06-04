package com.gaokao.recommend.repository;

import com.gaokao.recommend.entity.PredictionLine;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Repository
public class PredictionRepository {
    private final JdbcTemplate jdbcTemplate;

    public PredictionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> findSchoolProvinces() {
        return jdbcTemplate.queryForList("SELECT DISTINCT school_province FROM prediction_line ORDER BY school_province", String.class);
    }

    public List<PredictionLine> recommend(Integer score, String subjectType, String schoolProvince, String majorName,
                                          String bucket, boolean allowUndergraduate, boolean allowJuniorCollege,
                                          int limit) {
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
        sql.append(" AND major_category NOT IN ('理','术','技','管理','商务','包含','未提供','专业','验技术') ");

        addScoreBand(sql, args, score, bucket);

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

    private void addScoreBand(StringBuilder sql, List<Object> args, Integer score, String bucket) {
        if (score == null) {
            return;
        }

        int low;
        int high;
        if ("冲刺".equals(bucket)) {
            low = score + 1;
            high = score + 16;
        } else if ("稳妥".equals(bucket)) {
            low = score - 10;
            high = score + 5;
        } else if ("保底".equals(bucket)) {
            low = score - 26;
            high = score - 8;
        } else {
            low = score - 26;
            high = score + 16;
        }

        sql.append(" AND predict_score BETWEEN ? AND ? ");
        args.add(Math.max(0, low));
        args.add(Math.min(750, high));

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
