package com.undangan.online.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class PublicRateLimitFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(PublicRateLimitFilter.class);

    private static final int LIMIT = 10;
    private static final long WINDOW_MS = 60_000L;

    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        String path = request.getRequestURI();

        boolean isPublicSubmit = ("/api/v1/public/rsvp".equals(path) || "/api/v1/public/guestbook".equals(path))
                && "POST".equals(request.getMethod());

        if (isPublicSubmit) {
            String ip = clientIp(request);
            String key = ip + ":" + path;
            Deque<Long> dq = hits.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
            Instant now = Instant.now();
            while (!dq.isEmpty() && now.toEpochMilli() - dq.peekFirst() > WINDOW_MS) {
                dq.pollFirst();
            }
            if (dq.size() >= LIMIT) {
                log.warn("Rate limit exceeded: ip={}, path={}", ip, path);
                HttpServletResponse r = (HttpServletResponse) res;
                r.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                r.setContentType("application/json;charset=UTF-8");
                r.getWriter().write("{\"success\":false,\"message\":\"Terlalu banyak permintaan. Coba lagi nanti.\",\"data\":null}");
                return;
            }
            dq.offerLast(now.toEpochMilli());
        }

        chain.doFilter(req, res);
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
