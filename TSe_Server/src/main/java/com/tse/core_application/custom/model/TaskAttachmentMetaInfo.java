package com.tse.core_application.custom.model;

import lombok.Data;

@Data
public class TaskAttachmentMetaInfo {
    private Long taskAttachmentId;
    private String fileName;
    private String fileType;
    private Double fileSize;
    private Long commentLogId;

    public TaskAttachmentMetaInfo(Long taskAttachmentId, Object fileName, String fileType, Double fileSize, Long commentLogId) {
        this.taskAttachmentId = taskAttachmentId;
        this.fileName = (String) fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.commentLogId = commentLogId;
    }
}



