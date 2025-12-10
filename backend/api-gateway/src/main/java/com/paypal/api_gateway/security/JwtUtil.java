package com.paypal.api_gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;

@Component
public class JwtUtil {
    @Value("${app.jwt.secret}")
    private String JWT_SECRET;

    private Key getSigninKey() {
        return Keys.hmacShaKeyFor(JWT_SECRET.getBytes());
    }

    public String extractEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigninKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            extractEmail(token);    // If parsing succeeds, token is valid
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
