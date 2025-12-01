package com.tse.core_application.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dependency", schema = Constants.SCHEMA_NAME)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Dependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dependency_id")
    private Long dependencyId;

    @Column(name = "predecessor_task_id")
    private Long predecessorTaskId;

    @Column(name = "successor_task_id")
    private Long successorTaskId;

    @Column(name = "relation_type_id")
    private Integer relationTypeId;  // Could also consider using an Enum for clarity

    @Column(name = "lag_time")
    private Integer lagTime;

    @Column(name = "is_removed")
    private Boolean isRemoved = false;

    @CreationTimestamp
    @Column(name = "created_date_time", updatable = false, nullable = false)
    private LocalDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = "last_updated_date_time", insertable = false)
    private LocalDateTime lastUpdatedDateTime;
}
