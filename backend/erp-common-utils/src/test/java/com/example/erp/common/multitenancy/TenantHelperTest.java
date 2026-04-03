package com.example.erp.common.multitenancy;

import com.example.erp.common.exception.LocalizedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TenantHelper
 */
class TenantHelperTest {

    @BeforeEach
    void setUp() {
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void testRequireTenant_WithValidTenant_ReturnsTenant() {
        TenantContext.setTenantId("TENANT_001");
        
        String result = TenantHelper.requireTenant();
        
        assertEquals("tenant_001", result);
    }

    @Test
    void testRequireTenant_WithNoTenant_ThrowsException() {
        LocalizedException exception = assertThrows(
            LocalizedException.class,
            () -> TenantHelper.requireTenant()
        );
        
        assertEquals("MISSING_TENANT", exception.getMessageKey());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void testRequireTenant_WithNullTenant_ThrowsException() {
        TenantContext.setTenantId(null);
        
        assertThrows(LocalizedException.class, () -> TenantHelper.requireTenant());
    }

    @Test
    void testRequireTenant_WithBlankTenant_ThrowsException() {
        TenantContext.setTenantId("   ");
        
        assertThrows(LocalizedException.class, () -> TenantHelper.requireTenant());
    }

    @Test
    void testRequireTenant_WithEmptyTenant_ThrowsException() {
        TenantContext.setTenantId("");
        
        assertThrows(LocalizedException.class, () -> TenantHelper.requireTenant());
    }

    @Test
    void testGetTenantOrDefault_WithValidTenant_ReturnsTenant() {
        TenantContext.setTenantId("TENANT_001");
        
        String result = TenantHelper.getTenantOrDefault("DEFAULT_TENANT");
        
        assertEquals("tenant_001", result);
    }

    @Test
    void testGetTenantOrDefault_WithNoTenant_ReturnsDefault() {
        String result = TenantHelper.getTenantOrDefault("DEFAULT_TENANT");
        
        assertEquals("default_tenant", result);
    }

    @Test
    void testGetTenantOrDefault_WithNullTenant_ReturnsDefault() {
        TenantContext.setTenantId(null);
        
        String result = TenantHelper.getTenantOrDefault("DEFAULT_TENANT");
        
        assertEquals("default_tenant", result);
    }

    @Test
    void testGetTenantOrDefault_WithBlankTenant_ReturnsDefault() {
        TenantContext.setTenantId("   ");
        
        String result = TenantHelper.getTenantOrDefault("DEFAULT_TENANT");
        
        assertEquals("default_tenant", result);
    }

    @Test
    void testGetTenantOrDefault_WithEmptyTenant_ReturnsDefault() {
        TenantContext.setTenantId("");
        
        String result = TenantHelper.getTenantOrDefault("DEFAULT_TENANT");
        
        assertEquals("default_tenant", result);
    }

    @Test
    void testGetTenantOrDefault_WithNullDefault_ReturnsNull() {
        String result = TenantHelper.getTenantOrDefault(null);
        
        assertNull(result);
    }

    @Test
    void testRequireAndGetOrDefault_ConsistentBehavior() {
        TenantContext.setTenantId("TENANT_001");
        
        String required = TenantHelper.requireTenant();
        String orDefault = TenantHelper.getTenantOrDefault("DEFAULT");
        
        assertEquals(required, orDefault);
    }

    @Test
    void testConstructor_ThrowsException() throws Exception {
        var constructor = TenantHelper.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        
        var exception = assertThrows(java.lang.reflect.InvocationTargetException.class, () -> {
            constructor.newInstance();
        });
        
        assertInstanceOf(UnsupportedOperationException.class, exception.getCause());
    }

    @Test
    void testMultipleCalls_WorkConsistently() {
        TenantContext.setTenantId("TENANT_001");
        
        assertEquals("tenant_001", TenantHelper.requireTenant());
        assertEquals("tenant_001", TenantHelper.requireTenant());
        assertEquals("tenant_001", TenantHelper.getTenantOrDefault("DEFAULT"));
    }
}
