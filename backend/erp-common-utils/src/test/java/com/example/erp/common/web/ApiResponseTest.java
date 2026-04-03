package com.example.erp.common.web;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ApiResponse
 */
class ApiResponseTest {

    @Test
    void testOkWithDataAndMessage_CreatesSuccessResponse() {
        String data = "test data";
        String message = "Success";
        
        ApiResponse<String> response = ApiResponse.ok(data, message);
        
        assertTrue(response.isSuccess());
        assertEquals(message, response.getMessage());
        assertEquals(data, response.getData());
        assertNull(response.getError());
        assertNotNull(response.getTimestamp(), "timestamp must be set");
    }

    @Test
    void testOkWithData_CreatesSuccessResponseWithDefaultMessage() {
        String data = "test data";
        
        ApiResponse<String> response = ApiResponse.ok(data);
        
        assertTrue(response.isSuccess());
        // The method signature is ok(T data), so data becomes the message
        assertEquals(data, response.getMessage());
        assertNull(response.getData());
        assertNull(response.getError());
        assertNotNull(response.getTimestamp(), "timestamp must be set");
    }

    @Test
    void testOkWithMessage_CreatesSuccessResponseWithoutData() {
        String message = "Operation successful";
        
        ApiResponse<Void> response = ApiResponse.ok(message);
        
        assertTrue(response.isSuccess());
        assertEquals(message, response.getMessage());
        assertNull(response.getData());
        assertNull(response.getError());
        assertNotNull(response.getTimestamp(), "timestamp must be set");
    }

    @Test
    void testOkWithoutParams_CreatesDefaultSuccessResponse() {
        ApiResponse<Void> response = ApiResponse.ok();
        
        assertTrue(response.isSuccess());
        assertEquals("OK", response.getMessage());
        assertNull(response.getData());
        assertNull(response.getError());
        assertNotNull(response.getTimestamp(), "timestamp must be set");
    }

    @Test
    void testFailWithMessageAndError_CreatesErrorResponse() {
        String message = "Operation failed";
        ApiError error = new ApiError("ERR_001", "Error details", null);
        
        ApiResponse<Void> response = ApiResponse.fail(message, error);
        
        assertFalse(response.isSuccess());
        assertEquals(message, response.getMessage());
        assertNull(response.getData());
        assertNotNull(response.getError());
        assertEquals("ERR_001", response.getError().getCode());
        assertNotNull(response.getTimestamp(), "timestamp must be set");
    }

    @Test
    void testFailWithMessageAndErrorCode_CreatesErrorResponse() {
        String message = "Operation failed";
        String errorCode = "ERR_002";
        
        ApiResponse<Void> response = ApiResponse.fail(message, errorCode);
        
        assertFalse(response.isSuccess());
        assertEquals(message, response.getMessage());
        assertNull(response.getData());
        assertNotNull(response.getError());
        assertEquals(errorCode, response.getError().getCode());
        assertNotNull(response.getTimestamp(), "timestamp must be set");
    }

    @Test
    void testFailWithMessageErrorCodeAndDetails_CreatesErrorResponse() {
        String message = "Operation failed";
        String errorCode = "ERR_003";
        String details = "Detailed error message";
        
        ApiResponse<Void> response = ApiResponse.fail(message, errorCode, details);
        
        assertFalse(response.isSuccess());
        assertEquals(message, response.getMessage());
        assertNull(response.getData());
        assertNotNull(response.getError());
        assertEquals(errorCode, response.getError().getCode());
        assertEquals(details, response.getError().getDetails());
        assertNotNull(response.getTimestamp(), "timestamp must be set");
    }

    @Test
    void testOkWithComplexData_HandlesComplexTypes() {
        List<String> data = Arrays.asList("item1", "item2", "item3");
        
        ApiResponse<List<String>> response = ApiResponse.ok(data, "Success");
        
        assertTrue(response.isSuccess());
        assertEquals(3, response.getData().size());
        assertEquals("item1", response.getData().get(0));
        assertNotNull(response.getTimestamp(), "timestamp must be set");
    }

    @Test
    void testSettersAndGetters_WorkCorrectly() {
        ApiResponse<String> response = new ApiResponse<>();
        ApiError error = new ApiError("ERR_001", "Details", null);
        Instant now = Instant.now();
        
        response.setSuccess(true);
        response.setMessage("Test message");
        response.setData("Test data");
        response.setError(error);
        response.setTimestamp(now);
        
        assertTrue(response.isSuccess());
        assertEquals("Test message", response.getMessage());
        assertEquals("Test data", response.getData());
        assertEquals(error, response.getError());
        assertEquals(now, response.getTimestamp());
    }

    @Test
    void testFourArgConstructor_SetsTimestampAutomatically() {
        ApiError error = new ApiError("ERR_001", "Details", null);
        
        ApiResponse<String> response = new ApiResponse<>(true, "Message", "Data", error);
        
        assertTrue(response.isSuccess());
        assertEquals("Message", response.getMessage());
        assertEquals("Data", response.getData());
        assertEquals(error, response.getError());
        assertNotNull(response.getTimestamp(), "4-arg constructor must auto-set timestamp");
    }

    @Test
    void testFiveArgConstructor_UsesProvidedTimestamp() {
        ApiError error = new ApiError("ERR_001", "Details", null);
        Instant fixedTime = Instant.parse("2025-01-01T00:00:00Z");
        
        ApiResponse<String> response = new ApiResponse<>(true, "Message", "Data", error, fixedTime);
        
        assertTrue(response.isSuccess());
        assertEquals("Message", response.getMessage());
        assertEquals("Data", response.getData());
        assertEquals(error, response.getError());
        assertEquals(fixedTime, response.getTimestamp());
    }

    @Test
    void testNoArgsConstructor_CreatesEmptyInstance() {
        ApiResponse<String> response = new ApiResponse<>();
        
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertNull(response.getMessage());
        assertNull(response.getData());
        assertNull(response.getError());
        assertNotNull(response.getTimestamp(), "No-arg constructor should still initialize timestamp at field level");
    }

    @Test
    void testTimestamp_IsRecentOnFactoryMethods() {
        Instant before = Instant.now();
        ApiResponse<Void> response = ApiResponse.ok();
        Instant after = Instant.now();

        assertNotNull(response.getTimestamp());
        assertFalse(response.getTimestamp().isBefore(before), "timestamp must not be before creation");
        assertFalse(response.getTimestamp().isAfter(after), "timestamp must not be after creation");
    }
}
