package com.paypal.user_service.security;

import com.paypal.user_service.config.AppConstants;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Map;

@Component
public class JWTUtil {

    private static final Logger logger = LoggerFactory.getLogger(JWTUtil.class);

    @Value("${app.jwt.secret}")
    private String JWT_SECRET;

    private Key getSigninKey() {
        return Keys.hmacShaKeyFor(JWT_SECRET.getBytes());
    }

    public String extractEmail(String token) {
        logger.debug("Extracting email from JWT token");
        return Jwts.parserBuilder()
                .setSigningKey(getSigninKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token, String username) {
        try {
            logger.debug("Validating JWT token for user: {}", username);
            extractEmail(token);    // If parsing succeeds, token is valid
            logger.debug("JWT token is valid for user: {}", username);
            return true;
        } catch (Exception e) {
            logger.warn("JWT token validation failed for user: {}: {}", username, e.getMessage());
            return false;
        }
    }

    public String extractUsername(String token) {
        logger.debug("Extracting username from JWT token");
        return Jwts.parserBuilder()
                .setSigningKey(getSigninKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public String generateToken(Map<String, Object> claims, String email) {
        logger.info("Generating JWT token for email: {}", email);
        logger.debug("Token claims: {}", claims.keySet());
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + AppConstants.JWT_EXPIRATION))
                .signWith(getSigninKey(), SignatureAlgorithm.HS256)
                .compact(); // build
    }

    public String extractRole(String token) {
        logger.debug("Extracting role from JWT token");
        return (String) Jwts.parserBuilder()
                .setSigningKey(getSigninKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role");

    }

    public Long extractUserId(String token) {
        logger.debug("Extracting user ID from JWT token");
        return Long.parseLong(
                Jwts.parserBuilder()
                        .setSigningKey(getSigninKey())
                        .build()
                        .parseClaimsJws(token)
                        .getBody()
                        .get("userId").toString()
        );
    }
}
