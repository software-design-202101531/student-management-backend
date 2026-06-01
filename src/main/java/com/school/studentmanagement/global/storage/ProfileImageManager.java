package com.school.studentmanagement.global.storage;

import com.school.studentmanagement.global.dto.ProfileImageResponse;
import com.school.studentmanagement.global.validation.ImageFileValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.function.Consumer;

/**
 * 학생/교사 공통 프로필 사진 교체 흐름.
 * 검증 → 신규 업로드 → 엔티티 key 교체 → (트랜잭션 종료 후) 기존/신규 객체 정리.
 *
 * 순서 주의: 외부 스토리지는 DB 트랜잭션과 원자적이지 않다. 신규 업로드를 먼저 하고
 * 커밋 성공 시에만 기존 객체를 지운다(깨진 링크 방지). 롤백 시에는 방금 올린 신규 객체를 보상 삭제한다.
 */
@Component
@RequiredArgsConstructor
public class ProfileImageManager {

    private final ImageFileValidator imageFileValidator;
    private final FileStorageService fileStorageService;

    /**
     * @param file       업로드 파일
     * @param keyPrefix  객체 key 접두사 (예: "profiles/students")
     * @param oldKey     교체 전 기존 객체 key (없으면 null)
     * @param keySetter  엔티티에 신규 key를 반영하는 메서드 (예: student::updateProfileImageKey)
     * @return 신규 객체의 presigned URL
     */
    public ProfileImageResponse replace(MultipartFile file, String keyPrefix,
                                        String oldKey, Consumer<String> keySetter) {
        imageFileValidator.validate(file);

        String newKey = fileStorageService.upload(file, keyPrefix);
        keySetter.accept(newKey);
        registerCleanup(oldKey, newKey);

        return new ProfileImageResponse(fileStorageService.presignedGetUrl(newKey));
    }

    private void registerCleanup(String oldKey, String newKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // 트랜잭션 밖이면 즉시 기존 객체 삭제 (드문 경로)
            fileStorageService.delete(oldKey);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    fileStorageService.delete(oldKey);   // 반영 완료 → 옛 사진 제거
                } else if (StringUtils.hasText(newKey)) {
                    fileStorageService.delete(newKey);   // 롤백 → 고아 객체 보상 삭제
                }
            }
        });
    }
}
