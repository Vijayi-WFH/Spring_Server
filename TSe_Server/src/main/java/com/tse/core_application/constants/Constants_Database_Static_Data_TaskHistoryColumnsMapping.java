package com.tse.core_application.constants;

import com.tse.core_application.custom.model.TaskHistory_MappingIdAndCreatedBy;
import com.tse.core_application.model.TaskHistoryColumnsMapping;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class Constants_Database_Static_Data_TaskHistoryColumnsMapping {

    /**
     * static data of only column names.
     * Entity under use {@link TaskHistoryColumnsMapping }
     */
    private static final List<String> taskHistoryColumnNames = new ArrayList<>();

    /**
     * static data of all columns.
     * Entity under user {@link TaskHistoryColumnsMapping}
     */
    private static final List<TaskHistoryColumnsMapping> taskHistoryColumnsMappingList = new ArrayList<>();

    public void addTaskHistoryColumnNames(List<TaskHistoryColumnsMapping> taskHistoryColumnsMappings) {
        taskHistoryColumnsMappings.sort(Comparator.comparing(TaskHistoryColumnsMapping :: getTask_history_columns_mapping_id));
        taskHistoryColumnsMappingList.addAll(taskHistoryColumnsMappings);

        for (TaskHistoryColumnsMapping mapping : taskHistoryColumnsMappingList) {
            taskHistoryColumnNames.add(mapping.getColumnName());
        }
    }

    public List<String> getAllTaskHistoryColumnNames() {
        return taskHistoryColumnNames;
    }

    public static TaskHistory_MappingIdAndCreatedBy getTaskHistoryColumnsMappingIdByColumnName(String columnName) {
        HashMap<String, TaskHistory_MappingIdAndCreatedBy> map = new HashMap<>();

        for (TaskHistoryColumnsMapping mapping : taskHistoryColumnsMappingList) {
            TaskHistory_MappingIdAndCreatedBy taskHistory_mappingIdAndCreatedBy = new TaskHistory_MappingIdAndCreatedBy();
            taskHistory_mappingIdAndCreatedBy.setTaskHistoryColumnsMappingId(mapping.getTask_history_columns_mapping_id());
            taskHistory_mappingIdAndCreatedBy.setCreatedBy(mapping.getCreatedBy());
            map.put(mapping.getColumnName(), taskHistory_mappingIdAndCreatedBy);
        }
        return map.get(columnName);
        }
}
