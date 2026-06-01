package com.school.studentmanagement.global.security.encryption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 컬럼 레벨 암호화용 AES 헬퍼. 두 모드를 제공한다:
 * <ul>
 *   <li><b>{@link #encryptRandom}/{@link #decryptRandom}</b> — AES-256-GCM + 랜덤 IV. 같은 평문도 매번 다른 암호문.
 *   검색·동일성 비교가 필요 없는 PII에 적용.</li>
 *   <li><b>{@link #encryptDeterministic}/{@link #decryptDeterministic}</b> — AES-256-CBC + HMAC-SHA256으로 평문에서 결정적으로 유도한 IV.
 *   같은 평문 → 같은 암호문이라 DB exact match·unique 제약을 보존.</li>
 * </ul>
 *
 * <p>키 derivation: PBKDF2WithHmacSHA256, 65,536 라운드, 256비트.</p>
 *
 * <p>저장 포맷: {@code base64(iv) + ":" + base64(ciphertext)}. ":"는 base64 알파벳에 없어 안전한 구분자.</p>
 *
 * <p><b>키({@code APP_ENCRYPTION_PASSWORD}/{@code APP_ENCRYPTION_SALT}) 분실 시 데이터 복호화 불가</b> — 운영 가이드 필수.</p>
 */
@Component
public class AesEncryptor {

    private static final String AES = "AES";
    private static final String GCM_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String CBC_TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";

    private static final int KEY_BITS = 256;
    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final int CBC_IV_BYTES = 16;
    private static final int PBKDF2_ITERATIONS = 65_536;
    private static final char FORMAT_SEPARATOR = ':';

    private final SecretKey aesKey;
    private final SecretKey hmacKey; // deterministic IV 유도용 — AES 키와 분리

    private final SecureRandom secureRandom = new SecureRandom();

    public AesEncryptor(
            @Value("${app.encryption.password}") String password,
            @Value("${app.encryption.salt}") String saltHex
    ) {
        byte[] saltBytes = HexFormat.of().parseHex(saltHex);
        byte[] keyMaterial = deriveKey(password.toCharArray(), saltBytes, KEY_BITS);
        this.aesKey = new SecretKeySpec(keyMaterial, AES);
        // HMAC 키는 AES 키와 도메인 분리(같은 키 재사용 회피) — salt 뒤집어 한 번 더 derive
        byte[] reversedSalt = new byte[saltBytes.length];
        for (int i = 0; i < saltBytes.length; i++) {
            reversedSalt[i] = saltBytes[saltBytes.length - 1 - i];
        }
        byte[] hmacMaterial = deriveKey(password.toCharArray(), reversedSalt, KEY_BITS);
        this.hmacKey = new SecretKeySpec(hmacMaterial, HMAC_ALGORITHM);
    }

    /** 랜덤 IV 모드 — 같은 평문도 매번 다른 암호문 (AES-256-GCM). */
    public String encryptRandom(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(GCM_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return encode(iv, ciphertext);
        } catch (Exception e) {
            throw new IllegalStateException("랜덤 IV 암호화 실패", e);
        }
    }

    public String decryptRandom(String encoded) {
        try {
            byte[][] parts = decode(encoded);
            byte[] iv = parts[0];
            byte[] ciphertext = parts[1];
            Cipher cipher = Cipher.getInstance(GCM_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("랜덤 IV 복호화 실패", e);
        }
    }

    /** 결정적 모드 — 같은 평문 → 같은 암호문 (AES-256-CBC, IV는 HMAC(평문)에서 유도). */
    public String encryptDeterministic(String plaintext) {
        try {
            byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
            byte[] iv = deriveIv(plaintextBytes);
            Cipher cipher = Cipher.getInstance(CBC_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new IvParameterSpec(iv));
            byte[] ciphertext = cipher.doFinal(plaintextBytes);
            return encode(iv, ciphertext);
        } catch (Exception e) {
            throw new IllegalStateException("결정적 암호화 실패", e);
        }
    }

    public String decryptDeterministic(String encoded) {
        try {
            byte[][] parts = decode(encoded);
            byte[] iv = parts[0];
            byte[] ciphertext = parts[1];
            Cipher cipher = Cipher.getInstance(CBC_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new IvParameterSpec(iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("결정적 복호화 실패", e);
        }
    }

    // ─── 내부 헬퍼 ─────────────────────────────────────────

    private static byte[] deriveKey(char[] password, byte[] salt, int keyBits) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            PBEKeySpec spec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, keyBits);
            return factory.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("키 derivation 실패", e);
        }
    }

    private byte[] deriveIv(byte[] plaintextBytes) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(hmacKey);
            byte[] hmac = mac.doFinal(plaintextBytes);
            return Arrays.copyOf(hmac, CBC_IV_BYTES); // 앞 16바이트만 사용
        } catch (Exception e) {
            throw new IllegalStateException("결정적 IV 유도 실패", e);
        }
    }

    private static String encode(byte[] iv, byte[] ciphertext) {
        Base64.Encoder encoder = Base64.getEncoder().withoutPadding();
        return encoder.encodeToString(iv) + FORMAT_SEPARATOR + encoder.encodeToString(ciphertext);
    }

    private static byte[][] decode(String encoded) {
        int sep = encoded.indexOf(FORMAT_SEPARATOR);
        if (sep < 0) {
            throw new IllegalArgumentException("암호문 포맷 불일치: 구분자 없음");
        }
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] iv = decoder.decode(encoded.substring(0, sep));
        byte[] ciphertext = decoder.decode(encoded.substring(sep + 1));
        return new byte[][]{iv, ciphertext};
    }
}
