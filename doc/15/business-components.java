// 1. 基于Redis的限流实现
@Aspect
@Component
public class RateLimitAspect {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";
    
    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint point, RateLimit rateLimit) throws Throwable {
        String key = RATE_LIMIT_KEY_PREFIX + rateLimit.key();
        // 使用Redis的令牌桶算法实现
        String script = "local tokens_key = KEYS[1] " +
                       "local timestamp_key = KEYS[2] " +
                       "local rate = tonumber(ARGV[1]) " +
                       "local capacity = tonumber(ARGV[2]) " +
                       "local now = tonumber(ARGV[3]) " +
                       "local requested = tonumber(ARGV[4]) " +
                       "local fill_time = capacity/rate " +
                       "local ttl = math.floor(fill_time*2) " +
                       "local last_tokens = tonumber(redis.call('get', tokens_key) or capacity) " +
                       "local last_refreshed = tonumber(redis.call('get', timestamp_key) or 0) " +
                       "local delta = math.max(0, now-last_refreshed) " +
                       "local filled_tokens = math.min(capacity, last_tokens+(delta*rate)) " +
                       "local allowed = filled_tokens >= requested " +
                       "local new_tokens = filled_tokens " +
                       "if allowed then " +
                       "    new_tokens = filled_tokens - requested " +
                       "end " +
                       "redis.call('setex', tokens_key, ttl, new_tokens) " +
                       "redis.call('setex', timestamp_key, ttl, now) " +
                       "return allowed";
                       
        List<String> keys = Arrays.asList(key + ":tokens", key + ":timestamp");
        // 当前时间戳，单位：秒
        long now = System.currentTimeMillis() / 1000;
        boolean allowed = (boolean) redisTemplate.execute(
            new DefaultRedisScript<>(script, Boolean.class),
            keys,
            rateLimit.rate(),
            rateLimit.capacity(),
            now,
            1
        );
        
        if (!allowed) {
            throw new RuntimeException("请求太频繁，请稍后重试");
        }
        
        return point.proceed();
    }
}

// 2. 分布式事务实现(基于TCC模式)
@Component
public class DistributedTransactionManager {
    
    @Autowired
    private TransactionRepository repository;
    
    public void begin(String xid) {
        TransactionContext context = new TransactionContext(xid);
        TransactionContextHolder.set(context);
    }
    
    @Transactional(rollbackFor = Exception.class)
    public void commit(String xid) {
        TransactionContext context = repository.get(xid);
        if (context == null) {
            return;
        }
        
        try {
            // 执行所有登记的confirm操作
            context.getResources().forEach(resource -> {
                try {
                    resource.commit();
                } catch (Exception e) {
                    log.error("Commit resource failed", e);
                    throw new RuntimeException("事务提交失败");
                }
            });
            repository.remove(xid);
        } finally {
            TransactionContextHolder.clear();
        }
    }
    
    @Transactional(rollbackFor = Exception.class)
    public void rollback(String xid) {
        TransactionContext context = repository.get(xid);
        if (context == null) {
            return;
        }
        
        try {
            // 执行所有登记的cancel操作
            context.getResources().forEach(resource -> {
                try {
                    resource.rollback();
                } catch (Exception e) {
                    log.error("Rollback resource failed", e);
                }
            });
            repository.remove(xid);
        } finally {
            TransactionContextHolder.clear();
        }
    }
}

// 3. 动态数据源路由实现
public class DynamicDataSourceRouter extends AbstractRoutingDataSource {
    
    @Autowired
    private ProjectDataSourceConfigMapper configMapper;
    
    private Map<Object, Object> targetDataSources = new ConcurrentHashMap<>();
    
    @Override
    protected Object determineCurrentLookupKey() {
        return DataSourceContextHolder.get();
    }
    
    @Override
    protected DataSource determineTargetDataSource() {
        String projectId = DataSourceContextHolder.get();
        DataSource dataSource = (DataSource) targetDataSources.get(projectId);
        
        if (dataSource == null) {
            synchronized (this) {
                dataSource = (DataSource) targetDataSources.get(projectId);
                if (dataSource == null) {
                    // 从配置表获取数据源配置
                    ProjectDataSourceConfig config = configMapper.getByProjectId(projectId);
                    if (config == null) {
                        throw new RuntimeException("数据源配置不存在");
                    }
                    // 创建新的数据源
                    dataSource = createDataSource(config);
                    targetDataSources.put(projectId, dataSource);
                }
            }
        }
        
        return dataSource;
    }
    
    private DataSource createDataSource(ProjectDataSourceConfig config) {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName(config.getDriverClassName());
        dataSource.setUrl(config.getUrl());
        dataSource.setUsername(config.getUsername());
        dataSource.setPassword(config.getPassword());
        // 设置连接池配置
        dataSource.setInitialSize(5);
        dataSource.setMaxTotal(20);
        dataSource.setMaxIdle(10);
        dataSource.setMinIdle(5);
        return dataSource;
    }
}

// 4. 缓存管理实现
@Component
public class CacheManager {
    
    @Autowired
    private CaffeineCacheManager localCache;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    public void put(String key, Object value, long ttl, TimeUnit unit) {
        // 先写入Redis
        redisTemplate.opsForValue().set(key, value, ttl, unit);
        // 再写入本地缓存
        localCache.getCache("default").put(key, value);
    }
    
    public <T> T get(String key, Class<T> type) {
        // 先查本地缓存
        Cache.ValueWrapper localValue = localCache.getCache("default").get(key);
        if (localValue != null) {
            return (T) localValue.get();
        }
        
        // 查Redis
        Object value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            // 写入本地缓存
            localCache.getCache("default").put(key, value);
            return (T) value;
        }
        
        return null;
    }
    
    public void delete(String key) {
        redisTemplate.delete(key);
        localCache.getCache("default").evict(key);
    }
}
