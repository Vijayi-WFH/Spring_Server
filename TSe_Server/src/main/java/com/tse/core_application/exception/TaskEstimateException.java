package com.tse.core_application.exception;

public class TaskEstimateException extends RuntimeException{

    public TaskEstimateException(Integer systemGeneratedEstimate) {
        super("TaskEstimateException: In case you do not want to input any estimate, system generated estimate " + systemGeneratedEstimate +
                " will be taken");
    }
}
