package com.yourcompany.yourproject.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.List;

@Configuration
public class RedisConfig {

    private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${spring.redis.host:localhost}")
    private String host;

    @Value("${spring.redis.port:6379}")
    private int port;

    @Value("${spring.redis.password:}")
    private String password;

    @Value("${spring.redis.cluster.nodes:}")
    private List<String> clusterNodes;

    @PostConstruct
    public void init() {
        logger.info("Redis配置初始化");
        logger.info("Redis host: {}", host);
        logger.info("Redis port: {}", port);
        logger.info("Redis password: {}", password.isEmpty() ? "未设置" : "已设置");
        logger.info("Redis cluster nodes: {}", clusterNodes.isEmpty() ? "未设置" : clusterNodes);
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        logger.info("创建 RedisConnectionFactory");
        if (!clusterNodes.isEmpty()) {
            logger.info("使用集群模式");
            RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(clusterNodes);
            if (!password.isEmpty()) {
                clusterConfig.setPassword(password);
            }
            return new LettuceConnectionFactory(clusterConfig);
        } else {
            logger.info("使用单机模式");
            RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration(host, port);
            if (!password.isEmpty()) {
                standaloneConfig.setPassword(password);
            }
            return new LettuceConnectionFactory(standaloneConfig);
        }
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        logger.info("创建 RedisTemplate");
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        serializer.setObjectMapper(mapper);

        template.setValueSerializer(serializer);
        template.setKeySerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
