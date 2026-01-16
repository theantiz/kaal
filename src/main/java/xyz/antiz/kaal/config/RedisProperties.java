package xyz.antiz.kaal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Redis connection properties and Jedis connection pool configuration.
 * Binds to spring.redis.* properties with production-ready pool settings.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.redis")  // Custom prefix to avoid Spring Boot conflict
public class RedisProperties {

    private String host = "localhost";
    private int port = 6379;
    private int timeout = 2000;
    private int ttlSeconds = 3600;  // 1 hour TTL for rate limiter keys
    private long refillRate = 10;   // tokens per second default

    /**
     * Production-ready Jedis connection pool with optimal settings
     */
    @Bean
    public JedisPool jedisPool() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(20);           // Max total connections
        poolConfig.setMaxIdle(10);            // Max idle connections
        poolConfig.setMinIdle(2);             // Min idle connections
        poolConfig.setTestOnBorrow(true);     // Validate before borrow
        poolConfig.setTestOnReturn(true);     // Validate before return
        poolConfig.setTestWhileIdle(true);    // Validate idle connections
        poolConfig.setMinEvictableIdleTimeMillis(60000); // Evict idle after 1min

        return new JedisPool(poolConfig, host, port, timeout);
    }

    public int getTtlSeconds() {
        return ttlSeconds;
    }

    public long getRefillRate() {
        return refillRate;
    }
}
