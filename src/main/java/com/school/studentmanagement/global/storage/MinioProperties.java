package com.school.studentmanagement.global.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "minio")
public record MinioProperties(
        String endpoint,
        String region,
        String accessKey,
        String secretKey,
        String bucket,
        Duration presignedUrlTtl
) {
}
