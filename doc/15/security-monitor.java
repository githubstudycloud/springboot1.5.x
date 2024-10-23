// 1. API接口签名验证
@Aspect
@Component
public class ApiSignatureAspect {
    
    @Autowired
    private ApiKeyProperties apiKeyProperties;
    
    @Around("@annotation(apiSignature)")
    public Object validateSignature(ProceedingJoinPoint point, ApiSignature apiSignature) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        
        // 获取请求参数
        String timestamp = request.getHeader("X-Timestamp");
        String nonce = request.getHeader("X-Nonce");
        String signature = request.getHeader("X-Signature");
        String appId = request.getHeader("X-AppId");
        
        // 验证时间戳
        if (Math.abs(System.currentTimeMillis() - Long.parseLong(timestamp)) > 5 * 60 * 1000) {
            throw new SecurityException("请求已过期");
        }
        
        // 防重放攻击
        String nonceKey = "nonce:" + nonce;
        if (!redisTemplate.opsForValue().setIfAbsent(nonceKey, "1", 5, TimeUnit.MINUTES)) {
            throw new SecurityException("重复的请求");
        }
        
        // 验证签名
        String secretKey = apiKeyProperties.getSecretKey(appId);
        if (secretKey == null) {
            throw new SecurityException("无效的AppId");
        }
        
        // 构建签名字符串
        String signString = buildSignString(request, timestamp, nonce, appId, secretKey);
        String expectedSignature = DigestUtils.sha256Hex(signString);
        
        if (!expectedSignature.equals(signature)) {
            throw new SecurityException("签名验证失败");
        }
        
        return point.proceed();
    }
    
    private String buildSignString(HttpServletRequest request, String timestamp, 
                                 String nonce, String appId, String secretKey) {
        TreeMap<String, String> params = new TreeMap<>();
        params.put("timestamp", timestamp);
        params.put("nonce", nonce);
        params.put("appId", appId);
        
        // 添加请求参数
        request.getParameterMap().forEach((key, values) -> {
            params.put(key, values[0]);
        });
        
        // 按参数名排序并拼接
        StringBuilder sb = new StringBuilder();
        params.forEach((key, value) -> {
            sb.append(key).append("=").append(value).append("&");
        });
        sb.append("key=").append(secretKey);
        
        return sb.toString();
    }
}

// 2. 全链路监控实现
@Component
public class TraceContextHolder {
    private static final ThreadLocal<TraceContext> contextHolder = new ThreadLocal<>();
    
    public static void setContext(TraceContext context) {
        contextHolder.set(context);
    }
    
    public static TraceContext getContext() {
        TraceContext context = contextHolder.get();
        if (context == null) {
            context = new TraceContext();
            contextHolder.set(context);
        }
        return context;
    }
    
    public static void clear() {
        contextHolder.remove();
    }
}

@Data
public class TraceContext {
    private String traceId;
    private String spanId;
    private Long startTime;
    private Map<String, String> tags = new HashMap<>();
    private List<SpanEvent> spans = new ArrayList<>();
}

@Aspect
@Component
public class TraceAspect {
    
    @Around("@annotation(trace)")
    public Object trace(ProceedingJoinPoint point, Trace trace) throws Throwable {
        TraceContext context = TraceContextHolder.getContext();
        String parentSpanId = context.getSpanId();
        
        // 生成新的spanId
        String spanId = generateSpanId();
        context.setSpanId(spanId);
        
        SpanEvent span = new SpanEvent();
        span.setSpanId(spanId);
        span.setParentSpanId(parentSpanId);
        span.setStartTime(System.currentTimeMillis());
        span.setClassName(point.getTarget().getClass().getName());
        span.setMethodName(point.getSignature().getName());
        
        try {
            Object result = point.proceed();
            span.setSuccess(true);
            return result;
        } catch (Throwable t) {
            span.setSuccess(false);
            span.setErrorMessage(t.getMessage());
            throw t;
        } finally {
            span.setEndTime(System.currentTimeMillis());
            context.getSpans().add(span);
            // 还原父spanId
            context.setSpanId(parentSpanId);
        }
    }
}

// 3. 性能监控和告警
@Component
public class PerformanceMonitor {
    
    @Autowired
    private MeterRegistry registry;
    
    @Autowired
    private AlertService alertService;
    
    private final Map<String, Double> thresholds = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        // 初始化监控指标阈值
        thresholds.put("api.response.time", 1000.0);  // 1秒
        thresholds.put("jvm.memory.used.ratio", 0.85);  // 85%
        thresholds.put("system.cpu.usage", 0.80);  // 80%
    }
    
    @Scheduled(fixedRate = 60000)
    public void checkMetrics() {
        // 检查API响应时间
        Timer apiTimer = registry.find("api.response.time").timer();
        if (apiTimer != null && apiTimer.mean(TimeUnit.MILLISECONDS) > thresholds.get("api.response.time")) {
            alertService.sendAlert("API响应时间异常", 
                String.format("平均响应时间: %.2fms", apiTimer.mean(TimeUnit.MILLISECONDS)));
        }
        
        // 检查JVM内存使用
        Gauge memoryGauge = registry.find("jvm.memory.used").gauge();
        Gauge totalMemoryGauge = registry.find("jvm.memory.total").gauge();
        if (memoryGauge != null && totalMemoryGauge != null) {
            double usedRatio = memoryGauge.value() / totalMemoryGauge.value();
            if (usedRatio > thresholds.get("jvm.memory.used.ratio")) {
                alertService.sendAlert("内存使用率过高", 
                    String.format("当前使用率: %.2f%%", usedRatio * 100));
            }
        }
        
        // 检查CPU使用率
        Gauge cpuGauge = registry.find("system.cpu.usage").gauge();
        if (cpuGauge != null && cpuGauge.value() > thresholds.get("system.cpu.usage")) {
            alertService.sendAlert("CPU使用率过高", 
                String.format("当前使用率: %.2f%%", cpuGauge.value() * 100));
        }
    }
}

// 4. 数据脱敏处理
@Component
public class DataMaskingAspect {
    
    @Around("@annotation(dataMasking)")
    public Object mask(ProceedingJoinPoint point, DataMasking dataMasking) throws Throwable {
        Object result = point.proceed();
        if (result == null) {
            return null;
        }
        
        // 处理返回结果
        if (result instanceof Collection) {
            ((Collection<?>) result).forEach(this::maskObject);
        } else {
            maskObject(result);
        }
        
        return result;
    }
    
    private void maskObject(Object obj) {
        ReflectionUtils.doWithFields(obj.getClass(), field -> {
            field.setAccessible(true);
            Sensitive sensitive = field.getAnnotation(Sensitive.class);
            if (sensitive != null && field.get(obj) != null) {
                String value = field.get(obj).toString();
                field.set(obj, maskValue(value, sensitive.type()));
            }
        });
    }
    
    private String maskValue(String value, SensitiveType type) {
        switch (type) {
            case PHONE:
                return maskPhone(value);
            case ID_CARD:
                return maskIdCard(value);
            case EMAIL:
                return maskEmail(value);
            case BANK_CARD:
                return maskBankCard(value);
            default:
                return value;
        }
    }
}
