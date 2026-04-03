package com.erp.common.search;

import com.example.erp.common.converter.BooleanNumberConverter;

import java.util.Set;

/**
 * FieldValueConverter implementation that converts Boolean values to Integer (0/1)
 * for fields stored as NUMBER(1) in Oracle database.
 * 
 * <h2>Purpose:</h2>
 * When using JPA Specifications for search/filtering, boolean fields mapped to NUMBER(1)
 * columns require proper conversion. This converter handles the Boolean → Integer conversion
 * for specified fields during specification building.
 * 
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Define which fields should have Boolean->Integer conversion
 * Set<String> booleanFields = Set.of("isActive", "enabled", "visible");
 * 
 * // Create converter with those fields
 * FieldValueConverter converter = new BooleanFieldValueConverter(booleanFields);
 * 
 * // Use in spec building
 * Specification<MyEntity> spec = SpecBuilder.build(request, allowedFields, converter);
 * }</pre>
 * 
 * <h2>Conversion Rules:</h2>
 * <ul>
 *   <li>{@code Boolean.TRUE} → {@code 1}</li>
 *   <li>{@code Boolean.FALSE} → {@code 0}</li>
 *   <li>{@code null} → {@code null} (allows "ALL" queries when isActive is null)</li>
 *   <li>String "true"/"false" (case insensitive) → converted to Boolean first, then to Integer</li>
 * </ul>
 * 
 * @author ERP Team
 * @since 1.0
 * @see BooleanNumberConverter
 * @see FieldValueConverter
 */
public class BooleanFieldValueConverter implements FieldValueConverter {

    private final Set<String> booleanFields;
    private final FieldValueConverter delegate;

    /**
     * Creates a converter for the specified boolean fields.
     * 
     * @param booleanFields set of field names that should be treated as NUMBER(1) booleans
     */
    public BooleanFieldValueConverter(Set<String> booleanFields) {
        this(booleanFields, DefaultFieldValueConverter.INSTANCE);
    }

    /**
     * Creates a converter for the specified boolean fields with a delegate for other fields.
     * 
     * @param booleanFields set of field names that should be treated as NUMBER(1) booleans
     * @param delegate converter to use for non-boolean fields
     */
    public BooleanFieldValueConverter(Set<String> booleanFields, FieldValueConverter delegate) {
        this.booleanFields = booleanFields != null ? booleanFields : Set.of();
        this.delegate = delegate != null ? delegate : DefaultFieldValueConverter.INSTANCE;
    }

    @Override
    public Object convert(String field, Object rawValue, Op op) {
        // Check if this field should be converted as a boolean
        if (booleanFields.contains(field)) {
            return convertBooleanField(rawValue);
        }
        
        // Delegate to the wrapped converter for other fields
        return delegate.convert(field, rawValue, op);
    }

    /**
     * Converts a value to Integer for NUMBER(1) boolean columns.
     * 
     * @param rawValue the raw value (Boolean, String, or null)
     * @return Integer (1, 0, or null)
     */
    private Integer convertBooleanField(Object rawValue) {
        if (rawValue == null) {
            return null;
        }

        // Already a Boolean - convert directly
        if (rawValue instanceof Boolean) {
            return BooleanNumberConverter.toDbValue((Boolean) rawValue);
        }

        // String value - parse as boolean first
        if (rawValue instanceof String) {
            String strValue = ((String) rawValue).trim();
            if (strValue.isEmpty()) {
                return null;
            }
            
            // Handle common boolean string representations
            if ("true".equalsIgnoreCase(strValue) || "1".equals(strValue) || "yes".equalsIgnoreCase(strValue)) {
                return BooleanNumberConverter.DB_TRUE;
            }
            if ("false".equalsIgnoreCase(strValue) || "0".equals(strValue) || "no".equalsIgnoreCase(strValue)) {
                return BooleanNumberConverter.DB_FALSE;
            }
            
            throw new SearchException(
                "Invalid boolean value: '" + strValue + "'. Expected: true, false, 1, 0, yes, or no."
            );
        }

        // Integer value - validate and pass through
        if (rawValue instanceof Integer) {
            Integer intValue = (Integer) rawValue;
            if (BooleanNumberConverter.DB_TRUE.equals(intValue) || 
                BooleanNumberConverter.DB_FALSE.equals(intValue)) {
                return intValue;
            }
            throw new SearchException(
                "Invalid boolean integer value: " + intValue + ". Expected: 0 or 1."
            );
        }

        // Number value (Long, etc.) - convert to Integer
        if (rawValue instanceof Number) {
            int intValue = ((Number) rawValue).intValue();
            if (intValue == 1 || intValue == 0) {
                return intValue;
            }
            throw new SearchException(
                "Invalid boolean numeric value: " + rawValue + ". Expected: 0 or 1."
            );
        }

        throw new SearchException(
            "Cannot convert value of type " + rawValue.getClass().getSimpleName() + " to boolean."
        );
    }

    /**
     * Factory method to create a converter for common active/enabled fields.
     * 
     * @return converter configured for standard boolean field names
     */
    public static BooleanFieldValueConverter forActiveFields() {
        return new BooleanFieldValueConverter(Set.of(
            "isActive",
            "active", 
            "enabled",
            "visible",
            "deleted",
            "locked",
            "published",
            "verified"
        ));
    }

    /**
     * Factory method with custom delegate.
     * 
     * @param delegate the delegate converter for non-boolean fields
     * @return converter configured for standard boolean field names
     */
    public static BooleanFieldValueConverter forActiveFields(FieldValueConverter delegate) {
        return new BooleanFieldValueConverter(Set.of(
            "isActive",
            "active",
            "enabled",
            "visible",
            "deleted",
            "locked",
            "published",
            "verified"
        ), delegate);
    }
}
