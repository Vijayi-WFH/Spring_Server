package com.tse.core_application.model.personal_task;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.tse.core_application.configuration.DataEncryptionConverter;
import com.tse.core_application.constants.ErrorConstant;
import com.tse.core_application.model.Constants;
import com.tse.core_application.model.UserAccount;
import com.tse.core_application.model.WorkFlowTaskStatus;
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
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "personal_task", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PersonalTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "personal_task_id")
    private Long personalTaskId;

    @Column(name = "personal_task_number", nullable = false)
    private String personalTaskNumber;

    @Column(name = "personal_task_identifier", nullable = false)
    private Long personalTaskIdentifier;

    @NotNull(message = ErrorConstant.Task.ACCOUNT_ID)
    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id", referencedColumnName = "account_id")
    private UserAccount fkAccountId;

    @NotBlank(message = ErrorConstant.Task.TASK_TITLE)
    @Column(name = "task_title", nullable = false)
    @Size(min = 3, max = 70, message = ErrorConstant.Task.TITLE_LIMIT)
    private String taskTitle;

    @Nullable
    @Column(name = "task_desc", length = 20000)
    @Size(min = 3, max = 5000, message = ErrorConstant.Task.DESC_LIMIT)
    @Convert(converter = DataEncryptionConverter.class)
    private String taskDesc; // nullable in case of personal task

    @NotNull(message = ErrorConstant.Task.fk_WORK_FLOW_TASK_STATUS_ID)
    @ManyToOne(optional = false)
    @JoinColumn(name = "workflow_task_status_id", referencedColumnName = "workflow_task_status_id")
    private WorkFlowTaskStatus fkWorkflowTaskStatus;

    @Column(name = "task_priority")
    private String taskPriority;

    @Column(name = "task_estimate")
    private Integer taskEstimate;

    @NotNull(message = ErrorConstant.Task.TASK_TYPE_ID)
    @Column(name = "task_type_id")
    private Integer taskTypeId = Constants.TaskTypes.TASK;

    @Column(name = "recorded_effort")
    private Integer recordedEffort;

    @Column(name = "user_perceived_percentage_task_completed")
    private Integer userPerceivedPercentageTaskCompleted;

    @Column(name = "task_exp_start_date")
    private LocalDateTime taskExpStartDate;

    @Column(name = "task_exp_end_date")
    private LocalDateTime taskExpEndDate;

    @Column(name = "task_act_st_date")
    private LocalDateTime taskActStDate;

    @Column(name = "task_act_end_date")
    private LocalDateTime taskActEndDate;

    @Column(name = "current_activity_indicator", nullable = false)
    private Boolean currentActivityIndicator = false;

    @Column(name = "currently_scheduled_task_indicator", nullable = false)
    private Boolean currentlyScheduledTaskIndicator = false;

    @Column(name = "task_workflow_id", nullable = false)
    private Integer taskWorkflowId = Constants.TaskWorkFlowIds.PERSONAL_TASK; // by default it will be Personal Task

    @Column(name = "earned_time_task")
    private Integer earnedTimeTask;

    @Column(name = "task_progress_system")
    @Enumerated(EnumType.STRING)
    private com.tse.core_application.model.StatType taskProgressSystem;

    // ToDo: fields related to the Stats
//    @Nullable
//    @Column(name = "task_progress_system_last_updated")
//    private LocalDateTime taskProgressSystemLastUpdated;
//
//    @Nullable
//    @Column(name = "next_task_progress_system_change_date_time")
//    private LocalDateTime nextTaskProgressSystemChangeDateTime;

//    Todo: to be included later
//    @Column(name = "list_of_deliverables_delivered_id")
//    private Long listOfDeliverablesDeliveredId;
//
//    @OneToMany(mappedBy = "task", fetch = FetchType.LAZY)
//    @JsonManagedReference
//    private List<DeliverablesDelivered> listOfDeliverablesDelivered;

    @Column(name = "attachments", length = 2500)
    private String attachments;

    @OneToMany(mappedBy = "personalTask", fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<PersonalNote> notes;

    // Todo: to be included later
//    @ManyToMany(fetch = FetchType.LAZY)
//    @JoinTable(name = "task_label", schema = Constants.SCHEMA_NAME, joinColumns = @JoinColumn(name = "task_id"), inverseJoinColumns = @JoinColumn(name = "label_id"))
//    private List<Label> labels;

    @Column(name = "key_decisions", length = 5000)
    @Size(max = 1000, message = ErrorConstant.Task.KEY_DECISIONS)
    @Convert(converter = DataEncryptionConverter.class)
    private String keyDecisions;

    @Column(name = "parking_lot", length = 5000)
    @Size(max = 1000, message = ErrorConstant.Task.PARKING_LOT)
    @Convert(converter = DataEncryptionConverter.class)
    private String parkingLot;

    @Column(name = "task_state", length = 100)
    private String taskState;

    @Column(name = "task_progress_system_last_updated")
    private LocalDateTime taskProgressSystemLastUpdated;

    @Column(name = "next_task_progress_system_change_date_time")
    private LocalDateTime nextTaskProgressSystemChangeDateTime;

    @Version
    @Column(name = "version")
    private Long version;

    @CreationTimestamp
    @Column(name = "created_date_time", updatable = false, nullable = false)
    private LocalDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = "last_updated_date_time", insertable = false)
    private LocalDateTime lastUpdatedDateTime;

}
