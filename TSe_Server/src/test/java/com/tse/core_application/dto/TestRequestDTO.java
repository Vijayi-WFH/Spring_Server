package com.tse.core_application.dto;

import lombok.Data;

@Data
public class TestRequestDTO {
    private Long id;
    private String request;
    private String methodName;
    private boolean actualStatus;

}
