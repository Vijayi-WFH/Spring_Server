package com.tse.core_application.service.Impl;

import com.tse.core_application.custom.model.ActionId;
import com.tse.core_application.custom.model.CustomAccessDomain;
import com.tse.core_application.exception.FileNameException;
import com.tse.core_application.exception.FileStorageException;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.Constants;
import com.tse.core_application.model.Task;
import com.tse.core_application.model.TaskMedia;
import com.tse.core_application.repository.TaskMediaRepository;
import com.tse.core_application.repository.TaskRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class TaskMediaService {

    private static final Logger logger = LogManager.getLogger(TaskMediaService.class.getName());

    @Autowired
    private TaskMediaRepository taskMediaRepository;

    @Autowired
    private AccessDomainService accessDomainService;

    @Autowired
    private RoleActionService roleActionService;

    @Autowired
    private TaskService taskService;
    @Autowired
    private TaskRepository taskRepository;

    public TaskMedia storeFile(MultipartFile file, Long taskId, Long accountId, String timeZone) {
        Task foundTaskDb = taskRepository.findByTaskId(taskId);
        if (foundTaskDb != null) {
            if (!validateSingleHeaderAccountIdForTask(accountId, foundTaskDb)) {
                String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("Header AccountId is not validated"));
                logger.error("Header accountId is not validated. " + " ,    " + "accountId = " + accountId + " ,    " + "taskNumber = " + foundTaskDb.getTaskNumber(), new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw new ValidationFailedException("Header AccountId is not validated");
            }
        } else {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("No Work Item Exists"));
            logger.error("taskNumber = " + foundTaskDb.getTaskNumber() + " not found. ", new Throwable(allStackTraces));
            ThreadContext.clearMap();
            throw new ValidationFailedException("No Task Exists");
        }
        boolean isFileNameValidated = validateFileName(file);
        try {
            TaskMedia taskMediaDbFound = taskMediaRepository.findTaskMediaByTaskId(foundTaskDb.getTaskId());
            TaskMedia addedTaskMedia = null;

            if (taskMediaDbFound != null) {
                taskMediaDbFound.setAccountId(accountId);
                taskMediaDbFound.setMedia(file.getBytes());
                taskMediaDbFound.setFileName(file.getOriginalFilename());
                taskMediaDbFound.setFileType(file.getContentType());
                addedTaskMedia = taskMediaRepository.save(taskMediaDbFound);
            } else {
                TaskMedia taskMediaToAdd = new TaskMedia();
                taskMediaToAdd.setAccountId(accountId);
                taskMediaToAdd.setOrgId(foundTaskDb.getFkOrgId().getOrgId());
                taskMediaToAdd.setTaskId(foundTaskDb.getTaskId());
                taskMediaToAdd.setTaskNumber(foundTaskDb.getTaskNumber());
                taskMediaToAdd.setFileName(file.getOriginalFilename());
                taskMediaToAdd.setFileType(file.getContentType());
                taskMediaToAdd.setMedia(file.getBytes());
                addedTaskMedia = taskMediaRepository.save(taskMediaToAdd);
            }
            return addedTaskMedia;
        } catch (IOException e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(new FileStorageException());
            logger.error("Something went wrong: Not able to store the file for taskNumber = " + foundTaskDb.getTaskNumber(), new Throwable(allStackTraces));
            ThreadContext.clearMap();
            throw new FileStorageException();
        }
    }

    public boolean validateSingleHeaderAccountIdForTask(Long accountId, Task task) {
        boolean isHeaderAccountIdValidated = false;
        if (task != null) {
            List<CustomAccessDomain> foundAccessDomainsDb = accessDomainService.getAccessDomainByAccountIdAndEntityId(accountId, task.getFkTeamId().getTeamId());
            for (CustomAccessDomain accessDomain : foundAccessDomainsDb) {
                ArrayList<Integer> actionIds = new ArrayList<>();
                ArrayList<ActionId> actionIdsForRoleId = roleActionService.getActionIdByRoleId(accessDomain.getRoleId());

                for (ActionId actionId : actionIdsForRoleId) {
                    actionIds.add(actionId.getActionId());
                }

                if (actionIds.contains(Constants.ActionId.TEAM_TASK_VIEW)) {
                    isHeaderAccountIdValidated = true;
                } else {
                    if (actionIds.contains(Constants.ActionId.TASK_BASIC_UPDATE)) {
                        if (task.getFkAccountIdAssigned() != null) {
                            if (Objects.equals(accountId, task.getFkAccountIdAssigned().getAccountId())) {
                                isHeaderAccountIdValidated = true;
                            }
                        }
                    }
                }
            }
        }
        return isHeaderAccountIdValidated;

    }

    public boolean validateFileName(MultipartFile file) {
        boolean isFileNameValidated = true;
        String validateFileNameUsingRegexPattern = "^[A-Za-z0-9._-]{1,255}$";
        if (file.getOriginalFilename() != null && !file.getOriginalFilename().isEmpty()) {
            if (!file.getOriginalFilename().matches(validateFileNameUsingRegexPattern)) {
                String allStackTraces = StackTraceHandler.getAllStackTraces(new FileNameException());
                logger.error("Illegal file name = " + file.getOriginalFilename(), new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw new FileNameException();
            }
        } else {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new FileNameException());
            logger.error("Illegal file name = " + file.getOriginalFilename(), new Throwable(allStackTraces));
            ThreadContext.clearMap();
            throw new FileNameException();
        }
        return isFileNameValidated;
    }

    public TaskMedia getFile(Long taskId) {
        TaskMedia taskMedia = taskMediaRepository.findTaskMediaByTaskId(taskId);
        return taskMedia;
    }

    public boolean validateMultipleHeaderAccountIdsForTask(String accountIdStr, Task task) {
        boolean isHeaderAccountIdValidated = false;
        String[] allAccountIds = accountIdStr.split(",");
        List<Long> accountIds = new ArrayList<>();
        for (String accountId : allAccountIds) {
            accountIds.add(Long.valueOf(accountId));
        }

        if (task != null) {
            // Todo: add isActive condition
            List<CustomAccessDomain> foundAccessDomainsDb = accessDomainService.getAccessDomainsByAccountIdsAndEntityId(task.getFkTeamId().getTeamId(), accountIds);
            for (CustomAccessDomain accessDomain : foundAccessDomainsDb) {
                ArrayList<Integer> actionIds = new ArrayList<>();
                ArrayList<ActionId> actionIdsForRoleId = roleActionService.getActionIdByRoleId(accessDomain.getRoleId());

                for (ActionId actionId : actionIdsForRoleId) {
                    actionIds.add(actionId.getActionId());
                }

                if (actionIds.contains(Constants.ActionId.TEAM_TASK_VIEW)) {
                    isHeaderAccountIdValidated = true;
                } else {
                    if (actionIds.contains(Constants.ActionId.TASK_BASIC_UPDATE)) {
                        if (task.getFkAccountIdAssigned() != null) {
                            if (accountIds.contains(task.getFkAccountIdAssigned().getAccountId())) {
                                isHeaderAccountIdValidated = true;
                            }
                        }
                    }
                }
            }
        }
        return isHeaderAccountIdValidated;
    }

}
