package com.example.chat_app.config;

import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.Base64;

@Component
@Converter
@NoArgsConstructor
public class NewDataEncryptionConverter implements AttributeConverter<Object, String> {

    private String encryptionKey = "secret-test-key1";
    private final byte[] iv = new byte[16];
    private final String transformationString = "AES/CBC/PKCS5Padding";
    private final String algorithm = "AES";

    private Key createKey() {
        return new SecretKeySpec(encryptionKey.getBytes(), algorithm);
    }

    private Cipher createCipher() throws GeneralSecurityException {
        return Cipher.getInstance(transformationString);
    }

    // Initialize cipher into the memory -- decides whether we want to encrypt or decrypt data with the cipher
    private Cipher initCipher(int encryptMode) throws GeneralSecurityException {
        Cipher cipher = createCipher();
        Key key = createKey();
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        cipher.init(encryptMode, key, ivParameterSpec);
        return cipher;
    }

    @Override
    public String convertToDatabaseColumn(Object attribute) {
        if (attribute == null) {
            return null;
        }
        if (!(attribute instanceof String)) {
            throw new IllegalArgumentException("Expected String, got " + attribute.getClass().getName());
        }
        try {
            Cipher cipher = initCipher(Cipher.ENCRYPT_MODE);
            byte[] encrypted = cipher.doFinal(((String) attribute).getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    @Override
    public Object convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            Cipher cipher = initCipher(Cipher.DECRYPT_MODE);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(dbData));
            return new String(decrypted);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}




