package com.school.studentmanagement.global.security.encryption;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/**
 * PII 컬럼 중 검색 사용처가 없는 컬럼에 적용하는 랜덤 IV 암호화 컨버터(AES-256-GCM).
 * 동일 평문도 매번 다른 암호문이 되어 빈도 분석을 막는다.
 *
 * <p>적용 예: {@code Student.phoneNumber}, {@code Student.address},
 * {@code Parent.phoneNumber}, {@code Teacher.officePhoneNumber}.</p>
 *
 * <p>{@code autoApply = false} — 엔티티 필드에 {@code @Convert(converter = AesRandomStringConverter.class)} 로 명시 적용.</p>
 */
@Converter(autoApply = false)
@Component
public class AesRandomStringConverter implements AttributeConverter<String, String> {

    private final AesEncryptor encryptor;

    public AesRandomStringConverter(AesEncryptor encryptor) {
        this.encryptor = encryptor;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        return encryptor.encryptRandom(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return encryptor.decryptRandom(dbData);
    }
}
