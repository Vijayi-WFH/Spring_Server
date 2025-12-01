package com.tse.core_application.model;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Data
@Table(name = "task_type", schema = Constants.SCHEMA_NAME)
public class TaskType {

    @Id
    @Column(name = "task_type_id")
    private Integer taskTypeId;

    @Column(name = "task_type_name", nullable = false, length = 50)
    private String taskTypeName;

    @Column(name = "task_type_desc")
    private String taskTypeDesc;

    @CreationTimestamp
    @Column(name = "created_date_time", nullable = false, updatable = false)
    private Timestamp createdDateTime;
}