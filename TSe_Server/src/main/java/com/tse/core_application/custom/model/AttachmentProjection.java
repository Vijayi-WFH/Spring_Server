package com.tse.core_application.custom.model;

import lombok.Value;

public interface AttachmentProjection {
    Long getAttachmentId();
    String getFileName();
    String getFileType();
    Long getFileSize();
    Long getGroupConversationId();
}
