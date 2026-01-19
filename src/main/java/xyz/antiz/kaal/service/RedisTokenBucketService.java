package xyz.antiz.kaal.service;

/**
 * Redis key patterns and schema for Token Bucket Rate Limiter
 * <p>
 * NOMENCLATURE:
 * rate_limiter:{type}:{identifier}
 * - type:      "token" | "last_refill"
 * - identifier: clientId or IP address (String)
 * <p>
 * KEY PATTERNS:
 * 1. rate_limiter:token:{clientId}     -> Current available token count (Integer)
 * 2. rate_limiter:last_refill:{clientId} -> Last refill timestamp (Unix epoch, Long)
 * <p>
 * EXAMPLE USAGE:
 * Client IP: 192.168.1.100
 * ┌─────────────────────────────────────┐
 * │ rate_limiter:token:192.168.1.100   │ -> "7"    (7 tokens available)
 * │ rate_limiter:last_refill:192.168.1.100 │ -> "1640995200000" (refill timestamp)
 * └─────────────────────────────────────┘
 * <p>
 * DATA TYPES:
 * - Token count:    String representation of Integer (7 → "7")
 * - Timestamp:      String representation of Long (Unix epoch milliseconds)
 * <p>
 * STORAGE STRATEGY:
 * - Atomic Redis operations ensure thread-safety
 * - TTL auto-expiry prevents stale entries
 * - IP-based identifiers enable stateless rate limiting
 * <p>
 * THREAD SAFETY: Redis atomic operations (INCR, SETNX, EXPIRE)
 * SCALABILITY:  Horizontal scaling across Redis cluster/shard
 */


import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import xyz.antiz.kaal.config.RateLimiterProperties;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Jedis;
import java.time.Instant;

//store token bucket state in redis
//manage tokens per client
//handle token refill based on time
//provide rate limiting logic.
@Service
@RequiredArgsConstructor
public class RedisTokenBucketService {

    private final JedisPool jedisPool;

    private final RateLimiterProperties properties;

    private final String TOKENS_KEY_PREFIX = "rate_limiter:tokens:";
    private static final String LAST_REFILL_KEY_PREFIX = "rate_limiter:last_refill:";

    //Pattern
    //rate_limiter:{type}:{clientId}

    //rate_limiter:tokens:192.168.1.100 > Current Token Count
    //rate_limiter:last_refill:192.168.1.100 > Last Refill Timestamp

    //rate_limiter:tokens:192.168.1.100 > "7"
    // rate_limiter:last_refill:192.168.1.100 > "12324342342"

    public boolean isAllowed(String clientId) {

        String tokenKey = TOKENS_KEY_PREFIX + clientId;

        try (Jedis jedis = jedisPool.getResource()) {
            refillTokens(clientId, jedis);

            String tokenStr = jedis.get(tokenKey);

            long currentTokens = tokenStr != null ? Long.parseLong(tokenStr) : properties.getCapacity();

            if(currentTokens <= 0) {
                return false;
            }

            long decremented = jedis.decr(tokenKey);
            return decremented >= 0;
        }
    }

    public long getCapacity(String clientId) {

        return properties.getCapacity();
    }

    public long getAvailableTokens(String clientId) {

        String tokenKey = TOKENS_KEY_PREFIX + clientId;

        try(Jedis jedis = jedisPool.getResource()) {

            refillTokens(clientId, jedis);
            String tokenStr = jedis.get(tokenKey);
            return tokenStr != null ? Long.parseLong(tokenStr) : properties.getCapacity();

        }
    }

    public void refillTokens(String clientId, Jedis jedis) {

        String tokensKey = TOKENS_KEY_PREFIX + clientId;
        String lastRefillKey = LAST_REFILL_KEY_PREFIX + clientId;

        long now = System.currentTimeMillis();
        String lastRefillStr = jedis.get(lastRefillKey);
        if(lastRefillStr == null){

            jedis.set(tokensKey, String.valueOf(properties.getCapacity()));
            jedis.set(lastRefillKey, String.valueOf(now));
            return;
        }

        long lastRefillTime = Long.parseLong(lastRefillStr);
        long elapsedTime = now - lastRefillTime;

        if(elapsedTime <= 0) {
            return;
        }

        long tokensToAdd = (elapsedTime * properties.getRefillRate()) / 1000;
        if(tokensToAdd <= 0) {
            return;
        }

        String tokenStr = jedis.get(tokensKey);

        long currentTokens = tokenStr !=null ? Long.parseLong(tokenStr) : properties.getCapacity();
        long newTokens = Math.min(properties.getCapacity(), currentTokens + tokensToAdd);

        jedis.set(tokensKey, String.valueOf(newTokens));
        jedis.set(lastRefillKey, String.valueOf(now));

    }

    // tokensToAdd = (elapsedTime * properties.getRefillRate()) / 1000;

    //elapsedTime is in msec
    //refillRate is tokens per second
    //Convert msec to sec  / 1000

    //ElapsedTime > 2000ms (2sec)
    //RefillRate = 5 tokens per second
    //Cal > (2000 * 5) / 1000 > 10 tokens
}