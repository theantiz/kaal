package xyz.antiz.kaal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Data
@Configuration
@ConfigurationProperties(prefix = "spring.redis")
public class RedisProperties {

    private String host = "localhost";
    private int port = 6379;
    private int timeout = 2000;

    // Java Client library for Redis
    // Lets Java application communicate with Redis Server
    // JedisPool manages a pool of reusable Redis connections to avoid creating a new
    // connection for every request. It improves performance and resource usage by
    // providing ready-to-use connections and returning them to the pool after use.
    // Closing a Jedis instance does not close Redis; it simply releases the
    // connection back to the pool for reuse.

    @Bean
    public JedisPool getJedisPool() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxIdle(50);
        poolConfig.setMaxTotal(10);
        poolConfig.setMinIdle(5);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        return new JedisPool(poolConfig, host, port, timeout);


    }
}
