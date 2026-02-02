package com.lumie.academy.adapter.out.external;

import com.lumie.academy.application.port.out.AuthServicePort;
import com.lumie.grpc.auth.AuthServiceGrpc;
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
}
