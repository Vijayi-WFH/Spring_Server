package com.tse.core.model.supplements;

import com.tse.core.model.Constants;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.LocalTime;

@Entity
@Table(name = "office_hours", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class OfficeHours {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "office_hours_id", nullable = false, unique = true)
    private Integer officeHoursId;

    @Column(name = "workflow_type_id", nullable = false)
    private Integer workflowTypeId;

    @Column(name = "key", nullable = false, length = 50)
    private String key;

    @Column(name = "value", nullable = false)
    private LocalTime value;

    @CreationTimestamp
    @Column(name = "created_date_time", updatable = false, nullable = false)
    private Timestamp createdDateTime;


}
