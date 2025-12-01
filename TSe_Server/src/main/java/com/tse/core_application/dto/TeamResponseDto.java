package com.tse.core_application.dto;

import com.tse.core_application.model.Label;
import com.tse.core_application.model.Organization;
import com.tse.core_application.model.Project;
import com.tse.core_application.model.UserAccount;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeamResponseDto {

    private Long teamId;
    private String teamName;
    private String teamDesc;
    private Long parentTeamId;
    private String chatRoomName;
    private String teamCode;

    private LocalDateTime createdDateTime;
    private LocalDateTime lastUpdatedDateTime;

    private Project fkProjectId;
    private Organization fkOrgId;
    private UserAccount fkOwnerAccountId;

    private Boolean isDeleted;
    private Boolean isDisabled;
    private LocalDateTime deletedOn;
    private UserAccount fkDeletedByAccountId;
}
