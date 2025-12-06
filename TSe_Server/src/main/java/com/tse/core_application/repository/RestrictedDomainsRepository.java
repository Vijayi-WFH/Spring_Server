package com.tse.core_application.repository;

import com.tse.core_application.model.RestrictedDomains;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface RestrictedDomainsRepository extends JpaRepository<RestrictedDomains, Long> {

    @Query("SELECT r.domain FROM RestrictedDomains r where r.isPersonalAllowed = :isPersonalAllowed")
    List<String> findDomainByIsPersonalAllowed(Boolean isPersonalAllowed);

    @Query("SELECT r.domain FROM RestrictedDomains r where r.isOrgRegistrationAllowed = :isOrgRegistrationAllowed")
    List<String> findDomainByIsOrgRegistrationAllowed(Boolean isOrgRegistrationAllowed);

    Boolean existsByDomain(String domain);

    @Modifying
    @Query("UPDATE RestrictedDomains r SET r.isDeleted = true WHERE r.restrictedDomainId = :restrictedDomainId")
    void markDeleteById (Long restrictedDomainId);

    @Modifying
    @Transactional
    @Query("DELETE FROM RestrictedDomains rd WHERE rd.orgId = :orgId")
    void deleteByOrgId(Long orgId);
}
