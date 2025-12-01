package com.tse.core.model.supplements;

import com.tse.core.model.Constants;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name="access_domain", schema= Constants.SCHEMA_NAME)
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class AccessDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "access_domain_id", nullable = false, unique = true)
    private Long accessDomainId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "entity_type_id", nullable = false)
    private Integer entityTypeId;

    @JoinColumn(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "role_id")
    private Integer roleId;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "workflow_type_id")
    private Integer workflowTypeId;

    @CreationTimestamp
    @Column(name = "created_date_time", updatable = false, nullable = false)
    private Timestamp createdDateTime;

    @UpdateTimestamp
    @Column(name = "last_updated_date_time", insertable = false)
    private Timestamp lastUpdatedDateTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, insertable = false, updatable = false)
    private UserAccount userAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_type_id", nullable = false, insertable = false, updatable = false)
    private EntityType entities;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false, insertable = false, updatable = false)
    private Role role;

}
