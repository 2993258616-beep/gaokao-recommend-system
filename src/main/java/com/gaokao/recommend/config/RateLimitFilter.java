package com.gaokao.recommend.config;

import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter implements Filter {
    private static final int MAX_REQUESTS_PER_MINUTE = 180;
    private final Map<String, WindowCounter> counterMap = new ConcurrentHashMap<String, WindowCounter>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String ip = req.getRemoteAddr();
        long nowMinute = System.currentTimeMillis() / 60000L;
        WindowCounter counter = counterMap.compute(ip, (key, old) -> {
            if (old == null || old.minute != nowMinute) return new WindowCounter(nowMinute);
            return old;
        });
        if (counter.count.incrementAndGet() > MAX_REQUESTS_PER_MINUTE) {
            res.setStatus(429);
            res.setContentType("text/plain;charset=UTF-8");
            res.getWriter().write("请求过快，请稍后再试");
            return;
        }
        chain.doFilter(request, response);
    }

    private static class WindowCounter {
        private final long minute;
        private final AtomicInteger count = new AtomicInteger(0);
        private WindowCounter(long minute) { this.minute = minute; }
    }
}
