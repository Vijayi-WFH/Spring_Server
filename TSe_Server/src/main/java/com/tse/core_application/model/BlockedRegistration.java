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
@Table(name = "blocked_registration", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BlockedRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "blocked_registration_id")
    private Long blockedRegistrationId;

    @Column(name = "email", nullable = false, length = 350)
    @Convert(converter = DataEncryptionConverter.class)
    private String email;

    @Column(name = "organization_name", nullable = false)
    @Convert(converter = DataEncryptionConverter.class)
    private String organizationName;

    @Column(name = "is_deleted")
    private Boolean isDeleted;

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
}
