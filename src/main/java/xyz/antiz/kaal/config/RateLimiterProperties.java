package xyz.antiz.kaal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


//We have defined config. in two place -> this class is typesafe (internal) & application.properties is for external use case

@Data
@Component // to make this class as Spring Bean
@ConfigurationProperties (prefix = "rate-limiter")
public class RateLimiterProperties {
    private int capacity = 10;
    private int refillRate = 5;
    private int timeout = 5000;
    private String apiServerUrl = "http://localhost:8080";
}
