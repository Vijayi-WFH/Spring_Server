package com.tse.core_application.repository;

import com.tse.core_application.model.JiraToTseTaskMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JiraToTseTaskMappingRepository extends JpaRepository<JiraToTseTaskMapping, Long> {

    @Query("SELECT DISTINCT jt.issueId FROM JiraToTseTaskMapping jt WHERE jt.teamId = :teamId")
    List<Long> findIssueIdByTeamId(Long teamId);
}
