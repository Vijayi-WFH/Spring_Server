package com.tse.core_application.model;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.sql.Timestamp;

@Data
@Entity
@Table(name = "resolution", schema = Constants.SCHEMA_NAME)
public class Resolution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "resolution_id", nullable = false, unique = true)
    private Integer resolutionId;

    @Column(name = "resolution_display_name", nullable = false, length = 50)
    private String resolutionDisplayName;

    @Column(name = "resolution_description", nullable = false, length = 100)
    private String resolutionDescription;

    @CreationTimestamp
    @Column(name = "created_date_time", updatable = false, nullable = false)
    private Timestamp createdDateTime;

    @UpdateTimestamp
    @Column(name = "last_updated_date_time", insertable = false)
    private Timestamp lastUpdatedDateTime;


}
