
// SchedulerConfig.java
@Configuration
@EnableScheduling
public class SchedulerConfig implements SchedulingConfigurer {

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.setScheduler(taskScheduler());
    }

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("my-scheduled-task-pool-");
        scheduler.initialize();
        return scheduler;
    }
}

// DistributedLockService.java
@Service
public class DistributedLockService {
    private final StringRedisTemplate redisTemplate;

    @Autowired
    public DistributedLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean tryLock(String lockKey, long timeoutInMillis) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(lockKey, "locked", timeoutInMillis, TimeUnit.MILLISECONDS));
    }

    public void releaseLock(String lockKey) {
        redisTemplate.delete(lockKey);
    }
}

// ScheduledTasks.java
@Component
public class ScheduledTasks {
    private final DistributedLockService lockService;

    @Autowired
    public ScheduledTasks(DistributedLockService lockService) {
        this.lockService = lockService;
    }

    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public void scheduledTask() {
        String lockKey = "scheduledTask";
        boolean locked = lockService.tryLock(lockKey, 55000); // 设置锁的超时时间为55秒

        if (locked) {
            try {
                // 执行定时任务的逻辑
                System.out.println("执行定时任务 - " + new Date());
            } finally {
                lockService.releaseLock(lockKey);
            }
        } else {
            System.out.println("无法获取锁,跳过本次执行 - " + new Date());
        }
    }
}

// Application.java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
