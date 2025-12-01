package com.tse.core_application.model.personal_task;

import com.tse.core_application.configuration.DataEncryptionConverter;
import com.tse.core_application.constants.ErrorConstant;
import com.tse.core_application.model.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "personal_task_template", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PersonalTaskTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id", nullable = false, unique = true)
    private Long templateId;

    @Column(name = "template_number", nullable = false)
    private Long templateNumber;

    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id", referencedColumnName = "account_id")
    private UserAccount fkAccountId;

    @NotBlank(message = ErrorConstant.Task.TASK_TITLE)
    @Column(name = "task_title", nullable = false,  length = 255)
    @Convert(converter = DataEncryptionConverter.class)
    private String taskTitle;

    @Column(name = "template_title",  length = 255)
    @Convert(converter = DataEncryptionConverter.class)
    private String templateTitle;

    @NotBlank(message = ErrorConstant.Task.TASK_DESC)
    @Column(name = "task_desc", nullable = false, length = 20000)
    @Convert(converter = DataEncryptionConverter.class)
    private String taskDesc;

    @NotNull(message = ErrorConstant.Task.TASK_WORKFLOW_ID)
    @Column(name = "task_workflow_id", nullable = false)
    private Integer taskWorkflowId = Constants.TaskWorkFlowIds.PERSONAL_TASK; // by default it will be Personal Task

    @Nullable
    @Column(name = "task_estimate")
    private Integer taskEstimate;

    @Nullable
    @Column(name = "task_priority")
    private String taskPriority;

    @NotNull(message = ErrorConstant.Task.fk_WORK_FLOW_TASK_STATUS_ID)
    @ManyToOne(optional = false)
    @JoinColumn(name = "workflow_task_status_id", referencedColumnName = "workflow_task_status_id")
    private WorkFlowTaskStatus fkWorkflowTaskStatus;

    @ManyToOne
    @JoinColumn(name = "team_id", referencedColumnName = "team_id")
    private Team fkTeamId;

    @ManyToOne
    @JoinColumn(name = "project_id", referencedColumnName = "project_id")
    private Project fkProjectId;

    @ManyToOne
    @JoinColumn(name = "org_id", referencedColumnName = "org_id")
    private Organization fkOrgId;
}
