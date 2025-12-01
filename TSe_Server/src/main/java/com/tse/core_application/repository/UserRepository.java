package com.tse.core_application.repository;

import com.tse.core_application.custom.model.*;
import com.tse.core_application.dto.EmailFirstLastAccountIdIsActive;
import com.tse.core_application.dto.UserListResponse;
import com.tse.core_application.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
	
	User findByPrimaryEmail(String primaryEmail);

	//  to find the first and last name of the user by its userId
	UserName findFirstNameAndLastNameByUserId(Long userId);

	// to check if user exists by primary email
	boolean existsByPrimaryEmail(String primaryEmail);

	//  find primaryEmail, accountId, firstName, lastName by userId
	@Query("select new com.tse.core_application.dto.UserListResponse(a.email, a.accountId, u.firstName, u.middleName, u.lastName, a.isActive, a.isDisabledBySams, a.deactivatedByRole, a.deactivatedByAccountId) from UserAccount a inner join a.fkUserId u where a.orgId=:orgId")
	List<UserListResponse> getEmailAccountIdFirstMiddleAndLastNameByOrgId(Long orgId);

	// update timeZone by userId
	@Transactional
	@Modifying
	@Query("update User u set u.timeZone = :timeZone where u.userId = :userId")
	Integer updateTimeZoneByUserId(String timeZone, Long userId);


    User findByUserId(Long userId);

	List<User> findAllUserByManagingUserId(Long userId);

	@Query("select u.userId from User u where u.managingUserId = :userId")
	List<Long> findAllUserIdByManagingUserId(Long userId);

	@Query("select u.primaryEmail from User u where u.managingUserId = :userId")
	List<String> findAllUserPrimaryEmailByManagingUserId(Long userId);

	@Query("select u.firstName from User u where u.primaryEmail = :email")
	String findFirstNameByPrimaryEmail(String email);
}
