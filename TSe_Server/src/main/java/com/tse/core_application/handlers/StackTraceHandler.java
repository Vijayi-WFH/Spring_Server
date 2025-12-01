package com.tse.core_application.handlers;

public class StackTraceHandler {

    public static String getAllStackTraces(Throwable throwable) {
        StringBuilder allStackTrace = new StringBuilder();
        if(throwable != null) {
            StackTraceElement[] trace = throwable.getStackTrace();
            for (StackTraceElement stackTraceElement : trace) {
                allStackTrace.append("File Name: ").append(stackTraceElement.getFileName()).append(",     ").append("Class Name: ").append(stackTraceElement.getClassName()).append(",     ").append("Method Name: ").append(stackTraceElement.getMethodName()).append(",      ").append("Line: ").append(stackTraceElement.getLineNumber()).append("\r\n");
            }
        }
        return allStackTrace.toString();
    }
}
