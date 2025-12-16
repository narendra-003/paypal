package com.paypal.api_gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {
    private static final Logger logger = LoggerFactory.getLogger(RateLimitConfig.class);

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userEmail = exchange.getRequest().getHeaders().getFirst("X-User-Email");
            if(userEmail != null) {
                return Mono.just(userEmail);
            }

            String ipAddress = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            logger.debug("Rate limiting by IP address (no user email header): {}", ipAddress);

            // fallback via ip address
            return Mono.just(ipAddress);
        };
    }
}
