package com.tse.core_application.model;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "workflow_epic_status", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@ToString
@NoArgsConstructor
public class WorkFlowEpicStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "workflow_epic_status_id", nullable = false, unique = true)
    private Integer workflowEpicStatusId;

    @Column(name = "workflow_epic_status", nullable = false, length = 50)
    private String workflowEpicStatus;

    @CreationTimestamp
    @Column(name = "created_date_time", updatable = false, nullable = false)
    private Timestamp createdDateTime;

    @UpdateTimestamp
    @Column(name = "last_updated_date_time", insertable = false)
    private Timestamp lastUpdatedDateTime;

}
