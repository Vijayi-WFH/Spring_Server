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
public class UpdateBoardRequest {

    /* in case of update of active-tasks, this will contain all accountIds of user
    in case of update from board view, this will contain the accountIds for which the tasks were requested in board view */
    private List<Long> accountIds;

    private Long orgId;

    private Long teamId;

    private Long sprintId;

    @NotNull(message = ErrorConstant.PROJECT_ID_ERROR)
    private Long projectId;

    private List<Long> labelIds;

    private Boolean currentTaskTimeSheetIndicator = false;

    List<BoardResponse> boardResponseList;

    HashMap<Integer, SortingField> sortingPriorityList;

}
