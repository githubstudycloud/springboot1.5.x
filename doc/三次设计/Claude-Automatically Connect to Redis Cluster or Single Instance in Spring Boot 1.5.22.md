## Prompt:
undefined

## Response:
undefined

## Prompt:
springboot1.5.22的redis根据配置文件自动能连redis集群或者redis单机

## Response:
您说得对,对于Spring Boot 1.5.22版本,我们确实可以通过配置文件来实现自动连接Redis集群或单机。让我为您详细说明一下配置和代码实现:

1. 首先,在pom.xml中添加Redis相关依赖:

```xml
Copy<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

2. 在application.properties (或application.yml)中配置Redis连接信息:

对于单机Redis:

```properties
Copyspring.redis.host=localhost
spring.redis.port=6379
```

对于Redis集群:

```properties
Copyspring.redis.cluster.nodes=127.0.0.1:7000,127.0.0.1:7001,127.0.0.1:7002,127.0.0.1:7003,127.0.0.1:7004,127.0.0.1:7005
```

3. 创建Redis配置类:

```java
Copyimport org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.List;

@Configuration
public class RedisConfig {

    @Value("${spring.redis.host:}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Value("#{'${spring.redis.cluster.nodes:}'.split(',')}")
    private List<String> clusterNodes;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        if (!clusterNodes.isEmpty() && !clusterNodes.get(0).isEmpty()) {
            return new JedisConnectionFactory(new RedisClusterConfiguration(clusterNodes));
        } else {
            return new JedisConnectionFactory(new RedisStandaloneConfiguration(redisHost, redisPort));
        }
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
}
```

这个配置类会根据配置文件中的内容自动选择使用Redis集群还是单机模式。如果存在集群节点配置,则使用集群模式;否则使用单机模式。

4. 在您的服务类中使用RedisTemplate:

```java
Copyimport org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public CacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void setCache(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public String getCache(String key) {
        return (String) redisTemplate.opsForValue().get(key);
    }
}
```

这样,您就可以在应用中使用Redis缓存了,而且可以通过修改配置文件来轻松切换entre单机模式和集群模式。

需要注意的是,这个配置使用了Jedis作为Redis客户端。如果您更偏好使用Lettuce客户端,您需要修改pom.xml中的依赖和RedisConnectionFactory的实现。

如果您有任何其他问题或需要更多的解释,请随时告诉我。

