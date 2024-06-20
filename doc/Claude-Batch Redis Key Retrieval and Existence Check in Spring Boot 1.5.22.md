import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RedisService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 判断多个 key 是否存在
    public boolean existsMultipleKeys(List<String> keys) {
        return redisTemplate.execute((RedisCallback<Boolean>) connection -> {
            byte[][] keyArray = keys.stream()
                    .map(key -> key.getBytes())
                    .toArray(byte[][]::new);
            return connection.exists(keyArray).booleanValue() == keys.size();
        });
    }
}