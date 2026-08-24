package com.undangan.online.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.jupiter.api.Assertions.*;

class IpAddressUtilTest {

    @Test
    void extractClientIp_fromXForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.5, 198.51.100.1");
        assertEquals("203.0.113.5", IpAddressUtil.extractClientIp(request));
    }

    @Test
    void extractClientIp_fromXForwardedFor_singleIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.5");
        assertEquals("203.0.113.5", IpAddressUtil.extractClientIp(request));
    }

    @Test
    void extractClientIp_fallbackToRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.1");
        assertEquals("192.168.1.1", IpAddressUtil.extractClientIp(request));
    }

    @Test
    void extractClientIp_xForwardedForBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "");
        request.setRemoteAddr("192.168.1.1");
        assertEquals("192.168.1.1", IpAddressUtil.extractClientIp(request));
    }
}
