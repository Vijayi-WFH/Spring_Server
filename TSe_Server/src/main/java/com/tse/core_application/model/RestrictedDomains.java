package com.tse.core_application.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "restricted_domain", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RestrictedDomains {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "restricted_domain_id", nullable = false)
    private Long restrictedDomainId;

    @Column(name = "domain", nullable = false)
    private String domain;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "is_personal_allowed")
    private Boolean isPersonalAllowed = false;

    @Column(name = "is_org_registration_allowed")
    private Boolean isOrgRegistrationAllowed = false;

    @CreationTimestamp
    @Column(name = "created_date_time")
    private LocalDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = "last_updated_date_time")
    private LocalDateTime lastUpdatedDateTime;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;
}
