package com.example.chat_app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DeleteMessageAttachmentRequest {

    private Long messageId;
    private Long removerAccountId;
    private String optionIndicator;
    private Long messageAttachmentId;
}
