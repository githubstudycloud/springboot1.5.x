感谢您的反馈。这个问题通常是由于 Eureka 服务器之间无法正确通信造成的。让我们进行一些调整来解决这个问题：

1. 修改 application.yml：

```yaml
spring:
  application:
    name: eureka-server
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:default}

server:
  port: ${SERVER_PORT:8761}

eureka:
  instance:
    hostname: ${EUREKA_INSTANCE_HOSTNAME:localhost}
    preferIpAddress: true
    instanceId: ${spring.application.name}:${spring.application.instance_id:${random.value}}
  client:
    registerWithEureka: true
    fetchRegistry: true
    serviceUrl:
      defaultZone: ${EUREKA_SERVER_URL:http://localhost:8761/eureka/}
  server:
    enable-self-preservation: false
    eviction-interval-timer-in-ms: 5000
    wait-time-in-ms-when-sync-empty: 0

management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    health:
      show-details: always

logging:
  level:
    com.netflix.eureka: DEBUG
    com.netflix.discovery: DEBUG
```

2. 修改 docker-compose.yml：

```yaml
version: '3'
services:
  eureka-server1:
    build: .
    ports:
      - "8761:8761"
    environment:
      - SPRING_PROFILES_ACTIVE=peer1
      - EUREKA_INSTANCE_HOSTNAME=eureka-server1
      - SERVER_PORT=8761
      - EUREKA_SERVER_URL=http://eureka-server2:8762/eureka/,http://eureka-server3:8763/eureka/
    networks:
      - eureka-network

  eureka-server2:
    build: .
    ports:
      - "8762:8762"
    environment:
      - SPRING_PROFILES_ACTIVE=peer2
      - EUREKA_INSTANCE_HOSTNAME=eureka-server2
      - SERVER_PORT=8762
      - EUREKA_SERVER_URL=http://eureka-server1:8761/eureka/,http://eureka-server3:8763/eureka/
    networks:
      - eureka-network

  eureka-server3:
    build: .
    ports:
      - "8763:8763"
    environment:
      - SPRING_PROFILES_ACTIVE=peer3
      - EUREKA_INSTANCE_HOSTNAME=eureka-server3
      - SERVER_PORT=8763
      - EUREKA_SERVER_URL=http://eureka-server1:8761/eureka/,http://eureka-server2:8762/eureka/
    networks:
      - eureka-network

networks:
  eureka-network:
    driver: bridge
```

3. 修改 Dockerfile：

```dockerfile
FROM openjdk:8-jdk-alpine
VOLUME /tmp
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -jar /app.jar"]
```

4. 确保 EurekaServerApplication.java 保持不变：

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

这些修改主要做了以下几点：

1. 在 application.yml 中，我们添加了 `instanceId` 配置，以确保每个实例有唯一的 ID。
2. 设置 `wait-time-in-ms-when-sync-empty: 0`，这可以加快服务启动时的同步过程。
3. 在 docker-compose.yml 中，我们显式定义了一个网络 `eureka-network`，并将所有服务连接到这个网络。
4. 在 Dockerfile 中，我们稍微修改了启动命令，以确保 Java 进程正确处理 SIGTERM 信号。

现在，按以下步骤重新部署你的服务：

1. 停止并删除现有的容器和网络：

```bash
docker-compose down -v
```

2. 重新构建并启动服务：

```bash
docker-compose up --build
```

3. 等待几分钟，让服务完全启动并相互发现。

4. 检查日志以查看是否有任何错误：

```bash
docker-compose logs
```

如果一切正常，你应该能在 Eureka dashboard 上看到所有三个服务器都标记为 "available-replicas"。

如果问题仍然存在，可能需要进一步调查网络连接。你可以尝试进入容器并ping其他服务：

```bash
docker exec -it <container_name> /bin/sh
ping eureka-server1
ping eureka-server2
ping eureka-server3
```

如果 ping 不通，可能是 Docker 网络配置的问题。在这种情况下，你可能需要检查 Docker 网络设置或考虑使用 host 网络模式。

如果还有任何问题，请提供具体的错误信息或日志输出，我会继续协助你解决问题。