了解了你的需求后，如果你从外部 Java 应用程序访问 Redis 集群，应该使用宿主机的 IP 地址和端口，而不是 Docker 容器的内部名称。以下是一个完整的解决方案，确保 Redis 集群节点和总线端口都正确映射，并且可以从外部访问。

### 更新的 `docker-compose.yml`

```yaml
version: '3.8'

networks:
  redis-cluster-net:
    driver: bridge

services:
  redis-node-1:
    image: redis:6.2.6
    container_name: redis-node-1
    command: ["redis-server", "/usr/local/etc/redis/redis.conf"]
    ports:
      - "6379:6379"
      - "16379:16379"
    volumes:
      - ./redis-node-1.conf:/usr/local/etc/redis/redis.conf
    networks:
      - redis-cluster-net

  redis-node-2:
    image: redis:6.2.6
    container_name: redis-node-2
    command: ["redis-server", "/usr/local/etc/redis/redis.conf"]
    ports:
      - "6380:6379"
      - "16380:16380"
    volumes:
      - ./redis-node-2.conf:/usr/local/etc/redis/redis.conf
    networks:
      - redis-cluster-net

  redis-node-3:
    image: redis:6.2.6
    container_name: redis-node-3
    command: ["redis-server", "/usr/local/etc/redis/redis.conf"]
    ports:
      - "6381:6379"
      - "16381:16381"
    volumes:
      - ./redis-node-3.conf:/usr/local/etc/redis/redis.conf
    networks:
      - redis-cluster-net

  redis-node-4:
    image: redis:6.2.6
    container_name: redis-node-4
    command: ["redis-server", "/usr/local/etc/redis/redis.conf"]
    ports:
      - "6382:6379"
      - "16382:16382"
    volumes:
      - ./redis-node-4.conf:/usr/local/etc/redis/redis.conf
    networks:
      - redis-cluster-net

  redis-node-5:
    image: redis:6.2.6
    container_name: redis-node-5
    command: ["redis-server", "/usr/local/etc/redis/redis.conf"]
    ports:
      - "6383:6379"
      - "16383:16383"
    volumes:
      - ./redis-node-5.conf:/usr/local/etc/redis/redis.conf
    networks:
      - redis-cluster-net

  redis-node-6:
    image: redis:6.2.6
    container_name: redis-node-6
    command: ["redis-server", "/usr/local/etc/redis/redis.conf"]
    ports:
      - "6384:6379"
      - "16384:16384"
    volumes:
      - ./redis-node-6.conf:/usr/local/etc/redis/redis.conf
    networks:
      - redis-cluster-net
```

### Redis 配置文件

确保每个 Redis 配置文件（例如 `redis-node-1.conf`）中包含以下内容：

```conf
port 6379
cluster-enabled yes
cluster-config-file nodes.conf
cluster-node-timeout 5000
appendonly yes
cluster-announce-ip your_host_ip
cluster-announce-port 6379
cluster-announce-bus-port 16379
```

`your_host_ip` 应替换为实际的宿主机 IP 地址。

### 创建和启动集群

1. **启动 Redis 容器**：

   ```sh
   docker-compose up -d
   ```

2. **等待容器启动**：
   确保所有 Redis 容器都启动并且处于运行状态。可以使用 `docker ps` 命令查看容器状态。

3. **初始化集群**：
   进入其中一个 Redis 容器，并运行集群配置命令：

   ```sh
   docker exec -it redis-node-1 bash
   ```

   在容器内运行以下命令配置 Redis 集群：

   ```sh
   redis-cli --cluster create your_host_ip:6379 your_host_ip:6380 your_host_ip:6381 your_host_ip:6382 your_host_ip:6383 your_host_ip:6384 --cluster-replicas 0
   ```

### Java 连接配置

更新你的 Spring Boot 配置类来连接 Redis 集群：

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Arrays;

@Configuration
public class RedisClusterConfig {

    @Bean
    public JedisPoolConfig jedisPoolConfig() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(50);
        poolConfig.setMaxIdle(30);
        poolConfig.setMinIdle(10);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        return poolConfig;
    }

    @Bean
    public RedisClusterConfiguration redisClusterConfiguration() {
        RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(
                Arrays.asList(
                        "your_host_ip:6379",
                        "your_host_ip:6380",
                        "your_host_ip:6381",
                        "your_host_ip:6382",
                        "your_host_ip:6383",
                        "your_host_ip:6384"
                )
        );
        clusterConfig.setMaxRedirects(10);
        return clusterConfig;
    }

    @Bean
    public JedisConnectionFactory jedisConnectionFactory(JedisPoolConfig jedisPoolConfig,
                                                         RedisClusterConfiguration redisClusterConfiguration) {
        return new JedisConnectionFactory(redisClusterConfiguration, jedisPoolConfig);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(JedisConnectionFactory jedisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(jedisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}
```

### 总结

通过在 Redis 配置文件中使用宿主机 IP 地址，并确保端口映射正确，你可以确保 Redis 集群中的节点可以互相通信，同时外部的 Java 应用程序可以通过宿主机的 IP 地址和端口访问 Redis 集群。这样配置后，你的 Java 应用程序将能够正确连接到 Redis 集群并进行操作。