package com.tse.core_application.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tse.core_application.config.DebugConfig;
import com.tse.core_application.constants.Constants;
import com.tse.core_application.constants.PriorityEnum;
import com.tse.core_application.custom.model.TaskHistory_FieldMapping_Response;
import com.tse.core_application.dto.TaskAttachmentHistoryResponse;
import com.tse.core_application.model.*;
import com.tse.core_application.repository.*;
import com.tse.core_application.utils.CommonUtils;
import com.tse.core_application.utils.DateTimeUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TaskHistoryService {
    @Autowired
    private TaskHistoryRepository taskHistoryRepository;

    @Autowired
    private TaskService taskService;

    @Autowired
    private ObjectMapper objectMapperInstance;

    @Autowired
    private TaskHistoryColumnsMappingService taskHistoryColumnsMappingService;

    @Autowired
    private TaskHistoryMetadataService taskHistoryMetadataService;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private SprintRepository sprintRepository;
    @Autowired
    private SeverityRepository severityRepository;
    @Autowired
    private EnvironmentRepository environmentRepository;
    @Autowired
    private ResolutionRepository resolutionRepository;
    @Autowired
    private DependencyRepository dependencyRepository;
    @Autowired
    private EpicRepository epicRepository;
    @Autowired
    private TaskAttachmentHistoryService taskAttachmentHistoryService;
    @Autowired
    private TaskServiceImpl taskServiceImpl;
    @Autowired
    private MeetingRepository meetingRepository;

//    private DateTimeUtils dateTimeUtils;


    public List<TaskHistory> getTaskHistoriesForFieldChange(String fieldName, List<TaskHistory> taskHistories) {
        List<TaskHistory> resultTaskHistories = new ArrayList<>();
        //filter changed fields from taskHirtories provided
        if (taskHistories != null && taskHistories.size() > 0) {
            //INFO: use of java reflection to get field without getter.
            Object initialVal = null;
            for (TaskHistory taskHistory : taskHistories) {
                try {
                    Field field = TaskHistory.class.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object fieldVal = field.get(taskHistory);
                    if (fieldVal != null && !fieldVal.equals(initialVal)) {
                        resultTaskHistories.add(taskHistory);
                        initialVal = fieldVal;
                    }
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    if (DebugConfig.getInstance().isDebug()) {
                        System.out.println("Exception while fetching field of taskHistory" + e);
                    }
                }
            }
            initialVal = null;

        }
        //if taskHistoriws is not provided then fetch different from db
        else {

        }
        return resultTaskHistories;

    }


    public TaskHistory getLastTaskHistoryForFieldChangeWithValue(String fieldName, List<TaskHistory> taskHistories, Object value) {
        TaskHistory result = null;
        List<TaskHistory> allTaskWithFieldChange = this.getTaskHistoriesForFieldChange(fieldName, taskHistories);
        if (allTaskWithFieldChange != null && allTaskWithFieldChange.size() > 0) {
            for (TaskHistory taskHistory : allTaskWithFieldChange) {
                //Java reflection to get field value
                try {
                    Field field = TaskHistory.class.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object fieldVal = field.get(taskHistory);
                    if (fieldVal.equals(value)) {
                        result = taskHistory;
                    }
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    if (DebugConfig.getInstance().isDebug()) {
                        System.out.println("Exception while fetching field of taskHistory" + e);
                    }
                }
            }

        }
        return result;
    }

    @Deprecated(since = "2023-02-02")
//    public List<TaskHistory_FieldMapping_Response> getAllTaskHistoryByFieldMappings(String fieldMappingsStr, Long taskNumber, String timeZone) {
//        fieldMappingsStr = fieldMappingsStr.equalsIgnoreCase("0") ? "1,2,3,4,5,6,7,8,9" : fieldMappingsStr;
//        String[] fieldMapping = fieldMappingsStr.split(",");
//        List<String> fields = new ArrayList<>();
//        List<TaskHistory_FieldMapping_Response> taskHistoryResponse = new ArrayList<>();
//
//        for (String mapping : fieldMapping) {
//            Object obj = Constants.TaskHistory_Fields_Mapping.get(mapping);
//            if (obj instanceof List) {
//                List<String> list = (List<String>) obj;
//                fields.addAll(list);
//            } else {
//                if (obj instanceof String)
//                    fields.add((String) obj);
//            }
//        }
//
//        List<TaskHistory> allHistoryFoundDb = getAllTaskHistoryByTaskNumber(taskNumber);
//
//        Task taskFoundDb = taskService.getTaskByTaskNumber(taskNumber, timeZone);
//        HashMap<String, Object> mapTask = objectMapper.convertValue(taskFoundDb, HashMap.class);
//
//        ArrayList<HashMap<String, Object>> arrayList = new ArrayList<HashMap<String, Object>>();
//
//        for (TaskHistory taskHistory : allHistoryFoundDb) {
//            HashMap<String, Object> mapHistory = objectMapper.convertValue(taskHistory, HashMap.class);
//            arrayList.add(mapHistory);
//        }
//        arrayList.add(mapTask);
//
//        for (int i = 0; i < (arrayList.size() - 1); i++) {
//
//            HashMap<String, Object> oldValue = new HashMap<>();
//            HashMap<String, Object> newValue = new HashMap<>();
//            HashMap<String, Object> message = new HashMap<>();
//
//            TaskHistory_FieldMapping_Response historyResponse = new TaskHistory_FieldMapping_Response();
//
//            for (String field : fields) {
//                Object value1 = arrayList.get(i).get(field);
//                Object value2 = arrayList.get(i + 1).get(field);
//
//                if (!Objects.equals(value1, value2)) {
//
//                    if (field.equalsIgnoreCase("fkWorkflowTaskStatus")) {
//                        WorkFlowTaskStatus workFlowTaskStatus1 = objectMapper.convertValue(value1, WorkFlowTaskStatus.class);
//                        value1 = value1 != null ? workFlowTaskStatus1.getWorkflowTaskStatus() : "unassigned";
//                        WorkFlowTaskStatus workFlowTaskStatus2 = objectMapper.convertValue(value2, WorkFlowTaskStatus.class);
//                        value2 = value2 != null ? workFlowTaskStatus2.getWorkflowTaskStatus() : "unassigned";
//                    }
//                    if (field.equalsIgnoreCase("fkAccountIdMentor1") || field.equalsIgnoreCase("fkAccountIdMentor2") ||
//                            field.equalsIgnoreCase("fkAccountIdObserver1") || field.equalsIgnoreCase("fkAccountIdObserver2")) {
//                        UserAccount userAccount1 = objectMapper.convertValue(value1, UserAccount.class);
//                        value1 = value1 != null ? userAccount1.getFkUserId().getFirstName() + " " + userAccount1.getFkUserId().getLastName() : "unassigned";
//                        UserAccount userAccount2 = objectMapper.convertValue(value2, UserAccount.class);
//                        value2 = value2 != null ? userAccount2.getFkUserId().getFirstName() + " " + userAccount2.getFkUserId().getLastName() : "unassigned";
//                    }
//                    if (field.equalsIgnoreCase("taskExpStartDate") || field.equalsIgnoreCase("taskActStDate") ||
//                            field.equalsIgnoreCase("taskExpEndDate") || field.equalsIgnoreCase("taskActEndDate")) {
//                        LocalDateTime localDateTime1 = objectMapper.convertValue(value1, LocalDateTime.class);
//                        LocalDateTime localDateTime2 = objectMapper.convertValue(value2, LocalDateTime.class);
//                        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm:ss a");
//                        value1 = localDateTime1 != null ? dateTimeFormatter.format(localDateTime1) : "unassigned";
//                        value2 = localDateTime2 != null ? dateTimeFormatter.format(localDateTime2) : "unassigned";
//                    }
//
//                    historyResponse.setTaskNumber(taskNumber);
//                    UserAccount userAccount = objectMapper.convertValue(arrayList.get(i).get("fkAccountIdLastUpdated"), UserAccount.class);
//                    historyResponse.setModifiedBy(userAccount.getFkUserId().getFirstName() + " " + userAccount.getFkUserId().getLastName());
//                    historyResponse.setModifiedOn(objectMapper.convertValue(arrayList.get(i).get("createdDateTime"), LocalDateTime.class));
//
//                    oldValue.put(field, value1);
//                    newValue.put(field, value2);
//                    String msg;
//                    if (field.equals("taskProgressSystem")) {
//                        msg = arrayList.get(i).get("taskHistoryCreatedBy").equals(Constants.TaskUpdatedByIndicator_TaskHistoryTable.TASK_UPDATED_BY_SYSTEM) ? "Application" : historyResponse.getModifiedBy()
//                                + " has changed the " + field + " from " + (value1 != null ? value1 : "not calculated") + " to " + value2;
//                    } else {
//                        msg = historyResponse.getModifiedBy() + " has changed the " + field + " from " + (value1 != null ? value1 : "unassigned") +
//                                " to " + (value2 != null ? value2 : "unassigned");
//                    }
//                    message.put(field, msg);
//                    historyResponse.setOldValue(oldValue);
//                    historyResponse.setNewValue(newValue);
//                    historyResponse.setMessage(message);
//                }
//            }
//            historyResponse.setVersion((Long) arrayList.get(i + 1).get("version"));
//            if (historyResponse.getFieldName() != null && !historyResponse.getFieldName().isEmpty())
//                taskHistoryResponse.add(historyResponse);
//
//        }
//        return taskHistoryResponse;
//
//    }

    @SuppressWarnings("unchecked")
    private boolean areValuesEquals(Object value1, Object value2) {
        value1 = normalizeValue(value1);
        value2 = normalizeValue(value2);

        if (value1 instanceof List && value2 instanceof List) {
            List<?> list1 = new ArrayList<>((List<?>) value1);
            List<?> list2 = new ArrayList<>((List<?>) value2);

            if (list1.isEmpty() && list2.isEmpty()) {
                return true;
            }
            if (!list1.isEmpty() && list1.get(0) instanceof Comparable
                    && !list2.isEmpty() && list2.get(0) instanceof Comparable) {
                List<Comparable> cList1 = (List<Comparable>) list1;
                List<Comparable> cList2 = (List<Comparable>) list2;

                Collections.sort(cList1);
                Collections.sort(cList2);

                return cList1.equals(cList2);
            }

            return list1.equals(list2);
        }
        return Objects.equals(value1, value2);
    }

    /**
     * Normalizes values so that null, 0, empty string, and empty list are treated as equal.
     */
    private Object normalizeValue(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof String) {
            String str = ((String) value).trim();
            return str.isEmpty() ? null : str;
        }

        if (value instanceof Number) {
            return ((Number) value).longValue() == 0L ? null : value;
        }

        if (value instanceof List) {
            return ((List<?>) value).isEmpty() ? null : value;
        }
        return value;
    }

    public List<TaskHistory_FieldMapping_Response> getAllTaskHistoryByFieldMapping(String fieldMappingsStr, Long taskId, String timeZone) {
        Task taskFoundDb = taskRepository.findByTaskId(taskId);
        String taskNumber = taskFoundDb.getTaskNumber();

        fieldMappingsStr = fieldMappingsStr.equalsIgnoreCase("0") ? com.tse.core_application.model.Constants.ALL_FIELDS_MAPPING_STR : fieldMappingsStr;
        String[] fieldMapping = fieldMappingsStr.split(",");
        List<Integer> fieldMappingsToFind = new ArrayList<>();
        List<TaskHistory_FieldMapping_Response> taskHistoryResponse = new ArrayList<>();

        for (String field : fieldMapping) {
            //Todo This is a temporary fix for 8249
            if(Integer.valueOf(field)!=5)
                fieldMappingsToFind.add(Integer.valueOf(field));
        }

//        List<TaskHistoryColumnsMapping> allColumnMappingFoundDb = taskHistoryColumnsMappingService.getAllActiveColumnNameByTaskHistoryColumnMappingKey(fieldMappingsToFind);
        List<TaskHistoryColumnsMapping> allColumnMappingFoundDb = taskHistoryColumnsMappingService.getTaskHistoryColumnsMappingByMappingKey(fieldMappingsToFind);
        List<String> fields = new ArrayList<>();

//        Task taskFoundDb = taskService.getTaskByTaskNumber(taskNumber, timeZone);
        List<String> taskLabels = new ArrayList<>();
        if (taskFoundDb.getLabels() != null && !taskFoundDb.getLabels().isEmpty()) {
            taskLabels = taskFoundDb.getLabels().stream()
                    .map(Label::getLabelName)
                    .collect(Collectors.toList());
        }
        taskFoundDb.setTaskLabels(taskLabels);

        ObjectMapper objectMapper = CommonUtils.configureObjectMapper(objectMapperInstance);
        HashMap<String, Object> mapTask = objectMapper.convertValue(taskFoundDb, HashMap.class);
        mapTask.put("taskLabels", taskFoundDb.getTaskLabels());

//        List<Long> taskHistoryVersionToFind = new ArrayList<>();
        Set<Long> taskHistoryVersionToFindSet = new HashSet<>();

        for (TaskHistoryColumnsMapping mapping : allColumnMappingFoundDb) {
            if(mapping.getIsActive() == 1) {
                fields.add(mapping.getColumnName());
            }
            List<TaskHistoryMetadata> allMetadataFoundDb = taskHistoryMetadataService.getAllTaskHistoryMetadataByMappingIdAndTaskId(mapping.getTask_history_columns_mapping_id(), taskFoundDb.getTaskId());

            for (TaskHistoryMetadata metadata : allMetadataFoundDb) {
                taskHistoryVersionToFindSet.add(metadata.getVersion());
                taskHistoryVersionToFindSet.add(metadata.getVersion() - 1);
            }
        }
        List<Long> taskHistoryVersionToFind = new ArrayList<>(taskHistoryVersionToFindSet);
        List<TaskHistory> allHistoryFoundDb = getAllTaskHistoryByTaskIdAndVersion(taskId, new ArrayList<>(taskHistoryVersionToFind));

        ArrayList<HashMap<String, Object>> arrayList = new ArrayList<HashMap<String, Object>>();

        for (TaskHistory taskHistory : allHistoryFoundDb) {
            HashMap<String, Object> mapHistory = objectMapper.convertValue(taskHistory, HashMap.class);
            arrayList.add(mapHistory);
        }
        if (getMaxVersionInArrayList(taskHistoryVersionToFind) != null) {
            TaskHistory taskHistoryFound = taskHistoryRepository.findByTaskIdAndVersion(taskId, getMaxVersionInArrayList(taskHistoryVersionToFind) + 1);
            if (taskHistoryFound != null) {
                HashMap<String, Object> map = objectMapper.convertValue(taskHistoryFound, HashMap.class);
                arrayList.add(map);
            }
        }
        arrayList.add(mapTask);
        // haspmap of taskId to String(contains taskNumber and taskTitle)
        HashMap<Long, String> childTaskInfo = new HashMap<>();

        long countOfVersion = 0L;
        for (int i = 0; i < (arrayList.size() - 1); i++) {

            HashMap<String, String> fieldName = new HashMap<>();
            HashMap<String, Object> oldValue = new HashMap<>();
            HashMap<String, Object> newValue = new HashMap<>();
            HashMap<String, Object> message = new HashMap<>();

            TaskHistory_FieldMapping_Response historyResponse = new TaskHistory_FieldMapping_Response();
            TaskHistory_FieldMapping_Response taskProgressSystemHistoryResponse = null;

            for (String field : fields) {
                Object value1 = arrayList.get(i).get(field);
                Object value2 = arrayList.get(i + 1).get(field);

                if (!areValuesEquals(value1, value2)) {

                    if(field.equalsIgnoreCase("taskProgressSystem")) {
                        HashMap<String, Object> values = new HashMap<>();
                        values.put("oldValue", newTaskProgressSystemEnum(value1));
                        values.put("newValue", newTaskProgressSystemEnum(value2));
                        values.put("taskNumber", taskNumber);
                        values.put("modifiedOn", DateTimeUtils.convertServerDateToUserTimezone(objectMapper.convertValue(arrayList.get(i).get("createdDateTime"), LocalDateTime.class), timeZone));
                        values.put("countOfVersion", countOfVersion);
                        values.put("version", arrayList.get(i + 1).get("version"));
                        taskProgressSystemHistoryResponse = createHistoryResponseForTaskProgressSystem(values);
                        continue;
                    }
                    if (field.equalsIgnoreCase("fkWorkflowTaskStatus")) {
                        WorkFlowTaskStatus workFlowTaskStatus1 = objectMapper.convertValue(value1, WorkFlowTaskStatus.class);
                        WorkFlowTaskStatus workFlowTaskStatus2 = objectMapper.convertValue(value2, WorkFlowTaskStatus.class);
                        value1 = value1 != null ? workFlowTaskStatus1.getWorkflowTaskStatus() : "No Value";
                        value2 = value2 != null ? workFlowTaskStatus2.getWorkflowTaskStatus() : "No Value";

                        //appending reason in workflow status type
                        if (Objects.equals(workFlowTaskStatus1.getWorkflowTaskStatus(),com.tse.core_application.model.Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED_TITLE_CASE)) {
                            value1 = value1 + (taskFoundDb.getBlockedReason() != null ?  (" with reason : " + taskFoundDb.getBlockedReason()) : " with no reason ");
                        }
                        if (Objects.equals(workFlowTaskStatus2.getWorkflowTaskStatus(),com.tse.core_application.model.Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED_TITLE_CASE)) {
                            value2 = value2 + (taskFoundDb.getBlockedReason() != null ?  (" with reason : " + taskFoundDb.getBlockedReason()) : " with no reason ");
                        }
                    }

                    //adding recorded task effort
                    if (field.equalsIgnoreCase("recordedTaskEffort")) {
                        Integer recordedTaskEffort1 = (Integer) value1;
                        Integer recordedTaskEffort2 = (Integer) value2;

                        // Format the values
                        String formattedValue1 = formatEffort(recordedTaskEffort1);
                        String formattedValue2 = formatEffort(recordedTaskEffort2);

                        // Update values
                        value1 = formattedValue1;
                        value2 = formattedValue2;
                    }

                    //adding total meeting efforts
                    if (field.equalsIgnoreCase("totalMeetingEffort")) {
                        Integer totalMeetingEffort1 = (Integer) value1;
                        Integer totalMeetingEffort2 = (Integer) value2;

                        String formattedValue1 = formatEffort(totalMeetingEffort1);
                        String formattedValue2 = formatEffort(totalMeetingEffort2);
                        // Add meeting preference if available
                        Object preferenceIdObj = arrayList.get(i).get("meetingEffortPreferenceId");
                        if (preferenceIdObj != null) {
                            Integer preferenceId = (Integer) preferenceIdObj;
                            String meetingPreference = com.tse.core_application.model.Constants.MeetingPreferenceEnum.getById(preferenceId).getMeetingPreference();
                            formattedValue2 = formattedValue2 + " with meeting effort preference: " + meetingPreference;
                        }

                        // Update values
                        value1 = formattedValue1;
                        value2 = formattedValue2;
                    }

                    //adding billed meeting efforts
                    if (field.equalsIgnoreCase("billedMeetingEffort")) {
                        Integer billedMeetingEffort1 = (Integer) value1;
                        Integer billedMeetingEffort2 = (Integer) value2;

                        String formattedValue1 = formatEffort(billedMeetingEffort1);
                        String formattedValue2 = formatEffort(billedMeetingEffort2);

                        // Append meeting preference if available (for value2)
                        Object preferenceIdObj = arrayList.get(i).get("meetingEffortPreferenceId");
                        if (preferenceIdObj != null) {
                            Integer preferenceId = (Integer) preferenceIdObj;
                            String meetingPreference = com.tse.core_application.model.Constants.MeetingPreferenceEnum.getById(preferenceId).getMeetingPreference();
                            formattedValue2 = formattedValue2 + " with meeting effort preference: " + meetingPreference;
                        }

                        // Update values
                        value1 = formattedValue1;
                        value2 = formattedValue2;
                    }

                    //adding blocking reason in task history
                    if (field.equalsIgnoreCase("blockedReason")) {
                        WorkFlowTaskStatus workflowValue1 = objectMapper.convertValue(arrayList.get(i).get("fkWorkflowTaskStatus"), WorkFlowTaskStatus.class);
                        WorkFlowTaskStatus workflowValue2 = objectMapper.convertValue(arrayList.get(i + 1).get("fkWorkflowTaskStatus"), WorkFlowTaskStatus.class);
                        if (!Objects.equals(workflowValue1.getWorkflowTaskStatus(), workflowValue2.getWorkflowTaskStatus())) {
                            continue;
                        }
                    }

                    //adding Sprint History
                    if (field.equalsIgnoreCase("sprintId")) {
                        Optional<Sprint> oldSprint = value1 != null ? sprintRepository.findById((Long) value1) : Optional.empty();
                        Optional<Sprint> newSprint = value2 != null ? sprintRepository.findById((Long) value2) : Optional.empty();

                        if (oldSprint.isEmpty()) {
                            value1 = "Was not part of any Sprint";
                            value2 = "Added to sprint: '" + newSprint.get().getSprintTitle() + "'";
                        } else {
                            if (newSprint.isEmpty()) {
                                value1 = "Removed from sprint: '" + oldSprint.get().getSprintTitle() + "'";
                                value2 = "Not part of any Sprint";
                            } else {
                                value1 = "Removed from sprint: '" + oldSprint.get().getSprintTitle() + "'";
                                value2 = " Added to sprint: '" + newSprint.get().getSprintTitle() + "'";
                            }
                        }

                    }

                    if (field.equalsIgnoreCase("fkEpicId")) {

                        Epic oldFkEpic = objectMapper.convertValue(value1, Epic.class);
                        Epic newFkEpic = objectMapper.convertValue(value2, Epic.class);

                        Optional<Epic> oldEpic = oldFkEpic != null ? epicRepository.findById(oldFkEpic.getEpicId()) : Optional.empty();
                        Optional<Epic> newEpic = newFkEpic != null ? epicRepository.findById(newFkEpic.getEpicId()) : Optional.empty();

                        if (oldEpic.isEmpty()) {
                            value1 = "Was not part of any Epic";
                            value2 = "Added to Epic: '" + newEpic.get().getEpicTitle() + "'";
                        } else {
                            if (newEpic.isEmpty()) {
                                value1 = "Removed from epic: '" + oldEpic.get().getEpicTitle() + "'";
                                value2 = "Not part of any Epic";
                            } else {
                                value1 = "Removed from epic: '" + oldEpic.get().getEpicTitle() + "'";
                                value2 = " Added to epic: '" + newEpic.get().getEpicTitle() + "'";
                            }
                        }

                    }

                    if (field.equalsIgnoreCase("fkAccountIdBugReportedBy")) {
                        UserAccount userAccount1 = objectMapper.convertValue(value1, UserAccount.class);
                        value1 = value1 != null ? userAccount1.getFkUserId().getFirstName() + " " + userAccount1.getFkUserId().getLastName() : "unassigned";
                        UserAccount userAccount2 = objectMapper.convertValue(value2, UserAccount.class);
                        value2 = value2 != null ? userAccount2.getFkUserId().getFirstName() + " " + userAccount2.getFkUserId().getLastName() : "unassigned";
                    }

                    if (field.equalsIgnoreCase("dependencyIds")) {
                        List<Long> dependencyIds1 = (value1 != null) ? new ArrayList<>((List<Long>) value1) : new ArrayList<>();
                        List<Long> dependencyIds2 = (value2 != null) ? new ArrayList<>((List<Long>) value2) : new ArrayList<>();

                        // Determine added and removed dependencies
                        List<Long> addedDependencies = new ArrayList<>(dependencyIds2);
                        addedDependencies.removeAll(dependencyIds1);

                        List<Long> removedDependencies = new ArrayList<>(dependencyIds1);
                        removedDependencies.removeAll(dependencyIds2);

                        // Process added dependencies
                        StringBuilder addedMessages = new StringBuilder();
                        for (Long dependencyId : addedDependencies) {
                            Dependency dependency = dependencyRepository.findById(dependencyId).orElse(null);
                            if (dependency != null) {
                                boolean isPredecessor = taskFoundDb.getTaskId().equals(dependency.getPredecessorTaskId());
                                Task relatedTask = isPredecessor ? taskRepository.findById(dependency.getSuccessorTaskId()).orElse(null)
                                        : taskRepository.findById(dependency.getPredecessorTaskId()).orElse(null);
                                if (relatedTask != null) {
                                    String msg = (isPredecessor ? "Succeeded by " : "Preceded by ") + "Task# " + relatedTask.getTaskNumber() + " - " + relatedTask.getTaskTitle();
                                    if (addedMessages.length() > 0) {
                                        addedMessages.append(",   ");
                                    }
                                    addedMessages.append(msg);
                                }
                            }
                        }
                        if (addedMessages.length() > 0) {
                            value1 = "Dependency Added";
                            value2 = addedMessages.toString();
                        }

                        // process removed dependencies
                        for (Long dependencyId : removedDependencies) {
                            Dependency dependency = dependencyRepository.findById(dependencyId).orElse(null);
                            if (dependency != null) {
                                boolean isPredecessor = taskFoundDb.getTaskId().equals(dependency.getPredecessorTaskId());
                                Task relatedTask = isPredecessor ? taskRepository.findById(dependency.getSuccessorTaskId()).orElse(null)
                                        : taskRepository.findById(dependency.getPredecessorTaskId()).orElse(null);
                                if (relatedTask != null) {
                                    String msg = (isPredecessor ? "Succeeded by " : "Preceded by ") + "Task# " + relatedTask.getTaskNumber() + " - " + relatedTask.getTaskTitle();
                                    value1 = "Dependency Removed";
                                    value2 = msg;
                                }
                            }
                        }
                    }

                    if (field.equalsIgnoreCase("referenceWorkItemId")) {
                        List<Long> referenceWorkItemId1 = (value1 != null) ? new ArrayList<>((List<Long>) value1) : new ArrayList<>();
                        List<Long> referenceWorkItemId2 = (value2 != null) ? new ArrayList<>((List<Long>) value2) : new ArrayList<>();

                        // Determine added and removed dependencies
                        List<Long> addedReferenceWorkItems = new ArrayList<>(referenceWorkItemId2);
                        addedReferenceWorkItems.removeAll(referenceWorkItemId1);

                        List<Long> removedReferenceWorkItems = new ArrayList<>(referenceWorkItemId1);
                        removedReferenceWorkItems.removeAll(referenceWorkItemId2);

                        // Process added reference Work Item
                        StringBuilder addedMessages = new StringBuilder();
                        for (Long referenceId : addedReferenceWorkItems) {
                            Task task = taskRepository.findByTaskId(referenceId);
                            if (task != null) {
                                String msg = "WorkItem# " + task.getTaskNumber();
                                if (addedMessages.length() > 0) {
                                    addedMessages.append(",   ");
                                }
                                addedMessages.append(msg);

                            }
                        }

                        if (addedMessages.length() > 0) {
                            addedMessages.append(" is added");
                            value1 = "Reference Work Item Added";
                            value2 = addedMessages.toString();
                        }

                        StringBuilder removedMessages = new StringBuilder();
                        // process removed reference Work Item
                        for (Long referenceId : removedReferenceWorkItems) {
                            Task task = taskRepository.findByTaskId(referenceId);
                            if (task != null) {
                                String msg = "WorkItem# " + task.getTaskNumber();
                                if (removedMessages.length() > 0) {
                                    removedMessages.append(",   ");
                                }
                                removedMessages.append(msg);

                            }
                        }
                        if (removedMessages.length() > 0) {
                            removedMessages.append(" is removed");
                            if (addedMessages.length() > 0) {
                                removedMessages.insert(0, " AND ");
                                value1 += " AND Removed";
                                value2 += removedMessages.toString();
                            }
                            else {
                                value1 = "Reference Work Item Removed";
                                value2 = removedMessages.toString();
                            }
                        }
                    }

                    if (field.equalsIgnoreCase("taskLabels")) {
                        List<String> labels1 = (value1 != null) ? new ArrayList<>((List<String>) value1) : new ArrayList<>();
                        List<String> labels2 = (value2 != null) ? new ArrayList<>((List<String>) value2) : new ArrayList<>();

                        List<String> addedLabels = new ArrayList<>(labels2);
                        addedLabels.removeAll(labels1);

                        List<String> removedLabels = new ArrayList<>(labels1);
                        removedLabels.removeAll(labels2);

                        StringBuilder addedMessages = new StringBuilder();
                        if(addedLabels != null && !addedLabels.isEmpty()) {
                            for (String label : addedLabels) {
                                if (addedMessages.length() > 0) {
                                    addedMessages.append(",   ");
                                }
                                addedMessages.append(label);

                            }
                        }

                        if (addedMessages.length() > 0) {
                            addedMessages.append(" is added");
                            value1 = "Labels Added";
                            value2 = addedMessages.toString();
                        }

                        StringBuilder removedMessages = new StringBuilder();

                        if(removedLabels != null && !removedLabels.isEmpty()) {
                            for (String label : removedLabels) {
                                removedMessages.append(label);
                                removedMessages.append(" is removed");
                                value1 = "Label Removed";
                                value2 = removedMessages.toString();
                            }
                        }

                    }

                    if (field.equalsIgnoreCase("fkAccountIdMentor1") || field.equalsIgnoreCase("fkAccountIdMentor2") ||
                            field.equalsIgnoreCase("fkAccountIdObserver1") || field.equalsIgnoreCase("fkAccountIdObserver2") ||
                            field.equalsIgnoreCase("fkAccountIdAssignee") || field.equalsIgnoreCase("fkAccountIdAssigned") || field.equalsIgnoreCase("fkAccountIdRespondent")) {
                        UserAccount userAccount1 = objectMapper.convertValue(value1, UserAccount.class);
                        value1 = value1 != null ? userAccount1.getFkUserId().getFirstName() + " " + userAccount1.getFkUserId().getLastName() : "unassigned";
                        UserAccount userAccount2 = objectMapper.convertValue(value2, UserAccount.class);
                        value2 = value2 != null ? userAccount2.getFkUserId().getFirstName() + " " + userAccount2.getFkUserId().getLastName() : "unassigned";
                    }

                    if (field.equalsIgnoreCase("taskPriority")) {
                        value1 = value1 != null ? PriorityEnum.fromString((String) value1).getDescription() : "unassigned";
                        value2 = value2 != null ? PriorityEnum.fromString((String) value2).getDescription() : "unassigned";
                    }

                    if (field.equalsIgnoreCase("taskExpStartDate") || field.equalsIgnoreCase("taskActStDate") ||
                            field.equalsIgnoreCase("taskExpEndDate") || field.equalsIgnoreCase("taskActEndDate")) {
                        LocalDateTime localDateTime1 = objectMapper.convertValue(value1, LocalDateTime.class);
                        LocalDateTime localDateTime2 = objectMapper.convertValue(value2, LocalDateTime.class);
                        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm:ss a");
                        value1 = localDateTime1 != null ? dateTimeFormatter.format(DateTimeUtils.convertServerDateToUserTimezone(localDateTime1,timeZone)) : "No Value";
                        value2 = localDateTime2 != null ? dateTimeFormatter.format(DateTimeUtils.convertServerDateToUserTimezone(localDateTime2,timeZone)) : "No Value";

                    }

                    if(field.equalsIgnoreCase(com.tse.core_application.model.Constants.TaskFields.CHILD_TASK_IDS)) {
                        List<Long> list1 = (List<Long>) value1;
                        List<Long> list2 = (List<Long>) value2;
                        if (list1.size() != list2.size()) {
                            if (list1.size() > list2.size()) {
                                // Something was deleted in the new version
                                List<Long> deletedElements = new ArrayList<>(list1);
                                deletedElements.removeAll(list2);
                                if (!deletedElements.isEmpty()) {
                                    // only one element can be added/ deleted between versions
                                    value1 = "Child Task Deleted";
                                    if(childTaskInfo.containsKey(deletedElements.get(0))){
                                        value2 = childTaskInfo.get(deletedElements.get(0));
                                    } else {
                                        Task childTaskFromDb = taskRepository.findByTaskId(deletedElements.get(0));
                                        String newValueString = "TaskNumber: " + childTaskFromDb.getTaskNumber() + " Title: " + childTaskFromDb.getTaskTitle();
                                        childTaskInfo.put(deletedElements.get(0), newValueString);
                                    }
                                }
                            } else {
                                // Something was added to list1
                                List<Long> addedElements = new ArrayList<>(list2);
                                addedElements.removeAll(list1);
                                if (!addedElements.isEmpty()) {
                                    value1 = "Child Task Added";
                                    if(childTaskInfo.containsKey(addedElements.get(0))){
                                        value2 = childTaskInfo.get(addedElements.get(0));
                                    } else {
                                        Task childTaskFromDb = taskRepository.findByTaskId(addedElements.get(0));
                                        String newValueString = "#" + childTaskFromDb.getTaskNumber() + " : " + childTaskFromDb.getTaskTitle();
                                        value2 = newValueString;
                                        childTaskInfo.put(addedElements.get(0), newValueString);
                                    }
                                }
                            }
                        }
                    }
                    if (field.equalsIgnoreCase("severityId")) {
                        if(value1!=null){
                            Optional<Severity>severity=severityRepository.findById((Integer) value1);
                            value1=severity.isPresent()?severity.get().getSeverityDisplayName():"No Value";
                        }
                        else value1="No Value";
                        if(value2!=null){
                            Optional<Severity>severity=severityRepository.findById((Integer) value2);
                            value2=severity.isPresent()?severity.get().getSeverityDisplayName():"No Value";
                        }
                    }
                    if (field.equalsIgnoreCase("environmentId")) {
                        if (value1!=null) {
                            assert value1 instanceof Integer;
                            Optional<Environment>environment=environmentRepository.findById((Integer) value1);
                            value1=environment.isPresent()?environment.get().getEnvironmentDisplayName():"No Value";
                        }
                        else value1="No Value";
                        if (value2!=null) {
                            assert value2 instanceof Integer;
                            Optional<Environment>environment=environmentRepository.findById((Integer) value2);
                            value2=environment.isPresent()?environment.get().getEnvironmentDisplayName():"No Value";
                        }
                        else value2="No Value";
                    }
                    if (field.equalsIgnoreCase("resolutionId")) {
                        if (value1!=null) {
                            assert value1 instanceof Integer;
                            Optional<Resolution>resolution=resolutionRepository.findById((Integer) value1);
                            value1=resolution.isPresent()?resolution.get().getResolutionDisplayName():"No Value";
                        }
                        else value1="No Value";
                        if (value2!=null) {
                            Optional<Resolution>environment=resolutionRepository.findById((Integer) value2);
                            value2=environment.isPresent()?environment.get().getResolutionDisplayName():"No Value";
                        }
                        else value2="No Value";
                    }
                    if (field.equalsIgnoreCase("placeOfIdentification") || field.equalsIgnoreCase("stepsTakenToComplete")) {
                        if (value1==null || Objects.equals(value1.toString(),"")) {
                            value1="No Value";
                        }
                        if (value2==null || Objects.equals(value2.toString(),"")) {
                            value2="No Value";
                        }
                    }

                    if (field.equalsIgnoreCase("meetingList")) {
                        List<Long> referenceMeeting1 = (value1 != null) ? new ArrayList<>((List<Long>) value1) : new ArrayList<>();
                        List<Long> referenceMeeting2 = (value2 != null) ? new ArrayList<>((List<Long>) value2) : new ArrayList<>();

                        // Determine added and removed meetings
                        List<Long> addedReferenceMeeting = new ArrayList<>(referenceMeeting2);
                        addedReferenceMeeting.removeAll(referenceMeeting1);

                        List<Long> removedReferenceMeeting = new ArrayList<>(referenceMeeting1);
                        removedReferenceMeeting.removeAll(referenceMeeting2);

                        // Process added meetings
                        StringBuilder addedMessages = new StringBuilder();
                        for (Long referenceId : addedReferenceMeeting) {
                            Meeting meeting = meetingRepository.findByMeetingId(referenceId);
                            if (meeting != null) {
                                if (addedMessages.length() > 0) {
                                    addedMessages.append(",   ");
                                }
                                addedMessages.append("Meeting#").append(meeting.getMeetingNumber());
                            }
                        }

                        if (addedMessages.length() > 0) {
                            addedMessages.append(" is added");
                            value1 = "Reference Meeting Added";
                            value2 = addedMessages.toString();
                        }

                        // Process removed meetings
                        StringBuilder removedMessages = new StringBuilder();
                        for (Long referenceId : removedReferenceMeeting) {
                            Meeting meeting = meetingRepository.findByMeetingId(referenceId);
                            if (meeting != null) {
                                if (removedMessages.length() > 0) {
                                    removedMessages.append(",   ");
                                }
                                removedMessages.append("Meeting#").append(meeting.getMeetingNumber());
                            }
                        }

                        if (removedMessages.length() > 0) {
                            removedMessages.append(" is removed");
                            if (addedMessages.length() > 0) {
                                removedMessages.insert(0, " AND ");
                                value1 += " AND Removed";
                                value2 += removedMessages.toString();
                            } else {
                                value1 = "Reference Meeting Removed";
                                value2 = removedMessages.toString();
                            }
                        }
                    }

                    if (field.equalsIgnoreCase("countChildInternalDependencies")) {
                        if ((Integer) value1 < (Integer) value2) {
                            value1 = "Dependency added";
                            value2 = "Internal Dependency added";
                        }
                        else {
                            value1 = "Dependency removed";
                            value2 = "Internal Dependency removed";
                        }
                    }

                    if (field.equalsIgnoreCase("countChildExternalDependencies")) {
                        if ((Integer) value1 < (Integer) value2) {
                            value1 = "Dependency added";
                            value2 = "External Dependency added";
                        }
                        else {
                            value1 = "Dependency removed";
                            value2 = "External Dependency removed";
                        }
                    }

                    if (field.equalsIgnoreCase("releaseVersionName")) {
                        if (value1 == null) {
                            value1 = "No Value";
                        }

                        if (value2 == null) {
                            value2 = "No Value";
                        }
                    }

                    if (field.equalsIgnoreCase("isStarred")) {
                        boolean isValue1True = Boolean.TRUE.equals(value1);
                        boolean isValue2True = Boolean.TRUE.equals(value2);

                        if (!isValue1True && isValue2True) {
                            value1 = "No value";
                            value2 = "Mark as starred";
                        } else if (isValue1True && !isValue2True) {
                            value1 = "Was starred";
                            value2 = "Mark as un-starred";
                        } else {
                            continue;
                        }
                    }


                    historyResponse.setTaskNumber(taskNumber);
                    if(Objects.equals(arrayList.get(i).get("taskHistoryCreatedBy"),Constants.TaskUpdatedByIndicator_TaskHistoryTable.TASK_UPDATED_BY_SYSTEM)){
                        historyResponse.setModifiedBy("SYSTEM UPDATED");
                    }
                    else {
                        UserAccount userAccount = objectMapper.convertValue(arrayList.get(i+1).get("fkAccountIdLastUpdated"), UserAccount.class);
                        historyResponse.setModifiedBy(userAccount.getFkUserId().getFirstName() + " " + userAccount.getFkUserId().getLastName());
                    }
                    historyResponse.setModifiedOn(DateTimeUtils.convertServerDateToUserTimezone(objectMapper.convertValue(arrayList.get(i).get("createdDateTime"), LocalDateTime.class), timeZone));

                    String formattedFieldName = getFormattedFieldsName(field);
                    fieldName.put(field, formattedFieldName);
                    oldValue.put(field, (value1 != null ? value1 : "No Value"));
                    newValue.put(field, (value2 != null ? value2 : "No Value"));
                    String msg;
                    if (field.equals("taskProgressSystem")) {
                        msg = arrayList.get(i).get("taskHistoryCreatedBy").equals(Constants.TaskUpdatedByIndicator_TaskHistoryTable.TASK_UPDATED_BY_SYSTEM) ? "System Generated" : historyResponse.getModifiedBy() +
                                " has changed the " + field + " from " + (value1 != null ? value1 : "No Value") + " to " + (value2 != null ? value2 : "No Value");
                    }
                    else {
//                        if (value1 != null && value2 != null) {
                            msg = historyResponse.getModifiedBy() + " has changed the " + formattedFieldName + " from " + (value1 != null ? value1 : "No Value") +
                                    " to " + (value2 != null ? value2 : "No Value");
//                        } else {
//                            if (value1 != null && value2 == null) {
//                                msg = historyResponse.getModifiedBy() + " has changed the " + formattedFieldName + " from " + value1 +
//                                        " to ";
//                            } else {
//                                msg = historyResponse.getModifiedBy() + " has changed the " + formattedFieldName + " to " + value2;
//                            }
//                        }
                    }
                    message.put(field, msg);
                    historyResponse.setFieldName(fieldName);
                    historyResponse.setOldValue(oldValue);
                    historyResponse.setNewValue(newValue);
                    historyResponse.setMessage(message);
                }
            }

            historyResponse.setVersion((Long) arrayList.get(i + 1).get("version") + countOfVersion);
            if (historyResponse.getFieldName() != null && !historyResponse.getFieldName().isEmpty()) {
                taskHistoryResponse.add(historyResponse);
            }

            if(taskProgressSystemHistoryResponse != null){
                if(historyResponse.getFieldName() != null && !historyResponse.getFieldName().isEmpty()){
                    countOfVersion += 1L;
                    taskProgressSystemHistoryResponse.setVersion(taskProgressSystemHistoryResponse.getVersion() + 1L);
                }
                taskHistoryResponse.add(taskProgressSystemHistoryResponse);
            }
        }

        if (fieldMappingsToFind.contains(com.tse.core_application.model.Constants.FIELD_MAPPING_ID_FOR_ATTACHMENT)) {
            List<TaskAttachmentHistoryResponse> taskAttachmentHistoryResponseList = taskAttachmentHistoryService.getTaskAttachmentHistory(taskId, timeZone);
            for (TaskAttachmentHistoryResponse taskAttachmentHistoryResponse : taskAttachmentHistoryResponseList) {
                TaskHistory_FieldMapping_Response taskHistoryFieldMappingResponse = new TaskHistory_FieldMapping_Response();
                taskHistoryFieldMappingResponse.setTaskNumber(taskId.toString());
                taskHistoryFieldMappingResponse.setModifiedBy(taskAttachmentHistoryResponse.getModifiedBy());
                taskHistoryFieldMappingResponse.setModifiedOn(taskAttachmentHistoryResponse.getModifiedOn());
                HashMap<String, String> fieldName = new HashMap<>();
                HashMap<String, Object> oldValue = new HashMap<>();
                HashMap<String, Object> newValue = new HashMap<>();
                HashMap<String, Object> message = new HashMap<>();
                fieldName.put("fkAttachment", "Work Item Attachment");
                if (taskAttachmentHistoryResponse.getIsFileAdded()) {
                    oldValue.put("fkAttachment", "Attachment added");
                    message.put("fkAttachment", taskAttachmentHistoryResponse.getModifiedBy() + " added the attachment");
                } else {
                    oldValue.put("fkAttachment", "Attachment removed");
                    message.put("fkAttachment", taskAttachmentHistoryResponse.getModifiedBy() + " removed the attachment");
                }
                newValue.put("fkAttachment", taskAttachmentHistoryResponse.getMessage());
                taskHistoryFieldMappingResponse.setFieldName(fieldName);
                taskHistoryFieldMappingResponse.setOldValue(oldValue);
                taskHistoryFieldMappingResponse.setNewValue(newValue);
                taskHistoryFieldMappingResponse.setMessage(message);
                taskHistoryResponse.add(taskHistoryFieldMappingResponse);
            }
        }
        Collections.sort(taskHistoryResponse, Comparator.comparing(TaskHistory_FieldMapping_Response::getModifiedOn));
        long version = 1;
        for (TaskHistory_FieldMapping_Response taskHistory : taskHistoryResponse) {
            taskHistory.setVersion(version);
            version++;
        }
        return taskHistoryResponse;
    }

    // Split minutes into hours and minutes
    private String formatEffort(Integer effortMinutes) {
        if (effortMinutes == null || effortMinutes == 0) {
            return "0h 0min";
        } else {
            int hours = effortMinutes / 60;
            int minutes = effortMinutes % 60;
            return hours + "h " + minutes + "min";
        }
    }

    private TaskHistory_FieldMapping_Response createHistoryResponseForTaskProgressSystem(HashMap<String, Object> values){
        HashMap<String, String> fieldName = new HashMap<>();
        HashMap<String, Object> oldValue = new HashMap<>();
        HashMap<String, Object> newValue = new HashMap<>();
        HashMap<String, Object> message = new HashMap<>();
        String field = "taskProgressSystem";
        Object value1 =  values.get("oldValue") != null ? values.get("oldValue") : null;
        Object value2 = values.get("newValue") != null ? values.get("newValue") : null;

        TaskHistory_FieldMapping_Response taskProgressSystemHistoryResponse = new TaskHistory_FieldMapping_Response();
        taskProgressSystemHistoryResponse.setTaskNumber((String) values.get("taskNumber"));
        taskProgressSystemHistoryResponse.setModifiedBy("SYSTEM UPDATED");
        taskProgressSystemHistoryResponse.setModifiedOn((LocalDateTime) values.get("modifiedOn"));
        String formattedFieldName = getFormattedFieldsName("taskProgressSystem");
        fieldName.put(field, formattedFieldName);
        taskProgressSystemHistoryResponse.setFieldName(fieldName);
        oldValue.put(field, (value1 != null ? value1 : "No Value"));
        newValue.put(field, (value2 != null ? value2 : "No Value"));
        taskProgressSystemHistoryResponse.setOldValue(oldValue);
        taskProgressSystemHistoryResponse.setNewValue(newValue);
        String msg = "System" + " has changed the " + field + " from " + (value1 != null ? value1 : "No Value") + " to " + (value2 != null ? value2 : "No Value");
        message.put(field, msg);
        taskProgressSystemHistoryResponse.setMessage(message);
        taskProgressSystemHistoryResponse.setVersion((Long) values.get("version") + (Long) values.get("countOfVersion"));
        return taskProgressSystemHistoryResponse;
    }

    public String getFormattedFieldsName(String fieldName) {
        return Constants.TaskHistory_Column_Name.get(fieldName);
    }

    public Long getMaxVersionInArrayList(List<Long> versionList) {
        if (versionList != null && !versionList.isEmpty()) {
            Long max = versionList.get(0);
            for (int i = 1; i < versionList.size(); i++) {
                if (max < versionList.get(i))
                    max = versionList.get(i);
            }
            return max;
        } else {
            return null;
        }
    }

    public List<TaskHistory> getAllTaskHistoryByTaskIdAndVersion(Long taskId, List<Long> version) {
//        return taskHistoryRepository.findByTaskNumberAndVersionIn(taskNumber, version);
        return taskHistoryRepository.findByTaskIdAndVersionInOrderByVersionAscCreatedDateTimeAsc(taskId, version);
    }

    public TaskHistory addTaskHistoryOnUserUpdate(Task task) {
        taskServiceImpl.addLabelInWorkItem(task);
        TaskHistory taskHistory = new TaskHistory();
        BeanUtils.copyProperties(task, taskHistory);
        taskHistory.setTaskHistoryCreatedBy(Constants.TaskUpdatedByIndicator_TaskHistoryTable.TASK_UPDATED_BY_USER);
        return taskHistoryRepository.save(taskHistory);
    }

    public TaskHistory addTaskHistoryOnSystemUpdate(Task task) {
        taskServiceImpl.addLabelInWorkItem(task);
        TaskHistory taskHistory = new TaskHistory();
        BeanUtils.copyProperties(task, taskHistory);
        taskHistory.setTaskHistoryCreatedBy(Constants.TaskUpdatedByIndicator_TaskHistoryTable.TASK_UPDATED_BY_SYSTEM);
        return taskHistoryRepository.save(taskHistory);
    }

    Object newTaskProgressSystemEnum(Object value) {
        if(value!=null) {
            if(value.toString().equalsIgnoreCase(StatType.NOTSTARTED.name()))
                return "Not Started (No Delay)";
            if(value.toString().equalsIgnoreCase(StatType.LATE_COMPLETION.name()))
                return "Late Completion";
            if(value.toString().equalsIgnoreCase(StatType.COMPLETED.name()))
                return "On Time Completion";
            if(value.toString().equalsIgnoreCase(StatType.DELAYED.name()))
                return "Delayed";
            if(value.toString().equalsIgnoreCase(StatType.ONTRACK.name()))
                return "On Track";
            if(value.toString().equalsIgnoreCase(StatType.WATCHLIST.name()))
                return "Watchlist";
        }
        return value;
    }

}
