package org.ashkelyonok.orderservice.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component
public class JwtUtil {


    private final ObjectMapper objectMapper = new ObjectMapper();

    private JsonNode getPayload(String token) {
        try {
            String[] chunks = token.split("\\.");
            String payload = new String(java.util.Base64.getUrlDecoder().decode(chunks[1]));
            return objectMapper.readTree(payload);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid token format");
        }
    }

    public String extractUsername(String token) {
        return getPayload(token).get("sub").asText();
    }

    public String extractRole(String token) {
        return getPayload(token).get("role").asText();
    }

    public Long extractUserId(String token) {
        return getPayload(token).get("userId").asLong();
    }

    public boolean isTokenValid(String token) {
        try {
            long expSeconds = getPayload(token).get("exp").asLong();
            Date expirationDate = new Date(expSeconds * 1000);
            return expirationDate.after(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}