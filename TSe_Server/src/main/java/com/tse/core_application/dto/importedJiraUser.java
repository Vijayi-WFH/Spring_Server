package com.tse.core_application.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class importedJiraUser {
    private List<JiraUsers> failureList = new ArrayList<>();
    private List<JiraUsers> successList = new ArrayList<>();
}
