package com.tse.core_application.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "task_history_columns_mapping", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TaskHistoryColumnsMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_history_columns_mapping_id", nullable = false, unique = true)
    private Integer task_history_columns_mapping_id;

    @Column(name = "task_history_columns_mapping_key", nullable = false)
    private Integer taskHistoryColumnsMappingKey;

    @Column(name = "columns_desc", nullable = false, length = 100)
    private String columnsDesc;

    @Column(name = "column_name", nullable = false, length = 100)
    private String columnName;

    @Column(name = "is_active", nullable = false)
    private Integer isActive;

    @CreationTimestamp
    @Column(name = "created_date_time", updatable = false, nullable = false)
    private LocalDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = "last_updated_date_time", insertable = false)
    private LocalDateTime lastUpdatedDateTime;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

}
