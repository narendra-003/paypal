package com.paypal.api_gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userEmail = exchange.getRequest().getHeaders().getFirst("X-User-Email");
            if(userEmail != null) {
                return Mono.just(userEmail);
            }

            System.out.println("hhhhhhhhhhhhhhhhhhhhhhhh--------------------");

            // fallback via ip address
            System.out.println("ip: " + exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
            return Mono.just(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
        };
    }
}
