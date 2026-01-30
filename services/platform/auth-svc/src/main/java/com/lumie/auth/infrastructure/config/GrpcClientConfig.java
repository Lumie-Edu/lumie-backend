package com.lumie.auth.infrastructure.config;

import org.springframework.context.annotation.Configuration;

/**
 * gRPC client configuration for tenant-svc.
 * The actual configuration is done via application.yml:
 *
 * grpc:
 *   client:
 *     tenant-svc:
 *       address: static://tenant-svc-grpc.lumie-platform.svc:9090
 *       negotiationType: plaintext
 */
@Configuration
public class GrpcClientConfig {
    // Configuration is handled through application.yml
    // This class serves as a placeholder for any programmatic configuration needs
}
