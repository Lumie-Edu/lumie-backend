package com.lumie.auth.application.port.out;

public interface AdminServicePort {

    void createOwnerAdmin(String tenantSlug, Long userId, String userLoginId,
                          String name, String phone, String email);
}
