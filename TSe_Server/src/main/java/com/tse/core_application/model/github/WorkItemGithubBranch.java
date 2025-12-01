package com.tse.core_application.model.github;

import com.tse.core_application.model.Constants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "work_item_github_branch", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkItemGithubBranch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_item_github_branch_id", nullable = false)
    private Long workItemGithubBranchId;

    @Column(name = "work_item_id", nullable = false)
    private Long workItemId;

    @Column(name = "branch_name", nullable = false)
    private String branchName;

    @Column(name = "repository_id", nullable = false)
    private String repoId;

    @Column(name = "base_branch_name", nullable = false)
    private String baseBranchName;

    @Column(name = "branch_link", nullable = false)
    private String branchLink;

    @Column(name = "last_commit_hash", nullable = false)
    private String lastCommitHash;

    @CreationTimestamp
    @Column(name = "created_date_time", updatable = false, nullable = false)
    private LocalDateTime createdDateTime;
}
