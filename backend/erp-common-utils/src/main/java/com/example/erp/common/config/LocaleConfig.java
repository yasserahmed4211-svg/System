package com.example.erp.common.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.Locale;

/**
 * Internationalization (i18n) configuration for all ERP modules.
 * 
 * Provides:
 * - LocaleResolver: Resolves locale from Accept-Language header
 * - MessageSource: Loads i18n messages from classpath:messages.properties
 * 
 * Default locale: English
 * Supported locales: en, ar (configured in messages.properties, messages_ar.properties)
 * 
 * Usage in modules:
 * 1. Create messages.properties in src/main/resources/
 * 2. Create messages_ar.properties for Arabic
 * 3. Inject LocalizationService to get localized messages
 * 
 * Example messages.properties:
 * <pre>
 * USER_NOT_FOUND=User not found
 * VALIDATION_ERROR=Validation failed
 * </pre>
 * 
 * Example messages_ar.properties:
 * <pre>
 * USER_NOT_FOUND=المستخدم غير موجود
 * VALIDATION_ERROR=فشل التحقق من صحة البيانات
 * </pre>
 * 
 * @author ERP Team
 * @see com.example.erp.common.i18n.LocalizationService
 */
@Configuration
public class LocaleConfig {

    /**
     * LocaleResolver that extracts locale from Accept-Language HTTP header.
     * Falls back to English if no header is present.
     * 
     * @return configured LocaleResolver
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.ENGLISH);
        return resolver;
    }

    /**
     * MessageSource for loading internationalized messages.
     * 
     * Configuration:
     * - Base name: "messages" (searches for messages.properties)
     * - Encoding: UTF-8 (supports Arabic and special characters)
     * - No system locale fallback (uses default locale)
     * 
     * @return configured MessageSource
     */
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
        ms.setBasename("classpath:messages");
        ms.setDefaultEncoding("UTF-8");
        ms.setFallbackToSystemLocale(false);
        return ms;
    }
}
