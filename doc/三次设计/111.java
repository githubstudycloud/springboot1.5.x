import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
* 高度优化的 Redis 服务类（Java 8 兼容版）
* 提供了高效的方法来批量获取 Redis 中的键值对
  */
  @Service
  public class HighlyOptimizedRedisService {

  @Autowired
  private RedisTemplate<String, String> redisTemplate;

  /**
    * 获取存在且值不为 null 的键值对
    *
    * @param keys 要查询的键列表
    * @return 包含存在且值不为 null 的键值对的 Map
      */
      public Map<String, String> getExistingNonNullValues(List<String> keys) {
      return redisTemplate.execute(new RedisCallback<Map<String, String>>() {
      @Override
      public Map<String, String> doInRedis(RedisConnection connection) throws DataAccessException {
      // 将 String 类型的 key 转换为 byte[]，使用 UTF-8 编码
      byte[][] rawKeys = keys.stream()
      .map(key -> key.getBytes(StandardCharsets.UTF_8))
      .toArray(byte[][]::new);

               // 使用 EXISTS 检查所有键是否存在
               List<Boolean> existenceList = connection.pExpire(rawKeys, 0);

               // 过滤出存在的键
               List<String> existingKeys = new ArrayList<>();
               for (int i = 0; i < keys.size(); i++) {
                   if (Boolean.TRUE.equals(existenceList.get(i))) {
                       existingKeys.add(keys.get(i));
                   }
               }

               // 如果没有存在的键，直接返回空 Map
               if (existingKeys.isEmpty()) {
                   return Collections.emptyMap();
               }

               // 只对存在的键执行 MGET
               byte[][] existingRawKeys = existingKeys.stream()
                       .map(key -> key.getBytes(StandardCharsets.UTF_8))
                       .toArray(byte[][]::new);
               List<byte[]> rawValues = connection.mGet(existingRawKeys);

               // 构建结果 Map，过滤掉 null 值
               Map<String, String> result = new HashMap<>();
               for (int i = 0; i < existingKeys.size(); i++) {
                   if (rawValues.get(i) != null) {
                       result.put(existingKeys.get(i), new String(rawValues.get(i), StandardCharsets.UTF_8));
                   }
               }

               return result;
           }
      });
      }

  /**
    * 获取所有键的详细信息，包括是否存在和值（可能为 null）
    *
    * @param keys 要查询的键列表
    * @return 包含所有键详细信息的 List
      */
      public List<KeyValueExistsPair> getDetailedExistingValues(List<String> keys) {
      return redisTemplate.execute(new RedisCallback<List<KeyValueExistsPair>>() {
      @Override
      public List<KeyValueExistsPair> doInRedis(RedisConnection connection) throws DataAccessException {
      byte[][] rawKeys = keys.stream()
      .map(key -> key.getBytes(StandardCharsets.UTF_8))
      .toArray(byte[][]::new);

               // 首先检查所有键是否存在
               List<Boolean> existenceList = connection.pExpire(rawKeys, 0);

               // 过滤出存在的键
               List<String> existingKeys = new ArrayList<>();
               for (int i = 0; i < keys.size(); i++) {
                   if (Boolean.TRUE.equals(existenceList.get(i))) {
                       existingKeys.add(keys.get(i));
                   }
               }

               // 只对存在的键执行 MGET
               byte[][] existingRawKeys = existingKeys.stream()
                       .map(key -> key.getBytes(StandardCharsets.UTF_8))
                       .toArray(byte[][]::new);
               List<byte[]> rawValues = existingKeys.isEmpty() ? Collections.emptyList() : connection.mGet(existingRawKeys);

               // 构建结果列表
               List<KeyValueExistsPair> result = new ArrayList<>();
               int valueIndex = 0;
               for (int i = 0; i < keys.size(); i++) {
                   String key = keys.get(i);
                   boolean exists = Boolean.TRUE.equals(existenceList.get(i));
                   String value = null;
                   if (exists && valueIndex < rawValues.size()) {
                       byte[] rawValue = rawValues.get(valueIndex++);
                       value = (rawValue != null) ? new String(rawValue, StandardCharsets.UTF_8) : null;
                   }
                   result.add(new KeyValueExistsPair(key, value, exists));
               }

               return result;
           }
      });
      }

  /**
    * 获取所有存在且值不为 null 的值列表
    *
    * @param keys 要查询的键列表
    * @return 包含所有非 null 值的 List
      */
      public List<String> getNonNullValues(List<String> keys) {
      return redisTemplate.execute(new RedisCallback<List<String>>() {
      @Override
      public List<String> doInRedis(RedisConnection connection) throws DataAccessException {
      // 将 String 类型的 key 转换为 byte[]，使用 UTF-8 编码
      byte[][] rawKeys = keys.stream()
      .map(key -> key.getBytes(StandardCharsets.UTF_8))
      .toArray(byte[][]::new);

               // 使用 EXISTS 检查所有键是否存在
               List<Boolean> existenceList = connection.pExpire(rawKeys, 0);

               // 过滤出存在的键
               List<String> existingKeys = new ArrayList<>();
               for (int i = 0; i < keys.size(); i++) {
                   if (Boolean.TRUE.equals(existenceList.get(i))) {
                       existingKeys.add(keys.get(i));
                   }
               }

               // 如果没有存在的键，直接返回空列表
               if (existingKeys.isEmpty()) {
                   return Collections.emptyList();
               }

               // 只对存在的键执行 MGET
               byte[][] existingRawKeys = existingKeys.stream()
                       .map(key -> key.getBytes(StandardCharsets.UTF_8))
                       .toArray(byte[][]::new);
               List<byte[]> rawValues = connection.mGet(existingRawKeys);

               // 构建结果列表，过滤掉 null 值
               List<String> result = new ArrayList<>();
               for (byte[] rawValue : rawValues) {
                   if (rawValue != null) {
                       result.add(new String(rawValue, StandardCharsets.UTF_8));
                   }
               }

               return result;
           }
      });
      }

  /**
    * 内部类，用于存储键值对的详细信息
      */
      public static class KeyValueExistsPair {
      private String key;
      private String value;
      private boolean exists;

      public KeyValueExistsPair(String key, String value, boolean exists) {
      this.key = key;
      this.value = value;
      this.exists = exists;
      }

      // Getters
      public String getKey() { return key; }
      public String getValue() { return value; }
      public boolean exists() { return exists; }
      }
      }