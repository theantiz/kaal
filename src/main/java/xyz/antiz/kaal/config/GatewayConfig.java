package xyz.antiz.kaal.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder,
                               RateLimiterProperties props) {

        return builder.routes()
                .route("api",
                        r -> r.path("/api/**")
                                .uri(props.getApiServerUrl()))
                .build();
    }
}
