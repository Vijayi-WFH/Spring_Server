package com.tse.core.model.supplements;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.tse.core.configuration.DataEncryptionConverter;
import com.tse.core.constants.ErrorConstant;
import com.tse.core.custom.model.NewEffortTrack;
import com.tse.core.model.Constants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "task", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(value = { "labels" }, allowGetters = true)
public class Task implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "recorded_effort")
    private Integer recordedEffort;

    @Transient
    private List<NewEffortTrack> newEffortTracks;

    @Column(name = "sprint_id")
    private Long sprintId;

    @Column(name = "attachments", length = 2500)
    private String attachments;

    @NotBlank(message = ErrorConstant.Task.TASK_TITLE)
    @Column(name = "task_title", nullable = false)
    @Size(min = 3, max = 70, message = ErrorConstant.Task.TITLE_LIMIT)
//    @Convert(converter = DataEncryptionConverter.class)
    private String taskTitle;

    @Column(name = "task_number", nullable = false, length = 40)
    private String taskNumber;

//    @Column(name = "initials", nullable = false, length = 10) // task number initials
//    private String initials;

    @Column(name = "task_identifier", nullable = false)
    private Long taskIdentifier;

    @Column(name = "bu_id", nullable = false)
    private Long buId;

    @NotBlank(message = ErrorConstant.Task.TASK_DESC)
    @Column(name = "task_desc", nullable = false, length = 20000)
    @Size(min = 3, max = 5000, message = ErrorConstant.Task.DESC_LIMIT)
    @Convert(converter = DataEncryptionConverter.class)
    private String taskDesc;

    @Column(name = "task_exp_start_date")
    private LocalDateTime taskExpStartDate;

    @Column(name = "task_act_st_date")
    private LocalDateTime taskActStDate;

    @Column(name = "task_act_end_date")
    private LocalDateTime taskActEndDate;

    @Column(name = "task_act_st_time")
    private java.time.LocalTime taskActStTime;

    @Column(name = "task_act_end_time")
    private java.time.LocalTime taskActEndTime;

    @Column(name = "task_exp_start_time")
    private java.time.LocalTime taskExpStartTime;

    @Column(name = "task_exp_end_date")
    private LocalDateTime taskExpEndDate;

    @Column(name = "task_exp_end_time")
    private java.time.LocalTime taskExpEndTime;

    @Column(name = "comment_id")
    private Long commentId;

    @Column(name = "system_generated_expected_end_time")
    private java.time.LocalTime systemGeneratedExpectedEndTime;

    @Column(name = "task_completion_date")
    private LocalDateTime taskCompletionDate;

    @Column(name = "task_completion_time")
    private java.time.LocalTime taskCompletionTime;

    @NotNull(message = ErrorConstant.Task.TASK_WORKFLOW_ID)
    @Column(name = "task_workflow_id", nullable = false)
    private Integer taskWorkflowId;

    @NotNull(message = ErrorConstant.Task.CURRENT_ACTIVITY_INDICATOR)
    @Column(name = "current_activity_indicator", nullable = false)
    private Integer currentActivityIndicator;

    @NotNull(message = ErrorConstant.Task.TASK_TYPE_ID)
    @Column(name = "task_type_id")
    private Integer taskTypeId;

    @Nullable
    @Column(name = "parent_task_type_id")
    private Integer parentTaskTypeId;

    @Nullable
    @Column(name = "parent_task_id")
    private Long parentTaskId;

    @Nullable
    @Column(name = "task_estimate")
    private Integer taskEstimate;
    @Column(name = "user_perceived_remaining_time_for_completion")
    private Integer userPerceivedRemainingTimeForCompletion;      // added new field in task 3615

    @Nullable
    @Column(name = "task_progress_system_last_updated")
    private LocalDateTime taskProgressSystemLastUpdated;

    @Nullable
    @Column(name = "next_task_progress_system_change_date_time")
    private LocalDateTime nextTaskProgressSystemChangeDateTime;

    @Nullable
    @Column(name = "task_progress_set_by_account_id")
    private Long taskProgressSetByAccountId;

    @Nullable
    @Column(name = "task_progress_set_by_account_id_last_updated")
    private Timestamp taskProgressSetByAccountIdLastUpdated;

    @Column(name = "task_progress_set_by_user_last_updated")
    private java.time.LocalDateTime taskProgressSetByUserLastUpdated;

    @Column(name = "task_state", length = 100)
    private String taskState;

    @Column(name = "currently_scheduled_task_indicator")
    private Boolean currentlyScheduledTaskIndicator = false;

    @Column(name = "unplanned_scheduled_task_indicator")
    private Boolean unplannedScheduledTaskIndicator = false;

    @Nullable
    @Column(name = "task_priority")
    private String taskPriority;

    @Version
    @Column(name = "version")
    private Long version;

    @CreationTimestamp
    @Column(name = "created_date_time", updatable = false, nullable = false)
    private LocalDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = "last_updated_date_time", insertable = false)
    private LocalDateTime lastUpdatedDateTime;

    @Nullable
    @Column(name = "system_derived_end_ts")
    private LocalDateTime systemDerivedEndTs;

    @Column(name = "immediate_attention", length = 50)
    private Integer immediateAttention = 0;

    @Column(name = "immediate_attention_from", length = 255)
    @Size(max = 70)
    @Convert(converter = DataEncryptionConverter.class)
    private String immediateAttentionFrom;

    @Column(name = "immediate_attention_reason", length=1000)
    @Size(min=2, max=250, message=ErrorConstant.Task.REASON_CRITERIA)
    @Convert(converter = DataEncryptionConverter.class)
    private String immediateAttentionReason;

    @Column(name = "user_perceived_percentage_task_completed")
    private Integer userPerceivedPercentageTaskCompleted;

    @Column(name = "user_perceived_percentage_task_earned_value")
    private Integer userPerceivedPercentageTaskEarnedValue;

    @Column(name = "earned_time_task")
    private Integer earnedTimeTask;

    @Transient
    private Integer increaseInUserPerceivedPercentageTaskCompleted;

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

    @Nullable
    @Column(name = "environment_id")
    private Integer environmentId;

    @Nullable
    @Column(name = "resolution_id")
    private Integer resolutionId;

    @Nullable
    @Column(name = "severity_id")
    private Integer severityId;

    @Nullable
    @Column(name = "customer_impact")
    private Boolean customerImpact;

    @NotNull(message = ErrorConstant.Task.fk_TEAM_ID)
    @ManyToOne(optional = false)
    @JoinColumn(name = "team_id", referencedColumnName = "team_id")
    private Team fkTeamId;

    @NotNull(message = ErrorConstant.Task.ACCOUNT_ID)
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

    @NotNull(message = ErrorConstant.Task.fk_ORG_ID)
    @ManyToOne(optional = false)
    @JoinColumn(name = "org_id", referencedColumnName = "org_id")
    private Organization fkOrgId;

    @NotNull(message = ErrorConstant.Task.fk_WORK_FLOW_TASK_STATUS_ID)
    @ManyToOne(optional = false)
    @JoinColumn(name = "workflow_task_status_id", referencedColumnName = "workflow_task_status_id")
    private WorkFlowTaskStatus fkWorkflowTaskStatus;

    @Column(name = "note_id")
    private Long noteId;
    @Column(name = "list_of_deliverables_delivered_id")
    private Long listOfDeliverablesDeliveredId;

    @Column(name = "deliverables")
    private String deliverables;

    @Column(name = "blocked_reason_type_id")
    private Integer blockedReasonTypeId;

    @ManyToOne(optional = true)
    @JoinColumn(name = "account_id_respondent", referencedColumnName = "account_id")
    private UserAccount fkAccountIdRespondent;

    @Column(name = "reminder_interval")
    private Integer reminderInterval;

    @Column(name = "next_reminder_date_time")
    private LocalDateTime nextReminderDateTime;

    @Transient
    private List<String> labelsToAdd;

    @Transient
    private List<String> taskLabels;

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
    private Boolean isSprintChanged = false;

    @ManyToOne
    @JoinColumn(name = "account_id_bug_reported_by", referencedColumnName = "account_id")
    private UserAccount fkAccountIdBugReportedBy;

    @Column(name = "is_bug")
    private Boolean isBug = false;

    @Column(name = "child_internal_dependencies_count")
    private Integer countChildInternalDependencies = 0;

    @Column(name = "child_external_dependencies_count")
    private Integer countChildExternalDependencies = 0;

    @Column(name = "status_at_time_of_deletion")
    private String statusAtTimeOfDeletion;

    public void setTaskDesc(String taskDesc) {
        if (taskDesc != null) {
            this.taskDesc = taskDesc.trim();
        } else {
            this.taskDesc = null;
        }
    }

    public void setImmediateAttentionReason(String immediateAttentionReason) {
        this.immediateAttentionReason = immediateAttentionReason != null ? immediateAttentionReason.trim() : null;
    }

}
