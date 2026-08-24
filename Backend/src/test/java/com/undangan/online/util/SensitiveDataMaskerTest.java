package com.undangan.online.util;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class SensitiveDataMaskerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void maskAndSerialize_masksPassword() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("username", "john");
        map.put("password", "secret123");

        String json = SensitiveDataMasker.maskAndSerialize(map, MAPPER);
        assertTrue(json.contains("\"username\":\"john\""));
        assertTrue(json.contains("\"password\":\"***masked***\""));
    }

    @Test
    void maskAndSerialize_masksEmail() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("email", "john@example.com");

        String json = SensitiveDataMasker.maskAndSerialize(map, MAPPER);
        assertTrue(json.contains("\"email\":\"***masked***\""));
    }

    @Test
    void maskAndSerialize_masksNestedFields() throws Exception {
        Map<String, Object> inner = new HashMap<>();
        inner.put("passwordHash", "hashed");
        inner.put("name", "John");

        Map<String, Object> outer = new HashMap<>();
        outer.put("user", inner);

        String json = SensitiveDataMasker.maskAndSerialize(outer, MAPPER);
        assertTrue(json.contains("\"name\":\"John\""));
        assertTrue(json.contains("\"passwordHash\":\"***masked***\""));
    }

    @Test
    void maskAndSerialize_caseInsensitive() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("Password", "secret");
        map.put("REFRESHTOKEN", "token123");

        String json = SensitiveDataMasker.maskAndSerialize(map, MAPPER);
        assertTrue(json.contains("\"Password\":\"***masked***\""));
        assertTrue(json.contains("\"REFRESHTOKEN\":\"***masked***\""));
    }
}
