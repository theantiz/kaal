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

    /**
     * Attempts to consume one token from the client's bucket.
     *
     * The bucket is stored in Redis using two keys per client:
     * one for the current token count and one for the last refill time.
     * Refill is time-based, so a client slowly regains tokens even if no requests are made.
     */
    public boolean tryConsume(String clientId) {

        // Separate Redis keys per client keep each token bucket independent.
        String tokensKey = "rate_limiter:tokens:" + clientId;
        String timeKey = "rate_limiter:last_refill:" + clientId;

        try (Jedis jedis = jedisPool.getResource()) {

            // Current wall-clock time is the basis for the refill calculation.
            long now = clock.millis();

            // If we have never seen this client before, treat the current moment as the last refill time.
            String lastTimeStr = jedis.get(timeKey);
            long lastTime = lastTimeStr == null ? now : Long.parseLong(lastTimeStr);

            // Convert elapsed time into tokens using the configured refill rate.
            long elapsed = now - lastTime;
            long tokensToAdd = (elapsed * properties.getRefillRate()) / 1000;

            // New clients start at full capacity; existing clients resume from their stored balance.
            String tokenStr = jedis.get(tokensKey);
            long currentTokens = tokenStr == null
                    ? properties.getCapacity()
                    : Long.parseLong(tokenStr);

            // Never allow the bucket to exceed its configured capacity.
            long newTokens = Math.min(
                    properties.getCapacity(),
                    currentTokens + tokensToAdd
            );

            // Preserve fractional elapsed time so very frequent requests still accumulate refill credit.
            long updatedLastTime = lastTime;
            if (tokensToAdd > 0) {
                long millisConsumedForRefill = (tokensToAdd * 1000) / properties.getRefillRate();
                updatedLastTime = lastTime + millisConsumedForRefill;
            }

            // If the bucket is empty after refill, the request must be rejected.
            if (newTokens <= 0) {
                jedis.set(tokensKey, "0");
                jedis.set(timeKey, String.valueOf(updatedLastTime));
                return false;
            }

            // Consume exactly one token for the current request and persist the updated state.
            jedis.set(tokensKey, String.valueOf(newTokens - 1));
            jedis.set(timeKey, String.valueOf(updatedLastTime));
            return true;
        }
    }

    public long getCapacity(String clientId) {
        return properties.getCapacity();
    }

    /**
     * Returns the currently stored token count for the client.
     * If the client has not made a request yet, the bucket is considered full.
     */
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
