package com.gaokao.recommend.tools;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.Console;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Scanner;

public class RegisterAccountTool {
    private static final String DB_URL =
            "jdbc:h2:file:./data/gaokao_recommend_db;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;AUTO_SERVER=TRUE";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";
    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    public static void main(String[] args) throws Exception {
        AccountInput input = readInput(args);
        validate(input);

        Class.forName("org.h2.Driver");
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            if (exists(connection, input.username)) {
                System.out.println("账号已存在，请换一个账号：" + input.username);
                return;
            }
            createUser(connection, input.username, PASSWORD_ENCODER.encode(input.password));
            System.out.println("账号创建成功：" + input.username);
            System.out.println("该账号已保存到数据库，可以在网页登录使用。");
        }
    }

    private static AccountInput readInput(String[] args) {
        if (args != null && args.length >= 2) {
            return new AccountInput(args[0], args[1]);
        }

        Console console = System.console();
        if (console != null) {
            String username = console.readLine("请输入新账号：");
            char[] passwordChars = console.readPassword("请输入新密码：");
            char[] confirmChars = console.readPassword("请再次输入新密码：");
            String password = new String(passwordChars);
            String confirmPassword = new String(confirmChars);
            if (!password.equals(confirmPassword)) {
                throw new IllegalArgumentException("两次输入的密码不一致。");
            }
            return new AccountInput(username, password);
        }

        Scanner scanner = new Scanner(System.in);
        System.out.print("请输入新账号：");
        String username = scanner.nextLine();
        System.out.print("请输入新密码：");
        String password = scanner.nextLine();
        System.out.print("请再次输入新密码：");
        String confirmPassword = scanner.nextLine();
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("两次输入的密码不一致。");
        }
        return new AccountInput(username, password);
    }

    private static void validate(AccountInput input) {
        if (input.username == null) {
            throw new IllegalArgumentException("账号不能为空。");
        }
        input.username = input.username.trim();
        if (!input.username.matches("[A-Za-z0-9_]{3,30}")) {
            throw new IllegalArgumentException("账号只能使用字母、数字、下划线，长度 3 到 30 位。");
        }
        if (input.password == null || input.password.length() < 6 || input.password.length() > 50) {
            throw new IllegalArgumentException("密码长度需要在 6 到 50 位之间。");
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

    private static class AccountInput {
        private String username;
        private final String password;

        private AccountInput(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }
}
