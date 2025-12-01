package com.tse.core_application.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dependency_relation_type", schema = Constants.SCHEMA_NAME)
public class DependencyRelationType {

    @Id
    @Column(name = "relation_type_id")
    private Integer relationTypeId;

    @Column(name = "relation_name", nullable = false, unique = true)
    private String relationName;

    @Column(name = "relation_desc")
    private String relationDesc;

    @CreationTimestamp
    @Column(name = "created_date_time")
    private LocalDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = "updated_date_time")
    private LocalDateTime updatedDateTime;

}
