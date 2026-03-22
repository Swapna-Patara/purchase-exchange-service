package com.example.purchase.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.exchange")
@Getter
@Setter
public class ExchangeProperties {

    private String baseUrl;
    private int pageSize;
    private int maxMonths;
    private int maxIterations;

 }
