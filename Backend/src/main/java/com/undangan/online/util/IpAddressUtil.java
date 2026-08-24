package com.undangan.online.util;

import jakarta.servlet.http.HttpServletRequest;

public class IpAddressUtil {

    private IpAddressUtil() {
    }

    public static String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
