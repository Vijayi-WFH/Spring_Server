package com.example.chat_app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class FileMetadata {

    private Long attachmentId;
    private String fileName;
    private Double fileSize;

    public FileMetadata(Object fileName, Double fileSize){
        this.fileName = (String) fileName;
        this.fileSize = fileSize;
    }

    public FileMetadata(Long attachmentId, Object fileName, Double fileSize){
        this.attachmentId = attachmentId;
        this.fileName = (String) fileName;
        this.fileSize = fileSize;
    }

}
