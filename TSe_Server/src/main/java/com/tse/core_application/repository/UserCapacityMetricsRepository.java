package com.tse.core_application.repository;

import com.tse.core_application.model.UserCapacityMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserCapacityMetricsRepository extends JpaRepository<UserCapacityMetrics, Long> {

    @Query("SELECT u FROM UserCapacityMetrics u WHERE u.teamId = :teamId AND u.sprintId = :sprintId AND u.accountId = :accountId AND u.isRemoved = false")
    UserCapacityMetrics findByTeamIdAndSprintIdAndAccountId(Long teamId, Long sprintId, Long accountId);

    @Query("SELECT u FROM UserCapacityMetrics u WHERE u.sprintId = :sprintId AND u.isRemoved = false")
    List<UserCapacityMetrics> findBySprintId(Long sprintId);

    @Query("SELECT u FROM UserCapacityMetrics u WHERE u.sprintId = :sprintId AND u.accountId = :accountId AND u.isRemoved = false")
    Optional<UserCapacityMetrics> findBySprintIdAndAccountId(Long sprintId, Long accountId);

    @Query("SELECT u FROM UserCapacityMetrics u WHERE u.sprintId = :sprintId AND u.accountId IN :accountIds AND u.isRemoved = false")
    List<UserCapacityMetrics> findBySprintIdAndAccountIdIn(Long sprintId, List<Long> accountIds);

    @Modifying
    @Query("UPDATE UserCapacityMetrics u SET u.isRemoved = true where u.sprintId = :sprintId AND u.accountId = :accountId")
    void removeUserCapacity(Long sprintId, Long accountId);

    @Modifying
    @Transactional
    @Query("DELETE FROM UserCapacityMetrics ucm WHERE ucm.orgId = :orgId")
    void deleteByOrgId(Long orgId);
}
