package com.example.erp.common.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * ✅ CENTRALIZED Jackson ObjectMapper Configuration for ALL ERP modules
 * 
 * In Spring Boot 4.0, ObjectMapper is not always auto-configured as a bean.
 * This configuration ensures ObjectMapper is available across all modules.
 * 
 * Features:
 * - Java 8 Time support (LocalDateTime, ZonedDateTime, etc.)
 * - ISO-8601 date format (not timestamps)
 * - Pretty print in development
 * 
 * Architecture Rule:
 * - Rule 12: Common configurations belong in erp-common-utils
 * 
 * Usage:
 * Just @ComponentScan("com.example.erp.common.web") in your module:
 * 
 * <pre>
 * &#64;Autowired
 * private ObjectMapper objectMapper;
 * 
 * String json = objectMapper.writeValueAsString(object);
 * </pre>
 * 
 * @author ERP Team
 */
@Configuration
public class CommonJacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // Register Java 8 time module for LocalDateTime, ZonedDateTime, Instant, etc.
        objectMapper.registerModule(new JavaTimeModule());
        
        // Write dates as ISO-8601 strings instead of timestamps
        // Example: "2026-01-10T10:30:00Z" instead of 1736507400000
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Pretty print JSON for better readability in development
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        // Optional: Configure null handling
        // objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        
        return objectMapper;
    }
}
