package com.tse.core_application.model.github;

import com.tse.core_application.model.Constants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "github_account_and_repo_preference", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GithubAccountAndRepoPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "github_account_and_repo_preference_id", nullable = false)
    private Long githubAccountAndRepoPreferenceId;

    @Column(name = "github_account_user_name", nullable = false, length = 39)
    private String githubAccountUserName;

    @Column(name = "github_account_repo_name", nullable = false, length = 100)
    private String githubAccountRepoName;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @CreationTimestamp
    @Column(name = "created_date_time", nullable = false, updatable = false)
    private LocalDateTime createdDateTime;

    @UpdateTimestamp
    @Column(name = "updated_date_time", insertable = false)
    private LocalDateTime updatedDateTime;
}
