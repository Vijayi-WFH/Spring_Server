package com.tse.core_application.service.Impl;

import com.tse.core_application.dto.SprintHistoryResponse;
import com.tse.core_application.model.Constants;
import com.tse.core_application.model.Sprint;
import com.tse.core_application.model.SprintHistory;
import com.tse.core_application.model.UserAccount;
import com.tse.core_application.repository.AccessDomainRepository;
import com.tse.core_application.repository.SprintHistoryRepository;
import com.tse.core_application.repository.SprintRepository;
import com.tse.core_application.repository.UserAccountRepository;
import com.tse.core_application.utils.CommonUtils;
import com.tse.core_application.utils.DateTimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Service
public class SprintHistoryService {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private AccessDomainRepository accessDomainRepository;

    @Autowired
    private SprintHistoryRepository sprintHistoryRepository;

    @Autowired
    private SprintRepository sprintRepository;

    public List<SprintHistory> addSprintHistory (Sprint oldSprint, Sprint newSprint, String accountIds) {
        List<SprintHistory>  sprintHistoryList = new ArrayList<>();

        List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
        UserAccount updatedBy = userAccountRepository.findByAccountIdAndIsActive(accessDomainRepository.findAccountIdByEntityTypeIdAndEntityIdAndAccountIdInAndIsActive(newSprint.getEntityTypeId(), newSprint.getEntityId(), headerAccountIds, true), true);

        if (!Objects.equals(oldSprint.getSprintTitle(), newSprint.getSprintTitle())) {
            sprintHistoryList.add(new SprintHistory(newSprint.getSprintId(), Constants.SprintField.SPRINT_TITLE, oldSprint.getSprintTitle(), newSprint.getSprintTitle(), LocalDateTime.now(), updatedBy, newSprint.getVersion()));
        }

        if (!Objects.equals(oldSprint.getSprintObjective(), newSprint.getSprintObjective())) {
            sprintHistoryList.add(new SprintHistory(newSprint.getSprintId(), Constants.SprintField.SPRINT_OBJECTIVE, oldSprint.getSprintObjective(), newSprint.getSprintObjective(), LocalDateTime.now(), updatedBy, newSprint.getVersion()));
        }

        if (!Objects.equals(oldSprint.getSprintExpStartDate(), newSprint.getSprintExpStartDate())) {
            sprintHistoryList.add(new SprintHistory(newSprint.getSprintId(), Constants.SprintField.EXP_START_DATE, oldSprint.getSprintExpStartDate().toString(), newSprint.getSprintExpStartDate().toString(), LocalDateTime.now(), updatedBy, newSprint.getVersion()));
        }

        if (!Objects.equals(oldSprint.getSprintExpEndDate(), newSprint.getSprintExpEndDate())) {
            sprintHistoryList.add(new SprintHistory(newSprint.getSprintId(), Constants.SprintField.EXP_END_DATE, oldSprint.getSprintExpEndDate().toString(), newSprint.getSprintExpEndDate().toString(), LocalDateTime.now(), updatedBy, newSprint.getVersion()));
        }

        if (!Objects.equals(oldSprint.getCapacityAdjustmentDeadline(), newSprint.getCapacityAdjustmentDeadline())) {
            sprintHistoryList.add(new SprintHistory(newSprint.getSprintId(), Constants.SprintField.CAPACITY_ADJUSTMENT_DEADLINE, (oldSprint.getCapacityAdjustmentDeadline() != null ? oldSprint.getCapacityAdjustmentDeadline().toString() : null), (newSprint.getCapacityAdjustmentDeadline() != null ? newSprint.getCapacityAdjustmentDeadline().toString() : null), LocalDateTime.now(), updatedBy, newSprint.getVersion()));
        }

        if (!Objects.equals(oldSprint.getPreviousSprintId(), newSprint.getPreviousSprintId())) {
            sprintHistoryList.add(new SprintHistory(newSprint.getSprintId(), Constants.SprintField.PREVIOUS_SPRINT, (oldSprint.getPreviousSprintId() != null ? oldSprint.getPreviousSprintId().toString() : null), (newSprint.getPreviousSprintId() != null ? newSprint.getPreviousSprintId().toString() : null), LocalDateTime.now(), updatedBy, newSprint.getVersion()));
        }

        if (!Objects.equals(oldSprint.getNextSprintId(), newSprint.getNextSprintId())) {
            sprintHistoryList.add(new SprintHistory(newSprint.getSprintId(), Constants.SprintField.NEXT_SPRINT, (oldSprint.getNextSprintId() != null ? oldSprint.getNextSprintId().toString() : null), (newSprint.getNextSprintId() != null ? newSprint.getNextSprintId().toString() : null), LocalDateTime.now(), updatedBy, newSprint.getVersion()));
        }

        if (!Objects.equals(oldSprint.getCanModifyEstimates(), newSprint.getCanModifyEstimates())) {
            sprintHistoryList.add(new SprintHistory(newSprint.getSprintId(), Constants.SprintField.MODIFY_ESTIMATE, String.valueOf(oldSprint.getCanModifyEstimates()), String.valueOf(newSprint.getCanModifyEstimates()), LocalDateTime.now(), updatedBy, newSprint.getVersion()));
        }

        if (!Objects.equals(oldSprint.getCanModifyIndicatorStayActiveInStartedSprint(), newSprint.getCanModifyIndicatorStayActiveInStartedSprint())) {
            sprintHistoryList.add(new SprintHistory(newSprint.getSprintId(), Constants.SprintField.ACTIVE_INDICATOR, String.valueOf(oldSprint.getCanModifyIndicatorStayActiveInStartedSprint()), String.valueOf(newSprint.getCanModifyIndicatorStayActiveInStartedSprint()), LocalDateTime.now(), updatedBy, newSprint.getVersion()));
        }

        return sprintHistoryRepository.saveAll(sprintHistoryList);
    }

    public List<SprintHistoryResponse> getSprintHistory (Long sprintId, String timeZone) {
        List<SprintHistoryResponse> sprintHistoryResponseList = new ArrayList<>();
        List<Long> versionIdList = sprintHistoryRepository.findDistinctVersionsInDescendingOrder(sprintId);

        for (Long versionId : versionIdList) {

            List<SprintHistory> sprintHistoryList = sprintHistoryRepository.findBySprintIdAndVersion(sprintId, versionId);
            SprintHistoryResponse sprintHistoryResponse = new SprintHistoryResponse();
            HashMap<String, String> fields = new HashMap<>();
            HashMap<String, Object> oldValue = new HashMap<>();
            HashMap<String, Object> newValue = new HashMap<>();
            HashMap<String, Object> message = new HashMap<>();

            UserAccount userAccount = sprintHistoryList.get(0).getFkAccountIdLastUpdated();
            sprintHistoryResponse.setSprintId(sprintHistoryList.get(0).getSprintId());
            sprintHistoryResponse.setModifiedBy(userAccount.getFkUserId().getFirstName() + " " + userAccount.getFkUserId().getLastName());
            sprintHistoryResponse.setModifiedOn(DateTimeUtils.convertServerDateToUserTimezone(sprintHistoryList.get(0).getModifiedDate(), timeZone));
            sprintHistoryResponse.setVersion(versionId);
            for (SprintHistory sprintHistory : sprintHistoryList) {
                Object value1 = null;
                Object value2 = null;

                String fieldName = sprintHistory.getFieldName();
                String oldValueStr = sprintHistory.getOldValue();
                String newValueStr = sprintHistory.getNewValue();

                if (Objects.equals(Constants.SprintField.EXP_START_DATE, fieldName) || Objects.equals(Constants.SprintField.EXP_END_DATE, fieldName) || Objects.equals(Constants.SprintField.CAPACITY_ADJUSTMENT_DEADLINE, fieldName)) {
                    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm:ss a");
                    value1 = (oldValueStr != null ? dateTimeFormatter.format(DateTimeUtils.convertServerDateToUserTimezone(LocalDateTime.parse(oldValueStr), timeZone)) : "No value");
                    value2 = (newValueStr != null ? dateTimeFormatter.format(DateTimeUtils.convertServerDateToUserTimezone(LocalDateTime.parse(newValueStr), timeZone)) : "No value");
                }
                else if (Objects.equals(Constants.SprintField.NEXT_SPRINT, fieldName) || Objects.equals(Constants.SprintField.PREVIOUS_SPRINT, fieldName)) {
                    value1 = (oldValueStr != null ? sprintRepository.findBySprintId(Long.parseLong(oldValueStr)).getSprintTitle() : "None");
                    value2 = (newValueStr != null ? sprintRepository.findBySprintId(Long.parseLong(newValueStr)).getSprintTitle() : "None");
                }
                else if (Objects.equals(Constants.SprintField.MODIFY_ESTIMATE, fieldName) || Objects.equals(Constants.SprintField.ACTIVE_INDICATOR, fieldName)) {
                    value1 = Boolean.parseBoolean(oldValueStr);
                    value2 = Boolean.parseBoolean(newValueStr);
                }
                else {
                    value1 = oldValueStr;
                    value2 = newValueStr;
                }
                String formattedFieldName = com.tse.core_application.constants.Constants.SprintHistory_Column_Name.get(fieldName);
                String msg = sprintHistoryResponse.getModifiedBy() + " updated "  + formattedFieldName + " from " + value1 + " to " + value2;
                fields.put(fieldName, formattedFieldName);
                oldValue.put(fieldName, value1);
                newValue.put(fieldName, value2);
                message.put(fieldName, msg);
            }
            sprintHistoryResponse.setFieldName(fields);
            sprintHistoryResponse.setOldValue(oldValue);
            sprintHistoryResponse.setNewValue(newValue);
            sprintHistoryResponse.setMessage(message);
            sprintHistoryResponseList.add(sprintHistoryResponse);
        }
        return sprintHistoryResponseList;
    }
}
