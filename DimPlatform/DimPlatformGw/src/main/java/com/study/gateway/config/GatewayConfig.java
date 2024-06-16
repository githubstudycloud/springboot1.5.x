package com.study.gateway.config;

import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.netflix.zuul.ZuulFilterInitializer;

@Configuration
public class GatewayConfig {

    @Bean
    public ZuulProperties zuulProperties() {
        return new ZuulProperties();
    }

    @Bean
    public ZuulFilterInitializer zuulFilterInitializer() {
        return new ZuulFilterInitializer();
    }
}
