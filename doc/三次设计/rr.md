为了确保 Redis 集群正常工作，需要开放 Redis 集群的总线端口（默认是 16379）。此外，如果希望从外部访问 Redis 集群，还需要确保相应的端口映射和防火墙配置正确。

### 更新 `docker-compose.yml`

以下是更新后的 `docker-compose.yml` 文件，包括开放 Redis 集群总线端口和设置外部访问：

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

### 更新 Redis 配置文件

确保每个 Redis 配置文件（例如 `redis-node-1.conf`）中包含以下内容，特别是指定集群总线端口：

```conf
port 6379
cluster-enabled yes
cluster-config-file nodes.conf
cluster-node-timeout 5000
appendonly yes
cluster-announce-ip your_public_ip_or_docker_host_ip
cluster-announce-port 6379
cluster-announce-bus-port 16379
```

### 启动和初始化集群

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
   redis-cli --cluster create 127.0.0.1:6379 127.0.0.1:6380 127.0.0.1:6381 127.0.0.1:6382 127.0.0.1:6383 127.0.0.1:6384 --cluster-replicas 0
   ```

### 确保外部访问

如果需要从外部访问 Redis 集群，请确保防火墙规则允许访问相应的端口（例如 6379 和 16379）。你可能需要在云提供商的控制面板或服务器防火墙中配置这些规则。

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
                Arrays.asList("your_public_ip_or_docker_host_ip:6379", "your_public_ip_or_docker_host_ip:6380", "your_public_ip_or_docker_host_ip:6381",
                              "your_public_ip_or_docker_host_ip:6382", "your_public_ip_or_docker_host_ip:6383", "your_public_ip_or_docker_host_ip:6384")
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

确保在 `redisClusterConfiguration` 中使用你的公共 IP 或 Docker 主机 IP。这样可以确保你的应用能够正确连接到 Redis 集群。