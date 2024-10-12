// WebConfig.java
package com.example.common.config;

import com.example.common.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConditionalOnProperty(name = "app.web.config.enabled", havingValue = "true", matchIfMissing = false)
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Autowired
    private WebConfigProperties webConfigProperties;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (webConfigProperties.isCorsEnabled()) {
            registry.addMapping("/**")
                    .allowedOrigins(webConfigProperties.getAllowedOrigins())
                    .allowedMethods(webConfigProperties.getAllowedMethods())
                    .allowedHeaders(webConfigProperties.getAllowedHeaders())
                    .allowCredentials(webConfigProperties.isAllowCredentials())
                    .maxAge(webConfigProperties.getMaxAge());
        }
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (webConfigProperties.isLoginInterceptorEnabled()) {
            registry.addInterceptor(loginInterceptor)
                    .addPathPatterns("/**")
                    .excludePathPatterns(webConfigProperties.getLoginExcludePaths());
        }
    }
}

// LoginInterceptor.java
package com.example.common.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 在这里实现您的登录验证逻辑
        // 例如，检查 session 中是否有用户信息，或者验证 token
        // 如果未登录，可以抛出异常或重定向到登录页面
        // 返回 true 表示继续执行，返回 false 表示中断请求
        return true;
    }
}

// WebConfigProperties.java
package com.example.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.web.config")
public class WebConfigProperties {

    private boolean enabled = false;
    private boolean corsEnabled = false;
    private boolean loginInterceptorEnabled = false;
    private String[] allowedOrigins = {"*"};
    private String[] allowedMethods = {"GET", "POST", "PUT", "DELETE", "OPTIONS"};
    private String[] allowedHeaders = {"*"};
    private boolean allowCredentials = true;
    private long maxAge = 3600;
    private String[] loginExcludePaths = {"/login", "/register"};

    // Getters and setters for all properties
    // ...
}

// application.properties (或 application.yml)
app.web.config.enabled=false
app.web.config.cors-enabled=false
app.web.config.login-interceptor-enabled=false
app.web.config.allowed-origins=*
app.web.config.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
app.web.config.allowed-headers=*
app.web.config.allow-credentials=true
app.web.config.max-age=3600
app.web.config.login-exclude-paths=/login,/register
