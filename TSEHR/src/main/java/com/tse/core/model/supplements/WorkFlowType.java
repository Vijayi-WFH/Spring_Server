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
@Table(name = "workflow_type", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@ToString
@NoArgsConstructor
public class WorkFlowType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "workflow_type_id", nullable = false, unique = true)
    private Integer workflowTypeId;

    @Column(name = "workflow_name", nullable = false, length = 50)
    private String workflowName;

    @Column(name = "workflow_sub_type_name", nullable = false, length = 50)
    private String workflowSubTypeName;

    @Column(name = "workflow_desc")
    private String workflowDesc;

    @Column(name = "workflow_sub_type_desc")
    private String workflowSubTypeDesc;

    @CreationTimestamp
    @Column(name = "created_date_time", nullable = false, updatable = false)
    private Timestamp createdDateTime;

    @UpdateTimestamp
    @Column(name = "last_updated_date_time", insertable = false)
    private Timestamp lastUpdatedDateTime;

}

