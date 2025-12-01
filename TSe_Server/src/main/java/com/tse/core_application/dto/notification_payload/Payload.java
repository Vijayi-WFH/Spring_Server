package com.tse.core_application.dto.notification_payload;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Payload {
    private String accountId;

    private String notificationId;

    private String notificationType;

    private String categoryId;

    private String createdDateTime;

    private String title;

    private String body;

    //for task
    private String taskNumber;

    private String taskId;

    private String scrollTo;

    private String teamId;

    //for meeting
    private String meetingId;

    private String meetingMode;

    private String meetingVenue;

    private String meetingDate;

    private String gcEntityId; // for group conversation

    private String gcEntityTypeId; // for group conversation

    private String groupConversationId;

    private String orgId;

    private String punchRequestId;

}
