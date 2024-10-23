// 1. 分布式事务协调器实现
@Service
@Slf4j
public class TransactionCoordinator {

    @Autowired
    private RedissonClient redisson;
    
    @Autowired
    private TransactionRepository transactionRepo;
    
    private static final String TRANSACTION_LOCK_PREFIX = "tx:lock:";
    private static final String TRANSACTION_STATUS_PREFIX = "tx:status:";
    
    public String begin(TransactionContext context) {
        String txId = generateTransactionId();
        
        // 创建事务记录
        GlobalTransaction transaction = new GlobalTransaction();
        transaction.setTxId(txId);
        transaction.setStatus(TransactionStatus.BEGIN);
        transaction.setStartTime(System.currentTimeMillis());
        transaction.setTimeoutMs(context.getTimeoutMs());
        transactionRepo.save(transaction);
        
        // 注册分支事务
        for (BranchTransaction branch : context.getBranches()) {
            registerBranchTransaction(txId, branch);
        }
        
        return txId;
    }
    
    public void commit(String txId) {
        RLock lock = redisson.getLock(TRANSACTION_LOCK_PREFIX + txId);
        try {
            if (!lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                throw new TransactionException("Failed to acquire transaction lock");
            }
            
            GlobalTransaction transaction = transactionRepo.findByTxId(txId);
            if (transaction == null) {
                throw new TransactionException("Transaction not found: " + txId);
            }
            
            // 检查是否超时
            if (isTransactionTimeout(transaction)) {
                rollback(txId);
                throw new TransactionException("Transaction timeout");
            }
            
            // 获取所有分支事务
            List<BranchTransaction> branches = transactionRepo.findBranches(txId);
            
            // 执行两阶段提交
            boolean prepareSuccess = preparePhase(branches);
            if (prepareSuccess) {
                commitPhase(branches);
                transaction.setStatus(TransactionStatus.COMMITTED);
            } else {
                rollbackPhase(branches);
                transaction.setStatus(TransactionStatus.ROLLBACKED);
            }
            
            transaction.setEndTime(System.currentTimeMillis());
            transactionRepo.save(transaction);
            
        } catch (Exception e) {
            log.error("Transaction commit failed", e);
            rollback(txId);
            throw new TransactionException("Commit failed", e);
        } finally {
            lock.unlock();
        }
    }
    
    private boolean preparePhase(List<BranchTransaction> branches) {
        // 并行执行prepare
        CompletableFuture<Boolean>[] futures = branches.stream()
            .map(branch -> CompletableFuture.supplyAsync(() -> {
                try {
                    return executePrepare(branch);
                } catch (Exception e) {
                    log.error("Branch prepare failed", e);
                    return false;
                }
            }))
            .toArray(CompletableFuture[]::new);
            
        try {
            CompletableFuture.allOf(futures).get(10, TimeUnit.SECONDS);
            return Arrays.stream(futures)
                .map(CompletableFuture::join)
                .allMatch(success -> success);
        } catch (Exception e) {
            log.error("Prepare phase failed", e);
            return false;
        }
    }
    
    private boolean executePrepare(BranchTransaction branch) {
        try {
            // 调用分支事务prepare接口
            String result = httpClient.post(branch.getPrepareUrl(), branch.getContext());
            return "SUCCESS".equals(result);
        } catch (Exception e) {
            log.error("Prepare request failed", e);
            return false;
        }
    }
}

// 2. SQL注入防护实现
@Aspect
@Component
public class SqlInjectionProtector {

    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "('|%27|--|;|/\\*|\\*/|\\|\\||\\&\\&|\\b(select|insert|update|delete|drop|truncate)\\b)",
        Pattern.CASE_INSENSITIVE
    );
    
    @Before("@annotation(sqlParam)")
    public void checkSqlInjection(JoinPoint point, SqlParam sqlParam) {
        Object[] args = point.getArgs();
        String paramName = sqlParam.value();
        
        // 获取参数值
        Object paramValue = Arrays.stream(args)
            .filter(arg -> arg != null && paramName.equals(getParamName(arg)))
            .findFirst()
            .orElse(null);
            
        if (paramValue != null) {
            String value = paramValue.toString();
            if (containsSqlInjection(value)) {
                throw new SecurityException("Potential SQL injection detected");
            }
        }
    }
    
    private boolean containsSqlInjection(String value) {
        return SQL_INJECTION_PATTERN.matcher(value).find();
    }
    
    // SQL参数净化
    public String cleanSqlParam(String param) {
        if (param == null) {
            return null;
        }
        
        // 移除特殊字符
        param = param.replaceAll("['\"\\\\]", "");
        
        // 转义SQL关键字
        param = param.replaceAll("(?i)\\b(select|insert|update|delete|drop|truncate)\\b", "");
        
        return param;
    }
}

// 3. 性能优化管理器
@Service
public class PerformanceOptimizer {

    @Autowired
    private ThreadPoolExecutor businessThreadPool;
    
    @Autowired
    private CacheManager cacheManager;
    
    @Autowired
    private MeterRegistry registry;
    
    // 自适应线程池
    public void optimizeThreadPool() {
        int corePoolSize = businessThreadPool.getCorePoolSize();
        int maximumPoolSize = businessThreadPool.getMaximumPoolSize();
        int activeCount = businessThreadPool.getActiveCount();
        int queueSize = businessThreadPool.getQueue().size();
        
        // 计算线程池利用率
        double utilizationRate = (double) activeCount / maximumPoolSize;
        
        // 计算队列使用率
        int queueCapacity = ((LinkedBlockingQueue<?>) businessThreadPool.getQueue()).remainingCapacity() + queueSize;
        double queueUtilization = (double) queueSize / queueCapacity;
        
        // 动态调整线程池参数
        if (utilizationRate > 0.8 && queueUtilization > 0.5) {
            // 增加核心线程数
            int newCoreSize = Math.min(corePoolSize + 2, maximumPoolSize);
            businessThreadPool.setCorePoolSize(newCoreSize);
            
        } else if (utilizationRate < 0.2 && queueUtilization < 0.2) {
            // 减少核心线程数
            int newCoreSize = Math.max(corePoolSize - 1, 1);
            businessThreadPool.setCorePoolSize(newCoreSize);
        }
    }
    
    // 缓存优化
    public void optimizeCache() {
        // 监控缓存命中率
        Timer.Sample sample = Timer.start(registry);
        double hitRate = cacheManager.getStats().getHitRate();
        sample.stop(registry.timer("cache.hit.rate"));
        
        // 根据命中率调整缓存策略
        if (hitRate < 0.5) {
            // 增加缓存容量
            cacheManager.resize(cacheManager.getSize() * 2);
            // 调整过期时间
            cacheManager.adjustTtl(TimeUnit.MINUTES.toMillis(30));
        }
    }
    
    // SQL执行计划优化
    @Around("@annotation(SqlMonitor)")
    public Object monitorSqlExecution(ProceedingJoinPoint point) throws Throwable {
        String sql = extractSql(point);
        
        // 记录执行开始时间
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = point.proceed();
            
            // 记录执行时间
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 分析SQL执行计划
            analyzeSqlPlan(sql, executionTime);
            
            return result;
        } catch (Throwable e) {
            // 记录SQL执行异常
            recordSqlError(sql, e);
            throw e;
        }
    }
    
    private void analyzeSqlPlan(String sql, long executionTime) {
        // 获取SQL执行计划
        String explainPlan = getExplainPlan(sql);
        
        // 检查是否需要优化
        if (executionTime > 1000 || !isOptimalPlan(explainPlan)) {
            // 生成优化建议
            List<String> suggestions = generateOptimizationSuggestions(explainPlan);
            
            // 记录优化建议
            recordOptimizationSuggestions(sql, suggestions);
        }
    }
}
