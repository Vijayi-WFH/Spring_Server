package com.tse.core.model.supplements;


import com.tse.core.configuration.DataEncryptionConverter;
import com.tse.core.model.Constants;
import com.tse.core.dto.supplements.RegistrationRequest;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.sql.Timestamp;

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
    @Size(max=50)
    private String email;

    @Column(name = "is_default")
    private Boolean isDefault;

    @Column(name = "is_active")
    private Boolean isActive;

    @CreationTimestamp
    @Column(name = "created_date_time", updatable = false, nullable = false)
    private Timestamp createdDateTime;

    @UpdateTimestamp
    @Column(name = "last_updated_date_time", insertable = false)
    private Timestamp lastUpdatedDateTime;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    private User fkUserId;

    public UserAccount getUserAccountFromRegistrationReq(RegistrationRequest request, Long orgId, User user) {
        this.setFkUserId(user);
        this.setOrgId(orgId);
        this.setEmail(request.getPrimaryEmail());
        this.setIsActive(true);
        return this;
    }


}
