package com.lumie.admin.adapter.in.grpc;

import com.lumie.admin.application.dto.AdminResponse;
import com.lumie.admin.application.service.AdminQueryService;
import com.lumie.admin.domain.entity.Admin;
import com.lumie.admin.domain.exception.AdminNotFoundException;
import com.lumie.admin.domain.repository.AdminPermissionRepository;
import com.lumie.admin.domain.repository.AdminRepository;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.grpc.admin.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class AdminGrpcService extends AdminServiceGrpc.AdminServiceImplBase {

    private final AdminQueryService adminQueryService;
    private final AdminRepository adminRepository;
    private final AdminPermissionRepository adminPermissionRepository;

    @Override
    public void getAdmin(GetAdminRequest request, StreamObserver<com.lumie.grpc.admin.AdminResponse> responseObserver) {
        try {
            TenantContextHolder.setTenant(request.getTenantSlug());
            AdminResponse admin = adminQueryService.getAdmin(request.getAdminId());
            responseObserver.onNext(toGrpcResponse(admin));
            responseObserver.onCompleted();
        } catch (AdminNotFoundException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            log.error("Error getting admin: {}", request.getAdminId(), e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public void getAdminByUserId(GetAdminByUserIdRequest request,
                                  StreamObserver<com.lumie.grpc.admin.AdminResponse> responseObserver) {
        try {
            TenantContextHolder.setTenant(request.getTenantSlug());
            Admin admin = adminRepository.findByUserId(request.getUserId())
                    .orElseThrow(() -> new AdminNotFoundException("userId: " + request.getUserId()));
            responseObserver.onNext(toGrpcResponseFromEntity(admin));
            responseObserver.onCompleted();
        } catch (AdminNotFoundException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            log.error("Error getting admin by userId: {}", request.getUserId(), e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public void validateAdmin(ValidateAdminRequest request,
                               StreamObserver<ValidateAdminResponse> responseObserver) {
        try {
            TenantContextHolder.setTenant(request.getTenantSlug());
            try {
                adminQueryService.getAdmin(request.getAdminId());
                responseObserver.onNext(ValidateAdminResponse.newBuilder()
                    .setValid(true)
                    .setAdminId(request.getAdminId())
                    .setMessage("Admin is valid")
                    .build());
            } catch (AdminNotFoundException e) {
                responseObserver.onNext(ValidateAdminResponse.newBuilder()
                    .setValid(false)
                    .setAdminId(request.getAdminId())
                    .setMessage("Admin not found")
                    .build());
            }
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error validating admin: {}", request.getAdminId(), e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public void getAdminPermissions(GetAdminPermissionsRequest request,
                                     StreamObserver<AdminPermissionsResponse> responseObserver) {
        try {
            TenantContextHolder.setTenant(request.getTenantSlug());
            Admin admin = adminRepository.findById(request.getAdminId())
                    .orElseThrow(() -> new AdminNotFoundException(request.getAdminId()));

            AdminPermissionsResponse.Builder builder = AdminPermissionsResponse.newBuilder()
                    .setAdminId(admin.getId());

            adminPermissionRepository.findByAdminId(admin.getId())
                    .forEach(ap -> builder.addPermissions(
                            PermissionEntry.newBuilder()
                                    .setPermissionCode(ap.getPermissionCode())
                                    .setAccessLevel(ap.getAccessLevel().name())
                                    .build()));

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (AdminNotFoundException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            log.error("Error getting admin permissions: {}", request.getAdminId(), e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public void getAdminsByAcademy(GetAdminsByAcademyRequest request,
                                    StreamObserver<AdminListResponse> responseObserver) {
        try {
            TenantContextHolder.setTenant(request.getTenantSlug());

            var filter = new com.lumie.admin.application.dto.AdminFilterRequest(
                    null, request.getAcademyId(), true, null);
            var pageable = org.springframework.data.domain.PageRequest.of(request.getPage(), request.getSize());
            var admins = adminQueryService.getAllAdmins(filter, pageable);

            AdminListResponse.Builder responseBuilder = AdminListResponse.newBuilder()
                    .setTotalCount(admins.getTotalElements())
                    .setPage(request.getPage())
                    .setSize(request.getSize());

            for (AdminResponse admin : admins.getContent()) {
                responseBuilder.addAdmins(toGrpcResponse(admin));
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error getting admins by academy: {}", request.getAcademyId(), e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        } finally {
            TenantContextHolder.clear();
        }
    }

    private com.lumie.grpc.admin.AdminResponse toGrpcResponse(AdminResponse dto) {
        var builder = com.lumie.grpc.admin.AdminResponse.newBuilder()
                .setId(dto.id())
                .setUserId(dto.userId())
                .setUserLoginId(dto.userLoginId())
                .setName(dto.name())
                .setPhone(dto.phone() != null ? dto.phone() : "")
                .setEmail(dto.email() != null ? dto.email() : "")
                .setIsActive(dto.isActive())
                .setEmploymentStatus(dto.employmentStatus() != null ? dto.employmentStatus().name() : "")
                .setCreatedAt(dto.createdAt().toString())
                .setUpdatedAt(dto.updatedAt().toString());

        if (dto.position() != null) {
            builder.setPositionId(dto.position().id());
            builder.setPositionName(dto.position().name());
        }

        return builder.build();
    }

    private com.lumie.grpc.admin.AdminResponse toGrpcResponseFromEntity(Admin admin) {
        var builder = com.lumie.grpc.admin.AdminResponse.newBuilder()
                .setId(admin.getId())
                .setUserId(admin.getUserId())
                .setUserLoginId(admin.getUserLoginId())
                .setName(admin.getAdminName())
                .setPhone(admin.getAdminPhone() != null ? admin.getAdminPhone() : "")
                .setEmail(admin.getEmail() != null ? admin.getEmail() : "")
                .setIsActive(admin.isActive())
                .setEmploymentStatus(admin.getEmploymentStatus() != null ? admin.getEmploymentStatus().name() : "")
                .setCreatedAt(admin.getCreatedAt().toString())
                .setUpdatedAt(admin.getUpdatedAt().toString());

        if (admin.getPosition() != null) {
            builder.setPositionId(admin.getPosition().getId());
            builder.setPositionName(admin.getPosition().getName());
        }

        return builder.build();
    }
}
