package com.tse.core_application.service.Impl;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.custom.model.TaskHistoryMappingKeyColumnsDesc;
import com.tse.core_application.model.TaskHistoryColumnsMapping;
import com.tse.core_application.repository.TaskHistoryColumnsMappingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskHistoryColumnsMappingService {

    @Autowired
    private TaskHistoryColumnsMappingRepository taskHistoryColumnsMappingRepository;

    public List<TaskHistoryMappingKeyColumnsDesc> getAllActiveTaskHistoryMappingFieldsAndKeys() {
        return taskHistoryColumnsMappingRepository.getAllMappingKeysAndColumnDesc(Constants.TaskHistoryMapping_ActiveIndicator.TASK_HISTORY_MAPPING_KEY_ACTIVE);
    }

    public List<TaskHistoryColumnsMapping> getAllTaskHistoryMappings() {
        return taskHistoryColumnsMappingRepository.findAll();
    }

    public List<TaskHistoryColumnsMapping> getAllActiveColumnNameByTaskHistoryColumnMappingKey(List<Integer> mappingKeys) {
        return taskHistoryColumnsMappingRepository.findColumnNameByTaskHistoryColumnsMappingKeyInAndIsActive(mappingKeys, Constants.TaskHistoryMapping_ActiveIndicator.TASK_HISTORY_MAPPING_KEY_ACTIVE);
    }

    public List<TaskHistoryColumnsMapping> getTaskHistoryColumnsMappingByMappingKey(List<Integer> mappingKeys) {
        return taskHistoryColumnsMappingRepository.findByTaskHistoryColumnsMappingKeyIn(mappingKeys);
    }
}
