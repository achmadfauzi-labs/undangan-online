package com.undangan.online.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

public class SecurityHeadersFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(SecurityHeadersFilter.class);

    @Value("${app.env:dev}")
    private String appEnv;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("X-Frame-Options", "DENY");
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
        httpResponse.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self'");

        if ("prod".equalsIgnoreCase(appEnv)) {
            httpResponse.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            log.info("HSTS header applied for prod environment on request {} {}", httpRequest.getMethod(), httpRequest.getRequestURI());
        } else {
            log.info("HSTS header skipped in dev environment for request {} {}", httpRequest.getMethod(), httpRequest.getRequestURI());
        }

        chain.doFilter(request, response);
    }
}
