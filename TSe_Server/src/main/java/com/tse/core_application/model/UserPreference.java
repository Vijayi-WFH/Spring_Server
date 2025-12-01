package com.tse.core_application.model;

import com.tse.core_application.utils.IntegerListConverter;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(name = "user_preference", schema = Constants.SCHEMA_NAME)
public class UserPreference {

    @Id
    @Column(name = "userId", nullable = false)
    private Long userId;

    @Column(name = "team_id")
    private Long teamId; // preferred

    @Column(name = "org_id")
    private Long orgId; // preferred

    @Column(name = "project_id")
    private Long projectId; // preferred

    @Column(name = "preferred_language", length = 50)
    private String preferredLanguage;

    @Column(name = "time_zone",  length = 50)
    private String timeZone;

    @Column(name = "notification_sound", length = 50)
    private String notificationSound;

    @Column(name = "task_reminder_preference", length = 50)
    private String taskReminderPreference;

    @Column(name = "notification_category_ids")
    @Convert(converter = IntegerListConverter.class)
    private List<Integer> notificationCategoryIds = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_date_time", updatable = false)
    private LocalDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = "last_updated_date_time", insertable = false)
    private LocalDateTime lastUpdatedDateTime;

    private Integer userPreferredReminderNotification;

    public UserPreference(Long userId, Long orgId, Long projectId, Long teamId, List<Integer> notificationCategoryIds) {
        this.setUserId(userId);
        this.setOrgId(orgId);
        this.setProjectId(projectId);
        this.setTeamId(teamId);
        this.setNotificationCategoryIds(notificationCategoryIds);
    }
}
