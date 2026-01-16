package xyz.antiz.kaal.service;

/**
 * Redis key patterns and schema for Token Bucket Rate Limiter
 *
 * NOMENCLATURE:
 *   rate_limiter:{type}:{identifier}
 *   - type:      "token" | "last_refill"
 *   - identifier: clientId or IP address (String)
 *
 * KEY PATTERNS:
 *   1. rate_limiter:token:{clientId}     -> Current available token count (Integer)
 *   2. rate_limiter:last_refill:{clientId} -> Last refill timestamp (Unix epoch, Long)
 *
 * EXAMPLE USAGE:
 *   Client IP: 192.168.1.100
 *   ┌─────────────────────────────────────┐
 *   │ rate_limiter:token:192.168.1.100   │ -> "7"    (7 tokens available)
 *   │ rate_limiter:last_refill:192.168.1.100 │ -> "1640995200000" (refill timestamp)
 *   └─────────────────────────────────────┘
 *
 * DATA TYPES:
 *   - Token count:    String representation of Integer (7 → "7")
 *   - Timestamp:      String representation of Long (Unix epoch milliseconds)
 *
 * STORAGE STRATEGY:
 *   - Atomic Redis operations ensure thread-safety
 *   - TTL auto-expiry prevents stale entries
 *   - IP-based identifiers enable stateless rate limiting
 *
 * THREAD SAFETY: Redis atomic operations (INCR, SETNX, EXPIRE)
 * SCALABILITY:  Horizontal scaling across Redis cluster/shard
 */


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import xyz.antiz.kaal.config.RateLimiterProperties;
import xyz.antiz.kaal.config.RedisProperties;

//store token bucket state in redis
//manage tokens per client
//provide rate limiting logic
//handle token refill based on time

@RequiredArgsConstructor
@Service
public class RedisTokenBucketService {

    private static final String LAST_REFILL_KEY_PREFIX = "rate_limiter:last_refill:";
    private final String TOKEN_KEY_PREFIX = "rate_limiter:tokens:";
    @Autowired
    private JedisPool jedisPool;
    private RateLimiterProperties rateLimiterProperties;
    @Autowired
    private ClientHttpRequestFactorySettings clientHttpRequestFactorySettings;
    @Autowired
    private RedisProperties redisProperties;


    public boolean isAllowed(String clientId) {
        String tokenKey = TOKEN_KEY_PREFIX + clientId;

        try (Jedis jedis = jedisPool.getResource()) {
            refillTokens(clientId, jedis);
            String tokenStr = jedis.get(tokenKey);

            long currentTokens = tokenStr != null ? Long.parseLong(tokenStr) : rateLimiterProperties.getCapacity();

            if (currentTokens <= 0) {
                return false;
            }

            long decremented = jedis.decr(tokenKey);
            return decremented >= 0;

        }
    }

    public long getCapacity(String clientId) {
        String tokenKey = TOKEN_KEY_PREFIX + clientId;
        return rateLimiterProperties.getCapacity() - (tokenKey != null ? Long.parseLong(tokenKey) : 0);
    }

    public long getAvailableTokens(String clientId) {
        String tokenKey = TOKEN_KEY_PREFIX + clientId;
        try (Jedis jedis = jedisPool.getResource()) {
            refillTokens(clientId, jedis);
            String tokenStr = jedis.get(tokenKey);
            return tokenStr != null ? Long.parseLong(tokenStr) : rateLimiterProperties.getCapacity();

        }
    }

    public void refillTokens(String clientId, Jedis jedis) {
        String tokenKey = TOKEN_KEY_PREFIX + clientId;
        String lastRefillKey = LAST_REFILL_KEY_PREFIX + clientId;

        long currentTime = System.currentTimeMillis();

        String lastRefillStr = jedis.get(lastRefillKey);

        if (lastRefillStr == null) {
            jedis.set(tokenKey, String.valueOf(rateLimiterProperties.getCapacity()));
            jedis.set(lastRefillKey, String.valueOf(currentTime));
            return;
        }

        long lastRefillTime = Long.parseLong(lastRefillStr);
        long elpasedTIme = currentTime - lastRefillTime;

        if (elpasedTIme <= 0){
            return;
        }

        long tokenToAdd = (elpasedTIme * redisProperties.getRefillRate()) / 1000;

        if (tokensToAdd <= 0){
            return;

        }
    }


}



