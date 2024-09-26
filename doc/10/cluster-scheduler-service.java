// pom.xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
</dependencies>

// Application.java
@SpringBootApplication
@EnableScheduling
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

// SchedulerConfig.java
@Configuration
public class SchedulerConfig implements SchedulingConfigurer {
    @Autowired
    private TaskScheduler taskScheduler;

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.setScheduler(taskScheduler);
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
    @Autowired
    private StringRedisTemplate redisTemplate;

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
    @Autowired
    private DistributedLockService lockService;

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

// application.properties
spring.redis.host=localhost
spring.redis.port=6379
