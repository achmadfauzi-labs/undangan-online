package com.undangan.online.util;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SensitiveDataMasker {

    private static final Set<String> SENSITIVE_KEYS = new HashSet<>(Set.of(
            "password", "passwordhash", "token", "refreshtoken", "email"
    ));

    private SensitiveDataMasker() {
    }

    public static String maskAndSerialize(Object entityOrMap, ObjectMapper mapper) throws JacksonException {
        Map<String, Object> map = mapper.convertValue(entityOrMap, Map.class);
        maskSensitiveFields(map);
        return mapper.writeValueAsString(map);
    }

    private static void maskSensitiveFields(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (isSensitive(key)) {
                map.put(key, "***masked***");
            } else if (value instanceof Map) {
                maskSensitiveFields((Map<String, Object>) value);
            }
        }
    }

    private static boolean isSensitive(String key) {
        return SENSITIVE_KEYS.contains(key.toLowerCase());
    }
}
