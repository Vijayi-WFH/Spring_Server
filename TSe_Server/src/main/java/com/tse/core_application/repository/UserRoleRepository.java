package com.tse.core_application.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.tse.core_application.custom.model.RoleIdInUserRoleRepository;
import com.tse.core_application.model.UserRole;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long>{

	//  find UserRole from UserRole by given roleId and accountId
	public Optional<UserRole> findByRoleIdAndAccountId(Integer roleId, Long accountId);
	
	//  find userRoleId from UserRole, Role by roleName, accountId 
	@Query("select u.userRoleId from UserRole u inner join u.role r where r.roleName=:rolename and u.accountId=:accountid")
	public Optional<UserRole> getUserRoleByRoleNameAndAccountId(@Param("rolename") String roleName, @Param("accountid") Long accountId);

	//  find roleId from UserRole, UserAccount by userId
	@Query("select new com.tse.core_application.custom.model.RoleIdInUserRoleRepository( u.roleId ) from UserRole u inner join u.userAccount a where a.fkUserId.userId=:userId")
	public List<RoleIdInUserRoleRepository> getRoleIdByUserId(Long userId);

	public List<UserRole> findByAccountIdIn(List<Long> accountIds);

	Long findUserRoleIdByAccountId(Long accountId);

	@Query("select ur.roleId from UserRole ur where ur.accountId=:accountId")
	Integer findRoleIdByAccountId(Long accountId);

	@Modifying
	@Transactional
	@Query("DELETE FROM UserRole ur WHERE ur.accountId IN :accountIds")
	void deleteByAccountIdIn(List<Long> accountIds);
}
