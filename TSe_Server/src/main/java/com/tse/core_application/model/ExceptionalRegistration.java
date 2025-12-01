package com.tse.core_application.model;

import com.tse.core_application.configuration.DataEncryptionConverter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "exceptional_registration", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ExceptionalRegistration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exceptional_registration_id")
    private Long exceptionalRegistrationId;

    @Column(name = "email", nullable = false, length = 350)
    @Convert(converter = DataEncryptionConverter.class)
    private String email;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_date_time")
    private LocalDateTime createdDateTime;

    @ManyToOne
    @JoinColumn(name = "created_by_account_id", referencedColumnName = "account_id")
    private UserAccount createdByAccountId;

    @UpdateTimestamp
    @Column(name = "modifed_date_time")
    private LocalDateTime modifiedDateTime;

    @ManyToOne
    @JoinColumn(name = "modified_by_account_id", referencedColumnName = "account_id")
    private UserAccount modifiedByAccountId;

    @Column(name = "paid_subscription")
    private Boolean paidSubscription;

    @Column(name = "on_trial")
    private Boolean onTrial;

    @Column(name = "max_org_count")
    private Integer maxOrgCount;

    @Column(name = "max_bu_count")
    private Integer maxBuCount;

    @Column(name = "max_project_count")
    private Integer maxProjectCount;

    @Column(name = "max_team_count")
    private Integer maxTeamCount;

    @Column(name = "max_user_count")
    private Integer maxUserCount;

    @Column(name = "max_memory_quota")
    private Long maxMemoryQuota;
}
