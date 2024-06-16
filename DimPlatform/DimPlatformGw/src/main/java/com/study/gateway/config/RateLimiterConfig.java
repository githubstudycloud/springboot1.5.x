package com.study.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.netflix.zuul.filters.post.RateLimiterPostFilter;
import org.springframework.cloud.netflix.zuul.filters.RateLimiter;

@Configuration
public class RateLimiterConfig {

    @Bean
    public RateLimiter rateLimiter() {
        return new RateLimiter();
    }

    @Bean
    public RateLimiterPostFilter rateLimiterPostFilter() {
        return new RateLimiterPostFilter(rateLimiter());
    }
}
