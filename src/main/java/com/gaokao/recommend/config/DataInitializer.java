package com.gaokao.recommend.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Component
public class DataInitializer implements CommandLineRunner {
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        upsertAdminUser();
        reloadMajorsFromCsv();
    }

    private void upsertAdminUser() {
        String encoded = passwordEncoder.encode("admin123");
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM user_account WHERE username='admin'", Integer.class);
        if (count == null || count == 0) {
            jdbcTemplate.update("INSERT INTO user_account(username, password, role, enabled) VALUES(?,?,?,1)", "admin", encoded, "ROLE_ADMIN");
        } else {
            jdbcTemplate.update("UPDATE user_account SET password=?, role='ROLE_ADMIN', enabled=1 WHERE username='admin'", encoded);
        }
    }

    private void reloadMajorsFromCsv() throws Exception {
        ClassPathResource resource = new ClassPathResource("imports/all_database_unique_majors.csv");
        if (!resource.exists()) {
            return;
        }

        jdbcTemplate.update("DELETE FROM major_name_stat");
        BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
        try {
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", 3);
                if (parts.length < 1 || !StringUtils.hasText(parts[0])) {
                    continue;
                }
                String major = parts[0].trim();
                int count = parseInt(parts.length > 1 ? parts[1] : "0");
                String subjectTypes = parts.length > 2 ? parts[2].trim() : "";
                jdbcTemplate.update(
                        "INSERT INTO major_name_stat(major_name, count_num, subject_types) VALUES(?,?,?)",
                        major, count, subjectTypes);
            }
        } finally {
            reader.close();
        }
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return 0;
        }
    }
}
