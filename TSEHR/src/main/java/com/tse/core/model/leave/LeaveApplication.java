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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "leave_application", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LeaveApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "leave_application_id", nullable = false, unique = true)
    private Long leaveApplicationId;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "leave_type_id")
    private Short leaveTypeId;

    @Column(name = "leave_application_status_id")
    private Short leaveApplicationStatusId;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "from_time")
    private LocalTime fromTime;

    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    @Column(name = "to_time")
    private LocalTime toTime;

    @Column(name = "include_lunch_time", nullable = false)
    private Boolean includeLunchTime;

    @Column(name = "leave_reason", nullable = false, length = 4000)
    @Convert(converter = DataEncryptionConverter.class)
    private String leaveReason;

    @Column(name = "approver_reason", length = 4000)
    @Convert(converter = DataEncryptionConverter.class)
    private String approverReason;

    @Column(name = "approver_account_id", nullable = false)
    private Long approverAccountId;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "address", length = 1000)
    @Convert(converter = DataEncryptionConverter.class)
    private String address;

    @Column(name = "notifyTo")
    @Convert(converter = DataEncryptionConverter.class)
    private String notifyTo;

    @Column(name = "doctor_certificate")
    private byte[] doctorCertificate;

    @Column(name="doctor_certificate_file_name")
    private String doctorCertificateFileName;

    @Column(name="doctor_certificate_file_type")
    private String doctorCertificateFileType;

    @Column(name="doctor_certificate_file_size")
    private Long doctorCertificateFileSize;

    @CreationTimestamp
    @Column(name = "created_date_time", nullable = false)
    private LocalDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = "last_updated_date_time")
    private LocalDateTime lastUpdatedDateTime;

    @Column(name = "is_leave_for_half_day")
    private Boolean isLeaveForHalfDay;

    @Column(name = "number_of_leave_days")
    private Float numberOfLeaveDays;
    @Column(name = "leave_cancellation_reason")
    @Convert(converter = DataEncryptionConverter.class)
    private String leaveCancellationReason;

    @Column(name="is_attachment_present")
    private Boolean isAttachmentPresent;

    private Integer halfDayLeaveType;

    private Boolean isSprintCapacityAdjustment;

    private LocalDate expiryLeaveDate;

}
