// 1. 工作流引擎核心实现
@Service
@Slf4j
public class WorkflowEngine {
    
    @Autowired
    private WorkflowNodeRepository nodeRepository;
    
    @Autowired
    private WorkflowInstanceRepository instanceRepository;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @Transactional
    public WorkflowInstance startWorkflow(String workflowCode, String businessId, Map<String, Object> params) {
        // 创建工作流实例
        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflowCode(workflowCode);
        instance.setBusinessId(businessId);
        instance.setStatus(WorkflowStatus.RUNNING);
        instance.setVariables(params);
        
        // 获取开始节点
        WorkflowNode startNode = nodeRepository.findStartNode(workflowCode);
        instance.setCurrentNode(startNode.getNodeCode());
        instanceRepository.save(instance);
        
        // 发布工作流开始事件
        eventPublisher.publishEvent(new WorkflowStartEvent(instance));
        
        // 执行开始节点
        executeNode(instance, startNode);
        
        return instance;
    }
    
    @Transactional
    public void executeNode(WorkflowInstance instance, WorkflowNode node) {
        try {
            // 执行节点前的准备工作
            preExecute(instance, node);
            
            // 根据节点类型执行不同的逻辑
            switch (node.getType()) {
                case SERVICE_TASK:
                    executeServiceTask(instance, node);
                    break;
                case USER_TASK:
                    createUserTask(instance, node);
                    break;
                case GATEWAY:
                    evaluateGateway(instance, node);
                    break;
                case END:
                    completeWorkflow(instance);
                    break;
            }
            
            // 执行节点后的清理工作
            postExecute(instance, node);
            
        } catch (Exception e) {
            log.error("Node execution failed", e);
            handleNodeError(instance, node, e);
        }
    }
    
    private void evaluateGateway(WorkflowInstance instance, WorkflowNode node) {
        // 获取网关条件
        String expression = node.getConditionExpression();
        Map<String, Object> variables = instance.getVariables();
        
        // 使用SpEL表达式引擎评估条件
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariables(variables);
        
        Expression exp = parser.parseExpression(expression);
        String nextNode = exp.getValue(context, String.class);
        
        // 更新实例状态
        instance.setCurrentNode(nextNode);
        instanceRepository.save(instance);
        
        // 执行下一个节点
        WorkflowNode next = nodeRepository.findByNodeCode(nextNode);
        executeNode(instance, next);
    }
}

// 2. 分布式任务调度实现
@Service
public class DistributedTaskScheduler {
    
    @Autowired
    private RedissonClient redisson;
    
    @Autowired
    private TaskExecutor taskExecutor;
    
    @Autowired
    private TaskRepository taskRepository;
    
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        // 加载所有任务
        List<TaskDefinition> tasks = taskRepository.findAll();
        tasks.forEach(this::scheduleTask);
    }
    
    public void scheduleTask(TaskDefinition task) {
        RLock lock = redisson.getLock("task-lock:" + task.getTaskId());
        
        ScheduledFuture<?> future = taskExecutor.schedule(() -> {
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
        }, task.getCron());
        
        scheduledTasks.put(task.getTaskId(), future);
    }
    
    private void executeTask(TaskDefinition task) {
        try {
            // 执行具体任务逻辑
            Class<?> taskClass = Class.forName(task.getTaskClass());
            Object taskInstance = SpringContextHolder.getBean(taskClass);
            Method method = taskClass.getMethod(task.getMethodName());
            method.invoke(taskInstance);
            
            // 更新任务执行状态
            updateTaskStatus(task, TaskStatus.SUCCESS, null);
            
        } catch (Exception e) {
            log.error("Task execution failed", e);
            updateTaskStatus(task, TaskStatus.FAILED, e.getMessage());
        }
    }
}

// 3. 消息总线实现
@Service
public class MessageBus {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    private MessageRepository messageRepository;
    
    public void publish(String topic, Object message) {
        String messageId = IdGenerator.nextId();
        
        // 保存消息记录
        MessageRecord record = new MessageRecord();
        record.setMessageId(messageId);
        record.setTopic(topic);
        record.setContent(JsonUtils.toJson(message));
        record.setStatus(MessageStatus.PENDING);
        messageRepository.save(record);
        
        // 发送消息
        rabbitTemplate.convertAndSend(topic, message, msg -> {
            MessageProperties props = msg.getMessageProperties();
            props.setMessageId(messageId);
            props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            return msg;
        });
    }
    
    @RabbitListener(queues = "#{T(java.util.Arrays).asList('${mq.queues}'.split(','))}")
    public void onMessage(Message message, Channel channel) {
        String messageId = message.getMessageProperties().getMessageId();
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        
        try {
            // 检查消息是否已处理
            if (messageRepository.isProcessed(messageId)) {
                channel.basicAck(deliveryTag, false);
                return;
            }
            
            // 处理消息
            processMessage(message);
            
            // 更新消息状态
            messageRepository.updateStatus(messageId, MessageStatus.PROCESSED);
            
            channel.basicAck(deliveryTag, false);
            
        } catch (Exception e) {
            log.error("Message processing failed", e);
            try {
                // 消息处理失败，进入重试队列
                channel.basicNack(deliveryTag, false, true);
                messageRepository.updateStatus(messageId, MessageStatus.FAILED);
            } catch (IOException ex) {
                log.error("Failed to nack message", ex);
            }
        }
    }
}

// 4. 系统集成适配器
@Component
public class SystemIntegrationAdapter {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private IntegrationConfigRepository configRepository;
    
    // 集成配置缓存
    private final LoadingCache<String, IntegrationConfig> configCache = Caffeine.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build(key -> configRepository.findBySystemCode(key));
    
    public <T> T invoke(String systemCode, String api, Object request, Class<T> responseType) {
        IntegrationConfig config = configCache.get(systemCode);
        if (config == null) {
            throw new BusinessException("系统集成配置不存在");
        }
        
        // 构建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Auth-Token", generateToken(config));
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // 构建请求体
        HttpEntity<?> entity = new HttpEntity<>(request, headers);
        
        // 发送请求
        String url = config.getBaseUrl() + api;
        try {
            ResponseEntity<T> response = restTemplate.exchange(
                url,
                HttpMethod.valueOf(config.getMethod()),
                entity,
                responseType
            );
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new BusinessException("外部系统调用失败");
            }
            
            return response.getBody();
            
        } catch (Exception e) {
            log.error("System integration failed", e);
            throw new BusinessException("系统集成调用异常", e);
        }
    }
}
