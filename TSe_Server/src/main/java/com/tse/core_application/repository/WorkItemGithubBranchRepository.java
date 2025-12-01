package com.tse.core_application.repository;

import com.tse.core_application.model.github.WorkItemGithubBranch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkItemGithubBranchRepository extends JpaRepository<WorkItemGithubBranch, Long> {
    List<WorkItemGithubBranch> findByWorkItemId(Long workItemId);
}
