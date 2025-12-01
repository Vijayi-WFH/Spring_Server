package com.tse.core_application.repository;

import java.util.HashMap;

public interface TaskHistoryCustomInterface {
   void fetchTaskHistoryForTasksWithFieldChange(Long taskId, String field, HashMap<String, Object> filters,String orderBy);
}
