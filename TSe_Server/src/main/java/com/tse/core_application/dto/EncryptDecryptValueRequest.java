package com.tse.core_application.dto;

import lombok.Data;

import java.util.List;

@Data
public class EncryptDecryptValueRequest {
    private List<String> values;
}
