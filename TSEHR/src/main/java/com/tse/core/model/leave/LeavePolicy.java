package com.tse.core.model.leave;

import com.tse.core.configuration.DataEncryptionConverter;
import com.tse.core.model.Constants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_policy", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LeavePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "leave_policy_id", nullable = false, unique = true)
    private Long leavePolicyId;

    @Column(name = "leave_type_id", nullable = false)
    private Short leaveTypeId;

    @Column(name = "org_id")
    private Long orgId;

    @Column(name = "bu_id")
    private Long buId;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "leave_policy_title", length = 300)
    @Convert(converter = DataEncryptionConverter.class)
    @Size(max=70)
    private String leavePolicyTitle;

    @Column(name = "initial_leaves",precision = 4,scale = 2,nullable = false)
    private Float initialLeaves;

    @Column(name = "is_leave_carry_forward",nullable = false)
    private Boolean isLeaveCarryForward;

    @Column(name = "max_leave_carry_forward",precision = 4,scale = 2)
    private Float maxLeaveCarryForward;

    @Column(name = "max_negative_leaves")
    private Float maxNegativeLeaves;

    @Column(name = "is_negative_leaves_allowed")
    private Boolean isNegativeLeaveAllowed;

    @Column(name = "created_by")
    private Long createdByAccountId;

    @Column(name = "last_updated_by")
    private Long lastUpdatedByAccountId;

    @CreationTimestamp
    @Column(name = "created_date_time", updatable = false, nullable = false)
    private LocalDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = "modified_date_time")
    private LocalDateTime modifiedDateTime;

    @Column(name = "include_non_business_days_in_leave", nullable = false)
    private Boolean includeNonBusinessDaysInLeave;
}
