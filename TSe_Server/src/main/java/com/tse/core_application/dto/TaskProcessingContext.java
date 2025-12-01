package com.tse.core_application.dto;

import com.tse.core_application.model.Task;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TaskProcessingContext {
    private HashMap<Long, Boolean> isVisited = new HashMap<>();
    private HashMap<Long, Long> processedTasks = new HashMap<>();
    private HashMap<Long, Integer> taskEstimates = new HashMap<>();
    private HashMap<Long, LocalDateTime> taskEstimatesLastDate = new HashMap<>();
    private HashMap<Long, LocalDateTime> parentExpStartDate = new HashMap<>();
    private HashMap<Long, LocalDateTime> parentExpEndDate = new HashMap<>();
    private List<Task> sprint2TasksToProcess = new ArrayList<>();
    private List<Task> childTasksWithParentDependencies = new ArrayList<>();

    // Getters and setters for all fields
    public HashMap<Long, Boolean> getIsVisited() {
        return isVisited;
    }

    public void setIsVisited(HashMap<Long, Boolean> isVisited) {
        this.isVisited = isVisited;
    }

    public HashMap<Long, Long> getProcessedTasks() {
        return processedTasks;
    }

    public void setProcessedTasks(HashMap<Long, Long> processedTasks) {
        this.processedTasks = processedTasks;
    }

    public HashMap<Long, Integer> getTaskEstimates() {
        return taskEstimates;
    }

    public void setTaskEstimates(HashMap<Long, Integer> taskEstimates) {
        this.taskEstimates = taskEstimates;
    }

    public HashMap<Long, LocalDateTime> getTaskEstimatesLastDate() {
        return taskEstimatesLastDate;
    }

    public void setTaskEstimatesLastDate(HashMap<Long, LocalDateTime> taskEstimatesLastDate) {
        this.taskEstimatesLastDate = taskEstimatesLastDate;
    }

    public HashMap<Long, LocalDateTime> getParentExpStartDate() {
        return parentExpStartDate;
    }

    public void setParentExpStartDate(HashMap<Long, LocalDateTime> parentExpStartDate) {
        this.parentExpStartDate = parentExpStartDate;
    }

    public HashMap<Long, LocalDateTime> getParentExpEndDate() {
        return parentExpEndDate;
    }

    public void setParentExpEndDate(HashMap<Long, LocalDateTime> parentExpEndDate) {
        this.parentExpEndDate = parentExpEndDate;
    }

    public List<Task> getSprint2TasksToProcess() {
        return sprint2TasksToProcess;
    }

    public void setSprint2TasksToProcess(List<Task> sprint2TasksToProcess) {
        this.sprint2TasksToProcess = sprint2TasksToProcess;
    }

    public List<Task> getChildTasksWithParentDependencies() {
        return childTasksWithParentDependencies;
    }

    public void setChildTasksWithParentDependencies(List<Task> childTasksWithParentDependencies) {
        this.childTasksWithParentDependencies = childTasksWithParentDependencies;
    }
}


