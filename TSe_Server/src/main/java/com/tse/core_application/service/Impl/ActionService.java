package com.tse.core_application.service.Impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import com.tse.core_application.model.AccessDomain;
import com.tse.core_application.model.Constants;
import com.tse.core_application.repository.AccessDomainRepository;
import com.tse.core_application.repository.RoleActionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tse.core_application.custom.model.ActionId;
import com.tse.core_application.custom.model.ActionName;
import com.tse.core_application.repository.ActionRepository;

@Service
public class ActionService {

	@Autowired
	private AccessDomainService accessDomainService;

	@Autowired
	private ActionRepository actionRepository;

	@Autowired
	private RoleActionService roleActionService;
	@Autowired
	private AccessDomainRepository accessDomainRepository;
	@Autowired
	private RoleActionRepository roleActionRepository;

	ObjectMapper objectMapper = new ObjectMapper();

	// to confirm action by given actionName
	public boolean isInAction(ArrayList<Integer> roleids, String actionname) {
		ArrayList<Integer> arrayListActionIds = new ArrayList<Integer>();
		ActionId actionId1 = actionRepository.findActionIdByActionName(actionname);
		HashMap<String, Object> mapActionId1 = objectMapper.convertValue(actionId1, HashMap.class);
		Object value1 = mapActionId1.get("actionId");
		int intValue1 = (Integer) value1;
		for (Integer id : roleids) {
			List<ActionId> actionId2 = roleActionService.getActionIdByRoleId(id);
			for (ActionId actionId : actionId2) {
				HashMap<String, Object> mapActionId2 = objectMapper.convertValue(actionId, HashMap.class);
				Object value2 = mapActionId2.get("actionId");
				int intValue2 = (Integer) value2;
				arrayListActionIds.add(intValue2);
			}
		}
		boolean found = arrayListActionIds.contains(intValue1);
		return found;
	}

	// to find list of user's actionNames by its accountId
	public ArrayList<String> getUserActionList(long accountId, Long teamId) {

		ArrayList<Integer> listUserRoleIds = accessDomainService.getEffectiveRolesByAccountId(accountId, Constants.EntityTypes.TEAM, teamId);
		ArrayList<Integer> arrayListActionIds = new ArrayList<Integer>();
		ArrayList<String> arrayListUserActionNames = new ArrayList<String>();

		for (Integer listUserRoleId : listUserRoleIds) {
			ArrayList<ActionId> userActionId = roleActionService.getActionIdByRoleId(listUserRoleId);
			for (ActionId Id : userActionId) {
				HashMap<String, Object> mapUserActionId = objectMapper.convertValue(Id, HashMap.class);
				Object value1 = mapUserActionId.get("actionId");
				int value2 = (Integer) value1;
				arrayListActionIds.add(value2);
			}

		}

		for (Integer arrayListActionId : arrayListActionIds) {
			ActionName userActionName = actionRepository.findActionNameByActionId(arrayListActionId);
			HashMap<String, Object> mapUserActionName = objectMapper.convertValue(userActionName, HashMap.class);
			Object value1 = mapUserActionName.get("actionName");
			String stringValue1 = (String) value1;
			arrayListUserActionNames.add(stringValue1);
		}
		return arrayListUserActionNames;
	}

	public Boolean doesActionExistWithUser(Long accountId, Long teamId, Integer actionId) {
		List<AccessDomain> accessDomains = accessDomainRepository.findByEntityTypeIdAndEntityIdAndAccountIdAndIsActive(Constants.EntityTypes.TEAM, teamId, accountId, true);
		List<Integer> roleIds = accessDomains.stream().map(AccessDomain::getRoleId).collect(Collectors.toList());
		List<Integer> actionIds = roleActionRepository.findActionIdsByRoleIdIn(roleIds);
		return actionIds.contains(actionId);
	}

}
