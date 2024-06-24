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
Spring Boot 1.5.x 几乎不可能支持 Redis 6.x。这里有几个重要原因：

1. 版本差距：
   Spring Boot 1.5.x 是一个相对较旧的版本，最后一个版本（1.5.22）发布于2019年。而 Redis 6.0 是在2020年5月发布的。这种时间上的差距意味着 Spring Boot 1.5.x 在设计时并没有考虑到 Redis 6.x。

2. 官方支持范围：
   Spring Boot 1.5.x 官方支持的 Redis 版本范围通常到 3.2.x。即使在最宽松的情况下，也很少见到它支持超过 4.x 的版本。

3. 底层依赖：
   Spring Boot 使用 Spring Data Redis 来与 Redis 交互。Spring Boot 1.5.x 搭配的 Spring Data Redis 版本太旧，无法兼容 Redis 6.x 的新特性和协议变化。

4. 功能兼容性：
   Redis 6.x 引入了许多新特性（如 SSL 支持、客户端缓存、多线程 I/O 等），这些都需要客户端库的相应支持，而 Spring Boot 1.5.x 的相关依赖无法提供这种支持。

5. 安全性考虑：
   使用如此老旧的 Spring Boot 版本（1.5.x）可能存在安全风险，因为它已经不再接收安全更新。

6. 实际操作困难：
   即使强行尝试将 Spring Boot 1.5.x 与 Redis 6.x 配合使用，也很可能遇到各种运行时错误和兼容性问题。

建议：
1. 升级方案：如果你正在考虑使用 Redis 6.x，强烈建议升级你的 Spring Boot 版本。理想情况下，升级到 Spring Boot 2.4.x 或更高版本。

2. 如果无法升级 Spring Boot：考虑使用与 Spring Boot 1.5.x 兼容的较旧版本的 Redis（如 3.x 或 4.x 版本）。

3. 渐进式升级：如果项目规模较大，可以考虑先升级 Spring Boot 到 2.x 系列，然后再逐步提高 Redis 版本。

4. 全面测试：无论采取何种方案，都需要进行全面的测试，确保系统的稳定性和兼容性。

总之，Spring Boot 1.5.x 和 Redis 6.x 的组合是不推荐的，也很可能无法正常工作。在实际应用中，应该避免使用这种组合，而应该考虑升级策略或使用兼容的版本组合。
