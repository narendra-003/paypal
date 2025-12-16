package com.paypal.api_gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

public class JwtAuthFilter implements GlobalFilter, Ordered {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtUtil jwtUtil;
    private static final List<String> PUBLIC_PATHS = List.of("/api/auth/signup", "/api/auth/login");

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain filterChain) {
        String path = exchange.getRequest().getPath().value();
        String normalizedPath = path.replaceAll("/+$", "");

        if(PUBLIC_PATHS.contains(normalizedPath)) {
            logger.debug("Public path accessed without authentication: {}", normalizedPath);
            // skip filter
            return filterChain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Unauthorized request - Missing or invalid Authorization header for path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }


        String token = authHeader.substring(7);
        boolean isValidToken = jwtUtil.validateToken(token);

        if (isValidToken) {
            String userEmail = jwtUtil.extractEmail(token);
            exchange.getRequest().mutate()
                    .header("X-User-Email", userEmail);

            logger.debug("Request authenticated successfully for user: {}, path: {}", userEmail, path);
            return filterChain.filter(exchange);
        } else {
            logger.warn("Unauthorized request - Invalid JWT token for path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
