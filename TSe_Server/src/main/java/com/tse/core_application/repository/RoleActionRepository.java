package com.tse.core_application.repository;

import com.tse.core_application.custom.model.ActionId;
import com.tse.core_application.custom.model.CustomRoleAction;
import com.tse.core_application.model.RoleAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


import java.util.ArrayList;
import java.util.List;

@Repository
public interface RoleActionRepository extends JpaRepository<RoleAction, Integer> {

	//  find by roleId
	ArrayList<ActionId> findActionIdByRoleId(Integer roleId);

	//  find by actionId
	List<RoleAction> findByActionId(Integer actionId);

	@Query("select ra.actionId from RoleAction ra where ra.roleId in :roleIds")
	List<Integer> findActionIdsByRoleIdIn(List<Integer> roleIds);

	// find by roleId
	List<RoleAction> findByRoleId(Integer roleId);

	@Query("SELECT new com.tse.core_application.custom.model.CustomRoleAction(ra.roleId, ra.actionId) FROM RoleAction ra")
	List<CustomRoleAction> findAllRoleActionCustom();

}
