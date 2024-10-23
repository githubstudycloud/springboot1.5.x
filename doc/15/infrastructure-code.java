// 1. 分页查询封装
@Data
public class PageRequest<T> {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private T params;
    
    public PageRequest<T> of(Integer pageNum, Integer pageSize, T params) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.params = params;
        return this;
    }
}

// 2. 统一响应封装
@Data
public class Result<T> {
    private Integer code;
    private String message;
    private T data;
    
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("success");
        result.setData(data);
        return result;
    }
    
    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage(message);
        return result;
    }
}

// 3. 分布式锁实现
@Aspect
@Component
public class DistributedLockAspect {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint point, DistributedLock distributedLock) throws Throwable {
        String lockKey = getLockKey(point, distributedLock);
        boolean locked = false;
        try {
            locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "LOCKED", 
                distributedLock.expire(), TimeUnit.SECONDS);
            if (!locked) {
                throw new RuntimeException("获取锁失败");
            }
            return point.proceed();
        } finally {
            if (locked) {
                redisTemplate.delete(lockKey);
            }
        }
    }
}

// 4. 雪花ID生成器
@Component
public class SnowflakeIdGenerator {
    private final long workerId;
    private final long datacenterId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;
    
    public SnowflakeIdGenerator(@Value("${snowflake.worker-id}") long workerId,
                               @Value("${snowflake.datacenter-id}") long datacenterId) {
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }
    
    public synchronized long nextId() {
        long timestamp = timeGen();
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards");
        }
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - twepoch) << timestampLeftShift) |
                (datacenterId << datacenterIdShift) |
                (workerId << workerIdShift) |
                sequence;
    }
}

// 5. 全局异常处理
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        return Result.error(e.getMessage());
    }
    
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("System error", e);
        return Result.error("系统异常，请稍后重试");
    }
}

// 6. 请求日志记录
@Aspect
@Component
public class WebLogAspect {
    
    @Around("execution(* com.example.*.controller.*.*(..))")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        
        // 记录请求信息
        WebLog webLog = new WebLog();
        webLog.setUri(request.getRequestURI());
        webLog.setMethod(request.getMethod());
        webLog.setParams(getParameter(joinPoint));
        
        Object result = joinPoint.proceed();
        
        // 记录响应信息
        webLog.setResult(result);
        webLog.setSpendTime(System.currentTimeMillis() - startTime);
        // 异步记录日志
        asyncLogService.saveLog(webLog);
        
        return result;
    }
}
