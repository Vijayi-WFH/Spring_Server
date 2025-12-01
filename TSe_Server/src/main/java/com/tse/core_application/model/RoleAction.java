package com.tse.core_application.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "role_action", schema = Constants.SCHEMA_NAME)
@Data
@NoArgsConstructor
public class RoleAction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "role_action_id", nullable = false, unique = true)
	private Integer roleActionId;
	
	@Column(name = "role_id", nullable = false)
	private Integer roleId;
	
	@Column(name = "action_id", nullable = false)
	private Integer actionId;

	@CreationTimestamp
	@Column(name = "created_date_time", updatable = false, nullable = false)
	private Timestamp createdDateTime;

	@UpdateTimestamp
	@Column(name = "last_updated_date_time", insertable = false)
	private Timestamp lastUpdatedDateTime;


}
