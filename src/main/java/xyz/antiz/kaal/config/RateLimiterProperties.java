package xyz.antiz.kaal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rate-limiter")
public class RateLimiterProperties {

    private long capacity;
    private long refillRate;
    private String apiServerUrl;

    public long getCapacity() {
        return capacity;
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }

    public long getRefillRate() {
        return refillRate;
    }

    public void setRefillRate(long refillRate) {
        this.refillRate = refillRate;
    }

    public String getApiServerUrl() {
        return apiServerUrl;
    }

    public void setApiServerUrl(String apiServerUrl) {
        this.apiServerUrl = apiServerUrl;
    }
}
