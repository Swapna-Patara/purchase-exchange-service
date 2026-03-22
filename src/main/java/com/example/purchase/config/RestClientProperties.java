package com.example.purchase.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for REST client timeouts.
 */
@Configuration
@ConfigurationProperties(prefix = "app.rest-client")
@Getter
@Setter
public class RestClientProperties {
    
    /**
     * Connection timeout in milliseconds.
     * Time to establish a connection with the remote host.
     */
    private int connectTimeout;
    
    /**
     * Read timeout in milliseconds.
     * Time to wait for data after connection is established.
     */
    private int readTimeout;
}
