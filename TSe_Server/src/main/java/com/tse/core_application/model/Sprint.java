package com.tse.core_application.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tse.core_application.configuration.DataEncryptionConverter;
import com.tse.core_application.constants.ErrorConstant;
import com.tse.core_application.custom.model.EmailFirstLastAccountId;
import com.tse.core_application.dto.ProgressSystemSprintTask;
import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "sprint", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Sprint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sprint_id")
    private Long sprintId;

    @Column(name = "sprint_title", nullable = false)
    @Size(min = 3, max = 70, message = ErrorConstant.Sprint.TITLE_LIMIT)
    @Convert(converter = DataEncryptionConverter.class)
    private String sprintTitle;

    @Column(name = "sprint_objective", nullable = false, length = 5000)
    @Size(min = 3, max = 1000, message = ErrorConstant.Sprint.OBJECTIVE_LIMIT)
    @Convert(converter = DataEncryptionConverter.class)
    private String sprintObjective;

    @Column(name = "sprint_exp_start_date", nullable = false)
    private LocalDateTime sprintExpStartDate;

    @Column(name = "sprint_exp_end_date", nullable = false)
    private LocalDateTime sprintExpEndDate;

    @Column(name = "capacity_adjustment_deadline")
    private LocalDateTime capacityAdjustmentDeadline;

    @Column(name = "sprint_act_start_date")
    private LocalDateTime sprintActStartDate;

    @Column(name = "sprint_act_end_date")
    private LocalDateTime sprintActEndDate;

    @Column(name = "sprint_status")
    private Integer sprintStatus;

    @Column(name = "entity_type_id", nullable = false)
    private Integer entityTypeId;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "sprint_members", length = 10000)
    @Convert(converter = DataEncryptionConverter.class)
    private String sprintMembers;

    @Column(name = "hours_of_sprint")
    private Integer hoursOfSprint;

    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id_creator", referencedColumnName = "account_id")
    @JsonIgnore
    private UserAccount fkAccountIdCreator;

    @Column(name = "earned_efforts")
    private Integer earnedEfforts = 0;

    @Column(name = "next_sprint_id")
    private Long nextSprintId;

    @Column(name = "previous_sprint_id")
    private Long previousSprintId;

    @Column(name = "can_modify_estimates")
    private Boolean canModifyEstimates = false;

    @Column(name = "can_modify_indicator_stay_active_in_started_sprint")
    private Boolean canModifyIndicatorStayActiveInStartedSprint = false;

    @Column(name = "to_do_work_item_list", columnDefinition = "TEXT")
    @Convert(converter = DataEncryptionConverter.class)
    private String toDoWorkItemList;

    @Column(name = "in_progress_work_item_list", columnDefinition = "TEXT")
    @Convert(converter = DataEncryptionConverter.class)
    private String inProgressWorkItemList;

    @Column(name = "completed_work_item_list", columnDefinition = "TEXT")
    @Convert(converter = DataEncryptionConverter.class)
    private String completedWorkItemList ;

    @Version
    @Column(name = "version")
    private Long version;

    public Set<EmailFirstLastAccountId> getSprintMembers() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            if (sprintMembers != null && !sprintMembers.isEmpty()) {
                return objectMapper.readValue(sprintMembers, new TypeReference<Set<EmailFirstLastAccountId>>() {});
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setSprintMembers(Set<EmailFirstLastAccountId> sprintMembers) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            this.sprintMembers = objectMapper.writeValueAsString(sprintMembers);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException("Error while serializing sprintMembers to JSON", e);
        }
    }

    public List<ProgressSystemSprintTask> getToDoWorkItemList() {
        return convertJsonToList(toDoWorkItemList);
    }

    public void setToDoWorkItemList(List<ProgressSystemSprintTask> toDoWorkItemList) {
        this.toDoWorkItemList = convertListToJson(toDoWorkItemList);
    }

    public List<ProgressSystemSprintTask> getInProgressWorkItemList () {
        return convertJsonToList(inProgressWorkItemList);
    }

    public void setInProgressWorkItemList (List<ProgressSystemSprintTask> inProgressWorkItemList) {
        this.inProgressWorkItemList  = convertListToJson(inProgressWorkItemList);
    }

    public List<ProgressSystemSprintTask> getCompletedWorkItemList () {
        return convertJsonToList(completedWorkItemList );
    }

    public void setCompletedWorkItemList (List<ProgressSystemSprintTask> completedWorkItemList) {
        this.completedWorkItemList  = convertListToJson(completedWorkItemList);
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
