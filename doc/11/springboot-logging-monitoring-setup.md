# Spring Boot 3.2.x 日志和监控系统设置指南

## 1. 全局日志记录框架设置

Spring Boot 3.2.x 默认使用 Logback 作为日志实现。我们可以通过以下步骤来配置：

1. 在 `src/main/resources` 目录下创建 `logback-spring.xml` 文件：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    <include resource="org/springframework/boot/logging/logback/console-appender.xml"/>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>

    <logger name="com.yourpackage" level="DEBUG"/>
</configuration>
```

2. 在 `application.properties` 或 `application.yml` 中配置日志级别：

```yaml
logging:
  level:
    root: INFO
    com.yourpackage: DEBUG
```

## 2. 日志收集和可视化

为了收集和可视化日志，我们可以使用 ELK 栈（Elasticsearch, Logstash, Kibana）或类似的工具。这里我们以 ELK 为例：

1. 添加 Logstash 依赖到 `pom.xml`：

```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.2</version>
</dependency>
```

2. 更新 `logback-spring.xml` 以支持 Logstash：

```xml
<configuration>
    <!-- ... 之前的配置 ... -->

    <appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
        <destination>localhost:5000</destination>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="LOGSTASH"/>
    </root>
</configuration>
```

3. 配置 Logstash 以接收日志并发送到 Elasticsearch。

4. 使用 Kibana 来可视化 Elasticsearch 中的日志数据。

## 3. 业务流程监控通知

对于业务流程监控，我们可以使用 Spring Boot Actuator 结合 Prometheus 和 Grafana：

1. 添加 Actuator 和 Micrometer 依赖到 `pom.xml`：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

2. 在 `application.yml` 中配置 Actuator：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    health:
      show-details: always
```

3. 创建自定义指标：

```java
@Component
public class CustomMetrics {
    private final MeterRegistry meterRegistry;

    public CustomMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordBusinessProcessDuration(String processName, long duration) {
        meterRegistry.timer("business.process.duration", "process", processName)
                     .record(duration, TimeUnit.MILLISECONDS);
    }
}
```

4. 在业务代码中使用自定义指标：

```java
@Service
public class BusinessService {
    private final CustomMetrics customMetrics;

    public BusinessService(CustomMetrics customMetrics) {
        this.customMetrics = customMetrics;
    }

    public void performBusinessProcess() {
        long startTime = System.currentTimeMillis();
        // 执行业务逻辑
        long duration = System.currentTimeMillis() - startTime;
        customMetrics.recordBusinessProcessDuration("process-name", duration);
    }
}
```

5. 配置 Prometheus 来抓取指标数据。

6. 使用 Grafana 创建仪表板来可视化指标，并设置告警规则。

通过以上步骤，您可以建立一个全面的日志记录和监控系统，包括全局日志配置、日志收集和可视化，以及业务流程监控和通知。
