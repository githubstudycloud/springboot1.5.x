## Prompt:
undefined

## Response:
undefined

## Prompt:
快速搭建一个可以连接redis集群和单机的服务，同时写各种工具类和接口测试类

## Response:
好的,我理解你需要搭建一个基于Spring Boot 1.5.22的项目,可以连接Redis集群和单机,同时包含工具类和接口测试类。我会基于这个要求来设计和实现项目。

首先,让我们从项目结构开始:

```
Copysrc
├── main
│   ├── java
│   │   └── com
│   │       └── example
│   │           └── redisproject
│   │               ├── RedisProjectApplication.java
│   │               ├── config
│   │               │   ├── RedisClusterConfig.java
│   │               │   └── RedisSingleConfig.java
│   │               ├── controller
│   │               │   └── RedisTestController.java
│   │               ├── service
│   │               │   ├── RedisClusterService.java
│   │               │   └── RedisSingleService.java
│   │               └── util
│   │                   └── RedisUtil.java
│   └── resources
│       ├── application.properties
│       └── application-cluster.properties
└── test
    └── java
        └── com
            └── example
                └── redisproject
                    ├── RedisClusterTests.java
                    └── RedisSingleTests.java
```

现在,让我们逐个实现这些文件:

1. pom.xml

```xml
Copy<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>1.5.22.RELEASE</version>
        <relativePath/>
    </parent>
    <groupId>com.example</groupId>
    <artifactId>redis-project</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>redis-project</name>
    <description>Redis project for Spring Boot</description>

    <properties>
        <java.version>1.8</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>redis.clients</groupId>
            <artifactId>jedis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```

2. RedisProjectApplication.java

```java
Copypackage com.example.redisproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RedisProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(RedisProjectApplication.class, args);
    }

}
```

3. RedisClusterConfig.java

```java
Copypackage com.example.redisproject.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.List;

@Configuration
@Profile("cluster")
public class RedisClusterConfig {

    @Value("${spring.redis.cluster.nodes}")
    private List<String> clusterNodes;

    @Bean
    public RedisConnectionFactory connectionFactory() {
        return new JedisConnectionFactory(
                new RedisClusterConfiguration(clusterNodes)
        );
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
}
```

4. RedisSingleConfig.java

```java
Copypackage com.example.redisproject.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Profile("!cluster")
public class RedisSingleConfig {

    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private int redisPort;

    @Bean
    public RedisConnectionFactory connectionFactory() {
        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory();
        jedisConnectionFactory.setHostName(redisHost);
        jedisConnectionFactory.setPort(redisPort);
        return jedisConnectionFactory;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
}
```

5. RedisTestController.java

```java
Copypackage com.example.redisproject.controller;

import com.example.redisproject.service.RedisClusterService;
import com.example.redisproject.service.RedisSingleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/redis")
public class RedisTestController {

    @Autowired(required = false)
    private RedisClusterService redisClusterService;

    @Autowired(required = false)
    private RedisSingleService redisSingleService;

    @PostMapping("/set")
    public String setValue(@RequestParam String key, @RequestParam String value) {
        if (redisClusterService != null) {
            redisClusterService.setValue(key, value);
        } else if (redisSingleService != null) {
            redisSingleService.setValue(key, value);
        }
        return "Value set successfully";
    }

    @GetMapping("/get")
    public String getValue(@RequestParam String key) {
        if (redisClusterService != null) {
            return redisClusterService.getValue(key);
        } else if (redisSingleService != null) {
            return redisSingleService.getValue(key);
        }
        return "No Redis service available";
    }
}
```

6. RedisClusterService.java

```java
Copypackage com.example.redisproject.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Profile("cluster")
public class RedisClusterService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void setValue(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public String getValue(String key) {
        return (String) redisTemplate.opsForValue().get(key);
    }
}
```

7. RedisSingleService.java

```java
Copypackage com.example.redisproject.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Profile("!cluster")
public class RedisSingleService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void setValue(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public String getValue(String key) {
        return (String) redisTemplate.opsForValue().get(key);
    }
}
```

8. RedisUtil.java

```java
Copypackage com.example.redisproject.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }
}
```

9. application.properties

```properties
Copyspring.profiles.active=single

# Redis single node configuration
spring.redis.host=localhost
spring.redis.port=6379
```

10. application-cluster.properties

```properties
Copyspring.profiles.active=cluster

# Redis cluster configuration
spring.redis.cluster.nodes=127.0.0.1:7000,127.0.0.1:7001,127.0.0.1:7002,127.0.0.1:7003,127.0.0.1:7004,127.0.0.1:7005
```

11. RedisClusterTests.java

```java
Copypackage com.example.redisproject;

import com.example.redisproject.service.RedisClusterService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("cluster")
public class RedisClusterTests {

    @Autowired
    private RedisClusterService redisClusterService;

    @Test
    public void testSetAndGetValue() {
        String key = "testKey";
        String value = "testValue";

        redisClusterService.setValue(key, value);
        String retrievedValue = redisClusterService.getValue(key);

        assertEquals(value, retrievedValue);
    }
}
```

12. RedisSingleTests.java

```java
Copypackage com.example.redisproject;

import com.example.redisproject.service.RedisSingleService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("single")
public class RedisSingleTests {

    @Autowired
    private RedisSingleService redisSingleService;

    @Test
    public void testSetAndGetValue() {
        String key = "testKey";
        String value = "testValue";

        redisSingleService.setValue(key, value);
        String retrievedValue = redisSingleService.getValue(key);

        assertEquals(value, retrievedValue);
    }
}
```

这个项目结构允许你通过切换 Spring profiles 来在 Redis 单机模式和集群模式之间切换。默认情况下,它使用单机模式。要使用集群模式,你需要在运行应用时指定 `cluster` profile。

例如,要在集群模式下运行应用,你可以使用以下命令:

```
Copyjava -jar your-application.jar --spring.profiles.active=cluster
```

这个项目提供了基本的 Redis 操作,包括设置和获取值。你可以根据需要扩展 `RedisUtil` 类来添加更多的 Redis 操作。

测试类演示了如何在单元测试中使用这些服务。你可以根据需要添加更多的测试用例。

请注意,在运行测试或应用之前,确保你有一个运行中的 Redis 服务器(单机或集群,取决于你使用的配置)。

## Prompt:
集群最好支持ssh隧道

## Response:
理解了，您希望Redis集群支持SSH隧道连接。这是一个很好的安全实践，特别是当Redis集群部署在远程或云环境中时。我们可以修改配置来支持SSH隧道连接。以下是修改后的代码和配置：

1. 首先，我们需要添加SSH隧道的依赖。在`pom.xml`中添加：

```xml
Copy<dependency>
    <groupId>com.jcraft</groupId>
    <artifactId>jsch</artifactId>
    <version>0.1.55</version>
</dependency>
```

2. 然后，我们需要创建一个SSH隧道配置类：

```java
Copypackage com.example.redisproject.config;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("cluster")
public class SshTunnelConfig {

    @Value("${ssh.host}")
    private String sshHost;

    @Value("${ssh.port}")
    private int sshPort;

    @Value("${ssh.user}")
    private String sshUser;

    @Value("${ssh.password}")
    private String sshPassword;

    @Value("${ssh.remote.host}")
    private String remoteHost;

    @Value("${ssh.remote.port}")
    private int remotePort;

    @Value("${ssh.local.port}")
    private int localPort;

    @Bean
    public Session sshSession() throws Exception {
        JSch jsch = new JSch();
        Session session = jsch.getSession(sshUser, sshHost, sshPort);
        session.setPassword(sshPassword);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        session.setPortForwardingL(localPort, remoteHost, remotePort);
        return session;
    }
}
```

3. 修改`RedisClusterConfig.java`以使用SSH隧道：

```java
Copypackage com.example.redisproject.config;

import com.jcraft.jsch.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.List;

@Configuration
@Profile("cluster")
public class RedisClusterConfig {

    @Value("${spring.redis.cluster.nodes}")
    private List<String> clusterNodes;

    @Autowired
    private Session sshSession;

    @Bean
    public RedisConnectionFactory connectionFactory() {
        return new JedisConnectionFactory(
                new RedisClusterConfiguration(clusterNodes)
        );
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
}
```

4. 在`application-cluster.properties`中添加SSH隧道的配置：

```properties
Copyspring.profiles.active=cluster

# Redis cluster configuration
spring.redis.cluster.nodes=localhost:6379,localhost:6380,localhost:6381,localhost:6382,localhost:6383,localhost:6384

# SSH tunnel configuration
ssh.host=your_ssh_host
ssh.port=22
ssh.user=your_ssh_username
ssh.password=your_ssh_password
ssh.remote.host=your_redis_host
ssh.remote.port=6379
ssh.local.port=6379
```

5. 最后，我们需要在应用退出时关闭SSH会话。修改`RedisProjectApplication.java`：

```java
Copypackage com.example.redisproject;

import com.jcraft.jsch.Session;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class RedisProjectApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(RedisProjectApplication.class, args);

        // 添加关闭钩子，在应用退出时关闭SSH会话
        context.registerShutdownHook();
    }
}
```

这些修改允许你通过SSH隧道连接到Redis集群。当使用"cluster" profile运行应用时，它会首先建立SSH连接，然后通过这个连接访问Redis集群。

请注意，这个实现使用了密码认证。在生产环境中，你可能想使用更安全的认证方式，比如SSH密钥。另外，请确保properly处理异常，并在生产环境中不要在属性文件中存储明文密码。

使用这种配置，你可以通过以下命令在集群模式下运行应用：

```
Copyjava -jar your-application.jar --spring.profiles.active=cluster
```

这将启动应用，建立SSH隧道，然后通过隧道连接到Redis集群。

## Prompt:
我是springboot1.5.22，最好兼容

## Response:
非常抱歉我之前没有充分考虑到 Spring Boot 1.5.22 的兼容性问题。让我们调整代码以确保与 Spring Boot 1.5.22 完全兼容。以下是修改后的代码：

1. 首先，`pom.xml` 文件保持不变，但确保 Spring Boot 版本是 1.5.22.RELEASE。
2. `SshTunnelConfig.java` 文件基本保持不变，但我们需要确保它只在集群模式下生效：

```java
Copypackage com.example.redisproject.config;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("cluster")
public class SshTunnelConfig {

    @Value("${ssh.host}")
    private String sshHost;

    @Value("${ssh.port}")
    private int sshPort;

    @Value("${ssh.user}")
    private String sshUser;

    @Value("${ssh.password}")
    private String sshPassword;

    @Value("${ssh.remote.host}")
    private String remoteHost;

    @Value("${ssh.remote.port}")
    private int remotePort;

    @Value("${ssh.local.port}")
    private int localPort;

    @Bean
    public Session sshSession() throws Exception {
        JSch jsch = new JSch();
        Session session = jsch.getSession(sshUser, sshHost, sshPort);
        session.setPassword(sshPassword);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        session.setPortForwardingL(localPort, remoteHost, remotePort);
        return session;
    }
}
```

3. 修改 `RedisClusterConfig.java` 以适应 Spring Boot 1.5.22：

```java
Copypackage com.example.redisproject.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.List;

@Configuration
@Profile("cluster")
public class RedisClusterConfig {

    @Value("${spring.redis.cluster.nodes}")
    private List<String> clusterNodes;

    @Bean
    public RedisConnectionFactory connectionFactory() {
        return new JedisConnectionFactory(
                new RedisClusterConfiguration(clusterNodes)
        );
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
}
```

4. `RedisSingleConfig.java` 文件保持不变。
5. 在 `RedisProjectApplication.java` 中，我们需要手动关闭 SSH 会话：

```java
Copypackage com.example.redisproject;

import com.jcraft.jsch.Session;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class RedisProjectApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(RedisProjectApplication.class, args);

        // 添加关闭钩子，在应用退出时关闭SSH会话
        context.registerShutdownHook();

        // 获取SSH会话并在应用关闭时断开连接
        Session sshSession = context.getBean(Session.class);
        if (sshSession != null && sshSession.isConnected()) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                sshSession.disconnect();
            }));
        }
    }
}
```

6. `application-cluster.properties` 文件保持不变。
7. 其他服务类和控制器类不需要修改。

这些修改应该使得项目与 Spring Boot 1.5.22 完全兼容，同时支持通过 SSH 隧道连接到 Redis 集群。

请注意，在 Spring Boot 1.5.22 中，一些较新的特性可能不可用。例如，`@EnableConfigurationProperties` 注解在这个版本中的行为可能与更新的版本有所不同。如果你在使用过程中遇到任何问题，请告诉我，我会帮你进一步调整代码。

## Prompt:
总结并输出完整代码

## Response:
好的,我来总结并输出完整的代码。这个项目是一个基于Spring Boot 1.5.22的Redis客户端,支持单机模式和集群模式,并且在集群模式下支持SSH隧道连接。

项目结构如下:

```
Copysrc
├── main
│   ├── java
│   │   └── com
│   │       └── example
│   │           └── redisproject
│   │               ├── RedisProjectApplication.java
│   │               ├── config
│   │               │   ├── RedisClusterConfig.java
│   │               │   ├── RedisSingleConfig.java
│   │               │   └── SshTunnelConfig.java
│   │               ├── controller
│   │               │   └── RedisTestController.java
│   │               ├── service
│   │               │   ├── RedisClusterService.java
│   │               │   └── RedisSingleService.java
│   │               └── util
│   │                   └── RedisUtil.java
│   └── resources
│       ├── application.properties
│       └── application-cluster.properties
└── test
    └── java
        └── com
            └── example
                └── redisproject
                    ├── RedisClusterTests.java
                    └── RedisSingleTests.java
```

现在,我会提供每个文件的完整代码:

1. pom.xml

```xml
Copy<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>1.5.22.RELEASE</version>
        <relativePath/>
    </parent>
    <groupId>com.example</groupId>
    <artifactId>redis-project</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>redis-project</name>
    <description>Redis project for Spring Boot</description>

    <properties>
        <java.version>1.8</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>redis.clients</groupId>
            <artifactId>jedis</artifactId>
        </dependency>
        <dependency>
            <groupId>com.jcraft</groupId>
            <artifactId>jsch</artifactId>
            <version>0.1.55</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```

2. RedisProjectApplication.java

```java
Copypackage com.example.redisproject;

import com.jcraft.jsch.Session;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class RedisProjectApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(RedisProjectApplication.class, args);

        // 添加关闭钩子，在应用退出时关闭SSH会话
        context.registerShutdownHook();

        // 获取SSH会话并在应用关闭时断开连接
        Session sshSession = context.getBean(Session.class);
        if (sshSession != null && sshSession.isConnected()) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                sshSession.disconnect();
            }));
        }
    }
}
```

3. RedisClusterConfig.java

```java
Copypackage com.example.redisproject.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.List;

@Configuration
@Profile("cluster")
public class RedisClusterConfig {

    @Value("${spring.redis.cluster.nodes}")
    private List<String> clusterNodes;

    @Bean
    public RedisConnectionFactory connectionFactory() {
        return new JedisConnectionFactory(
                new RedisClusterConfiguration(clusterNodes)
        );
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
}
```

4. RedisSingleConfig.java

```java
Copypackage com.example.redisproject.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Profile("!cluster")
public class RedisSingleConfig {

    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private int redisPort;

    @Bean
    public RedisConnectionFactory connectionFactory() {
        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory();
        jedisConnectionFactory.setHostName(redisHost);
        jedisConnectionFactory.setPort(redisPort);
        return jedisConnectionFactory;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
}
```

5. SshTunnelConfig.java

```java
Copypackage com.example.redisproject.config;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("cluster")
public class SshTunnelConfig {

    @Value("${ssh.host}")
    private String sshHost;

    @Value("${ssh.port}")
    private int sshPort;

    @Value("${ssh.user}")
    private String sshUser;

    @Value("${ssh.password}")
    private String sshPassword;

    @Value("${ssh.remote.host}")
    private String remoteHost;

    @Value("${ssh.remote.port}")
    private int remotePort;

    @Value("${ssh.local.port}")
    private int localPort;

    @Bean
    public Session sshSession() throws Exception {
        JSch jsch = new JSch();
        Session session = jsch.getSession(sshUser, sshHost, sshPort);
        session.setPassword(sshPassword);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        session.setPortForwardingL(localPort, remoteHost, remotePort);
        return session;
    }
}
```

6. RedisTestController.java

```java
Copypackage com.example.redisproject.controller;

import com.example.redisproject.service.RedisClusterService;
import com.example.redisproject.service.RedisSingleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/redis")
public class RedisTestController {

    @Autowired(required = false)
    private RedisClusterService redisClusterService;

    @Autowired(required = false)
    private RedisSingleService redisSingleService;

    @PostMapping("/set")
    public String setValue(@RequestParam String key, @RequestParam String value) {
        if (redisClusterService != null) {
            redisClusterService.setValue(key, value);
        } else if (redisSingleService != null) {
            redisSingleService.setValue(key, value);
        }
        return "Value set successfully";
    }

    @GetMapping("/get")
    public String getValue(@RequestParam String key) {
        if (redisClusterService != null) {
            return redisClusterService.getValue(key);
        } else if (redisSingleService != null) {
            return redisSingleService.getValue(key);
        }
        return "No Redis service available";
    }
}
```

7. RedisClusterService.java

```java
Copypackage com.example.redisproject.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Profile("cluster")
public class RedisClusterService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void setValue(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public String getValue(String key) {
        return (String) redisTemplate.opsForValue().get(key);
    }
}
```

8. RedisSingleService.java

```java
Copypackage com.example.redisproject.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Profile("!cluster")
public class RedisSingleService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void setValue(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public String getValue(String key) {
        return (String) redisTemplate.opsForValue().get(key);
    }
}
```

9. RedisUtil.java

```java
Copypackage com.example.redisproject.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }
}
```

10. application.properties

```properties
Copyspring.profiles.active=single

# Redis single node configuration
spring.redis.host=localhost
spring.redis.port=6379
```

11. application-cluster.properties

```properties
Copyspring.profiles.active=cluster

# Redis cluster configuration
spring.redis.cluster.nodes=localhost:6379,localhost:6380,localhost:6381,localhost:6382,localhost:6383,localhost:6384

# SSH tunnel configuration
ssh.host=your_ssh_host
ssh.port=22
ssh.user=your_ssh_username
ssh.password=your_ssh_password
ssh.remote.host=your_redis_host
ssh.remote.port=6379
ssh.local.port=6379
```

12. RedisClusterTests.java

```java
Copypackage com.example.redisproject;

import com.example.redisproject.service.RedisClusterService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("cluster")
public class RedisClusterTests {

    @Autowired
    private RedisClusterService redisClusterService;

    @Test
    public void testSetAndGetValue() {
        String key = "testKey";
        String value = "testValue";

        redisClusterService.setValue(key, value);
        String retrievedValue = redisClusterService.getValue(key);

        assertEquals(value, retrievedValue);
    }
}
```

13. RedisSingleTests.java

```java
Copypackage com.example.redisproject;

import com.example.redisproject.service.RedisSingleService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("single")
public class RedisSingleTests {

    @Autowired
    private RedisSingleService redisSingleService;

    @Test
    public void testSetAndGetValue() {
        String key = "testKey";
        String value = "testValue";

        redisSingleService.setValue(key, value);
        String retrievedValue = redisSingleService.getValue(key);

        assertEquals(value, retrievedValue);
    }
}
```

这个项目提供了以下功能:

1. 支持Redis单机模式和集群模式
2. 在集群模式下支持SSH隧道连接
3. 提供了基本的Redis操作(设置和获取值)
4. 包含了单元测试 5

