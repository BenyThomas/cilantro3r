package com.security.keycloak;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "keycloak.enabled", havingValue = "true")
public class KeycloakSecurityConfig {

    @Bean
    public SecurityFilterChain keycloakSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/**")
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
                // Browser login via Keycloak
                .oauth2Login(oauth -> oauth
                        // if you want a custom login page, you can add:
                        // .loginPage("/oauth2/authorization/cilantro")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userAuthoritiesMapper(authorities -> authorities)
                        )
                )
                // JWT for API requests (e.g. if you call Cilantro APIs from other services)
                .oauth2ResourceServer(resource -> resource
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                )
                // For browser login, keep sessions enabled
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakUserAuthoritiesConverter());
        return converter;
    }

}
