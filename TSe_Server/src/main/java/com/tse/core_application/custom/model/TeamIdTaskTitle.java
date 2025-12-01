package com.tse.core_application.custom.model;

import lombok.Value;

@Value
public class TeamIdTaskTitle {

	Long teamId;
	String taskTitle;

	public TeamIdTaskTitle(Long teamId, Object taskTitle) {
		this.teamId = teamId;
		this.taskTitle = (String) taskTitle;
	}
}
