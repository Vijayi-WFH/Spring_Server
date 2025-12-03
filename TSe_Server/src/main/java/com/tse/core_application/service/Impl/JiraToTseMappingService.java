package com.tse.core_application.service.Impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.tse.core_application.constants.RoleEnum;
import com.tse.core_application.controller.TaskAttachmentController;
import com.tse.core_application.custom.model.AccountId;
import com.tse.core_application.custom.model.FileMetadata;
import com.tse.core_application.custom.model.WorkflowTaskStatusIdTypeState;
import com.tse.core_application.dto.*;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.model.*;
import com.tse.core_application.model.User;
import com.tse.core_application.repository.*;
import com.tse.core_application.utils.CommonUtils;
import com.tse.core_application.utils.DateTimeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityNotFoundException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
@Service
public class JiraToTseMappingService {

    private static final Logger logger = LogManager.getLogger(TaskAttachmentController.class.getName());

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AccessDomainRepository accessDomainRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private JiraToTseTaskMappingRepository jiraToTseTaskMappingRepository;

    @Autowired
    private WorkFlowTaskStatusRepository workFlowTaskStatusRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private EntityPreferenceRepository entityPreferenceRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private BURepository buRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private TaskSequenceRepository taskSequenceRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private OtpService otpService;

    @Autowired
    private RestrictedDomainsRepository restrictedDomainsRepository;

    @Autowired
    private ExceptionalRegistrationRepository exceptionalRegistrationRepository;

    @Autowired
    private BlockedRegistrationRepository blockedRegistrationRepository;

    @Autowired
    private UserPreferenceRepository userPreferenceRepository;

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private EntityPreferenceService entityPreferenceService;

    @Autowired
    private TaskAttachmentRepository taskAttachmentRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TaskServiceImpl taskServiceImpl;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentService commentService;

    @Value("${restricted.domain.list}")
    private List<String> restrictedDomains;

    @Value("${email.subject}")
    private String emailSubject;

    @Autowired
    @Qualifier("jiraExecutor")
    private Executor jiraExecutor;

    public List<JiraTasks> parseJiraCsv(MultipartFile file) throws Exception {
        List<JiraTasks> jiraTasks = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))) {

            List<String> logicalLines = new ArrayList<>();
            StringBuilder currentLine = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                if (currentLine.length() > 0) {
                    currentLine.append("\n").append(line);
                } else {
                    currentLine = new StringBuilder(line);
                }

                if (isQuoteBalanced(currentLine.toString())) {
                    logicalLines.add(currentLine.toString());
                    currentLine.setLength(0);
                }
            }

            // Final incomplete line: try to add if safe, skip otherwise
            if (currentLine.length() > 0 && isQuoteBalanced(currentLine.toString())) {
                logicalLines.add(currentLine.toString());
            }

            // Build the CSV reader from collected lines
            CSVReader csvReader = new CSVReaderBuilder(new StringReader(String.join("\n", logicalLines)))
                    .withCSVParser(new CSVParserBuilder().withSeparator(',').withQuoteChar('"').build())
                    .build();

            String[] headers = csvReader.readNext(); // header row
            String[] row;

            while (true) {
                row = safeReadNext(csvReader);
                if (row == null) break; // End of file

                if (row.length == 0) continue;

                try {
                    JiraTasks taskDTO = new JiraTasks();
                    List<String> watchers = new ArrayList<>();
                    List<String> watchersId = new ArrayList<>();
                    List<JiraComment> comments = new ArrayList<>();
                    List<LogWork> logWorkDetails = new ArrayList<>();
                    List<JiraAttachment> attachments = new ArrayList<>();

                    for (int j = 0; j < headers.length; j++) {
                        String header = headers[j];
                        String value = parseField(row[j]);

                        if (value != null) {
                            switch (header) {
                                case "Summary":
                                    taskDTO.setSummary(value);
                                    break;
                                case "Description":
                                    taskDTO.setDescription(value);
                                    break;
                                case "Issue id":
                                    taskDTO.setIssueId(parseLong(value));
                                    break;
                                case "Parent id":
                                    taskDTO.setParentId(value);
                                    break;
                                case "Issue Type":
                                    taskDTO.setIssueType(value);
                                    break;
                                case "Status":
                                    taskDTO.setStatus(value);
                                    break;
                                case "Project lead":
                                    taskDTO.setProjectLead(value);
                                    break;
                                case "Project lead id":
                                    taskDTO.setProjectLeadId(value);
                                    break;
                                case "Priority":
                                    taskDTO.setPriority(value);
                                    break;
                                case "Assignee":
                                    taskDTO.setAssignee(value);
                                    break;
                                case "Assignee Id":
                                    taskDTO.setAssigneeId(value);
                                    break;
                                case "Reporter":
                                    taskDTO.setReporter(value);
                                    break;
                                case "Reporter Id":
                                    taskDTO.setReporterId(value);
                                    break;
                                case "Creator":
                                    taskDTO.setCreator(value);
                                    break;
                                case "Creator Id":
                                    taskDTO.setCreatorId(value);
                                    break;
                                case "Created":
                                    taskDTO.setCreated(DateTimeUtils.parseDynamicDate(value));
                                    break;
                                case "Updated":
                                    taskDTO.setUpdated(DateTimeUtils.parseDynamicDate(value));
                                    break;
                                case "Last Viewed":
                                    taskDTO.setLastViewed(DateTimeUtils.parseDynamicDate(value));
                                    break;
                                case "Resolved":
                                    taskDTO.setResolved(DateTimeUtils.parseDynamicDate(value));
                                    break;
                                case "Due date":
                                    taskDTO.setDueDate(DateTimeUtils.parseDynamicDate(value));
                                    break;
                                case "Votes":
                                    taskDTO.setVotes(parseInteger(value));
                                    break;
                                default:
                                    if (header.startsWith("Watchers")) {
                                        watchers.add(value);
                                    } else if (header.startsWith("Log Work")) {
                                        parseLogWork(value, logWorkDetails);
                                    } else if (header.startsWith("Watchers Id")) {
                                        watchersId.add(value);
                                    }
                                    else if (header.startsWith("Attachment")) {
                                        JiraAttachment att = parseAttachment(value);
                                        if (att != null) attachments.add(att);
                                    }
                                    else if (header.startsWith("Comment")) {
                                        JiraComment comment = parseComment(value);
                                        if (comment != null) comments.add(comment);
                                    }

                                    break;
                            }
                        }
                    }

                    taskDTO.setWatchers(watchers);
                    taskDTO.setComments(comments);
                    taskDTO.setWatchersId(watchersId);
                    taskDTO.setAttachments(attachments);
                    taskDTO.setLogWorkDetails(logWorkDetails);

                    if (taskDTO.getIssueId() != null) {
                        jiraTasks.add(taskDTO);
                    }
                } catch (Exception rowError) {
                    // Skip this row
//                    System.err.println("Skipped malformed row: " + String.join(",", row));
                }
            }
        }

        return jiraTasks;
    }

    // Safely read next line without crashing on malformed entries
    private String[] safeReadNext(CSVReader reader) {
        while (true) {
            try {
                String[] row = reader.readNext();
                return row; // Could be null or valid
            } catch (Exception e) {
//                System.err.println("Malformed row skipped due to exception: " + e.getMessage());
                // loop again
            }
        }
    }

    private JiraComment parseComment(String value) {
        try {
            if (value == null || value.trim().isEmpty()) return null;
            String[] parts = value.split(";", 3);
            if (parts.length < 3) return null;

            JiraComment comment = new JiraComment();
            comment.setDate(DateTimeUtils.parseDynamicDate(parts[0].trim()));
            comment.setUploaderJiraId(parts[1].trim());
            comment.setMessage(parts[2].trim());
            return comment;
        } catch (Exception e) {
            return null;
        }
    }


    // More accurate quote balance check
    private boolean isQuoteBalanced(String str) {
        boolean inQuotes = false;
        for (char c : str.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            }
        }
        return !inQuotes;
    }

    private String parseField(String value) {
        return (value == null || value.trim().isEmpty()) ? null : value.trim();
    }

    private Integer parseInteger(String value) {
        try {
            return (value == null || value.trim().isEmpty()) ? null : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            System.err.println("Invalid integer value: " + value);
            return null;
        }
    }

    private Long parseLong(String value) {
        try {
            return (value == null || value.trim().isEmpty()) ? null : Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
//            System.err.println("Invalid long value: " + value);
            return null;
        }
    }

    private JiraAttachment parseAttachment(String value) {
        try {
            String[] parts = value.split(";");
            if (parts.length == 4) {
                JiraAttachment att = new JiraAttachment();
                att.setCreated(DateTimeUtils.parseDynamicDate(parts[0]));
                att.setUploaderJiraId(parts[1].replace("ug:", ""));
                att.setFileName(parts[2]);
                att.setUrl(parts[3]);
                return att;
            }
        } catch (Exception ignored) {

        }
        return null;
    }

    private LocalDateTime parseDate(String date) {
        return DateTimeUtils.parseDynamicDate(date);
    }

    private void parseLogWork(String logWorkEntry, List<LogWork> logWorkDetails) {
        if (logWorkEntry != null && !logWorkEntry.isEmpty()) {
            String[] parts = logWorkEntry.split(";");
            try {
                LocalDateTime date = parseDate(parts[1].trim()); // Dynamically parse Log Work date
                int value = Integer.parseInt(parts[3].trim());
                LogWork logWork = new LogWork();
                logWork.setDate(date);
                logWork.setValue(value);
                logWorkDetails.add(logWork);
            } catch (Exception e) {
                System.err.println("Invalid log work entry: " + logWorkEntry);
            }
        }
    }

    /**
     * This method used to convert all the LocalDateTime of a jira task to server time zone
     * @param jiraTask
     * @param timeZone
     */
    public void convertJiraTaskDateTimeToServerTimeZone(JiraTasks jiraTask, String timeZone) {
        if (jiraTask.getCreated() != null) {
            jiraTask.setCreated(DateTimeUtils.convertUserDateToServerTimezone(jiraTask.getCreated(), timeZone));
        }
        if (jiraTask.getUpdated() != null) {
            jiraTask.setUpdated(DateTimeUtils.convertUserDateToServerTimezone(jiraTask.getUpdated(), timeZone));
        }
        if (jiraTask.getLastViewed() != null) {
            jiraTask.setLastViewed(DateTimeUtils.convertUserDateToServerTimezone(jiraTask.getLastViewed(), timeZone));
        }
        if (jiraTask.getResolved() != null) {
            jiraTask.setResolved(DateTimeUtils.convertUserDateToServerTimezone(jiraTask.getResolved(), timeZone));
        }
        if (jiraTask.getDueDate() != null) {
            jiraTask.setDueDate(DateTimeUtils.convertUserDateToServerTimezone(jiraTask.getDueDate(), timeZone));
        }
    }

    /**
     * This method used to convert jira task and list of child task (if present) to server timezone
     * @param jiraTasks
     * @param jiraChildTaskList
     * @param timeZone
     */
    public void convertJiraTaskAndChildTaskDateTimeToServerTimeZone(JiraTasks jiraTasks, List<JiraTasks> jiraChildTaskList, String timeZone) {
        convertJiraTaskDateTimeToServerTimeZone(jiraTasks, timeZone);

        if(jiraChildTaskList != null && !jiraChildTaskList.isEmpty()) {
            for (JiraTasks jiraChildTask : jiraChildTaskList) {
                convertJiraTaskDateTimeToServerTimeZone(jiraChildTask, timeZone);
            }
        }
    }

    /**
     * This method returns all the User from jira task's list
     * @param jiraTaskList
     * @return
     */
    public List<JiraUsers> getJiraUsersFromFile(List<JiraTasks> jiraTaskList, List<JiraUsers> jiraUserFileList) {
        Map<String, JiraUsers> uniqueUsersMap = new HashMap<>();

        // Add existing users from file to the map
        for (JiraUsers user : jiraUserFileList) {
            if (user.getUserId() != null) {
                uniqueUsersMap.put(user.getUserId(), user);
            }
        }

        for (JiraTasks jiraTask : jiraTaskList) {
            if (jiraTask.getProjectLead() != null && jiraTask.getProjectLeadId() != null &&
                    !uniqueUsersMap.containsKey(jiraTask.getProjectLeadId())) {
                uniqueUsersMap.put(jiraTask.getProjectLeadId(), new JiraUsers(jiraTask.getProjectLeadId(), jiraTask.getProjectLead(), null, null, null));
            }

            if (jiraTask.getAssignee() != null && jiraTask.getAssigneeId() != null && !uniqueUsersMap.containsKey(jiraTask.getAssigneeId())) {
                uniqueUsersMap.put(jiraTask.getAssigneeId(), new JiraUsers(jiraTask.getAssigneeId(), jiraTask.getAssignee(), null, null, null));
            }

            if (jiraTask.getReporter() != null && jiraTask.getReporterId() != null && !uniqueUsersMap.containsKey(jiraTask.getReporterId())) {
                uniqueUsersMap.put(jiraTask.getReporterId(), new JiraUsers(jiraTask.getReporterId(), jiraTask.getReporter(), null, null, null));
            }

            if (jiraTask.getCreator() != null && jiraTask.getCreatorId() != null && !uniqueUsersMap.containsKey(jiraTask.getCreatorId())) {
                uniqueUsersMap.put(jiraTask.getCreatorId(), new JiraUsers(jiraTask.getCreatorId(), jiraTask.getCreator(), null, null, null));
            }

            if (jiraTask.getWatchers() != null && jiraTask.getWatchersId() != null && jiraTask.getWatchers().size() == jiraTask.getWatchersId().size()) {
                for (int i = 0; i < jiraTask.getWatchers().size(); i++) {
                    String watcherId = jiraTask.getWatchersId().get(i);
                    String watcherName = jiraTask.getWatchers().get(i);
                    if (watcherId != null && !uniqueUsersMap.containsKey(watcherId)) {
                        uniqueUsersMap.put(watcherId, new JiraUsers(watcherId, watcherName, null, null, null));
                    }
                }
            }
        }

        return new ArrayList<>(uniqueUsersMap.values());
    }

    public void validateCustomJiraStatus(Map<String, Integer> jiraCustomStatusMappedList) {
        if (jiraCustomStatusMappedList == null || jiraCustomStatusMappedList.isEmpty()) return;
        List<WorkflowTaskStatusIdTypeState> workflowTaskStatusIdTypeStateList = workFlowTaskStatusRepository.getWorkflowTaskStatusIdTypeState(List.of(Constants.TEAM_WORK_FLOW_TYPE_ID));
        Set<Integer> workflowStatusToRemove = new HashSet<>(Arrays.asList(Constants.WorkFlowStatusTeamTaskStatusId.BLOCKED, Constants.WorkFlowStatusTeamTaskStatusId.ON_HOLD, Constants.WorkFlowStatusTeamTaskStatusId.DELETED));
        workflowTaskStatusIdTypeStateList.removeIf(item ->
                item.getWorkflowTaskStatusId() != null && workflowStatusToRemove.contains(item.getWorkflowTaskStatusId())
        );

        Set<Integer> allowedStatuses = workflowTaskStatusIdTypeStateList.stream()
                    .filter(Objects::nonNull)
                    .map(WorkflowTaskStatusIdTypeState::getWorkflowTaskStatusId)
                    .collect(Collectors.toSet());

        for (Integer status : jiraCustomStatusMappedList.values()) {
            if (!allowedStatuses.contains(status)) {
                throw new ValidationFailedException("Mapped workflow status is not the valid status");
            }
        }
    }


    /**
     * This method will check is all the user of jira is mapped or not and mapped accountId should be present in team
     * @param jiraUsersMappedList
     * @param teamId
     */
    public void validateMappedUserAndAccountId(List<MappedJiraUser> jiraUsersMappedList, Long teamId) {
        // Get the distinct account IDs for the team
        List<Long> accountIdList = accessDomainRepository
                .findDistinctAccountIdByEntityTypeIdAndEntityId(Constants.EntityTypes.TEAM, teamId)
                .stream()
                .map(AccountId::getAccountId)
                .collect(Collectors.toList());

        for (MappedJiraUser mappedJiraUser : jiraUsersMappedList) {
            if (mappedJiraUser.getJiraUserId() != null && mappedJiraUser.getAccountId() != null && !accountIdList.contains(mappedJiraUser.getAccountId())) {
                throw new IllegalArgumentException("User " + mappedJiraUser.getJiraUserName() + " with userId " + mappedJiraUser.getJiraUserId() + " is not mapped or mapped account ID is invalid.");
            }
        }
    }

    /**
     * This method is map all the account and it's status (isActive) in the team against jira's user
     * @param jiraUsersMappedList
     * @param teamId
     * @return
     */
    public Map<String, MappedUserDetails> getAllUserMappedList (List<MappedJiraUser> jiraUsersMappedList, Long teamId) {
        Map<String, MappedUserDetails> userDetailsMap = new HashMap<>();
        for(MappedJiraUser mappedJiraUser : jiraUsersMappedList) {
            if (accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdAndIsActive(Constants.EntityTypes.TEAM, teamId, mappedJiraUser.getAccountId(), true)) {
                userDetailsMap.put(mappedJiraUser.getJiraUserId(), new MappedUserDetails(userAccountRepository.findByAccountId(mappedJiraUser.getAccountId()), true));
            }
            else {
                userDetailsMap.put(mappedJiraUser.getJiraUserId(), new MappedUserDetails(userAccountRepository.findByAccountId(mappedJiraUser.getAccountId()), false));
            }
        }
        return userDetailsMap;
    }

    /**
     * This method is used to map all the jira task with boolean which will indicate whether user want to create task or not
     * @param jiraTaskToCreateList
     * @return
     */
    public Map<Long, Boolean> getAllJiraTaskMappedList (List<JiraTaskToCreate> jiraTaskToCreateList) {
        Map<Long, Boolean> jiraTaskMappedList = new HashMap<>();
        if (jiraTaskToCreateList != null && !jiraTaskToCreateList.isEmpty()) {
            jiraTaskMappedList = jiraTaskToCreateList.stream()
                    .collect(Collectors.toMap(
                            JiraTaskToCreate::getIssueId,
                            JiraTaskToCreate::getIsCreated
                    ));
        }
        return jiraTaskMappedList;
    }

    /**
     * This method will map all the parent task with all it's child task
     * @param jiraTasksList
     * @param jiraTaskToCreateMap
     * @return
     */
    public Map<Long, List<JiraTasks>> getParentAndChildTaskMappedList(List<JiraTasks> jiraTasksList, Map<Long, Boolean> jiraTaskToCreateMap, Map<String, Integer> jiraIssueTypeMappedList) {
        Map<Long, List<JiraTasks>> parentAndChildTaskMapping = new HashMap<>();

        for (JiraTasks jiraTask : jiraTasksList) {
            if (jiraTask.getParentId() != null) {
                Long parentId = Long.valueOf(jiraTask.getParentId());
                if (jiraTaskToCreateMap.getOrDefault(parentId, false)) {
                    parentAndChildTaskMapping.computeIfAbsent(parentId, key -> new ArrayList<>())
                            .add(jiraTask);
                }
            }
        }

        return parentAndChildTaskMapping;
    }


    private String processJiraTaskTitle (String summary) {
        if (summary == null || summary.trim().isEmpty()) {
            return "Title";
        } else if (summary.length() < 3) {
            return summary + "...";
        } else if (summary.length() > 70) {
            return summary.substring(0, 67) + "...";
        } else {
            return summary;
        }
    }

    private String processJiraDescription(String description) {
        String result = description != null ? Jsoup.parse(description).text() : "No Description";
        if (result.trim().isEmpty()) {
            return "No Description";
        }
        else if (result.length() < 3) {
            return result + "...";
        }
        else if (result.length() > 5000) {
            return result.substring(0, 4980) + " .. trimmed";
        }
        return result;
    }

    public Integer getWorkFlowStatus(AddJiraTaskRequest addJiraTaskRequest, JiraTasks jiraTask) {
        Integer taskHandlingStrategy = addJiraTaskRequest.getTaskHandlingStrategy();

        if (jiraTask.getStatus() == null) return null;

        if (Objects.equals(jiraTask.getStatus(), Constants.JiraStatus.TO_DO)) {
            if (jiraTask.getTimeSpent() != null && jiraTask.getTimeSpent() > 0) {
                switch (taskHandlingStrategy) {
                    case Constants.TaskHandlingStrategy.IGNORE_TASK:
                        return null;

                    case Constants.TaskHandlingStrategy.IGNORE_LOGGED_TIME:
                        if (jiraTask.getOriginalEstimate() != null && jiraTask.getDueDate() != null) {
                            return Constants.WorkFlowStatusTeamTaskStatusId.NOT_STARTED;
                        } else {
                            return Constants.WorkFlowStatusTeamTaskStatusId.BACKLOG;
                        }

                    case Constants.TaskHandlingStrategy.BASED_ON_LOGGED_TIME:
                        return Constants.WorkFlowStatusTeamTaskStatusId.STARTED;

                    default:
                        throw new IllegalArgumentException("Unexpected task handling strategy: " + taskHandlingStrategy);
                }
            } else {
                if (jiraTask.getOriginalEstimate() != null && jiraTask.getDueDate() != null) {
                    return Constants.WorkFlowStatusTeamTaskStatusId.NOT_STARTED; // Not-Started
                } else {
                    return Constants.WorkFlowStatusTeamTaskStatusId.BACKLOG; // Backlog
                }
            }
        } else if (Objects.equals(jiraTask.getStatus(), Constants.JiraStatus.IN_PROGRESS)) {
            return Constants.WorkFlowStatusTeamTaskStatusId.STARTED;
        } else if (Objects.equals(jiraTask.getStatus(), Constants.JiraStatus.DONE) ||
                Objects.equals(jiraTask.getStatus(), Constants.JiraStatus.TESTABLE) ||
                Objects.equals(jiraTask.getStatus(), Constants.JiraStatus.IN_REVIEW)) {
            return Constants.WorkFlowStatusTeamTaskStatusId.COMPLETED;
        }
        else if (addJiraTaskRequest.getJiraCustomStatusMappedList() != null && !addJiraTaskRequest.getJiraCustomStatusMappedList().isEmpty() && addJiraTaskRequest.getJiraCustomStatusMappedList().containsKey(jiraTask.getStatus())) {
            return addJiraTaskRequest.getJiraCustomStatusMappedList().get(jiraTask.getStatus());
        }

        return null;
    }




    /**
     * This method is used to get all the normal task/bug and parent task of jira (excluding child task)
     * @param jiraTasksList
     * @return
     */
    public List<JiraTaskToCreate> getAllJiraTaskWithIdAndTitle(GetJiraTaskIdAndTitleRequest jiraTaskIdAndTitleRequest, List<JiraTasks> jiraTasksList) {
        List<JiraTaskToCreate> jiraTaskToCreateList = new ArrayList<>();

        if (jiraTaskIdAndTitleRequest == null) {
            throw new ValidationFailedException("Request can't be null");
        }
        Map<String, Integer>  jiraIssueTypeMappingList = getJiraIssueTypeMappedList (jiraTaskIdAndTitleRequest.getJiraIssueTypeMappingList());

        for (JiraTasks jiraTask : jiraTasksList) {
            try {
                if (jiraTask.getIssueId() != null && jiraTask.getIssueType() != null && jiraTask.getParentId() == null && jiraIssueTypeMappingList.containsKey(jiraTask.getIssueType()) && jiraIssueTypeMappingList.get(jiraTask.getIssueType()) != null) {
                    JiraTaskToCreate taskToCreate = new JiraTaskToCreate();
                    taskToCreate.setIssueId(jiraTask.getIssueId());
                    String summary = jiraTask.getSummary();
                    taskToCreate.setJiraTaskTitle(processJiraTaskTitle(summary));

                    jiraTaskToCreateList.add(taskToCreate);
                }
            } catch (Exception e) {
//                throw new RuntimeException(e);
            }
        }
        return jiraTaskToCreateList;
    }

    /**
     * This method will find Work flow status of all the child task of a parent task and also map jira child task to our app child task
     * @param addJiraTaskRequest
     * @param jiraChildTasksToCreate
     * @param childTaskList
     */
    public void getAndSetAllChildTaskWorkFlowStatus (AddJiraTaskRequest addJiraTaskRequest, List<JiraTasks> jiraChildTasksToCreate, Map<Long, Task> childTaskList) {
        // First pass: collect all child statuses from Jira
        for (JiraTasks jiraChildTask : jiraChildTasksToCreate) {
            Task childTask = new Task();
            Integer childWorkflowStatusId = getWorkFlowStatus(addJiraTaskRequest, jiraChildTask);

            if (childWorkflowStatusId != null) {
                childTask.setFkWorkflowTaskStatus(workFlowTaskStatusRepository.findByWorkflowTaskStatusId(childWorkflowStatusId));
                childTaskList.put(jiraChildTask.getIssueId(), childTask);
            }
        }

        // Second pass: enforce business rule - if any child is beyond Backlog, no child should be in Backlog
        boolean anyChildBeyondBacklog = childTaskList.values().stream()
                .anyMatch(task -> !Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatusId(),
                        Constants.WorkFlowStatusTeamTaskStatusId.BACKLOG));

        // If any child is beyond Backlog, promote all Backlog children to Not Started
        if (anyChildBeyondBacklog) {
            for (Task childTask : childTaskList.values()) {
                if (Objects.equals(childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatusId(),
                        Constants.WorkFlowStatusTeamTaskStatusId.BACKLOG)) {
                    childTask.setFkWorkflowTaskStatus(
                            workFlowTaskStatusRepository.findByWorkflowTaskStatusId(Constants.WorkFlowStatusTeamTaskStatusId.NOT_STARTED));
                }
            }
        }
    }

    /**
     * This method will find the status of parent task using child task status
     * @param task
     * @param childTaskList
     */
    public void getAndSetParentTaskWorkFlowStatus (Task task, Map<Long, Task> childTaskList) {
        if (childTaskList == null || childTaskList.isEmpty()) {
            task.setFkWorkflowTaskStatus(null);
        } else {
            Set<Integer> childStatuses = childTaskList.values().stream()
                    .map(childTask -> childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatusId())
                    .collect(Collectors.toSet());

            if (childStatuses.size() == 1 && childStatuses.contains(Constants.WorkFlowStatusTeamTaskStatusId.BACKLOG)) {
                task.setFkWorkflowTaskStatus(workFlowTaskStatusRepository.findByWorkflowTaskStatusId(Constants.WorkFlowStatusTeamTaskStatusId.BACKLOG));
            } else if (childStatuses.contains(Constants.WorkFlowStatusTeamTaskStatusId.NOT_STARTED) &&
                    childStatuses.stream().allMatch(status -> Objects.equals(status, Constants.WorkFlowStatusTeamTaskStatusId.BACKLOG)
                            || Objects.equals(status, Constants.WorkFlowStatusTeamTaskStatusId.NOT_STARTED))) {
                task.setFkWorkflowTaskStatus(workFlowTaskStatusRepository.findByWorkflowTaskStatusId(Constants.WorkFlowStatusTeamTaskStatusId.NOT_STARTED));
            } else if (childStatuses.size() == 1 && childStatuses.contains(Constants.WorkFlowStatusTeamTaskStatusId.COMPLETED)) {
                task.setFkWorkflowTaskStatus(workFlowTaskStatusRepository.findByWorkflowTaskStatusId(Constants.WorkFlowStatusTeamTaskStatusId.COMPLETED));
            } else {
                task.setFkWorkflowTaskStatus(workFlowTaskStatusRepository.findByWorkflowTaskStatusId(Constants.WorkFlowStatusTeamTaskStatusId.STARTED));
            }
        }
    }

    public void setWorkFlowStatusInWorkItems (AddJiraTaskRequest addJiraTaskRequest, Task task, JiraTasks jiraTask, List<JiraTasks> jiraChildTasksToCreate, Map<Long, Task> childTaskList) {
        boolean isParentTask = jiraChildTasksToCreate != null && !jiraChildTasksToCreate.isEmpty();

        if (!isParentTask) {
            Integer workflowStatusId = getWorkFlowStatus(addJiraTaskRequest, jiraTask);
            if (workflowStatusId != null) {
                task.setFkWorkflowTaskStatus(workFlowTaskStatusRepository.findByWorkflowTaskStatusId(workflowStatusId));
            }
        } else {
            getAndSetAllChildTaskWorkFlowStatus(addJiraTaskRequest, jiraChildTasksToCreate, childTaskList);
            getAndSetParentTaskWorkFlowStatus(task, childTaskList);
        }
    }

    /**
     * This method find and set the estimate of task
     * @param addJiraTaskRequest
     * @param task
     * @param jiraTask
     * @param jiraChildTasksToCreate
     * @param childTaskList
     */
    public void setEstimateInWorkItems (AddJiraTaskRequest addJiraTaskRequest, Task task, JiraTasks jiraTask, List<JiraTasks> jiraChildTasksToCreate, Map<Long, Task> childTaskList) {
        boolean isParentTask = jiraChildTasksToCreate != null && !jiraChildTasksToCreate.isEmpty();

        if (!isParentTask) {
            // Handle normal task or bug
            Integer workflowStatusId = task.getFkWorkflowTaskStatus().getWorkflowTaskStatusId();
            Integer taskEstimate = calculateTaskEstimate(addJiraTaskRequest, jiraTask, workflowStatusId);
            if (taskEstimate != null) {
                task.setTaskEstimate(taskEstimate);
            }
        } else {
            // Handle parent task
            int totalChildEstimate = 0;

            for (JiraTasks jiraChildTask : jiraChildTasksToCreate) {
                Task childTask = childTaskList.get(jiraChildTask.getIssueId());
                if (childTask != null) {
                    Integer workflowStatusId = childTask.getFkWorkflowTaskStatus().getWorkflowTaskStatusId();
                    Integer childEstimate = calculateTaskEstimate(addJiraTaskRequest, jiraChildTask, workflowStatusId);
                    if (childEstimate != null) {
                        childTask.setTaskEstimate(childEstimate);
                        totalChildEstimate += childEstimate;
                    }
                }
            }

            if (totalChildEstimate > 0) {
                task.setTaskEstimate(totalChildEstimate);
            }
        }
    }

    /**
     * This method calculate the estimate of jira task based on work flow task status
     * @param addJiraTaskRequest
     * @param jiraTask
     * @param workflowStatusId
     * @return
     */
    private Integer calculateTaskEstimate (AddJiraTaskRequest addJiraTaskRequest, JiraTasks jiraTask, Integer workflowStatusId) {
        Integer originalEstimate = (jiraTask.getOriginalEstimate() != null && jiraTask.getOriginalEstimate() != 0)
                ? jiraTask.getOriginalEstimate() / 60
                : null; // Convert seconds to minutes, or null if zero or null

        Integer timeSpent = (jiraTask.getTimeSpent() != null && jiraTask.getTimeSpent() != 0)
                ? jiraTask.getTimeSpent() / 60
                : null; // Convert seconds to minutes, or null if zero or null

        Integer customEstimate = addJiraTaskRequest.getCustomEstimate();

        if (Objects.equals(workflowStatusId, Constants.WorkFlowStatusTeamTaskStatusId.BACKLOG)) {
            return originalEstimate;
        } else if (Objects.equals(workflowStatusId, Constants.WorkFlowStatusTeamTaskStatusId.NOT_STARTED)) {
            return originalEstimate != null ? originalEstimate : customEstimate;
        } else if (Objects.equals(workflowStatusId, Constants.WorkFlowStatusTeamTaskStatusId.STARTED) ||
                Objects.equals(workflowStatusId, Constants.WorkFlowStatusTeamTaskStatusId.COMPLETED)) {
            if (originalEstimate != null) {
                return originalEstimate;
            } else if (timeSpent != null) {
                Integer completedPercentage = addJiraTaskRequest.getCompletedTaskPercentage();
                if (completedPercentage != null && completedPercentage > 0) {
                    return (int) Math.ceil((100.0 / completedPercentage) * timeSpent);
                } else {
                    return customEstimate;
                }
            } else {
                return customEstimate;
            }
        }
        return null;
    }

    /**
     * This method will find and set the priority of task and child task (If task is parent task)
     * @param task
     * @param jiraTask
     * @param jiraChildTasksToCreate
     * @param childTaskList
     */
    public void setPriorityInWorkItems (Task task, JiraTasks jiraTask, List<JiraTasks> jiraChildTasksToCreate, Map<Long, Task> childTaskList) {
        task.setTaskPriority(findPriorityOfWorkItem(task, jiraTask));

        if (childTaskList != null && !childTaskList.isEmpty()) {
            for (JiraTasks jiraChildTask : jiraChildTasksToCreate) {
                Task childTask = childTaskList.get(jiraChildTask.getIssueId());
                if (childTask != null) {
                    childTask.setTaskPriority(findPriorityOfWorkItem(childTask, jiraChildTask));
                }
            }
        }
    }

    public String findPriorityOfWorkItem (Task task, JiraTasks jiraTask) {
        if (jiraTask.getPriority() == null) {
            if (Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatusId(), Constants.WorkFlowStatusTeamTaskStatusId.BACKLOG)) {
                return null;
            }
            else {
                return Constants.Priorities.PRIORITY_P2;
            }
        }
        else if (Objects.equals(jiraTask.getPriority(), Constants.JiraTaskPriority.HIGHEST) || Objects.equals(jiraTask.getPriority(), Constants.JiraTaskPriority.HIGH)) {
            if (Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatusId(), Constants.WorkFlowStatusTeamTaskStatusId.BACKLOG)) {
                return Constants.Priorities.PRIORITY_P2;
            }
            else if (Objects.equals(jiraTask.getPriority(), Constants.JiraTaskPriority.HIGHEST)) {
                return Constants.Priorities.PRIORITY_P0;
            }
            else {
                return Constants.Priorities.PRIORITY_P1;
            }
        }
        else if (Objects.equals(jiraTask.getPriority(), Constants.JiraTaskPriority.MEDIUM)) {
            return Constants.Priorities.PRIORITY_P2;
        }
        else if (Objects.equals(jiraTask.getPriority(), Constants.JiraTaskPriority.LOW)) {
            return Constants.Priorities.PRIORITY_P3;
        }
        else if (Objects.equals(jiraTask.getPriority(), Constants.JiraTaskPriority.LOWEST)) {
            return Constants.Priorities.PRIORITY_P4;
        }
        return "P2";
    }

    /**
     * This method is used the get the assignTo of our app corresponding to jira task assigne and set it to the task
     * @param task
     * @param jiraTask
     * @param jiraChildTasksToCreate
     * @param childTaskList
     * @param userAccountIdDetailsMap
     * @param defaultUserAccount
     */
    public void getAndSetAssignedToInWorkItems(Task task, JiraTasks jiraTask, List<JiraTasks> jiraChildTasksToCreate, Map<Long, Task> childTaskList, Map<String, MappedUserDetails> userAccountIdDetailsMap, UserAccount defaultUserAccount) {
        setAssignedToForTask(task, jiraTask, userAccountIdDetailsMap, defaultUserAccount);

        // Set assigned to for child tasks if present
        if (jiraChildTasksToCreate != null && !jiraChildTasksToCreate.isEmpty()) {
            for (JiraTasks jiraChildTask : jiraChildTasksToCreate) {
                Task childTask = childTaskList.get(jiraChildTask.getIssueId());
                if (childTask != null) {
                    setAssignedToForTask(childTask, jiraChildTask, userAccountIdDetailsMap, defaultUserAccount);
                }
            }
        }
    }

    private void setAssignedToForTask(Task task, JiraTasks jiraTask, Map<String, MappedUserDetails> userAccountIdDetailsMap, UserAccount defaultUserAccount) {
        Integer workflowStatusId = task.getFkWorkflowTaskStatus().getWorkflowTaskStatusId();
        String assigneeId = jiraTask.getAssigneeId();

        if (Objects.equals(workflowStatusId, Constants.WorkFlowStatusTeamTaskStatusId.COMPLETED)) {
            if (assigneeId != null) {
                MappedUserDetails userDetails = userAccountIdDetailsMap.get(assigneeId);
                if (userDetails != null) {
                    task.setFkAccountIdAssigned(userDetails.getUserAccount());
                } else {
                    task.setFkAccountIdAssigned(defaultUserAccount);
                }
            } else {
                task.setFkAccountIdAssigned(defaultUserAccount);
            }
        } else if (Objects.equals(workflowStatusId, Constants.WorkFlowStatusTeamTaskStatusId.BACKLOG)) {
            if (assigneeId != null) {
                setUserAccountInTask(task, assigneeId, userAccountIdDetailsMap, defaultUserAccount);
            } else {
                task.setFkAccountIdAssigned(null);
            }
        } else {
            // For all other statuses, assign based on assigneeId or default
            if (assigneeId == null) {
                task.setFkAccountIdAssigned(defaultUserAccount);
            } else {
                setUserAccountInTask(task, assigneeId, userAccountIdDetailsMap, defaultUserAccount);
            }
        }
    }

    private void setUserAccountInTask(Task task, String assigneeId, Map<String, MappedUserDetails> userAccountIdDetailsMap, UserAccount defaultUserAccount) {
        MappedUserDetails userDetails = userAccountIdDetailsMap.get(assigneeId);

        if (userDetails != null && userDetails.isActive()) {
            task.setFkAccountIdAssigned(userDetails.getUserAccount());
        } else {
            task.setFkAccountIdAssigned(defaultUserAccount);
        }
    }

    public void getAndSetDateAndTimeInWorkItems(Task task, JiraTasks jiraTask, List<JiraTasks> jiraChildTasksToCreate, Map<Long, Task> childTaskList, Team team, String timeZone) {
        EntityPreference entityPreference = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, team.getFkOrgId().getOrgId()).get();
        LocalTime officeStartTime = DateTimeUtils.convertUserTimeToServerTimeZone(entityPreference.getOfficeHrsStartTime(), timeZone);
        LocalTime officeEndTime = DateTimeUtils.convertUserTimeToServerTimeZone(entityPreference.getOfficeHrsEndTime(), timeZone);

        List<Integer> offDays = entityPreference.getOffDays();
        List<Integer> workWeekDays = new ArrayList<>(Constants.daysOfWeek);
        LocalDateTime now = LocalDateTime.now();

        // Remove offDays from workWeekDays
        if (offDays != null && !offDays.isEmpty()) {
            workWeekDays.removeAll(offDays);
        }

        if (childTaskList != null && !childTaskList.isEmpty()) {
            // Process child tasks
            for (JiraTasks jiraChildTask : jiraChildTasksToCreate) {
                Task childTask = childTaskList.get(jiraChildTask.getIssueId());
                if (childTask != null) {
                    setTaskExpectedDates(childTask, jiraChildTask, officeStartTime, officeEndTime, workWeekDays);
                    setActualDates(childTask, jiraChildTask, officeStartTime, workWeekDays, now);
                }
            }
            // Process parent task after child tasks
            setParentTaskExpectedDates(task, childTaskList);
            setParentTaskActualDates(task, childTaskList);
        } else {
            // Process as a normal task or bug
            setTaskExpectedDates(task, jiraTask, officeStartTime, officeEndTime, workWeekDays);
            setActualDates(task, jiraTask, officeStartTime, workWeekDays, now);
        }
    }

    private void setTaskExpectedDates(Task task, JiraTasks jiraTask, LocalTime officeStartTime, LocalTime officeEndTime, List<Integer> workWeekDays) {
        Integer workflowStatusId = task.getFkWorkflowTaskStatus().getWorkflowTaskStatusId();
        LocalDateTime now = LocalDateTime.now();

        if (Objects.equals(workflowStatusId, Constants.WorkFlowStatusTeamTaskStatusId.BACKLOG)) {
            // Backlog: Set exp end date and time if due date is present
            if (jiraTask.getDueDate() != null) {
                task.setTaskExpEndDate(jiraTask.getDueDate());
                task.setTaskExpEndTime(jiraTask.getDueDate().toLocalTime());
            }
        } else if (Objects.equals(workflowStatusId, Constants.WorkFlowStatusTeamTaskStatusId.NOT_STARTED)) {
            setExpectedDates(task, jiraTask, officeStartTime, officeEndTime, workWeekDays, now);
        } else if (Objects.equals(workflowStatusId, Constants.WorkFlowStatusTeamTaskStatusId.STARTED)) {
            setExpectedDates(task, jiraTask, officeStartTime, officeEndTime, workWeekDays, now);
        } else if (Objects.equals(workflowStatusId, Constants.WorkFlowStatusTeamTaskStatusId.COMPLETED)) {
            setExpectedDates(task, jiraTask, officeStartTime, officeEndTime, workWeekDays, now);
        }
    }

    private void setExpectedDates(Task task, JiraTasks jiraTask, LocalTime officeStartTime, LocalTime officeEndTime, List<Integer> workWeekDays, LocalDateTime now) {
        LocalDateTime expEndDateTime;

        if (jiraTask.getDueDate() != null) {
            expEndDateTime = jiraTask.getDueDate();
        } else {
            expEndDateTime = calculateEndOfWeek(now, officeEndTime, workWeekDays);
        }

        task.setTaskExpEndDate(expEndDateTime);
        task.setTaskExpEndTime(expEndDateTime.toLocalTime());

        Integer taskEstimate = task.getTaskEstimate(); // in minutes

        if (taskEstimate == null || taskEstimate <= 0) {
            // Default: use start of current week before expEndDate
            LocalDateTime startDateTime = calculateStartOfWeek(expEndDateTime, officeStartTime, workWeekDays);
            task.setTaskExpStartDate(startDateTime);
            task.setTaskExpStartTime(startDateTime.toLocalTime());
            return;
        }

        LocalDateTime expStartDateTime = calculateStartOfWeek(expEndDateTime, officeStartTime, workWeekDays);
        while (Duration.between(expStartDateTime, expEndDateTime).toMinutes() < taskEstimate) {
            // Go one more workweek back
            expStartDateTime = calculateStartOfWeek(expStartDateTime.minusDays(1), officeStartTime, workWeekDays);
        }

        task.setTaskExpStartDate(expStartDateTime);
        task.setTaskExpStartTime(expStartDateTime.toLocalTime());
    }

    private void setActualDates(Task task, JiraTasks jiraTask, LocalTime officeStartTime, List<Integer> workWeekDays, LocalDateTime now) {
        Integer workflowStatusId = task.getFkWorkflowTaskStatus().getWorkflowTaskStatusId();

        if (Objects.equals(workflowStatusId, Constants.WorkFlowStatusTeamTaskStatusId.STARTED) || Objects.equals(workflowStatusId, Constants.WorkFlowStatusTeamTaskStatusId.COMPLETED)) {
            LocalDateTime actStartDateTime = null;

            // Calculate actual start date and time
            if (jiraTask.getLogWorkDetails() != null && !jiraTask.getLogWorkDetails().isEmpty()) {
                actStartDateTime = jiraTask.getLogWorkDetails().stream()
                        .map(LogWork::getDate)
                        .min(LocalDateTime::compareTo)
                        .orElse(null);
            }
            if (actStartDateTime == null) {
                actStartDateTime = task.getTaskExpStartDate();
            }

            task.setTaskActStDate(actStartDateTime);
            task.setTaskActStTime(actStartDateTime != null ? actStartDateTime.toLocalTime() : null);

            // Calculate actual end date and time for Completed status
            if (Objects.equals(workflowStatusId, Constants.WorkFlowStatusTeamTaskStatusId.COMPLETED)) {
                LocalDateTime actEndDateTime = jiraTask.getResolved() != null ? jiraTask.getResolved() : jiraTask.getUpdated();
                if (actEndDateTime == null) {
                    actEndDateTime = task.getTaskExpEndDate();
                }

                task.setTaskActEndDate(actEndDateTime);
                task.setTaskActEndTime(actEndDateTime != null ? actEndDateTime.toLocalTime() : null);

                // Validate that actual start date is before actual end date
                if (actStartDateTime != null && actEndDateTime != null && !actStartDateTime.isBefore(actEndDateTime)) {
                    LocalDateTime adjustedStart = calculateStartOfWeek(actEndDateTime, officeStartTime, workWeekDays);
                    task.setTaskActStDate(adjustedStart);
                    task.setTaskActStTime(adjustedStart != null ? adjustedStart.toLocalTime() : null);
                }
            }
        }
    }

    private void setParentTaskExpectedDates(Task task, Map<Long, Task> childTaskList) {
        LocalDateTime minStartDate = null;
        LocalDateTime maxEndDate = null;

        for (Task childTask : childTaskList.values()) {
            if (minStartDate == null || (childTask.getTaskExpStartDate() != null && childTask.getTaskExpStartDate().isBefore(minStartDate))) {
                minStartDate = childTask.getTaskExpStartDate();
            }
            if (maxEndDate == null || (childTask.getTaskExpEndDate() != null && childTask.getTaskExpEndDate().isAfter(maxEndDate))) {
                maxEndDate = childTask.getTaskExpEndDate();
            }
        }

        task.setTaskExpStartDate(minStartDate);
        task.setTaskExpStartTime(minStartDate != null ? minStartDate.toLocalTime() : null);

        task.setTaskExpEndDate(maxEndDate);
        task.setTaskExpEndTime(maxEndDate != null ? maxEndDate.toLocalTime() : null);
    }

    private void setParentTaskActualDates(Task task, Map<Long, Task> childTaskList) {
        LocalDateTime minStartDate = null;
        LocalDateTime maxEndDate = null;

        for (Task childTask : childTaskList.values()) {
            if (minStartDate == null || (childTask.getTaskActStDate() != null && childTask.getTaskActStDate().isBefore(minStartDate))) {
                minStartDate = childTask.getTaskActStDate();
            }
            if (maxEndDate == null || (childTask.getTaskActEndDate() != null && childTask.getTaskActEndDate().isAfter(maxEndDate))) {
                maxEndDate = childTask.getTaskActEndDate();
            }
        }

        task.setTaskActStDate(minStartDate);
        task.setTaskActStTime(minStartDate != null ? minStartDate.toLocalTime() : null);

        task.setTaskActEndDate(maxEndDate);
        task.setTaskActEndTime(maxEndDate != null ? maxEndDate.toLocalTime() : null);
    }

    private LocalDateTime calculateEndOfWeek(LocalDateTime now, LocalTime officeEndTime, List<Integer> workWeekDays) {
        int currentDay = now.getDayOfWeek().getValue();
        int endDay = workWeekDays.get(workWeekDays.size() - 1);

        LocalDate nextValidDay = now.toLocalDate();
        while (currentDay != endDay || nextValidDay.isBefore(now.toLocalDate())) {
            nextValidDay = nextValidDay.plusDays(1);
            currentDay = nextValidDay.getDayOfWeek().getValue();
            if (!workWeekDays.contains(currentDay)) {
                continue;
            }
        }
        return nextValidDay.atTime(officeEndTime);
    }

    private LocalDateTime calculateStartOfWeek(LocalDateTime now, LocalTime officeStartTime, List<Integer> workWeekDays) {
        LocalDate checkDate = now.toLocalDate();

        while (true) {
            int dayOfWeek = checkDate.getDayOfWeek().getValue();

            if (workWeekDays.get(0) == dayOfWeek) {
                LocalDateTime possibleStart = checkDate.atTime(officeStartTime);
                if (possibleStart.isBefore(now)) {
                    return possibleStart;
                }
            }
            // Go to previous day and repeat
            checkDate = checkDate.minusDays(1);
        }
    }

    public void getAndSetEffortAndPercentageTaskCompleted(AddJiraTaskRequest addJiraTaskRequest, Task task, JiraTasks jiraTask, List<JiraTasks> jiraChildTasksToCreate, Map<Long, Task> childTaskList) {

        boolean isParentTask = jiraChildTasksToCreate != null && !jiraChildTasksToCreate.isEmpty();

        if (!isParentTask) {
            // Process individual task or bug
            setEffortAndPercentageForTask(addJiraTaskRequest, task, jiraTask);
        } else {
            // Process child tasks first
            for (JiraTasks jiraChildTask : jiraChildTasksToCreate) {
                Task childTask = childTaskList.get(jiraChildTask.getIssueId());
                if (childTask != null) {
                    setEffortAndPercentageForTask(addJiraTaskRequest, childTask, jiraChildTask);
                }
            }
            // Process parent task after child tasks
            setEffortAndPercentageForParentTask(task, childTaskList);
        }
    }

    private void setEffortAndPercentageForTask(AddJiraTaskRequest addJiraTaskRequest, Task task, JiraTasks jiraTask) {
        Integer workflowStatusId = task.getFkWorkflowTaskStatus().getWorkflowTaskStatusId();
        Integer timeSpentInSeconds = jiraTask.getTimeSpent();
        Integer timeSpentInMinutes = (timeSpentInSeconds != null) ? timeSpentInSeconds / 60 : null; // Convert to minutes
        Integer taskEstimate = task.getTaskEstimate();

        if (Objects.equals(workflowStatusId, Constants.WorkFlowStatusTeamTaskStatusId.STARTED)) {
            if (timeSpentInMinutes != null && timeSpentInMinutes > 0) {
                // Set efforts to timeSpentInMinutes
                task.setRecordedEffort(timeSpentInMinutes);
                task.setRecordedTaskEffort(timeSpentInMinutes);
                task.setTotalEffort(timeSpentInMinutes);

                // Set userPerceivedPercentageTaskCompleted and earnedTimeTask
                Integer completedTaskPercentage = addJiraTaskRequest.getCompletedTaskPercentage();
                task.setUserPerceivedPercentageTaskCompleted(completedTaskPercentage);

                if (taskEstimate != null && completedTaskPercentage != null) {
                    Integer earnedTimeTask = (completedTaskPercentage * taskEstimate) / 100;
                    task.setEarnedTimeTask(earnedTimeTask);
                } else {
                    task.setEarnedTimeTask(null);
                }
            }
        } else if (Objects.equals(workflowStatusId, Constants.WorkFlowStatusTeamTaskStatusId.COMPLETED)) {
            // Completed status
            if (timeSpentInMinutes != null && timeSpentInMinutes > 0) {
                // Set efforts to timeSpentInMinutes
                task.setRecordedEffort(timeSpentInMinutes);
                task.setRecordedTaskEffort(timeSpentInMinutes);
                task.setTotalEffort(timeSpentInMinutes);
            } else if (taskEstimate != null) {
                // Set efforts to taskEstimate
                task.setRecordedEffort(taskEstimate);
                task.setRecordedTaskEffort(taskEstimate);
                task.setTotalEffort(taskEstimate);
            }

            // Set earnedTimeTask and userPerceivedPercentageTaskCompleted
            task.setEarnedTimeTask(taskEstimate);
            task.setUserPerceivedPercentageTaskCompleted(100);
        }
    }

    private void setEffortAndPercentageForParentTask(Task task, Map<Long, Task> childTaskList) {
        Integer workflowStatusId = task.getFkWorkflowTaskStatus().getWorkflowTaskStatusId();

        if (Objects.equals(workflowStatusId, Constants.WorkFlowStatusTeamTaskStatusId.STARTED)) {
            // Parent task in Started state
            int totalEffort = 0;
            int totalEarnedTimeTask = 0;

            for (Task childTask : childTaskList.values()) {
                totalEffort += childTask.getTotalEffort() != null ? childTask.getTotalEffort() : 0;
                totalEarnedTimeTask += childTask.getEarnedTimeTask() != null ? childTask.getEarnedTimeTask() : 0;
            }

            task.setRecordedEffort(totalEffort);
            task.setRecordedTaskEffort(totalEffort);
            task.setTotalEffort(totalEffort);
            task.setEarnedTimeTask(totalEarnedTimeTask);

            Integer taskEstimate = task.getTaskEstimate();
            if (taskEstimate != null && totalEarnedTimeTask > 0) {
                Integer completedPercentage = (totalEarnedTimeTask * 100) / taskEstimate;
                task.setUserPerceivedPercentageTaskCompleted(completedPercentage);
            } else {
                task.setUserPerceivedPercentageTaskCompleted(null);
            }
        } else if (Objects.equals(workflowStatusId, Constants.WorkFlowStatusTeamTaskStatusId.COMPLETED)) {
            // Parent task in Completed state
            Integer taskEstimate = task.getTaskEstimate();

            if (taskEstimate != null) {
                // Set all efforts to taskEstimate
                task.setRecordedEffort(taskEstimate);
                task.setRecordedTaskEffort(taskEstimate);
                task.setTotalEffort(taskEstimate);
                task.setEarnedTimeTask(taskEstimate);
                task.setUserPerceivedPercentageTaskCompleted(100);
            }
        }
    }

    public void getAndSetAllTheMandatoryField(AddJiraTaskRequest addJiraTaskRequest, Task task, JiraTasks jiraTask, List<JiraTasks> jiraChildTasksToCreate, Map<Long, Task> childTaskList, Team team, Map<String, MappedUserDetails> userAccountIdDetailsMap, UserAccount defaultAccountId, Map<String, Integer> jiraIssueTypeMappedList) {
        getAndSetTaskTitleAndDescriptionAndWorkFlowId(task, jiraTask, jiraChildTasksToCreate, childTaskList);
        getAndSetTeamProjectBuOrg(task, childTaskList, team);
        getAndSetTaskTypeIdAndParentTaskTypeIdAndIsBug(task, jiraTask, jiraChildTasksToCreate, childTaskList, jiraIssueTypeMappedList);
        getAndSetAllAccountIds(task, jiraTask, jiraChildTasksToCreate, childTaskList, userAccountIdDetailsMap, defaultAccountId);

    }

    public void getAndSetTaskTitleAndDescriptionAndWorkFlowId(Task task, JiraTasks jiraTask, List<JiraTasks> jiraChildTasksToCreate, Map<Long, Task> childTaskList) {
        task.setTaskTitle(processJiraTaskTitle(jiraTask.getSummary()));
        task.setTaskDesc(processJiraDescription(jiraTask.getDescription()));
        task.setTaskWorkflowId(3);

        // If there are child tasks, process each one
        if (jiraChildTasksToCreate != null && !jiraChildTasksToCreate.isEmpty()) {
            for (JiraTasks jiraChildTask : jiraChildTasksToCreate) {
                Task childTask = childTaskList.get(jiraChildTask.getIssueId());
                if (childTask != null) {
                    childTask.setTaskTitle(processJiraTaskTitle(jiraChildTask.getSummary()));
                    childTask.setTaskDesc(processJiraDescription(jiraChildTask.getDescription()));
                    childTask.setTaskWorkflowId(3);
                }
            }
        }
    }

    public void getAndSetTeamProjectBuOrg (Task task, Map<Long, Task> childTaskList, Team team) {
        setTeamProjectBuOrg(task, team);
        if (childTaskList != null && !childTaskList.isEmpty()) {
            for (Task childTask : childTaskList.values()) {
                setTeamProjectBuOrg(childTask, team);
            }
        }
    }

    private void setTeamProjectBuOrg (Task task, Team team) {
        task.setFkTeamId(team);
        task.setFkProjectId(team.getFkProjectId());
        task.setBuId(team.getFkProjectId().getBuId());
        task.setFkOrgId(team.getFkOrgId());
    }

    public void getAndSetTaskTypeIdAndParentTaskTypeIdAndIsBug (Task task, JiraTasks jiraTask, List<JiraTasks> jiraChildTasksToCreate, Map<Long, Task> childTaskList, Map<String, Integer> jiraIssueTypeMappedList) {
        boolean isParent = childTaskList != null && !childTaskList.isEmpty();
        if (isParent) {
            task.setTaskTypeId(Constants.TaskTypes.PARENT_TASK);
            if (Objects.equals(jiraIssueTypeMappedList.get(jiraTask.getIssueType()), Constants.TaskTypes.BUG_TASK)) {
                task.setIsBug(true);
            }
        }
        else if (Objects.equals(jiraIssueTypeMappedList.get(jiraTask.getIssueType()), Constants.TaskTypes.BUG_TASK)) {
            task.setTaskTypeId(Constants.TaskTypes.BUG_TASK);
            task.setIsBug(true);
        }
        else {
            task.setTaskTypeId(Constants.TaskTypes.TASK);
        }

        if (isParent) {
            for (JiraTasks jiraChildTask : jiraChildTasksToCreate) {
                Task childTask = childTaskList.get(jiraChildTask.getIssueId());
                if (childTask != null) {
                    childTask.setTaskTypeId(Constants.TaskTypes.CHILD_TASK);
                    childTask.setParentTaskTypeId(task.getTaskTypeId());
                }
            }
        }
    }

    public void getAndSetAllAccountIds (Task task, JiraTasks jiraTask, List<JiraTasks> jiraChildTasksToCreate, Map<Long, Task> childTaskList, Map<String, MappedUserDetails> userAccountIdDetailsMap, UserAccount defaultAccountId) {
        boolean isParent = childTaskList != null && !childTaskList.isEmpty();
        setAllAccountIds(task, jiraTask, userAccountIdDetailsMap, defaultAccountId);

        if (isParent) {
            for (JiraTasks jiraChildTask : jiraChildTasksToCreate) {
                Task childTask = childTaskList.get(jiraChildTask.getIssueId());
                if (childTask != null) {
                    setAllAccountIds(childTask, jiraChildTask, userAccountIdDetailsMap, defaultAccountId);
                }
            }
        }
    }

    public void setAllAccountIds (Task task, JiraTasks jiraTask, Map<String, MappedUserDetails> userAccountIdDetailsMap, UserAccount defaultAccountId) {
        MappedUserDetails mappedUserDetails = userAccountIdDetailsMap.get(jiraTask.getCreatorId());
        if (mappedUserDetails != null) {
            task.setFkAccountId(mappedUserDetails.getUserAccount());
            task.setFkAccountIdCreator(mappedUserDetails.getUserAccount());
            task.setFkAccountIdAssignee(mappedUserDetails.getUserAccount());
            task.setFkAccountIdLastUpdated(mappedUserDetails.getUserAccount());
        }
        else {
            task.setFkAccountId(defaultAccountId);
            task.setFkAccountIdCreator(defaultAccountId);
            task.setFkAccountIdAssignee(defaultAccountId);
            task.setFkAccountIdLastUpdated(defaultAccountId);
        }
    }

    public void getAndSetTaskNumberAndIdentifier (Task task, Team team) {
        Long nextTaskIdentifier = taskService.getNextTaskIdentifier(team.getTeamId());
        task.setTaskIdentifier(nextTaskIdentifier);
        task.setTaskNumber(team.getTeamCode() + "-" + nextTaskIdentifier);
    }

    public void setTaskInJiraToOurAppMappingTable (Task task, JiraTasks jiraTask, Team team) {
        JiraToTseTaskMapping jiraToOurAppTaskMapping = new JiraToTseTaskMapping();
        jiraToOurAppTaskMapping.setIssueId(jiraTask.getIssueId());
        jiraToOurAppTaskMapping.setFkTaskId(task);
        jiraToOurAppTaskMapping.setTeamId(team.getTeamId());
        jiraToTseTaskMappingRepository.save(jiraToOurAppTaskMapping);
    }

    public void validateUserRequest(AddJiraTaskRequest addJiraTaskRequest, Team team) {
        if (!accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdAndIsActive(Constants.EntityTypes.TEAM, team.getTeamId(), addJiraTaskRequest.getDefaultAccountIdAssigned(), true)) {
            throw new ValidationFailedException("Default assigned to should be active in the team");
        }
        Integer completedTaskPercentage = addJiraTaskRequest.getCompletedTaskPercentage();
        if (completedTaskPercentage == null || !(completedTaskPercentage > 0 && completedTaskPercentage <100)) {
            throw new ValidationFailedException("Completed Task percentage should be between 0 to 100");
        }
        Integer taskHandlingStrategy = addJiraTaskRequest.getTaskHandlingStrategy();
        if (!Objects.equals(taskHandlingStrategy, Constants.TaskHandlingStrategy.IGNORE_TASK) && !Objects.equals(taskHandlingStrategy, Constants.TaskHandlingStrategy.IGNORE_LOGGED_TIME) &&
            !Objects.equals(taskHandlingStrategy, Constants.TaskHandlingStrategy.BASED_ON_LOGGED_TIME)) {
            throw new ValidationFailedException("Please select a valid task strategy");
        }
        if (!(addJiraTaskRequest.getCustomEstimate() > 0)) {
            throw new ValidationFailedException("Please enter default estimate and it should be greater than 0");
        }
    }

    public AddJiraTaskResponse addAllJiraTask (AddJiraTaskRequest addJiraTaskRequest, List<JiraTasks> jiraTasksList, String accountIds, String timeZone) {
        Team team = teamRepository.findByTeamId(addJiraTaskRequest.getTeamId());
        if (team == null) {
            throw new ValidationFailedException("Team does not exist or it is deleted");
        }
        List<Long> accountIdList = CommonUtils.convertToLongList(accountIds);
        Long accountIdOfUser = userAccountRepository.findAccountIdByOrgIdAndIsActiveAndAccountIdIn(team.getFkOrgId().getOrgId(), true, accountIdList);
        List<MappedJiraUser> jiraUsersMappedList = addJiraTaskRequest.getJiraUserMappedList();
        validateCustomJiraStatus (addJiraTaskRequest.getJiraCustomStatusMappedList());
        validateMappedUserAndAccountId(jiraUsersMappedList, addJiraTaskRequest.getTeamId());

        UserAccount defaultUserAccount = userAccountRepository.findByAccountIdAndIsActive(addJiraTaskRequest.getDefaultAccountIdAssigned(), true);
        if (defaultUserAccount == null) {
            throw new ValidationFailedException("Default assigned to is not part of the organisation");
        }

        validateUserRequest(addJiraTaskRequest, team);

        Map<String, Integer> jiraIssueTypeMappedList = getJiraIssueTypeMappedList (addJiraTaskRequest.getJiraIssueTypeMappingList());
        Map<String, MappedUserDetails> userAccountIdDetailsMap = getAllUserMappedList(jiraUsersMappedList, team.getTeamId());
        Map<Long, Boolean> jiraTaskToCreateMap = getAllJiraTaskMappedList(addJiraTaskRequest.getJiraTaskToCreateList());
        Map<Long, List<JiraTasks>> parentAndChildTaskMapping = getParentAndChildTaskMappedList(jiraTasksList, jiraTaskToCreateMap, jiraIssueTypeMappedList);
        AddJiraTaskResponse addJiraTaskResponse = addAllJiraTaskToOurApp(addJiraTaskRequest, team, jiraTasksList, userAccountIdDetailsMap, jiraTaskToCreateMap, defaultUserAccount, parentAndChildTaskMapping, jiraIssueTypeMappedList, accountIdOfUser, timeZone);
        return addJiraTaskResponse;
    }

    private Map<String, Integer> getJiraIssueTypeMappedList(List<JiraIssueTypeMapping> jiraIssueTypeMappingList) {
        Map<String, Integer> jiraIssueTypeMappedList = new HashMap<>();
        List<Integer> taskTypeIdList = List.of(Constants.TaskTypes.TASK, Constants.TaskTypes.BUG_TASK);
        if (jiraIssueTypeMappingList != null && !jiraIssueTypeMappingList.isEmpty()) {
            for (JiraIssueTypeMapping jiraIssueTypeMapping : jiraIssueTypeMappingList) {
                if (jiraIssueTypeMapping.getIssueType() != null && jiraIssueTypeMapping.getTaskTypeId() != null && taskTypeIdList.contains(jiraIssueTypeMapping.getTaskTypeId())) {
                    jiraIssueTypeMappedList.put(jiraIssueTypeMapping.getIssueType(), jiraIssueTypeMapping.getTaskTypeId());
                }
            }
        }
        if (jiraIssueTypeMappedList.isEmpty()) {
            throw new ValidationFailedException("Please map at least one issue type with valid task type");
        }
        return jiraIssueTypeMappedList;
    }

    public AddJiraTaskResponse addAllJiraTaskToOurApp(AddJiraTaskRequest addJiraTaskRequest, Team team, List<JiraTasks> jiraTasksList, Map<String, MappedUserDetails> userAccountIdDetailsMap, Map<Long, Boolean> jiraTaskToCreateMap, UserAccount defaultUserAccount, Map<Long, List<JiraTasks>> parentAndChildTaskMapping, Map<String, Integer> jiraIssueTypeMappedList, Long accountIdOfUser, String timeZone) {
        AddJiraTaskResponse addJiraTaskResponse = new AddJiraTaskResponse();
        List<JiraTaskBulkResponse> successList = new ArrayList<>();
        List<JiraTaskBulkResponse> failureList = new ArrayList<>();
        AtomicInteger bugsCount       = new AtomicInteger(0);
        AtomicInteger tasksCount      = new AtomicInteger(0);
        AtomicInteger childTasksCount = new AtomicInteger(0);
        AtomicInteger parentTasksCount= new AtomicInteger(0);
        List<Long> alreadyCreatedTaskIssueIdList = jiraToTseTaskMappingRepository.findIssueIdByTeamId(team.getTeamId());
        if (jiraTasksList != null && !jiraTasksList.isEmpty()) {
            for (JiraTasks jiraTask : jiraTasksList) {
                if (jiraTask.getParentId() != null) {
                    continue;
                }
                if (jiraTask.getIssueId() != null && jiraTaskToCreateMap.containsKey(jiraTask.getIssueId()) && jiraTaskToCreateMap.get(jiraTask.getIssueId())
                    && jiraTask.getIssueType() != null && jiraTask.getParentId() == null && jiraIssueTypeMappedList.containsKey(jiraTask.getIssueType()) && jiraIssueTypeMappedList.get(jiraTask.getIssueType()) != null) {
                    try {
                        if (jiraTask.getIssueId() == null) {
                            throw new IllegalArgumentException("Issue id of jira task should not be null");
                        }
                        if (alreadyCreatedTaskIssueIdList != null && alreadyCreatedTaskIssueIdList.contains(jiraTask.getIssueId())) {
                            throw new IllegalStateException("This jira task is already present in the team");
                        }
                        Task task = new Task();
                        addJiraTaskToOurApp(task, addJiraTaskRequest, team, jiraTasksList, jiraTask, userAccountIdDetailsMap, parentAndChildTaskMapping, defaultUserAccount, jiraIssueTypeMappedList, bugsCount, tasksCount, childTasksCount, parentTasksCount, timeZone);
                        successList.add(new JiraTaskBulkResponse(jiraTask.getIssueId(), jiraTask.getSummary(), task.getTaskId(), task.getTaskNumber(), team.getTeamId(), "Jira task created successfully"));

                    } catch (Exception e) {
                        failureList.add(new JiraTaskBulkResponse(jiraTask.getIssueId(), jiraTask.getSummary(), null, null, team.getTeamId(), e.getMessage()));
                    }
                }
            }
        }

        addJiraTaskResponse.setSuccessList(successList);
        addJiraTaskResponse.setFailureList(failureList);
        try {
            sendNotificationOfJiraImport(accountIdOfUser, successList, failureList, team, timeZone, tasksCount, parentTasksCount, childTasksCount, bugsCount);
        } catch (Exception e) {
            System.out.println("Not able to send notification of imported jira task");
        }
        return addJiraTaskResponse;
    }

    public void sendNotificationOfJiraImport(
            Long accountIdOfUser,
            List<JiraTaskBulkResponse> successList,
            List<JiraTaskBulkResponse> failureList,
            Team team,
            String timeZone,
            AtomicInteger tasksCount,
            AtomicInteger parentTasksCount,
            AtomicInteger childTasksCount,
            AtomicInteger bugsCount
    ) {
        if (accountIdOfUser == null) {
            return;
        }

        List<JiraTaskBulkResponse> success = (successList != null) ? successList : Collections.emptyList();
        List<JiraTaskBulkResponse> failure = (failureList != null) ? failureList : Collections.emptyList();

        int successCount = success.size();
        int failureCount = failure.size();
        int totalCount   = successCount + failureCount;

        String notificationBody;

        if (!success.isEmpty()) {
            String minTaskNumber = success.stream()
                    .filter(t -> t.getTaskId() != null && t.getTaskNumber() != null)
                    .min(Comparator.comparing(JiraTaskBulkResponse::getTaskId))
                    .map(JiraTaskBulkResponse::getTaskNumber)
                    .orElse("N/A");

            String maxTaskNumber = success.stream()
                    .filter(t -> t.getTaskId() != null && t.getTaskNumber() != null)
                    .max(Comparator.comparing(JiraTaskBulkResponse::getTaskId))
                    .map(JiraTaskBulkResponse::getTaskNumber)
                    .orElse("N/A");

            int tCount  = (tasksCount != null) ? tasksCount.get() : 0;
            int ptCount = (parentTasksCount != null)? parentTasksCount.get(): 0;
            int ctCount = (childTasksCount != null) ? childTasksCount.get() : 0;
            int bCount  = (bugsCount != null) ? bugsCount.get() : 0;

            notificationBody = String.format(
                    "JIRA Import Success — %d/%d Work Items (%s → %s)\n" +
                            "Tasks: %d | Parent Tasks: %d | Child-Tasks: %d | Bugs: %d | Failed: %d",
                    successCount, totalCount, minTaskNumber, maxTaskNumber,
                    tCount, ptCount, ctCount, bCount, failureCount
            );
        } else {
            notificationBody = "No Jira tasks were imported. All tasks may have already been imported or the CSV file may not be in the correct format.";
        }

        List<HashMap<String, String>> payload = notificationService.sendNotificationForJiraImport(team, notificationBody, accountIdOfUser, timeZone);
        taskServiceImpl.sendPushNotification(payload);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void addJiraTaskToOurApp (Task task, AddJiraTaskRequest addJiraTaskRequest, Team team, List<JiraTasks> jiraTasksList, JiraTasks jiraTask,
                                     Map<String, MappedUserDetails> userAccountIdDetailsMap, Map<Long, List<JiraTasks>> parentAndChildTaskMapping,
                                     UserAccount defaultUserAccount, Map<String, Integer> jiraIssueTypeMappedList, AtomicInteger bugsCount, AtomicInteger tasksCount,
                                     AtomicInteger childTasksCount, AtomicInteger parentTasksCount,String timeZone) throws JsonProcessingException {
        List<JiraTasks> jiraChildTasksToCreate = parentAndChildTaskMapping.get(jiraTask.getIssueId());
        Map<Long, Task> childTaskList = new HashMap<>();
        convertJiraTaskAndChildTaskDateTimeToServerTimeZone(jiraTask, jiraChildTasksToCreate, timeZone);
        setWorkFlowStatusInWorkItems(addJiraTaskRequest, task, jiraTask, jiraChildTasksToCreate, childTaskList);
        if (task.getFkWorkflowTaskStatus() == null) {
            throw new ValidationFailedException("Status of this work item is not valid");
        }
        setEstimateInWorkItems(addJiraTaskRequest, task, jiraTask, jiraChildTasksToCreate, childTaskList);
        setPriorityInWorkItems(task, jiraTask, jiraChildTasksToCreate, childTaskList);
        getAndSetAssignedToInWorkItems(task, jiraTask, jiraChildTasksToCreate, childTaskList, userAccountIdDetailsMap, defaultUserAccount);
        getAndSetDateAndTimeInWorkItems(task, jiraTask, jiraChildTasksToCreate, childTaskList, team, timeZone);
        getAndSetEffortAndPercentageTaskCompleted(addJiraTaskRequest, task, jiraTask, jiraChildTasksToCreate, childTaskList);
        getAndSetAllTheMandatoryField(addJiraTaskRequest, task, jiraTask, jiraChildTasksToCreate, childTaskList, team, userAccountIdDetailsMap, defaultUserAccount, jiraIssueTypeMappedList);

        // Set task number and identifier in task
        getAndSetTaskNumberAndIdentifier(task, team);
        task.setCurrentActivityIndicator(0);
        if (task.getIsBug()) {
            if (Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatusId(), Constants.WorkFlowStatusTeamTaskStatusId.COMPLETED)) {
                task.setEnvironmentId(2);
                task.setSeverityId(3);
                task.setPlaceOfIdentification(PlaceOfIdentification.INTERNAL);
                task.setResolutionId(4);
                task.setStepsTakenToComplete("Code changes");
                task.setRcaId(Constants.RCAEnum.RCA_DOES_NOT_REQUIRED.getTypeId());
            }
            else {
                task.setEnvironmentId(2);
                task.setSeverityId(3);
            }
        }
        taskRepository.save(task);
        attachJiraFilesSimple(task, jiraTask, addJiraTaskRequest, userAccountIdDetailsMap);
        addJiraCommentsSimple(task, jiraTask, addJiraTaskRequest, userAccountIdDetailsMap, timeZone);
        setTaskInJiraToOurAppMappingTable(task, jiraTask, team);

        if (Objects.equals(Constants.TaskTypes.PARENT_TASK, task.getTaskTypeId())) {
            parentTasksCount.incrementAndGet();
        } else if (Objects.equals(Constants.TaskTypes.BUG_TASK, task.getTaskTypeId())) {
            bugsCount.incrementAndGet();
        } else if (Objects.equals(Constants.TaskTypes.TASK, task.getTaskTypeId())) {
            tasksCount.incrementAndGet();
        }
        if (!childTaskList.isEmpty()) {
            for (JiraTasks jiraChildTask : jiraChildTasksToCreate) {
                Task childTask = childTaskList.get(jiraChildTask.getIssueId());
                if (childTask != null) {
                    childTask.setParentTaskId(task.getTaskId());
                    getAndSetTaskNumberAndIdentifier(childTask, team);
                    childTask.setCurrentActivityIndicator(0);
                    taskRepository.save(childTask);
                    attachJiraFilesSimple(childTask, jiraChildTask, addJiraTaskRequest, userAccountIdDetailsMap);
                    addJiraCommentsSimple(childTask, jiraChildTask, addJiraTaskRequest, userAccountIdDetailsMap, timeZone);

                    setTaskInJiraToOurAppMappingTable(childTask, jiraChildTask, team);
                    childTasksCount.incrementAndGet();
                }
            }
        }
    }

    public WorkflowTypeStatusOfOurAppAndJira getJiraCustomWorkflowStatus(List<JiraTasks> jiraTaskList) {
        WorkflowTypeStatusOfOurAppAndJira workflowTypeStatusOfOurAppAndJira = new WorkflowTypeStatusOfOurAppAndJira();
        List<WorkflowTaskStatusIdTypeState> workflowTaskStatusIdTypeStateList = workFlowTaskStatusRepository.getWorkflowTaskStatusIdTypeState(List.of(Constants.TEAM_WORK_FLOW_TYPE_ID));
        Set<Integer> workflowStatusToRemove = new HashSet<>(Arrays.asList(Constants.WorkFlowStatusTeamTaskStatusId.BLOCKED, Constants.WorkFlowStatusTeamTaskStatusId.ON_HOLD, Constants.WorkFlowStatusTeamTaskStatusId.DELETED));
        workflowTaskStatusIdTypeStateList.removeIf(item ->
                item.getWorkflowTaskStatusId() != null && workflowStatusToRemove.contains(item.getWorkflowTaskStatusId())
        );

        List<String> jiraCustomWorkFlowStatusList = new ArrayList<>();
        List<String> jiraWorkFlowStatusList = Constants.JIRA_STATUS_LIST;
        for (JiraTasks jiraTask : jiraTaskList) {
            if (jiraTask != null && jiraTask.getStatus() != null && !jiraWorkFlowStatusList.contains(jiraTask.getStatus()) && !jiraCustomWorkFlowStatusList.contains(jiraTask.getStatus())) {
                jiraCustomWorkFlowStatusList.add(jiraTask.getStatus());
            }
        }

        workflowTypeStatusOfOurAppAndJira.setWorkflowTaskStatusIdTypeStateList(workflowTaskStatusIdTypeStateList);
        workflowTypeStatusOfOurAppAndJira.setJiraCustomWorkFlowStatusList(jiraCustomWorkFlowStatusList);
        return workflowTypeStatusOfOurAppAndJira;
    }

    public List<JiraUsers> parseJiraUserCsv(MultipartFile file) throws Exception {
        List<JiraUsers> jiraUsersList = new ArrayList<>();
        Set<String> uniqueUserIds = new HashSet<>();

        try (CSVReader csvReader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            List<String[]> rows = csvReader.readAll();

            if (rows.isEmpty()) {
                throw new ValidationFailedException("CSV file is empty.");
            }

            String[] headers = rows.get(0);
            List<String> jiraHeadersList = Constants.JIRA_USER_FILE_HEADERS;
            Set<String> headerSet = new HashSet<>(Arrays.asList(headers));

            // Check if any important field is missing in csv
            for (String jiraHeader : jiraHeadersList) {
                if (!headerSet.contains(jiraHeader)) {
                    throw new ValidationFailedException("In uploaded jira file required header: " + jiraHeader + " is missing");
                }
            }

            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                JiraUsers jiraUsersDto = new JiraUsers();

                String tempUserId = null;

                for (int j = 0; j < headers.length && j < row.length; j++) {
                    String header = headers[j];
                    String value = parseField(row[j]);

                    if (value != null) {
                        switch (header) {
                            case "User id":
                                tempUserId = value;
                                jiraUsersDto.setUserId(value);
                                break;
                            case "User name":
                                jiraUsersDto.setUserName(value);
                                break;
                            case "email":
                                jiraUsersDto.setEmail(value);
                                break;
                            case "User status":
                                if (value.equalsIgnoreCase("Active")) {
                                    jiraUsersDto.setStatus(true);
                                } else {
                                    jiraUsersDto.setStatus(false);
                                }
                                break;
                        }
                    }
                }

                if (tempUserId == null || uniqueUserIds.contains(tempUserId)) {
                    continue;
                }

                uniqueUserIds.add(tempUserId);

                if (!headerSet.contains("User status") || jiraUsersDto.getStatus() == null) {
                    jiraUsersDto.setStatus(true);
                }

                jiraUsersList.add(jiraUsersDto);
            }
        }

        return jiraUsersList;
    }

    public importedJiraUser importUserFromJiraFile(List<JiraUsers> jiraUsersList, Long teamId, User adminUser, String timeZone) {
        importedJiraUser importedJiraUsers = new importedJiraUser();
        List<JiraUsers> successList = new ArrayList<>();
        List<JiraUsers> failureList = new ArrayList<>();

        Team team = getValidTeam(teamId);

        for (JiraUsers jiraUser : jiraUsersList) {
            try {
                importSingleJiraUser(jiraUser, team, adminUser, timeZone, successList, failureList);
            } catch (Exception e) {
                // already added to failure list inside
            }
        }

        importedJiraUsers.setSuccessList(successList);
        importedJiraUsers.setFailureList(failureList);

        return importedJiraUsers;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void importSingleJiraUser(JiraUsers jiraUser, Team team, User adminUser, String timeZone, List<JiraUsers> successList, List<JiraUsers> failureList) {
        try {
            User user = userRepository.findByPrimaryEmail(jiraUser.getEmail());

            if (user == null) {
                validateEmailOfJiraUser (jiraUser, team);
                user = createNewUser(jiraUser, adminUser, timeZone);
                UserAccount userAccount = createUserAccount(user, team, jiraUser);
                addUserToTeam(userAccount, team, jiraUser);
                triggerVerificationEmailIfNeeded(jiraUser, team);
            } else {
                handleExistingUser(user, team, jiraUser);
            }

            successList.add(new JiraUsers(jiraUser.getUserId(), jiraUser.getUserName(), jiraUser.getEmail(), jiraUser.getStatus(), "User is successfully added and email is sent if not present or inactive or active and not verified"));
        } catch (Exception e) {
            failureList.add(new JiraUsers(jiraUser.getUserId(), jiraUser.getUserName(), jiraUser.getEmail(), jiraUser.getStatus(), e.getMessage()));
            throw e; // important so transaction can rolled back
        }
    }


    private Team getValidTeam(Long teamId) {
        Team team = teamRepository.findByTeamId(teamId);
        if (team == null) {
            throw new ValidationFailedException("Team does not exist or it is deleted");
        }
        return team;
    }

    private User createNewUser(JiraUsers jiraUser, User adminUser, String timeZone) {
        User user = new User();
        HashMap<String, String> nameMap = getFirstAndLastName(jiraUser.getUserName());
        user.setFirstName(nameMap.get("firstName"));
        user.setLastName(nameMap.get("lastName"));
        user.setPrimaryEmail(jiraUser.getEmail());
        user.setFkCountryId(adminUser.getFkCountryId());
        user.setTimeZone(timeZone);
        return userRepository.save(user);
    }

    private UserAccount createUserAccount(User user, Team team, JiraUsers jiraUser) {
        validateEmailOfJiraUser (jiraUser, team);
        UserPreference userPreferenceOfUser = userPreferenceRepository.findByUserId(user.getUserId());
        if (userPreferenceOfUser == null) {
            userPreferenceRepository.save(new UserPreference(user.getUserId(), team.getFkOrgId().getOrgId(), team.getFkProjectId().getProjectId(), team.getTeamId(), com.tse.core_application.model.Constants.NOTIFICATION_CATEGORY_IDS));
        }
        else {
            if (userPreferenceOfUser.getOrgId() == null || userPreferenceOfUser.getProjectId() == null || userPreferenceOfUser.getTeamId() == null) {
                userPreferenceOfUser.setOrgId(team.getFkOrgId().getOrgId());
                userPreferenceOfUser.setProjectId(team.getFkProjectId().getProjectId());
                userPreferenceOfUser.setTeamId(team.getTeamId());
            }
            userPreferenceRepository.save(userPreferenceOfUser);
        }
        UserAccount account = new UserAccount();
        account.setFkUserId(user);
        account.setEmail(jiraUser.getEmail());
        account.setOrgId(team.getFkOrgId().getOrgId());
        account.setIsVerified(false);
        if (jiraUser.getStatus() != null && !jiraUser.getStatus()) {
            account.setIsActive(false);
        }
        return userAccountRepository.save(account);
    }

    private void addUserToTeam(UserAccount account, Team team, JiraUsers jiraUser) {
        AccessDomain access = new AccessDomain();
        access.setAccountId(account.getAccountId());
        access.setEntityId(team.getTeamId());
        access.setEntityTypeId(Constants.EntityTypes.TEAM);
        access.setWorkflowTypeId(null);
        access.setRoleId(RoleEnum.TASK_BASIC_USER.getRoleId());
        if (jiraUser.getStatus() != null && !jiraUser.getStatus()) {
            access.setIsActive(false);
        }
        accessDomainRepository.save(access);
    }

    public void validateEmailOfJiraUser (JiraUsers jiraUsers, Team team) {
        String email = jiraUsers.getEmail();

        if (email == null || email.trim().isEmpty()) {
            throw new ValidationFailedException("Email is required to import this user.");
        }

        String lowerCaseEmail = email.trim().toLowerCase();

        int atIndex = lowerCaseEmail.indexOf('@');
        if (atIndex == -1 || atIndex == lowerCaseEmail.length() - 1) {
            throw new ValidationFailedException("Invalid email format provided: " + email);
        }
        String domain = lowerCaseEmail.substring(atIndex + 1);

        String domainName = domain.split("\\.")[0];

        List<String> restrictedDomainList = new ArrayList<>();
        restrictedDomainList.addAll(restrictedDomainsRepository.findDomainByIsOrgRegistrationAllowed(false));
        if ((restrictedDomainList.contains(domainName) || restrictedDomains.contains(domainName)) && !exceptionalRegistrationRepository.existsByEmailAndIsDeleted(lowerCaseEmail, false)) {
            throw new ValidationFailedException("Registration by the provided domain is not allowed by the system admin.");
        }
        if (blockedRegistrationRepository.existsByEmailAndOrganizationNameAndIsDeleted(jiraUsers.getEmail(), team.getFkOrgId().getOrganizationName(), false)) {
            throw new IllegalStateException("Username have been blocked by the system admin. Please contact system administrator at support@vijayi-wfh.com.");
        }
    }

    private void triggerVerificationEmailIfNeeded(JiraUsers jiraUser, Team team) {
        if (jiraUser.getStatus() == null || jiraUser.getStatus()) {
            User user = userRepository.findByPrimaryEmail(jiraUser.getEmail());
            if (user != null) {
                emailService.sendVerificationEmail(jiraUser.getEmail(), jiraUser.getUserName(), team.getFkOrgId().getOrgId(), team.getFkOrgId().getOrganizationName());
            }
        }
    }

    private void handleExistingUser(User user, Team team, JiraUsers jiraUser) {
        UserAccount activeAccount = userAccountRepository.findByOrgIdAndFkUserIdUserIdAndIsActive(team.getFkOrgId().getOrgId(), user.getUserId(), true);

        if (activeAccount != null) {
            handleActiveUserAccount(activeAccount, team, jiraUser);
        } else {
            UserAccount inactiveAccount = userAccountRepository.findByOrgIdAndFkUserIdUserIdAndIsActive(team.getFkOrgId().getOrgId(), user.getUserId(), false);

            if (inactiveAccount != null) {
                if (inactiveAccount.getIsDisabledBySams() != null && inactiveAccount.getIsDisabledBySams()) {
                    throw new ValidationFailedException("User is disable by super admin. Please contact Super admin or Org admin");
                }
                handleInactiveUserAccount(inactiveAccount, team, jiraUser);
            } else {
                UserAccount newAccount = createUserAccount(user, team, jiraUser);
                addUserToTeam(newAccount, team, jiraUser);
                triggerVerificationEmailIfNeeded(jiraUser, team);
            }
        }
    }

    private void handleActiveUserAccount(UserAccount account, Team team, JiraUsers jiraUser) {
        boolean accessExists = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdAndIsActive(Constants.EntityTypes.TEAM, team.getTeamId(), account.getAccountId(), true);

        if (!accessExists) {
            addUserToTeam(account, team, jiraUser);
        }

        if (account.getIsVerified() != null && !account.getIsVerified()) {
            // Resend OTP verification
            emailService.sendVerificationEmail(jiraUser.getEmail(), jiraUser.getUserName(), team.getFkOrgId().getOrgId(), team.getFkOrgId().getOrganizationName());
        }
    }

    private void handleInactiveUserAccount(UserAccount account, Team team, JiraUsers jiraUser) {
        if (jiraUser.getStatus() == null || jiraUser.getStatus()) {
            account.setIsActive(true);
            account.setIsVerified(false);
            userAccountRepository.save(account);
            boolean accessExists = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdAndIsActive(Constants.EntityTypes.TEAM, team.getTeamId(), account.getAccountId(), true);

            if (!accessExists) {
                addUserToTeam(account, team, jiraUser);
            }

            triggerVerificationEmailIfNeeded(jiraUser, team);
        } else {
            boolean accessExists = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdAndIsActive(Constants.EntityTypes.TEAM, team.getTeamId(), account.getAccountId(), false);

            if (!accessExists) {
                addUserToTeam(account, team, jiraUser);
            }
        }
    }

    public HashMap<String, String> getFirstAndLastName(String fullName) {
        HashMap<String, String> nameMap = new HashMap<>();

        if (fullName == null || fullName.trim().isEmpty()) {
            nameMap.put("firstName", "");
            nameMap.put("lastName", "");
            return nameMap;
        }

        String[] parts = fullName.trim().split("\\s+");
        String firstName = parts[0];
        String lastName = parts.length == 1 ? firstName : parts[parts.length - 1];

        nameMap.put("firstName", firstName);
        nameMap.put("lastName", lastName);

        return nameMap;
    }

    public String verifyAndSendOtp (VerifyJiraUserRequest verifyJiraUserRequest, String timeZone) {
        UserAccount userAccount = userAccountRepository.findByEmailAndOrgIdAndIsActive(verifyJiraUserRequest.getPrimaryEmail(), verifyJiraUserRequest.getOrgId(), true);
        Organization organization = organizationRepository.findByOrgId(verifyJiraUserRequest.getOrgId());
        if (organization == null) {
            throw new EntityNotFoundException("Organisation doesn't exist");
        }
        if (!verifyJiraUserRequest.getIsVerify() && userAccount != null && userAccount.getIsActive() && (userAccount.getIsVerified() != null && !userAccount.getIsVerified())) {
            Long accountIdOfOrgAdmin = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(Constants.EntityTypes.ORG, verifyJiraUserRequest.getOrgId(), List.of(RoleEnum.ORG_ADMIN.getRoleId()), true).get(0).getAccountId();
            if (accountIdOfOrgAdmin != null) {
                UserAccount userAccountOfOrgAdmin = userAccountRepository.findByAccountIdAndOrgId(accountIdOfOrgAdmin, verifyJiraUserRequest.getOrgId());
                if (userAccountOfOrgAdmin != null) {
                    List<HashMap<String, String>> payload = notificationService.sendNotificationForDeclineInvite(organization, userAccount, userAccountOfOrgAdmin, timeZone);
                    taskServiceImpl.sendPushNotification(payload);
                    emailService.sendDeclinedEmailOfJiraUserImport (organization.getOrganizationName(), userAccountOfOrgAdmin, userAccount);
                }
            }

//            userAccount.setIsActive(false);
//            userAccountRepository.save(userAccount);
//            List<Long> teamIdList = teamRepository.findTeamIdsByOrgId(verifyJiraUserRequest.getOrgId());
//            List<Long> projectIdList = projectRepository.findByOrgId(verifyJiraUserRequest.getOrgId()).stream().map(ProjectIdProjectName:: getProjectId).collect(Collectors.toList());
//            accessDomainRepository.deactivateUserAllAccessDomainsInAllTeams(userAccount.getAccountId());
//            accessDomainRepository.deactivateUserAllAccessDomainsInAllProjects(userAccount.getAccountId());
//            UserPreference userPreferenceOfUser = userPreferenceRepository.findByUserId(userAccount.getFkUserId().getUserId());
//            if (userPreferenceOfUser != null) {
//
//                if (userPreferenceOfUser.getOrgId() != null && Objects.equals(userPreferenceOfUser.getOrgId(), verifyJiraUserRequest.getOrgId())) {
//                    userPreferenceOfUser.setOrgId(null);
//                }
//                if (userPreferenceOfUser.getProjectId() != null && projectIdList.contains(userPreferenceOfUser.getProjectId())) {
//                    userPreferenceOfUser.setProjectId(null);
//                }
//                if (userPreferenceOfUser.getTeamId() != null && teamIdList.contains(userPreferenceOfUser.getTeamId())) {
//                    userPreferenceOfUser.setTeamId(null);
//                }
//                userPreferenceRepository.save(userPreferenceOfUser);
//            }
            return  "The organization admin has been notified to remove your account";
        }
        if (userAccount == null) {
            throw new EntityNotFoundException("User account does not exist or not active");
        }
        else if (!userAccount.getIsActive()){
            throw new ValidationFailedException("Inactive account can't be verified");
        }
        else if (userAccount.getIsVerified() == null || userAccount.getIsVerified()) {
            throw new ValidationFailedException("This account is already verified");
        }
        String ownerEmail = null;
        Otp otp = otpService.putOtp(CommonUtils.getRedisKeyForOtp(verifyJiraUserRequest.getPrimaryEmail(), verifyJiraUserRequest.getDeviceUniqueIdentifier()));
        if (entityPreferenceRepository.existsByEntityTypeIdAndEntityIdInAndShouldOtpSendToOrgAdmin (com.tse.core_application.model.Constants.EntityTypes.ORG, List.of(organization.getOrgId()), true)) {
            ownerEmail = organization.getOwnerEmail();
        }
        emailService.sendOtp(verifyJiraUserRequest.getPrimaryEmail(), otp.getOtp(), emailSubject, ownerEmail, true);
        return "Otp sent successfully";
    }

    public void verifyAndActivateUserAccount (VerifyJiraUserRequest verifyJiraUserRequest, String timeZone) {
        UserAccount userAccount = userAccountRepository.findByEmailAndOrgIdAndIsActive(verifyJiraUserRequest.getPrimaryEmail(), verifyJiraUserRequest.getOrgId(), true);
        if (userAccount == null) {
            throw new EntityNotFoundException("User account does not exist or not active");
        }
        else if (!userAccount.getIsActive()){
            throw new ValidationFailedException("Inactive account can't be verified");
        }
        else if (userAccount.getIsVerified() == null || userAccount.getIsVerified()) {
            throw new ValidationFailedException("This account is already verified");
        }
        String resp = otpService.verifyOtp(verifyJiraUserRequest.getDeviceUniqueIdentifier(), verifyJiraUserRequest.getPrimaryEmail(), verifyJiraUserRequest.getOtp());

        if (resp.equals(com.tse.core_application.constants.Constants.SUCCESS)) {
            User user = userRepository.findByUserId(userAccount.getFkUserId().getUserId());
            user.setTimeZone(timeZone);
            userRepository.save(user);

            userAccount.setIsVerified(true);
            userAccountRepository.save(userAccount);
        }
        else {
            throw new ValidationFailedException("Invalid otp");
        }
    }

    public void validateUserIsAdminOrNot (String accountIds, Long teamId) {
        List<Long> accountIdList = CommonUtils.convertToLongList(accountIds);
        Team team = teamRepository.findByTeamId(teamId);
        if (team == null) {
            throw new ValidationFailedException("Team does not exist or it is deleted");
        }
        if (!accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.ORG, team.getFkOrgId().getOrgId(), accountIdList ,List.of(RoleEnum.ORG_ADMIN.getRoleId()), true) &&
            !accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.PROJECT, team.getFkProjectId().getProjectId(), accountIdList ,List.of(RoleEnum.PROJECT_ADMIN.getRoleId()), true)) {
            throw new ValidationFailedException("You are not authorised to import jira user or task");
        }
    }

    public void attachJiraFilesSimple(Task task, JiraTasks jiraTask, AddJiraTaskRequest addJiraTaskRequest, Map<String, MappedUserDetails> userAccountIdDetailsMap) throws JsonProcessingException {

        if (addJiraTaskRequest.getEmailToFetchAttachment() == null || addJiraTaskRequest.getJiraToken() == null) {
            return;
        }
        String jiraEmail = addJiraTaskRequest.getEmailToFetchAttachment();
        String jiraToken = addJiraTaskRequest.getJiraToken();
        if (jiraTask.getAttachments() == null || jiraTask.getAttachments().isEmpty()) return;

        Long defaultUploaderAccountId = addJiraTaskRequest.getDefaultAccountIdAssigned();

        for (JiraAttachment jiraAttachment : jiraTask.getAttachments()) {
            try {
                Long uploaderAccountId = resolveUploaderAccount(jiraAttachment.getUploaderJiraId(), userAccountIdDetailsMap, task.getFkAccountIdAssigned(), defaultUploaderAccountId);
                byte[] fileData = downloadJiraAttachment(jiraAttachment.getUrl(), jiraEmail, jiraToken);

                Organization organization = organizationRepository.findByOrgId(task.getFkOrgId().getOrgId());
                Long allowedFileSize = entityPreferenceService.getAllowedFileSizeForEntity(Constants.EntityTypes.ORG, organization.getOrgId());

                if (fileData.length > allowedFileSize) continue;

                long updatedQuota = (organization.getUsedMemoryQuota() != null ? organization.getUsedMemoryQuota() : 0L) + fileData.length;

                if (organization.getMaxMemoryQuota() != null && organization.getMaxMemoryQuota() > 0 && updatedQuota > organization.getMaxMemoryQuota()) {
                    continue;
                }

                organization.setUsedMemoryQuota(updatedQuota);
                organizationRepository.save(organization);

                TaskAttachment attachment = new TaskAttachment();
                attachment.setTaskId(task.getTaskId());
                attachment.setFileName(jiraAttachment.getFileName());
                attachment.setFileContent(fileData);
                attachment.setFileStatus(com.tse.core_application.constants.Constants.FileAttachmentStatus.A);
                attachment.setFileSize((double) fileData.length);
                attachment.setUploaderAccountId(uploaderAccountId);
                attachment.setFileType("application/octet-stream");

                taskAttachmentRepository.save(attachment);
            } catch (Exception e) {
                logger.error("Attachment skipped for taskId = " + task.getTaskNumber() + " and file = " + jiraAttachment.getFileName() + " due to error: " + e.getMessage());
            }
        }

        List<FileMetadata> allActiveTaskAttachmentsFound = taskAttachmentRepository.findFileMetadataByTaskIdAndFileStatus(task.getTaskId(), com.tse.core_application.constants.Constants.FileAttachmentStatus.A);

        taskServiceImpl.updateTaskAttachmentsByTaskId(objectMapper.writeValueAsString(allActiveTaskAttachmentsFound), task.getTaskId());
    }

    private Long resolveUploaderAccount(String jiraUploaderId, Map<String, MappedUserDetails> userAccountIdDetailsMap, UserAccount assignedTo, Long defaultAccountId) {
        if (userAccountIdDetailsMap.containsKey(jiraUploaderId)) {
            return userAccountIdDetailsMap.get(jiraUploaderId).getUserAccount().getAccountId();
        }
        if (assignedTo != null) {
            return assignedTo.getAccountId();
        }
        return defaultAccountId;
    }

    public static byte[] downloadJiraAttachment(String fileUrl, String email, String token) throws IOException {
        URL url = new URL(fileUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        String basicAuth = "Basic " + Base64.getEncoder().encodeToString((email + ":" + token).getBytes(StandardCharsets.UTF_8));
        connection.setRequestProperty("Authorization", basicAuth);
        connection.setRequestMethod("GET");
        connection.setDoInput(true);

        try (InputStream inputStream = connection.getInputStream()) {
            return inputStream.readAllBytes();
        }
    }

    public JiraIssueTypeResponse getCustomJiraIssueType(List<JiraTasks> jiraTaskList) {
        JiraIssueTypeResponse jiraIssueTypeResponse = new JiraIssueTypeResponse();
        List<JiraIssueTypeMapping> jiraIssueTypeMappingList = getJiraIssueTypeListForMapping(jiraTaskList);
        List<TaskTypeDTO> taskTypeDTOList = new ArrayList<>();
        taskTypeDTOList.add(new TaskTypeDTO(Constants.taskTypeMap.get(Constants.TaskTypes.TASK), Constants.TaskTypes.TASK));
        taskTypeDTOList.add(new TaskTypeDTO(Constants.taskTypeMap.get(Constants.TaskTypes.BUG_TASK), Constants.TaskTypes.BUG_TASK));
        jiraIssueTypeResponse.setJiraIssueTypeMappingList(jiraIssueTypeMappingList);
        jiraIssueTypeResponse.setTaskTypeDTOList(taskTypeDTOList);
        return jiraIssueTypeResponse;
    }

    private static List<JiraIssueTypeMapping> getJiraIssueTypeListForMapping(List<JiraTasks> jiraTaskList) {
        List<JiraIssueTypeMapping> jiraIssueTypeMappingList = new ArrayList<>();
        HashSet<String> alreadyPresentIssueType = new HashSet<>();
        for (JiraTasks jiraTask : jiraTaskList) {
            if (jiraTask.getIssueType() != null && !alreadyPresentIssueType.contains(jiraTask.getIssueType())) {
                JiraIssueTypeMapping jiraIssueTypeMapping = new JiraIssueTypeMapping();
                jiraIssueTypeMapping.setIssueType(jiraTask.getIssueType());
                jiraIssueTypeMappingList.add(jiraIssueTypeMapping);
                alreadyPresentIssueType.add(jiraTask.getIssueType());
            }
        }
        return jiraIssueTypeMappingList;
    }

    public void addJiraCommentsSimple(Task task, JiraTasks jiraTask, AddJiraTaskRequest request,
                                      Map<String, MappedUserDetails> userMap, String timeZone) {
        if (jiraTask.getComments() == null || jiraTask.getComments().isEmpty()) return;

        for (JiraComment jc : jiraTask.getComments()) {
            try {
                // Resolve uploader account ID
                Long accountId = resolveUploaderAccount(
                        jc.getUploaderJiraId(),
                        userMap,
                        task.getFkAccountIdAssigned(),
                        request.getDefaultAccountIdAssigned()
                );

                // Sanitize/convert Jira comment message
                String message = convertJiraMarkupToHtml(jc.getMessage());
                if (message == null || message.trim().isEmpty()) continue;

                // Create Comment object
                Comment comment = new Comment();
                comment.setComment(message);
                comment.setPostedByAccountId(accountId);
                comment.setCommentsTags(new String[]{"COMMENT"});
                comment.setTask(task);
                comment.setCreatedDateTime(
                        Timestamp.valueOf(jc.getDate() != null
                                ? DateTimeUtils.convertUserDateToServerTimezone(jc.getDate(), timeZone)
                                : LocalDateTime.now())
                );

                // Handle commentId logic
                Long taskId = task.getTaskId();
                Long commentId = commentService.getCommentIdFromTaskTable(taskId); // Check if this task already has a commentId
                if (commentId == null) {
                    Long assignCommentId = commentService.getMaxCommentId(); // Generate a new commentId
                    comment.setCommentId(assignCommentId);
                    commentRepository.save(comment);
                    taskRepository.setTaskCommentIdByTaskId(assignCommentId, taskId); // Set the commentId in Task
                } else {
                    comment.setCommentId(commentId);
                    commentRepository.save(comment);
                }

            } catch (Exception e) {
                logger.error("Comment skipped for task number = " + task.getTaskNumber() + " due to error: " + e.getMessage());
            }
        }
    }

    private String convertJiraMarkupToHtml(String text) {
        if (text == null) return "";
        // Convert [url|label] or [url|label|smart-link] to HTML links
        return text.replaceAll("\\[(https?://[^\\]|]+)\\|([^\\]|]+)(\\|[^\\]]+)?\\]",
                "<a href=\"$1\" target=\"_blank\">$2</a>");
    }

    public List<JiraProjectResponse> fetchAllJiraProjects(JiraConnectionRequest getJiraProjectsRequest) {
        String rawSiteUrl = getJiraProjectsRequest.getSiteUrl().trim();
        String email = getJiraProjectsRequest.getJiraEmail().trim();
        String token = getJiraProjectsRequest.getJiraToken().trim();

        String siteUrl = rawSiteUrl.startsWith("http") ? rawSiteUrl : "https://" + rawSiteUrl;
        if (siteUrl.endsWith("/")) siteUrl = siteUrl.substring(0, siteUrl.length() - 1);

        List<JiraProjectResponse> projects = new ArrayList<>();
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper mapper = new ObjectMapper();

        int startAt = 0;
        int maxResults = 50;
        boolean isLast = false;

        String apiUrl = siteUrl + "/rest/api/3/project/search";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(email, token);

        while (!isLast) {
            String urlWithParams = apiUrl + "?startAt=" + startAt + "&maxResults=" + maxResults;
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        urlWithParams, HttpMethod.GET, requestEntity, String.class);

                if (!response.getStatusCode().is2xxSuccessful()) {
                    throw new ValidationFailedException("Failed to fetch Jira projects. Status: " + response.getStatusCode());
                }

                JsonNode root = mapper.readTree(response.getBody());
                JsonNode values = root.path("values");
                int resultsFetched = values.size();
                isLast = root.path("isLast").asBoolean();

                for (JsonNode projectNode : values) {
                    JiraProjectResponse project = new JiraProjectResponse();
                    project.setProjectId(projectNode.path("id").asText());
                    project.setProjectCode(projectNode.path("key").asText());
                    project.setProjectName(projectNode.path("name").asText());
                    project.setSelfUrl(projectNode.path("self").asText());
                    project.setExpand(projectNode.path("expand").asText());
                    project.setProjectTypeKey(projectNode.path("projectTypeKey").asText());

                    JsonNode avatarUrls = projectNode.path("avatarUrls");
                    project.setAvatarUrl(avatarUrls.path("48x48").asText());

                    projects.add(project);
                }

                startAt += resultsFetched;

            } catch (Exception e) {
                logger.error("Error fetching Jira projects from {}: {}", urlWithParams, e.getMessage());
                throw new ValidationFailedException("Unable to fetch Jira projects. Please check your Jira credentials or URL.");
            }
        }

        return projects;
    }

    public List<JiraUserResponse> getAllRealUsersFromJira(GetJiraUsersDetailsRequest request) {
        if (request == null || request.getConnection() == null) {
            throw new ValidationFailedException("Connection details are required");
        }

        String rawSiteUrl = request.getConnection().getSiteUrl().trim();
        String email = request.getConnection().getJiraEmail().trim();
        String token = request.getConnection().getJiraToken().trim();

        String siteUrl = rawSiteUrl.startsWith("http") ? rawSiteUrl : "https://" + rawSiteUrl;
        if (siteUrl.endsWith("/")) siteUrl = siteUrl.substring(0, siteUrl.length() - 1);

        List<JiraUserResponse> users = new ArrayList<>();
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper mapper = new ObjectMapper();

        int startAt = 0;
        int maxResults = 1000;
        boolean morePages = true;

        while (morePages) {
            String url = siteUrl + "/rest/api/3/users/search?startAt=" + startAt + "&maxResults=" + maxResults;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBasicAuth(email, token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response;
            try {
                response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            } catch (Exception ex) {
                throw new ValidationFailedException("Failed to connect to Jira. Check your token/site URL.");
            }

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ValidationFailedException("Failed to fetch users from Jira.");
            }

            JsonNode rootNode;
            try {
                rootNode = mapper.readTree(response.getBody());
            } catch (IOException e) {
                throw new ValidationFailedException("Invalid response from Jira.");
            }

            if (!rootNode.isArray()) {
                throw new ValidationFailedException("Unexpected format in Jira response.");
            }

            int resultsFetched = rootNode.size();

            for (JsonNode userNode : rootNode) {
                String accountType = userNode.path("accountType").asText("");
                boolean isActive = userNode.path("active").asBoolean(false);

                if (!"atlassian".equalsIgnoreCase(accountType)) continue;

                JiraUserResponse user = new JiraUserResponse();
                user.setAccountId(userNode.path("accountId").asText());
                user.setDisplayName(userNode.path("displayName").asText());
                user.setEmailAddress(userNode.path("emailAddress").asText(null));
                user.setAvatarUrl(userNode.path("avatarUrls").path("48x48").asText());
                user.setStatus(isActive);

                users.add(user);
            }

            morePages = resultsFetched == maxResults;
            startAt += resultsFetched;
        }

        return users;
    }

    public JiraIssueTypeResponse getCustomJiraIssueTypesFromJira(GetJiraUsersDetailsRequest request) {
        String rawSiteUrl = request.getConnection().getSiteUrl().trim();
        String email = request.getConnection().getJiraEmail().trim();
        String token = request.getConnection().getJiraToken().trim();
        String projectId = request.getProjectId();

        String siteUrl = rawSiteUrl.startsWith("http") ? rawSiteUrl : "https://" + rawSiteUrl;
        if (siteUrl.endsWith("/")) {
            siteUrl = siteUrl.substring(0, siteUrl.length() - 1);
        }

        String url = siteUrl + "/rest/api/3/issuetype/project?projectId=" + projectId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(email, token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response;
        try {
            RestTemplate restTemplate = new RestTemplate();
            response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        } catch (Exception ex) {
            throw new ValidationFailedException("Unable to reach Jira. Verify credentials or project ID.");
        }

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new ValidationFailedException("Failed to fetch Jira issue types.");
        }

        List<JiraIssueTypeMapping> mappingList = new ArrayList<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode issueTypesArray = mapper.readTree(response.getBody());

            for (JsonNode node : issueTypesArray) {
                boolean isSubTask = node.path("subtask").asBoolean(false);
                if (isSubTask) continue;

                String name = node.path("name").asText();
                if (!isValidIssueType(name)) continue;

                JiraIssueTypeMapping mapping = new JiraIssueTypeMapping();
                mapping.setIssueType(name);
                mappingList.add(mapping);
            }

        } catch (Exception e) {
            throw new ValidationFailedException("Failed to parse Jira issue types.");
        }

        List<TaskTypeDTO> taskTypeList = new ArrayList<>();
        taskTypeList.add(new TaskTypeDTO(Constants.taskTypeMap.get(Constants.TaskTypes.TASK), Constants.TaskTypes.TASK));
        taskTypeList.add(new TaskTypeDTO(Constants.taskTypeMap.get(Constants.TaskTypes.BUG_TASK), Constants.TaskTypes.BUG_TASK));

        JiraIssueTypeResponse result = new JiraIssueTypeResponse();
        result.setJiraIssueTypeMappingList(mappingList);
        result.setTaskTypeDTOList(taskTypeList);
        return result;
    }

    private boolean isValidIssueType(String name) {
        if (name == null) return false;
        return !Constants.JIRA_INVALID_ISSUE_TYPE_LIST.contains(name.toLowerCase());
    }

    public WorkflowTypeStatusOfOurAppAndJira getJiraCustomWorkflowStatusFromToken(GetJiraUsersDetailsRequest request) {
        String rawSiteUrl = request.getConnection().getSiteUrl().trim();
        String email = request.getConnection().getJiraEmail().trim();
        String token = request.getConnection().getJiraToken().trim();
        String projectId = request.getProjectId();

        String siteUrl = rawSiteUrl.startsWith("http") ? rawSiteUrl : "https://" + rawSiteUrl;
        if (siteUrl.endsWith("/")) siteUrl = siteUrl.substring(0, siteUrl.length() - 1);

        String url = siteUrl + "/rest/api/3/project/" + projectId + "/statuses";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(email, token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response;
        try {
            RestTemplate restTemplate = new RestTemplate();
            response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        } catch (Exception ex) {
            throw new ValidationFailedException("Unable to connect to Jira. Please verify site URL and credentials.");
        }

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new ValidationFailedException("Failed to fetch Jira workflow statuses.");
        }

        Set<String> customStatusSet = new HashSet<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            for (JsonNode workflowScheme : root) {
                JsonNode statuses = workflowScheme.path("statuses");
                for (JsonNode statusNode : statuses) {
                    String statusName = statusNode.path("name").asText();
                    if (!Constants.JIRA_STATUS_LIST.contains(statusName)) {
                        customStatusSet.add(statusName);
                    }
                }
            }
        } catch (Exception e) {
            throw new ValidationFailedException("Failed to parse Jira workflow statuses.");
        }

        List<String> customStatusList = new ArrayList<>(customStatusSet);

        List<WorkflowTaskStatusIdTypeState> workflowStatuses = workFlowTaskStatusRepository.getWorkflowTaskStatusIdTypeState(
                List.of(Constants.TEAM_WORK_FLOW_TYPE_ID)
        );
        Set<Integer> removeIds = Set.of(Constants.WorkFlowStatusTeamTaskStatusId.BLOCKED,
                Constants.WorkFlowStatusTeamTaskStatusId.ON_HOLD,
                Constants.WorkFlowStatusTeamTaskStatusId.DELETED);
        workflowStatuses.removeIf(status -> status.getWorkflowTaskStatusId() != null && removeIds.contains(status.getWorkflowTaskStatusId()));

        WorkflowTypeStatusOfOurAppAndJira result = new WorkflowTypeStatusOfOurAppAndJira();
        result.setJiraCustomWorkFlowStatusList(customStatusList);
        result.setWorkflowTaskStatusIdTypeStateList(workflowStatuses);
        return result;
    }

    public List<JiraUsers> fetchAllUsersFromJiraProjectUsingToken(GetJiraUsersDetailsRequest request) {
        String rawSiteUrl = request.getConnection().getSiteUrl().trim();
        String email = request.getConnection().getJiraEmail().trim();
        String token = request.getConnection().getJiraToken().trim();

        String siteUrl = rawSiteUrl.startsWith("http") ? rawSiteUrl : "https://" + rawSiteUrl;
        if (siteUrl.endsWith("/")) {
            siteUrl = siteUrl.substring(0, siteUrl.length() - 1);
        }

        List<JiraUsers> users = new ArrayList<>();
        RestTemplate restTemplate = new RestTemplate();

        int startAt = 0;
        int maxResults = 1000;
        boolean morePages = true;

        while (morePages) {
            String url = siteUrl + "/rest/api/3/users/search?startAt=" + startAt + "&maxResults=" + maxResults;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBasicAuth(email, token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response;
            try {
                response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            } catch (Exception e) {
                throw new ValidationFailedException("Failed to connect to Jira. Check your token/site URL.");
            }

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ValidationFailedException("Failed to fetch users from Jira.");
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode;
            try {
                rootNode = mapper.readTree(response.getBody());
            } catch (IOException e) {
                throw new ValidationFailedException("Invalid response from Jira while parsing users.");
            }

            int pageSize = rootNode.size();
            for (JsonNode userNode : rootNode) {
                String accountType = userNode.path("accountType").asText(null);

                if (!"atlassian".equalsIgnoreCase(accountType)) continue;

                JiraUsers user = new JiraUsers();
                user.setUserId(userNode.path("accountId").asText());
                user.setUserName(userNode.path("displayName").asText());
                user.setEmail(userNode.path("emailAddress").asText(null));
                user.setStatus(userNode.path("active").asBoolean(true));
                users.add(user);
            }

            morePages = pageSize == maxResults;
            startAt += pageSize;
        }

        return users;
    }

    public List<JiraTaskToCreate> getAllJiraTaskWithIdAndTitle(GetJiraTaskIdAndTitleUsingTokenRequest request) {
        if (request == null || request.getGetJiraUsersRequest() == null)
            throw new ValidationFailedException("Request cannot be null");

        String rawSiteUrl = request.getGetJiraUsersRequest().getConnection().getSiteUrl().trim();
        String email = request.getGetJiraUsersRequest().getConnection().getJiraEmail().trim();
        String token = request.getGetJiraUsersRequest().getConnection().getJiraToken().trim();
        String projectId = request.getGetJiraUsersRequest().getProjectId().trim();

        String siteUrl = rawSiteUrl.startsWith("http") ? rawSiteUrl : "https://" + rawSiteUrl;
        if (siteUrl.endsWith("/")) siteUrl = siteUrl.substring(0, siteUrl.length() - 1);

        Map<String, Integer> allowedIssueTypes = getJiraIssueTypeMappedList(request.getJiraIssueTypeMappingList());

        int total = fetchTotalTaskCountForTitle(siteUrl, email, token, projectId);
        int pageSize = 100;

        List<CompletableFuture<List<JiraTaskToCreate>>> futures = new ArrayList<>();
        for (int startAt = 0; startAt < total; startAt += pageSize) {
            final int currentStart = startAt;
            final String finalSiteUrl = siteUrl;
            final String finalEmail = email;
            final String finalToken = token;
            final String finalProjectId = projectId;

            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    return fetchJiraTitlePage(finalSiteUrl, finalEmail, finalToken, finalProjectId, currentStart, pageSize, allowedIssueTypes);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, jiraExecutor));
        }

        return futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private int fetchTotalTaskCountForTitle(String siteUrl, String email, String token, String projectId) {
        String url = siteUrl + "/rest/api/3/search";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(email, token);

        Map<String, Object> body = new HashMap<>();
        body.put("jql", "project = " + projectId);
        body.put("startAt", 0);
        body.put("maxResults", 1);
        body.put("fields", List.of("id")); // lightweight request

        try {
            HttpEntity<String> request = new HttpEntity<>(new ObjectMapper().writeValueAsString(body), headers);
            ResponseEntity<String> response = new RestTemplate().exchange(url, HttpMethod.POST, request, String.class);

            JsonNode root = new ObjectMapper().readTree(response.getBody());
            return root.path("total").asInt(0);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch Jira total task count", e);
        }
    }

    private List<JiraTaskToCreate> fetchJiraTitlePage(
            String siteUrl, String email, String token, String projectId,
            int startAt, int maxResults, Map<String, Integer> allowedIssueTypes) throws Exception {

        List<JiraTaskToCreate> tasks = new ArrayList<>();
        RestTemplate restTemplate = new RestTemplate();
        int currentStart = startAt;
        int endLimit = startAt + maxResults;

        while (currentStart < endLimit) {
            String url = siteUrl + "/rest/api/3/search";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBasicAuth(email, token);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("jql", "project = " + projectId);
            requestBody.put("startAt", currentStart);
            requestBody.put("maxResults", Math.min(maxResults, endLimit - currentStart));
            requestBody.put("fields", List.of("summary", "issuetype", "parent"));

            HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ValidationFailedException("Failed to fetch Jira title page at startAt = " + currentStart);
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode issues = root.path("issues");

            int fetched = issues.size();
            for (JsonNode issue : issues) {
                long issueId = issue.path("id").asLong();
                JsonNode fields = issue.path("fields");

                // Skip subtasks and disallowed types
                String issueTypeName = fields.path("issuetype").path("name").asText();
                if (fields.has("parent") || !allowedIssueTypes.containsKey(issueTypeName)) {
                    continue;
                }

                JiraTaskToCreate task = new JiraTaskToCreate();
                task.setIssueId(issueId);
                task.setJiraTaskTitle(processJiraTaskTitle(fields.path("summary").asText()));
                tasks.add(task);
            }

            if (fetched == 0) break;

            currentStart += fetched;
        }

        return tasks;

    }

    public List<JiraTasks> fetchAllJiraTasksUsingToken(String siteUrl, String email, String token, String projectId) throws Exception {
        siteUrl = normalizeUrl(siteUrl);
        int pageSize = 100;

        // First request to get total
        int total = fetchTotalTaskCount(siteUrl, email, token, projectId);

        List<CompletableFuture<List<JiraTasks>>> futures = new ArrayList<>();

        for (int startAt = 0; startAt < total; startAt += pageSize) {
            final int currentStart = startAt;
            final String finalSiteUrl = siteUrl;
            final String finalEmail = email;
            final String finalToken = token;
            final String finalProjectId = projectId;
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    return fetchJiraTaskPage(finalSiteUrl, finalEmail, finalToken, finalProjectId, currentStart, pageSize);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, jiraExecutor));
        }

        // Combine all pages
        List<JiraTasks> all = futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        return all;
    }

    private int fetchTotalTaskCount(String siteUrl, String email, String token, String projectId) throws Exception {
        String url = siteUrl + "/rest/api/3/search";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(email, token);

        Map<String, Object> body = new HashMap<>();
        body.put("jql", "project = " + projectId);
        body.put("startAt", 0);
        body.put("maxResults", 1);
        body.put("fields", List.of("id"));

        HttpEntity<String> request = new HttpEntity<>(new ObjectMapper().writeValueAsString(body), headers);
        ResponseEntity<String> response = new RestTemplate().exchange(url, HttpMethod.POST, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new ValidationFailedException("Failed to fetch total count");
        }

        JsonNode root = new ObjectMapper().readTree(response.getBody());
        return root.path("total").asInt(0);
    }

    private List<JiraTasks> fetchJiraTaskPage(String siteUrl, String email, String token, String projectId, int startAt, int maxResults) throws Exception {
        String url = siteUrl + "/rest/api/3/search";
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();

        List<JiraTasks> tasks = new ArrayList<>();
        int currentStart = startAt;
        int endLimit = startAt + maxResults;

        while (currentStart < endLimit) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBasicAuth(email, token);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("jql", "project = " + projectId);
            requestBody.put("startAt", currentStart);
            requestBody.put("maxResults", Math.min(maxResults, endLimit - currentStart));
            requestBody.put("fields", List.of("*all"));

            HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ValidationFailedException("Failed to fetch Jira tasks at startAt = " + currentStart);
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode issues = root.path("issues");

            int fetched = 0;
            for (JsonNode issue : issues) {
                tasks.add(parseJiraTask(issue));
                fetched++;
            }

            if (fetched == 0) break;

            currentStart += fetched;
        }

        return tasks;
    }


    private JiraTasks parseJiraTask(JsonNode issue) {
        JsonNode fields = issue.path("fields");
        JiraTasks task = new JiraTasks();

        task.setIssueId(parseIssueId(issue));
        task.setSummary(getText(fields, "summary"));
        task.setDescription(getText(fields, "description"));
        task.setIssueType(getText(fields.path("issuetype"), "name"));
        task.setParentId(getText(fields.path("parent"), "id"));
        task.setStatus(getText(fields.path("status"), "name"));
        task.setPriority(getText(fields.path("priority"), "name"));
        task.setAssignee(getText(fields.path("assignee"), "displayName"));
        task.setAssigneeId(getText(fields.path("assignee"), "accountId"));
        task.setReporter(getText(fields.path("reporter"), "displayName"));
        task.setReporterId(getText(fields.path("reporter"), "accountId"));
        task.setCreator(getText(fields.path("creator"), "displayName"));
        task.setCreatorId(getText(fields.path("creator"), "accountId"));
        task.setCreated(DateTimeUtils.parseDynamicDate(fields.path("created").asText(null)));
        task.setUpdated(DateTimeUtils.parseDynamicDate(fields.path("updated").asText(null)));
        task.setDueDate(DateTimeUtils.parseDynamicDate(fields.path("duedate").asText(null)));
        task.setResolved(DateTimeUtils.parseDynamicDate(fields.path("resolutiondate").asText(null)));
        task.setLastViewed(DateTimeUtils.parseDynamicDate(fields.path("lastViewed").asText(null)));
        task.setVotes(fields.path("votes").path("votes").asInt(0));

        task.setComments(parseComments(fields.path("comment").path("comments")));
        task.setAttachments(parseAttachments(fields.path("attachment")));
        task.setLogWorkDetails(parseWorkLogs(fields.path("worklog").path("worklogs")));
        task.setTimeSpent(fields.path("timespent").asInt(0));
        task.setOriginalEstimate(fields.path("timeoriginalestimate").asInt(0));
        task.setRemainingEstimate(fields.path("timeestimate").asInt(0));

        task.setWatchers(parseWatchers(fields.path("watches")));

        JsonNode project = fields.path("project");
        if (project != null && project.has("lead")) {
            task.setProjectLead(getText(project.path("lead"), "displayName"));
            task.setProjectLeadId(getText(project.path("lead"), "accountId"));
        }

        return task;
    }

    private List<JiraComment> parseComments(JsonNode commentsNode) {
        List<JiraComment> comments = new ArrayList<>();
        if (commentsNode != null && commentsNode.isArray()) {
            for (JsonNode comment : commentsNode) {
                JiraComment c = new JiraComment();
                c.setDate(DateTimeUtils.parseDynamicDate(comment.path("created").asText(null)));
                c.setUploaderJiraId(getText(comment.path("author"), "accountId"));

                // Process message body
                StringBuilder msgBuilder = new StringBuilder();
                JsonNode bodyContent = comment.path("body").path("content");
                if (bodyContent.isArray()) {
                    for (JsonNode content : bodyContent) {
                        JsonNode subContents = content.path("content");
                        if (subContents.isArray()) {
                            for (JsonNode sub : subContents) {
                                if (sub.has("text")) {
                                    msgBuilder.append(sub.path("text").asText());
                                }
                                if (sub.has("type") && "application".equals(sub.path("type").asText())
                                        && sub.has("attrs") && sub.path("attrs").has("url")) {
                                    msgBuilder.append(" [LINK: ").append(sub.path("attrs").path("url").asText()).append("] ");
                                }
                            }
                        }
                    }
                }

                String finalMsg = msgBuilder.toString().trim();
                if (finalMsg.length() > 4500) {
                    finalMsg = finalMsg.substring(0, 4500) + "...";
                }

                c.setMessage(finalMsg);
                comments.add(c);
            }
        }
        return comments;
    }

    private List<JiraAttachment> parseAttachments(JsonNode attArray) {
        List<JiraAttachment> attachments = new ArrayList<>();
        if (attArray != null && attArray.isArray()) {
            for (JsonNode att : attArray) {
                JiraAttachment a = new JiraAttachment();
                a.setCreated(DateTimeUtils.parseDynamicDate(att.path("created").asText(null)));
                a.setUploaderJiraId(getText(att.path("author"), "accountId"));
                a.setFileName(getText(att, "filename"));
                a.setUrl(getText(att, "content"));
                attachments.add(a);
            }
        }
        return attachments;
    }

    private List<LogWork> parseWorkLogs(JsonNode worklogsNode) {
        List<LogWork> logs = new ArrayList<>();
        if (worklogsNode != null && worklogsNode.isArray()) {
            for (JsonNode wl : worklogsNode) {
                LogWork log = new LogWork();
                log.setDate(DateTimeUtils.parseDynamicDate(wl.path("started").asText(null)));
                log.setValue(wl.path("timeSpentSeconds").asInt(0));
                logs.add(log);
            }
        }
        return logs;
    }

    private List<String> parseWatchers(JsonNode watchesNode) {
        List<String> watchers = new ArrayList<>();
        if (watchesNode != null && watchesNode.has("watchCount")) {
            JsonNode watchersArr = watchesNode.path("watchers");
            if (watchersArr != null && watchersArr.isArray()) {
                for (JsonNode watcher : watchersArr) {
                    watchers.add(getText(watcher, "displayName"));
                    // We can also collect accountIds separately if needed
                    // watchersId.add(getText(watcher, "accountId"));
                }
            }
        }
        return watchers;
    }


    private String normalizeUrl(String rawSiteUrl) {
        String url = rawSiteUrl.startsWith("http") ? rawSiteUrl : "https://" + rawSiteUrl;
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private Long parseIssueId(JsonNode issueNode) {
        try {
            return Long.parseLong(issueNode.path("id").asText());
        } catch (Exception e) {
            return null;
        }
    }

    private String getText(JsonNode node, String field) {
        return node != null && node.has(field) ? node.get(field).asText() : null;
    }

}
