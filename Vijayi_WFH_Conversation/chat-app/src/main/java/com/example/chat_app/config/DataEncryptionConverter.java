package com.example.chat_app.config;

import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.util.SerializationUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.Base64;

//import org.apache.log4j.Logger;

@Converter
@NoArgsConstructor
public class DataEncryptionConverter implements AttributeConverter<Object, String> {

    //    @Value("${encryption.key}")
//   private String encryptionKey;
    private String encryptionKey = "secret-test-key1";
    private final byte[] iv = new byte[16];
    private final String transformationString = "AES/CBC/PKCS5Padding";
    private final String algorithm = "AES";

//    private static final org.apache.log4j.Logger logger = Logger.getLogger(DataEncryptionConverter.class);

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

    @SneakyThrows
    @Override
    public String convertToDatabaseColumn(Object attribute) {
        if (attribute == null) {
            return null;
        }
        Cipher cipher = initCipher(Cipher.ENCRYPT_MODE);
        byte[] bytes = SerializationUtils.serialize(attribute);
        return Base64.getEncoder().encodeToString(cipher.doFinal(bytes));
    }

    @SneakyThrows
    @Override
    public Object convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try{
            Cipher cipher = initCipher(Cipher.DECRYPT_MODE);
            byte[] bytes = cipher.doFinal(Base64.getDecoder().decode(dbData));
            return SerializationUtils.deserialize(bytes);
        } catch (Exception e){
//            logger.error("Error decrypting the value: " + dbData, e);
        }
        return "";
    }
}




