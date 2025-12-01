package com.tse.core_application.dto.conversations;

import com.tse.core_application.dto.ParentGcInfo;
import com.tse.core_application.model.Attachment;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

@Data
public class GroupConversationResponse {

    private Long groupConversationId;

    private String message;

    private String[] messageTags;

    private Long postedByAccountId;

    private Timestamp createdDateTime;

    private Timestamp lastUpdatedDateTime;

    private Integer entityTypeId;

    private Long entityId;

    private ParentGcInfo parentGcInfo;

    private List<Attachment> attachments;
}
