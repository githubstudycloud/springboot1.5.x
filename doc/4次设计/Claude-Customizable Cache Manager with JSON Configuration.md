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

## Prompt:
我是要用1个类型名标记缓存，，把类型作为参数传进方法获取缓存，返回一个token因为实现会通过不同的接口url根据不同json参数获取token，同时我代码有其它缓存管理，，希望不影响其他的

## Response:
明白了，您希望创建一个新的缓存管理器，使用类型名来标记缓存，并通过方法参数传入类型和JSON参数来获取缓存。同时，您希望这个新的缓存管理器不影响现有的其他缓存管理。让我们来实现这个需求。

首先，创建一个新的缓存管理器类：

```java
Copyimport com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class TypeBasedCacheManager implements CacheManager {

    private final RedisCacheManager redisCacheManager;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, Cache> cacheMap = new ConcurrentHashMap<>();

    public TypeBasedCacheManager(RedisTemplate<Object, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisCacheManager = new RedisCacheManager(redisTemplate);
        this.redisCacheManager.setDefaultExpiration(30 * 60); // 30 minutes default expiration
        this.objectMapper = objectMapper;
    }

    public String getToken(String type, String jsonParams) {
        try {
            // 解析JSON参数
            TokenParams params = objectMapper.readValue(jsonParams, TokenParams.class);
            
            // 构造缓存key
            String cacheKey = type + ":" + params.getKey();
            
            // 获取或创建缓存
            Cache cache = getCache(type);
            
            // 尝试从缓存获取token
            String token = cache.get(cacheKey, String.class);
            if (token != null) {
                return token;
            }
            
            // 如果缓存中没有，则通过接口获取token（这里需要实现具体的接口调用逻辑）
            token = callApiToGetToken(type, params);
            
            // 将token存入缓存
            cache.put(cacheKey, token);
            
            return token;
        } catch (Exception e) {
            // 处理异常
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Cache getCache(String name) {
        return cacheMap.computeIfAbsent(name, k -> {
            RedisCache redisCache = (RedisCache) redisCacheManager.getCache(name);
            return new TypeBasedRedisCache(redisCache, name);
        });
    }

    @Override
    public Collection<String> getCacheNames() {
        return cacheMap.keySet();
    }

    // 内部类，用于解析JSON参数
    private static class TokenParams {
        private String key;
        // 其他必要的参数

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }
    }

    // 这个方法需要根据实际情况实现，用于调用不同的接口获取token
    private String callApiToGetToken(String type, TokenParams params) {
        // 根据type和params调用相应的API获取token
        // 这里需要实现具体的逻辑
        return "dummy-token-" + type + "-" + params.getKey();
    }

    // 自定义的Redis缓存实现，用于添加类型前缀
    private static class TypeBasedRedisCache implements Cache {
        private final RedisCache delegate;
        private final String type;

        public TypeBasedRedisCache(RedisCache delegate, String type) {
            this.delegate = delegate;
            this.type = type;
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public Object getNativeCache() {
            return delegate.getNativeCache();
        }

        @Override
        public ValueWrapper get(Object key) {
            return delegate.get(type + ":" + key);
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            return delegate.get(this.type + ":" + key, type);
        }

        @Override
        public <T> T get(Object key, Callable<T> valueLoader) {
            return delegate.get(type + ":" + key, valueLoader);
        }

        @Override
        public void put(Object key, Object value) {
            delegate.put(type + ":" + key, value);
        }

        @Override
        public ValueWrapper putIfAbsent(Object key, Object value) {
            return delegate.putIfAbsent(type + ":" + key, value);
        }

        @Override
        public void evict(Object key) {
            delegate.evict(type + ":" + key);
        }

        @Override
        public void clear() {
            delegate.clear();
        }
    }
}
```

然后，在Spring配置类中注册这个新的缓存管理器：

```java
Copyimport com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class CacheConfig {

    @Bean
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }

    @Bean
    public TypeBasedCacheManager typeBasedCacheManager(RedisTemplate<Object, Object> redisTemplate, ObjectMapper objectMapper) {
        return new TypeBasedCacheManager(redisTemplate, objectMapper);
    }
}
```

使用这个缓存管理器的示例：

```java
Copy@Service
public class TokenService {

    @Autowired
    private TypeBasedCacheManager cacheManager;

    public String getToken(String type, String jsonParams) {
        return cacheManager.getToken(type, jsonParams);
    }
}
```

这个实现有以下特点：

1. 它创建了一个新的`TypeBasedCacheManager`，不会影响现有的其他缓存管理器。
2. 使用类型名来标记不同的缓存。
3. 通过`getToken`方法，可以传入类型和JSON参数来获取缓存的token。
4. 默认使用Redis作为缓存存储，过期时间为30分钟。
5. 如果缓存中没有找到token，会调用`callApiToGetToken`方法（这个方法需要根据实际情况实现）来获取新的token并缓存。

请注意，您需要根据实际情况实现`callApiToGetToken`方法，该方法应该根据不同的类型和参数调用相应的API来获取token。

这个设计提供了灵活性，允许您使用不同的类型和参数来管理缓存，同时不会影响现有的其他缓存管理。如果您需要进一步的定制或有任何问题，请随时告诉我。

