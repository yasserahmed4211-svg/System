package com.erp.common.search;

import java.util.Objects;

/**
 * Represents a single search filter criterion.
 * <p>
 * A filter consists of a field name (supporting dot notation for nested properties),
 * an operator, and a value (which may be null for IS_NULL/IS_NOT_NULL operators).
 * </p>
 *
 * <p><b>Examples:</b></p>
 * <ul>
 *   <li>{@code field="username", op=LIKE, value="john"}</li>
 *   <li>{@code field="tenant.id", op=EQ, value=1}</li>
 *   <li>{@code field="roles.name", op=IN, value=["ADMIN", "USER"]}</li>
 *   <li>{@code field="enabled", op=IS_NOT_NULL, value=null}</li>
 * </ul>
 *
 * @author ERP System
 * @since 1.0
 */
public class SearchFilter {

    private String field;
    private Op op;
    private Object value;

    /**
     * Default constructor for deserialization.
     */
    public SearchFilter() {
    }

    /**
     * Constructs a new SearchFilter.
     *
     * @param field the field name (supports dot notation for nested properties)
     * @param op    the operator to apply
     * @param value the value to compare (may be null for IS_NULL/IS_NOT_NULL)
     */
    public SearchFilter(String field, Op op, Object value) {
        this.field = field;
        this.op = op;
        this.value = value;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public Op getOp() {
        return op;
    }

    public void setOp(Op op) {
        this.op = op;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchFilter that = (SearchFilter) o;
        return Objects.equals(field, that.field) &&
               op == that.op &&
               Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, op, value);
    }

    @Override
    public String toString() {
        return "SearchFilter{" +
               "field='" + field + '\'' +
               ", op=" + op +
               ", value=" + value +
               '}';
    }
}
