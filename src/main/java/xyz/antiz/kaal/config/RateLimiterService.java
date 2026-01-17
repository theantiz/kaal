package xyz.antiz.kaal.config;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.stereotype.Service;
import xyz.antiz.kaal.service.RedisTokenBucketService;

@RequiredArgsConstructor
@Service
public class RateLimiterService {

    private final RedisTokenBucketService redisTokenBucketService;

    public boolean isAllowed(String clientId) {
        return redisTokenBucketService.isAllowed(clientId);
    }
    public long getCapacity(String clientId) {
        return redisTokenBucketService.getCapacity(clientId);
    }

    public long getAvailableToken(String clientId) {
        return redisTokenBucketService.getAvailableTokens(clientId);
    }
}
