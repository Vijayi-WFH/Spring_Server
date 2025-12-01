package com.tse.core_application.constants;

import com.tse.core_application.model.TaskHistoryColumnsMapping;
import com.tse.core_application.service.Impl.TaskHistoryColumnsMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Component
public final class Initialize_Database_Static_Data {

    @Autowired
    private TaskHistoryColumnsMappingService taskHistoryColumnsMappingService;

    @PostConstruct
    public void initializeTaskHistoryMappingColumnName() {
        List<TaskHistoryColumnsMapping> columnNamesFoundDb = new ArrayList<>();
        columnNamesFoundDb = taskHistoryColumnsMappingService.getAllTaskHistoryMappings();
        Constants_Database_Static_Data_TaskHistoryColumnsMapping constants_database_static_data = new Constants_Database_Static_Data_TaskHistoryColumnsMapping();
        constants_database_static_data.addTaskHistoryColumnNames(columnNamesFoundDb);
    }


}
