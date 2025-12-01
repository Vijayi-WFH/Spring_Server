package com.tse.core_application.service.Impl;

import com.tse.core_application.constants.Constants_Database_Static_Data_TaskHistoryColumnsMapping;
import com.tse.core_application.custom.model.TaskHistory_MappingIdAndCreatedBy;
import com.tse.core_application.model.Task;
import com.tse.core_application.model.TaskHistoryMetadata;
import com.tse.core_application.repository.TaskHistoryMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TaskHistoryMetadataService {

    @Autowired
    private TaskHistoryMetadataRepository taskHistoryMetadataRepository;

    public List<TaskHistoryMetadata> addTaskHistoryMetadata(List<String> allUpdatedFields, Task task) {
        List<TaskHistoryMetadata> taskHistoryMetadataToAdd = new ArrayList<>();
        for (String field : allUpdatedFields) {
            TaskHistory_MappingIdAndCreatedBy mappingId = Constants_Database_Static_Data_TaskHistoryColumnsMapping.getTaskHistoryColumnsMappingIdByColumnName(field);
            if (mappingId != null) {
                TaskHistoryMetadata taskHistoryMetadata = new TaskHistoryMetadata();
                taskHistoryMetadata.setTaskId(task.getTaskId());
                taskHistoryMetadata.setTaskHistoryColumnsMappingId(mappingId.getTaskHistoryColumnsMappingId());
                taskHistoryMetadata.setCreatedBy(mappingId.getCreatedBy());
                taskHistoryMetadata.setVersion(task.getVersion() + 1);
                taskHistoryMetadataToAdd.add(taskHistoryMetadata);
            }
        }
        return taskHistoryMetadataRepository.saveAll(taskHistoryMetadataToAdd);
    }

    public List<TaskHistoryMetadata> addTaskHistoryMetadataBySystemUpdate(List<String> allUpdatedFields, Task task) {
        List<TaskHistoryMetadata> taskHistoryMetadataToAdd = new ArrayList<>();
        for (String field : allUpdatedFields) {
            TaskHistory_MappingIdAndCreatedBy mappingId = Constants_Database_Static_Data_TaskHistoryColumnsMapping.getTaskHistoryColumnsMappingIdByColumnName(field);
            if (mappingId != null) {
                TaskHistoryMetadata taskHistoryMetadata = new TaskHistoryMetadata();
                taskHistoryMetadata.setTaskId(task.getTaskId());
                taskHistoryMetadata.setTaskHistoryColumnsMappingId(mappingId.getTaskHistoryColumnsMappingId());
                taskHistoryMetadata.setCreatedBy(mappingId.getCreatedBy());
                taskHistoryMetadata.setVersion(task.getVersion());
                taskHistoryMetadataToAdd.add(taskHistoryMetadata);
            }
        }
        return taskHistoryMetadataRepository.saveAll(taskHistoryMetadataToAdd);
    }

    public List<TaskHistoryMetadata> getAllTaskHistoryMetadataByMappingIdAndTaskId(Integer mappingId, Long taskId) {
        return taskHistoryMetadataRepository.findByTaskHistoryColumnsMappingIdAndTaskId(mappingId, taskId);

    }
}
