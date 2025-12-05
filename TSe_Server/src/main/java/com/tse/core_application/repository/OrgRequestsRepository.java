package com.tse.core_application.repository;

import com.tse.core_application.model.OrgRequests;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface OrgRequestsRepository extends JpaRepository<OrgRequests,Long> {

    @Query("select count(o) from OrgRequests o where o.fromOrgId=:orgId and o.forUserId=:userId and o.isAccepted=false")
    int existsByOrgIdUserIdAndIsAccepted(Long orgId, Long userId);

    @Query("select o from OrgRequests o where o.forUserId=:userId and o.isAccepted=false")
    List<OrgRequests> findByForUserIdAndIsAccepted(Long userId);

    @Modifying
    @Transactional
    @Query("update OrgRequests o set o.isAccepted=:response where o.orgRequestId=:orgRequestId")
    int updateIsAcceptedByOrgRequestId(Boolean response, Long orgRequestId);

    OrgRequests findByOrgRequestId(Long orgRequestId);

    @Modifying
    @Transactional
    @Query("DELETE FROM OrgRequests or WHERE or.fromOrgId = :orgId")
    void deleteByOrgId(Long orgId);
}
