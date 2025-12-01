package com.example.chat_app.custom.model;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class RestResponseWithData {

    private Integer status;
    private String message;
    private String timestamp;
    private Object data;
}
