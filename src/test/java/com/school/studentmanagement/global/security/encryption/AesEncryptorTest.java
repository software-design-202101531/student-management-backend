package com.school.studentmanagement.global.security.encryption;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesEncryptorTest {

    // 테스트 전용 키/솔트. 운영용 키와는 무관.
    private static final String TEST_PASSWORD = "test-encryption-password-do-not-use-in-prod";
    private static final String TEST_SALT_HEX = "0123456789abcdef0123456789abcdef";

    private final AesEncryptor encryptor = new AesEncryptor(TEST_PASSWORD, TEST_SALT_HEX);

    @Nested
    @DisplayName("랜덤 IV 모드 (AES-256-GCM)")
    class RandomMode {

        @Test
        @DisplayName("라운드트립: 같은 평문이 복호화로 복원됨")
        void roundTrip() {
            String plaintext = "01012345678";

            String encrypted = encryptor.encryptRandom(plaintext);

            assertThat(encryptor.decryptRandom(encrypted)).isEqualTo(plaintext);
        }

        @Test
        @DisplayName("같은 평문도 매번 다른 암호문 (랜덤 IV)")
        void differentCiphertextEachTime() {
            String plaintext = "서울시 종로구 청계천로 1";

            String first = encryptor.encryptRandom(plaintext);
            String second = encryptor.encryptRandom(plaintext);

            assertThat(first).isNotEqualTo(second);
            assertThat(encryptor.decryptRandom(first)).isEqualTo(plaintext);
            assertThat(encryptor.decryptRandom(second)).isEqualTo(plaintext);
        }

        @Test
        @DisplayName("한글·이모지·긴 텍스트도 라운드트립")
        void unicodeAndLong() {
            String plaintext = "🏫 학교 주소 — 서울 강남구 테헤란로 ".repeat(20);

            String encrypted = encryptor.encryptRandom(plaintext);

            assertThat(encryptor.decryptRandom(encrypted)).isEqualTo(plaintext);
        }
    }

    @Nested
    @DisplayName("결정적 모드 (AES-256-CBC + HMAC IV)")
    class DeterministicMode {

        @Test
        @DisplayName("같은 평문 → 항상 같은 암호문")
        void sameCiphertext() {
            String plaintext = "EMP00123";

            String first = encryptor.encryptDeterministic(plaintext);
            String second = encryptor.encryptDeterministic(plaintext);

            assertThat(first).isEqualTo(second);
        }

        @Test
        @DisplayName("다른 평문 → 다른 암호문")
        void differentCiphertext() {
            String a = encryptor.encryptDeterministic("EMP00123");
            String b = encryptor.encryptDeterministic("EMP00124");

            assertThat(a).isNotEqualTo(b);
        }

        @Test
        @DisplayName("라운드트립")
        void roundTrip() {
            String plaintext = "01099887766";

            String encrypted = encryptor.encryptDeterministic(plaintext);

            assertThat(encryptor.decryptDeterministic(encrypted)).isEqualTo(plaintext);
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class Failures {

        @Test
        @DisplayName("포맷 불일치(구분자 없음) → 명확한 예외")
        void invalidFormat() {
            assertThatThrownBy(() -> encryptor.decryptRandom("not-a-valid-format"))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("다른 키로 만든 암호문은 복호화 실패")
        void wrongKey() {
            AesEncryptor other = new AesEncryptor("another-password", "ffffffffffffffffffffffffffffffff");
            String enc = other.encryptRandom("secret");

            assertThatThrownBy(() -> encryptor.decryptRandom(enc))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("컨버터 경유 동작")
    class Converters {

        @Test
        @DisplayName("AesRandomStringConverter null 안전 + 라운드트립")
        void randomConverter() {
            AesRandomStringConverter conv = new AesRandomStringConverter(encryptor);

            assertThat(conv.convertToDatabaseColumn(null)).isNull();
            assertThat(conv.convertToEntityAttribute(null)).isNull();

            String plain = "서울시 강남구";
            String stored = conv.convertToDatabaseColumn(plain);
            assertThat(stored).isNotEqualTo(plain); // 실제로 암호화됨
            assertThat(conv.convertToEntityAttribute(stored)).isEqualTo(plain);
        }

        @Test
        @DisplayName("AesDeterministicStringConverter null 안전 + 동일 평문 → 동일 결과")
        void deterministicConverter() {
            AesDeterministicStringConverter conv = new AesDeterministicStringConverter(encryptor);

            assertThat(conv.convertToDatabaseColumn(null)).isNull();
            assertThat(conv.convertToEntityAttribute(null)).isNull();

            String plain = "EMP9999";
            String first = conv.convertToDatabaseColumn(plain);
            String second = conv.convertToDatabaseColumn(plain);
            assertThat(first).isEqualTo(second);
            assertThat(conv.convertToEntityAttribute(first)).isEqualTo(plain);
        }
    }
}
