package com.example.chat_app.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@JsonSerialize
@Getter
@Setter
public class SocketFormatter {
    private String messageType;
    private Object data;
}
