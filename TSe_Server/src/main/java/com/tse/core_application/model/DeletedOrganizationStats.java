package com.tse.core_application.model;

import com.tse.core_application.configuration.DataEncryptionConverter;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Entity to store high-level statistics of organizations that have been hard deleted.
 * This data is retained for legal/financial audit purposes after org deletion.
 */
@Entity
@Table(name = "deleted_organization_stats", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeletedOrganizationStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    private Long id;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "org_name", length = 500)
    @Convert(converter = DataEncryptionConverter.class)
    private String orgName;

    @Column(name = "org_display_name", length = 500)
    @Convert(converter = DataEncryptionConverter.class)
    private String orgDisplayName;

    @Column(name = "owner_email", length = 500)
    @Convert(converter = DataEncryptionConverter.class)
    private String ownerEmail;

    // Counts
    @Column(name = "bu_count")
    private Integer buCount = 0;

    @Column(name = "project_count")
    private Integer projectCount = 0;

    @Column(name = "team_count")
    private Integer teamCount = 0;

    @Column(name = "total_user_count")
    private Integer totalUserCount = 0;

    @Column(name = "active_user_count")
    private Integer activeUserCount = 0;

    @Column(name = "inactive_user_count")
    private Integer inactiveUserCount = 0;

    @Column(name = "epic_count")
    private Integer epicCount = 0;

    @Column(name = "sprint_count")
    private Integer sprintCount = 0;

    @Column(name = "task_count")
    private Integer taskCount = 0;

    @Column(name = "note_count")
    private Integer noteCount = 0;

    @Column(name = "comment_count")
    private Integer commentCount = 0;

    @Column(name = "template_count")
    private Integer templateCount = 0;

    @Column(name = "meeting_count")
    private Integer meetingCount = 0;

    @Column(name = "sticky_note_count")
    private Integer stickyNoteCount = 0;

    @Column(name = "leave_count")
    private Integer leaveCount = 0;

    @Column(name = "feedback_count")
    private Integer feedbackCount = 0;

    @Column(name = "deleted_projects_count")
    private Integer deletedProjectsCount = 0;

    @Column(name = "deleted_teams_count")
    private Integer deletedTeamsCount = 0;

    @Column(name = "memory_used_gb", precision = 10, scale = 2)
    private BigDecimal memoryUsedGb = BigDecimal.ZERO;

    @Column(name = "memory_quota_gb", precision = 10, scale = 2)
    private BigDecimal memoryQuotaGb = BigDecimal.ZERO;

    // Audit fields
    @Column(name = "deletion_requested_at")
    private Timestamp deletionRequestedAt;

    @Column(name = "deletion_requested_by_account_id")
    private Long deletionRequestedByAccountId;

    @Column(name = "hard_deleted_at", nullable = false)
    private Timestamp hardDeletedAt;

    @CreationTimestamp
    @Column(name = "created_date_time", updatable = false, nullable = false)
    private Timestamp createdDateTime;
}
