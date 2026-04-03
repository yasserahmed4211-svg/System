package com.example.erp.common.multitenancy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TenantContext
 */
class TenantContextTest {

    @BeforeEach
    void setUp() {
        // Clear tenant context before each test
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        // Clear tenant context after each test
        TenantContext.clear();
    }

    @Test
    void testSetTenantId_StoresTenantId() {
        TenantContext.setTenantId("TENANT_001");
        
        assertEquals("tenant_001", TenantContext.getTenantId());
    }

    @Test
    void testGetTenantId_WithNoTenant_ReturnsNull() {
        assertNull(TenantContext.getTenantId());
    }

    @Test
    void testSetTenantId_OverwritesPreviousTenant() {
        TenantContext.setTenantId("TENANT_001");
        TenantContext.setTenantId("TENANT_002");
        
        assertEquals("tenant_002", TenantContext.getTenantId());
    }

    @Test
    void testClear_RemovesTenantId() {
        TenantContext.setTenantId("TENANT_001");
        TenantContext.clear();
        
        assertNull(TenantContext.getTenantId());
    }

    @Test
    void testClear_WithNoTenant_DoesNotThrowException() {
        assertDoesNotThrow(() -> TenantContext.clear());
    }

    @Test
    void testMultipleSetAndGet_WorksCorrectly() {
        TenantContext.setTenantId("TENANT_A");
        assertEquals("tenant_a", TenantContext.getTenantId());
        
        TenantContext.setTenantId("TENANT_B");
        assertEquals("tenant_b", TenantContext.getTenantId());
        
        TenantContext.setTenantId("TENANT_C");
        assertEquals("tenant_c", TenantContext.getTenantId());
    }

    @Test
    void testSetTenantId_WithNull_StoresNull() {
        TenantContext.setTenantId("TENANT_001");
        TenantContext.setTenantId(null);
        
        assertNull(TenantContext.getTenantId());
    }

    @Test
    void testSetTenantId_WithEmptyString_StoresEmptyString() {
        TenantContext.setTenantId("");
        
        assertEquals("", TenantContext.getTenantId());
    }

    @Test
    void testThreadLocal_IsolatesBetweenThreads() throws InterruptedException {
        TenantContext.setTenantId("MAIN_TENANT");
        
        Thread thread = new Thread(() -> {
            assertNull(TenantContext.getTenantId());
            TenantContext.setTenantId("THREAD_TENANT");
            assertEquals("thread_tenant", TenantContext.getTenantId());
        });
        
        thread.start();
        thread.join();
        
        // Main thread should still have its own tenant
        assertEquals("main_tenant", TenantContext.getTenantId());
    }

    @Test
    void testConstructor_ThrowsException() throws Exception {
        var constructor = TenantContext.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        
        var exception = assertThrows(java.lang.reflect.InvocationTargetException.class, () -> {
            constructor.newInstance();
        });
        
        assertInstanceOf(UnsupportedOperationException.class, exception.getCause());
    }
}
