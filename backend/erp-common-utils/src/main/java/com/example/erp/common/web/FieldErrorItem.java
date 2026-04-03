package com.example.erp.common.web;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a single field validation error.
 * Used in ApiError to provide detailed field-level validation feedback.
 * 
 * Example:
 * {
 *   "field": "email",
 *   "message": "must be a valid email address"
 * }
 * 
 * @author ERP Team
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FieldErrorItem {
    
    /**
     * Name of the field that failed validation
     */
    private String field;
    
    /**
     * Validation error message
     */
    private String message;
}
