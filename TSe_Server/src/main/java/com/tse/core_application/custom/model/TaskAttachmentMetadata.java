package com.tse.core_application.custom.model;

import lombok.Data;
import lombok.Value;

@Value
public class TaskAttachmentMetadata {

    private Long taskAttachmentId;
    private String fileName;
    private String fileType;
    private Double fileSize;
    private Long uploaderAccountId;
    private Character fileStatus;
    private Long commentLogId;

    public TaskAttachmentMetadata(Long taskAttachmentId, Object fileName, String fileType, Double fileSize, Long uploaderAccountId, Character fileStatus, Long commentLogId) {
        this.taskAttachmentId = taskAttachmentId;
        this.fileName = (String) fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.uploaderAccountId = uploaderAccountId;
        this.fileStatus = fileStatus;
        this.commentLogId = commentLogId;
    }
}

