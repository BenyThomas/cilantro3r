package com.security.keycloak;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.security.oauth2.jwt.Jwt;

public final class KeycloakUserDetailsExtractor {

    private static final String STAFF_CLAIM = "staff";
    private static final String USERNAME = "preferred_username";
    private static final String EMAIL = "email";
    private static final String FEATURE_FLAGS = "feature_flags";

    private KeycloakUserDetailsExtractor() {
    }

    public static StaffDetails fromJwt(Jwt jwt) {
        Map<String, Object> staffSection = jwt.getClaimAsMap(STAFF_CLAIM);
        return new StaffDetails(
                jwt.getClaimAsString(USERNAME),
                jwt.getClaimAsString(EMAIL),
                asString(staffSection, "staffId"),
                asString(staffSection, "branchCode"),
                asString(staffSection, "department"),
                asString(staffSection, "employmentType"),
                jwt.getClaimAsStringList(FEATURE_FLAGS)
        );
    }

    private static String asString(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        return value instanceof String ? (String) value : null;
    }

    public record StaffDetails(
            String username,
            String email,
            String staffId,
            String branchCode,
            String department,
            String employmentType,
            List<String> featureFlags
    ) {
        public List<String> featureFlags() {
            return featureFlags == null ? Collections.emptyList() : featureFlags;
        }
    }
}

