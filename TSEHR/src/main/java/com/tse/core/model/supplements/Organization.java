package com.tse.core.model.supplements;

import com.tse.core.configuration.DataEncryptionConverter;
import com.tse.core.dto.supplements.RegistrationRequest;
import com.tse.core.model.Constants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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

	@Column(name = "organization_name")
	@Convert(converter = DataEncryptionConverter.class)
	@Size(max=50)
	private String organizationName;

	@Column(name = "organization_display_name")
	@Convert(converter = DataEncryptionConverter.class)
	@Size(max=50)
	private String organizationDisplayName;

	@Column(name = "owner_account_id")
	private Long ownerAccountId;

	@CreationTimestamp
	@Column(name = "created_date_time", updatable = false, nullable = false)
	private Timestamp createdDateTime;

	@UpdateTimestamp
	@Column(name = "last_updated_date_time", insertable = false)
	private Timestamp lastUpdatedDateTime;

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
