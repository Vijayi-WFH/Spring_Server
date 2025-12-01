package com.tse.core_application.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tse.core_application.configuration.DataEncryptionConverter;
import com.tse.core_application.dto.ModelFetchedDto;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.List;

@Converter(autoApply = false)
public class ModelFetchedDtoListConverter implements AttributeConverter<List<ModelFetchedDto>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DataEncryptionConverter ENCRYPTOR = new DataEncryptionConverter();

    @Override
    public String convertToDatabaseColumn(List<ModelFetchedDto> attribute) {
        try {
            if (attribute == null) {
                return null;
            }
            // Step 1: List -> JSON
            String json = MAPPER.writeValueAsString(attribute);
            // Step 2: Encrypt JSON
            return ENCRYPTOR.convertToDatabaseColumn(json);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize and encrypt modelFetchedList", e);
        }
    }

    @Override
    public List<ModelFetchedDto> convertToEntityAttribute(String dbData) {
        try {
            if (dbData == null || dbData.isEmpty()) {
                return null;
            }
            // Step 1: Decrypt string to JSON
            String json = (String) ENCRYPTOR.convertToEntityAttribute(dbData);
            if (json == null || json.isEmpty()) {
                return null;
            }
            // Step 2: JSON -> List<ModelFetchedDto>
            return MAPPER.readValue(json, new TypeReference<List<ModelFetchedDto>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt and deserialize modelFetchedList", e);
        }
    }
}
