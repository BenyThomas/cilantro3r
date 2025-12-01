package com.security.keycloak;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public class KeycloakUserAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String REALM_ACCESS = "realm_access";
    private static final String RESOURCE_ACCESS = "resource_access";
    private static final String ROLES = "roles";
    private static final String CLIENT_ID = "cilantro";

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<String> roles = new HashSet<>();

        roles.addAll(resolveRealmRoles(jwt));
        roles.addAll(resolveClientRoles(jwt));

        return roles.stream()
                .map(this::asAuthority)
                .collect(Collectors.toUnmodifiableSet());
    }

    private Collection<String> resolveRealmRoles(Jwt jwt) {
        Object realmAccess = jwt.getClaims().get(REALM_ACCESS);
        if (realmAccess instanceof Map<?, ?> realmAccessMap) {
            Object realmRoles = realmAccessMap.get(ROLES);
            if (realmRoles instanceof List<?> roleList) {
                return roleList.stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .collect(Collectors.toSet());
            }
        }
        return Set.of();
    }

    private Collection<String> resolveClientRoles(Jwt jwt) {
        Object resourceAccess = jwt.getClaims().get(RESOURCE_ACCESS);
        if (resourceAccess instanceof Map<?, ?> resourceAccessMap) {
            Object clientAccess = resourceAccessMap.get(CLIENT_ID);
            if (clientAccess instanceof Map<?, ?> clientMap) {
                Object clientRoles = clientMap.get(ROLES);
                if (clientRoles instanceof List<?> roleList) {
                    return roleList.stream()
                            .filter(String.class::isInstance)
                            .map(String.class::cast)
                            .collect(Collectors.toSet());
                }
            }
        }
        return Set.of();
    }

    private GrantedAuthority asAuthority(String role) {
        String formatted = role.toUpperCase().startsWith("ROLE_") ? role.toUpperCase() : "ROLE_" + role.toUpperCase();
        return new SimpleGrantedAuthority(formatted);
    }
}

