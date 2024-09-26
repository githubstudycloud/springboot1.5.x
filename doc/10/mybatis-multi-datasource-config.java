// application.yml
spring:
  datasource:
    common:
      url: jdbc:mysql://common-db-host:3306/common_db
      username: common_user
      password: common_password
      driver-class-name: com.mysql.cj.jdbc.Driver
    business:
      default:
        url: jdbc:mysql://default-business-db-host:3306/default_business_db
        username: default_business_user
        password: default_business_password
        driver-class-name: com.mysql.cj.jdbc.Driver
      additional:
        - name: business1
          url: jdbc:mysql://business-db1-host:3306/business_db1
          username: business_user1
          password: business_password1
        - name: business2
          url: jdbc:mysql://business-db2-host:3306/business_db2
          username: business_user2
          password: business_password2
        # ... 配置到 business6

mybatis:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.yourcompany.domain

// DataSourceConfig.java
@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.common")
    public DataSource commonDataSource() {
        return DataSourceBuilder.create().type(BasicDataSource.class).build();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.business.default")
    public DataSource defaultBusinessDataSource() {
        return DataSourceBuilder.create().type(BasicDataSource.class).build();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.business.additional")
    public List<DataSourceProperties> additionalBusinessDataSourceProperties() {
        return new ArrayList<>();
    }

    @Bean
    public Map<String, DataSource> additionalBusinessDataSources(List<DataSourceProperties> properties) {
        Map<String, DataSource> dataSources = new HashMap<>();
        for (DataSourceProperties prop : properties) {
            DataSource ds = DataSourceBuilder.create()
                .type(BasicDataSource.class)
                .url(prop.getUrl())
                .username(prop.getUsername())
                .password(prop.getPassword())
                .driverClassName(prop.getDriverClassName())
                .build();
            dataSources.put(prop.getName(), ds);
        }
        return dataSources;
    }

    @Bean
    @Primary
    public DataSource routingDataSource(
            @Qualifier("commonDataSource") DataSource commonDataSource,
            @Qualifier("defaultBusinessDataSource") DataSource defaultBusinessDataSource,
            @Qualifier("additionalBusinessDataSources") Map<String, DataSource> additionalBusinessDataSources) {
        Map<Object, Object> targetDataSources = new HashMap<>(additionalBusinessDataSources);
        targetDataSources.put("common", commonDataSource);
        targetDataSources.put("business", defaultBusinessDataSource);

        RoutingDataSource routingDataSource = new RoutingDataSource();
        routingDataSource.setDefaultTargetDataSource(defaultBusinessDataSource);
        routingDataSource.setTargetDataSources(targetDataSources);
        return routingDataSource;
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(@Qualifier("routingDataSource") DataSource routingDataSource) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(routingDataSource);
        sessionFactory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:mapper/**/*.xml"));
        return sessionFactory.getObject();
    }
}

// RoutingDataSource.java
public class RoutingDataSource extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
        String dataSourceType = DataSourceContextHolder.getDataSourceType();
        if ("common".equals(dataSourceType)) {
            return "common";
        } else {
            String businessDataSourceName = DataSourceContextHolder.getBusinessDataSourceName();
            return businessDataSourceName != null ? businessDataSourceName : "business";
        }
    }
}

// DataSourceContextHolder.java
public class DataSourceContextHolder {
    private static final ThreadLocal<String> dataSourceType = new ThreadLocal<>();
    private static final ThreadLocal<String> businessDataSourceName = new ThreadLocal<>();

    public static void setDataSourceType(String type) {
        dataSourceType.set(type);
    }

    public static String getDataSourceType() {
        return dataSourceType.get();
    }

    public static void setBusinessDataSourceName(String name) {
        businessDataSourceName.set(name);
    }

    public static String getBusinessDataSourceName() {
        return businessDataSourceName.get();
    }

    public static void clear() {
        dataSourceType.remove();
        businessDataSourceName.remove();
    }
}

// DataSourceAspect.java
@Aspect
@Component
public class DataSourceAspect {
    
    @Before("execution(* com.yourcompany.dao.common.*.*(..))")
    public void setCommonDataSource() {
        DataSourceContextHolder.setDataSourceType("common");
    }
    
    @Before("execution(* com.yourcompany.dao.business.*.*(..))")
    public void setBusinessDataSource() {
        DataSourceContextHolder.setDataSourceType("business");
    }
    
    @After("execution(* com.yourcompany.dao..*.*(..))")
    public void clearDataSource() {
        DataSourceContextHolder.clear();
    }
}

// BusinessDataSourceSwitcher.java
public class BusinessDataSourceSwitcher {
    public static void switchTo(String businessDataSourceName) {
        DataSourceContextHolder.setBusinessDataSourceName(businessDataSourceName);
    }

    public static void resetToDefault() {
        DataSourceContextHolder.setBusinessDataSourceName(null);
    }
}
