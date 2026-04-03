package com.erp.common.search;

/**
 * Interface for converting raw filter values to appropriate types.
 * <p>
 * Implementations can provide entity-specific or field-specific conversions,
 * such as parsing date strings, converting numeric strings, or transforming
 * enum values.
 * </p>
 *
 * <p><b>Example implementation:</b></p>
 * <pre>
 * public class UserFieldValueConverter implements FieldValueConverter {
 *     public Object convert(String field, Object rawValue, Op op) {
 *         if ("createdAt".equals(field) && rawValue instanceof String) {
 *             return LocalDate.parse((String) rawValue);
 *         }
 *         return rawValue;
 *     }
 * }
 * </pre>
 *
 * @author ERP System
 * @since 1.0
 */
public interface FieldValueConverter {

    /**
     * Converts a raw value to the appropriate type for the given field and operator.
     *
     * @param field    the field name being filtered
     * @param rawValue the raw value from the search request
     * @param op       the operator being applied
     * @return the converted value, or the original value if no conversion is needed
     * @throws SearchException if the value cannot be converted
     */
    Object convert(String field, Object rawValue, Op op);
}
