package xyz.antiz.kaal.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import xyz.antiz.kaal.config.RateLimiterProperties;

import java.time.Clock;

@Service
public class RedisTokenBucketService {

    private final JedisPool jedisPool;
    private final RateLimiterProperties properties;
    private final Clock clock;

    @Autowired
    public RedisTokenBucketService(JedisPool jedisPool,
                                   RateLimiterProperties properties) {
        this.jedisPool = jedisPool;
        this.properties = properties;
        this.clock = Clock.systemUTC();
    }

    RedisTokenBucketService(JedisPool jedisPool,
                            RateLimiterProperties properties,
                            Clock clock) {
        this.jedisPool = jedisPool;
        this.properties = properties;
        this.clock = clock;
    }

    public boolean tryConsume(String clientId) {

        String tokensKey = "rate_limiter:tokens:" + clientId;
        String timeKey = "rate_limiter:last_refill:" + clientId;

        try (Jedis jedis = jedisPool.getResource()) {

            long now = clock.millis();

            String lastTimeStr = jedis.get(timeKey);
            long lastTime = lastTimeStr == null ? now : Long.parseLong(lastTimeStr);

            long elapsed = now - lastTime;
            long tokensToAdd = (elapsed * properties.getRefillRate()) / 1000;

            String tokenStr = jedis.get(tokensKey);
            long currentTokens = tokenStr == null
                    ? properties.getCapacity()
                    : Long.parseLong(tokenStr);

            long newTokens = Math.min(
                    properties.getCapacity(),
                    currentTokens + tokensToAdd
            );

            // Preserve fractional elapsed time so high-frequency calls still refill over time.
            long updatedLastTime = lastTime;
            if (tokensToAdd > 0) {
                long millisConsumedForRefill = (tokensToAdd * 1000) / properties.getRefillRate();
                updatedLastTime = lastTime + millisConsumedForRefill;
            }

            if (newTokens <= 0) {
                jedis.set(tokensKey, "0");
                jedis.set(timeKey, String.valueOf(updatedLastTime));
                return false;
            }

            jedis.set(tokensKey, String.valueOf(newTokens - 1));
            jedis.set(timeKey, String.valueOf(updatedLastTime));
            return true;
        }
    }

    public long getCapacity(String clientId) {
        return properties.getCapacity();
    }

    public long getAvailableTokens(String clientId) {

        String tokensKey = "rate_limiter:tokens:" + clientId;

        try (Jedis jedis = jedisPool.getResource()) {

            String tokenStr = jedis.get(tokensKey);
            return tokenStr == null
                    ? properties.getCapacity()
                    : Long.parseLong(tokenStr);
        }
    }
}
