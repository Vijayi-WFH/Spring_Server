package com.tse.core.repository.supplements;

import com.tse.core.custom.model.*;
import com.tse.core.dto.supplements.EmailFirstLastAccountIdIsActive;
import com.tse.core.model.supplements.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount,Long> {

////    List<UserAccount> findByUserId(Long userId);
//
//    List<UserAccount> findByFkUserIdUserId(Long userId);
//
////    List<UserAccount> findByUserIdAndOrgIdIn(Long userId,List<Long> orgIds);
//
//    List<UserAccount> findByFkUserIdUserIdAndOrgIdIn(Long userId,List<Long> orgIds);
//
//    //   find user's userId by accountId
////    UserId findUserIdByAccountId(Long accountId);
//
//    UserAccount findFkUserIdByAccountId(Long accountId);
//
//
//    List<AccountId> findAccountIdByOrgId(Long orgId);
//    //   find user's accountId by userId
////    AccountId findAccountIdByUserId(Long userId);

    List<AccountId> findDistinctAccountIdByFkUserIdUserIdIn(List<Long> userId);


//    //   find user by email
//    List<UserAccount> findByEmail(String email);
//
//    //  find by orgId
//
//    //  find userId by orgId
////    List<UserId> findUserIdByOrgId(Long orgId);
//
////    @Query("select new com.tse.core.custom.model.EmailFirstLastAccountId( a.email, a.accountId, u.firstName, u.lastName ) from UserAccount a inner join a.fkUserId u where a.accountId=:accountId")
////    EmailFirstLastAccountId getEmailFirstNameLastNameAccountIdByAccountId(Long accountId);
//
//    //  find by accountId
//    UserAccount findByAccountId(Long accountId);
//
//
////    UserAccount findByOrgIdAndUserId(Long orgId, Long userId);
//
//    UserAccount findByOrgIdAndFkUserIdUserId(Long orgId, Long userId);
//
//    UserAccount findByEmailAndOrgId(String email, Long orgId);
//
//    List<UserAccount> findByOrgId(Long orgId);
//
//    List<UserAccount> findByAccountIdIn(List<Long> accountIds);
//
    @Query("select ua.orgId from UserAccount ua where ua.accountId=:accountId AND ua.isActive = true")
    Long findOrgIdByAccountId(Long accountId);

    @Query("select ua.email from UserAccount ua where ua.accountId=:approverAccountId AND ua.isActive = true")
    String findEmailByAccountId(Long approverAccountId);

    UserAccount findByAccountId(Long accountId);
    UserAccount findByAccountIdAndIsActive(Long accountId, Boolean isActive);

    @Query("select new com.tse.core.dto.supplements.EmailFirstLastAccountIdIsActive( a.email, a.accountId, u.firstName, u.lastName, a.isActive ) from UserAccount a inner join a.fkUserId u where a.accountId = :accountId")
    EmailFirstLastAccountIdIsActive getEmailFirstNameLastNameAccountIdIsActiveByAccountId(Long accountId);

    List<AccountId> findAccountIdByOrgIdAndIsActive(Long orgId, Boolean isActive);
}
