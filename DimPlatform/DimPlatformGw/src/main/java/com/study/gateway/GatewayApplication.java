//package com.study.gateway;
//
//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
//import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
//import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
//import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
//import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
//
//@EnableAutoConfiguration(exclude = HibernateJpaAutoConfiguration.class)
//@SpringBootApplication
//@EnableEurekaClient
//@EnableDiscoveryClient
//@EnableZuulProxy
//public class GatewayApplication {
//    public static void main(String[] args) {
//        SpringApplication.run(GatewayApplication.class, args);
//    }
//}