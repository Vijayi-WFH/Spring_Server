package com.tse.core_application.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Data
public class GenerateTeamCodeRequest {

    @NotNull(message = "Team name is a required field")
    private String teamName;

    @NotNull(message = "Organization must be provided for initials")
    private Long orgId;

    private List<String> exclusions = new ArrayList<>();
}
