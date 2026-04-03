package com.example.erp.common.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BusinessException
 */
class BusinessExceptionTest {

    @Test
    void testConstructorWithCodeAndMessage_CreatesInstance() {
        BusinessException exception = new BusinessException("ERR_001", "Business error");
        
        assertEquals("ERR_001", exception.getCode());
        assertEquals("Business error", exception.getMessage());
        assertNull(exception.getDetails());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithCodeMessageAndDetails_CreatesInstance() {
        BusinessException exception = new BusinessException(
            "ERR_002", 
            "Duplicate account", 
            "Account code already exists in the system"
        );
        
        assertEquals("ERR_002", exception.getCode());
        assertEquals("Duplicate account", exception.getMessage());
        assertEquals("Account code already exists in the system", exception.getDetails());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithCodeMessageAndCause_CreatesInstance() {
        Throwable cause = new IllegalArgumentException("Invalid input");
        
        BusinessException exception = new BusinessException("ERR_003", "Business error", cause);
        
        assertEquals("ERR_003", exception.getCode());
        assertEquals("Business error", exception.getMessage());
        assertNull(exception.getDetails());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testConstructorWithAllParameters_CreatesInstance() {
        Throwable cause = new RuntimeException("Root cause");
        
        BusinessException exception = new BusinessException(
            "ERR_004",
            "Complex business error",
            "Detailed explanation",
            cause
        );
        
        assertEquals("ERR_004", exception.getCode());
        assertEquals("Complex business error", exception.getMessage());
        assertEquals("Detailed explanation", exception.getDetails());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testExceptionIsRuntimeException() {
        BusinessException exception = new BusinessException("ERR_001", "Error");
        
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    void testGettersWorkCorrectly() {
        BusinessException exception = new BusinessException(
            "DUPLICATE_CODE",
            "Duplicate account code",
            "The account code 'ACC001' already exists"
        );
        
        assertEquals("DUPLICATE_CODE", exception.getCode());
        assertEquals("Duplicate account code", exception.getMessage());
        assertEquals("The account code 'ACC001' already exists", exception.getDetails());
    }

    @Test
    void testWithNullDetails_HandlesNull() {
        BusinessException exception = new BusinessException("ERR_001", "Error", (String) null);
        
        assertNull(exception.getDetails());
    }

    @Test
    void testChainedCause_PreservesCauseChain() {
        Throwable rootCause = new IllegalStateException("Root");
        Throwable intermediateCause = new RuntimeException("Intermediate", rootCause);
        
        BusinessException exception = new BusinessException(
            "ERR_001",
            "Business error",
            intermediateCause
        );
        
        assertEquals(intermediateCause, exception.getCause());
        assertEquals(rootCause, exception.getCause().getCause());
    }
}
