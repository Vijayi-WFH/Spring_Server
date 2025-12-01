package com.example.chat_app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UploadAttachmentResponse {

    private Long messageAttachmentId;
    private String fileFullName;
    private Double fileSize;

}
