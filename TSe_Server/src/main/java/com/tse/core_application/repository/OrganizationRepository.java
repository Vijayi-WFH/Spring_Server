package com.tse.core_application.repository;

import com.tse.core_application.custom.model.OrgIdOrgName;
import com.tse.core_application.custom.model.OrgDetailsForSuperUser;
import com.tse.core_application.model.Organization;
//import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

//	@Query("SELECT o FROM Organization o WHERE LOWER(o.organizationName) = LOWER(:orgName)")
//	Organization findByOrganizationName(@Param("orgName") String orgName);
//	Organization findByOrganizationNameIgnoreCase(String orgName);

	@Query("select o from Organization o where o.orgId in :orgIds and o.isDisabled = false")
	List<Organization> findByOrgIdIn(List<Long> orgIds);

	@Query("select new com.tse.core_application.custom.model.OrgIdOrgName(o.orgId, o.organizationName) from Organization o where o.orgId = :orgId and o.isDisabled = false")
	OrgIdOrgName findOrgIdAndOrganizationNameByOrgId(Long orgId);

	@Query("select o from Organization o where o.orgId = :orgId and o.isDisabled = false")
	Organization findByOrgId(Long orgId);

	@Query("select o.organizationName from Organization o where o.orgId = :orgId and o.isDisabled = false")
	String findOrganizationNameByOrgId(Long orgId);

	//TODO: remove after setting office hours for orgId
	@Query("select o.orgId from Organization o where o.isDisabled = false")
    List<Long> findAllOrgId();

    Organization findByOrgIdAndOwnerAccountIdIn(Long orgId, List<Long> account);

	List<OrgIdOrgName> findOrgIdAndOrganizationNameByOwnerAccountIdIn(List<Long> account);

	@Query("select new com.tse.core_application.custom.model.OrgIdOrgName(o.orgId, o.organizationName) from Organization o where o.orgId in :orgIds and o.isDisabled = false")
	List<OrgIdOrgName> findOrgIdAndOrganizationNameByOrgIdIn(List<Long> orgIds);

	Optional<Organization> findByOrganizationName(String orgName);

	@Modifying
	@Transactional
	@Query("update Organization o set o.isDisabled = :isDisabled where o.orgId = :orgId")
	void updateIsDisabledByOrgId (Long orgId, Boolean isDisabled);

    @Query("select new com.tse.core_application.custom.model.OrgDetailsForSuperUser(" +
            "o.orgId, o.organizationName, o.isDisabled, " +
            "o.maxBuCount, o.maxProjectCount, o.maxTeamCount, o.maxUserCount, " +
            "o.maxMemoryQuota, o.ownerEmail, o.paidSubscription, o.onTrial, o.ownerEmail, " +
            "u.firstName, u.lastName, " +
            "COALESCE(ep.isGeoFencingAllowed, false), " +
            "COALESCE(ep.isGeoFencingActive, false)) " +
            "from Organization o " +
            "join User u on u.primaryEmail = o.ownerEmail " +
            "left join EntityPreference ep on ep.entityTypeId = 2 and ep.entityId = o.orgId")
    List<OrgDetailsForSuperUser> findAllOrgDetails();

    @Query("SELECT count(o) FROM Organization o WHERE o.ownerEmail = :email")
	Integer getOrgCountByEmail(String email);

	@Query("SELECT o FROM Organization o WHERE o.ownerEmail in :ownerEmails")
	List<Organization> findAllOrgByOwnerEmailIn (@Param("ownerEmails") List<String> ownerEmails);

	@Query("select o.organizationName from Organization o where o.orgId = :orgId")
	String findOrgNameByOrgId(Long orgId);

	@Query("SELECT o FROM Organization o WHERE o.ownerEmail = :ownerEmail AND o.orgId = :orgId")
	Organization findByOwnerEmailAndOrgId(@Param("ownerEmail") String ownerEmail, @Param("orgId") Long orgId);

    Boolean existsByOrgId(Long orgId);

	@Query("select new com.tse.core_application.custom.model.OrgIdOrgName(o.orgId, o.organizationName) from Organization o where o.orgId IN :orgIdList and (o.isDisabled is null or o.isDisabled = false)")
	List<OrgIdOrgName> findOrgIdAndOrganizationNameByOrgId(List<Long> orgIdList);

	@Query("SELECT o FROM Organization o WHERE o.isDeletionRequested = true AND o.scheduledDeletionDate <= CURRENT_TIMESTAMP")
	List<Organization> findOrganizationsScheduledForDeletion();

	@Query("SELECT o FROM Organization o WHERE o.isDeletionRequested = true")
	List<Organization> findOrganizationsPendingDeletion();

	@Modifying
	@Transactional
	@Query("UPDATE Organization o SET o.isDeletionRequested = :isDeletionRequested, o.deletionRequestedAt = :deletionRequestedAt, " +
			"o.deletionRequestedByAccountId = :deletionRequestedByAccountId, o.deletionReason = :deletionReason, " +
			"o.scheduledDeletionDate = :scheduledDeletionDate WHERE o.orgId = :orgId")
	void updateDeletionRequestFields(@Param("orgId") Long orgId,
									 @Param("isDeletionRequested") Boolean isDeletionRequested,
									 @Param("deletionRequestedAt") java.sql.Timestamp deletionRequestedAt,
									 @Param("deletionRequestedByAccountId") Long deletionRequestedByAccountId,
									 @Param("deletionReason") String deletionReason,
									 @Param("scheduledDeletionDate") java.sql.Timestamp scheduledDeletionDate);

	@Modifying
	@Transactional
	@Query("UPDATE Organization o SET o.isDeletionRequested = false, o.deletionRequestedAt = null, " +
			"o.deletionRequestedByAccountId = null, o.deletionReason = null, " +
			"o.scheduledDeletionDate = null WHERE o.orgId = :orgId")
	void clearDeletionRequestFields(@Param("orgId") Long orgId);
}
