package xyz.antiz.kaal.service;

import org.springframework.stereotype.Service;

@Service
public class RateLimiterService {

    private final RedisTokenBucketService redisTokenBucketService;

    public RateLimiterService(RedisTokenBucketService redisTokenBucketService) {
        this.redisTokenBucketService = redisTokenBucketService;
    }

    // Used by Gateway Filter
    public boolean isAllowed(String clientId) {
        return redisTokenBucketService.tryConsume(clientId);
    }

    // Optional alias (semantic clarity)
    public boolean allowRequest(String clientId) {
        return isAllowed(clientId);
    }

    public long getCapacity(String clientId) {
        return redisTokenBucketService.getCapacity(clientId);
    }

    public long getAvailableTokens(String clientId) {
        return redisTokenBucketService.getAvailableTokens(clientId);
    }
}
