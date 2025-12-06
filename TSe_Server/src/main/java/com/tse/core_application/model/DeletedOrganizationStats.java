package com.tse.core_application.model;

import com.tse.core_application.configuration.DataEncryptionConverter;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "deleted_organization_stats", schema = Constants.SCHEMA_NAME)
public class DeletedOrganizationStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    private Long id;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "organization_name", length = 500)
    @Convert(converter = DataEncryptionConverter.class)
    private String organizationName;

    @Column(name = "owner_email")
    @Convert(converter = DataEncryptionConverter.class)
    private String ownerEmail;

    @Column(name = "bu_count")
    private Integer buCount;

    @Column(name = "project_count")
    private Integer projectCount;

    @Column(name = "team_count")
    private Integer teamCount;

    @Column(name = "total_user_count")
    private Integer totalUserCount;

    @Column(name = "active_user_count")
    private Integer activeUserCount;

    @Column(name = "inactive_user_count")
    private Integer inactiveUserCount;

    @Column(name = "epic_count")
    private Integer epicCount;

    @Column(name = "sprint_count")
    private Integer sprintCount;

    @Column(name = "task_count")
    private Integer taskCount;

    @Column(name = "note_count")
    private Integer noteCount;

    @Column(name = "comment_count")
    private Integer commentCount;

    @Column(name = "template_count")
    private Integer templateCount;

    @Column(name = "meeting_count")
    private Integer meetingCount;

    @Column(name = "sticky_notes_count")
    private Integer stickyNotesCount;

    @Column(name = "leaves_count")
    private Integer leavesCount;

    @Column(name = "feedback_count")
    private Integer feedbackCount;

    @Column(name = "deleted_projects_count")
    private Integer deletedProjectsCount;

    @Column(name = "deleted_teams_count")
    private Integer deletedTeamsCount;

    @Column(name = "used_memory_bytes")
    private Long usedMemoryBytes;

    @Column(name = "max_memory_quota_bytes")
    private Long maxMemoryQuotaBytes;

    @Column(name = "deletion_reason")
    @Convert(converter = DataEncryptionConverter.class)
    private String deletionReason;

    @Column(name = "deletion_requested_at")
    private Timestamp deletionRequestedAt;

    @Column(name = "deletion_requested_by_account_id")
    private Long deletionRequestedByAccountId;

    @CreationTimestamp
    @Column(name = "deleted_at", updatable = false, nullable = false)
    private Timestamp deletedAt;

    @Column(name = "org_created_at")
    private Timestamp orgCreatedAt;

    @Column(name = "paid_subscription")
    private Boolean paidSubscription;

    @Column(name = "on_trial")
    private Boolean onTrial;
}
