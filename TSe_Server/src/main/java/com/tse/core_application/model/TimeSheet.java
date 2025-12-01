package com.tse.core_application.model;

import com.tse.core_application.configuration.DataEncryptionConverter;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
@Table(name = "time_tracking", schema = Constants.SCHEMA_NAME)
public class TimeSheet {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "time_tracking_id", nullable = false, unique = true)
	private Long timeTrackingId;

	@Column(name = "new_effort", nullable = false)
	private Integer newEffort;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "account_id", nullable = false)
	private Long accountId;

	@Column(name = "org_id", nullable = false)
	private Long orgId;

	@Column(name = "bu_id", nullable = false)
	private Long buId;

	@Column(name = "project_id", nullable = false)
	private Long projectId;

	@Column(name = "team_id", nullable = false)
	private Long teamId;

	@Column(name = "entity_id", nullable = false)
	private Long entityId;

//	// indicates id of the entity ex taskId for task entity -- taskId was renamed to entityId
//	@Column(name = "entity_id")
//	private Long entityId;

	// indicates type of entity ex task/ team
	@Column(name = "entity_type_id")
	private Integer entityTypeId;

	// indicates number of the entity ex entityNumber for task entity
	@Column(name = "entity_num")
	private String entityNumber;

	// indicates title for the entity ex entityTitle for task entity
	@Column(name = "entity_title", length = 1000)
//	@Convert(converter = DataEncryptionConverter.class)
	private String entityTitle;

	// indicates type of task: ex - Task , Sub task , Bug task , Meeting task ----- Added in TASK 2364
	@Nullable
	@Column(name = "task_type_id")
	private Integer taskTypeId;

	// indicates task type of ---> reference task ----- Added in TASK 2364
	@Nullable
	@Column(name = "reference_task_type_id")
	private Integer referenceTaskTypeId;

	// indicates id of entity like task id for ---> reference task ----- Added in TASK 2364
	@Nullable
	@Column(name = "reference_entity_id")
	private Long referenceEntityId;

	// indicates type id of entity like 6 for task id , 7 for meeting task id for ---> reference task ----- Added in TASK 2364
	@Nullable
	@Column(name = "reference_entity_type_id")
	private Integer referenceEntityTypeId;

	// indicates number of entity like task number for ---> reference task ----- Added in TASK 2364
	@Nullable
	@Column(name = "reference_entity_num")
	private String referenceEntityNum;

	// indicates title of entity like task title for ---> reference task ----- Added in TASK 2364
	@Nullable
	@Column(name = "reference_entity_title")
	@Convert(converter = DataEncryptionConverter.class)
	private String referenceEntityTitle;

	@Column(name = "new_effort_date", nullable = false)
	private LocalDate newEffortDate;

	@Column(name = "earned_time", nullable = true)
	private Integer earnedTime;

	@Column(name = "increase_in_user_perceived_percentage_task_completed")
	private Integer increaseInUserPerceivedPercentageTaskCompleted;

	@Column(name="increase_in_user_perceived_percentage_task_earned_value")
	private Integer increaseInUserPerceivedPercentageTaskEarnedValue;

	@Column(name = "sprint_id")
	private Long sprintId;

	@Column(name = "epic_id")
	private Long epicId;

	@CreationTimestamp
	@Column(name = "created_date_time", updatable = false, nullable = false)
	private LocalDateTime createdDateTime;

	@UpdateTimestamp
	@Column(name = "last_updated_date_time", insertable = false)
	private LocalDateTime lastUpdatedDateTime;
}
