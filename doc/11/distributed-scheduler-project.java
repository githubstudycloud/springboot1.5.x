// pom.xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.example</groupId>
        <artifactId>springboot-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>distributed-scheduler</artifactId>
    <version>1.0.0-SNAPSHOT</version>

    <dependencies>
        <!-- 引入公共模块 -->
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>common-web-module</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>

        <!-- Redisson for distributed lock -->
        <dependency>
            <groupId>org.redisson</groupId>
            <artifactId>redisson-spring-boot-starter</artifactId>
            <version>3.18.0</version>
        </dependency>
    </dependencies>
</project>

// DistributedSchedulerApplication.java
package com.example.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DistributedSchedulerApplication {
    public static void main(String[] args) {
        SpringApplication.run(DistributedSchedulerApplication.class, args);
    }
}

// RedissonConfig.java
package com.example.scheduler.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://localhost:6379");
        return Redisson.create(config);
    }
}

// DistributedSchedulerTask.java
package com.example.scheduler.task;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class DistributedSchedulerTask {

    @Autowired
    private RedissonClient redissonClient;

    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public void scheduledTask() {
        RLock lock = redissonClient.getLock("scheduledTaskLock");
        boolean isLocked = false;
        try {
            // 尝试获取锁，等待5秒，10秒后自动释放
            isLocked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (isLocked) {
                // 执行定时任务的逻辑
                System.out.println("执行定时任务 - " + System.currentTimeMillis());
            } else {
                System.out.println("未能获取锁，跳过本次执行");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (isLocked) {
                lock.unlock();
            }
        }
    }
}

// application.yml
spring:
  application:
    name: distributed-scheduler

app:
  web:
    config:
      enabled: true
      cors-enabled: true
      login-interceptor-enabled: true

# Redis配置（如果使用外部Redis服务器，请相应修改）
spring.redis:
  host: localhost
  port: 6379
