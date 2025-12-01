package com.tse.core_application.model;


import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tse.core_application.configuration.DataEncryptionConverter;
import com.tse.core_application.constants.ErrorConstant;
import com.tse.core_application.dto.ProgressSystemSprintTask;
import com.tse.core_application.utils.LongListConverter;
import com.tse.core_application.validators.CleanedSize;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "epic", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Epic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "epic_id", nullable = false)
    private Long epicId;

    @NotBlank(message = ErrorConstant.Epic.EPIC_TITLE)
    @Column(name = "epic_title", nullable = false, length = 255)
    @Size(min = 3, max = 70, message = ErrorConstant.Epic.EPIC_LIMIT)
    @Convert(converter = DataEncryptionConverter.class)
    private String epicTitle;

    @Column(name = "epic_number", nullable = false, length = 40)
    private String epicNumber;

    @NotBlank(message = ErrorConstant.Epic.EPIC_DESC)
    @Column(name = "epic_desc", nullable = false, length = 20000)
    @Size(min = 3, max = 5000, message = ErrorConstant.Epic.DESC_LIMIT)
    @Convert(converter = DataEncryptionConverter.class)
    private String epicDesc;

    @Column(name = "epic_priority")
    private String epicPriority;

    @NotNull(message = ErrorConstant.Epic.WORK_FLOW_EPIC_STATUS_ID)
    @ManyToOne(optional = false)
    @JoinColumn(name = "workflow_epic_status_id", referencedColumnName = "workflow_epic_status_id")
    private WorkFlowEpicStatus fkWorkflowEpicStatus;

    @Column(name = "note",length=5000)
    private String epicNote;

    @Column(name = "linked_epics")
    @Convert(converter = LongListConverter.class)
    private List<Long> linkedEpicId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "org_id", referencedColumnName = "org_id", nullable = false)
    private Organization fkOrgId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "project_id", referencedColumnName = "project_id", nullable = false)
    private Project fkProjectId;

    @Column(name = "team_id_list", nullable = false)
    @Convert(converter = LongListConverter.class)
    private List<Long> teamIdList;

    @Column(name = "entity_type_id", nullable = false)
    private Integer entityTypeId;

    @Column(name = "entity_id")
    private Long entityId;

//    @ManyToMany(fetch = FetchType.LAZY)
//    @JoinTable(name = "epic_label", schema = Constants.SCHEMA_NAME, joinColumns = @JoinColumn(name = "epic_id"), inverseJoinColumns = @JoinColumn(name = "label_id"))
//    private List<Label> epicLabels;

//    @Transient
//    private List<String> labelsToAdd;

    @Column(name = "estimate")
    private Integer estimate;

    @Column(name = "calculated_estimate")
    private Integer originalEstimate;

    @Column(name = "running_estimate")
    private Integer runningEstimate;

    @ManyToOne
    @JoinColumn(name = "account_id_assigned", referencedColumnName = "account_id")
    private UserAccount fkAccountIdAssigned;

    @ManyToOne
    @JoinColumn(name = "account_id_owner", referencedColumnName = "account_id")
    private UserAccount fkEpicOwner;

    @Column(name = "attachments", length = 2500)
    private String attachments;

    @Column(name = "release", length = 30)
    private String release;

    @Column(name = "epic_exp_start_date")
    private LocalDateTime expStartDateTime;

    @Column(name = "epic_act_st_date")
    private LocalDateTime actStartDateTime;

    @Column(name = "epic_exp_end_date")
    private LocalDateTime expEndDateTime;

    @Column(name = "epic_act_end_date")
    private LocalDateTime actEndDateTime;

    @Column(name = "epic_due_date")
    private LocalDateTime dueDateTime;

    @Column(name = "value_area")
    @Size(max = 50, message = ErrorConstant.Epic.VALUE_LIMIT)
    private String valueArea;

    @Column(name = "functional_area", length = 255)
    @Size(max = 50, message = ErrorConstant.Epic.FUNCTIONAL_LIMIT)
    @Convert(converter = DataEncryptionConverter.class)
    private String functionalArea;

    @Column(name = "quarterly_area")
    @Size(max = 50, message = ErrorConstant.Epic.QUARTERLY_LIMIT)
    private String quarterlyPlan;

    @Column(name = "yearly_area")
    @Size(max = 20, message = ErrorConstant.Epic.YEARLY_LIMIT)
    private String yearlyPlan;

    @OneToMany(mappedBy = "epic", fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<EpicComment> comments;

    @Column(name = "color", length = 7)
    private String color;

    @Column(name = "backlog_work_item_list", columnDefinition = "TEXT")
    @Convert(converter = DataEncryptionConverter.class)
    private String backlogWorkItemList;

    @Column(name = "not_started_work_item_list", columnDefinition = "TEXT")
    @Convert(converter = DataEncryptionConverter.class)
    private String notStartedWorkItemList;

    @Column(name = "started_work_item_list", columnDefinition = "TEXT")
    @Convert(converter = DataEncryptionConverter.class)
    private String startedWorkItemList;

    @Column(name = "completed_work_item_list", columnDefinition = "TEXT")
    @Convert(converter = DataEncryptionConverter.class)
    private String completedWorkItemList;

    @Column(name = "logged_efforts")
    private Integer loggedEfforts;

    @Column(name = "earned_efforts")
    private Integer earnedEfforts;

    public List<ProgressSystemSprintTask> getBacklogWorkItemList() {
        return convertJsonToList(backlogWorkItemList);
    }

    public void setBacklogWorkItemList(List<ProgressSystemSprintTask> backlogWorkItemList) {
        this.backlogWorkItemList = convertListToJson(backlogWorkItemList);
    }

    public List<ProgressSystemSprintTask> getNotStartedWorkItemList() {
        return convertJsonToList(notStartedWorkItemList);
    }

    public void setNotStartedWorkItemList(List<ProgressSystemSprintTask> notStartedWorkItemList) {
        this.notStartedWorkItemList = convertListToJson(notStartedWorkItemList);
    }

    public List<ProgressSystemSprintTask> getStartedWorkItemList() {
        return convertJsonToList(startedWorkItemList);
    }

    public void setStartedWorkItemList(List<ProgressSystemSprintTask> startedWorkItemList) {
        this.startedWorkItemList = convertListToJson(startedWorkItemList);
    }

    public List<ProgressSystemSprintTask> getCompletedWorkItemList() {
        return convertJsonToList(completedWorkItemList);
    }

    public void setCompletedWorkItemList(List<ProgressSystemSprintTask> completedWorkItemList) {
        this.completedWorkItemList = convertListToJson(completedWorkItemList);
    }

    private List<ProgressSystemSprintTask> convertJsonToList(String json) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            if (json != null && !json.isEmpty()) {
                return objectMapper.readValue(json, new TypeReference<List<ProgressSystemSprintTask>>() {});
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String convertListToJson(List<ProgressSystemSprintTask> list) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException("Error while serializing task list to JSON", e);
        }
    }
}
