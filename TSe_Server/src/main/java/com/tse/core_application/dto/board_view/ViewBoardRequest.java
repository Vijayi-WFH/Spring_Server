package com.tse.core_application.dto.board_view;

import com.tse.core_application.constants.ErrorConstant;
import com.tse.core_application.model.SortingField;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ViewBoardRequest {

    // accountIds for whom we want to view the board tasks
    private List<Long> accountIds;

    @NotNull(message= ErrorConstant.ORG_ID_ERROR)
    private Long orgId;

    private Long teamId;

    @NotNull(message = ErrorConstant.PROJECT_ID_ERROR)
    private Long projectId;

    private Long sprintId;

    private List<Long> labelIds;

    private Boolean currentTaskTimeSheetIndicator = false;

    HashMap<Integer, SortingField> sortingPriorityList;

    private Boolean isStarred;

    private List<Long> starredBy;

    public ViewBoardRequest(List<Long> accountIds,
                            Long orgId,
                            Long teamId,
                            Long projectId,
                            Long sprintId,
                            List<Long> labelIds,
                            Boolean currentTaskTimeSheetIndicator,
                            HashMap<Integer, SortingField> sortingPriorityList) {
        this.accountIds = accountIds;
        this.orgId = orgId;
        this.teamId = teamId;
        this.projectId = projectId;
        this.sprintId = sprintId;
        this.labelIds = labelIds;
        this.currentTaskTimeSheetIndicator = currentTaskTimeSheetIndicator;
        this.sortingPriorityList = sortingPriorityList;
    }
}