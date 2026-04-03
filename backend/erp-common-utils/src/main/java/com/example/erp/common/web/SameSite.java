package com.example.erp.common.web;

/**
 * SameSite attribute لـ cookies
 */
public enum SameSite {
    STRICT("Strict"),
    LAX("Lax"),
    NONE("None");
    
    private final String value;
    
    SameSite(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return value;
    }
}
