package com.tse.core_application.repository;

import com.tse.core_application.model.github.GithubAccountAndRepoPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface GithubAccountAndRepoPreferenceRepository extends JpaRepository<GithubAccountAndRepoPreference, Long> {

    Optional<GithubAccountAndRepoPreference> findByOrgIdAndGithubAccountUserNameIgnoreCaseAndGithubAccountRepoNameIgnoreCase(Long orgId, String username, String repoName);

    List<GithubAccountAndRepoPreference> findByOrgIdAndIsActive(Long orgId, Boolean isActive);

    Optional<GithubAccountAndRepoPreference> findByOrgIdAndGithubAccountUserNameIgnoreCaseAndIsActive(Long orgId, String githubUserName, Boolean isActive);

    boolean existsByOrgIdAndIsActiveTrueAndGithubAccountUserNameIgnoreCaseIn(Long orgId, List<String> githubUserName);

    List<GithubAccountAndRepoPreference> findAllByOrgIdAndGithubAccountUserNameIgnoreCaseInAndIsActiveTrue(Long orgId, List<String> usernames);

    List<GithubAccountAndRepoPreference> findAllByOrgIdAndIsActiveTrue(Long orgId);

    @Modifying
    @Transactional
    @Query("DELETE FROM GithubAccountAndRepoPreference garp WHERE garp.githubAccountId IN :githubAccountIds")
    void deleteByGithubAccountIdIn(List<Long> githubAccountIds);
}
