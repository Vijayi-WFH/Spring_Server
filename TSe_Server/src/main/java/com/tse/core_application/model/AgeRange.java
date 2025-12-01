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
@ToString
@Table(name="age_range", schema= Constants.SCHEMA_NAME)
public class AgeRange {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "age_range_id", nullable = false, unique = true)
	private Integer ageRangeId;
	
	@Column(name = "age_range_display_name",nullable = false, length = 50)
	private String ageRangeDisplayName;
	
	@Column(name = "age_range_description", nullable = false, length = 100)
	private String ageRangeDescription;

	@CreationTimestamp
	@Column(name = "created_date_time", updatable = false, nullable = false)
	private Timestamp createdDateTime;

	@UpdateTimestamp
	@Column(name = "last_updated_date_time", insertable = false)
	private Timestamp lastUpdatedDateTime;


}
