package com.tse.core_application.service.Impl;

import com.tse.core_application.dto.TaskAttachmentHistoryResponse;
import com.tse.core_application.model.TaskAttachmentHistory;
import com.tse.core_application.model.UserAccount;
import com.tse.core_application.repository.TaskAttachmentHistoryRepository;
import com.tse.core_application.repository.UserAccountRepository;
import com.tse.core_application.utils.DateTimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TaskAttachmentHistoryService {
    @Autowired
    TaskAttachmentHistoryRepository taskAttachmentHistoryRepository;
    @Autowired
    UserAccountRepository userAccountRepository;

    public TaskAttachmentHistory addTaskAttachmentHistory (Long taskId, String fileName, Long modifiedBy, Boolean isFileAdded, Long version) {
        TaskAttachmentHistory taskAttachmentHistory = new TaskAttachmentHistory();
        taskAttachmentHistory.setTaskId(taskId);
        taskAttachmentHistory.setFileName(fileName);
        taskAttachmentHistory.setIsFileAdded(isFileAdded);
        taskAttachmentHistory.setVersion(version + 1);
        taskAttachmentHistory.setFkAccountIdLastUpdated(userAccountRepository.findByAccountId(modifiedBy));
        taskAttachmentHistory.setModifiedDate(LocalDateTime.now());

        return taskAttachmentHistoryRepository.save(taskAttachmentHistory);
    }

    public List<TaskAttachmentHistoryResponse> getTaskAttachmentHistory (Long taskId, String timeZone) {
        List<TaskAttachmentHistoryResponse> taskAttachmentHistoryResponseList = new ArrayList<>();
        List<Long> versionList = taskAttachmentHistoryRepository.findDistinctVersionsInDescendingOrder(taskId);

        for (Long version : versionList) {
            TaskAttachmentHistoryResponse taskAttachmentHistoryResponse = new TaskAttachmentHistoryResponse();
            List<TaskAttachmentHistory> taskAttachmentHistoryList = taskAttachmentHistoryRepository.findByTaskIdAndVersion(taskId, version);
            UserAccount userAccount = taskAttachmentHistoryList.get(0).getFkAccountIdLastUpdated();
            taskAttachmentHistoryResponse.setTaskId(taskId);
            taskAttachmentHistoryResponse.setModifiedOn(DateTimeUtils.convertServerDateToUserTimezone(taskAttachmentHistoryList.get(0).getModifiedDate(), timeZone));
            taskAttachmentHistoryResponse.setModifiedBy(userAccount.getFkUserId().getFirstName() + " " + userAccount.getFkUserId().getLastName());
            taskAttachmentHistoryResponse.setVersion(version);

            StringBuilder msg = new StringBuilder();
            for (TaskAttachmentHistory taskAttachmentHistory : taskAttachmentHistoryList) {
                if (msg.length() == 0) {
                    msg.append(taskAttachmentHistory.getFileName());
                } else {
                    msg.append(", ").append(taskAttachmentHistory.getFileName());
                }
            }
            if (taskAttachmentHistoryList.get(0).getIsFileAdded()) {
                taskAttachmentHistoryResponse.setIsFileAdded(true);
            }
            else {
                taskAttachmentHistoryResponse.setIsFileAdded(false);
            }
            taskAttachmentHistoryResponse.setMessage(msg.toString());
            taskAttachmentHistoryResponseList.add(taskAttachmentHistoryResponse);
        }
        return taskAttachmentHistoryResponseList;
    }
}
