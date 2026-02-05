package com.lumie.academy.adapter.out.external;

import com.lumie.academy.application.port.out.AuthServicePort;
import com.lumie.grpc.auth.AuthServiceGrpc;
import com.lumie.grpc.auth.CreateUserResponse;
import com.lumie.grpc.auth.DeleteUserRequest;
import com.lumie.grpc.auth.DeleteUserResponse;
import com.lumie.grpc.auth.ValidateTokenRequest;
import com.lumie.grpc.auth.ValidateTokenResponse;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class AuthServiceClient implements AuthServicePort {

    @GrpcClient("auth-svc")
    private AuthServiceGrpc.AuthServiceBlockingStub authServiceStub;

    @Override
    public Optional<TokenClaimsData> validateToken(String token) {
        log.debug("Validating token via gRPC");

        try {
            ValidateTokenRequest request = ValidateTokenRequest.newBuilder()
                    .setToken(token)
                    .build();

            ValidateTokenResponse response = authServiceStub.validateToken(request);

            if (!response.getValid()) {
                log.debug("Token validation failed: {}", response.getMessage());
                return Optional.empty();
            }

            var claims = response.getClaims();
            return Optional.of(new TokenClaimsData(
                    Long.parseLong(claims.getSub()),
                    claims.getTenantSlug(),
                    claims.getTenantId(),
                    claims.getRole()
            ));
        } catch (StatusRuntimeException e) {
            log.error("gRPC error validating token", e);
            return Optional.empty();
        } catch (NumberFormatException e) {
            log.error("Failed to parse user ID from token claims", e);
            return Optional.empty();
        }
    }

    @Override
    public CreateUserResult createUser(CreateUserRequest request) {
        log.debug("Creating user via gRPC: {}", request.userLoginId());

        try {
            com.lumie.grpc.auth.CreateUserRequest grpcRequest = com.lumie.grpc.auth.CreateUserRequest.newBuilder()
                    .setUserLoginId(request.userLoginId())
                    .setPassword(request.password())
                    .setName(request.name())
                    .setPhone(request.phone() != null ? request.phone() : "")
                    .setRole(request.role())
                    .setTenantId(request.tenantId())
                    .build();

            CreateUserResponse response = authServiceStub.createUser(grpcRequest);

            return new CreateUserResult(
                    response.getSuccess(),
                    response.getMessage(),
                    response.getUserId()
            );
        } catch (StatusRuntimeException e) {
            log.error("gRPC error creating user", e);
            return new CreateUserResult(false, "gRPC error: " + e.getMessage(), null);
        }
    }

    @Override
    public DeleteUserResult deleteUser(Long userId) {
        log.debug("Deleting user via gRPC: {}", userId);

        try {
            DeleteUserRequest grpcRequest = DeleteUserRequest.newBuilder()
                    .setUserId(userId)
                    .build();

            DeleteUserResponse response = authServiceStub.deleteUser(grpcRequest);

            return new DeleteUserResult(
                    response.getSuccess(),
                    response.getMessage()
            );
        } catch (StatusRuntimeException e) {
            log.error("gRPC error deleting user", e);
            return new DeleteUserResult(false, "gRPC error: " + e.getMessage());
        }
    }
}
