// 1. 统一文件存储服务
@Service
@Slf4j
public class FileStorageService {
    
    @Value("${storage.type:local}")
    private String storageType;
    
    @Autowired
    private MinioClient minioClient;
    
    @Autowired
    private FileStorageProperties properties;
    
    public String upload(MultipartFile file) {
        String fileName = generateFileName(file);
        try {
            switch (storageType) {
                case "local":
                    return uploadLocal(file, fileName);
                case "minio":
                    return uploadMinio(file, fileName);
                default:
                    throw new RuntimeException("Unsupported storage type");
            }
        } catch (Exception e) {
            log.error("File upload failed", e);
            throw new BusinessException("文件上传失败");
        }
    }
    
    private String uploadMinio(MultipartFile file, String fileName) {
        try {
            // 检查bucket是否存在
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(properties.getBucketName())
                .build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(properties.getBucketName())
                    .build());
            }
            
            // 上传文件
            minioClient.putObject(PutObjectArgs.builder()
                .bucket(properties.getBucketName())
                .object(fileName)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build());
                
            return fileName;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to MinIO", e);
        }
    }
    
    // 支持断点续传的文件下载
    public void download(String fileName, HttpServletResponse response) throws IOException {
        String filePath = properties.getStoragePath() + fileName;
        File file = new File(filePath);
        
        if (!file.exists()) {
            throw new BusinessException("文件不存在");
        }
        
        String range = request.getHeader("Range");
        long start = 0, end = file.length() - 1;
        
        if (range != null && range.startsWith("bytes=")) {
            String[] ranges = range.substring(6).split("-");
            start = Long.parseLong(ranges[0]);
            if (ranges.length > 1) {
                end = Long.parseLong(ranges[1]);
            }
        }
        
        long contentLength = end - start + 1;
        response.setHeader("Content-Range", String.format("bytes %d-%d/%d", start, end, file.length()));
        response.setHeader("Content-Length", String.valueOf(contentLength));
        response.setHeader("Accept-Ranges", "bytes");
        response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
             OutputStream outputStream = response.getOutputStream()) {
            randomAccessFile.seek(start);
            byte[] buffer = new byte[4096];
            long remaining = contentLength;
            while (remaining > 0) {
                int read = randomAccessFile.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                outputStream.write(buffer, 0, read);
                remaining -= read;
            }
        }
    }
}

// 2. 分布式定时任务实现
@Component
public class DistributedScheduler {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private TaskExecutor taskExecutor;
    
    private static final String SCHEDULER_LEADER_KEY = "scheduler:leader";
    private static final long LEADER_EXPIRE_SECONDS = 30;
    
    @PostConstruct
    public void init() {
        // 启动选主任务
        taskExecutor.execute(this::startLeaderElection);
    }
    
    private void startLeaderElection() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                boolean isLeader = redisTemplate.opsForValue()
                    .setIfAbsent(SCHEDULER_LEADER_KEY, getNodeId(), LEADER_EXPIRE_SECONDS, TimeUnit.SECONDS);
                
                if (isLeader) {
                    // 作为主节点执行调度任务
                    executeScheduledTasks();
                }
                
                Thread.sleep(LEADER_EXPIRE_SECONDS * 1000 / 3);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    @Scheduled(fixedRate = 1000)
    public void executeScheduledTasks() {
        List<ScheduledTask> tasks = taskRepository.findDueTasks();
        for (ScheduledTask task : tasks) {
            try {
                // 获取任务锁
                String lockKey = "task:lock:" + task.getId();
                boolean locked = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "LOCKED", 5, TimeUnit.MINUTES);
                
                if (locked) {
                    taskExecutor.execute(() -> {
                        try {
                            // 执行任务
                            executeTask(task);
                            // 更新下次执行时间
                            updateNextExecutionTime(task);
                        } finally {
                            redisTemplate.delete(lockKey);
                        }
                    });
                }
            } catch (Exception e) {
                log.error("Failed to execute task: " + task.getId(), e);
            }
        }
    }
}

// 3. 分布式ID生成服务
@Service
public class IdGeneratorService {
    
    private final SnowflakeIdWorker worker1;
    private final SnowflakeIdWorker worker2;
    
    public IdGeneratorService(
            @Value("${snowflake.datacenter-id}") long datacenterId,
            @Value("${snowflake.worker-id}") long workerId) {
        this.worker1 = new SnowflakeIdWorker(workerId, datacenterId);
        this.worker2 = new SnowflakeIdWorker(workerId + 1, datacenterId);
    }
    
    /**
     * 获取ID（双工作者模式，提高性能）
     */
    public long nextId() {
        return Thread.currentThread().getId() % 2 == 0 ? 
            worker1.nextId() : worker2.nextId();
    }
    
    /**
     * 批量获取ID
     */
    public List<Long> batchNextId(int size) {
        List<Long> ids = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ids.add(nextId());
        }
        return ids;
    }
}

// 4. 统一监控数据采集
@Aspect
@Component
public class MetricsCollector {
    
    @Autowired
    private MeterRegistry registry;
    
    private final Map<String, Timer> apiTimers = new ConcurrentHashMap<>();
    
    @Around("@annotation(metrics)")
    public Object collectMetrics(ProceedingJoinPoint point, Metrics metrics) throws Throwable {
        Timer timer = apiTimers.computeIfAbsent(
            metrics.name(),
            name -> Timer.builder(name)
                .tags(metrics.tags())
                .register(registry)
        );
        
        return timer.record(() -> {
            try {
                return point.proceed();
            } catch (Throwable throwable) {
                registry.counter(metrics.name() + ".error")
                    .increment();
                throw throwable;
            }
        });
    }
    
    @Scheduled(fixedRate = 60000)
    public void reportMetrics() {
        // 收集JVM指标
        registry.gauge("jvm.memory.used", 
            Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        registry.gauge("jvm.memory.total", 
            Runtime.getRuntime().totalMemory());
        
        // 收集业务指标
        collectBusinessMetrics();
    }
}
