package com.tse.core_application.model;

import com.tse.core_application.configuration.DataEncryptionConverter;
import com.tse.core_application.dto.RegistrationRequest;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "organization", schema= Constants.SCHEMA_NAME)
public class Organization {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "org_id", nullable = false, unique = true)
	private Long orgId;

	@Column(name = "organization_name", length = 500)
	@Convert(converter = DataEncryptionConverter.class)
	@Size(max=100)
	private String organizationName;

	@Column(name = "organization_display_name", length = 500)
	@Convert(converter = DataEncryptionConverter.class)
	@Size(max=100)
	private String organizationDisplayName;

	@Column(name = "owner_account_id")
	private Long ownerAccountId;

	@CreationTimestamp
	@Column(name = "created_date_time", updatable = false, nullable = false)
	private Timestamp createdDateTime;

	@UpdateTimestamp
	@Column(name = "last_updated_date_time", insertable = false)
	private Timestamp lastUpdatedDateTime;

	@Column(name = "is_disabled")
	private Boolean isDisabled = false;

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

	@Column(name = "used_memory_quota")
	private Long usedMemoryQuota = 0L;

	@Column(name = "owner_email")
	@Convert(converter = DataEncryptionConverter.class)
	private String ownerEmail;

	@Column(name = "paid_subscription")
	private Boolean paidSubscription;

	@Column(name = "on_trial")
	private Boolean onTrial;
	
	public Organization getOrgFromRegistrationReq(RegistrationRequest req) {
		this.setOrganizationName(req.getOrganizationName());
		this.setOrganizationDisplayName(req.getOrganizationName());
		return this;
	}
	
	public Organization getOrgReq(Long orgId, String organizationName) {
		this.setOrgId(orgId);
		this.setOrganizationName(organizationName);
		this.setOrganizationDisplayName(organizationName);
		return this;
	}

}
