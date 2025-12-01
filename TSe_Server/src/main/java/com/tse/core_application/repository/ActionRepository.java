package com.tse.core_application.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.tse.core_application.custom.model.ActionId;
import com.tse.core_application.custom.model.ActionName;
import com.tse.core_application.model.Action;

import java.util.List;

@Repository
public interface ActionRepository extends JpaRepository<Action, Integer> {

	ActionName findActionNameByActionId(Integer actionId);

	@Query("select a.actionName from Action a where a.actionId in :actionIds")
	List<String> findActionNameByActionIdIn(List<Integer> actionIds);

	ActionId findActionIdByActionName(String actionname);

	@Query("select a from Action a where a.actionId in :actionId")
	Action findActionByActionId(Integer actionId);

}
