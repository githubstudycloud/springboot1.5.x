package com.study.gateway.config;

import org.apache.commons.dbcp.BasicDataSource;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = "com.study.gateway.dao.common", sqlSessionFactoryRef = "commonSqlSessionFactory")
public class CommonDbConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.common")
    public BasicDataSource commonDataSource() {
        return new BasicDataSource();
    }
}