package com.example.erp.common.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FieldErrorItem
 */
class FieldErrorItemTest {

    @Test
    void testAllArgsConstructor_CreatesInstance() {
        FieldErrorItem item = new FieldErrorItem("email", "Invalid email format");
        
        assertEquals("email", item.getField());
        assertEquals("Invalid email format", item.getMessage());
    }

    @Test
    void testNoArgsConstructor_CreatesEmptyInstance() {
        FieldErrorItem item = new FieldErrorItem();
        
        assertNotNull(item);
        assertNull(item.getField());
        assertNull(item.getMessage());
    }

    @Test
    void testSetters_SetValuesCorrectly() {
        FieldErrorItem item = new FieldErrorItem();
        
        item.setField("username");
        item.setMessage("Username is required");
        
        assertEquals("username", item.getField());
        assertEquals("Username is required", item.getMessage());
    }

    @Test
    void testWithNullValues_HandlesNulls() {
        FieldErrorItem item = new FieldErrorItem(null, null);
        
        assertNull(item.getField());
        assertNull(item.getMessage());
    }

    @Test
    void testWithEmptyStrings_HandlesEmptyStrings() {
        FieldErrorItem item = new FieldErrorItem("", "");
        
        assertEquals("", item.getField());
        assertEquals("", item.getMessage());
    }

    @Test
    void testMultipleInstances_AreIndependent() {
        FieldErrorItem item1 = new FieldErrorItem("field1", "message1");
        FieldErrorItem item2 = new FieldErrorItem("field2", "message2");
        
        assertNotEquals(item1.getField(), item2.getField());
        assertNotEquals(item1.getMessage(), item2.getMessage());
    }
}
