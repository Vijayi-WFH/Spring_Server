package com.tse.core_application.repository;

import com.tse.core_application.model.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

    UserPreference findByUserId(Long userId);

    List<UserPreference> findByTeamId (Long teamId);

    List<UserPreference> findByProjectId(Long projectId);

    @Modifying
    @Transactional
    @Query("DELETE FROM UserPreference up WHERE up.orgId = :orgId")
    void deleteByOrgId(Long orgId);
}
