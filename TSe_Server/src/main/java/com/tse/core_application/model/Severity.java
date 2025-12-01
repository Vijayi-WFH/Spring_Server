package com.tse.core_application.model;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.sql.Timestamp;

@Data
@Entity
@Table(name = "severity", schema = Constants.SCHEMA_NAME)
public class Severity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "severity_id", nullable = false, unique = true)
    private Integer severityId;

    @Column(name = "severity_display_name", nullable = false, length = 50)
    private String severityDisplayName;

    @Column(name = "severity_description", nullable = false, length = 100)
    private String severityDescription;

    @CreationTimestamp
    @Column(name = "created_date_time", updatable = false, nullable = false)
    private Timestamp createdDateTime;

    @UpdateTimestamp
    @Column(name = "last_updated_date_time", insertable = false)
    private Timestamp lastUpdatedDateTime;


}
