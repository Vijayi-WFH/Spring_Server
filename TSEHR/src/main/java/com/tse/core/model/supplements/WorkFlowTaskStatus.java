package com.tse.core.model.supplements;

import com.tse.core.model.Constants;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "workflow_task_status", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@ToString
@NoArgsConstructor
public class WorkFlowTaskStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "workflow_task_status_id", nullable = false, unique = true)
    private Integer workflowTaskStatusId;

    @Column(name = "workflow_task_status", nullable = false, length = 50)
    private String workflowTaskStatus;

    @Column(name = "workflow_task_state", length = 20, nullable = false)
    private String workflowTaskState;

    @CreationTimestamp
    @Column(name = "created_date_time", updatable = false, nullable = false)
    private Timestamp createdDateTime;

    @UpdateTimestamp
    @Column(name = "last_updated_date_time", insertable = false)
    private Timestamp lastUpdatedDateTime;

    @ManyToOne(optional = false)
    @JoinColumn(name = "workflow_type_id", referencedColumnName = "workflow_type_id")
    private WorkFlowType fkWorkFlowType;

}

