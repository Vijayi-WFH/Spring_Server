package com.tse.core.repository.supplements;

import com.tse.core.model.supplements.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Long findOrgIdByOrganizationName(String organizationName);
}
