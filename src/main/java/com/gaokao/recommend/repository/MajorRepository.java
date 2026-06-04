package com.gaokao.recommend.repository;

import com.gaokao.recommend.entity.MajorNameStat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MajorRepository {
    private final JdbcTemplate jdbcTemplate;

    public MajorRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MajorNameStat> findAll() {
        String sql =
                "SELECT m.id, m.major_name, m.count_num, m.subject_types " +
                "FROM major_name_stat m " +
                "WHERE m.major_name NOT IN ('热门专业', '综合类', '其他专业类', '职业本科') " +
                "  AND m.major_name NOT LIKE '%未提供%' " +
                "  AND EXISTS ( " +
                "      SELECT 1 FROM prediction_line p " +
                "      WHERE p.major_category = m.major_name " +
                "         OR p.major_direction LIKE CONCAT('%', m.major_name, '%') " +
                "  ) " +
                "ORDER BY m.count_num DESC, m.major_name ASC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            MajorNameStat item = new MajorNameStat();
            item.setId(rs.getLong("id"));
            item.setMajorName(rs.getString("major_name"));
            item.setCountNum(rs.getInt("count_num"));
            item.setSubjectTypes(rs.getString("subject_types"));
            return item;
        });
    }
}
