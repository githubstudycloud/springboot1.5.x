## Prompt:
undefined

## Response:
undefined

## Prompt:
项目已经🈶一个缓存管理器，我要写个新的缓存管理器，可以通过类别，json参数指定至少不同的缓存，30分钟过期，用的springboot1.5.22

## Response:
理解了您的需求。您希望在Spring Boot 1.5.22环境下创建一个新的缓存管理器，该管理器能够通过类别和JSON参数指定不同的缓存，并且默认过期时间为30分钟。让我为您设计并实现这个缓存管理器。

首先，我们需要创建一个自定义的缓存管理器类：

```java
Copyimport com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CustomCacheManager implements CacheManager {

    private final SimpleCacheManager inMemoryCacheManager;
    private final RedisCacheManager redisCacheManager;
    private final ObjectMapper objectMapper;

    public CustomCacheManager(RedisTemplate<Object, Object> redisTemplate, ObjectMapper objectMapper) {
        this.inMemoryCacheManager = new SimpleCacheManager();
        this.redisCacheManager = new RedisCacheManager(redisTemplate);
        this.objectMapper = objectMapper;

        // 设置Redis缓存的默认过期时间为30分钟
        this.redisCacheManager.setDefaultExpiration(30 * 60);

        List<ConcurrentMapCache> caches = new ArrayList<>();
        caches.add(new ConcurrentMapCache("defaultCache"));
        this.inMemoryCacheManager.setCaches(caches);
        this.inMemoryCacheManager.afterPropertiesSet();
    }

    @Override
    public org.springframework.cache.Cache getCache(String name) {
        try {
            CacheConfig config = objectMapper.readValue(name, CacheConfig.class);
            if ("inmemory".equals(config.getType())) {
                return inMemoryCacheManager.getCache(config.getName());
            } else if ("redis".equals(config.getType())) {
                return redisCacheManager.getCache(config.getName());
            }
        } catch (Exception e) {
            // 如果解析失败，默认使用Redis缓存
            return redisCacheManager.getCache(name);
        }
        return null;
    }

    @Override
    public Collection<String> getCacheNames() {
        List<String> names = new ArrayList<>();
        names.addAll(inMemoryCacheManager.getCacheNames());
        names.addAll(redisCacheManager.getCacheNames());
        return names;
    }

    // 内部类，用于解析JSON配置
    private static class CacheConfig {
        private String type;
        private String name;

        // getters and setters
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
```

然后，我们需要在Spring配置类中注册这个自定义的缓存管理器：

```java
Copyimport com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisTemplate<Object, Object> redisTemplate, ObjectMapper objectMapper) {
        return new CustomCacheManager(redisTemplate, objectMapper);
    }
}
```

使用这个缓存管理器，您可以通过JSON字符串来指定缓存类型和名称。例如：

```java
Copy@Cacheable(value = "{\"type\":\"redis\",\"name\":\"userCache\"}", key = "#id")
public User getUserById(Long id) {
    // 方法实现
}

@Cacheable(value = "{\"type\":\"inmemory\",\"name\":\"tempCache\"}", key = "#key")
public Object getTempData(String key) {
    // 方法实现
}
```

这个实现允许您通过JSON参数指定缓存类型（内存或Redis）和缓存名称。Redis缓存默认过期时间设置为30分钟。如果JSON解析失败，它会默认使用Redis缓存。

请注意，这个实现假设您已经配置了Redis连接。如果还没有配置，您需要在application.properties或application.yml中添加Redis配置：

```
Copyspring.redis.host=localhost
spring.redis.port=6379
```

这个设计提供了灵活性，允许您在运行时动态选择缓存类型，同时保持了与Spring Boot 1.5.22的兼容性。如果您需要进一步的定制或有任何问题，请随时告诉我。

