package com.tse.core_application.custom.model;

import com.tse.core_application.dto.ParentMsgInfo;
import com.tse.core_application.dto.ReplyMsgInfo;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class EntityMessageResponse {

    private Long groupConversationId;
    private Long entityId;
    private Integer entityTypeId;
    private String message;
    private String[] messageTags;
    private List<ReplyMsgInfo> replyMsgInfoList;
    private ParentMsgInfo parentMsgInfo;
    private LocalDateTime createdDateTime;
    private String lastMessageFromEmail;
    private String lastMessageFromFullName;
    private List<AttachmentMetadata> attachments;
}
