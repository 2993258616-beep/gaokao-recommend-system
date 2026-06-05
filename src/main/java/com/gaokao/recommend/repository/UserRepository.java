package com.gaokao.recommend.repository;

import com.gaokao.recommend.entity.UserAccount;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UserAccount> findByUsername(String username) {
        String sql = "SELECT id, username, password, role, enabled FROM user_account WHERE LOWER(username) = LOWER(?) LIMIT 1";
        return jdbcTemplate.query(sql, new Object[]{username}, rs -> {
            if (!rs.next()) return Optional.empty();
            UserAccount u = new UserAccount();
            u.setId(rs.getLong("id"));
            u.setUsername(rs.getString("username"));
            u.setPassword(rs.getString("password"));
            u.setRole(rs.getString("role"));
            u.setEnabled(rs.getInt("enabled") == 1);
            return Optional.of(u);
        });
    }

    public List<UserAccount> findAll() {
        String sql = "SELECT id, username, password, role, enabled FROM user_account ORDER BY id";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            UserAccount u = new UserAccount();
            u.setId(rs.getLong("id"));
            u.setUsername(rs.getString("username"));
            u.setPassword(rs.getString("password"));
            u.setRole(rs.getString("role"));
            u.setEnabled(rs.getInt("enabled") == 1);
            return u;
        });
    }

    public long countAll() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM user_account", Long.class);
        return count == null ? 0 : count;
    }

    public long countEnabled() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM user_account WHERE enabled=1", Long.class);
        return count == null ? 0 : count;
    }

    public boolean existsByUsername(String username) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM user_account WHERE LOWER(username) = LOWER(?)",
                new Object[]{username},
                Integer.class);
        return count != null && count > 0;
    }

    public void createUser(String username, String encodedPassword) {
        createUser(username, encodedPassword, "ROLE_USER");
    }

    public void createUser(String username, String encodedPassword, String role) {
        jdbcTemplate.update(
                "INSERT INTO user_account(username, password, role, enabled) VALUES(?,?,?,1)",
                username,
                encodedPassword,
                role);
    }

    public void updatePassword(Long id, String encodedPassword) {
        jdbcTemplate.update("UPDATE user_account SET password=? WHERE id=?", encodedPassword, id);
    }

    public void setEnabled(Long id, boolean enabled) {
        jdbcTemplate.update("UPDATE user_account SET enabled=? WHERE id=?", enabled ? 1 : 0, id);
    }
}
