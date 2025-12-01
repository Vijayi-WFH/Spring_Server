package com.tse.core_application.model;

import com.fasterxml.jackson.annotation.*;

import com.tse.core_application.configuration.DataEncryptionConverter;
import com.tse.core_application.constants.ErrorConstant;
import com.tse.core_application.custom.model.DependentTaskDetail;
import com.tse.core_application.custom.model.DependentTaskDetailResponse;
import com.tse.core_application.custom.model.NewEffortTrack;
import com.tse.core_application.custom.model.childbugtask.LinkedTask;
import com.tse.core_application.dto.AccountDetailsForBulkResponse;
import com.tse.core_application.utils.LongListConverter;
import com.tse.core_application.custom.model.childbugtask.ChildTask;
import com.tse.core_application.custom.model.childbugtask.ParentTaskResponse;
import lombok.*;

import org.hibernate.annotations.*;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.Valid;
import javax.validation.constraints.*;
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
    @Valid
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
    @Transient
    private List<ChildTask> childTaskList;

    @Transient
    @Nullable
    private ParentTaskResponse parentTaskResponse;

    @Nullable
    @Convert(converter = LongListConverter.class)
    @Column(name = "child_task_ids")
    private List<Long> childTaskIds;

    @Nullable
    @Convert(converter = LongListConverter.class)
    @Column(name = "deleted_child_task_ids")
    private List<Long> deletedChildTaskIds = new ArrayList<>();

    @Nullable
    @Convert(converter = LongListConverter.class)
    @Column(name = "bug_task_relation")
    private List<Long> bugTaskRelation;

    @Nullable
    @Convert(converter = LongListConverter.class)
    @Column(name = "meeting_list")
    private List<Long> meetingList = new ArrayList<>();

    @Nullable
    @Column(name = "parent_task_id")
    private Long parentTaskId;

    @Nullable
    @Transient
    private List<ReferenceWorkItem> referenceWorkItemList;

    @Nullable
    @Convert(converter = LongListConverter.class)
    @Column(name = "reference_work_item_id")
    private List<Long> referenceWorkItemId;

    @Nullable
    @Column(name = "task_estimate")
    private Integer taskEstimate;
    @Column(name = "user_perceived_remaining_time_for_completion")
    private Integer userPerceivedRemainingTimeForCompletion;      // added new field in task 3615

    @Nullable
    @Column(name = "parking_lot", length=5000)
    @Size(max=1000, message= ErrorConstant.Task.PARKING_LOT)
    @Convert(converter = DataEncryptionConverter.class)
    private String parkingLot;

    @Nullable
    @Column(name = "key_decisions", length=5000)
    @Size(max=1000, message=ErrorConstant.Task.KEY_DECISIONS)
    @Convert(converter = DataEncryptionConverter.class)
    private String keyDecisions;


    @Nullable
    @Column(name = "acceptance_criteria", length=5000)
    @Size(max=1000, message=ErrorConstant.Task.ACCEPTANCE_CRITERIA)
    @Convert(converter = DataEncryptionConverter.class)
    private String acceptanceCriteria;

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
    @Column(name = "task_dependency")
    @Enumerated(EnumType.STRING)
    private com.tse.core_application.model.TaskDependency taskDependency;

    @Nullable
    @Column(name = "system_derived_end_ts")
    private LocalDateTime systemDerivedEndTs;

    @Column(name = "immediate_attention", length = 50)
    private Integer immediateAttention = 0;

    @Column(name = "immediate_attention_from", length = 255)
    @Size(max = 70)
    @Convert(converter = DataEncryptionConverter.class)
    private String immediateAttentionFrom;

    @Column(name = "immediate_attention_reason", length = 1000)
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

    @Transient
    private Boolean isToCreateDuplicateTask;

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
    @Column(name = "steps_taken_to_complete", length=5000)
    @Size(max=1000, message=ErrorConstant.Task.STEPS_TAKEN_TO_COMPLETE )
    @Convert(converter = DataEncryptionConverter.class)
    private String stepsTakenToComplete;

    @Nullable
    @Column(name = "place_of_identification")
    @Enumerated(EnumType.STRING)
    private com.tse.core_application.model.PlaceOfIdentification placeOfIdentification;

    @Nullable
    @Column(name = "customer_impact")
    private Boolean customerImpact;

    @Nullable
    @Transient
    private List<LinkedTask> linkedTaskList;

    @NotNull(message = ErrorConstant.Task.fk_TEAM_ID)
    @ManyToOne(optional = false)
    @JoinColumn(name = "team_id", referencedColumnName = "team_id")
    private Team fkTeamId;

    @NotNull(message = ErrorConstant.Task.ACCOUNT_ID)
    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id", referencedColumnName = "account_id")
    private UserAccount fkAccountId;

    @ManyToOne(optional = true)
    @JoinColumn(name = "epic_id", referencedColumnName = "epic_id")
    private Epic fkEpicId;

    @Column(name = "added_in_epic_after_completion")
    private Boolean addedInEpicAfterCompletion;

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

    @OneToMany(mappedBy = "task", fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Comment> comments;

    @Column(name = "note_id")
    private Long noteId;
    @Column(name = "list_of_deliverables_delivered_id")
    private Long listOfDeliverablesDeliveredId;

    @OneToMany(mappedBy = "task", fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<Note> notes;

    @OneToMany(mappedBy = "task", fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<DeliverablesDelivered> listOfDeliverablesDelivered;

    @Column(name = "deliverables")
    private String deliverables;

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
    @Min(value = 1, message = ErrorConstant.Task.REMINDER_LIMIT)
    @Max(value = 30, message = ErrorConstant.Task.REMINDER_LIMIT)
    private Integer reminderInterval;

    @Column(name = "next_reminder_date_time")
    private LocalDateTime nextReminderDateTime;

    @Transient
    private List<DependentTaskDetailResponse> dependentTaskDetailResponseList;

    @Transient
    private List<DependentTaskDetail> dependentTaskDetailRequestList;

    @Nullable
    @Column(name = "dependency_ids")
    @Convert(converter = LongListConverter.class)
    private List<Long> dependencyIds;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "task_label", schema = Constants.SCHEMA_NAME, joinColumns = @JoinColumn(name = "task_id"), inverseJoinColumns = @JoinColumn(name = "label_id"))
    private List<Label> labels;

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

    @Column(name = "prev_sprints")
    @Convert(converter = LongListConverter.class)
    private List<Long> prevSprints = new ArrayList<>();

    @Column(name = "child_internal_dependencies_count")
    private Integer countChildInternalDependencies = 0;

    @Column(name = "child_external_dependencies_count")
    private Integer countChildExternalDependencies = 0;

    @Column(name = "status_at_time_of_deletion")
    private String statusAtTimeOfDeletion;

    @Column(name = "deletion_reason_id")
    private Integer deletionReasonId;

    @Column(name = "duplicate_work_item_number")
    private String duplicateWorkItemNumber;

    @Column(name = "deleted_reason")
    @Size(min=2, max=250, message=ErrorConstant.Task.DELETION_REASON)
    private String deletedReason;

    // Root cause analysis id (in case of bug)
    @Column(name = "rca_id")
    private Integer rcaId;

    @Column(name = "is_rca_done")
    private Boolean isRcaDone;

    @Column(name = "rca_reason")
    private String rcaReason;

    @Column(name = "rca_introduced_by")
    @Convert(converter = LongListConverter.class)
    private List<Long> rcaIntroducedBy;

    @Transient
    private List<AccountDetailsForBulkResponse> rcaMemberAccountIdList;

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

    @ManyToOne
    @JoinColumn(name = "account_id_blocked_by", referencedColumnName = "account_id")
    private UserAccount fkAccountIdBlockedBy;

    @Column(name = "meeting_notification_sent_time") // notify referenceMeeting attendees
    private LocalDateTime meetingNotificationSentTime;

    public void setAcceptanceCriteria(String acceptanceCriteria) {
        if (acceptanceCriteria != null) {
            this.acceptanceCriteria = acceptanceCriteria.trim();
        } else {
            this.acceptanceCriteria = null;
        }
    }

    public void setKeyDecisions(String keyDecisions) {
        if (keyDecisions != null) {
            this.keyDecisions = keyDecisions.trim();
        } else {
            this.keyDecisions = null;
        }
    }

    public void setParkingLot(String parkingLot) {
        if (parkingLot != null) {
            this.parkingLot = parkingLot.trim();
        } else {
            this.parkingLot = null;
        }
    }

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


//    @Override
//    public String toString() {
//        return "Task{" +
//                "taskId=" + taskId +
//                ", recordedEffort=" + recordedEffort +
//                // check if we need to include transient field newEffortTracks
//                ", sprintId=" + sprintId +
//                ", attachments='" + attachments + '\'' +
//                ", taskTitle='" + taskTitle + '\'' +
//                ", taskNumber=" + taskNumber +
//                ", buId=" + buId +
//                ", taskDesc='" + taskDesc + '\'' +
//                ", taskExpStartDate=" + taskExpStartDate +
//                ", taskActStDate=" + taskActStDate +
//                ", taskActEndDate=" + taskActEndDate +
//                ", taskActStTime=" + taskActStTime +
//                ", taskActEndTime=" + taskActEndTime +
//                ", taskExpStartTime=" + taskExpStartTime +
//                ", taskExpEndDate=" + taskExpEndDate +
//                ", taskExpEndTime=" + taskExpEndTime +
//                ", commentId=" + commentId +
//                ", systemGeneratedExpectedEndTime=" + systemGeneratedExpectedEndTime +
//                ", taskCompletionDate=" + taskCompletionDate +
//                ", taskCompletionTime=" + taskCompletionTime +
//                ", taskWorkflowId=" + taskWorkflowId +
//                ", currentActivityIndicator=" + currentActivityIndicator +
//                ", taskTypeId=" + taskTypeId +
//                ", parentTaskTypeId=" + parentTaskTypeId +
//                // check if we need to include transient field childTaskList
//                // check if we need to include transient field parentTaskResponse
//                ", childTaskIds=" + (childTaskIds != null ? childTaskIds.toString() : null) +
//                ", bugTaskRelation=" + (bugTaskRelation != null ? bugTaskRelation.toString() : null) +
//                ", meetingList=" + (meetingList != null ? meetingList.toString() : null) +
//                ", epicId=" + epicId +
//                ", parentTaskId=" + parentTaskId +
//                ", referenceEntityId=" + referenceEntityId +
//                ", taskEstimate=" + taskEstimate +
//                ", userPerceivedRemainingTimeForCompletion=" + userPerceivedRemainingTimeForCompletion +
//                ", parkingLot='" + parkingLot + '\'' +
//                ", keyDecisions='" + keyDecisions + '\'' +
//                ", acceptanceCriteria='" + acceptanceCriteria + '\'' +
//                ", taskProgressSystem=" + taskProgressSystem +
//                ", taskProgressSystemLastUpdated=" + taskProgressSystemLastUpdated +
//                ", nextTaskProgressSystemChangeDateTime=" + nextTaskProgressSystemChangeDateTime +
//                ", taskProgressSetByUser=" + taskProgressSetByUser +
//                ", taskProgressSetByAccountId=" + taskProgressSetByAccountId +
//                ", taskProgressSetByAccountIdLastUpdated=" + taskProgressSetByAccountIdLastUpdated +
//                ", taskProgressSetByUserLastUpdated=" + taskProgressSetByUserLastUpdated +
//                ", taskState='" + taskState + '\'' +
//                ", currentlyScheduledTaskIndicator=" + currentlyScheduledTaskIndicator +
//                ", unplannedScheduledTaskIndicator=" + unplannedScheduledTaskIndicator +
//                ", taskPriority='" + taskPriority + '\'' +
//                ", version=" + version +
//                ", createdDateTime=" + createdDateTime +
//                ", lastUpdatedDateTime=" + lastUpdatedDateTime +
//                ", taskDependency=" + taskDependency +
//                ", systemDerivedEndTs=" + systemDerivedEndTs +
//                ", immediateAttention=" + immediateAttention +
//                ", immediateAttentionFrom='" + immediateAttentionFrom + '\'' +
//                ", userPerceivedPercentageTaskCompleted=" + userPerceivedPercentageTaskCompleted +
//                ", userPerceivedPercentageTaskEarnedValue=" + userPerceivedPercentageTaskEarnedValue +
//                ", earnedTimeTask=" + earnedTimeTask +
//                ", increaseInUserPerceivedPercentageTaskCompleted=" + increaseInUserPerceivedPercentageTaskCompleted +
//                ", accountIdPrevAssigned1=" + (accountIdPrevAssigned1 != null ? accountIdPrevAssigned1.toString() : null) +
//                ", accountIdPrevAssigned2=" + (accountIdPrevAssigned2 != null ? accountIdPrevAssigned2.toString() : null) +
//                ", accountIdPrevAssignee1=" + (accountIdPrevAssignee1 != null ? accountIdPrevAssignee1.toString() : null) +
//                ", accountIdPrevAssignee2=" + (accountIdPrevAssignee2 != null ? accountIdPrevAssignee2.toString() : null) +
//                ", estimateTimeLogEvaluation='" + estimateTimeLogEvaluation + '\'' +
//                ", taskCompletionImpact='" + taskCompletionImpact + '\'' +
//                ", isBallparkEstimate=" + isBallparkEstimate +
//                ", isEstimateSystemGenerated=" + isEstimateSystemGenerated +
//                ", environmentId=" + environmentId +
//                ", resolutionId=" + resolutionId +
//                ", severityId=" + severityId +
//                ", stepsTakenToComplete='" + stepsTakenToComplete + '\'' +
//                ", placeOfIdentification=" + placeOfIdentification +
//                ", customerImpact=" + customerImpact +
//                ", linkedTaskList=" + (linkedTaskList != null ? linkedTaskList.toString() : null) +
//                ", fkTeamId=" + (fkTeamId != null ? fkTeamId.toString() : null) +
//                ", fkAccountId=" + (fkAccountId != null ? fkAccountId.toString() : null) +
//                ", fkProjectId=" + (fkProjectId != null ? fkProjectId.toString() : null) +
//                ", fkAccountIdCreator=" + (fkAccountIdCreator != null ? fkAccountIdCreator.toString() : null) +
//                ", fkAccountIdAssignee=" + (fkAccountIdAssignee != null ? fkAccountIdAssignee.toString() : null) +
//                ", fkAccountIdAssigned=" + (fkAccountIdAssigned != null ? fkAccountIdAssigned.toString() : null) +
//                ", fkAccountIdLastUpdated=" + (fkAccountIdLastUpdated != null ? fkAccountIdLastUpdated.toString() : null) +
//                ", fkAccountIdMentor1=" + (fkAccountIdMentor1 != null ? fkAccountIdMentor1.toString() : null) +
//                ", fkAccountIdMentor2=" + (fkAccountIdMentor2 != null ? fkAccountIdMentor2.toString() : null) +
//                ", fkAccountIdObserver1=" + (fkAccountIdObserver1 != null ? fkAccountIdObserver1.toString() : null) +
//                ", fkAccountIdObserver2=" + (fkAccountIdObserver2 != null ? fkAccountIdObserver2.toString() : null) +
//                ", fkOrgId=" + (fkOrgId != null ? fkOrgId.toString() : null) +
//                ", fkWorkflowTaskStatus=" + (fkWorkflowTaskStatus != null ? fkWorkflowTaskStatus.toString() : null) +
//                ", comments=" + (comments != null ? comments.toString() : null) +
//                ", noteId=" + noteId +
//                ", listOfDeliverablesDeliveredId=" + listOfDeliverablesDeliveredId +
//                ", notes=" + (notes != null ? notes.toString() : null) +
//                ", listOfDeliverablesDelivered=" + (listOfDeliverablesDelivered != null ? listOfDeliverablesDelivered.toString() : null) +
//                ", deliverables='" + deliverables + '\'' +
//                ", blockedReasonTypeId=" + blockedReasonTypeId +
//                ", blockedReason='" + blockedReason + '\'' +
//                ", fkAccountIdRespondent=" + (fkAccountIdRespondent != null ? fkAccountIdRespondent.toString() : null) +
//                ", reminderInterval=" + reminderInterval +
//                ", nextReminderDateTime=" + nextReminderDateTime +
//                ", dependentTaskDetailResponseList=" + (dependentTaskDetailResponseList != null ? dependentTaskDetailResponseList.toString() : null) +
//                ", dependentTaskDetailRequestList=" + (dependentTaskDetailRequestList != null ? dependentTaskDetailRequestList.toString() : null) +
//                ", dependencyIds=" + (dependencyIds != null ? dependencyIds.toString() : null) +
//                ", labels=" + (labels != null ? labels.toString() : null) +
//                ", labelsToAdd=" + (labelsToAdd != null ? labelsToAdd.toString() : null) +
//                ", recordedTaskEffort=" + recordedTaskEffort +
//                ", totalEffort=" + totalEffort +
//                ", totalMeetingEffort=" + totalMeetingEffort +
//                ", billedMeetingEffort=" + billedMeetingEffort +
//                ", meetingEffortPreferenceId=" + meetingEffortPreferenceId +
//                ", isSprintChanged=" + isSprintChanged +
//                '}';
//    }

}
