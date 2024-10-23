// 1. 微服务熔断器实现
@Service
@Slf4j
public class CircuitBreakerManager {

    private final Map<String, CircuitBreaker> breakers = new ConcurrentHashMap<>();
    private final Map<String, CircuitBreakerMetrics> metrics = new ConcurrentHashMap<>();
    
    @Value("${circuit.breaker.failureThreshold:50}")
    private int failureThreshold;
    
    @Value("${circuit.breaker.resetTimeout:60000}")
    private long resetTimeout;
    
    public <T> T execute(String serviceName, Supplier<T> execution) {
        CircuitBreaker breaker = breakers.computeIfAbsent(serviceName, 
            key -> new CircuitBreaker(serviceName, failureThreshold, resetTimeout));
        
        if (!breaker.allowRequest()) {
            throw new CircuitBreakerException("Service is not available");
        }
        
        try {
            T result = execution.get();
            breaker.recordSuccess();
            return result;
        } catch (Exception e) {
            breaker.recordFailure();
            throw e;
        }
    }
    
    private class CircuitBreaker {
        private final String serviceName;
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicReference<CircuitBreakerState> state = 
            new AtomicReference<>(CircuitBreakerState.CLOSED);
        private final AtomicLong lastStateChangeTime = new AtomicLong(0);
        
        public CircuitBreaker(String serviceName, int failureThreshold, long resetTimeout) {
            this.serviceName = serviceName;
            // 初始化监控指标
            metrics.put(serviceName, new CircuitBreakerMetrics());
        }
        
        public boolean allowRequest() {
            CircuitBreakerState currentState = state.get();
            if (currentState == CircuitBreakerState.OPEN) {
                if (System.currentTimeMillis() - lastStateChangeTime.get() > resetTimeout) {
                    // 进入半开状态
                    if (state.compareAndSet(CircuitBreakerState.OPEN, CircuitBreakerState.HALF_OPEN)) {
                        lastStateChangeTime.set(System.currentTimeMillis());
                        return true;
                    }
                }
                return false;
            }
            return true;
        }
        
        public void recordSuccess() {
            failureCount.set(0);
            if (state.get() == CircuitBreakerState.HALF_OPEN) {
                state.set(CircuitBreakerState.CLOSED);
                lastStateChangeTime.set(System.currentTimeMillis());
            }
            metrics.get(serviceName).recordSuccess();
        }
        
        public void recordFailure() {
            metrics.get(serviceName).recordFailure();
            if (failureCount.incrementAndGet() >= failureThreshold) {
                state.set(CircuitBreakerState.OPEN);
                lastStateChangeTime.set(System.currentTimeMillis());
            }
        }
    }
}

// 2. 分布式限流实现
@Service
public class DistributedRateLimiter {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String RATE_LIMITER_KEY = "rate_limiter:";
    
    public boolean tryAcquire(String key, int permits, int limit, int timeout) {
        String script = 
            "local current = redis.call('get', KEYS[1]) " +
            "current = tonumber(current) or 0 " +
            "if current + tonumber(ARGV[1]) > tonumber(ARGV[2]) then " +
            "    return 0 " +
            "end " +
            "redis.call('incrby', KEYS[1], ARGV[1]) " +
            "if current == 0 then " +
            "    redis.call('expire', KEYS[1], ARGV[3]) " +
            "end " +
            "return 1";
            
        List<String> keys = Collections.singletonList(RATE_LIMITER_KEY + key);
        List<String> args = Arrays.asList(
            String.valueOf(permits),
            String.valueOf(limit),
            String.valueOf(timeout)
        );
        
        Long result = (Long) redisTemplate.execute(
            new DefaultRedisScript<>(script, Long.class),
            keys,
            args.toArray()
        );
        
        return result != null && result == 1;
    }
}

// 3. 服务注册与发现增强
@Configuration
public class ServiceDiscoveryConfig {
    
    @Autowired
    private NacosDiscoveryProperties nacosProperties;
    
    @Bean
    public ServiceRegistry serviceRegistry() {
        return new EnhancedServiceRegistry();
    }
    
    private class EnhancedServiceRegistry implements ServiceRegistry {
        
        @Override
        public void register(ServiceInstance instance) {
            // 添加服务健康检查
            instance.getMetadata().put("health.check.url", 
                instance.getUri() + "/actuator/health");
            
            // 添加服务版本信息
            instance.getMetadata().put("version", getApplicationVersion());
            
            // 添加服务权重信息
            instance.getMetadata().put("weight", "100");
            
            // 注册服务实例
            nacosProperties.namingServiceInstance().registerInstance(
                instance.getServiceId(),
                instance.getHost(),
                instance.getPort(),
                instance.getMetadata()
            );
        }
    }
}

// 4. 动态线程池管理
@Component
public class DynamicThreadPoolManager {
    
    private final Map<String, ThreadPoolExecutor> threadPools = new ConcurrentHashMap<>();
    private final Map<String, ThreadPoolConfig> poolConfigs = new ConcurrentHashMap<>();
    
    @Autowired
    private ThreadPoolMonitor monitor;
    
    public ThreadPoolExecutor getThreadPool(String poolName) {
        return threadPools.computeIfAbsent(poolName, this::createThreadPool);
    }
    
    public void updateThreadPool(String poolName, ThreadPoolConfig config) {
        ThreadPoolExecutor executor = threadPools.get(poolName);
        if (executor != null) {
            // 更新线程池配置
            executor.setCorePoolSize(config.getCorePoolSize());
            executor.setMaximumPoolSize(config.getMaxPoolSize());
            executor.setKeepAliveTime(config.getKeepAliveTime(), TimeUnit.SECONDS);
            
            // 更新工作队列
            if (config.getQueueSize() > 0) {
                ResizableLinkedBlockingQueue<Runnable> newQueue = 
                    new ResizableLinkedBlockingQueue<>(config.getQueueSize());
                executor.setQueue(newQueue);
            }
            
            // 更新拒绝策略
            executor.setRejectedExecutionHandler(createRejectedHandler(config.getRejectedPolicy()));
            
            // 更新配置缓存
            poolConfigs.put(poolName, config);
        }
    }
    
    private ThreadPoolExecutor createThreadPool(String poolName) {
        ThreadPoolConfig config = poolConfigs.get(poolName);
        if (config == null) {
            config = loadDefaultConfig(poolName);
        }
        
        ThreadPoolExecutor executor = new MonitoredThreadPoolExecutor(
            config.getCorePoolSize(),
            config.getMaxPoolSize(),
            config.getKeepAliveTime(),
            TimeUnit.SECONDS,
            new ResizableLinkedBlockingQueue<>(config.getQueueSize()),
            new ThreadFactoryBuilder().setNameFormat(poolName + "-%d").build(),
            createRejectedHandler(config.getRejectedPolicy())
        );
        
        // 注册监控
        monitor.register(poolName, executor);
        
        return executor;
    }
    
    // 自定义监控线程池
    private class MonitoredThreadPoolExecutor extends ThreadPoolExecutor {
        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            monitor.beforeExecute(t, r);
        }
        
        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            monitor.afterExecute(r, t);
        }
    }
}
