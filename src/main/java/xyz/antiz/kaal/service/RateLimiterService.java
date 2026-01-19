package xyz.antiz.kaal.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final RedisTokenBucketService redisTokenBucketService;

    public boolean isAllowed(String clientId) {
        // FIX: call existing method
        return redisTokenBucketService.tryConsume(clientId);
    }

    public long getCapacity(String clientId) {
        return redisTokenBucketService.getCapacity(clientId);
    }

    public long getAvailableTokens(String clientId) {
        return redisTokenBucketService.getAvailableTokens(clientId);
    }
}
