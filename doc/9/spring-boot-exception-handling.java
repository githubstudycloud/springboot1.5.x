// 项目结构
src
├── main
│   ├── java
│   │   └── com
│   │       └── example
│   │           └── demo
│   │               ├── DemoApplication.java
│   │               ├── controller
│   │               │   └── UserController.java
│   │               ├── service
│   │               │   ├── UserService.java
│   │               │   └── impl
│   │               │       └── UserServiceImpl.java
│   │               ├── exception
│   │               │   ├── BusinessException.java
│   │               │   └── GlobalExceptionHandler.java
│   │               └── aspect
│   │                   └── LoggingAspect.java
│   └── resources
│       └── application.yml

// DemoApplication.java
package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}

// UserController.java
package com.example.demo.controller;

import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public String getUserInfo(@PathVariable Long id) throws Exception {
        return userService.getUserInfo(id);
    }
}

// UserService.java
package com.example.demo.service;

public interface UserService {
    String getUserInfo(Long id) throws Exception;
}

// UserServiceImpl.java
package com.example.demo.service.impl;

import com.example.demo.exception.BusinessException;
import com.example.demo.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Override
    public String getUserInfo(Long id) throws Exception {
        // 模拟业务逻辑
        if (id <= 0) {
            throw new BusinessException("Invalid user ID");
        }
        if (id > 100) {
            throw new Exception("User not found");
        }
        return "User info for ID: " + id;
    }
}

// BusinessException.java
package com.example.demo.exception;

public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}

// GlobalExceptionHandler.java
package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<String> handleBusinessException(BusinessException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        return new ResponseEntity<>("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

// LoggingAspect.java
package com.example.demo.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

    @Around("execution(* com.example.demo.service.*.*(..))")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        logger.info("Entering method: {}", methodName);
        try {
            Object result = joinPoint.proceed();
            logger.info("Method {} completed successfully", methodName);
            return result;
        } catch (Exception e) {
            logger.error("Error in method {}: {}", methodName, e.getMessage());
            throw e;
        }
    }
}

// application.yml
spring:
  application:
    name: demo-app

server:
  port: 8080

logging:
  level:
    root: INFO
    com.example.demo: DEBUG
