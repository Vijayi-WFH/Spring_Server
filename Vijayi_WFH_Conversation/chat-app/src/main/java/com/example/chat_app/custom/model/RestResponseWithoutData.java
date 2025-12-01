package com.example.chat_app.custom.model;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class RestResponseWithoutData {
    private Integer status;
    private String message;
    private String timestamp;
}
