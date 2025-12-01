package com.tse.core_application.dto;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class ReplyMsgInfo {
    private Long groupConversationId;
    private String message;
    private String[] messageTags;
    private Long postedByAccountId;
    private String lastMessageFromEmail;
    private String lastMessageFromFullName;
    private String attachmentName;
    private Timestamp createdDateTime;
}

