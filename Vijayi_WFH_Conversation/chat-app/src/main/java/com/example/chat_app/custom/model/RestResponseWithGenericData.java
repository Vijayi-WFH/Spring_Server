package com.example.chat_app.custom.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RestResponseWithGenericData<T> {
    private Integer status;
    private String message;
    private String timestamp;
    private T data;
}
