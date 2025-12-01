package com.tse.core_application.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;

@Entity
@Table(name = "role", schema = Constants.SCHEMA_NAME)
@Data
@NoArgsConstructor
public class Role {

	@Id
	@GeneratedValue( strategy = GenerationType.IDENTITY)
	@Column(name = "role_id", nullable = false, unique = true)
	private Integer roleId;
	
	@Column(name = "role_name", nullable = false, length = 50)
	private String roleName;
	
	@Column(name = "role_desc", length = 256)
	private String roleDesc;

	@CreationTimestamp
	@Column(name = "created_date_time", updatable = false, nullable = false)
	private Timestamp createdDateTime;

	@UpdateTimestamp
	@Column(name = "last_updated_date_time", insertable = false)
	private Timestamp lastUpdatedDateTime;
	
	@OneToMany(mappedBy = "role", fetch = FetchType.LAZY)
	private List<UserRole> userRole;

	@ToString.Exclude
	@OneToMany(mappedBy = "role", fetch = FetchType.LAZY)
	private List<AccessDomain> accessDomains;

}
