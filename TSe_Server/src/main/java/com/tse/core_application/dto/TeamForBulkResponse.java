package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TeamForBulkResponse {
    private Long teamId;
    private String teamName;
    private String teamCode;
    private String message;
}
