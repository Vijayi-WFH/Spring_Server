package com.tse.core_application.custom.model;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class RestRespWithData<T> implements Serializable {

    private Integer status;
    private String message;
    private String timestamp;
    private T data;
}
