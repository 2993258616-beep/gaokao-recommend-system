package com.gaokao.recommend.controller;

import com.gaokao.recommend.repository.AccessLogRepository;
import com.gaokao.recommend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminController {
    private final UserRepository userRepository;
    private final AccessLogRepository accessLogRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminController(UserRepository userRepository, AccessLogRepository accessLogRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.accessLogRepository = accessLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        model.addAttribute("totalUsers", userRepository.countAll());
        model.addAttribute("enabledUsers", userRepository.countEnabled());
        model.addAttribute("recommendToday", accessLogRepository.countToday("/api/recommend"));
        model.addAttribute("recommendTotal", accessLogRepository.countAll("/api/recommend"));
        model.addAttribute("recentLogs", accessLogRepository.recent(12));
        return "admin-dashboard";
    }

    @GetMapping("/admin/accounts")
    public String accounts(Model model) {
        model.addAttribute("users", userRepository.findAll());
        return "admin-accounts";
    }

    @PostMapping("/admin/accounts")
    public String createAccount(@RequestParam String username,
                                @RequestParam String password,
                                @RequestParam(defaultValue = "ROLE_USER") String role,
                                RedirectAttributes redirectAttributes) {
        username = username == null ? "" : username.trim();
        if (!username.matches("[A-Za-z0-9_]{3,30}") || !StringUtils.hasText(password) || password.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "账号格式或密码长度不符合要求。");
            return "redirect:/admin/accounts";
        }
        if (userRepository.existsByUsername(username)) {
            redirectAttributes.addFlashAttribute("error", "账号已存在，可以直接重置密码。");
            return "redirect:/admin/accounts";
        }
        userRepository.createUser(username, passwordEncoder.encode(password), normalizeRole(role));
        redirectAttributes.addFlashAttribute("ok", "账号已创建。");
        return "redirect:/admin/accounts";
    }

    @PostMapping("/admin/accounts/{id}/password")
    public String resetPassword(@PathVariable Long id, @RequestParam String password,
                                RedirectAttributes redirectAttributes) {
        if (!StringUtils.hasText(password) || password.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "新密码至少 6 位。");
            return "redirect:/admin/accounts";
        }
        userRepository.updatePassword(id, passwordEncoder.encode(password));
        redirectAttributes.addFlashAttribute("ok", "密码已重置。");
        return "redirect:/admin/accounts";
    }

    @PostMapping("/admin/accounts/{id}/enable")
    public String enable(@PathVariable Long id) {
        userRepository.setEnabled(id, true);
        return "redirect:/admin/accounts";
    }

    @PostMapping("/admin/accounts/{id}/disable")
    public String disable(@PathVariable Long id) {
        userRepository.setEnabled(id, false);
        return "redirect:/admin/accounts";
    }

    @GetMapping("/terms")
    public String terms() {
        return "terms";
    }

    private String normalizeRole(String role) {
        return "ROLE_ADMIN".equals(role) ? "ROLE_ADMIN" : "ROLE_USER";
    }
}
