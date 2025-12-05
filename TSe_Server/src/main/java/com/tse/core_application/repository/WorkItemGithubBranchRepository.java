package com.tse.core_application.repository;

import com.tse.core_application.model.github.WorkItemGithubBranch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface WorkItemGithubBranchRepository extends JpaRepository<WorkItemGithubBranch, Long> {
    List<WorkItemGithubBranch> findByWorkItemId(Long workItemId);

    @Modifying
    @Transactional
    @Query("DELETE FROM WorkItemGithubBranch wigb WHERE wigb.workItemId IN :taskIds")
    void deleteByTaskIdIn(List<Long> taskIds);
}
