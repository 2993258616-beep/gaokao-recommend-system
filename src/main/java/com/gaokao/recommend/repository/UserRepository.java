package com.gaokao.recommend.repository;

import com.gaokao.recommend.entity.UserAccount;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UserAccount> findByUsername(String username) {
        String sql = "SELECT id, username, password, role, enabled FROM user_account WHERE username = ? LIMIT 1";
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

    public boolean existsByUsername(String username) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM user_account WHERE LOWER(username) = LOWER(?)",
                new Object[]{username},
                Integer.class);
        return count != null && count > 0;
    }

    public void createUser(String username, String encodedPassword) {
        jdbcTemplate.update(
                "INSERT INTO user_account(username, password, role, enabled) VALUES(?,?,?,1)",
                username,
                encodedPassword,
                "ROLE_USER");
    }
}
