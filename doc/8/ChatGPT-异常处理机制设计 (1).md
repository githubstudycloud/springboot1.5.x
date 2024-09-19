## Prompt:
根据我们的讨论，我理解您的主要需求如下： 1. 异常处理策略： - 不允许直接捕获 Exception、Throwable 或 RuntimeException。 - 尽量减少异常捕获的地方，优先使用 throws 声明来传播异常。 2. 异常处理的连续性： - 即使发生异常，也要确保业务逻辑能够继续执行，不中断整个流程。 3. 全局异常管理： - 使用全局异常管理器来处理异常。 - 实现某种回调机制，使异常处理后能够返回到原始的业务逻辑。 4. 异常分类处理： - 对不同类型的异常实现分类处理。 5. 异常监控： - 使用 AOP（面向切面编程）来监控异常，而不是直接捕获它们。 6. 代码简洁性： - 在最少的地方编写异常处理代码，保持主要业务逻辑的清晰性。 7. 灵活性： - 能够处理可预见的异常和不可预见的异常（如突然出现的空指针异常）。 8. 日志记录： - 确保所有异常都被适当地记录下来，便于后续的问题诊断和调试。 总的来说，您希望设计一个既能有效处理各种异常情况，又不影响代码主体结构和可读性的异常处理机制。这个机制应该是全局的、灵活的，并且能够让程序在发生异常后仍然继续执行后续的业务逻辑。 JAVA。

## Response:
基于您的需求，您可以采用以下设计方案来实现一个灵活、高效的异常处理机制，符合您提到的要求：

### 1\. **全局异常处理**

使用 Spring 的 `@ControllerAdvice` 或 `@RestControllerAdvice` 来处理全局的异常。这可以拦截所有未捕获的异常，并在一个地方进行集中处理。

```java
java@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomBusinessException.class)
    public ResponseEntity<?> handleBusinessException(CustomBusinessException ex) {
        // 分类处理业务异常
        log.error("Business error occurred", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneralException(Exception ex) {
        // 处理未预见的异常
        log.error("An unexpected error occurred", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server error");
    }
}
```

### 2\. **使用 AOP 监控异常**

通过 AOP 实现异常的监控，而不是直接捕获。你可以在方法执行时前后切入，用于监控异常，并在捕获到异常后进行日志记录和处理。

```java
java@Aspect
@Component
public class ExceptionMonitoringAspect {

    @Around("execution(* com.example..*(..))")
    public Object monitorExceptions(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return joinPoint.proceed();
        } catch (Exception e) {
            // 记录异常并分类处理
            log.error("Exception in method: " + joinPoint.getSignature(), e);
            throw e; // 继续抛出异常，交给全局异常处理器处理
        }
    }
}
```

### 3\. **异常传播与回调机制**

对于需要业务逻辑继续执行的场景，您可以设计一种回调机制，确保即使发生异常，程序能够继续进行下一步操作。

```java
javapublic interface ErrorHandlerCallback {
    void onError(Exception ex);
}

public void businessMethod(ErrorHandlerCallback callback) throws BusinessException {
    try {
        // 业务逻辑
    } catch (BusinessException e) {
        // 捕获已知异常，并执行回调
        callback.onError(e);
        throw e;
    }
}
```

### 4\. **日志记录与追踪**

每次异常都要记录，方便后续调试。可以使用 SLF4J 或 Logback 来记录详细的异常信息。

```java
javapublic class LogHelper {
    public static void logError(Logger logger, Exception ex) {
        logger.error("Error occurred: ", ex);
    }
}
```

### 5\. **继续执行业务逻辑**

即使发生异常，业务逻辑也不应中断。通过使用类似 CompletableFuture 或 try-catch-finally 的机制，确保程序不会因为某个异常而停止。

```java
javatry {
    // 执行业务操作
} catch (SpecificException ex) {
    // 处理已知异常
} finally {
    // 继续执行剩余业务
}
```

通过这种设计，您可以实现一个全局的异常处理机制，既能灵活应对各种异常，又不会对代码的简洁性造成影响，同时保障业务逻辑的连续性。

## Prompt:
除了TRY FINALLY还有哪些方法，我怎么封装TRY FINALLY，包括并行和串行，记录失败的步骤和日志，并控制哪些失败不影响后面

## Response:
要实现比 `try-finally` 更灵活的错误处理和业务逻辑执行方案，您可以采用以下几种方式：

### 1\. **使用模板方法模式**

通过模板方法模式来封装 `try-finally`，并提供自定义的异常处理逻辑，允许业务逻辑根据需求定义哪些异常会中断执行，哪些异常可以忽略。这是串行执行的基础架构。

#### 代码示例：

```java
javapublic abstract class TaskTemplate {

    public void execute() {
        try {
            doExecute();
        } catch (Exception e) {
            if (!handleError(e)) {
                // 记录错误并继续后续操作
                logError(e);
            }
        } finally {
            afterExecute();
        }
    }

    // 具体的业务逻辑由子类实现
    protected abstract void doExecute() throws Exception;

    // 处理异常，返回 true 表示中断后续操作，false 表示继续执行
    protected boolean handleError(Exception e) {
        return false; // 默认异常不影响后续逻辑
    }

    // 可以在此进行资源清理或其他善后操作
    protected void afterExecute() {
        // 默认实现为空，可由子类覆盖
    }

    private void logError(Exception e) {
        // 日志记录逻辑
        System.err.println("Error occurred: " + e.getMessage());
    }
}

public class MyTask extends TaskTemplate {
    @Override
    protected void doExecute() throws Exception {
        // 业务逻辑
        System.out.println("Executing task");
        throw new RuntimeException("Task failed");
    }

    @Override
    protected boolean handleError(Exception e) {
        // 对特定异常进行分类处理
        return e instanceof RuntimeException; // 中断操作
    }
}

public class Main {
    public static void main(String[] args) {
        new MyTask().execute();
    }
}
```

### 2\. **并行处理和任务失败管理**

使用 `CompletableFuture` 或 `ExecutorService` 可以处理并行任务。通过处理每个任务的完成结果，可以决定哪些失败的任务不影响整体流程，哪些失败需要立即停止。

#### 代码示例：

```java
javaimport java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ParallelTaskManager {

    public static void main(String[] args) {
        // 定义多个并行任务
        CompletableFuture<Void> task1 = CompletableFuture.runAsync(() -> {
            try {
                System.out.println("Task 1 executed");
                // 模拟业务异常
                throw new RuntimeException("Task 1 failed");
            } catch (Exception e) {
                logError(e, "Task 1");
            }
        });

        CompletableFuture<Void> task2 = CompletableFuture.runAsync(() -> {
            System.out.println("Task 2 executed successfully");
        });

        // 等待所有任务完成并处理
        CompletableFuture<Void> allTasks = CompletableFuture.allOf(task1, task2);
        try {
            allTasks.get(); // 等待所有任务完成
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        // 最终操作，如记录日志
        System.out.println("All tasks completed.");
    }

    private static void logError(Exception e, String taskName) {
        System.err.println(taskName + " failed: " + e.getMessage());
    }
}
```

### 3\. **串行任务封装与控制**

对于串行执行的多个步骤，您可以采用 `List<Callable>` 或类似的结构，通过迭代执行每一步，并根据是否捕获到异常，决定是否中断或继续执行。

#### 代码示例：

```java
javaimport java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class SerialTaskManager {

    public static void main(String[] args) {
        List<Callable<Void>> tasks = new ArrayList<>();
        tasks.add(() -> {
            System.out.println("Step 1 executed successfully");
            return null;
        });
        tasks.add(() -> {
            System.out.println("Step 2 executed");
            throw new RuntimeException("Step 2 failed");
        });
        tasks.add(() -> {
            System.out.println("Step 3 executed successfully");
            return null;
        });

        executeTasks(tasks);
    }

    private static void executeTasks(List<Callable<Void>> tasks) {
        for (Callable<Void> task : tasks) {
            try {
                task.call();
            } catch (Exception e) {
                logError(e);
                // 可选择在某些情况下继续或停止
                if (shouldStopOnError(e)) {
                    break;
                }
            }
        }
        System.out.println("All tasks completed.");
    }

    private static void logError(Exception e) {
        System.err.println("Task failed: " + e.getMessage());
    }

    private static boolean shouldStopOnError(Exception e) {
        // 根据具体异常类型决定是否中断任务
        return e instanceof RuntimeException;
    }
}
```

### 4\. **构建执行上下文与失败记录**

创建一个上下文对象，用于存储执行过程中产生的错误、成功或其他状态，并在整个任务执行链中共享这个上下文。

#### 代码示例：

```java
javapublic class TaskExecutionContext {
    private final List<String> errors = new ArrayList<>();

    public void addError(String error) {
        errors.add(error);
    }

    public List<String> getErrors() {
        return errors;
    }
}

public class TaskManager {

    public void executeTask(TaskExecutionContext context) {
        try {
            System.out.println("Executing task");
            throw new RuntimeException("Task failed");
        } catch (Exception e) {
            context.addError(e.getMessage());
        } finally {
            // 可执行善后操作
        }
    }

    public void runAllTasks() {
        TaskExecutionContext context = new TaskExecutionContext();

        // 依次执行多个任务
        executeTask(context);
        executeTask(context);

        // 最后记录错误或其他状态
        System.out.println("Execution finished with errors: " + context.getErrors());
    }
}

public class Main {
    public static void main(String[] args) {
        new TaskManager().runAllTasks();
    }
}
```

### 总结：

* **串行执行**可以使用模板方法模式或 `List<Callable>` 来封装每个步骤，并根据需要进行错误控制。
* **并行执行**可以利用 `CompletableFuture` 来管理任务并处理任务失败的逻辑。
* **失败记录**可以通过上下文对象来统一管理并记录任务执行中的错误信息。

这种设计使得任务执行更加灵活，并且能够根据需要动态决定是否继续后续任务，避免不必要的中断，同时记录所有失败的步骤和相关日志。

## Prompt:
但我不能抓EXCEPTION和THROWABLE，要考虑有些异常我暂时发现不了

## Response:
由于您需要避免直接捕获 `Exception` 和 `Throwable`，同时希望处理一些未预见的异常，我们可以通过更精细的异常分类与自定义处理机制来满足您的需求。在这种情况下，可以使用以下几种策略来实现：

### 1\. **定义具体异常**

尽量不要使用泛化的 `Exception` 或 `Throwable`，而是明确定义具体的业务异常。例如，定义 `BusinessException`、`SystemException` 等具体异常类来应对不同的错误场景。

#### 代码示例：

```java
javapublic class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}

public class SystemException extends RuntimeException {
    public SystemException(String message) {
        super(message);
    }
}
```

在执行任务时，只处理明确的异常类型，避免捕获所有异常。

### 2\. **自定义异常处理器**

为了捕获和处理可能无法预见的异常，您可以在框架中增加一个自定义异常处理器或错误处理回调，通过配置哪些类型的异常可以忽略，哪些需要特别处理。为了避免捕获 `Exception` 和 `Throwable`，可以只针对业务相关的异常类型进行处理，而不是直接使用 `catch (Exception)`。

#### 代码示例：

```java
javapublic class TaskExecutor {

    public void executeTask() throws BusinessException {
        try {
            // 业务逻辑
            System.out.println("Task execution in progress");
            throw new NullPointerException("Unexpected null pointer");
        } catch (NullPointerException e) {
            // 捕获不可预见的异常，并封装为业务异常
            throw new BusinessException("Business logic failed due to null pointer: " + e.getMessage());
        }
    }
}
```

### 3\. **使用 `throws` 传播异常**

当您不能提前捕获所有异常时，使用 `throws` 将异常传播给上层逻辑处理，从而避免直接捕获 `Exception` 或 `Throwable`，同时允许上层处理某些不可预见的异常。

#### 代码示例：

```java
javapublic class ServiceLayer {

    public void executeService() throws BusinessException, SystemException {
        try {
            new TaskExecutor().executeTask();
        } catch (BusinessException e) {
            // 业务异常处理，决定是否继续
            handleBusinessException(e);
        }
    }

    private void handleBusinessException(BusinessException e) {
        // 记录日志或进行业务处理
        System.err.println("Handled BusinessException: " + e.getMessage());
    }
}
```

### 4\. **异常处理的回调机制**

您可以使用回调机制来处理某些不可预见的异常。通过定义回调接口，您可以灵活处理某些不可预见的异常，而不需要捕获 `Exception` 或 `Throwable`。

#### 代码示例：

```java
javapublic interface ExceptionCallback {
    void onBusinessException(BusinessException e);
    void onSystemException(SystemException e);
}

public class TaskExecutorWithCallback {

    private final ExceptionCallback callback;

    public TaskExecutorWithCallback(ExceptionCallback callback) {
        this.callback = callback;
    }

    public void executeTask() throws BusinessException {
        try {
            // 执行业务逻辑
            System.out.println("Executing task");
            throw new NullPointerException("Unexpected null");
        } catch (NullPointerException e) {
            // 将异常转为业务异常，通过回调处理
            callback.onBusinessException(new BusinessException("NullPointerException occurred"));
        }
    }
}
```

#### 回调实现：

```java
javapublic class MyExceptionCallback implements ExceptionCallback {

    @Override
    public void onBusinessException(BusinessException e) {
        // 特定的业务异常处理
        System.out.println("Handling business exception: " + e.getMessage());
    }

    @Override
    public void onSystemException(SystemException e) {
        // 系统异常处理逻辑
        System.out.println("Handling system exception: " + e.getMessage());
    }
}

public class Main {
    public static void main(String[] args) {
        TaskExecutorWithCallback executor = new TaskExecutorWithCallback(new MyExceptionCallback());
        try {
            executor.executeTask();
        } catch (BusinessException e) {
            System.err.println("Unhandled business exception: " + e.getMessage());
        }
    }
}
```

### 5\. **AOP监控不可预见异常**

使用 Spring 的 AOP，您可以在特定业务逻辑执行的前后进行切入，捕获并处理任何异常。通过这样的方法，您可以避免在业务代码中直接编写异常捕获代码，同时不需要捕获所有异常。

#### 代码示例：

```java
java@Aspect
@Component
public class ExceptionMonitoringAspect {

    @Around("execution(* com.example.service..*(..))")
    public Object handleUnseenExceptions(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return joinPoint.proceed();
        } catch (NullPointerException e) {
            // 捕获特定异常并处理
            System.err.println("NullPointerException detected in method: " + joinPoint.getSignature());
            throw new BusinessException("Null pointer encountered in service execution");
        }
    }
}
```

### 6\. **日志和失败步骤记录**

通过定义任务执行上下文（如前面提到的 `TaskExecutionContext`），可以在多个任务中共享状态，并在发生异常时记录相关步骤和错误。这种方法可以避免直接捕获所有异常，并在失败时记录每一步的状态。

#### 代码示例：

```java
javapublic class TaskExecutionContext {
    private final List<String> errorLogs = new ArrayList<>();
    private boolean continueOnFailure = true;

    public void logError(String message) {
        errorLogs.add(message);
    }

    public List<String> getErrorLogs() {
        return errorLogs;
    }

    public boolean isContinueOnFailure() {
        return continueOnFailure;
    }

    public void setContinueOnFailure(boolean continueOnFailure) {
        this.continueOnFailure = continueOnFailure;
    }
}

public class TaskExecutor {
    private final TaskExecutionContext context;

    public TaskExecutor(TaskExecutionContext context) {
        this.context = context;
    }

    public void executeTask() throws BusinessException {
        try {
            // 执行业务逻辑
            System.out.println("Executing task");
            throw new NullPointerException("Unexpected null");
        } catch (NullPointerException e) {
            context.logError("Task failed due to null pointer: " + e.getMessage());
            if (!context.isContinueOnFailure()) {
                throw new BusinessException("Task execution stopped due to failure.");
            }
        }
    }
}
```

### 总结：

* **通过异常分类和细化处理**，避免直接捕获 `Exception` 和 `Throwable`，仅针对具体异常进行捕获。
* **使用回调机制和 `throws` 传播异常**，可以灵活处理不可预见的错误。
* **结合 AOP 异常监控**，实现透明的异常管理，不需要在业务逻辑中直接捕获异常。
* **上下文管理和日志记录**帮助跟踪任务执行过程中失败的步骤，记录异常详情。

这些设计策略可以让您的系统在发生不可预见的异常时更具弹性，同时保持代码的简洁和灵活性。

## Prompt:
大厂是怎么设计的呢，请完整输出设计方案，结合之前讨论

## Response:
大厂通常采用 **稳健性、高扩展性和高性能** 设计方案，处理异常时，尤其在分布式系统、大规模微服务架构中，追求的是 **高可用性、故障隔离、透明的异常处理和强大的监控与日志系统**。结合之前讨论的细节，可以为您设计一个符合大厂标准的异常处理机制。

### 1\. **异常处理的总体设计原则**

1. **异常分类**：确保不同类型的异常被清晰划分为可预见（如业务异常）和不可预见（如系统级别或运行时异常）两大类。

   * **业务异常** (`BusinessException`)：用于表示业务逻辑中的异常，这些通常是可预见的，业务逻辑层应当捕获并处理。
   * **系统异常** (`SystemException`)：通常为不可预见的异常，如网络超时、数据库不可用、NullPointerException 等系统性错误，这些异常需要传递到上层，集中处理。

2. **全局异常管理**：实现一个全局的异常处理器，集中处理不同类型的异常，确保异常被统一记录和监控。

3. **非侵入式监控（AOP）**：通过 AOP 监控系统中异常的发生频率、类型，并自动触发相关告警与修复机制。

4. **回调机制**：保证即使发生异常，某些任务或业务逻辑仍然可以继续执行，或执行降级操作。

5. **日志与监控集成**：将日志记录、监控系统、告警机制集成在一起，使异常被实时监控，并且在某些临界情况下触发自动恢复或人工介入。

### 2\. **整体架构设计**

#### 2.1 **异常处理架构图**

```sql
sql+---------------------------------------------+
|                  API Gateway               |
+---------------------------------------------+
                  |
+---------------------------------------------+
|                Global Exception Handler     |
+---------------------------------------------+
                  |
+----------------------------------------------------+
|                Exception Monitoring (AOP)          |
+----------------------------------------------------+
                  |
+----------------------------------------------------+
|        Business Logic Layer (Service Layer)        |
+----------------------------------------------------+
  /            |                |              \
 Task 1      Task 2           Task 3           ...
 (Error)   (Success)         (Continue)      
```

### 3\. **大厂设计中核心模块细化**

#### 3.1 **异常分类与分层处理**

大厂会对异常进行分层处理，不同层次的异常有不同的处理策略：

* **接口层异常**：接口调用或服务调用的异常，通常由网关进行异常处理。
* **业务逻辑层异常**：由于逻辑错误或业务规则造成的异常，这类异常通过全局异常处理器来捕获。
* **系统层异常**：系统资源不足、数据库不可用、网络问题等系统性异常，这些异常会直接抛出并由上层捕获，触发回滚、降级或熔断机制。

#### 代码示例：

```java
javapublic class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}

public class SystemException extends RuntimeException {
    public SystemException(String message) {
        super(message);
    }
}

public class ValidationException extends BusinessException {
    public ValidationException(String message) {
        super(message);
    }
}
```

#### 3.2 **全局异常管理器**

大厂的异常管理通常使用 `@ControllerAdvice` 来集中处理异常，并结合监控系统（如 Prometheus, ELK）来记录所有的异常信息。通过统一的管理器，可以对不同的异常类型执行不同的处理策略，如记录日志、触发告警或执行自动恢复操作。

#### 代码示例：

```java
java@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusinessException(BusinessException ex) {
        // 业务异常处理逻辑
        log.error("Business exception: ", ex);
        // 通常返回用户友好的信息
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(SystemException.class)
    public ResponseEntity<?> handleSystemException(SystemException ex) {
        // 系统异常处理逻辑
        log.error("System exception: ", ex);
        // 系统异常可能触发告警或自动恢复机制
        alertSystemAdmin(ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleUnknownException(Exception ex) {
        // 未知异常处理逻辑，通常记录并分类为系统异常
        log.error("Unknown exception: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred");
    }

    private void alertSystemAdmin(Exception ex) {
        // 发送告警或触发自愈系统
    }
}
```

#### 3.3 **基于 AOP 的异常监控**

大厂会通过 AOP 来监控系统中的异常，避免在每个方法中手动编写异常处理代码。AOP 允许在业务逻辑之前、之后或抛出异常时执行相应的处理逻辑，从而实现非侵入式的异常监控。

#### 代码示例：

```java
java@Aspect
@Component
public class ExceptionMonitoringAspect {

    @Around("execution(* com.example.service..*(..))")
    public Object monitorExceptions(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return joinPoint.proceed();
        } catch (BusinessException e) {
            log.error("BusinessException caught in method: " + joinPoint.getSignature(), e);
            throw e;  // 再次抛出业务异常
        } catch (SystemException e) {
            log.error("SystemException caught in method: " + joinPoint.getSignature(), e);
            triggerAlert(e);  // 触发告警
            throw e;  // 继续抛出系统异常
        } catch (Exception e) {
            log.error("Unknown exception caught in method: " + joinPoint.getSignature(), e);
            throw new SystemException("Unknown system error");
        }
    }

    private void triggerAlert(Exception e) {
        // 通知运维团队，触发警报
    }
}
```

#### 3.4 **回调与降级机制**

大厂为了保证系统高可用性，在异常处理时常会加入回调或降级机制。降级机制在某些系统组件出现异常时，可以让系统自动切换到降级模式，保证系统的其他部分不受影响。

#### 代码示例：

```java
javapublic interface FallbackHandler {
    void onFallback();
}

public class ServiceExecutor {

    private final FallbackHandler fallbackHandler;

    public ServiceExecutor(FallbackHandler fallbackHandler) {
        this.fallbackHandler = fallbackHandler;
    }

    public void executeService() throws BusinessException {
        try {
            // 业务逻辑
            System.out.println("Executing service logic");
            throw new BusinessException("Business logic failed");
        } catch (BusinessException e) {
            log.error("Error during service execution", e);
            fallbackHandler.onFallback();  // 执行降级操作
            throw e;  // 再次抛出业务异常
        }
    }
}

public class MyFallbackHandler implements FallbackHandler {
    @Override
    public void onFallback() {
        // 降级逻辑
        System.out.println("Executing fallback logic");
    }
}
```

#### 3.5 **日志和监控集成**

大厂通常会将异常记录与监控集成，比如通过 ELK（Elasticsearch, Logstash, Kibana） 或 Prometheus 监控异常情况，并实时可视化展示。每次异常发生时，日志会被及时收集和分析，触发运维告警或自动化修复。

#### 日志系统示例：

* 使用 **Logback** 或 **SLF4J** 进行日志记录，结合 `Filebeat` 或 `Logstash` 将日志发送至 **Elasticsearch**，并通过 **Kibana** 实时监控日志。
* 使用 **Prometheus** 监控异常发生的频率，并在异常激增时触发 **Alertmanager** 警报。

#### 日志配置示例（Logback）：

```xml
xml<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/app.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <fileNamePattern>logs/app-%d{yyyy-MM-dd}.log</fileNamePattern>
        <maxHistory>30</maxHistory>
    </rollingPolicy>
    <encoder>
        <pattern>%d{yyyy-MM-dd HH:mm:ss} %-5level [%thread] %logger{36} - %msg%n</pattern>
    </encoder>
</appender>

<root level="info">
    <appender-ref ref="FILE" />
</root>
```

### 4\. **总结：大厂的异常处理设计方案**

* **异常分类与分层处理**：区分业务异常与系统异常，并根据系统级别进行不同层次的处理。
* **全局异常管理**：使用 `@ControllerAdvice` 统一处理异常，避免在每个方法中捕获异常，同时集成监控与告警机制。
* **AOP监控**：通过 AOP 实现非侵入式异常监控，简化代码逻辑，并统一异常处理流程。
* **降级与回调**：异常发生时通过回调或降级保证业务继续执行，提升系统的容错能力。
* **日志与监控集成**：将日志系统与监控系统集成，实现实时监控和快速响应。

这个设计既符合大厂的高可用性需求，也能确保系统具备强健的容错能力与自动化修复机制。

