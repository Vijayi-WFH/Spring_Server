package com.tse.core_application.model;

import com.tse.core_application.configuration.DataEncryptionConverter;
import com.tse.core_application.constants.ErrorConstant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "invite", schema = Constants.SCHEMA_NAME)
public class Invite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="invite_log_id", nullable = false)
    private Long inviteLogId;

    @Column(name = "first_name", length = 255)
    @Convert(converter = DataEncryptionConverter.class)
    @NotBlank(message = ErrorConstant.InviteError.FIRST_NAME)
    private String firstName;

    @Column(name = "middle_name", length = 255)
    @Convert(converter = DataEncryptionConverter.class)
    private String middleName;

    @Column(name = "last_name", length = 255)
    @Convert(converter = DataEncryptionConverter.class)
    @NotBlank(message = ErrorConstant.InviteError.LAST_NAME)
    private String lastName;

    @Convert(converter = DataEncryptionConverter.class)
    @Email(message = ErrorConstant.InviteError.EMAIL_FORMAT)
    @NotNull(message = ErrorConstant.InviteError.EMAIL)
    @Column(name = "primary_email", length = 350)
    private String primaryEmail;

    @NotNull(message = ErrorConstant.InviteError.ENTITY_TYPE_ID)
    @Column(name = "entity_type_id")
    private Integer entityTypeId;

    @NotNull(message = ErrorConstant.InviteError.ENTITY_ID)
    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "invite_id")
    private String inviteId; // Unique identifier (UUID) for the invite

    @NotNull(message = ErrorConstant.InviteError.SENT_DATE)
    @Column(name = "user_local_sent_date")
    private LocalDate userLocalSentDate; // User's local date when the invite was sent

    @NotNull(message = ErrorConstant.InviteError.VALIDITY_DURATION_REQUIRED)
    @Min(value = 0, message = ErrorConstant.InviteError.VALIDITY_DURATION_RANGE)
    @Column(name = "validity_duration")
    private Integer validityDuration = 2; // number of days the invite is valid

    @Column(name = "is_revoked")
    private Boolean isRevoked = false; // default is false

    @Column(name = "is_expired")
    private Boolean isExpired = false; // default is false

    @Column(name = "is_accepted")
    private Boolean isAccepted = false;

    @CreationTimestamp
    @Column(name = "created_date_time", updatable = false, nullable = false)
    private LocalDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = "last_updated_date_time", insertable = false)
    private LocalDateTime lastUpdatedDateTime;

    @Transient
    private String message;
}
