package com.school.studentmanagement.global.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
@Slf4j
public class MinioConfig {

    @Bean
    public S3Client s3Client(MinioProperties props) {
        return S3Client.builder()
                .endpointOverride(URI.create(props.endpoint()))
                .region(Region.of(props.region()))
                .credentialsProvider(credentials(props))
                .forcePathStyle(true)
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(MinioProperties props) {
        return S3Presigner.builder()
                .endpointOverride(URI.create(props.endpoint()))
                .region(Region.of(props.region()))
                .credentialsProvider(credentials(props))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    private StaticCredentialsProvider credentials(MinioProperties props) {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(props.accessKey(), props.secretKey()));
    }


    // 버킷 생성 메서드
    @Bean
    public ApplicationRunner minioBucketInitializer(S3Client s3Client, MinioProperties props) {
        return args -> {
            try {
                s3Client.headBucket(b -> b.bucket(props.bucket()));
            } catch (NoSuchBucketException e) {
                s3Client.createBucket(b -> b.bucket(props.bucket()));
                log.info("MinIO 버킷 생성: {}", props.bucket());
            } catch (Exception e) {
                log.warn("MinIO 버킷 확인/생성 실패 (MinIO 미기동 가능성): {}", e.getMessage());
            }
        };
    }
}
