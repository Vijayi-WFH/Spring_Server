package com.tse.core_application.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tse.core_application.configuration.DataEncryptionConverter;
import com.tse.core_application.constants.ErrorConstant;
import com.tse.core_application.utils.LongListConverter;
import com.tse.core_application.utils.StringListConverter;
import lombok.*;
import org.hibernate.annotations.*;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "task_history", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TaskHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_history_id")
    private Long taskHistoryId;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "version")
    private Long version;

    @Column(name = "recorded_effort")
    private Integer recordedEffort;

    @Column(name = "sprint_id")
    private Long sprintId;

    @Column(name = "attachments", length = 2500)
    private String attachments;

    @Column(name = "task_title", nullable = false)
    @Size(min = 3, max = 70, message = ErrorConstant.Task.TITLE_LIMIT)
//	@Convert(converter = DataEncryptionConverter.class)
    private String taskTitle;

    @Column(name = "task_number", nullable = false, length = 40)
    private String taskNumber;

    @Column(name = "task_identifier",nullable = false)
    private Long taskIdentifier;

    @Column(name = "bu_id", nullable = false)
    private Long buId;

    @Column(name = "task_desc", nullable = false, length = 20000)
    @Size(min = 3, max = 5000, message = ErrorConstant.Task.TASK_DESC)
    @Convert(converter = DataEncryptionConverter.class)
    private String taskDesc;

    @Column(name = "task_exp_start_date")
    private LocalDateTime taskExpStartDate;

    @Column(name = "task_act_st_date")
    private LocalDateTime taskActStDate;

    @Column(name = "task_act_end_date")
    private LocalDateTime taskActEndDate;

    @Column(name = "task_act_st_time")
    private LocalTime taskActStTime;

    @Column(name = "task_act_end_time")
    private LocalTime taskActEndTime;

    @Column(name = "task_exp_start_time")
    private LocalTime taskExpStartTime;

    @Column(name = "task_exp_end_date")
    private LocalDateTime taskExpEndDate;

    @Column(name = "task_exp_end_time")
    private LocalTime taskExpEndTime;

    @Column(name = "comment_id")
    private Long commentId;

    @Column(name = "system_generated_expected_end_time")
    private LocalTime systemGeneratedExpectedEndTime;

    @Column(name = "task_completion_date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDateTime taskCompletionDate;

    @Column(name = "task_completion_time")
    private LocalTime taskCompletionTime;

    @Column(name = "task_workflow_id", nullable = false)
    private Integer taskWorkflowId;

    @Nullable                              // field added here for task 2634
    @Column(name = "task_type_id")
    private Integer taskTypeId;

    @Nullable
    @Column(name = "parent_task_type_id")
    private Integer parentTaskTypeId;

    @Nullable
    @Convert(converter = LongListConverter.class)
    @Column(name = "bug_task_relation")
    private List<Long> bugTaskRelation;

    @Nullable
    @Convert(converter = LongListConverter.class)
    @Column(name = "meeting_list")
    private List<Long> meetingList;

    @Nullable
    @Convert(converter = LongListConverter.class)
    @Column(name = "reference_work_item_id")   // field added here for task 2634
    private List<Long> referenceWorkItemId;

    @NotNull
    @Column(name = "current_activity_indicator", nullable = false)
    private Integer currentActivityIndicator;

    @Nullable
    @Column(name = "task_estimate")
    private Integer taskEstimate;
    @Column(name = "user_perceived_remaining_time_for_completion")
    private Integer userPerceivedRemainingTimeForCompletion;  // added in task 3615

    @Nullable
    @Column(name = "parking_lot", length = 5000)
    @Size(max=1000, message= ErrorConstant.Task.PARKING_LOT)
    @Convert(converter = DataEncryptionConverter.class)
    private String parkingLot;

    @Nullable
    @Column(name = "key_decisions", length = 5000)
    @Size(max=1000, message=ErrorConstant.Task.KEY_DECISIONS)
    @Convert(converter = DataEncryptionConverter.class)
    private String keyDecisions;

    @Nullable
    @Column(name = "acceptance_criteria", length = 5000)
    @Size(max=1000, message=ErrorConstant.Task.ACCEPTANCE_CRITERIA)
    @Convert(converter = DataEncryptionConverter.class)
    private String acceptanceCriteria;

    @Nullable
    @Column(name = "parent_task_id")
    private Long parentTaskId;

    @Nullable
    @Convert(converter = LongListConverter.class)
    private List<Long> childTaskIds;

    @Nullable
    @Column(name = "task_progress_system")
    @Enumerated(EnumType.STRING)
    private com.tse.core_application.model.StatType taskProgressSystem;

    @Nullable
    @Column(name = "task_progress_system_last_updated")
    private LocalDateTime taskProgressSystemLastUpdated;

    @Nullable
    @Column(name = "next_task_progress_system_change_date_time")
    private LocalDateTime nextTaskProgressSystemChangeDateTime;

    @Nullable
    @Column(name = "task_progress_set_by_user")
    @Enumerated(EnumType.STRING)
    private com.tse.core_application.model.StatType taskProgressSetByUser;

    @Nullable
    @Column(name = "task_progress_set_by_account_id")
    private Long taskProgressSetByAccountId;

    @Nullable
    @Column(name = "task_progress_set_by_account_id_last_updated")
    private Timestamp taskProgressSetByAccountIdLastUpdated;

    @Column(name = "task_progress_set_by_user_last_updated")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime taskProgressSetByUserLastUpdated;

    @Column(name = "task_state", length = 100)
    private String taskState;

    @Nullable
    @Column(name = "task_priority")
    private String taskPriority;

    @CreationTimestamp
    @Column(name = "created_date_time", updatable = false, nullable = false)
    private LocalDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = "last_updated_date_time", insertable = false)
    private LocalDateTime lastUpdatedDateTime;

    @Nullable
    @Column(name = "task_dependency")
    @Enumerated(EnumType.STRING)
    private com.tse.core_application.model.TaskDependency taskDependency;

    @Nullable
    @Column(name = "system_derived_end_ts")
    private LocalDateTime systemDerivedEndTs;

    @Column(name = "immediate_attention", length = 50)
    private Integer immediateAttention;

    @Column(name = "immediate_attention_from", length = 1000)
    @Convert(converter = DataEncryptionConverter.class)
    private String immediateAttentionFrom;

    @Column(name = "immediate_attention_reason")
    @Size(min=2, max=250, message=ErrorConstant.Task.REASON_CRITERIA)
    @Convert(converter = DataEncryptionConverter.class)
    private String immediateAttentionReason;

    @Column(name = "user_perceived_percentage_task_completed")
    private Integer userPerceivedPercentageTaskCompleted;

    @Column(name = "user_perceived_percentage_task_earned_value")
    private Integer userPerceivedPercentageTaskEarnedValue;

    @Column(name = "earned_time_task", nullable = true)
    private Integer earnedTimeTask;

    @Column(name = "account_id_prev_assigned_1")
    private Long accountIdPrevAssigned1;

    @Column(name = "account_id_prev_assigned_2")
    private Long accountIdPrevAssigned2;

    @Column(name = "account_id_prev_assignee_1")
    private Long accountIdPrevAssignee1;

    @Column(name = "account_id_prev_assignee_2")
    private Long accountIdPrevAssignee2;

    @Column(name = "estimate_time_log_evaluation", length = 20)
    private String estimateTimeLogEvaluation;

    @Column(name = "task_completion_impact", length = 20)
    private String taskCompletionImpact;

    @Column(name = "is_ballpark_estimate")
    private Integer isBallparkEstimate;

    @Column(name = "is_estimate_system_generated")
    private Integer isEstimateSystemGenerated;

    @Column(name="currently_scheduled_task_indicator")
    private Boolean currentlyScheduledTaskIndicator = false;

    @Column(name="unplanned_scheduled_task_indicator")
    private Boolean unplannedScheduledTaskIndicator = false;

    @ManyToOne(optional = false)
    @JoinColumn(name = "team_id", referencedColumnName = "team_id")
    private Team fkTeamId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id", referencedColumnName = "account_id")
    private UserAccount fkAccountId;

    @ManyToOne
    @JoinColumn(name = "project_id", referencedColumnName = "project_id")
    private Project fkProjectId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id_creator", referencedColumnName = "account_id")
    private UserAccount fkAccountIdCreator;

    @ManyToOne
    @JoinColumn(name = "account_id_assignee", referencedColumnName = "account_id")
    private UserAccount fkAccountIdAssignee;

    @ManyToOne
    @JoinColumn(name = "account_id_assigned", referencedColumnName = "account_id")
    private UserAccount fkAccountIdAssigned;

    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id_last_updated", referencedColumnName = "account_id")
    private UserAccount fkAccountIdLastUpdated;

    @ManyToOne
    @JoinColumn(name = "account_id_mentor_1", referencedColumnName = "account_id")
    private UserAccount fkAccountIdMentor1;

    @ManyToOne
    @JoinColumn(name = "account_id_mentor_2", referencedColumnName = "account_id")
    private UserAccount fkAccountIdMentor2;

    @ManyToOne
    @JoinColumn(name = "account_id_observer_1", referencedColumnName = "account_id")
    private UserAccount fkAccountIdObserver1;

    @ManyToOne
    @JoinColumn(name = "account_id_observer_2", referencedColumnName = "account_id")
    private UserAccount fkAccountIdObserver2;

    @ManyToOne(optional = false)
    @JoinColumn(name = "org_id", referencedColumnName = "org_id")
    private Organization fkOrgId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "workflow_task_status_id", referencedColumnName = "workflow_task_status_id")
    private WorkFlowTaskStatus fkWorkflowTaskStatus;

    @Column(name = "task_history_created_by")
    private Integer taskHistoryCreatedBy;

    @Nullable
    @Column(name = "note_id")
    private Long noteId;

    @Nullable
    @Column(name = "list_of_deliverables_delivered_id")
    private Long listOfDeliverablesDeliveredId;

    @Column(name = "deliverables")
    private String deliverables;

    @Nullable
    @Column(name = "dependency_ids")
    @Convert(converter = LongListConverter.class)
    private List<Long> dependencyIds;

    @Column(name = "blocked_reason_type_id")
    private Integer blockedReasonTypeId;

    @Column(name = "blocked_reason", length = 5000)
    @Size(min = 3, max = 1000, message = ErrorConstant.Task.EXPLANATION_LIMIT)
    @Convert(converter = DataEncryptionConverter.class)
    private String blockedReason;

    @ManyToOne(optional = true)
    @JoinColumn(name = "account_id_respondent", referencedColumnName = "account_id")
    private UserAccount fkAccountIdRespondent;

    @Column(name = "reminder_interval")
    private Integer reminderInterval;

    @Column(name = "next_reminder_date_time")
    private LocalDateTime nextReminderDateTime;

    @Column(name = "recorded_task_effort")
    private Integer recordedTaskEffort;

    @Column(name = "total_effort")
    private Integer totalEffort;

    @Column(name = "total_meeting_effort")
    private Integer totalMeetingEffort;

    @Column(name = "billed_meeting_effort")
    private Integer billedMeetingEffort;

    @Column(name = "meeting_effort_preference_id")
    private Integer meetingEffortPreferenceId;

    @Column(name = "is_Sprint_Changed")
    private Boolean isSprintChanged;

    @ManyToOne
    @JoinColumn(name = "account_id_bug_reported_by", referencedColumnName = "account_id")
    private UserAccount fkAccountIdBugReportedBy;

    @Nullable
    @Column(name = "environment_id")
    private Integer environmentId;

    @Nullable
    @Column(name = "severity_id")
    private Integer severityId;

    @Nullable
    @Column(name = "resolution_id")
    private Integer resolutionId;

    @Nullable
    @Column(name = "place_of_identification")
    @Enumerated(EnumType.STRING)
    private com.tse.core_application.model.PlaceOfIdentification placeOfIdentification;

    @Nullable
    @Column(name = "steps_taken_to_complete", length=5000)
    @Size(max=1000, message= ErrorConstant.Task.STEPS_TAKEN_TO_COMPLETE )
    @Convert(converter = DataEncryptionConverter.class)
    private String stepsTakenToComplete;

    @ManyToOne(optional = true)
    @JoinColumn(name = "epic_id", referencedColumnName = "epic_id")
    private Epic fkEpicId;

    @Convert(converter = StringListConverter.class)
    @Column(name = "task_labels")
    private List<String> taskLabels;

    @Column(name = "child_internal_dependencies_count")
    private Integer countChildInternalDependencies;

    @Column(name = "child_external_dependencies_count")
    private Integer countChildExternalDependencies;

    @Column(name = "release_version_name")
    @Pattern(
            regexp = "^(?:[a-zA-Z])?(0|[1-9]\\d*)(?:\\.(0|[1-9]\\d*))?(?:\\.(0|[1-9]\\d*))?(?:-(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*)?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$",
            message = ErrorConstant.ReleaseVersion.INVALID_VERSION_PATTERN
    )
    private String releaseVersionName;

    @Column(name = "is_starred")
    private Boolean isStarred;

    @ManyToOne
    @JoinColumn(name = "account_id_starred_by", referencedColumnName = "account_id")
    private UserAccount fkAccountIdStarredBy;
}
