package xyz.antiz.kaal.config;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Configuration
public class RedisProperties {

    @Bean
    public JedisPool jedisPool() {

        GenericObjectPoolConfig<Jedis> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(20);
        poolConfig.setMaxIdle(10);
        poolConfig.setMinIdle(2);
        poolConfig.setJmxEnabled(false);

        return new JedisPool(
                poolConfig,
                "localhost",
                6379,
                2000
        );
    }
}
