package com.example.purchase.config;


import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;


@Configuration
public class AppConfig {

    private final RestClientProperties restClientProperties;

    public AppConfig(RestClientProperties restClientProperties) {
        this.restClientProperties = restClientProperties;
    }

    /**
     * Creates a RestTemplate bean with configured timeouts from application properties.
     * <p>
     * Timeouts configured:
     * - Connection Timeout: Time to establish connection with remote host
     * - Read Timeout: Time to wait for data after connection is established
     * <p>
     * These timeouts prevent the application from hanging indefinitely
     * when the downstream Treasury API is slow or unresponsive.
     *
     * @param builder RestTemplateBuilder for configuration
     * @return configured RestTemplate instance with timeout settings
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofMillis(restClientProperties.getConnectTimeout()))
                .readTimeout(Duration.ofMillis(restClientProperties.getReadTimeout()))
                .build();
    }
}
