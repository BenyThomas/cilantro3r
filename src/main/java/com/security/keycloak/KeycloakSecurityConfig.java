package com.security.keycloak;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.csrf.LazyCsrfTokenRepository;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.*;

@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "keycloak.enabled", havingValue = "true")
public class KeycloakSecurityConfig {

    @Bean
    public SecurityFilterChain keycloakSecurityFilterChain(HttpSecurity http, ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/assets/**",
                                "/error",
                                "/actuator/**",
                                "/actuator/health",
                                "/actuator/info",
                                "/actuator/health/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(info-> info.oidcUserService(oidcUserService()))
                        .defaultSuccessUrl("/dashboard", true)
                )
                .oauth2ResourceServer(resource -> resource
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository())
                        .ignoringRequestMatchers("/api/**")
                ).logout(logout -> logout
                        .logoutUrl("/logout")                // your <a href="/logout">
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler(oidcLogoutSuccessHandler(clientRegistrationRepository))
                );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {

            // 1) Keep default SCOPE_ authorities from "scope"/"scp"
            JwtGrantedAuthoritiesConverter scopesConverter = new JwtGrantedAuthoritiesConverter();
            Collection<GrantedAuthority> authorities = new ArrayList<>(scopesConverter.convert(jwt));

            // 2) Add Keycloak client roles for "cilantro" as ROLE_ or PERM_
            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
            if (resourceAccess != null) {
                Map<String, Object> cilantro = (Map<String, Object>) resourceAccess.get("cilantro");
                if (cilantro != null) {
                    List<String> roles = (List<String>) cilantro.get("roles");
                    if (roles != null) {
                        roles.forEach(r -> {
                            // e.g. ROLE_FIRE_LUKU_GW_TRANSACTIONS
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + r));
                        });
                    }
                }
            }

            return authorities;
        });

        return converter;
    }
    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        // delegate to the default implementation for loading user info
        OidcUserService delegate = new OidcUserService();

        return (OidcUserRequest userRequest) -> {
            // Load the user as usual
            OidcUser oidcUser = delegate.loadUser(userRequest);

            Map<String, Object> claims = oidcUser.getClaims();

            // Start with existing authorities (OIDC_USER, SCOPE_*, etc.)
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>(oidcUser.getAuthorities());

            // === Custom: add Keycloak client roles from resource_access.cilantro.roles ===
            Object resourceAccessObj = claims.get("resource_access");
            if (resourceAccessObj instanceof Map<?, ?> resourceAccess) {
                Object cilantroObj = resourceAccess.get("cilantro");
                if (cilantroObj instanceof Map<?, ?> cilantro) {
                    Object rolesObj = cilantro.get("roles");
                    if (rolesObj instanceof Collection<?> roles) {
                        for (Object r : roles) {
                            if (r != null) {
                                String roleName = r.toString().trim();
                                if (!roleName.isEmpty()) {
                                    // We prefix with ROLE_ so we can use hasAuthority('ROLE_...')
                                    mappedAuthorities.add(
                                            new SimpleGrantedAuthority("ROLE_" + roleName)
                                    );
                                }
                            }
                        }
                    }
                }
            }

            // You can also pull branchCode, pfNo, etc. from claims if needed

            // Rebuild the OidcUser with the enriched authorities
            return new DefaultOidcUser(
                    mappedAuthorities,
                    oidcUser.getIdToken(),
                    oidcUser.getUserInfo(),
                    "preferred_username" // name attribute key
            );
        };
    }


    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        return new LazyCsrfTokenRepository(new HttpSessionCsrfTokenRepository());
    }

    /**
     * CORS configuration – adjust allowed origins to your environments.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Frontend / callers that are allowed to call Cilantro
        config.setAllowedOrigins(List.of(
                "http://localhost:8030",
                "http://localhost:8080",
                "https://cilantrouat.tcbbank.co.tz:8443",
                "https://cilantro.tcbbank.co.tz:8443"
        ));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "X-XSRF-TOKEN"
        ));
        config.setAllowCredentials(true); // needed if you use cookies / sessions

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
    @Bean
    public LogoutSuccessHandler oidcLogoutSuccessHandler(
            ClientRegistrationRepository clientRegistrationRepository   // 👈 injected here too
    ) {
        OidcClientInitiatedLogoutSuccessHandler handler =
                new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);

        // After Keycloak logout, redirect back here:
        handler.setPostLogoutRedirectUri("{baseUrl}/");
        return handler;
    }
}
