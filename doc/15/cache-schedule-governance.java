// 1. 多级缓存管理器实现
@Service
public class MultiLevelCacheManager {

    @Autowired
    private CaffeineCacheManager localCache;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private final LoadingCache<String, Lock> lockCache = Caffeine.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build(key -> new ReentrantLock());
        
    public <T> T get(String key, Class<T> type) {
        // 查询本地缓存
        T value = getFromLocalCache(key, type);
        if (value != null) {
            return value;
        }
        
        // 获取锁防止缓存击穿
        Lock lock = lockCache.get(key);
        if (!lock.tryLock()) {
            // 等待一段时间后重试本地缓存
            try {
                Thread.sleep(50);
                value = getFromLocalCache(key, type);
                if (value != null) {
                    return value;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        try {
            // 双重检查
            value = getFromLocalCache(key, type);
            if (value != null) {
                return value;
            }
            
            // 查询Redis缓存
            value = getFromRedisCache(key, type);
            if (value != null) {
                // 写入本地缓存
                setLocalCache(key, value);
                return value;
            }
            
            // 查询数据库
            value = loadFromDb(key, type);
            if (value != null) {
                // 写入多级缓存
                setMultiLevelCache(key, value);
            }
            
            return value;
        } finally {
            lock.unlock();
        }
    }
    
    public void set(String key, Object value, Duration ttl) {
        // 写入本地缓存
        setLocalCache(key, value);
        // 写入Redis缓存
        setRedisCache(key, value, ttl);
    }
    
    public void delete(String key) {
        // 删除本地缓存
        localCache.getCache("default").evict(key);
        // 删除Redis缓存
        redisTemplate.delete(key);
    }
    
    // 缓存预热
    public void warmUp(List<String> keys) {
        CompletableFuture.runAsync(() -> {
            for (String key : keys) {
                try {
                    Object value = loadFromDb(key, Object.class);
                    if (value != null) {
                        setMultiLevelCache(key, value);
                    }
                } catch (Exception e) {
                    log.error("Cache warm up failed for key: " + key, e);
                }
            }
        });
    }
    
    // 缓存更新
    @TransactionalEventListener
    public void handleDataChange(DataChangeEvent event) {
        // 清除相关缓存
        String cacheKey = generateCacheKey(event);
        delete(cacheKey);
        
        // 异步重建缓存
        CompletableFuture.runAsync(() -> {
            try {
                Object newValue = loadFromDb(cacheKey, Object.class);
                if (newValue != null) {
                    setMultiLevelCache(cacheKey, newValue);
                }
            } catch (Exception e) {
                log.error("Cache rebuild failed for key: " + cacheKey, e);
            }
        });
    }
}

// 2. 高级任务调度器实现
@Service
public class AdvancedTaskScheduler {

    @Autowired
    private RedissonClient redisson;
    
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(
        Runtime.getRuntime().availableProcessors()
    );
    
    private final Map<String, TaskDefinition> taskDefinitions = new ConcurrentHashMap<>();
    private final Map<String, Future<?>> runningTasks = new ConcurrentHashMap<>();
    
    public void scheduleTask(TaskDefinition task) {
        validateTask(task);
        taskDefinitions.put(task.getTaskId(), task);
        
        Future<?> future = scheduleTaskExecution(task);
        runningTasks.put(task.getTaskId(), future);
    }
    
    private Future<?> scheduleTaskExecution(TaskDefinition task) {
        return executor.scheduleWithFixedDelay(() -> {
            RLock lock = redisson.getLock("task:" + task.getTaskId());
            try {
                // 获取分布式锁
                if (lock.tryLock(1, 5, TimeUnit.SECONDS)) {
                    try {
                        executeTask(task);
                    } finally {
                        lock.unlock();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, task.getInitialDelay(), task.getInterval(), TimeUnit.MILLISECONDS);
    }
    
    private void executeTask(TaskDefinition task) {
        try {
            // 记录任务开始
            TaskExecution execution = createTaskExecution(task);
            
            // 执行任务
            Object result = invokeTask(task);
            
            // 更新执行记录
            updateTaskExecution(execution, result, null);
            
        } catch (Exception e) {
            log.error("Task execution failed: " + task.getTaskId(), e);
            handleTaskError(task, e);
        }
    }
    
    // 任务分片执行
    private void executeShardedTask(TaskDefinition task) {
        // 获取当前节点分片
        int currentShard = getCurrentShard();
        int totalShards = getTotalShards();
        
        // 计算分片范围
        long totalRecords = getTotalRecords(task);
        long shardSize = totalRecords / totalShards;
        long startIndex = currentShard * shardSize;
        long endIndex = (currentShard == totalShards - 1) ? 
            totalRecords : (startIndex + shardSize);
            
        // 执行分片任务
        processShardedData(task, startIndex, endIndex);
    }
}

// 3. 服务治理实现
@Service
public class ServiceGovernanceManager {

    @Autowired
    private NacosNamingService namingService;
    
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
    
    @Autowired
    private LoadBalancerClient loadBalancerClient;
    
    private final Map<String, ServiceStats> serviceStats = new ConcurrentHashMap<>();
    
    // 服务调用
    public <T> T invokeService(String serviceId, ServiceRequest request, Class<T> responseType) {
        // 获取服务实例
        ServiceInstance instance = chooseInstance(serviceId);
        if (instance == null) {
            throw new ServiceUnavailableException("No available service instance");
        }
        
        // 获取熔断器
        CircuitBreaker circuitBreaker = getCircuitBreaker(serviceId);
        
        // 执行服务调用
        return circuitBreaker.executeSupplier(() -> {
            try {
                // 记录调用开始
                long startTime = System.currentTimeMillis();
                
                // 执行调用
                T response = executeRequest(instance, request, responseType);
                
                // 记录调用成功
                recordSuccess(serviceId, System.currentTimeMillis() - startTime);
                
                return response;
            } catch (Exception e) {
                // 记录调用失败
                recordFailure(serviceId, e);
                throw e;
            }
        });
    }
    
    // 自适应负载均衡
    private ServiceInstance chooseInstance(String serviceId) {
        // 获取所有实例
        List<ServiceInstance> instances = loadBalancerClient.getInstances(serviceId);
        if (CollectionUtils.isEmpty(instances)) {
            return null;
        }
        
        // 计算实例权重
        Map<ServiceInstance, Double> weights = calculateWeights(instances);
        
        // 根据权重选择实例
        return weightedRandom(weights);
    }
    
    private Map<ServiceInstance, Double> calculateWeights(List<ServiceInstance> instances) {
        Map<ServiceInstance, Double> weights = new HashMap<>();
        
        for (ServiceInstance instance : instances) {
            ServiceStats stats = serviceStats.get(instance.getInstanceId());
            if (stats == null) {
                weights.put(instance, 1.0);
                continue;
            }
            
            // 计算实例得分
            double score = calculateInstanceScore(stats);
            weights.put(instance, score);
        }
        
        return weights;
    }
    
    private double calculateInstanceScore(ServiceStats stats) {
        // 计算成功率
        double successRate = stats.getSuccessRate();
        // 计算平均响应时间
        double avgResponseTime = stats.getAverageResponseTime();
        // 计算CPU使用率
        double cpuUsage = stats.getCpuUsage();
        
        // 综合评分
        return successRate * 0.4 + (1.0 / avgResponseTime) * 0.3 + (1.0 - cpuUsage) * 0.3;
    }
}
