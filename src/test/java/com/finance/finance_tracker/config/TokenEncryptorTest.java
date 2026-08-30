package com.finance.finance_tracker.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenEncryptorTest {

    private TokenEncryptor encryptor;

    @BeforeEach
    void setUp() {
        byte[] key = new byte[32]; // AES-256
        new Random(42).nextBytes(key); // детерминированный seed — тест воспроизводим, не флаки
        String base64Key = Base64.getEncoder().encodeToString(key);
        encryptor = new TokenEncryptor(base64Key);
    }

    @Test
    @DisplayName("round-trip: расшифровка зашифрованного значения возвращает исходную строку")
    void encryptThenDecrypt_returnsOriginalPlaintext() {
        String original = "real-oauth2-access-token-value-abc123";

        String encrypted = encryptor.encrypt(original);
        String decrypted = encryptor.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(original);
    }

    @Test
    @DisplayName("зашифрованное значение не совпадает с исходным текстом")
    void encrypt_producesCiphertextDifferentFromPlaintext() {
        String original = "some-refresh-token";
        assertThat(encryptor.encrypt(original)).isNotEqualTo(original);
    }

    @Test
    @DisplayName("два шифрования одного и того же значения дают РАЗНЫЙ ciphertext — доказывает, что IV меняется на каждый вызов")
    void encrypt_sameInputTwice_producesDifferentCiphertext() {
        String original = "same-token-value";

        String firstEncryption = encryptor.encrypt(original);
        String secondEncryption = encryptor.encrypt(original);

        assertThat(firstEncryption).isNotEqualTo(secondEncryption);

        assertThat(encryptor.decrypt(firstEncryption)).isEqualTo(original);
        assertThat(encryptor.decrypt(secondEncryption)).isEqualTo(original);
    }

    @Test
    @DisplayName("расшифровка повреждённых данных бросает понятное исключение, не молчит")
    void decrypt_corruptedData_throwsIllegalStateException() {
        assertThatThrownBy(() -> encryptor.decrypt("not-valid-base64-or-corrupted-ciphertext!!!"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("конструктор бросает понятную ошибку на пустой ключ, а не NPE где-то дальше при первом использовании")
    void constructor_blankKey_throwsIllegalStateException() {
        assertThatThrownBy(() -> new TokenEncryptor(""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("alfa.token-encryption-key");
    }

    @Test
    @DisplayName("конструктор бросает понятную ошибку на null-ключ")
    void constructor_nullKey_throwsIllegalStateException() {
        assertThatThrownBy(() -> new TokenEncryptor(null))
                .isInstanceOf(IllegalStateException.class);
    }
}