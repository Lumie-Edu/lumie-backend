package com.lumie.common.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TenantContextHolderTest {

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void setAndGetTenant() {
        TenantContextHolder.setTenant("test-tenant");

        assertEquals("test-tenant", TenantContextHolder.getTenant());
        assertEquals("test-tenant", TenantContextHolder.getTenantSlug());
    }

    @Test
    void getRequiredTenant_whenSet_returnsTenant() {
        TenantContextHolder.setTenant("test-tenant");

        assertEquals("test-tenant", TenantContextHolder.getRequiredTenant());
    }

    @Test
    void getRequiredTenant_whenNotSet_throwsException() {
        assertThrows(IllegalStateException.class, TenantContextHolder::getRequiredTenant);
    }

    @Test
    void getRequiredTenant_whenBlank_throwsException() {
        TenantContextHolder.setTenant("   ");

        assertThrows(IllegalStateException.class, TenantContextHolder::getRequiredTenant);
    }

    @Test
    void clear_removesTenant() {
        TenantContextHolder.setTenant("test-tenant");
        TenantContextHolder.clear();

        assertNull(TenantContextHolder.getTenant());
    }

    @Test
    void threadIsolation() throws InterruptedException {
        TenantContextHolder.setTenant("main-thread-tenant");

        Thread otherThread = new Thread(() -> {
            assertNull(TenantContextHolder.getTenant());
            TenantContextHolder.setTenant("other-thread-tenant");
            assertEquals("other-thread-tenant", TenantContextHolder.getTenant());
        });

        otherThread.start();
        otherThread.join();

        assertEquals("main-thread-tenant", TenantContextHolder.getTenant());
    }
}
