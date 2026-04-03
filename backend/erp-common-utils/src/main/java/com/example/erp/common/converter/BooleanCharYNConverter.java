package com.example.erp.common.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Explicit converter for Oracle CHAR(1) / VARCHAR2(1) boolean flags.
 *
 * Mapping (Y/N):
 * - 'Y' -> true
 * - 'N' -> false
 * - null -> null
 *
 * Any other value is rejected (fail-fast).
 */
@Converter(autoApply = false)
public class BooleanCharYNConverter implements AttributeConverter<Boolean, String> {

    @Override
    public String convertToDatabaseColumn(Boolean attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute ? "Y" : "N";
    }

    @Override
    public Boolean convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        String normalized = dbData.trim().toUpperCase();
        if (normalized.isEmpty()) {
            return null;
        }
        if ("Y".equals(normalized)) {
            return Boolean.TRUE;
        }
        if ("N".equals(normalized)) {
            return Boolean.FALSE;
        }
        throw new IllegalArgumentException("Invalid CHAR(1) boolean value (expected Y/N): '" + dbData + "'");
    }
}
