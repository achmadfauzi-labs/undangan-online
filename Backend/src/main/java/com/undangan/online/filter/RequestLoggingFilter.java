package com.undangan.online.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import com.undangan.online.util.IpAddressUtil;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "confirmpassword", "token", "refreshtoken"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        org.slf4j.MDC.put("requestId", requestId);

        String ip = IpAddressUtil.extractClientIp(request);
        String method = request.getMethod();
        String uri = request.getRequestURI();

        log.info("Incoming request: {} {} from {}", method, uri, ip);

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        long start = System.currentTimeMillis();

        try {
            filterChain.doFilter(wrappedRequest, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            int status = response.getStatus();
            log.info("Request {} {} completed in {}ms with status {}", method, uri, duration, status);

            String contentType = request.getContentType();
            if (contentType != null && contentType.contains("application/json") && !contentType.contains("multipart")) {
                byte[] body = wrappedRequest.getContentAsByteArray();
                if (body.length > 0) {
                    String bodyStr = new String(body, request.getCharacterEncoding() != null ? request.getCharacterEncoding() : "UTF-8");
                    try {
                        Map<String, Object> map = objectMapper.readValue(bodyStr, Map.class);
                        maskSensitiveFields(map);
                        String maskedJson = objectMapper.writeValueAsString(map);
                        log.info("Request body: {}", maskedJson);
                    } catch (Exception e) {
                        log.warn("Failed to parse request body for logging", e);
                    }
                }
            }

            org.slf4j.MDC.clear();
        }
    }

    private void maskSensitiveFields(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            if (isSensitive(key)) {
                entry.setValue("***masked***");
            } else if (entry.getValue() instanceof Map) {
                maskSensitiveFields((Map<String, Object>) entry.getValue());
            }
        }
    }

    private boolean isSensitive(String key) {
        return SENSITIVE_KEYS.contains(key.toLowerCase());
    }
}
