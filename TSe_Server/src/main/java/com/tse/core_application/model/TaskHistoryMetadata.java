package com.tse.core_application.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "task_history_metadata", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TaskHistoryMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_history_metadata_id", nullable = false, unique = true)
    private Long taskHistoryMetadataId;

    @Column(name = "task_history_columns_mapping_id", nullable = false)
    private Integer taskHistoryColumnsMappingId;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "version", nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_date_time", updatable = false, nullable = false)
    private LocalDateTime createdDateTime;

    @Column(name = "created_by", length = 50)
    private String createdBy;

}
