package com.example.erp.common.i18n;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * خدمة الترجمة (i18n)
 * تستخدم MessageSource لترجمة message keys
 */
@Component
@RequiredArgsConstructor
public class LocalizationService {
    
    private final MessageSource messageSource;
    
    /**
     * ترجمة message key باستخدام اللغة الحالية
     */
    public String getMessage(String key, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(key, args, key, locale);
    }
    
    /**
     * ترجمة message key باستخدام لغة محددة
     */
    public String getMessage(String key, Locale locale, Object... args) {
        return messageSource.getMessage(key, args, key, locale);
    }
}
