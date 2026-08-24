package com.undangan.online.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicyConfig {

    private static final Logger log = LoggerFactory.getLogger(PasswordPolicyConfig.class);

    private static final String SPECIAL_CHARS = "!@#$%^&*()_+-=[]{}|;:,.<>?";

    public void validate(String password) {
        if (password == null || password.length() < 8) {
            log.warn("Password validation failed: length < 8");
            throw new com.undangan.online.exception.AuthException("VALIDATION_ERROR", "Password minimal 8 karakter", 400);
        }
        if (!password.matches(".*[A-Z].*")) {
            log.warn("Password validation failed: missing uppercase letter");
            throw new com.undangan.online.exception.AuthException("VALIDATION_ERROR", "Password harus mengandung minimal 1 huruf besar", 400);
        }
        if (!password.matches(".*[a-z].*")) {
            log.warn("Password validation failed: missing lowercase letter");
            throw new com.undangan.online.exception.AuthException("VALIDATION_ERROR", "Password harus mengandung minimal 1 huruf kecil", 400);
        }
        if (!password.matches(".*[0-9].*")) {
            log.warn("Password validation failed: missing digit");
            throw new com.undangan.online.exception.AuthException("VALIDATION_ERROR", "Password harus mengandung minimal 1 angka", 400);
        }
        boolean hasSpecial = false;
        for (char c : password.toCharArray()) {
            if (SPECIAL_CHARS.indexOf(c) >= 0) {
                hasSpecial = true;
                break;
            }
        }
        if (!hasSpecial) {
            log.warn("Password validation failed: missing special character");
            throw new com.undangan.online.exception.AuthException("VALIDATION_ERROR", "Password harus mengandung minimal 1 karakter khusus (!@#$%^&*()_+-=[]{}|;:,.<>?)", 400);
        }
    }
}
