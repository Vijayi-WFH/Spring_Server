package com.tse.core_application.repository;

import com.tse.core_application.model.BlockedRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlockedRegistrationRepository extends JpaRepository<BlockedRegistration, Long> {

    Boolean existsByEmailAndOrganizationNameAndIsDeleted(String email, String organizationName, Boolean isDeleted);

    BlockedRegistration findByBlockedRegistrationId(Long registrationId);

    List<BlockedRegistration> findByIsDeleted (Boolean isDeleted);

    BlockedRegistration findByEmailAndOrganizationNameAndIsDeleted(String email, String organizationName, Boolean isDeleted);
}
