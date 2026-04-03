package com.erp.main.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA Configuration for multi-module entity scanning.
 * EntityManagerFactory is auto-configured by Spring Boot to pick up
 * all JPA properties (ddl-auto, schema-generation, etc.) from application.properties.
 */
@Configuration("erpMainJpaConfig")
@EntityScan(basePackages = {
    "com.example.security.domain",
    "com.example.masterdata.entity",
    "com.example.erp.finance.gl.entity"
})
@EnableJpaRepositories(
    basePackages = {
        "com.example.security.repo",
        "com.example.masterdata.repository",
        "com.example.erp.finance.gl.repository"
    }
)
public class JpaConfig {
    // Spring Boot auto-configures EntityManagerFactory with all properties
    // from application.properties / application-dev.properties
}
