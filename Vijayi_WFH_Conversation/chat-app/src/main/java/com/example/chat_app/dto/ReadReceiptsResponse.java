package com.example.chat_app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReadReceiptsResponse {

    private Long messageId;
    private String tickStatus;
    private Integer deliveredCount;
    private Integer readCount;
    private Integer groupSize;
    private String action;
}
