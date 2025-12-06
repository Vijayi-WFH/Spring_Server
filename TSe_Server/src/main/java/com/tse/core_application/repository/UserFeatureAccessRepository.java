package com.tse.core_application.repository;

import com.tse.core_application.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface UserFeatureAccessRepository extends JpaRepository<UserFeaturesAccess,Long> {

    Optional<UserFeaturesAccess> findByEntityTypeIdAndEntityIdAndUserAccountId(
            Integer entityTypeId, Long entityId, Long userAccountId);

    List<UserFeaturesAccess> findByOrgIdAndIsDeleted(Long orgId, Boolean isDeleted);

    @Query("SELECT u FROM UserFeaturesAccess u WHERE u.userAccountId in :accountIds AND u.isDeleted=false")
    List<UserFeaturesAccess> findByUserAccountIds(@Param("accountIds") List<Long> accountIds);

    @Query(value = "SELECT DISTINCT ufa.user_account_id FROM tse.user_features_access ufa " +
            "WHERE ufa.entity_type_id = :entityTypeId " +
            "AND ufa.entity_id IN (:entityIds) " +
            "AND CAST(:actionId AS TEXT) = ANY(string_to_array(ufa.action_ids, ',')) " +
            "AND ufa.is_deleted = :isDeleted",
            nativeQuery = true)
    List<Long> findDistinctUserAccountIdByEntityTypeIdAndEntityIdInAndActionIdsAndIsDeleted(
            @Param("entityTypeId") Integer entityTypeId,
            @Param("entityIds") List<Long> entityIds,
            @Param("actionId") Integer actionId,
            @Param("isDeleted") Boolean isDeleted);

    @Query(value = "SELECT COUNT(*) > 0 FROM tse.user_features_access ufa " +
            "WHERE ufa.entity_type_id = :entityTypeId " +
            "AND ufa.entity_id = :entityId " +
            "AND ufa.user_account_id IN (:userAccountIds) " +
            "AND CAST(:actionId AS TEXT) = ANY(string_to_array(ufa.action_ids, ',')) " +
            "AND ufa.is_deleted = false",
            nativeQuery = true)
    boolean existsByEntityTypeIdAndEntityIdAndUserAccountIdAndActionIdAndIsDeletedFalse(
            @Param("entityTypeId") Integer entityTypeId,
            @Param("entityId") Long entityId,
            @Param("userAccountIds") List <Long> userAccountIds,
            @Param("actionId") Integer actionId);

    @Query(value = "SELECT entity_id FROM tse.user_features_access ufa " +
            "WHERE ufa.entity_type_id = :entityTypeId " +
            "AND ufa.user_account_id = :userAccountId " +
            "AND ufa.entity_id IN (:entityIds) " +
            "AND CAST(:actionId AS TEXT) = ANY(string_to_array(ufa.action_ids, ',')) " +
            "AND ufa.is_deleted = false",
            nativeQuery = true)
    List<Long> findAllMatchingEntityIds(
            @Param("entityTypeId") Integer entityTypeId,
            @Param("userAccountId") List<Long> userAccountId,
            @Param("entityIds") List<Long> entityIds,
            @Param("actionId") Integer actionId);

    @Query(value = "SELECT DISTINCT ufa.entity_id FROM tse.user_features_access ufa " +
            "WHERE ufa.entity_type_id = :entityTypeId " +
            "AND ufa.user_account_id IN (:accountIds) " +
            "AND CAST(:actionId AS TEXT) = ANY(string_to_array(ufa.action_ids, ',')) " +
            "AND ufa.is_deleted = :isDeleted",
            nativeQuery = true)
    List<Long> findDistinctEntityIdByEntityTypeIdAndAccountIdInAndActionIdAndIsDeleted(
            @Param("entityTypeId") Integer entityTypeId,
            @Param("accountIds") List<Long> accountIds,
            @Param("actionId") Integer actionId,
            @Param("isDeleted") Boolean isDeleted);

    @Query(value = "SELECT COUNT(*) > 0 FROM tse.user_features_access ufa " +
            "WHERE ufa.entity_type_id = :entityTypeId " +
            "AND ufa.entity_id IN (:entityIds) " +
            "AND ufa.user_account_id IN (:userAccountIds) " +
            "AND CAST(:actionId AS TEXT) = ANY(string_to_array(ufa.action_ids, ',')) " +
            "AND ufa.is_deleted = false",
            nativeQuery = true)
    boolean existsByEntityTypeIdAndEntityIdsAndUserAccountIdAndActionId(
            @Param("entityTypeId") Integer entityTypeId,
            @Param("entityIds") List<Long>entityIds,
            @Param("userAccountIds") List <Long> userAccountIds,
            @Param("actionId") Integer actionId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE tse.user_features_access SET is_deleted = true WHERE org_id = :orgId AND user_account_id = :accountId", nativeQuery = true)
    void updateIsDeletedByOrgIdAndAccountId(@Param("orgId") Long orgId,
                                            @Param("accountId") Long accountId);

    @Modifying
    @Transactional
    @Query("DELETE FROM UserFeaturesAccess ufa WHERE ufa.orgId = :orgId")
    void deleteByOrgId(Long orgId);
}
