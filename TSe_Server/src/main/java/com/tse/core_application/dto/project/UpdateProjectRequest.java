package com.tse.core_application.dto.project;

import com.tse.core_application.constants.ErrorConstant;
import com.tse.core_application.custom.model.CustomAccessDomain;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class UpdateProjectRequest {

    @NotNull(message = ErrorConstant.Project.PROJECT_ID)
    private Long projectId;

    @Size(min = 3, max = 50, message = ErrorConstant.Project.PROJECT_NAME_LENGTH)
    private String projectName;

    @Size(min = 3, max = 255, message = ErrorConstant.Project.PROJECT_DESC_LENGTH)
    private String projectDesc;

    List<AccessDomainRequest> accessDomains = new ArrayList<>();
}
