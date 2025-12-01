package com.tse.core_application.custom.model;

import lombok.Value;

@Value
public class FileMetadata {

    String fileName;
    Double fileSize;
    Long commentLogId;

    public FileMetadata(Object fileName, Double fileSize, Long commentLogId){
        this.fileName = (String) fileName;
        this.fileSize = fileSize;
        this.commentLogId = commentLogId;
    }

}
