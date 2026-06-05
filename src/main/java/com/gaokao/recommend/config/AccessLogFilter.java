package com.gaokao.recommend.config;

import com.gaokao.recommend.repository.AccessLogRepository;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Component
@Order(20)
public class AccessLogFilter implements Filter {
    private final AccessLogRepository accessLogRepository;

    public AccessLogFilter(AccessLogRepository accessLogRepository) {
        this.accessLogRepository = accessLogRepository;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String path = req.getRequestURI();
        if (shouldRecord(path)) {
            accessLogRepository.record(currentUsername(), path, clientIp(req), req.getHeader("User-Agent"));
        }
        chain.doFilter(request, response);
    }

    private boolean shouldRecord(String path) {
        return "/".equals(path)
                || "/recommend".equals(path)
                || "/login".equals(path)
                || "/api/recommend".equals(path)
                || path.startsWith("/admin");
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "";
        }
        String name = authentication.getName();
        return "anonymousUser".equals(name) ? "" : name;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.trim().isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
