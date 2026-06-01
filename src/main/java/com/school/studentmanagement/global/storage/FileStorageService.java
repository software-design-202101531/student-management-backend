package com.school.studentmanagement.global.storage;

import com.school.studentmanagement.global.exception.BusinessException;
import com.school.studentmanagement.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.util.UUID;

// MinIO(S3) 객체 저장/삭제/조회용 범용 스토리지 서비스.
// DB에는 객체 key만 저장하고, 조회 시 presigned URL을 발급한다.
@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final MinioProperties props;

    // 파일을 {keyPrefix}/{UUID}.{ext} key로 업로드하고 생성된 key를 반환한다.
    public String upload(MultipartFile file, String keyPrefix) {
        String key = buildKey(keyPrefix, file.getOriginalFilename());
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(props.bucket())
                    .key(key)
                    .contentType(file.getContentType())
                    .build();
            s3Client.putObject(request,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return key;
        } catch (IOException | RuntimeException e) {
            log.error("파일 업로드 실패 - key: {}", key, e);
            throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR);
        }
    }

    // 객체 삭제. 실패해도 본 흐름을 막지 않도록 best-effort(로그만)로 처리한다.
    public void delete(String key) {
        if (!StringUtils.hasText(key)) {
            return;
        }
        try {
            s3Client.deleteObject(b -> b.bucket(props.bucket()).key(key));
        } catch (RuntimeException e) {
            log.warn("파일 삭제 실패 - key: {} : {}", key, e.getMessage());
        }
    }

    /**
     * 조회용 presigned GET URL 발급 (TTL은 minio.presigned-url-ttl).
     */
    public String presignedGetUrl(String key) {
        if (!StringUtils.hasText(key)) {
            return null;
        }
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(props.bucket())
                .key(key)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(props.presignedUrlTtl())
                .getObjectRequest(getObjectRequest)
                .build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    private String buildKey(String keyPrefix, String originalFilename) {
        String ext = StringUtils.getFilenameExtension(originalFilename);
        String filename = (ext == null || ext.isBlank())
                ? UUID.randomUUID().toString()
                : UUID.randomUUID() + "." + ext.toLowerCase();
        return keyPrefix + "/" + filename;
    }
}
