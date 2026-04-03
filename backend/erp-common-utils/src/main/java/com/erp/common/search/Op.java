package com.erp.common.search;

/**
 * Enumeration of supported search operators for dynamic filtering.
 * <p>
 * These operators are used in {@link SearchFilter} to define comparison logic
 * when building JPA Specifications dynamically.
 * </p>
 *
 * @author ERP System
 * @since 1.0
 */
public enum Op {
    /**
     * Equals comparison (=)
     */
    EQ,

    /**
     * Not equals comparison (!=)
     */
    NE,

    /**
     * Case-insensitive substring match (LIKE %value%)
     */
    LIKE,

    /**
     * Case-insensitive prefix match (LIKE value%)
     */
    STARTS_WITH,

    /**
     * Case-insensitive suffix match (LIKE %value)
     */
    ENDS_WITH,

    /**
     * In list comparison (IN (...))
     */
    IN,

    /**
     * Greater than comparison (>)
     */
    GT,

    /**
     * Greater than or equals comparison (>=)
     */
    GTE,

    /**
     * Less than comparison (<)
     */
    LT,

    /**
     * Less than or equals comparison (<=)
     */
    LTE,

    /**
     * Between two values (BETWEEN from AND to)
     */
    BETWEEN,

    /**
     * Is null check (IS NULL)
     */
    IS_NULL,

    /**
     * Is not null check (IS NOT NULL)
     */
    IS_NOT_NULL
}
