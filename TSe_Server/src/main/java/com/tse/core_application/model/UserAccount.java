package com.tse.core_application.model;


import com.tse.core_application.configuration.DataEncryptionConverter;
import com.tse.core_application.dto.RegistrationRequest;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_account", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id", nullable = false, unique = true)
    private Long accountId;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "email", nullable = false)
    @Convert(converter = DataEncryptionConverter.class)
    @Size(max=70)
    private String email;

    @Column(name = "is_default")
    private Boolean isDefault;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_date_time", updatable = false, nullable = false)
    private Timestamp createdDateTime;

    @UpdateTimestamp
    @Column(name = "last_updated_date_time", insertable = false)
    private Timestamp lastUpdatedDateTime;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    private User fkUserId;

    @Column(name = "account_deactivated_date")
    private LocalDateTime accountDeactivatedDate;

    @Column(name = "is_disabled_by_sams")
    private Boolean isDisabledBySams;

    @Column(name = "is_verified")
    private Boolean isVerified = true;

    @Column(name = "is_registered_in_ai_service")
    private Boolean isRegisteredInAiService = true;

    private Integer deactivatedByRole;

    private Long deactivatedByAccountId;

    public UserAccount getUserAccountFromRegistrationReq(RegistrationRequest request, Long orgId, User user) {
        this.setFkUserId(user);
        this.setOrgId(orgId);
        this.setEmail(request.getPrimaryEmail());
        this.setIsActive(true);
        return this;
    }

//    @Override
//    public String toString() {
//        return "UserAccount{" +
//                "accountId=" + accountId +
//                ", orgId=" + orgId +
//                ", email='" + email + '\'' +
//                ", isDefault=" + isDefault +
//                ", isActive=" + isActive +
//                ", createdDateTime=" + createdDateTime +
//                ", lastUpdatedDateTime=" + lastUpdatedDateTime +
//                ", fkUserId=" + (fkUserId != null ? fkUserId.toString() : null) +
//                '}';
//    }



}
