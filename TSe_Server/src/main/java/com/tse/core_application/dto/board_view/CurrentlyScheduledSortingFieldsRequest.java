package com.tse.core_application.dto.board_view;

import com.tse.core_application.model.SortingField;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;

@Getter
@Setter
public class CurrentlyScheduledSortingFieldsRequest {
    HashMap<Integer, SortingField> sortingPriorityList;
}
