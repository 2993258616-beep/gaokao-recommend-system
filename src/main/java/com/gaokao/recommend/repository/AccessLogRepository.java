package com.gaokao.recommend.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public class AccessLogRepository {
    private final JdbcTemplate jdbcTemplate;

    public AccessLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void record(String username, String path, String remoteIp, String userAgent) {
        jdbcTemplate.update(
                "INSERT INTO access_log(username, path, remote_ip, user_agent) VALUES(?,?,?,?)",
                username, limit(path, 120), limit(remoteIp, 60), limit(userAgent, 255));
    }

    public long countToday(String pathPrefix) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM access_log WHERE create_time>=? AND create_time<? AND path LIKE ?",
                new Object[]{Timestamp.valueOf(start), Timestamp.valueOf(end), pathPrefix + "%"},
                Long.class);
        return count == null ? 0 : count;
    }

    public long countAll(String pathPrefix) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM access_log WHERE path LIKE ?",
                new Object[]{pathPrefix + "%"},
                Long.class);
        return count == null ? 0 : count;
    }

    public List<Map<String, Object>> recent(int limit) {
        return jdbcTemplate.queryForList(
                "SELECT username, path, remote_ip, create_time FROM access_log ORDER BY id DESC LIMIT ?",
                limit);
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
