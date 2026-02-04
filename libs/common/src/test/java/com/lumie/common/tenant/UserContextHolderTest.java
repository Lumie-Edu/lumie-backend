package com.lumie.common.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserContextHolderTest {

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void setAndGetUserId() {
        UserContextHolder.setUserId(123L);

        assertEquals(123L, UserContextHolder.getUserId());
    }

    @Test
    void getRequiredUserId_whenSet_returnsUserId() {
        UserContextHolder.setUserId(456L);

        assertEquals(456L, UserContextHolder.getRequiredUserId());
    }

    @Test
    void getRequiredUserId_whenNotSet_throwsException() {
        assertThrows(IllegalStateException.class, UserContextHolder::getRequiredUserId);
    }

    @Test
    void setAndGetUserRole() {
        UserContextHolder.setUserRole("ADMIN");

        assertEquals("ADMIN", UserContextHolder.getUserRole());
    }

    @Test
    void clear_removesAllContext() {
        UserContextHolder.setUserId(123L);
        UserContextHolder.setUserRole("ADMIN");

        UserContextHolder.clear();

        assertNull(UserContextHolder.getUserId());
        assertNull(UserContextHolder.getUserRole());
    }

    @Test
    void threadIsolation() throws InterruptedException {
        UserContextHolder.setUserId(1L);
        UserContextHolder.setUserRole("ADMIN");

        Thread otherThread = new Thread(() -> {
            assertNull(UserContextHolder.getUserId());
            assertNull(UserContextHolder.getUserRole());

            UserContextHolder.setUserId(2L);
            UserContextHolder.setUserRole("STUDENT");

            assertEquals(2L, UserContextHolder.getUserId());
            assertEquals("STUDENT", UserContextHolder.getUserRole());
        });

        otherThread.start();
        otherThread.join();

        assertEquals(1L, UserContextHolder.getUserId());
        assertEquals("ADMIN", UserContextHolder.getUserRole());
    }
}
