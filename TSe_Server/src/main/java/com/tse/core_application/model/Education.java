package com.tse.core_application.model;

import com.tse.core_application.model.Constants;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.sql.Timestamp;


@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "education", schema= Constants.SCHEMA_NAME)
public class Education {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "education_id", nullable = false, unique = true)
	private Integer educationId;

	@Column(name = "education_display_name", nullable = false, length = 50)
	private String educationDisplayName;

	@Column(name = "education_description", nullable = false, length = 100)
	private String educationDescription;

	@CreationTimestamp
	@Column(name = "created_date_time", updatable = false, nullable = false)
	private Timestamp createdDateTime;

	@UpdateTimestamp
	@Column(name = "last_updated_date_time", insertable = false)
	private Timestamp lastUpdatedDateTime;


}
