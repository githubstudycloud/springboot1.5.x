### 监控服务文件目录结构

以下是监控服务的标准文件目录结构示例。监控服务将提供系统监控、告警功能和Spring Boot Admin功能。

### 目录结构

```
DimPlatformMon
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── study
│   │   │           └── monitor
│   │   │               ├── config
│   │   │               │   └── MonitorConfig.java
│   │   │               ├── controller
│   │   │               │   └── HealthController.java
│   │   │               ├── service
│   │   │               │   └── MonitorService.java
│   │   │               └── MonitorApplication.java
│   │   ├── resources
│   │   │   ├── application.yml
│   │   │   └── logback-spring.xml
│   │   └── webapp
│   │       └── WEB-INF
│   │           └── web.xml
│   └── test
│       ├── java
│       │   └── com
│       │       └── study
│       │           └── monitor
│       │               └── MonitorApplicationTests.java
│       └── resources
├── pom.xml
└── README.md
```

### 主要文件说明

1. **MonitorApplication.java**：主应用程序启动类。
2. **MonitorConfig.java**：Spring Boot Admin配置类。
3. **HealthController.java**：健康检查控制器。
4. **MonitorService.java**：监控服务逻辑。
5. **application.yml**：Spring Boot配置文件。
6. **logback-spring.xml**：Logback日志配置文件。
