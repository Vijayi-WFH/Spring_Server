package com.tse.core_application.custom.model;

import lombok.Data;

@Data
public class AttachmentMetadata {
    Long attachmentId;
    String fileName;
    String fileType;
    Long fileSize;
}
