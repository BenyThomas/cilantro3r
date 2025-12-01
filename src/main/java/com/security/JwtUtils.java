/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.security;

import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

/**
 *
 * @author arthur.ndossi
 */
@Component
public class JwtUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(JwtUtils.class);
	@Value("${bot.app.jwtSecret: bezKoderSecretKey}")
	private String jwtSecret;
	@Value("${bot.app.jwtExpirationMs: 259200000}")// 90000
	private int jwtExpirationMs;

	public String generateJwtToken(Authentication authentication) {
		LOGGER.info("Authentication Principal {}", authentication.getPrincipal());
		UserDetails userPrincipal = new UserDetails() {
			@Override
			public Collection<? extends GrantedAuthority> getAuthorities() {
				return new ArrayList<>();
			}

			@Override
			public String getPassword() {
				return "p@ssw0rd";
			}

			@Override
			public String getUsername() {
				return (String) authentication.getPrincipal();
			}

			@Override
			public boolean isAccountNonExpired() {
				return false;
			}

			@Override
			public boolean isAccountNonLocked() {
				return false;
			}

			@Override
			public boolean isCredentialsNonExpired() {
				return false;
			}

			@Override
			public boolean isEnabled() {
				return true;
			}
		};
//		UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
		Map<String, Object> claims = new HashMap<>();
		claims.put("device", "api");
		return Jwts.builder().setClaims(claims)
				.setSubject((userPrincipal.getUsername()))
				.setIssuedAt(new Date())
				.setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
				.signWith(SignatureAlgorithm.HS512, jwtSecret)
				.compact();
	}

	public String getUserNameFromJwtToken(String token) {
		String username =Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).getBody().getSubject();
		LOGGER.info("getUserNameFromJwtToken->{}  from {}", username, token);
		return username;
	}

	public boolean validateJwtToken(String authToken) {
		try {
			Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(authToken);
			return true;
		} catch (MalformedJwtException e) {
			LOGGER.error("Invalid JWT token: {}", e.getMessage());
		} catch (ExpiredJwtException e) {
			LOGGER.error("JWT token is expired: {}", e.getMessage());
		} catch (UnsupportedJwtException e) {
			LOGGER.error("JWT token is unsupported: {}", e.getMessage());
		} catch (IllegalArgumentException e) {
			LOGGER.error("JWT claims string is empty: {}", e.getMessage());
		}
		return false;
	}

	public String parseJwt(HttpServletRequest request) {
		String headerAuth = request.getHeader("Authorization");
		if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
			return headerAuth.substring(7);
		}
		return null;
	}
}
