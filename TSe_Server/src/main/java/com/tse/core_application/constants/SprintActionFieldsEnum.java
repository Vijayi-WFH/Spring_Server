package com.tse.core_application.constants;

import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public enum SprintActionFieldsEnum {

    ADD_TASK(1, "ADD_TASK", List.of(12, 15, 132)),
    REMOVE_FROM_SPRINT(2, "REMOVE_FROM_SPRINT", List.of(12, 15, 132)),
    DELETE_WORK_ITEM(3, "DELETE_WORK_ITEM", List.of(12, 15, 132)),
    PRIORITY(4, "PRIORITY", List.of(3, 4, 6, 7, 8, 9, 10, 11, 12, 15, 132)),
    ASSIGNED_TO(5, "ASSIGNED_TO", List.of(3, 4, 6, 7, 8, 9, 10, 11, 12, 15, 132)),
    ESTIMATE_CHANGES(6, "ESTIMATE_CHANGES", List.of(3, 4, 6, 7, 8, 9, 10, 11, 12, 15, 132)),
    CHANGE_STATUS(7, "CHANGE_STATUS", List.of(1, 2, 4, 5, 6, 7, 8, 9, 10, 11, 12, 15, 132)),
    UPDATE_PROGRESS(8, "UPDATE_PROGRESS", List.of(1, 2, 4, 5, 6, 7, 8, 9, 10, 11, 12, 15, 132)),
    FLAGGING(9, "FLAGGING", null);

    // added Priority, Assigned_to & Estimate_Changes are essential fields, and their marking is a little bit different so
    // check it in fetchWorkItemFieldsForSprintByRoles() method.

    private final int fieldId;
    private final String fieldName;
    private final List<Integer> allowedRoleIds;

    private static final Map<Integer, SprintActionFieldsEnum> FIELDS_BY_ID = new HashMap<>();
    private static final Map<String, SprintActionFieldsEnum> FIELDS_BY_NAME = new HashMap<>();

    static {
        for (SprintActionFieldsEnum e : values()) {
            FIELDS_BY_ID.put(e.fieldId, e);
            FIELDS_BY_NAME.put(e.fieldName, e);
        }
    }

    SprintActionFieldsEnum(int fieldId, String fieldName, List<Integer> allowedRoleIds) {
        this.fieldId = fieldId;
        this.fieldName = fieldName;
        this.allowedRoleIds = allowedRoleIds;
    }

    static SprintActionFieldsEnum getById(int fieldId) {
        return FIELDS_BY_ID.get(fieldId);
    }

    static SprintActionFieldsEnum getFieldIdByName(String fieldName){
        return FIELDS_BY_NAME.get(fieldName);
    }
}
