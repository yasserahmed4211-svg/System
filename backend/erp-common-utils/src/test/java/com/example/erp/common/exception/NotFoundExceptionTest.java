package com.example.erp.common.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NotFoundException
 */
class NotFoundExceptionTest {

    @Test
    void testConstructorWithMessage_CreatesInstance() {
        NotFoundException exception = new NotFoundException("User not found");
        
        assertEquals("User not found", exception.getMessage());
        assertNull(exception.getResourceName());
        assertNull(exception.getFieldName());
        assertNull(exception.getFieldValue());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithResourceFieldAndValue_CreatesInstance() {
        NotFoundException exception = new NotFoundException("User", "id", 123L);
        
        assertEquals("User not found with id='123'", exception.getMessage());
        assertEquals("User", exception.getResourceName());
        assertEquals("id", exception.getFieldName());
        assertEquals(123L, exception.getFieldValue());
    }

    @Test
    void testConstructorWithResourceFieldAndStringValue_CreatesInstance() {
        NotFoundException exception = new NotFoundException("Customer", "code", "CUST001");
        
        assertEquals("Customer not found with code='CUST001'", exception.getMessage());
        assertEquals("Customer", exception.getResourceName());
        assertEquals("code", exception.getFieldName());
        assertEquals("CUST001", exception.getFieldValue());
    }

    @Test
    void testConstructorWithMessageAndCause_CreatesInstance() {
        Throwable cause = new IllegalStateException("DB connection failed");
        
        NotFoundException exception = new NotFoundException("Resource not found", cause);
        
        assertEquals("Resource not found", exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertNull(exception.getResourceName());
        assertNull(exception.getFieldName());
        assertNull(exception.getFieldValue());
    }

    @Test
    void testExceptionIsRuntimeException() {
        NotFoundException exception = new NotFoundException("Not found");
        
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    void testGettersWorkCorrectly() {
        NotFoundException exception = new NotFoundException("Account", "accountCode", "ACC001");
        
        assertEquals("Account", exception.getResourceName());
        assertEquals("accountCode", exception.getFieldName());
        assertEquals("ACC001", exception.getFieldValue());
        assertTrue(exception.getMessage().contains("Account"));
        assertTrue(exception.getMessage().contains("accountCode"));
        assertTrue(exception.getMessage().contains("ACC001"));
    }

    @Test
    void testWithNullFieldValue_HandlesNull() {
        NotFoundException exception = new NotFoundException("Product", "id", null);
        
        assertEquals("Product", exception.getResourceName());
        assertEquals("id", exception.getFieldName());
        assertNull(exception.getFieldValue());
        assertEquals("Product not found with id='null'", exception.getMessage());
    }

    @Test
    void testWithNumericFieldValue_FormatsCorrectly() {
        NotFoundException exception = new NotFoundException("Invoice", "invoiceNumber", 12345);
        
        assertEquals(12345, exception.getFieldValue());
        assertTrue(exception.getMessage().contains("12345"));
    }

    @Test
    void testChainedCause_PreservesCauseChain() {
        Throwable rootCause = new IllegalArgumentException("Invalid ID");
        Throwable intermediateCause = new RuntimeException("Processing error", rootCause);
        
        NotFoundException exception = new NotFoundException(
            "Resource not found",
            intermediateCause
        );
        
        assertEquals(intermediateCause, exception.getCause());
        assertEquals(rootCause, exception.getCause().getCause());
    }

    @Test
    void testMultipleInstances_AreIndependent() {
        NotFoundException exception1 = new NotFoundException("User", "id", 1L);
        NotFoundException exception2 = new NotFoundException("Role", "id", 2L);
        
        assertEquals("User", exception1.getResourceName());
        assertEquals("Role", exception2.getResourceName());
        assertEquals(1L, exception1.getFieldValue());
        assertEquals(2L, exception2.getFieldValue());
    }
}
