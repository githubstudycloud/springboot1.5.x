非常好的想法!使用外部IP和端口串联集群,并在docker-compose中设置集群参数,可以确保配置文件的一致性,同时简化管理。这种方法可以让你更灵活地管理集群配置。以下是实现这一方案的步骤:

1. 修改docker-compose.yml文件:

```yaml
version: '3.8'

x-redis-common: &redis-common
  image: redis:6.2.6
  command: redis-server /usr/local/etc/redis/redis.conf
  volumes:
    - ./redis.conf:/usr/local/etc/redis/redis.conf
  networks:
    - redis-cluster

services:
  redis-node-1:
    <<: *redis-common
    ports:
      - "6379:6379"
      - "16379:16379"
    environment:
      - REDIS_PORT=6379
      - REDIS_CLUSTER_ANNOUNCE_PORT=6379
      - REDIS_CLUSTER_ANNOUNCE_BUS_PORT=16379

  redis-node-2:
    <<: *redis-common
    ports:
      - "6380:6379"
      - "16380:16379"
    environment:
      - REDIS_PORT=6379
      - REDIS_CLUSTER_ANNOUNCE_PORT=6380
      - REDIS_CLUSTER_ANNOUNCE_BUS_PORT=16380

  redis-node-3:
    <<: *redis-common
    ports:
      - "6381:6379"
      - "16381:16379"
    environment:
      - REDIS_PORT=6379
      - REDIS_CLUSTER_ANNOUNCE_PORT=6381
      - REDIS_CLUSTER_ANNOUNCE_BUS_PORT=16381

  redis-node-4:
    <<: *redis-common
    ports:
      - "6382:6379"
      - "16382:16379"
    environment:
      - REDIS_PORT=6379
      - REDIS_CLUSTER_ANNOUNCE_PORT=6382
      - REDIS_CLUSTER_ANNOUNCE_BUS_PORT=16382

  redis-node-5:
    <<: *redis-common
    ports:
      - "6383:6379"
      - "16383:16379"
    environment:
      - REDIS_PORT=6379
      - REDIS_CLUSTER_ANNOUNCE_PORT=6383
      - REDIS_CLUSTER_ANNOUNCE_BUS_PORT=16383

  redis-node-6:
    <<: *redis-common
    ports:
      - "6384:6379"
      - "16384:16379"
    environment:
      - REDIS_PORT=6379
      - REDIS_CLUSTER_ANNOUNCE_PORT=6384
      - REDIS_CLUSTER_ANNOUNCE_BUS_PORT=16384

networks:
  redis-cluster:
    driver: bridge
```

2. 创建一个通用的redis.conf文件:

```
port ${REDIS_PORT}
bind 0.0.0.0
protected-mode no
requirepass 123456
masterauth 123456
cluster-enabled yes
cluster-config-file nodes.conf
cluster-node-timeout 5000
appendonly yes
cluster-announce-ip YOUR_HOST_IP
cluster-announce-port ${REDIS_CLUSTER_ANNOUNCE_PORT}
cluster-announce-bus-port ${REDIS_CLUSTER_ANNOUNCE_BUS_PORT}
```

请将YOUR_HOST_IP替换为你的实际主机IP。

3. 启动集群:

```
docker-compose up -d
```

4. 创建集群:

```
docker exec -it redis-node-1 redis-cli --cluster create YOUR_HOST_IP:6379 YOUR_HOST_IP:6380 YOUR_HOST_IP:6381 YOUR_HOST_IP:6382 YOUR_HOST_IP:6383 YOUR_HOST_IP:6384 --cluster-replicas 0 -a 123456
```

这种方法的优点是:

1. 配置文件保持一致,减少错误。
2. 通过环境变量动态设置端口,更加灵活。
3. 使用外部IP和端口,方便外部访问和管理。
4. 通过docker-compose简化了管理过程。

如果仍然遇到问题,请检查:

1. 确保YOUR_HOST_IP正确设置为你的实际主机IP。
2. 检查所有端口是否正确映射和开放。
3. 查看docker容器日志以获取更多信息:
   ```
   docker-compose logs
   ```

4. 进入容器检查Redis是否正常运行:
   ```
   docker exec -it redis-node-1 redis-cli -a 123456 ping
   ```

如果还有任何问题,请提供具体的错误信息,我会进一步协助你解决。