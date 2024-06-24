以下是Spring Boot 1.5.x到3.x版本对不同Redis版本的支持情况的表格：

| Spring Boot 版本 | 支持的 Redis 版本              | 备注                                          |
|------------------|------------------------------|---------------------------------------------|
| 1.5.x            | 2.8.x, 3.x, 4.x              | 默认支持Jedis，使用Spring Data Redis 1.x     |
| 2.0.x            | 3.x, 4.x, 5.x                | 默认支持Lettuce和Jedis，使用Spring Data Redis 2.0  |
| 2.1.x            | 3.x, 4.x, 5.x                | 默认支持Lettuce和Jedis，使用Spring Data Redis 2.1  |
| 2.2.x            | 3.x, 4.x, 5.x                | 默认支持Lettuce和Jedis，使用Spring Data Redis 2.2  |
| 2.3.x            | 3.x, 4.x, 5.x, 6.x           | 默认支持Lettuce和Jedis，使用Spring Data Redis 2.3  |
| 2.4.x            | 3.x, 4.x, 5.x, 6.x           | 默认支持Lettuce和Jedis，使用Spring Data Redis 2.4  |
| 2.5.x            | 4.x, 5.x, 6.x                | 默认支持Lettuce和Jedis，使用Spring Data Redis 2.5  |
| 2.6.x            | 4.x, 5.x, 6.x                | 默认支持Lettuce和Jedis，使用Spring Data Redis 2.6  |
| 2.7.x            | 5.x, 6.x                     | 默认支持Lettuce和Jedis，使用Spring Data Redis 2.7  |
| 2.8.x            | 5.x, 6.x                     | 默认支持Lettuce和Jedis，使用Spring Data Redis 2.8  |
| 3.0.x            | 6.x, 7.x                     | 默认支持Lettuce和Jedis，使用Spring Data Redis 3.0  |

请注意，这只是一个概览，具体版本之间的兼容性可能还需要参考Spring Boot和Spring Data Redis的官方文档。对于每个版本的具体使用，建议查看对应的发行说明和文档以获取最新和最准确的信息。