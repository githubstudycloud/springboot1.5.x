## Prompt:
如何设置容器时区是东八区

## Response:
要设置容器的时区为东八区(UTC+8),可以在docker-compose.yml文件中为每个服务添加环境变量TZ=Asia/Shanghai。

例如:

```yml
Copyredis:
  image: redis:3.2 
  container_name: redis
  environment:
    - TZ=Asia/Shanghai
  ...

rabbitmq:
  image: rabbitmq:3.6-management
  container_name: rabbitmq  
  environment:
    - RABBITMQ_DEFAULT_USER=guest
    - RABBITMQ_DEFAULT_PASS=guest
    - RABBITMQ_PLUGINS_ENABLE=rabbitmq_management,rabbitmq_tracing
    - TZ=Asia/Shanghai
  ...
```

为需要设置时区的服务都加上environment下的- TZ=Asia/Shanghai即可。Asia/Shanghai对应的是东八区时间。

这样设置后,容器内的时区将会是东八区的时间,与宿主机所在时区无关。程序获取的时间也是正确的东八区时间。

另外,还有一种方法是挂载宿主机的/etc/localtime文件到容器内:

```yml
Copyvolumes:
  - /etc/localtime:/etc/localtime:ro
```

这种方式容器的时区将与宿主机保持一致,前提是宿主机时区本身就是东八区。这种方式略微影响容器的可移植性。

总之,通过设置TZ环境变量是在compose文件中设置容器时区的最佳实践,简单有效。

## Prompt:
怎么把rabbitmq的日志跟踪默认开启，把日志设置和日志记录保存到容器外

## Response:
要在RabbitMQ容器中默认开启日志跟踪(Tracing),并将日志文件保存到容器外,可以按照以下步骤配置:

1. 在rabbitmq.conf配置文件中开启日志跟踪:

```conf
Copylisteners.tcp.default = 5672
management.listener.port = 15672
management.listener.ssl = false

logtrace.console = true
log.dir = /var/log/rabbitmq/trace
```

将logtrace.console设为true表示启用日志跟踪,log.dir指定了日志文件存放的目录。

2. 在docker-compose.yml中挂载日志目录,并设置日志跟踪相关的环境变量:

```yml
Copyrabbitmq:
  image: rabbitmq:3.6-management
  container_name: rabbitmq
  ports:
    - 5672:5672 
    - 15672:15672
  volumes:
    - ./rabbitmq/rabbitmq.conf:/etc/rabbitmq/rabbitmq.conf
    - rabbitmq_data:/var/lib/rabbitmq
    - ./rabbitmq/trace:/var/log/rabbitmq/trace
  environment:
    - RABBITMQ_DEFAULT_USER=guest
    - RABBITMQ_DEFAULT_PASS=guest 
    - RABBITMQ_TRACE=true
    - RABBITMQ_TRACE_LOG_LEVEL=info   
    - TZ=Asia/Shanghai
  networks:
    - backend
```

volumes中新增一行,将容器内的/var/log/rabbitmq/trace目录挂载到宿主机的./rabbitmq/trace目录。

environment中添加了以下变量:

* RABBITMQ\_TRACE=true 表示开启日志跟踪功能
* RABBITMQ\_TRACE\_LOG\_LEVEL=info 设置日志等级为info

这样配置后,启动RabbitMQ容器时就会默认开启日志跟踪功能,日志文件会自动保存到宿主机的./rabbitmq/trace目录中,方便在容器外查看和管理日志。

需要注意日志文件会持续增长,需要定期进行清理,避免占用过多磁盘空间。可以通过添加一个日志切割的定时任务来自动管理日志文件。

