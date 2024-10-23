// 1. 动态数据源管理器
@Component
public class DynamicDataSourceManager {

    private final Map<String, DataSource> dataSources = new ConcurrentHashMap<>();
    private final Map<String, DataSourceMetrics> dataSourceMetrics = new ConcurrentHashMap<>();
    
    @Autowired
    private DataSourceProperties defaultProperties;
    
    @Autowired
    private MeterRegistry registry;
    
    public DataSource getDataSource(String key) {
        return dataSources.computeIfAbsent(key, this::createDataSource);
    }
    
    private DataSource createDataSource(String key) {
        // 获取数据源配置
        DataSourceConfig config = loadDataSourceConfig(key);
        
        // 创建HikariCP数据源
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.getUrl());
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(config.getPassword());
        hikariConfig.setDriverClassName(config.getDriverClassName());
        
        // 设置连接池参数
        configurePool(hikariConfig, config);
        
        // 创建数据源
        HikariDataSource dataSource = new HikariDataSource(hikariConfig);
        
        // 注册监控
        registerMetrics(key, dataSource);
        
        return dataSource;
    }
    
    private void configurePool(HikariConfig config, DataSourceConfig properties) {
        config.setMinimumIdle(properties.getMinIdle());
        config.setMaximumPoolSize(properties.getMaxPoolSize());
        config.setIdleTimeout(properties.getIdleTimeout());
        config.setMaxLifetime(properties.getMaxLifetime());
        config.setConnectionTimeout(properties.getConnectionTimeout());
        
        // 设置连接检测
        config.setValidationTimeout(5000);
        config.setLeakDetectionThreshold(30000);
        config.setConnectionTestQuery("SELECT 1");
        
        // 设置性能优化参数
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
    }
    
    private void registerMetrics(String key, HikariDataSource dataSource) {
        // 注册连接池指标
        registry.gauge("hikaricp.connections.active", 
            Tags.of("pool", key), dataSource, HikariDataSource::getActiveConnections);
            
        registry.gauge("hikaricp.connections.idle",
            Tags.of("pool", key), dataSource, HikariDataSource::getIdleConnections);
            
        registry.gauge("hikaricp.connections.total",
            Tags.of("pool", key), dataSource, HikariDataSource::getHikariPoolMXBean,
            pool -> pool.getTotalConnections());
            
        // 创建监控对象
        DataSourceMetrics metrics = new DataSourceMetrics(key, dataSource);
        dataSourceMetrics.put(key, metrics);
    }
    
    // 定期检查连接池状态
    @Scheduled(fixedRate = 60000)
    public void monitorDataSources() {
        dataSourceMetrics.forEach((key, metrics) -> {
            // 检查连接池使用情况
            if (metrics.isOverloaded()) {
                // 扩展连接池
                expandConnectionPool(key);
            } else if (metrics.isUnderutilized()) {
                // 收缩连接池
                shrinkConnectionPool(key);
            }
            
            // 检查连接泄漏
            if (metrics.hasConnectionLeak()) {
                handleConnectionLeak(key);
            }
        });
    }
}

// 2. 消息处理中心
@Service
public class MessageProcessingCenter {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    private final BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>(10000);
    private final Map<String, MessageHandler> handlers = new ConcurrentHashMap<>();
    private final Map<String, RetryPolicy> retryPolicies = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        // 启动消息处理线程
        startMessageProcessors();
        // 初始化重试策略
        initializeRetryPolicies();
    }
    
    public void submitMessage(Message message) {
        // 消息验证
        validateMessage(message);
        
        // 消息预处理
        preprocessMessage(message);
        
        // 提交到处理队列
        if (!messageQueue.offer(message)) {
            // 队列已满，执行降级策略
            handleQueueOverflow(message);
        }
    }
    
    private void startMessageProcessors() {
        int processorCount = Runtime.getRuntime().availableProcessors();
        for (int i = 0; i < processorCount; i++) {
            Thread processor = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Message message = messageQueue.take();
                        processMessage(message);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }, "message-processor-" + i);
            processor.start();
        }
    }
    
    private void processMessage(Message message) {
        MessageHandler handler = handlers.get(message.getType());
        if (handler == null) {
            log.error("No handler found for message type: " + message.getType());
            return;
        }
        
        RetryPolicy retryPolicy = retryPolicies.get(message.getType());
        int retryCount = 0;
        
        while (retryCount < retryPolicy.getMaxAttempts()) {
            try {
                // 处理消息
                ProcessResult result = handler.handle(message);
                
                // 处理成功
                if (result.isSuccess()) {
                    // 发送确认
                    sendAck(message);
                    return;
                }
                
                // 处理失败，准备重试
                retryCount++;
                if (retryCount < retryPolicy.getMaxAttempts()) {
                    // 等待重试间隔
                    Thread.sleep(retryPolicy.getNextInterval(retryCount));
                }
                
            } catch (Exception e) {
                log.error("Message processing failed", e);
                retryCount++;
                
                // 记录错误信息
                recordProcessingError(message, e, retryCount);
            }
        }
        
        // 达到最大重试次数，处理失败
        handleProcessingFailure(message);
    }
}

// 3. 服务监控中心
@Service
public class ServiceMonitorCenter {

    @Autowired
    private MeterRegistry registry;
    
    private final Map<String, ServiceHealthChecker> healthCheckers = new ConcurrentHashMap<>();
    private final Map<String, AlertRule> alertRules = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        // 初始化健康检查器
        initializeHealthCheckers();
        // 初始化告警规则
        initializeAlertRules();
    }
    
    @Scheduled(fixedRate = 30000)
    public void checkServiceHealth() {
        healthCheckers.forEach((serviceName, checker) -> {
            // 执行健康检查
            HealthCheckResult result = checker.check();
            
            // 更新健康状态指标
            updateHealthMetrics(serviceName, result);
            
            // 检查是否需要告警
            checkAlertConditions(serviceName, result);
        });
    }
    
    private void updateHealthMetrics(String serviceName, HealthCheckResult result) {
        // 记录可用性
        registry.gauge("service.availability", 
            Tags.of("service", serviceName), result.isAvailable() ? 1 : 0);
            
        // 记录响应时间
        registry.timer("service.response.time",
            Tags.of("service", serviceName)).record(result.getResponseTime(), TimeUnit.MILLISECONDS);
            
        // 记录错误率
        registry.counter("service.errors",
            Tags.of("service", serviceName)).increment(result.getErrorCount());
    }
    
    private void checkAlertConditions(String serviceName, HealthCheckResult result) {
        AlertRule rule = alertRules.get(serviceName);
        if (rule == null) {
            return;
        }
        
        // 检查各项指标
        if (result.getResponseTime() > rule.getMaxResponseTime()) {
            triggerAlert(serviceName, "Response time exceeded", result);
        }
        
        if (result.getErrorRate() > rule.getMaxErrorRate()) {
            triggerAlert(serviceName, "Error rate exceeded", result);
        }
        
        if (!result.isAvailable() && rule.isRequireHighAvailability()) {
            triggerAlert(serviceName, "Service unavailable", result);
        }
    }
    
    private void triggerAlert(String serviceName, String reason, HealthCheckResult result) {
        Alert alert = new Alert();
        alert.setServiceName(serviceName);
        alert.setReason(reason);
        alert.setSeverity(determineSeverity(result));
        alert.setTimestamp(System.currentTimeMillis());
        alert.setDetails(buildAlertDetails(result));
        
        // 发送告警
        sendAlert(alert);
    }
}
