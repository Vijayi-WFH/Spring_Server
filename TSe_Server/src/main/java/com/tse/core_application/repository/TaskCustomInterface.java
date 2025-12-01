package com.tse.core_application.repository;

import com.tse.core_application.model.Task;

import java.util.List;

public interface TaskCustomInterface {
    boolean updateTask(Task task, List<String> fields);

}
