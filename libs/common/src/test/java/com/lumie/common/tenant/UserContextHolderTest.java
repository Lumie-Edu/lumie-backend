package com.lumie.common.tenant;

import com.lumie.common.auth.Role;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.exception.CommonErrorCode;
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
        BusinessException exception = assertThrows(BusinessException.class, UserContextHolder::getRequiredUserId);
        assertEquals(CommonErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }

    @Test
    void setAndGetUserRole_normalizesLegacyAlias() {
        // "ADMIN" is a legacy alias that fromString maps to INSTRUCTOR.
        UserContextHolder.setUserRole("ADMIN");

        assertEquals(Role.INSTRUCTOR, UserContextHolder.getUserRole());
    }

    @Test
    void setUserRole_acceptsEnumDirectly() {
        UserContextHolder.setUserRole(Role.OWNER);

        assertEquals(Role.OWNER, UserContextHolder.getUserRole());
    }

    @Test
    void clear_removesAllContext() {
        UserContextHolder.setUserId(123L);
        UserContextHolder.setUserRole(Role.MANAGER);

        UserContextHolder.clear();

        assertNull(UserContextHolder.getUserId());
        assertNull(UserContextHolder.getUserRole());
    }

    @Test
    void threadIsolation() throws InterruptedException {
        UserContextHolder.setUserId(1L);
        UserContextHolder.setUserRole(Role.OWNER);

        Thread otherThread = new Thread(() -> {
            assertNull(UserContextHolder.getUserId());
            assertNull(UserContextHolder.getUserRole());

            UserContextHolder.setUserId(2L);
            UserContextHolder.setUserRole(Role.STUDENT);

            assertEquals(2L, UserContextHolder.getUserId());
            assertEquals(Role.STUDENT, UserContextHolder.getUserRole());
        });

        otherThread.start();
        otherThread.join();

        assertEquals(1L, UserContextHolder.getUserId());
        assertEquals(Role.OWNER, UserContextHolder.getUserRole());
    }
}
