package com.tse.core_application.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "table_columns_type", schema = Constants.SCHEMA_NAME)
@Data
@NoArgsConstructor
public class TableColumnsType {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "table_column_type_id", nullable = false, unique = true)
	private Integer tableColumnTypeId;
	
	@Column(name = "table_name", nullable = false, length = 50)
	private String tableName;
	
	@Column(name = "column_name", nullable = false, length = 50)
	private String columnName;
	
	@Column(name = "column_type", nullable = false)
	private Integer columnType;
	
	@Column(name = "column_type_desc", nullable = false)
	private String columnTypeDesc;

	@CreationTimestamp
	@Column(name = "created_date_time", updatable = false, nullable = false)
	private Timestamp createdDateTime;

	@UpdateTimestamp
	@Column(name = "last_updated_date_time", insertable = false)
	private Timestamp lastUpdatedDateTime;
	
}
