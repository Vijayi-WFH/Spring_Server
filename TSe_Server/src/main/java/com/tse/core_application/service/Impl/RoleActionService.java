package com.tse.core_application.service.Impl;

import java.util.ArrayList;
import java.util.List;

import com.tse.core_application.custom.model.CustomAccessDomain;
import com.tse.core_application.custom.model.CustomRoleAction;
import com.tse.core_application.model.RoleAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tse.core_application.custom.model.ActionId;
import com.tse.core_application.repository.RoleActionRepository;

@Service
public class RoleActionService {

	@Autowired
	private RoleActionRepository roleActionRepository;

	//  to get actionId by roleId
	public ArrayList<ActionId> getActionIdByRoleId(Integer roleId) {
		ArrayList<ActionId> action = roleActionRepository.findActionIdByRoleId(roleId);
		return action;
	}

	public List<CustomRoleAction> getAllRoleActionsByAccessDomains(List<CustomAccessDomain> accessDomains) {
		List<CustomRoleAction> roleActions = new ArrayList<>();
		if (!accessDomains.isEmpty()) {
			for (CustomAccessDomain accessDomain: accessDomains) {
				List<RoleAction> roleActionList = roleActionRepository.findByRoleId(accessDomain.getRoleId());
				for (RoleAction roleAction: roleActionList) {
					CustomRoleAction customRoleActionToAdd = new CustomRoleAction();
					customRoleActionToAdd.setActionId(roleAction.getActionId());
					customRoleActionToAdd.setRoleId(roleAction.getRoleId());
					boolean isRoleActionListAdded =  roleActions.add(customRoleActionToAdd);
				}
			}
			return roleActions;
		} else {
			return roleActions;
		}
	}


}
