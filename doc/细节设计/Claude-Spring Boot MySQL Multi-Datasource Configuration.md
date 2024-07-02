## Prompt:
undefined

## Response:
undefined

## Prompt:
springboot配置mysql多数据源，分业务库和公共库，业务库多个数据库，公共库一个数据库，使用dbcp和mybatis，数据库版本mysql5.7

## Response:
理解了您的需求,我会为您设计一个基于Spring Boot 1.5.22的多数据源配置方案,使用DBCP连接池和MyBatis ORM框架,连接MySQL 5.7数据库。这个方案将包含一个公共库和多个业务库。以下是具体的实现步骤和代码示例:

1. 首先,添加必要的依赖到pom.xml文件:

```xml
Copy<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-jdbc</artifactId>
    </dependency>
    <dependency>
        <groupId>org.mybatis.spring.boot</groupId>
        <artifactId>mybatis-spring-boot-starter</artifactId>
        <version>1.3.2</version>
    </dependency>
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>5.1.47</version>
    </dependency>
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-dbcp2</artifactId>
        <version>2.5.0</version>
    </dependency>
</dependencies>
```

2. 在application.properties文件中配置数据源:

```properties
Copy# 公共库数据源配置
common.datasource.url=jdbc:mysql://localhost:3306/common_db?useSSL=false
common.datasource.username=root
common.datasource.password=password
common.datasource.driver-class-name=com.mysql.jdbc.Driver

# 业务库1数据源配置
business1.datasource.url=jdbc:mysql://localhost:3306/business1_db?useSSL=false
business1.datasource.username=root
business1.datasource.password=password
business1.datasource.driver-class-name=com.mysql.jdbc.Driver

# 业务库2数据源配置
business2.datasource.url=jdbc:mysql://localhost:3306/business2_db?useSSL=false
business2.datasource.username=root
business2.datasource.password=password
business2.datasource.driver-class-name=com.mysql.jdbc.Driver

# DBCP连接池配置
dbcp.initial-size=5
dbcp.max-total=20
dbcp.max-idle=10
dbcp.min-idle=5
dbcp.max-wait-millis=30000
```

3. 创建一个通用的数据源配置类:

```java
Copyimport org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "dbcp")
    public BasicDataSource dbcpProperties() {
        return new BasicDataSource();
    }

    @Bean
    public DataSource createDataSource(BasicDataSource properties) {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName(properties.getDriverClassName());
        dataSource.setUrl(properties.getUrl());
        dataSource.setUsername(properties.getUsername());
        dataSource.setPassword(properties.getPassword());
        dataSource.setInitialSize(properties.getInitialSize());
        dataSource.setMaxTotal(properties.getMaxTotal());
        dataSource.setMaxIdle(properties.getMaxIdle());
        dataSource.setMinIdle(properties.getMinIdle());
        dataSource.setMaxWaitMillis(properties.getMaxWaitMillis());
        return dataSource;
    }
}
```

4. 创建公共库的数据源配置:

```java
Copyimport org.apache.commons.dbcp2.BasicDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

@Configuration
@MapperScan(basePackages = "com.example.dao.common", sqlSessionFactoryRef = "commonSqlSessionFactory")
public class CommonDataSourceConfig extends DataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "common.datasource")
    public BasicDataSource commonDataSourceProperties() {
        return new BasicDataSource();
    }

    @Bean
    @Primary
    public DataSource commonDataSource(@Qualifier("commonDataSourceProperties") BasicDataSource properties) {
        return createDataSource(properties);
    }

    @Bean
    @Primary
    public SqlSessionFactory commonSqlSessionFactory(@Qualifier("commonDataSource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:mapper/common/*.xml"));
        return factoryBean.getObject();
    }
}
```

5. 创建业务库1的数据源配置:

```java
Copyimport org.apache.commons.dbcp2.BasicDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

@Configuration
@MapperScan(basePackages = "com.example.dao.business1", sqlSessionFactoryRef = "business1SqlSessionFactory")
public class Business1DataSourceConfig extends DataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "business1.datasource")
    public BasicDataSource business1DataSourceProperties() {
        return new BasicDataSource();
    }

    @Bean
    public DataSource business1DataSource(@Qualifier("business1DataSourceProperties") BasicDataSource properties) {
        return createDataSource(properties);
    }

    @Bean
    public SqlSessionFactory business1SqlSessionFactory(@Qualifier("business1DataSource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:mapper/business1/*.xml"));
        return factoryBean.getObject();
    }
}
```

6. 创建业务库2的数据源配置(类似业务库1的配置):

```java
Copyimport org.apache.commons.dbcp2.BasicDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

@Configuration
@MapperScan(basePackages = "com.example.dao.business2", sqlSessionFactoryRef = "business2SqlSessionFactory")
public class Business2DataSourceConfig extends DataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "business2.datasource")
    public BasicDataSource business2DataSourceProperties() {
        return new BasicDataSource();
    }

    @Bean
    public DataSource business2DataSource(@Qualifier("business2DataSourceProperties") BasicDataSource properties) {
        return createDataSource(properties);
    }

    @Bean
    public SqlSessionFactory business2SqlSessionFactory(@Qualifier("business2DataSource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:mapper/business2/*.xml"));
        return factoryBean.getObject();
    }
}
```

7. 在不同的包中创建对应的Mapper接口和XML文件:

* com.example.dao.common
* com.example.dao.business1
* com.example.dao.business2

8. 在resources目录下创建对应的mapper文件夹:

* resources/mapper/common
* resources/mapper/business1
* resources/mapper/business2

这个配置将创建三个独立的数据源,分别对应公共库和两个业务库。每个数据源都使用DBCP连接池,并配置了独立的MyBatis SqlSessionFactory。通过使用@MapperScan注解,我们可以将不同的Mapper接口映射到对应的数据源。

请注意,这个配置假设您的项目结构和包名是按照示例中的方式组织的。您可能需要根据实际的项目结构进行适当的调整。

此外,为了确保事务的正确性,您可能还需要配置相应的事务管理器。在实际使用中,请确保在正确的数据源上执行相应的操作,以避免跨库操作可能带来的问题。

## Prompt:
业务库能共用一套dao和xml吗，定义一个dbname，自动检索配置文件的业务库配置去配置数据源，并且可以通过dbname根据业务不同使用工具类随时切换数据源

## Response:
非常好的想法!我们可以通过动态数据源和AOP来实现这个需求。这样可以让业务库共用一套DAO和XML,并且能够根据需要动态切换数据源。下面是具体的实现步骤:

1. 首先,创建一个动态数据源类:

```java
Copyimport org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class DynamicDataSource extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
        return DataSourceContextHolder.getDataSourceType();
    }
}
```

2. 创建一个数据源上下文持有者:

```java
Copypublic class DataSourceContextHolder {
    private static final ThreadLocal<String> contextHolder = new ThreadLocal<>();

    public static void setDataSourceType(String dataSourceType) {
        contextHolder.set(dataSourceType);
    }

    public static String getDataSourceType() {
        return contextHolder.get();
    }

    public static void clearDataSourceType() {
        contextHolder.remove();
    }
}
```

3. 创建一个注解,用于标记需要切换数据源的方法:

```java
Copyimport java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TargetDataSource {
    String value();
}
```

4. 创建一个AOP切面来处理数据源切换:

```java
Copyimport org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Order(-1)
@Component
public class DynamicDataSourceAspect {

    @Pointcut("@annotation(com.example.annotation.TargetDataSource)")
    public void dataSourcePointCut() {}

    @Around("dataSourcePointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();

        TargetDataSource ds = method.getAnnotation(TargetDataSource.class);
        if (ds == null) {
            DataSourceContextHolder.setDataSourceType("defaultDataSource");
        } else {
            DataSourceContextHolder.setDataSourceType(ds.value());
        }

        try {
            return point.proceed();
        } finally {
            DataSourceContextHolder.clearDataSourceType();
        }
    }
}
```

5. 修改数据源配置类,使用动态数据源:

```java
Copyimport org.apache.commons.dbcp2.BasicDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@MapperScan(basePackages = "com.example.dao")
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "dbcp")
    public BasicDataSource dbcpProperties() {
        return new BasicDataSource();
    }

    @Bean
    @ConfigurationProperties(prefix = "common.datasource")
    public DataSource commonDataSource() {
        return createDataSource(dbcpProperties());
    }

    @Bean
    @Primary
    public DataSource dynamicDataSource(@Qualifier("commonDataSource") DataSource commonDataSource) {
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("commonDataSource", commonDataSource);

        // 动态添加业务数据源
        addBusinessDataSources(targetDataSources);

        DynamicDataSource dataSource = new DynamicDataSource();
        dataSource.setDefaultTargetDataSource(commonDataSource);
        dataSource.setTargetDataSources(targetDataSources);
        return dataSource;
    }

    private void addBusinessDataSources(Map<Object, Object> targetDataSources) {
        // 从配置文件中读取所有业务数据源配置
        // 这里假设业务数据源的配置前缀都是 "business.datasource."
        // 实际实现时,您需要根据您的配置文件结构来调整这个逻辑
        Environment env = applicationContext.getEnvironment();
        String[] propertyNames = env.getPropertyNames();
        for (String propertyName : propertyNames) {
            if (propertyName.startsWith("business.datasource.") && propertyName.endsWith(".url")) {
                String dbName = propertyName.replace("business.datasource.", "").replace(".url", "");
                BasicDataSource dataSourceProperties = new BasicDataSource();
                dataSourceProperties.setUrl(env.getProperty("business.datasource." + dbName + ".url"));
                dataSourceProperties.setUsername(env.getProperty("business.datasource." + dbName + ".username"));
                dataSourceProperties.setPassword(env.getProperty("business.datasource." + dbName + ".password"));
                dataSourceProperties.setDriverClassName(env.getProperty("business.datasource." + dbName + ".driver-class-name"));
                
                DataSource businessDataSource = createDataSource(dataSourceProperties);
                targetDataSources.put(dbName, businessDataSource);
            }
        }
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(@Qualifier("dynamicDataSource") DataSource dynamicDataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dynamicDataSource);
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:mapper/*.xml"));
        return factoryBean.getObject();
    }

    private DataSource createDataSource(BasicDataSource properties) {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName(properties.getDriverClassName());
        dataSource.setUrl(properties.getUrl());
        dataSource.setUsername(properties.getUsername());
        dataSource.setPassword(properties.getPassword());
        dataSource.setInitialSize(properties.getInitialSize());
        dataSource.setMaxTotal(properties.getMaxTotal());
        dataSource.setMaxIdle(properties.getMaxIdle());
        dataSource.setMinIdle(properties.getMinIdle());
        dataSource.setMaxWaitMillis(properties.getMaxWaitMillis());
        return dataSource;
    }
}
```

6. 创建一个工具类来切换数据源:

```java
Copypublic class DataSourceSwitcher {
    public static void use(String dataSource) {
        DataSourceContextHolder.setDataSourceType(dataSource);
    }

    public static void clear() {
        DataSourceContextHolder.clearDataSourceType();
    }
}
```

7. 在application.properties中配置数据源:

```properties
Copy# 公共数据源配置
common.datasource.url=jdbc:mysql://localhost:3306/common_db?useSSL=false
common.datasource.username=root
common.datasource.password=password
common.datasource.driver-class-name=com.mysql.jdbc.Driver

# 业务数据源配置
business.datasource.business1.url=jdbc:mysql://localhost:3306/business1_db?useSSL=false
business.datasource.business1.username=root
business.datasource.business1.password=password
business.datasource.business1.driver-class-name=com.mysql.jdbc.Driver

business.datasource.business2.url=jdbc:mysql://localhost:3306/business2_db?useSSL=false
business.datasource.business2.username=root
business.datasource.business2.password=password
business.datasource.business2.driver-class-name=com.mysql.jdbc.Driver

# DBCP连接池配置
dbcp.initial-size=5
dbcp.max-total=20
dbcp.max-idle=10
dbcp.min-idle=5
dbcp.max-wait-millis=30000
```

8. 使用示例:

```java
Copy@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;

    @TargetDataSource("business1")
    public User getUserFromBusiness1(Long id) {
        return userMapper.getUserById(id);
    }

    @TargetDataSource("business2")
    public User getUserFromBusiness2(Long id) {
        return userMapper.getUserById(id);
    }

    public User getUserWithManualSwitch(Long id, String dbName) {
        try {
            DataSourceSwitcher.use(dbName);
            return userMapper.getUserById(id);
        } finally {
            DataSourceSwitcher.clear();
        }
    }
}
```

这种实现方式允许业务库共用一套DAO和XML,并且可以通过注解或者工具类方法来动态切换数据源。系统会自动从配置文件中读取所有的业务数据源配置,并创建相应的数据源。

请注意,这种动态数据源切换的方式可能会影响到事务管理。如果您的应用涉及复杂的事务操作,可能需要额外的事务管理配置。

此外,这种实现假设所有的业务库都有相同的表结构。如果不同的业务库有不同的表结构,您可能需要为不同的业务库创建不同的DAO接口和XML映射文件。

最后,请确保在实际使用中正确处理异常,并在操作完成后清理数据源上下文,以避免潜在的数据源泄漏问题。

