package com.tse.core_application.model;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Data
@Table(name = "environment", schema = Constants.SCHEMA_NAME)
public class Environment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "environment_id", nullable = false, unique = true)
    private Integer environmentId;

    @Column(name = "environment_display_name", nullable = false, length = 50)
    private String environmentDisplayName;

    @Column(name = "environment_description", nullable = false, length = 100)
    private String environmentDescription;

    @CreationTimestamp
    @Column(name = "created_date_time", updatable = false, nullable = false)
    private Timestamp createdDateTime;

    @UpdateTimestamp
    @Column(name = "last_updated_date_time", insertable = false)
    private Timestamp lastUpdatedDateTime;
}
