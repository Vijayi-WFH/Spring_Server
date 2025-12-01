package com.tse.core_application.custom.model.openfire;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OpenFireResponse {

    private String status;
    private int statusCode;
    private String message;
    private Object data;
}

