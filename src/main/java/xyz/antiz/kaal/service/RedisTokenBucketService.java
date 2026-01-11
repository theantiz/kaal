package xyz.antiz.kaal.service;


import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPool;
import xyz.antiz.kaal.config.RateLimiterProperties;

//store token bucket state in redis
//manage tokens per client
//provide rate limiting logic
//handle token refill based on time

@RequiredArgsConstructor
@Service
public class RedisTokenBucketService  {
    private final JedisPool jedisPool;

    private final RateLimiterProperties rateLimiterProperties;

    private final String TOKEN_KEY_PREFIX = "rate_limiter:tokens:";
    private static final String LAST_REFILL_KEY_PREFIX = "rate_limiter:last_refill:";

    //Pattern
    //rate_limiter:{type}:{clientId}


}
