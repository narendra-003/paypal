package com.paypal.api_gateway.security;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

public class JwtAuthFilter implements GlobalFilter, Ordered {

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
            // skip filter
            return filterChain.filter(exchange)
                    .doOnSubscribe(s -> System.out.println("Proceeding request without check"))
                    .doOnSuccess(s -> System.out.println("Request passed successfully"))
                    .doOnError(e -> System.out.println("Error occurred "));
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }


        String token = authHeader.substring(7);
        boolean isValidToken = jwtUtil.validateToken(token);

        if (isValidToken) {
            String userEmail = jwtUtil.extractEmail(token);
            exchange.getRequest().mutate()
                    .header("X-User-Email", userEmail);

            return filterChain.filter(exchange)
                    .doOnSubscribe(s -> System.out.println("Proceeding request without check"))
                    .doOnSuccess(s -> System.out.println("Request passed successfully"))
                    .doOnError(e -> System.out.println("Error occurred "));
        } else {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
