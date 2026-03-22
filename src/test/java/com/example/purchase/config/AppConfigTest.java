package com.example.purchase.config;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for AppConfig to verify RestTemplate bean configuration.
 */
@SpringBootTest
@ActiveProfiles("test")
class AppConfigTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void testRestTemplateBeanExists() {
        // Check if RestTemplate bean is present in the context
        assertTrue(applicationContext.containsBean("restTemplate"),
                "RestTemplate bean should exist in the Spring context");

        RestTemplate restTemplate = applicationContext.getBean(RestTemplate.class);
        assertNotNull(restTemplate, "RestTemplate bean should not be null");
    }
}

