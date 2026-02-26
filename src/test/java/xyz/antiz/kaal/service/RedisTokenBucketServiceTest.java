package xyz.antiz.kaal.service;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import xyz.antiz.kaal.config.RateLimiterProperties;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisTokenBucketServiceTest {

    @Test
    void refillsCorrectlyUnderHighFrequencyCalls() {
        Map<String, String> redis = new HashMap<>();
        JedisPool jedisPool = mock(JedisPool.class);
        Jedis jedis = mock(Jedis.class);
        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.get(org.mockito.ArgumentMatchers.anyString())).thenAnswer(invocation -> redis.get(invocation.getArgument(0)));
        when(jedis.set(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString())).thenAnswer(invocation -> {
            redis.put(invocation.getArgument(0), invocation.getArgument(1));
            return "OK";
        });

        RateLimiterProperties properties = new RateLimiterProperties();
        properties.setCapacity(1);
        properties.setRefillRate(1);

        MutableClock clock = new MutableClock(Instant.ofEpochMilli(0), ZoneId.of("UTC"));
        RedisTokenBucketService service = new RedisTokenBucketService(jedisPool, properties, clock);

        String clientId = "client-1";

        assertTrue(service.tryConsume(clientId));
        assertFalse(service.tryConsume(clientId));

        clock.setMillis(500);
        assertFalse(service.tryConsume(clientId));

        clock.setMillis(1000);
        assertTrue(service.tryConsume(clientId));
        assertEquals(0, service.getAvailableTokens(clientId));
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        private final ZoneId zoneId;

        private MutableClock(Instant instant, ZoneId zoneId) {
            this.instant = instant;
            this.zoneId = zoneId;
        }

        @Override
        public ZoneId getZone() {
            return zoneId;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void setMillis(long millis) {
            this.instant = Instant.ofEpochMilli(millis);
        }
    }
}
