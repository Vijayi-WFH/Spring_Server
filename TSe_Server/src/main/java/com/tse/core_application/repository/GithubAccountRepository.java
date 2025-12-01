package com.tse.core_application.repository;

import com.tse.core_application.model.github.GithubAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GithubAccountRepository extends JpaRepository<GithubAccount, Long> {
    Optional<GithubAccount> findByFkUserIdUserIdAndOrgId(Long userId, Long orgId);

    @Query("SELECT g.orgId FROM GithubAccount g WHERE g.fkUserId.userId = :userId AND g.isLinked = :isLinked")
    List<Long> findOrgIdByFkUserIdUserIdAndIsValidAndIsLinked(Long userId, Boolean isLinked);
}
