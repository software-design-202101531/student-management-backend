package com.school.studentmanagement.global.security.encryption;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/**
 * exact match 검색 또는 unique 제약을 보존해야 하는 PII 컬럼에 적용하는 결정적 암호화 컨버터
 * (AES-256-CBC + HMAC-SHA256 기반 결정적 IV). 같은 평문은 항상 같은 암호문이 된다.
 *
 * <p>적용 예:
 * <ul>
 *   <li>{@code ParentInvitation.phoneNumber} — 학부모 가입 검증 시 exact match.</li>
 *   <li>{@code Teacher.employeeNumber} — DB unique 제약 보존.</li>
 * </ul></p>
 *
 * <p>약점: 동일 평문이 같은 암호문이라 빈도 분석 가능. 검색·무결성 보존이 꼭 필요한 컬럼에만 신중히 적용.</p>
 *
 * <p>{@code autoApply = false} — 엔티티 필드에 {@code @Convert(converter = AesDeterministicStringConverter.class)} 로 명시 적용.</p>
 */
@Converter(autoApply = false)
@Component
public class AesDeterministicStringConverter implements AttributeConverter<String, String> {

    private final AesEncryptor encryptor;

    public AesDeterministicStringConverter(AesEncryptor encryptor) {
        this.encryptor = encryptor;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        return encryptor.encryptDeterministic(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return encryptor.decryptDeterministic(dbData);
    }
}
