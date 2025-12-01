package com.tse.core_application.dto;

import lombok.Data;

@Data
public class TestResponseDTO {

    private Long id;
    private String requestedPayload;
    private String errorLocalMessage;
    private String errorMsg;
    private String stackTrace;
    private boolean actualStatus;

    public TestResponseDTO(Long id, String requestedPayload, String errorLocalMessage, String errorMsg, String stackTrace, boolean actualStatus) {
        this.id = id;
        this.requestedPayload = requestedPayload;
        this.errorLocalMessage = errorLocalMessage;
        this.errorMsg = errorMsg;
        this.stackTrace = stackTrace;
        this.actualStatus = actualStatus;
    }
}
