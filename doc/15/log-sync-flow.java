// 1. 分布式日志管理系统
@Service
@Slf4j
public class DistributedLogManager {
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Autowired
    private ElasticsearchClient esClient;
    
    @Value("${log.kafka.topic}")
    private String logTopic;
    
    private static final int BATCH_SIZE = 1000;
    private final BlockingQueue<LogEvent> logQueue = new LinkedBlockingQueue<>(10000);
    
    @PostConstruct
    public void init() {
        // 启动日志处理线程
        startLogProcessor();
    }
    
    public void logEvent(LogEvent event) {
        try {
            // 添加上下文信息
            enrichLogEvent(event);
            // 异步写入队列
            boolean offered = logQueue.offer(event, 100, TimeUnit.MILLISECONDS);
            if (!offered) {
                log.warn("Log queue is full, discarding log event");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Failed to queue log event", e);
        }
    }
    
    private void enrichLogEvent(LogEvent event) {
        event.setTimestamp(System.currentTimeMillis());
        event.setHost(getHostName());
        event.setTraceId(TraceContext.getCurrentTraceId());
        event.setThread(Thread.currentThread().getName());
        
        // 添加MDC上下文
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        if (contextMap != null) {
            event.setContext(contextMap);
        }
    }
    
    private void startLogProcessor() {
        Thread processor = new Thread(() -> {
            List<LogEvent> batch = new ArrayList<>(BATCH_SIZE);
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // 批量收集日志
                    drainLogQueue(batch);
                    if (!batch.isEmpty()) {
                        // 批量处理日志
                        processLogBatch(batch);
                        batch.clear();
                    }
                } catch (Exception e) {
                    log.error("Error processing log batch", e);
                }
            }
        }, "log-processor");
        processor.start();
    }
    
    private void processLogBatch(List<LogEvent> batch) {
        // 根据日志级别分组
        Map<String, List<LogEvent>> logsByLevel = batch.stream()
            .collect(Collectors.groupingBy(LogEvent::getLevel));
            
        // 错误日志单独处理
        List<LogEvent> errorLogs = logsByLevel.get("ERROR");
        if (errorLogs != null && !errorLogs.isEmpty()) {
            processErrorLogs(errorLogs);
        }
        
        // 发送到Kafka
        String batchJson = JsonUtils.toJson(batch);
        kafkaTemplate.send(logTopic, batchJson);
        
        // 异步写入Elasticsearch
        asyncWriteToES(batch);
    }
    
    private void processErrorLogs(List<LogEvent> errorLogs) {
        // 错误日志聚合分析
        Map<String, Long> errorStats = errorLogs.stream()
            .collect(Collectors.groupingBy(LogEvent::getErrorType, Collectors.counting()));
            
        // 检查错误阈值
        errorStats.forEach((errorType, count) -> {
            if (count >= getErrorThreshold(errorType)) {
                // 触发告警
                triggerAlert(errorType, count);
            }
        });
    }
    
    private void asyncWriteToES(List<LogEvent> logs) {
        CompletableFuture.runAsync(() -> {
            try {
                BulkRequest.Builder bulkRequest = new BulkRequest.Builder();
                
                for (LogEvent log : logs) {
                    bulkRequest.operations(op -> op
                        .index(idx -> idx
                            .index(getLogIndex())
                            .document(log)
                        )
                    );
                }
                
                esClient.bulk(bulkRequest.build());
            } catch (Exception e) {
                log.error("Failed to write logs to Elasticsearch", e);
            }
        });
    }
}

// 2. 数据同步服务
@Service
public class DataSyncService {
    
    @Autowired
    private DataSourceManager dataSourceManager;
    
    @Autowired
    private MessageBus messageBus;
    
    private final BlockingQueue<SyncTask> syncQueue = new LinkedBlockingQueue<>();
    private final Map<String, AtomicLong> syncProgress = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        // 启动同步工作线程池
        startSyncWorkers();
    }
    
    public void submitSyncTask(SyncTask task) {
        // 初始化同步进度
        syncProgress.put(task.getTaskId(), new AtomicLong(0));
        // 提交同步任务
        syncQueue.offer(task);
    }
    
    private void startSyncWorkers() {
        int workerCount = Runtime.getRuntime().availableProcessors();
        for (int i = 0; i < workerCount; i++) {
            Thread worker = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        SyncTask task = syncQueue.take();
                        processSync(task);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }, "sync-worker-" + i);
            worker.start();
        }
    }
    
    private void processSync(SyncTask task) {
        try {
            // 获取源和目标数据源
            DataSource sourceDs = dataSourceManager.getDataSource(task.getSourceDb());
            DataSource targetDs = dataSourceManager.getDataSource(task.getTargetDb());
            
            // 计算数据差异
            List<DataDiff> diffs = calculateDiffs(sourceDs, targetDs, task);
            
            // 按批次同步数据
            for (List<DataDiff> batch : Lists.partition(diffs, task.getBatchSize())) {
                syncBatch(batch, sourceDs, targetDs);
                // 更新进度
                updateProgress(task.getTaskId(), batch.size());
            }
            
            // 发布同步完成事件
            publishSyncComplete(task);
            
        } catch (Exception e) {
            log.error("Sync failed for task: " + task.getTaskId(), e);
            handleSyncError(task, e);
        }
    }
    
    private List<DataDiff> calculateDiffs(DataSource sourceDs, DataSource targetDs, 
                                        SyncTask task) {
        // 使用临时表存储数据签名
        String tempTable = createTempTable(task);
        
        // 计算源数据签名
        calculateSourceSignatures(sourceDs, tempTable, task);
        
        // 计算目标数据签名
        calculateTargetSignatures(targetDs, tempTable, task);
        
        // 比较差异
        return findDifferences(tempTable);
    }
    
    private void syncBatch(List<DataDiff> batch, DataSource sourceDs, 
                          DataSource targetDs) {
        TransactionTemplate sourceTx = new TransactionTemplate(
            new DataSourceTransactionManager(sourceDs));
        TransactionTemplate targetTx = new TransactionTemplate(
            new DataSourceTransactionManager(targetDs));
            
        targetTx.execute(status -> {
            for (DataDiff diff : batch) {
                switch (diff.getType()) {
                    case INSERT:
                        handleInsert(diff, sourceDs, targetDs);
                        break;
                    case UPDATE:
                        handleUpdate(diff, sourceDs, targetDs);
                        break;
                    case DELETE:
                        handleDelete(diff, targetDs);
                        break;
                }
            }
            return null;
        });
    }
}

// 3. 业务流程引擎
@Service
public class BusinessFlowEngine {
    
    @Autowired
    private FlowDefinitionRepository flowRepo;
    
    @Autowired
    private FlowInstanceRepository instanceRepo;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    public FlowInstance startFlow(String flowCode, Map<String, Object> params) {
        // 获取流程定义
        FlowDefinition flow = flowRepo.findByCode(flowCode);
        if (flow == null) {
            throw new BusinessException("Flow not found: " + flowCode);
        }
        
        // 创建流程实例
        FlowInstance instance = new FlowInstance();
        instance.setFlowCode(flowCode);
        instance.setStatus(FlowStatus.RUNNING);
        instance.setVariables(params);
        instanceRepo.save(instance);
        
        // 发布流程开始事件
        eventPublisher.publishEvent(new FlowStartEvent(instance));
        
        // 执行开始节点
        executeNode(instance, flow.getStartNode());
        
        return instance;
    }
    
    private void executeNode(FlowInstance instance, FlowNode node) {
        try {
            // 执行节点前处理
            preExecute(instance, node);
            
            // 执行节点逻辑
            NodeExecutionResult result = executeNodeLogic(instance, node);
            
            // 处理执行结果
            handleExecutionResult(instance, node, result);
            
            // 执行节点后处理
            postExecute(instance, node);
            
        } catch (Exception e) {
            handleNodeError(instance, node, e);
        }
    }
    
    private NodeExecutionResult executeNodeLogic(FlowInstance instance, FlowNode node) {
        NodeExecutor executor = getNodeExecutor(node.getType());
        return executor.execute(instance, node);
    }
    
    private void handleExecutionResult(FlowInstance instance, FlowNode node, 
                                     NodeExecutionResult result) {
        if (result.isSuccess()) {
            // 确定下一个节点
            FlowNode nextNode = determineNextNode(instance, node, result);
            if (nextNode != null) {
                // 执行下一个节点
                executeNode(instance, nextNode);
            } else {
                // 流程结束
                completeFlow(instance);
            }
        } else {
            // 处理执行失败
            handleNodeFailure(instance, node, result);
        }
    }
    
    private FlowNode determineNextNode(FlowInstance instance, FlowNode currentNode,
                                     NodeExecutionResult result) {
        // 根据条件确定下一个节点
        if (currentNode.getType() == NodeType.GATEWAY) {
            return evaluateGatewayConditions(instance, currentNode);
        } else {
            return currentNode.getNextNode();
        }
    }
}
