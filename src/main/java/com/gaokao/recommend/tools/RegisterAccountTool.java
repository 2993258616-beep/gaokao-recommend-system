package com.gaokao.recommend.tools;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.Console;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegisterAccountTool {
    private static final String DB_URL =
            "jdbc:h2:file:./data/gaokao_recommend_db;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;AUTO_SERVER=TRUE";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";
    private static final String STATIC_ADMIN_HASH = "240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9";
    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();
    private static final Pattern STATIC_USER_PATTERN = Pattern.compile(
            "\\{\\s*\"username\"\\s*:\\s*\"([A-Za-z0-9_]+)\"\\s*,\\s*\"passwordHash\"\\s*:\\s*\"([a-fA-F0-9]{64})\"\\s*\\}");

    public static void main(String[] args) throws Exception {
        AccountInput input = readInput(args);
        validate(input);

        Class.forName("org.h2.Driver");
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String encodedPassword = PASSWORD_ENCODER.encode(input.password);
            if (exists(connection, input.username)) {
                updateUser(connection, input.username, encodedPassword);
                System.out.println("Account password updated: " + input.username);
            } else {
                createUser(connection, input.username, encodedPassword);
                System.out.println("Account created: " + input.username);
            }
            syncStaticAccount(input.username, input.password);
            System.out.println("Saved to local database and public-page account list.");
        }
    }

    private static AccountInput readInput(String[] args) {
        if (args != null && args.length >= 2) {
            return new AccountInput(args[0], args[1]);
        }

        Console console = System.console();
        if (console != null) {
            String username = console.readLine("New username: ");
            char[] passwordChars = console.readPassword("New password: ");
            char[] confirmChars = console.readPassword("Confirm password: ");
            String password = new String(passwordChars);
            String confirmPassword = new String(confirmChars);
            if (!password.equals(confirmPassword)) {
                throw new IllegalArgumentException("The two passwords are different.");
            }
            return new AccountInput(username, password);
        }

        Scanner scanner = new Scanner(System.in);
        System.out.print("New username: ");
        String username = scanner.nextLine();
        System.out.print("New password: ");
        String password = scanner.nextLine();
        System.out.print("Confirm password: ");
        String confirmPassword = scanner.nextLine();
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("The two passwords are different.");
        }
        return new AccountInput(username, password);
    }

    private static void validate(AccountInput input) {
        if (input.username == null) {
            throw new IllegalArgumentException("Username cannot be empty.");
        }
        input.username = input.username.trim();
        if (!input.username.matches("[A-Za-z0-9_]{3,30}")) {
            throw new IllegalArgumentException("Username can only use letters, numbers, and underscores, length 3 to 30.");
        }
        if (input.password == null || input.password.length() < 6 || input.password.length() > 50) {
            throw new IllegalArgumentException("Password length must be 6 to 50.");
        }
    }

    private static boolean exists(Connection connection, String username) throws Exception {
        String sql = "SELECT COUNT(1) FROM user_account WHERE LOWER(username) = LOWER(?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private static void createUser(Connection connection, String username, String encodedPassword) throws Exception {
        String sql = "INSERT INTO user_account(username, password, role, enabled) VALUES(?,?,?,1)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, encodedPassword);
            statement.setString(3, "ROLE_USER");
            statement.executeUpdate();
        }
    }

    private static void updateUser(Connection connection, String username, String encodedPassword) throws Exception {
        String sql = "UPDATE user_account SET password=?, enabled=1 WHERE LOWER(username)=LOWER(?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, encodedPassword);
            statement.setString(2, username);
            statement.executeUpdate();
        }
    }

    private static void syncStaticAccount(String username, String password) throws Exception {
        String passwordHash = sha256(password);
        syncStaticAccountFile(Paths.get("assets", "static-users.json"), username, passwordHash);
        syncStaticAccountFile(Paths.get("docs", "assets", "static-users.json"), username, passwordHash);
    }

    private static void syncStaticAccountFile(Path path, String username, String passwordHash) throws Exception {
        LinkedHashMap<String, String> users = new LinkedHashMap<String, String>();
        users.put("admin", STATIC_ADMIN_HASH);

        if (Files.exists(path)) {
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            Matcher matcher = STATIC_USER_PATTERN.matcher(content);
            while (matcher.find()) {
                users.put(matcher.group(1).toLowerCase(), matcher.group(2).toLowerCase());
            }
        }

        users.put(username.toLowerCase(), passwordHash);
        Files.createDirectories(path.getParent());
        Files.write(path, renderStaticUsers(users).getBytes(StandardCharsets.UTF_8));
    }

    private static String renderStaticUsers(LinkedHashMap<String, String> users) {
        StringBuilder builder = new StringBuilder("[\n");
        int index = 0;
        for (Map.Entry<String, String> entry : users.entrySet()) {
            if (index++ > 0) {
                builder.append(",\n");
            }
            builder.append("  {\n")
                    .append("    \"username\": \"").append(entry.getKey()).append("\",\n")
                    .append("    \"passwordHash\": \"").append(entry.getValue()).append("\"\n")
                    .append("  }");
        }
        builder.append("\n]\n");
        return builder.toString();
    }

    private static String sha256(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        for (byte b : hash) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    private static class AccountInput {
        private String username;
        private final String password;

        private AccountInput(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }
}
