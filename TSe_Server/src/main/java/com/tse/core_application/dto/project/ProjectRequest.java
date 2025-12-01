package com.tse.core_application.dto.project;

import com.tse.core_application.constants.ErrorConstant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProjectRequest {

    @NotNull(message = ErrorConstant.Project.PROJECT_NAME)
    @Size(min = 3, max = 50, message = ErrorConstant.Project.PROJECT_NAME_LENGTH)
    private String projectName;

    @Size(max = 255, message = ErrorConstant.Project.PROJECT_DESC_LENGTH)
    private String projectDesc;

    @NotNull(message = ErrorConstant.Project.ORG_ID)
    private Long orgId;

    @NotNull(message = ErrorConstant.Project.BU_ID)
    private Long buId;

    List<AccessDomainRequest> accessDomains = new ArrayList<>();
}
