# 项目结构
src
├── main
│   ├── java
│   │   └── com
│   │       └── example
│   │           └── monitoringdemo
│   │               ├── MonitoringDemoApplication.java
│   │               ├── config
│   │               │   └── MetricsConfig.java
│   │               ├── controller
│   │               │   └── DemoController.java
│   │               ├── service
│   │               │   └── BusinessService.java
│   │               └── metrics
│   │                   └── CustomMetrics.java
│   └── resources
│       ├── application.yml
│       └── logback-spring.xml
└── test
    └── java
        └── com
            └── example
                └── monitoringdemo
                    └── MonitoringDemoApplicationTests.java

# pom.xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>
    <groupId>com.example</groupId>
    <artifactId>monitoring-demo</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>monitoring-demo</name>
    <description>Demo project for Spring Boot with monitoring</description>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
        </dependency>
        <dependency>
            <groupId>net.logstash.logback</groupId>
            <artifactId>logstash-logback-encoder</artifactId>
            <version>7.4</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>

# MonitoringDemoApplication.java
package com.example.monitoringdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MonitoringDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(MonitoringDemoApplication.class, args);
    }
}

# MetricsConfig.java
package com.example.monitoringdemo.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}

# DemoController.java
package com.example.monitoringdemo.controller;

import com.example.monitoringdemo.service.BusinessService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {
    private final BusinessService businessService;

    public DemoController(BusinessService businessService) {
        this.businessService = businessService;
    }

    @GetMapping("/demo")
    public String demo() {
        return businessService.performBusinessOperation();
    }
}

# BusinessService.java
package com.example.monitoringdemo.service;

import com.example.monitoringdemo.metrics.CustomMetrics;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BusinessService {
    private static final Logger logger = LoggerFactory.getLogger(BusinessService.class);
    private final CustomMetrics customMetrics;

    public BusinessService(CustomMetrics customMetrics) {
        this.customMetrics = customMetrics;
    }

    @Timed(value = "business.operation", description = "Time taken to perform business operation")
    public String performBusinessOperation() {
        logger.info("Starting business operation");
        try {
            Thread.sleep(1000); // Simulating work
            customMetrics.incrementBusinessOperationCount();
            logger.info("Business operation completed successfully");
            return "Operation completed successfully";
        } catch (InterruptedException e) {
            logger.error("Business operation interrupted", e);
            return "Operation failed";
        }
    }
}

# CustomMetrics.java
package com.example.monitoringdemo.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class CustomMetrics {
    private final Counter businessOperationCounter;

    public CustomMetrics(MeterRegistry registry) {
        this.businessOperationCounter = Counter.builder("business.operation.count")
                .description("Number of business operations performed")
                .register(registry);
    }

    public void incrementBusinessOperationCount() {
        this.businessOperationCounter.increment();
    }
}

# application.yml
server:
  port: 8080

spring:
  application:
    name: monitoring-demo

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    health:
      show-details: always

logging:
  level:
    root: INFO
    com.example.monitoringdemo: DEBUG

# logback-spring.xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    <include resource="org/springframework/boot/logging/logback/console-appender.xml"/>

    <appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
        <destination>localhost:5000</destination>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="LOGSTASH"/>
    </root>

    <logger name="com.example.monitoringdemo" level="DEBUG"/>
</configuration>

# MonitoringDemoApplicationTests.java
package com.example.monitoringdemo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MonitoringDemoApplicationTests {
    @Test
    void contextLoads() {
    }
}
