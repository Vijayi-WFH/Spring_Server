package com.tse.core_application.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserPreferenceDTO {
        private Long userId;
        private Long teamId;
        private Long orgId;
        private Long projectId;
        private String preferredLanguage;
        private String timeZone;
        private String notificationSound;
        private String taskReminderPreference;
        private List<Integer> notificationCategoryIds;
        private LocalDateTime createdDateTime;
        private LocalDateTime lastUpdatedDateTime;
        private Integer userPreferredReminderNotification;

}
