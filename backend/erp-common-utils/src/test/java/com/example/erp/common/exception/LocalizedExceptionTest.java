package com.example.erp.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LocalizedException
 */
class LocalizedExceptionTest {

    @Test
    void testConstructorWithStatusAndMessageKey_CreatesInstance() {
        LocalizedException exception = new LocalizedException(
            HttpStatus.NOT_FOUND,
            "error.user.notfound"
        );
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("error.user.notfound", exception.getMessageKey());
        assertNotNull(exception.getArgs());
        assertEquals(0, exception.getArgs().length);
    }

    @Test
    void testConstructorWithStatusMessageKeyAndArgs_CreatesInstance() {
        LocalizedException exception = new LocalizedException(
            HttpStatus.BAD_REQUEST,
            "error.validation.field",
            "username",
            "admin"
        );
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("error.validation.field", exception.getMessageKey());
        assertEquals(2, exception.getArgs().length);
        assertEquals("username", exception.getArgs()[0]);
        assertEquals("admin", exception.getArgs()[1]);
    }

    @Test
    void testConstructorWithMessageKeyOnly_UsesDefaultBadRequestStatus() {
        LocalizedException exception = new LocalizedException("error.generic");
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("error.generic", exception.getMessageKey());
        assertEquals(0, exception.getArgs().length);
    }

    @Test
    void testConstructorWithMessageKeyAndArgs_UsesDefaultBadRequestStatus() {
        LocalizedException exception = new LocalizedException(
            "error.field.invalid",
            "email",
            "test@example.com"
        );
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("error.field.invalid", exception.getMessageKey());
        assertEquals(2, exception.getArgs().length);
        assertEquals("email", exception.getArgs()[0]);
        assertEquals("test@example.com", exception.getArgs()[1]);
    }

    @Test
    void testGetMessage_ReturnsMessageKey() {
        LocalizedException exception = new LocalizedException("error.test");
        
        assertEquals("error.test", exception.getMessage());
        assertEquals("error.test", exception.getMessageKey());
    }

    @Test
    void testExceptionIsRuntimeException() {
        LocalizedException exception = new LocalizedException("error.test");
        
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    void testWithDifferentHttpStatuses_HandlesAllStatuses() {
        LocalizedException notFound = new LocalizedException(HttpStatus.NOT_FOUND, "error.notfound");
        LocalizedException forbidden = new LocalizedException(HttpStatus.FORBIDDEN, "error.forbidden");
        LocalizedException conflict = new LocalizedException(HttpStatus.CONFLICT, "error.conflict");
        
        assertEquals(HttpStatus.NOT_FOUND, notFound.getStatus());
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatus());
        assertEquals(HttpStatus.CONFLICT, conflict.getStatus());
    }

    @Test
    void testWithMultipleArgs_PreservesAllArgs() {
        Object[] args = {"arg1", 123, true, 45.67};
        
        LocalizedException exception = new LocalizedException(
            HttpStatus.BAD_REQUEST,
            "error.multi",
            args
        );
        
        assertEquals(4, exception.getArgs().length);
        assertEquals("arg1", exception.getArgs()[0]);
        assertEquals(123, exception.getArgs()[1]);
        assertEquals(true, exception.getArgs()[2]);
        assertEquals(45.67, exception.getArgs()[3]);
    }

    @Test
    void testWithNoArgs_HandlesEmptyArgsArray() {
        LocalizedException exception = new LocalizedException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "error.internal"
        );
        
        assertNotNull(exception.getArgs());
        assertEquals(0, exception.getArgs().length);
    }

    @Test
    void testGettersWorkCorrectly() {
        LocalizedException exception = new LocalizedException(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "error.business.rule",
            "field1",
            "value1"
        );
        
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
        assertEquals("error.business.rule", exception.getMessageKey());
        assertEquals("error.business.rule", exception.getMessage());
        assertEquals(2, exception.getArgs().length);
    }
}
