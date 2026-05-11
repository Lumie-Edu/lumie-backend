package com.lumie.exam.adapter.out.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.external-endpoint}")
    private String externalEndpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    @Bean
    @Qualifier("presignMinioClient")
    public MinioClient presignMinioClient() {
        return MinioClient.builder()
                .endpoint(externalEndpoint)
                .credentials(accessKey, secretKey)
                .region("us-east-1")
                .build();
    }
}
