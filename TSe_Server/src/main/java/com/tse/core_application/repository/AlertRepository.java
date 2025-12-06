package com.tse.core_application.repository;

import com.tse.core_application.model.Alert;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findAllByFkAccountIdSenderAccountIdInAndIsDeleted(List<Long> accountIds, Boolean isDeleted);
    List<Alert> findAllByFkAccountIdReceiverAccountIdInAndIsDeleted(List<Long> accountIds, Boolean isDeleted);
    List<Alert> findAllByFkAccountIdReceiverAccountIdInAndAlertStatusAndIsDeleted(List<Long> accountIds, String alertStatus, Boolean isDeleted);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Alert a " +
            "SET a.isDeleted = true " +
            "WHERE a.isDeleted = false " +
            "AND a.createdDateTime < :cutoff")
    int updateIsDeletedTrueWhereCreatedDateTimeOlderThan(@Param("cutoff") Timestamp cutoff);

    @Modifying
    @Transactional
    @Query("DELETE FROM Alert a WHERE a.orgId = :orgId")
    void deleteByOrgId(Long orgId);

}
