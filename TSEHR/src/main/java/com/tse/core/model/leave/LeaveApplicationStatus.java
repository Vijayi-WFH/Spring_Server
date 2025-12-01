package com.tse.core.model.leave;

import com.tse.core.model.Constants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "leave_application_status", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LeaveApplicationStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "leave_application_status_id", nullable = false, unique = true)
    private Short leaveApplicationStatusId;

    @Column(name = "leave_application_status", length = 50)
    private String leaveApplicationStatus;

    @Column(name = "leave_application_status_desc")
    private String leaveApplicationStatusDesc;
}
