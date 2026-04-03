package com.example.erp.common.web;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ApiError
 */
class ApiErrorTest {

    @Test
    void testConstructorWithCodeOnly_CreatesInstance() {
        ApiError error = new ApiError("ERR_001", null, null);
        
        assertEquals("ERR_001", error.getCode());
        assertNull(error.getDetails());
        assertNull(error.getFieldErrors());
    }

    @Test
    void testConstructorWithCodeAndDetails_CreatesInstance() {
        ApiError error = new ApiError("ERR_002", "Error details");
        
        assertEquals("ERR_002", error.getCode());
        assertEquals("Error details", error.getDetails());
    }

    @Test
    void testConstructorWithCodeDetailsAndFieldErrors_CreatesInstance() {
        FieldErrorItem field1 = new FieldErrorItem("email", "Invalid email format");
        FieldErrorItem field2 = new FieldErrorItem("age", "Must be positive");
        List<FieldErrorItem> fieldErrors = Arrays.asList(field1, field2);
        
        ApiError error = new ApiError("VALIDATION_ERROR", "Validation failed", fieldErrors);
        
        assertEquals("VALIDATION_ERROR", error.getCode());
        assertEquals("Validation failed", error.getDetails());
        assertNotNull(error.getFieldErrors());
        assertEquals(2, error.getFieldErrors().size());
        assertEquals("email", error.getFieldErrors().get(0).getField());
        assertEquals("Invalid email format", error.getFieldErrors().get(0).getMessage());
    }

    @Test
    void testSetTimestamp_SetsValueCorrectly() {
        ApiError error = new ApiError("ERR_001", "Details");
        Instant timestamp = Instant.now();
        
        error.setTimestamp(timestamp);
        
        assertEquals(timestamp, error.getTimestamp());
    }

    @Test
    void testSetPath_SetsValueCorrectly() {
        ApiError error = new ApiError("ERR_001", "Details");
        String path = "/api/v1/users";
        
        error.setPath(path);
        
        assertEquals(path, error.getPath());
    }

    @Test
    void testSetters_AllFieldsSetCorrectly() {
        ApiError error = new ApiError("ERR_001", null, null);
        FieldErrorItem fieldError = new FieldErrorItem("username", "Required");
        List<FieldErrorItem> fieldErrors = Arrays.asList(fieldError);
        Instant timestamp = Instant.now();
        
        error.setCode("ERR_002");
        error.setDetails("New details");
        error.setFieldErrors(fieldErrors);
        error.setTimestamp(timestamp);
        error.setPath("/api/v1/test");
        
        assertEquals("ERR_002", error.getCode());
        assertEquals("New details", error.getDetails());
        assertEquals(1, error.getFieldErrors().size());
        assertEquals(timestamp, error.getTimestamp());
        assertEquals("/api/v1/test", error.getPath());
    }

    @Test
    void testNoArgsConstructor_CreatesEmptyInstance() {
        ApiError error = new ApiError();
        
        assertNotNull(error);
        assertNull(error.getCode());
        assertNull(error.getDetails());
        assertNull(error.getFieldErrors());
        assertNull(error.getTimestamp());
        assertNull(error.getPath());
    }

    @Test
    void testAllArgsConstructor_CreatesFullInstance() {
        FieldErrorItem fieldError = new FieldErrorItem("field1", "error1");
        List<FieldErrorItem> fieldErrors = Arrays.asList(fieldError);
        Instant timestamp = Instant.now();
        
        ApiError error = new ApiError("ERR_001", "Details", fieldErrors);
        error.setTimestamp(timestamp);
        error.setPath("/api/test");
        
        assertEquals("ERR_001", error.getCode());
        assertEquals("Details", error.getDetails());
        assertEquals(1, error.getFieldErrors().size());
        assertEquals(timestamp, error.getTimestamp());
        assertEquals("/api/test", error.getPath());
    }

    @Test
    void testWithEmptyFieldErrors_HandlesEmptyList() {
        List<FieldErrorItem> emptyList = Arrays.asList();
        
        ApiError error = new ApiError("ERR_001", "Details", emptyList);
        
        assertNotNull(error.getFieldErrors());
        assertTrue(error.getFieldErrors().isEmpty());
    }
}
