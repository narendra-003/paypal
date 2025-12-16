package com.paypal.api_gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

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

    public boolean validateToken(String token) {
        try {
            logger.debug("Validating JWT token");
            extractEmail(token);    // If parsing succeeds, token is valid
            logger.debug("JWT token is valid");
            return true;
        } catch (Exception e) {
            logger.warn("JWT token validation failed: {}", e.getMessage());
            return false;
        }
    }
}
