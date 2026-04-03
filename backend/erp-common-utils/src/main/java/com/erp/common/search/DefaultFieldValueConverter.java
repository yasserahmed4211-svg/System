package com.erp.common.search;

/**
 * Default implementation of {@link FieldValueConverter} that returns values unchanged.
 * <p>
 * This no-op converter is used when no custom conversion logic is needed.
 * Entity modules can provide their own implementations for specific type conversions.
 * </p>
 *
 * @author ERP System
 * @since 1.0
 */
public class DefaultFieldValueConverter implements FieldValueConverter {

    /**
     * Singleton instance.
     */
    public static final DefaultFieldValueConverter INSTANCE = new DefaultFieldValueConverter();

    private DefaultFieldValueConverter() {
    }

    @Override
    public Object convert(String field, Object rawValue, Op op) {
        return rawValue;
    }
}
