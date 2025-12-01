package com.tse.core_application.dto;

import com.tse.core_application.custom.model.AttachmentMetadata;
import com.tse.core_application.model.Attachment;
import lombok.Data;
import java.sql.Timestamp;
import java.util.List;

@Data
public class ParentGcInfo {
    private Long groupConversationId;
    private String message;
    private String[] messageTags;
    private Long postedByAccountId;
    private String lastMessageFromEmail;
    private String lastMessageFromFullName;
    private List<AttachmentMetadata> attachmentsMetadata;
    private Timestamp createdDateTime;
}
