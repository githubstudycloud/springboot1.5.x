// 1. 高级参数验证器
@Service
public class AdvancedValidator {

    @Autowired
    private ValidatorFactory validatorFactory;
    
    @Autowired
    private RuleEngine ruleEngine;
    
    private final Map<String, ValidationRule> customRules = new ConcurrentHashMap<>();
    
    public ValidationResult validate(Object target, String scenario) {
        ValidationResult result = new ValidationResult();
        
        try {
            // 基础JSR303验证
            performJsr303Validation(target, result);
            
            // 自定义业务规则验证
            performCustomValidation(target, scenario, result);
            
            // 复杂业务规则验证
            performBusinessRuleValidation(target, scenario, result);
            
        } catch (Exception e) {
            log.error("Validation failed", e);
            result.addError("SYSTEM_ERROR", "System validation error occurred");
        }
        
        return result;
    }
    
    private void performJsr303Validation(Object target, ValidationResult result) {
        Validator validator = validatorFactory.getValidator();
        Set<ConstraintViolation<Object>> violations = validator.validate(target);
        
        for (ConstraintViolation<Object> violation : violations) {
            result.addError(
                violation.getPropertyPath().toString(),
                violation.getMessage()
            );
        }
    }
    
    private void performCustomValidation(Object target, String scenario, 
                                       ValidationResult result) {
        // 获取场景对应的验证规则
        List<ValidationRule> rules = getScenarioRules(scenario);
        
        for (ValidationRule rule : rules) {
            try {
                boolean valid = rule.validate(target);
                if (!valid) {
                    result.addError(rule.getField(), rule.getMessage());
                }
            } catch (Exception e) {
                log.error("Custom validation failed", e);
                result.addError(rule.getField(), "Validation failed");
            }
        }
    }
    
    // 动态规则注册
    public void registerRule(String field, ValidationRule rule) {
        customRules.put(field, rule);
    }
    
    // 支持脚本规则
    public void registerScriptRule(String field, String script) {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("nashorn");
        
        try {
            CompiledScript compiled = ((Compilable) engine).compile(script);
            ValidationRule rule = new ScriptValidationRule(field, compiled);
            registerRule(field, rule);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compile validation script", e);
        }
    }
}

// 2. 全局异常处理增强
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @Autowired
    private ErrorCodeService errorCodeService;
    
    @Autowired
    private AlertService alertService;
    
    private final Map<Class<? extends Exception>, ErrorHandler> errorHandlers = new HashMap<>();
    
    @PostConstruct
    public void init() {
        // 注册异常处理器
        registerErrorHandlers();
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex, WebRequest request) {
        // 生成错误追踪ID
        String errorId = generateErrorId();
        
        // 记录详细错误信息
        logError(errorId, ex, request);
        
        // 查找对应的处理器
        ErrorHandler handler = findErrorHandler(ex);
        
        // 处理异常
        ErrorResponse response = handler.handle(ex, errorId);
        
        // 检查是否需要告警
        checkAlertThreshold(ex);
        
        return new ResponseEntity<>(response, response.getStatus());
    }
    
    private void logError(String errorId, Exception ex, WebRequest request) {
        ErrorLogEntry entry = new ErrorLogEntry();
        entry.setErrorId(errorId);
        entry.setTimestamp(System.currentTimeMillis());
        entry.setException(ex.getClass().getName());
        entry.setMessage(ex.getMessage());
        entry.setStackTrace(ExceptionUtils.getStackTrace(ex));
        entry.setRequestInfo(extractRequestInfo(request));
        
        // 异步写入错误日志
        CompletableFuture.runAsync(() -> {
            try {
                errorLogService.save(entry);
            } catch (Exception e) {
                log.error("Failed to save error log", e);
            }
        });
    }
    
    private void checkAlertThreshold(Exception ex) {
        String exceptionType = ex.getClass().getName();
        ErrorStats stats = errorStatsService.getStats(exceptionType);
        
        // 检查错误频率
        if (stats.getOccurrences() > getAlertThreshold(exceptionType)) {
            // 触发告警
            Alert alert = new Alert();
            alert.setType("ERROR_THRESHOLD_EXCEEDED");
            alert.setSource(exceptionType);
            alert.setCount(stats.getOccurrences());
            alertService.sendAlert(alert);
        }
    }
}

// 3. 业务规则引擎
@Service
public class BusinessRuleEngine {

    @Autowired
    private RuleRepository ruleRepository;
    
    private final Map<String, Rule> ruleCache = new ConcurrentHashMap<>();
    private final Map<String, RuleScript> compiledScripts = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        // 加载规则配置
        loadRules();
        // 编译规则脚本
        compileRules();
    }
    
    public RuleExecutionResult executeRules(String ruleSet, Map<String, Object> facts) {
        RuleExecutionResult result = new RuleExecutionResult();
        
        // 获取规则集
        List<Rule> rules = getRules(ruleSet);
        
        // 创建执行上下文
        RuleContext context = createContext(facts);
        
        // 按优先级排序规则
        rules.sort(Comparator.comparingInt(Rule::getPriority));
        
        // 执行规则
        for (Rule rule : rules) {
            if (shouldExecuteRule(rule, context)) {
                executeRule(rule, context, result);
            }
        }
        
        return result;
    }
    
    private void executeRule(Rule rule, RuleContext context, 
                           RuleExecutionResult result) {
        try {
            // 获取编译后的规则脚本
            RuleScript script = compiledScripts.get(rule.getId());
            
            // 执行规则
            Object ruleResult = script.execute(context);
            
            // 处理规则执行结果
            handleRuleResult(rule, ruleResult, result);
            
            // 记录规则执行
            recordRuleExecution(rule, context, ruleResult);
            
        } catch (Exception e) {
            log.error("Rule execution failed", e);
            result.addError(rule.getId(), e.getMessage());
        }
    }
    
    private void recordRuleExecution(Rule rule, RuleContext context, 
                                   Object result) {
        RuleExecution execution = new RuleExecution();
        execution.setRuleId(rule.getId());
        execution.setTimestamp(System.currentTimeMillis());
        execution.setContext(context.toMap());
        execution.setResult(result);
        
        // 异步保存执行记录
        CompletableFuture.runAsync(() -> {
            try {
                ruleExecutionRepository.save(execution);
            } catch (Exception e) {
                log.error("Failed to save rule execution", e);
            }
        });
    }
    
    // 动态规则更新
    public void updateRule(String ruleId, String script) {
        try {
            // 编译新规则
            RuleScript compiled = compileScript(script);
            
            // 验证规则
            validateRule(compiled);
            
            // 更新缓存
            compiledScripts.put(ruleId, compiled);
            
            // 保存规则
            Rule rule = ruleRepository.findById(ruleId);
            rule.setScript(script);
            rule.setUpdateTime(System.currentTimeMillis());
            ruleRepository.save(rule);
            
        } catch (Exception e) {
            throw new RuleCompilationException("Failed to update rule: " + ruleId, e);
        }
    }
}
