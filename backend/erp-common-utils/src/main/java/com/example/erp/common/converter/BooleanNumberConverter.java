package com.example.erp.common.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter for Oracle NUMBER(1) boolean flags.
 * 
 * <h2>Database-to-Java Mapping:</h2>
 * <ul>
 *   <li>{@code 1} → {@code Boolean.TRUE}</li>
 *   <li>{@code 0} → {@code Boolean.FALSE}</li>
 *   <li>{@code null} → {@code null}</li>
 *   <li>Any other value → {@link IllegalArgumentException} (fail-fast)</li>
 * </ul>
 * 
 * <h2>Java-to-Database Mapping:</h2>
 * <ul>
 *   <li>{@code true} → {@code 1}</li>
 *   <li>{@code false} → {@code 0}</li>
 *   <li>{@code null} → {@code null}</li>
 * </ul>
 * 
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * @Entity
 * public class MyEntity {
 *     @Column(name = "IS_ACTIVE", nullable = false)
 *     @Convert(converter = BooleanNumberConverter.class)
 *     private Boolean isActive = Boolean.TRUE;
 * }
 * }</pre>
 * 
 * <h2>Architecture Rules:</h2>
 * <ul>
 *   <li>Entities MUST use {@code Boolean} for active flags</li>
 *   <li>Database columns use NUMBER(1) (0/1)</li>
 *   <li>Frontend NEVER sees numeric or string flags (only true/false/null)</li>
 *   <li>This converter is explicit (autoApply = false) for controlled usage</li>
 * </ul>
 * 
 * @author ERP Team
 * @since 1.0
 * @see BooleanCharYNConverter for Y/N character-based boolean columns
 */
@Converter(autoApply = false)
public class BooleanNumberConverter implements AttributeConverter<Boolean, Integer> {

    /** Database value representing TRUE */
    public static final Integer DB_TRUE = 1;
    
    /** Database value representing FALSE */
    public static final Integer DB_FALSE = 0;

    /**
     * Converts a Java Boolean to Oracle NUMBER(1).
     * 
     * @param attribute the Boolean value to convert (may be null)
     * @return 1 for true, 0 for false, null for null
     */
    @Override
    public Integer convertToDatabaseColumn(Boolean attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute ? DB_TRUE : DB_FALSE;
    }

    /**
     * Converts an Oracle NUMBER(1) to Java Boolean.
     * 
     * @param dbData the database value (should be 0, 1, or null)
     * @return Boolean.TRUE for 1, Boolean.FALSE for 0, null for null
     * @throws IllegalArgumentException if dbData is not 0, 1, or null (fail-fast for data integrity)
     */
    @Override
    public Boolean convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        if (DB_TRUE.equals(dbData)) {
            return Boolean.TRUE;
        }
        if (DB_FALSE.equals(dbData)) {
            return Boolean.FALSE;
        }
        throw new IllegalArgumentException(
            "Invalid NUMBER(1) boolean value: " + dbData + 
            ". Expected 0 (false), 1 (true), or null."
        );
    }

    /**
     * Utility method for converting Boolean to Integer outside JPA context.
     * Useful for building native queries or JDBC operations.
     * 
     * @param value the Boolean value
     * @return 1 for true, 0 for false, null for null
     */
    public static Integer toDbValue(Boolean value) {
        if (value == null) {
            return null;
        }
        return value ? DB_TRUE : DB_FALSE;
    }

    /**
     * Utility method for converting Integer to Boolean outside JPA context.
     * Useful for processing native query results.
     * 
     * @param dbValue the database value
     * @return Boolean.TRUE for 1, Boolean.FALSE for 0, null for null
     * @throws IllegalArgumentException if dbValue is not 0, 1, or null
     */
    public static Boolean fromDbValue(Integer dbValue) {
        if (dbValue == null) {
            return null;
        }
        if (DB_TRUE.equals(dbValue)) {
            return Boolean.TRUE;
        }
        if (DB_FALSE.equals(dbValue)) {
            return Boolean.FALSE;
        }
        throw new IllegalArgumentException(
            "Invalid NUMBER(1) boolean value: " + dbValue
        );
    }
}
