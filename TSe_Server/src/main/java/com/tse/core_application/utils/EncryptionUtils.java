package com.tse.core_application.utils;

import com.tse.core_application.configuration.DataEncryptionConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class EncryptionUtils {

    private static final Logger logger = LogManager.getLogger(EncryptionUtils.class.getName());

    public List<String> getEncryptValues(List<String> decryptedValues) {
        List<String> convertedValues = new ArrayList<>();
        DataEncryptionConverter converter = new DataEncryptionConverter();

        try {
            for (String value : decryptedValues) {
                convertedValues.add(converter.convertToDatabaseColumn(value));
            }
        } catch (Exception e){
            logger.error("Error encrypting the value: " , e);
        }

        return convertedValues;
    }

    public List<String> getDecryptedValues(List<String> encryptedValues) {
        List<String> convertedValues = new ArrayList<>();
        DataEncryptionConverter converter = new DataEncryptionConverter();

        for (String value : encryptedValues) {
            convertedValues.add((String) converter.convertToEntityAttribute(value));
        }

        return convertedValues;
    }
}
