package com.tse.core_application.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tse.core_application.configuration.DataEncryptionConverter;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static com.tse.core_application.constants.Constants.ProjectType.USER_PROJECT;

@Entity
@Table(name = "project", schema=Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "project_name", length = 255, nullable = false)
    @Convert(converter = DataEncryptionConverter.class)
    private String projectName;

    @Column(name = "project_desc", length = 1000)
    @Convert(converter = DataEncryptionConverter.class)
    private String projectDesc;

    @Column(name = "project_type")
    private Integer projectType = USER_PROJECT;   // default project - 1 and user created project - 2

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "owner_account_id")
    private Long ownerAccountId;

    @Column(name = "bu_id", nullable = false)
    private Long buId;

    @CreationTimestamp
    @Column(name = "created_date_time", nullable = false, updatable = false)
    private LocalDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = "last_updated_date_time", insertable = false)
    private LocalDateTime lastUpdatedDateTime;

    @Column(name = "is_disabled")
    private Boolean isDisabled = false;

    @Column(name = "is_deleted")
    private Boolean isDeleted;

    @Column(name = "deleted_on")
    private LocalDateTime deletedOn;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "deleted_by_accountId", referencedColumnName = "account_id")
    private UserAccount fkDeletedByAccountId;
}
