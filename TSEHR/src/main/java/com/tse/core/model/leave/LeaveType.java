package com.tse.core.model.leave;

import com.tse.core.model.Constants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "leave_type", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LeaveType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "leave_type_id", nullable = false, unique = true)
    private Short leaveTypeId;

    @Column(name = "leave_type", length = 50)
    private String leaveType;

    @Column(name = "leave_type_desc")
    private String leaveTypeDesc;
}
