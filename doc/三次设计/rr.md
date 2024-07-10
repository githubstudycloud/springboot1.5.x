好的，以下是一个示例的 `docker-compose.yml` 文件，用于快速搭建一个标准的 Redis 集群。这个文件配置了 6 个 Redis 节点，并将它们连接到一个 overlay 网络中。请确保你已经安装了 Docker 和 Docker Compose。

### `docker-compose.yml`

```yaml
version: '3.8'

services:
  redis-node-1:
    image: redis:6.2.6
    container_name: redis-node-1
    command: ["redis-server", "/usr/local/etc/redis/redis.conf"]
    ports:
      - "6379:6379"
    volumes:
      - ./redis-node-1.conf:/usr/local/etc/redis/redis.conf
    networks:
      - redis-cluster

  redis-node-2:
    image: redis:6.2.6
    container_name: redis-node-2
    command: ["redis-server", "/usr/local/etc/redis/redis.conf"]
    ports:
      - "6380:6379"
    volumes:
      - ./redis-node-2.conf:/usr/local/etc/redis/redis.conf
    networks:
      - redis-cluster

  redis-node-3:
    image: redis:6.2.6
    container_name: redis-node-3
    command: ["redis-server", "/usr/local/etc/redis/redis.conf"]
    ports:
      - "6381:6379"
    volumes:
      - ./redis-node-3.conf:/usr/local/etc/redis/redis.conf
    networks:
      - redis-cluster

  redis-node-4:
    image: redis:6.2.6
    container_name: redis-node-4
    command: ["redis-server", "/usr/local/etc/redis/redis.conf"]
    ports:
      - "6382:6379"
    volumes:
      - ./redis-node-4.conf:/usr/local/etc/redis/redis.conf
    networks:
      - redis-cluster

  redis-node-5:
    image: redis:6.2.6
    container_name: redis-node-5
    command: ["redis-server", "/usr/local/etc/redis/redis.conf"]
    ports:
      - "6383:6379"
    volumes:
      - ./redis-node-5.conf:/usr/local/etc/redis/redis.conf
    networks:
      - redis-cluster

  redis-node-6:
    image: redis:6.2.6
    container_name: redis-node-6
    command: ["redis-server", "/usr/local/etc/redis/redis.conf"]
    ports:
      - "6384:6379"
    volumes:
      - ./redis-node-6.conf:/usr/local/etc/redis/redis.conf
    networks:
      - redis-cluster

networks:
  redis-cluster:
    driver: bridge
```

### Redis 配置文件 (`redis-node-1.conf` 到 `redis-node-6.conf`)

为每个节点创建一个配置文件，例如 `redis-node-1.conf`：

```conf
port 6379
cluster-enabled yes
cluster-config-file nodes.conf
cluster-node-timeout 5000
appendonly yes
```

为每个节点创建相应的配置文件 `redis-node-2.conf` 到 `redis-node-6.conf`，内容相同。

### 创建和启动集群

1. **启动 Redis 容器**：

   ```sh
   docker-compose up -d
   ```

2. **配置集群**：

   启动容器后，进入其中一个 Redis 容器，并运行集群配置命令：

   ```sh
   docker exec -it redis-node-1 bash
   ```

   在容器内运行以下命令配置 Redis 集群：

   ```sh
   redis-cli --cluster create 127.0.0.1:6379 127.0.0.1:6380 127.0.0.1:6381 127.0.0.1:6382 127.0.0.1:6383 127.0.0.1:6384 --cluster-replicas 1
   ```

这将配置一个具有 3 个主节点和 3 个从节点的 Redis 集群。

### 连接 Redis 集群

在你的应用配置中，使用 `127.0.0.1:6379`, `127.0.0.1:6380`, `127.0.0.1:6381`, `127.0.0.1:6382`, `127.0.0.1:6383`, `127.0.0.1:6384` 作为 Redis 集群节点的地址。

### 设置密码（可选）

如果需要设置密码，可以在每个 `redis.conf` 文件中添加以下行：

```conf
requirepass your_redis_password
```

并在创建集群时使用 `redis-cli` 的 `-a` 选项传递密码，例如：

```sh
redis-cli --cluster create 127.0.0.1:6379 127.0.0.1:6380 127.0.0.1:6381 127.0.0.1:6382 127.0.0.1:6383 127.0.0.1:6384 --cluster-replicas 1 -a your_redis_password
```

确保在应用程序中配置连接时也使用相应的密码。