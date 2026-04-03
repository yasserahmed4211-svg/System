package com.example.security.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;

/**
 * JPA Configuration for Spring Boot 4.0 (Security module fallback)
 * 
 * Provides EntityManagerFactory only when running standalone (e.g., module-level tests).
 * When running under erp-main, its JpaConfig takes precedence via @ConditionalOnMissingBean.
 * 
 * Note: @EnableJpaRepositories is NOT placed here to avoid duplicate bean definitions
 * when erp-main aggregates all modules. erp-main/JpaConfig handles all repository scanning.
 */
@Configuration
public class JpaConfig {
    
    /**
     * Configure EntityManagerFactory with explicit entity package scanning
     * Required in Spring Boot 4.0 for cross-module entity detection
     */
    @Bean
    @ConditionalOnMissingBean(name = "entityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan(
            "com.example.security.domain"            // Security module entities
        );
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        return em;
    }
}
