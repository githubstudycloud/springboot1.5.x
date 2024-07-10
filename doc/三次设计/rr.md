非常抱歉造成了困扰。你说得对，我之前的建议确实存在问题。让我们采用一个更简单、更可靠的方法来解决这个问题。我们将直接在 Redis 配置文件中设置固定的值，而不使用环境变量。

以下是修改后的配置：

1. 更新 docker-compose.yml 文件：

```yaml
version: '3.8'

x-redis-common: &redis-common
  image: redis:6.2.6
  command: redis-server /usr/local/etc/redis/redis.conf
  networks:
    - redis-cluster

services:
  redis-node-1:
    <<: *redis-common
    ports:
      - "6379:6379"
      - "16379:16379"
    volumes:
      - ./redis-node-1.conf:/usr/local/etc/redis/redis.conf

  redis-node-2:
    <<: *redis-common
    ports:
      - "6380:6379"
      - "16380:16379"
    volumes:
      - ./redis-node-2.conf:/usr/local/etc/redis/redis.conf

  redis-node-3:
    <<: *redis-common
    ports:
      - "6381:6379"
      - "16381:16379"
    volumes:
      - ./redis-node-3.conf:/usr/local/etc/redis/redis.conf

  redis-node-4:
    <<: *redis-common
    ports:
      - "6382:6379"
      - "16382:16379"
    volumes:
      - ./redis-node-4.conf:/usr/local/etc/redis/redis.conf

  redis-node-5:
    <<: *redis-common
    ports:
      - "6383:6379"
      - "16383:16379"
    volumes:
      - ./redis-node-5.conf:/usr/local/etc/redis/redis.conf

  redis-node-6:
    <<: *redis-common
    ports:
      - "6384:6379"
      - "16384:16379"
    volumes:
      - ./redis-node-6.conf:/usr/local/etc/redis/redis.conf

networks:
  redis-cluster:
    driver: bridge
```

2. 为每个节点创建单独的配置文件。例如，redis-node-1.conf 的内容如下：

```
port 6379
bind 0.0.0.0
protected-mode no
requirepass 123456
masterauth 123456
cluster-enabled yes
cluster-config-file nodes.conf
cluster-node-timeout 5000
appendonly yes
cluster-announce-ip YOUR_HOST_IP
cluster-announce-port 6379
cluster-announce-bus-port 16379
```

对于其他节点（redis-node-2.conf 到 redis-node-6.conf），只需要修改 cluster-announce-port 和 cluster-announce-bus-port 的值，使其与 docker-compose.yml 中的端口映射一致。

例如，redis-node-2.conf 应该是：

```
port 6379
bind 0.0.0.0
protected-mode no
requirepass 123456
masterauth 123456
cluster-enabled yes
cluster-config-file nodes.conf
cluster-node-timeout 5000
appendonly yes
cluster-announce-ip YOUR_HOST_IP
cluster-announce-port 6380
cluster-announce-bus-port 16380
```

请确保将 YOUR_HOST_IP 替换为你的实际主机 IP 地址。

3. 启动集群：

```
docker-compose up -d
```

4. 创建集群：

```
docker exec -it redis-node-1 redis-cli --cluster create YOUR_HOST_IP:6379 YOUR_HOST_IP:6380 YOUR_HOST_IP:6381 YOUR_HOST_IP:6382 YOUR_HOST_IP:6383 YOUR_HOST_IP:6384 --cluster-replicas 0 -a 123456
```

这种方法避免了使用环境变量，直接在每个 Redis 配置文件中设置正确的端口值。这样应该能解决之前遇到的 "参数不能被转换 int" 的问题。

如果还有任何问题，请提供具体的错误信息，我会继续帮助你解决。