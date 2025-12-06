package com.tse.core_application.repository.geo_fencing.punch;

import com.tse.core_application.model.geo_fencing.punch.PunchRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PunchRequestRepository extends JpaRepository<PunchRequest, Long> {

    Optional<PunchRequest> findByIdAndOrgId(Long id, Long orgId);

    /**
     * Find pending requests for specific target entities within the active time window.
     */
    @Query("SELECT pr FROM PunchRequest pr " +
           "WHERE pr.orgId = :orgId " +
           "AND pr.state = 'PENDING' " +
           "AND pr.requestedDatetime <= :now " +
           "AND pr.expiresAt > :now " +
           "AND pr.entityTypeId = :entityTypeId " +
           "AND pr.entityId IN :entityIds")
    List<PunchRequest> findPendingForEntities(
            @Param("orgId") Long orgId,
            @Param("now") LocalDateTime now,
            @Param("entityTypeId") Integer entityTypeId,
            @Param("entityIds") Collection<Long> entityIds
    );

    /**
     * Find requests whose time window overlaps the given range [from, to).
     */
    @Query("SELECT pr FROM PunchRequest pr " +
           "WHERE pr.orgId = :orgId " +
           "AND pr.requestedDatetime < :to " +
           "AND pr.expiresAt > :from " +
           "AND pr.entityTypeId = :entityTypeId " +
           "AND pr.entityId IN :entityIds " +
           "ORDER BY pr.requestedDatetime DESC")
    List<PunchRequest> findHistoryForEntities(
            @Param("orgId") Long orgId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("entityTypeId") Integer entityTypeId,
            @Param("entityIds") Collection<Long> entityIds
    );

    /**
     * Find all pending requests for an org (admin view).
     */
    @Query("SELECT pr FROM PunchRequest pr " +
           "WHERE pr.orgId = :orgId " +
           "AND pr.state = 'PENDING' " +
           "AND pr.requestedDatetime <= :now " +
           "AND pr.expiresAt > :now " +
           "ORDER BY pr.requestedDatetime ASC")
    List<PunchRequest> findAllPendingForOrg(
            @Param("orgId") Long orgId,
            @Param("now") LocalDateTime now
    );

    @Modifying
    @Transactional
    @Query("DELETE FROM PunchRequest pr WHERE pr.orgId = :orgId")
    void deleteByOrgId(Long orgId);
}
