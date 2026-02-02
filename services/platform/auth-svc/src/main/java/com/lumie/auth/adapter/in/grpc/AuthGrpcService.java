package com.lumie.auth.adapter.in.grpc;

import com.lumie.auth.application.port.in.ValidateTokenUseCase;
import com.lumie.auth.application.port.out.TenantServicePort;
import com.lumie.auth.application.port.out.UserLookupPort;
import com.lumie.auth.domain.vo.TokenClaims;
import com.lumie.grpc.auth.*;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.Optional;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class AuthGrpcService extends AuthServiceGrpc.AuthServiceImplBase {

    private final ValidateTokenUseCase validateTokenUseCase;
    private final UserLookupPort userLookupPort;
    private final TenantServicePort tenantServicePort;

    @Override
    public void validateToken(ValidateTokenRequest request, StreamObserver<ValidateTokenResponse> responseObserver) {
        log.debug("gRPC ValidateToken request received");

        try {
            Optional<TokenClaims> claimsOpt = validateTokenUseCase.validate(request.getToken());

            ValidateTokenResponse.Builder responseBuilder = ValidateTokenResponse.newBuilder();

            if (claimsOpt.isPresent()) {
                TokenClaims claims = claimsOpt.get();
                responseBuilder
                        .setValid(true)
                        .setMessage("Token is valid")
                        .setClaims(toGrpcClaims(claims));
            } else {
                responseBuilder
                        .setValid(false)
                        .setMessage("Token is invalid or expired");
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Failed to validate token", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getUserInfo(GetUserInfoRequest request, StreamObserver<UserInfoResponse> responseObserver) {
        log.debug("gRPC GetUserInfo request received");

        try {
            Optional<TokenClaims> claimsOpt = validateTokenUseCase.validate(request.getToken());

            UserInfoResponse.Builder responseBuilder = UserInfoResponse.newBuilder();

            if (claimsOpt.isEmpty()) {
                responseBuilder
                        .setSuccess(false)
                        .setMessage("Invalid or expired token");
                responseObserver.onNext(responseBuilder.build());
                responseObserver.onCompleted();
                return;
            }

            TokenClaims claims = claimsOpt.get();

            // Get user info from public.users
            var userOpt = userLookupPort.findById(claims.getUserId());
            if (userOpt.isEmpty()) {
                responseBuilder
                        .setSuccess(false)
                        .setMessage("User not found");
                responseObserver.onNext(responseBuilder.build());
                responseObserver.onCompleted();
                return;
            }

            var user = userOpt.get();
            responseBuilder
                    .setSuccess(true)
                    .setMessage("User info retrieved successfully")
                    .setUser(UserInfo.newBuilder()
                            .setId(user.id())
                            .setEmail(user.userLoginId())
                            .setName(user.name())
                            .setRole(user.role().name())
                            .setTenantSlug(claims.tenantSlug())
                            .setTenantId(claims.tenantId())
                            .build());

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Failed to get user info", e);
            responseObserver.onError(e);
        }
    }

    private com.lumie.grpc.auth.TokenClaims toGrpcClaims(TokenClaims claims) {
        return com.lumie.grpc.auth.TokenClaims.newBuilder()
                .setSub(claims.sub())
                .setTenantSlug(claims.tenantSlug())
                .setTenantId(claims.tenantId())
                .setRole(claims.role().name())
                .setIat(claims.iat().getEpochSecond())
                .setExp(claims.exp().getEpochSecond())
                .setJti(claims.jti())
                .build();
    }
}
