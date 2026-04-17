package com.erp.main.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * JPA Configuration for multi-module entity scanning.
 * Creates EntityManagerFactory with explicit entity packages and JPA properties
 * injected directly from the active Spring profile configuration.
 */
@Configuration("erpMainJpaConfig")
@EnableJpaRepositories(
    basePackages = {
        "com.example.security.repo",
        "com.example.masterdata.repository",
        "com.example.erp.finance.gl.repository",
        "com.example.org.repository"
    }
)
public class JpaConfig {

    @Value("${spring.jpa.properties.hibernate.dialect:org.hibernate.dialect.OracleDialect}")
    private String hibernateDialect;

    @Value("${spring.jpa.properties.hibernate.default_schema:test}")
    private String defaultSchema;

    @Value("${spring.jpa.properties.jakarta.persistence.schema-generation.database.action:none}")
    private String schemaAction;

    @Value("${spring.jpa.show-sql:false}")
    private boolean showSql;

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);

        em.setPackagesToScan(
            "com.example.security.domain",
            "com.example.masterdata.entity",
            "com.example.erp.finance.gl.entity",
            "com.example.org.entity"
        );

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setShowSql(showSql);
        vendorAdapter.setDatabasePlatform(hibernateDialect);
        em.setJpaVendorAdapter(vendorAdapter);

        Properties props = new Properties();
        props.setProperty("hibernate.dialect", hibernateDialect);
        props.setProperty("hibernate.default_schema", defaultSchema);
        props.setProperty("jakarta.persistence.schema-generation.database.action", schemaAction);
        em.setJpaProperties(props);

        return em;
    }
}
