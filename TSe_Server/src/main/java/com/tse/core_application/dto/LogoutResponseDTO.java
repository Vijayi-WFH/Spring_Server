package com.tse.core_application.dto;

import lombok.Data;

@Data
public class LogoutResponseDTO {

    private String message;
    private String status;
    private Object response;

    public LogoutResponseDTO(String message, String status, Object response) {
        this.message = message;
        this.status = status;
        this.response = response;
    }
}
