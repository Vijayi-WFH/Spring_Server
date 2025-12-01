package com.tse.core_application.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tse.core_application.constants.RelationDirection;
import com.tse.core_application.constants.RoleEnum;
import com.tse.core_application.custom.model.*;
import com.tse.core_application.custom.model.Notification;
import com.tse.core_application.custom.model.childbugtask.ParentTaskResponse;
import com.tse.core_application.dto.*;
import com.tse.core_application.dto.SearchCriteria;
import com.tse.core_application.dto.duplicate_task.DuplicateTask;
import com.tse.core_application.dto.duplicate_task.DuplicateTaskRequest;
import com.tse.core_application.dto.duplicate_task.DuplicateTaskResponse;
import com.tse.core_application.exception.ForbiddenException;
import com.tse.core_application.exception.InvalidStatsRequestFilterException;
import com.tse.core_application.exception.TaskNotFoundException;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.filters.JwtRequestFilter;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.*;
import com.tse.core_application.model.User;
import com.tse.core_application.predicates.JPACustomPredicates;
import com.tse.core_application.repository.*;
import com.tse.core_application.specification.TaskSpecification;
import com.tse.core_application.utils.CommonUtils;
import com.tse.core_application.utils.DateTimeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import java.lang.reflect.Field;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.tse.core_application.predicates.JPACustomPredicates.*;
import static com.tse.core_application.utils.DateTimeUtils.convertServerDateToUserTimezone;
import static org.springframework.data.jpa.domain.Specification.where;

@Service
public class TaskService {

    private static final Logger logger = LogManager.getLogger(TaskService.class.getName());
    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskHistoryRepository taskHistoryRepository;

    @Autowired
    private StatsService statsService;

    @Autowired
    private ConfigRepository configRepository;

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private AccessDomainRepository accessDomainRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ActionService actionService;

    @Autowired
    private TaskServiceImpl taskServiceImpl;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private AccessDomainService accessDomainService;

    @Autowired
    private RoleActionService roleActionService;

    @Autowired
    private UserService userService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private TaskAttachmentRepository taskAttachmentRepository;

    @Autowired
    private WorkflowTaskStatusService workflowTaskStatusService;

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    private TeamService teamService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private NoteService noteService;
    @Autowired
    private DeliverablesDeliveredService deliverablesDeliveredService;
    @Autowired
    private WorkFlowTaskStatusRepository workFlowTaskStatusRepository;

    @Autowired
    private TimeSheetRepository timeSheetRepository;

    @Autowired
    private DependencyRepository dependencyRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private RoleActionRepository roleActionRepository;

    @Autowired
    private ActionRepository actionRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private MeetingService meetingService;

    @Autowired
    private JwtRequestFilter jwtRequestFilter;
    @Autowired
    private PersonalTaskService personalTaskService;

    @Autowired
    private AttendanceService attendanceService;

    private static List<Integer> workflowOtherStatusesListFirst;
    private static List<Integer> workflowOtherStatusesListSecond;
    private static List<Integer> workflowOtherStatusesListThird;
    private static List<Integer> workflowCompletedStatusList;

    private static Map<String, List<WorkFlowTaskStatus>> workflowStatusMap;

    @Value("${tse.search.multiplier}")
    private float searchMultiplier;
    @Value("${personal.org.id}")
    private Long personalOrgId;
    @Autowired
    private TaskSequenceRepository taskSequenceRepository;

    ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    private void initializeWorkflowStatusMap() {
        final List<WorkFlowTaskStatus> allWorkflowTaskStatuses = workflowTaskStatusService.getAllWorkflowTaskStatus();
        workflowStatusMap = workflowTaskStatusService.createWorkflowStatusMap(allWorkflowTaskStatuses);

        workflowOtherStatusesListFirst = workflowStatusMap.entrySet()
                .stream()
                .filter(entry -> {
                    String status = entry.getKey().toLowerCase();
                    return status.equals(Constants.WorkFlowTaskStatusConstants.STATUS_STARTED)
                            || status.equals(Constants.WorkFlowTaskStatusConstants.STATUS_ON_HOLD)
                            || status.equals(Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED)
                            || status.equals(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED);
                })
                .flatMap(entry -> entry.getValue().stream())
                .map(WorkFlowTaskStatus::getWorkflowTaskStatusId)
                .collect(Collectors.toList());

        workflowOtherStatusesListSecond = workflowStatusMap.entrySet()
                .stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG))
                .flatMap(entry -> entry.getValue().stream())
                .map(WorkFlowTaskStatus::getWorkflowTaskStatusId)
                .collect(Collectors.toList());

        workflowOtherStatusesListThird = workflowStatusMap.entrySet()
                .stream()
                .filter(entry -> {
                    String status = entry.getKey().toLowerCase();
                    return status.equals(Constants.WorkFlowTaskStatusConstants.STATUS_ON_HOLD)
                            || status.equals(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED)
                            || status.equals(Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED);
                })
                .flatMap(entry -> entry.getValue().stream())
                .map(WorkFlowTaskStatus::getWorkflowTaskStatusId)
                .collect(Collectors.toList());

        workflowCompletedStatusList = workflowStatusMap.entrySet()
                .stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED))
                .flatMap(entry -> entry.getValue().stream())
                .map(WorkFlowTaskStatus::getWorkflowTaskStatusId)
                .collect(Collectors.toList());
    }

    public List<Task> getTasksByAccountId(Long accountId) {
        return taskRepository.findByFkAccountIdAccountId(accountId);
    }

    /* This method is used to get the list of all the search criteria filters only for the task master. This method is
     * used for both type of the task master. i.e. My Tasks and All Tasks. */
    private List<SearchCriteria> getSearchCriteriaFiltersForTaskmaster(StatsRequest statsRequest, Map<String, List<Long>> teamPermissions, List<Long> accountIdsOfUser) {
        List<SearchCriteria> params = new ArrayList<>();

        LocalDateTime currentDate = LocalDateTime.now();

//        // Logic to add team-based and account-based conditions
//        if (statsRequest.getAccountIdAssigned() == null) {
//            params.addAll(addRoleBasedTeamIds(teamPermissions.get("teamView"), null));
//            params.addAll(addRoleBasedTeamIds(teamPermissions.get("basicUpdate"), accountIdsOfUser));
//        } else {
//            if (accountIdsOfUser.contains(statsRequest.getAccountIdAssigned())) {
//                List<Long> allTeams = new ArrayList<>();
//                allTeams.addAll(teamPermissions.get("teamView"));
//                allTeams.addAll(teamPermissions.get("basicUpdate"));
//                params.addAll(addRoleBasedTeamIds(allTeams, null));
//            } else {
//                params.addAll(addRoleBasedTeamIds(teamPermissions.get("teamView"), List.of(statsRequest.getAccountIdAssigned())));
//            }
//        }



        // todo we can create separate method which only tries to add params.
        if (statsRequest.getNoOfDays() != null) {
            LocalDateTime newEndDate = currentDate.minusDays(statsRequest.getNoOfDays());
            params.add(new SearchCriteria("taskExpEndDate", ">", newEndDate));
            params.add(new SearchCriteria("taskExpEndDate", "<", currentDate.plusMinutes(1)));
        }

        // todo we might have in query.
        if (statsRequest.getAccountIds() != null && !statsRequest.getAccountIds().isEmpty() && statsRequest.getAccountIds().size() > 0) {
            for (Long accountId : statsRequest.getAccountIds()) {
                params.add(new SearchCriteria("fkAccountIdAssigned", ":", accountId));
            }
        }

        if (statsRequest.getAccountIds() != null && statsRequest.getAccountIds().contains(Constants.UNASSIGNED_ACCOUNT_ID)) {
            params.add(new SearchCriteria("fkAccountIdAssigned", "isNull", "null"));//random value to maintain not null annotation
        }

        if (statsRequest.getAccountIdCreator() != null) {
            params.add(new SearchCriteria("fkAccountIdCreator", ":", statsRequest.getAccountIdCreator()));
        }

        if (statsRequest.getTaskTypeList() != null && !statsRequest.getTaskTypeList().isEmpty()) {
            for (Integer taskType : statsRequest.getTaskTypeList()) {
                if (Objects.equals(taskType, Constants.TaskTypes.BUG_TASK)) {
                    params.add(new SearchCriteria("isBug", ":", true));
                } else {
                    params.add(new SearchCriteria("taskTypeId", ":", taskType, true, false));
                }
            }
        }

        if (statsRequest.getOrgIds() != null && !statsRequest.getOrgIds().isEmpty()) {
            for (Long orgId : statsRequest.getOrgIds()) {
                params.add(new SearchCriteria("fkOrgId", ":", orgId, true, false));
            }
        }

        if (statsRequest.getCurrentlyScheduledTaskIndicator() != null && statsRequest.getCurrentlyScheduledTaskIndicator().equals(true)) {
            params.add(new SearchCriteria("currentlyScheduledTaskIndicator", ":", true));
        }

        if (statsRequest.getLabelIds() != null && !statsRequest.getLabelIds().isEmpty()) {
            params.add(new SearchCriteria("labels", "in", statsRequest.getLabelIds(), false, true));
        }


        if (statsRequest.getBuId() != null) {
            params.add(new SearchCriteria("buId", ":", statsRequest.getBuId()));
            String allStackTraces = StackTraceHandler.getAllStackTraces(new InvalidStatsRequestFilterException("BU"));
            logger.error("Invalid Filter: BU filter is not available for task master. ", new Throwable(allStackTraces));
            ThreadContext.clearMap();
            throw new InvalidStatsRequestFilterException("BU");
        }

        if (statsRequest.getProjectId() != null) {
            params.add(new SearchCriteria("fkProjectId", ":", statsRequest.getProjectId()));
        }

        if (statsRequest.getTeamId() != null) {
            params.add(new SearchCriteria("fkTeamId", ":", statsRequest.getTeamId()));
        }

        if (statsRequest.getSprintId() != null) {
            params.add(new SearchCriteria("sprintId", ":", statsRequest.getSprintId()));
        }

        if (statsRequest.getEpicId() != null) {
            params.add(new SearchCriteria("fkEpicId", ":", statsRequest.getEpicId()));
        }

        // todo 1 We can diret set priority why we are adding all priorities first.
        if (statsRequest.getTaskPriority() != null && !statsRequest.getTaskPriority().isEmpty() && statsRequest.getTaskPriority().size() > 0) {
            for (TaskPriority taskPriority : statsRequest.getTaskPriority()) {
                params.add(new SearchCriteria("taskPriority", ":", taskPriority, true, false));
            }
        }
        else if (statsRequest.getTaskPriority() == null)
            params.add(new SearchCriteria("taskPriority", ":", null, true, false));

        if (statsRequest.getTaskWorkflowId() != null) {
            params.add(new SearchCriteria("taskWorkflowId", ":", statsRequest.getTaskWorkflowId()));
        }

        if (statsRequest.getCurrentActivityIndicator() != null && statsRequest.getCurrentActivityIndicator() == 1) {
            params.add(new SearchCriteria("currentActivityIndicator", ":", 1));
        }

        if (statsRequest.getWorkflowTaskStatus() != null && !statsRequest.getWorkflowTaskStatus().isEmpty()) {
            for (String workflowTaskStatus : statsRequest.getWorkflowTaskStatus()) {
                if (Objects.equals(workflowTaskStatus, Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED_TITLE_CASE) && statsRequest.getBlockedBy() != null && !statsRequest.getBlockedBy().isEmpty()) {
                    params.add(new SearchCriteria("fkWorkflowTaskStatus", ":", workflowTaskStatus));
                    if(statsRequest.getBlockedReasonTypeId()==null || statsRequest.getBlockedReasonTypeId().isEmpty())
                    {
                        statsRequest.setBlockedReasonTypeId(Constants.blockedTypesList);
                    }
                } else {
                    List<WorkFlowTaskStatus> allWorkflowsFound = workflowTaskStatusService.getAllWorkflowTaskStatusByWorkflowTaskStatus(workflowTaskStatus);
                    for (WorkFlowTaskStatus workflowStatus : allWorkflowsFound) {
                        params.add(new SearchCriteria("fkWorkflowTaskStatus", ":", workflowStatus.getWorkflowTaskStatusId()));
                    }
                }
            }
            }
        if (statsRequest.getStatName() != null && !statsRequest.getStatName().isEmpty()) {
            for (StatType stat : statsRequest.getStatName()) {
                params.add(new SearchCriteria("taskProgressSystem", ":", stat, true, false));
            }
        }

        if (statsRequest.getFromDateType() != null && !statsRequest.getFromDateType().isEmpty()) {
            if (statsRequest.getFromDate() != null) {
                if (statsRequest.isFirstTypeLessThan())
                    params.add(new SearchCriteria(statsRequest.getFromDateType(), "<", statsRequest.getFromDate()));
                else {
                    params.add(new SearchCriteria(statsRequest.getFromDateType(), ">", statsRequest.getFromDate()));

                }
            }
        }

        if (statsRequest.getToDateType() != null && !statsRequest.getToDateType().isEmpty()) {
            if (statsRequest.getToDate() != null) {
                if (statsRequest.isSecondTypeLessThan())
                    params.add(new SearchCriteria(statsRequest.getToDateType(), "<", statsRequest.getToDate()));
                else
                    params.add(new SearchCriteria(statsRequest.getToDateType(), ">", statsRequest.getToDate()));
            }
        }

        if (statsRequest.getMentorAccountId() != null) {
            params.add(new SearchCriteria("fkAccountIdMentor1", ":", statsRequest.getMentorAccountId()));
            params.add(new SearchCriteria("fkAccountIdMentor2", ":", statsRequest.getMentorAccountId()));
        }
        if (statsRequest.getObserverAccountId() != null) {
            params.add(new SearchCriteria("fkAccountIdObserver1", ":", statsRequest.getObserverAccountId()));
            params.add(new SearchCriteria("fkAccountIdObserver2", ":", statsRequest.getObserverAccountId()));

        }
        if(statsRequest.getIsStarred()!=null && statsRequest.getIsStarred())
        {
            params.add(new SearchCriteria("isStarred",":",true));
        }
        if (statsRequest.getLastUpdatedBy() != null && !statsRequest.getLastUpdatedBy().isEmpty()) {
            params.add(new SearchCriteria("fkAccountIdLastUpdated", ":", statsRequest.getLastUpdatedBy()));
        }
        return params;
    }

//    private List<SearchCriteria> addRoleBasedTeamIds(List<Long> teamIds, List<Long> accountIdAssigned) {
//        List<SearchCriteria> criteria = new ArrayList<>();
//        if (accountIdAssigned != null) {
//            for (Long teamId : teamIds) {
//                for (Long accountId : accountIdAssigned) {
//                    criteria.add(new SearchCriteria("fkTeamId", ":", teamId));
//                    criteria.add(new SearchCriteria("fkAccountIdAssigned", ":", accountId));
//                }
//            }
//        } else {
//            for (Long teamId : teamIds) {
//                criteria.add(new SearchCriteria("fkTeamId", ":", teamId));
//            }
//        }
//        return criteria;
//    }



    public boolean validateStatsRequestFiltersForMyTask(StatsRequest statsRequest) {
        boolean isStatsRequestValidated = true;

        if (statsRequest.getTeamId() != null) {
            List<Long> userAllAccountIds = userAccountService.getUserActiveAccountIdsFromUserId(statsRequest.getUserId());
            if (accessDomainRepository.findByEntityIdAndAccountIdInAndIsActive(statsRequest.getTeamId(), userAllAccountIds, true).isEmpty()) {
                String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("Selected Team is not Validated"));
                logger.error("Selected team is not validated for the user because the user is not part of the team." + " ,    " + "teamId = " + statsRequest.getTeamId() + " ,   " + "userId = " + statsRequest.getUserId(), new Throwable(allStackTraces));
                throw new ValidationFailedException("Selected Team is not Validated");
            }
        }
        return isStatsRequestValidated;
    }

    /* This method is used to get the list of all the tasks only for the task master. This method is used for
     * both type of task master i.e. My Tasks and All Tasks. */
    public List<Task> getTasksByFiltersForTaskmaster(StatsRequest statsRequest, Map<String, List<Long>> teamPermissions, List<Long> accountIdsOfUser) {
        List<Long> accountIds = null;
        if (statsRequest.getAccountIdAssigned() != null) {
            accountIds = List.of(statsRequest.getAccountIdAssigned());
            statsRequest.setAccountIds(accountIds);
        }

        List<Specification<Task>> teamSpecs = new ArrayList<>();
        List<Long> teamViewIds = teamPermissions.get("teamView");
        List<Long> updateTeams = teamPermissions.get("basicUpdate");

        if (!teamViewIds.isEmpty()) {
            teamSpecs.addAll(teamViewIds.stream()
                    .map(TaskSpecification::byTeamId)
                    .collect(Collectors.toList()));
        }

        // Handling basic update teams more carefully
        teamPermissions.get("basicUpdate").forEach(teamId -> {
            if (statsRequest.getAccountIdAssigned() != null) {
                if (accountIdsOfUser.contains(statsRequest.getAccountIdAssigned())) {
                    // AccountIdAssigned is the account of the given user
                    teamSpecs.add(TaskSpecification.byTeamIdAndAccountId(teamId, statsRequest.getAccountIdAssigned()));
                }
            } else {
                // AccountIdAssigned is null, restrict to tasks of the user only
                teamSpecs.add(TaskSpecification.byTeamIdAndAccountIdsIn(teamId, accountIdsOfUser));
            }
        });

        Specification<Task> result = Specification.where(null);
        // Ensuring result is safely initialized
        if (!teamSpecs.isEmpty()) {
            Specification<Task> teamSpec = teamSpecs.stream()
                    .reduce(Specification::or)
                    .orElse((root, query, cb) -> cb.conjunction()); // returns a true predicate if empty
            result = teamSpec;
        } else {
            result = (root, query, cb) -> cb.disjunction(); // returns a false predicate if no valid team specs
        }

        List<SearchCriteria> params = this.getSearchCriteriaFiltersForTaskmaster(statsRequest, teamPermissions, accountIdsOfUser);
        List<Specification<Task>> specs = params.stream().map(criteria -> {
            if (criteria.getIsJoin() != null && criteria.getIsJoin()) {
                return TaskSpecification.joinSpecification(criteria);
            } else {
                return new TaskSpecification(criteria);
            }
        }).collect(Collectors.toList());

        //todo we can skip these empty checks on a list.
        if (specs.size() > 0) {
            Specification<Task> taskPriorityResult = null;
            Specification<Task> taskProgressSystemResult = null;
            Specification<Task> fkAccountIdAssignedResult = null;
            Specification<Task> fkWorkflowTaskStatusResult = null;
            Specification<Task> labelSpec = null;
            Specification<Task> taskTypeResult = null;
            Specification<Task> mentorSpecs = null;
            Specification<Task> observerSpecs = null;
            Specification<Task> bugReportedSpecs = null;
            Specification<Task> starredSpecs = null;
            Specification<Task> fkOrgId = null;
            Specification<Task> updatedSpecs = null;



            for (int i = 0; i < params.size(); i++) {
                SearchCriteria criteria = params.get(i);
                Specification<Task> currentSpec = specs.get(i);
//                if ("labels".equals(params.get(i).getKey())) {
//                    labelSpec = specs.get(i);
//                    continue;
//                }
//
//                if (Objects.equals(params.get(i).getKey(), com.tse.core_application.constants.Constants.SearchCriteriaConstants.FK_ACCOUNT_ID_ASSIGNED)) {
//                    fkAccountIdAssignedResult = where(fkAccountIdAssignedResult).or(specs.get(i));
//                } else if (Objects.equals(params.get(i).getKey(), com.tse.core_application.constants.Constants.SearchCriteriaConstants.TASK_PRIORITY)) {
//                    taskPriorityResult = where(taskPriorityResult).or(specs.get(i));
//                } else if (Objects.equals(params.get(i).getKey(), com.tse.core_application.constants.Constants.SearchCriteriaConstants.TASK_PROGRESS_SYSTEM)) {
//                    taskProgressSystemResult = where(taskProgressSystemResult).or(specs.get(i));
//                } else if (Objects.equals(params.get(i).getKey(), com.tse.core_application.constants.Constants.SearchCriteriaConstants.FK_WORKFLOW_TASK_STATUS)) {
//                    fkWorkflowTaskStatusResult = where(fkWorkflowTaskStatusResult).or(specs.get(i));
//                } else {
//                    result = params.get(i).getOrPredicate() != null && params.get(i).getOrPredicate() ? where(result).or(specs.get(i)) : where(result).and(specs.get(i));
//                }
//            }
//            result = where(result).and(taskPriorityResult);
//            result = where(result).and(fkAccountIdAssignedResult);
//            result = where(result).and(fkWorkflowTaskStatusResult);
//            result = where(result).and(taskProgressSystemResult);
//            if (labelSpec != null) {
//                result = where(result).and(labelSpec);
//            }

            switch (criteria.getKey()) {
                case "labels":
                    labelSpec = labelSpec == null ? currentSpec : where(labelSpec).or(currentSpec);
                    break;
                case com.tse.core_application.constants.Constants.SearchCriteriaConstants.FK_ACCOUNT_ID_ASSIGNED:
                    fkAccountIdAssignedResult = fkAccountIdAssignedResult == null ? currentSpec : where(fkAccountIdAssignedResult).or(currentSpec);
                    break;
                case com.tse.core_application.constants.Constants.SearchCriteriaConstants.TASK_PRIORITY:
                    if (criteria.getValue() == null) {
                        // Adding a condition for taskPriority IS NULL
                        taskPriorityResult = taskPriorityResult == null ? (root, query, builder) -> builder.isNull(root.get("taskPriority")) : taskPriorityResult.or((root, query, builder) -> builder.isNull(root.get("taskPriority")));
                    } else {
                        taskPriorityResult = taskPriorityResult == null ? currentSpec : where(taskPriorityResult).or(currentSpec);
                    }
                    break;
                case com.tse.core_application.constants.Constants.SearchCriteriaConstants.TASK_PROGRESS_SYSTEM:
                    taskProgressSystemResult = taskProgressSystemResult == null ? currentSpec : where(taskProgressSystemResult).or(currentSpec);
                    break;
                case com.tse.core_application.constants.Constants.SearchCriteriaConstants.FK_WORKFLOW_TASK_STATUS:
                    if (Objects.equals(criteria.getValue(),
                            Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED_TITLE_CASE) && statsRequest.getBlockedBy() != null && !statsRequest.getBlockedBy().isEmpty()) {
                        Specification<Task> isBlockSpec = (root, query, builder) ->
                                builder.and(
                                        builder.equal(root.get("fkWorkflowTaskStatus").get("workflowTaskStatus"), Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED_TITLE_CASE),
                                        root.get("blockedReasonTypeId").in(statsRequest.getBlockedReasonTypeId()),
                                        root.get("fkAccountIdBlockedBy").get("accountId").in(statsRequest.getBlockedBy())
                                );
                        fkWorkflowTaskStatusResult = fkWorkflowTaskStatusResult == null
                                ? isBlockSpec
                                : where(fkWorkflowTaskStatusResult).or(isBlockSpec);
                    } else {
                        fkWorkflowTaskStatusResult = fkWorkflowTaskStatusResult == null ? currentSpec : where(fkWorkflowTaskStatusResult).or(currentSpec);
                    }
                    break;
                case "fkOrgId":
                    fkOrgId = fkOrgId == null ? currentSpec : where(fkOrgId).or(currentSpec);
                    break;
                case "fkTeamId": break;
                case com.tse.core_application.constants.Constants.SearchCriteriaConstants.TASK_TYPE_ID:
                    taskTypeResult = taskTypeResult == null ? currentSpec : where(taskTypeResult).or(currentSpec);
                    break;
                case "isBug":
                    if (criteria.getValue() != null
                            && Boolean.TRUE.equals(criteria.getValue())
                            && statsRequest.getReportedBy() != null
                            && !statsRequest.getReportedBy().isEmpty()) {

                        Specification<Task> isBugSpec = (root, query, builder) ->
                                builder.and(
                                        builder.equal(root.get("taskTypeId"), Constants.TaskTypes.BUG_TASK),
                                        root.get("fkAccountIdBugReportedBy").get("accountId").in(statsRequest.getReportedBy())
                                );

                        bugReportedSpecs = bugReportedSpecs == null
                                ? isBugSpec
                                : where(bugReportedSpecs).or(isBugSpec);

                    } else if (criteria.getValue() != null
                            && Boolean.TRUE.equals(criteria.getValue())
                            && (statsRequest.getReportedBy() == null || statsRequest.getReportedBy().isEmpty())) {
                            taskTypeResult = taskTypeResult == null ? currentSpec : where(taskTypeResult).or(currentSpec);
                    }
                    break;
                case "isStarred":
                    if (Boolean.TRUE.equals(criteria.getValue())) {
                        Specification<Task> isStarredSpec = (root, query, builder) ->
                                builder.equal(root.get("isStarred"), true);
                        if (statsRequest.getStarredBy() != null && !statsRequest.getStarredBy().isEmpty()) {
                            Specification<Task> starredBySpec = (root, query, builder) -> {
                                Join<Task, UserAccount> accountJoin = root.join("fkAccountIdStarredBy");
                                return accountJoin.get("accountId").in(statsRequest.getStarredBy());
                            };
                            isStarredSpec = isStarredSpec.and(starredBySpec);
                        }
                        starredSpecs = (starredSpecs == null)
                                ? isStarredSpec
                                : where(starredSpecs).or(isStarredSpec);
                    }
                    break;
                case "fkAccountIdMentor1" :
                    mentorSpecs = mentorSpecs == null ? currentSpec : where(mentorSpecs).or(currentSpec);
                    break;
                case "fkAccountIdMentor2" :
                    mentorSpecs = mentorSpecs == null ? currentSpec : where(mentorSpecs).or(currentSpec);
                    break;
                case "fkAccountIdObserver1" :
                    observerSpecs = observerSpecs == null ? currentSpec : where(observerSpecs).or(currentSpec);
                    break;
                case "fkAccountIdObserver2" :
                    observerSpecs = observerSpecs == null ? currentSpec : where(observerSpecs).or(currentSpec);
                    break;
                case "fkAccountIdLastUpdated":
                    Specification<Task> isUpdatedSpec = (root, query, builder) ->
                            builder.and(
                                    root.get("fkAccountIdLastUpdated").get("accountId").in(statsRequest.getLastUpdatedBy())
                            );
                    updatedSpecs = updatedSpecs == null
                            ? isUpdatedSpec
                            : where(updatedSpecs).or(isUpdatedSpec);
                    break;
                default:
                    // For unspecified cases, apply a default logic
                    result = Specification.where(result).and(currentSpec);
                    break;
            }
        }

    // After looping through all parameters, combine results from specific filters
        if (fkAccountIdAssignedResult != null) result = result.and(fkAccountIdAssignedResult);
        if (taskPriorityResult != null) result = result.and(taskPriorityResult);
        if (taskProgressSystemResult != null) result = result.and(taskProgressSystemResult);
        if (fkWorkflowTaskStatusResult != null) result = result.and(fkWorkflowTaskStatusResult);
        if (labelSpec != null) result = result.and(labelSpec);
        if (taskTypeResult != null) result = result.and(taskTypeResult);
        if (observerSpecs != null) result = result.and(observerSpecs);
        if (mentorSpecs != null) result = result.and(mentorSpecs);
        if (bugReportedSpecs != null) result = result.and(bugReportedSpecs);
        if (starredSpecs != null) result = result.and(starredSpecs);
        if (updatedSpecs != null) result = result.and(updatedSpecs);
        if(fkOrgId != null) result=result.and(fkOrgId);

        if (statsRequest.getSearches() != null && !statsRequest.getSearches().isEmpty()) {
//                Specification p1 = JPACustomPredicates.addSimilarityFunctionPredicate(statsRequest.getSearches(), "taskDesc", searchMultiplier);
                Specification<Task> p2 = JPACustomPredicates.addSimilarityFunctionPredicate(statsRequest.getSearches(), "taskTitle", searchMultiplier);
//                p1 = where(p1).or(p2);
//                result = where(result).and(p1);
                p2 = where(p2);
                result = where(result).and(p2);
            }
            return taskRepository.findAll(result);
        } else {
            return taskRepository.findAll();
        }
    }


    /* This method is used to get all the search criteria filters only for the Dashboard. This method will be used for both
     * types of Dashboard i.e. My Tasks and All Tasks.*/
    private List<SearchCriteria> getSearchCriteriaFiltersForDashboard(StatsRequest statsRequest, String filterFor) {
        List<SearchCriteria> params = new ArrayList<>();

        LocalDateTime currentDate = LocalDateTime.now();
        if (statsRequest.getNoOfDays() != null) {
            LocalDateTime newEndDate = currentDate.minusDays(statsRequest.getNoOfDays());
            params.add(new SearchCriteria("taskExpEndDate", ">", newEndDate));

        }

/*
        List<WorkFlowTaskStatus> allWorkflowTaskStatuses = workflowTaskStatusService.getAllWorkflowTaskStatus();
        if (!allWorkflowTaskStatuses.isEmpty()) {
            for (WorkFlowTaskStatus workFlowTaskStatus : allWorkflowTaskStatuses) {
                if (!(workFlowTaskStatus.getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_DELETE)))
                    params.add(new SearchCriteria("fkWorkflowTaskStatus", ":", workFlowTaskStatus.getWorkflowTaskStatusId()));
            }
        }
*/
        // todo we should handle this case in some better modular way.
        Long assignedId = statsRequest.getAccountIdAssigned();
        boolean isUnassigned = Objects.equals(assignedId, com.tse.core_application.constants.Constants.SearchCriteriaConstants.FK_ACCOUNT_ID_NOT_ASSIGNED);

        if (filterFor.equalsIgnoreCase(Constants.flags.STATS_ALL_TASKS)) {

            if (isUnassigned) {
                params.add(new SearchCriteria("fkAccountIdAssigned", "isNull", "null"));

            } else if (assignedId != null) {
                statsRequest.setAccountIds(List.of(statsRequest.getAccountIdAssigned()));
                params.add(new SearchCriteria("fkAccountIdAssigned", ":", statsRequest.getAccountIds()));
            }

        } else if (statsRequest.getAccountIds() != null && !statsRequest.getAccountIds().isEmpty()) {
            if (filterFor.equalsIgnoreCase(Constants.flags.STATS_MY_TASKS)) {
                params.add(new SearchCriteria("fkAccountIdAssigned", ":", statsRequest.getAccountIds(), true, false));
            }
        }

        if (statsRequest.getOrgIds() != null && !statsRequest.getOrgIds().isEmpty() && statsRequest.getOrgIds().size() > 0) {
                for (Long orgId : statsRequest.getOrgIds()) {
                    params.add(new SearchCriteria("fkOrgId", ":", orgId, true, false));
                }
            }

            if (statsRequest.getBuId() != null) {
                params.add(new SearchCriteria("buId", ":", statsRequest.getBuId()));
                String allStackTraces = StackTraceHandler.getAllStackTraces(new InvalidStatsRequestFilterException("BU"));
                logger.error("Invalid Filter: BU filter is not available for dashboard. ", new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw new InvalidStatsRequestFilterException("BU");
            }

            if (statsRequest.getProjectId() != null) {
                params.add(new SearchCriteria("fkProjectId", ":", statsRequest.getProjectId()));
            }

            if (statsRequest.getTeamId() != null) {
                params.add(new SearchCriteria("fkTeamId", ":", statsRequest.getTeamId()));
            }

            if (statsRequest.getSprintId() != null) {
                params.add(new SearchCriteria("sprintId", ":", statsRequest.getSprintId()));
            }

            if (statsRequest.getEpicId() != null) {
                params.add(new SearchCriteria("fkEpicId", ":", statsRequest.getEpicId()));
            }

            if (statsRequest.getTaskPriority() != null && !statsRequest.getTaskPriority().isEmpty() && statsRequest.getTaskPriority().size() > 0) {
                for (TaskPriority taskPriority : statsRequest.getTaskPriority()) {
                    params.add(new SearchCriteria("taskPriority", ":", taskPriority, true, false));
                }
            }

            if (statsRequest.getTaskWorkflowId() != null) {
                params.add(new SearchCriteria("taskWorkflowId", ":", statsRequest.getTaskWorkflowId()));
            }

            if (statsRequest.getStatName() != null && !statsRequest.getStatName().isEmpty() && statsRequest.getStatName().size() > 0) {
                for (StatType stat : statsRequest.getStatName()) {
                    params.add(new SearchCriteria("taskProgressSystem", ":", stat, true, false));
                }
            }

            if (statsRequest.getLabelIds() != null && !statsRequest.getLabelIds().isEmpty()) {
                params.add(new SearchCriteria("labels", "in", statsRequest.getLabelIds(), false, true));
            }

            if (statsRequest.getAccountIdCreator() != null) {
                params.add(new SearchCriteria("fkAccountIdCreator", ":", statsRequest.getAccountIdCreator()));
            }

            if (statsRequest.getTaskTypeList() != null && !statsRequest.getTaskTypeList().isEmpty()) {
                for (Integer taskType : statsRequest.getTaskTypeList()) {
                    if (Objects.equals(taskType, Constants.TaskTypes.BUG_TASK)) {
                        params.add(new SearchCriteria("isBug", ":", true));
                    } else {
                        params.add(new SearchCriteria("taskTypeId", ":", taskType, true, false));
                    }
                }
            }
            if (statsRequest.getIsStarred() != null && statsRequest.getIsStarred()) {
                params.add(new SearchCriteria("isStarred", ":", true));
            }

            return params;
        }

    /* This method is used to get all the tasks by all the selected filters for Dashboard. */
    public List<Task> getTasksByFiltersForDashboard(StatsRequest statsRequest, String filterFor) {

        if (filterFor.equalsIgnoreCase(Constants.flags.STATS_MY_TASKS)) {
            validateStatsRequestFiltersForMyTask(statsRequest);
        }

        List<SearchCriteria> params = this.getSearchCriteriaFiltersForDashboard(statsRequest, filterFor);
//        List<Specification<Task>> specs = params.stream().map(TaskSpecification::new).collect(Collectors.toList());
        List<Specification<Task>> specs = params.stream().map(criteria -> {
            if (criteria.getIsJoin() != null && criteria.getIsJoin()) {
                return TaskSpecification.joinSpecification(criteria);
            } else {
                return new TaskSpecification(criteria);
            }
        }).collect(Collectors.toList());

        if (specs.size() > 0) {
            Specification<Task> result = null;
            Specification<Task> taskPriorityResult = null;
            Specification<Task> taskProgressSystemResult = null;
            Specification<Task> fkAccountIdAssignedResult = null;
            Specification<Task> workFlowStatusIDFilter = null;
            Specification<Task> expEndDateQuery = null;
            Specification<Task> connectWorkflowStatusesAndTaskReqAllOr = null;
            Specification<Task> labelSpec = null;
            Specification<Task> taskTypeResult = null;
            Specification<Task>starredSpec=null;
            Specification<Task> fkOrgId = null;

//            final List<Integer> workflowOtherStatusesListFirst = new ArrayList<>();
//            final List<Integer> workflowOtherStatusesListSecond = new ArrayList<>();
//            final List<Integer> workflowOtherStatusesListThird = new ArrayList<>();
//            final List<Integer> workflowCompletedStatusList = new ArrayList<>();
//
//            //ToDo: make this method as static and move createWorkflowStatusMap to WorkflowTaskStatus service
//            // ToDo: this has to be retrieved from constants -- code line till 403 are static for now there is no need to calculate this everytime
//            final List<WorkFlowTaskStatus> allWorkflowTaskStatuses = workflowTaskStatusService.getAllWorkflowTaskStatus();
//            final Map<String, List<WorkFlowTaskStatus>> workflowStatusMap = createWorkflowStatusMap(allWorkflowTaskStatuses);
//
//            //ToDo: this can be defined in the constants only for now -- why calculate this static thing everytime unnecessarily
//            workflowStatusMap.forEach((status, statusObj) -> {
//
//                if (status.toLowerCase().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_STARTED) || status.toLowerCase().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_ON_HOLD) || status.toLowerCase().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED) || status.toLowerCase().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED)) {
//
//                    workflowOtherStatusesListFirst.addAll(statusObj.stream().map(WorkFlowTaskStatus::getWorkflowTaskStatusId).collect(Collectors.toList()));
//                }
//
//                if (status.toLowerCase().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG)) {
//                    workflowOtherStatusesListSecond.addAll(statusObj.stream().map(WorkFlowTaskStatus::getWorkflowTaskStatusId).collect(Collectors.toList()));
//                }
//
//                if (status.toLowerCase().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_ON_HOLD) || status.toLowerCase().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED) || status.toLowerCase().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED)) {
//
//                    workflowOtherStatusesListThird.addAll(statusObj.stream().map(WorkFlowTaskStatus::getWorkflowTaskStatusId).collect(Collectors.toList()));
//                }
//                if (status.toLowerCase().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED)) {
//                    workflowCompletedStatusList.addAll(statusObj.stream().map(WorkFlowTaskStatus::getWorkflowTaskStatusId).collect(Collectors.toList()));
//                }
//
//            });

            if (statsRequest.getWorkflowTaskStatus() != null && !statsRequest.getWorkflowTaskStatus().isEmpty()) {
                List<Integer> values = new ArrayList<>();
                for (String workFlowStatus : statsRequest.getWorkflowTaskStatus()) {
                    if(workflowStatusMap.containsKey(workFlowStatus))
                        values.addAll(workflowStatusMap.get(workFlowStatus).stream().map(WorkFlowTaskStatus::getWorkflowTaskStatusId).collect(Collectors.toList()));
                    else
                        throw new ValidationFailedException("Incorrect Workflow Status");
                }
                workFlowStatusIDFilter = where(workFlowStatusIDFilter).and(workflowStatusIDFilterPredicate(values));
            }

            for (int i = 0; i < params.size(); i++) {
                if ("labels".equals(params.get(i).getKey())) {
                    labelSpec = specs.get(i);
                    continue;
                }
                if("isStarred".equals(params.get(i).getKey()))
                        if (Boolean.TRUE.equals(params.get(i).getValue())) {
                            Specification<Task> isStarredSpec = (root, query, builder) ->
                                    builder.equal(root.get("isStarred"), true);
                            if (statsRequest.getStarredBy() != null && !statsRequest.getStarredBy().isEmpty()) {
                                Specification<Task> starredBySpec = (root, query, builder) -> {
                                    Join<Task, UserAccount> accountJoin = root.join("fkAccountIdStarredBy");
                                    return accountJoin.get("accountId").in(statsRequest.getStarredBy());
                                };
                                isStarredSpec = isStarredSpec.and(starredBySpec);
                            }
                            starredSpec = (starredSpec == null)
                                    ? isStarredSpec
                                    : where(starredSpec).or(isStarredSpec);
                        continue;
                }
                if ("fkOrgId".equals(params.get(i).getKey())) {
                    fkOrgId = fkOrgId == null ? specs.get(i) : where(fkOrgId).or(specs.get(i));
                    continue;
                }
                if (Objects.equals(params.get(i).getKey(), com.tse.core_application.constants.Constants.SearchCriteriaConstants.FK_ACCOUNT_ID_ASSIGNED)) {
                    String valueStr = String.valueOf(params.get(i).getValue()).trim();
                    if ("null".equalsIgnoreCase(valueStr)) {
                        fkAccountIdAssignedResult = TaskSpecification.byEntityAndNotAssigned(
                                statsRequest.getTeamId(),
                                statsRequest.getProjectId(),
                                statsRequest.getOrgIds()
                        );
                    } else {
//                        Long valueLong = Long.parseLong(valueStr);
                        fkAccountIdAssignedResult = (root, query, builder) ->
                                root.get("fkAccountIdAssigned").get("accountId").in(statsRequest.getAccountIds());
                    }
                    if (fkAccountIdAssignedResult != null) {
                        result = (result == null)
                                ? fkAccountIdAssignedResult
                                : where(result).and(fkAccountIdAssignedResult);
                    }
                    continue;
                } else if (Objects.equals(params.get(i).getKey(), com.tse.core_application.constants.Constants.SearchCriteriaConstants.TASK_PRIORITY)) {
                    taskPriorityResult = taskPriorityResult == null ? specs.get(i) : where(taskPriorityResult).or(specs.get(i));
                } else {
                    if (Objects.equals(params.get(i).getKey(), com.tse.core_application.constants.Constants.SearchCriteriaConstants.TASK_PROGRESS_SYSTEM)) {
                        taskProgressSystemResult = taskProgressSystemResult == null ? specs.get(i) : where(taskProgressSystemResult).or(specs.get(i));
                    } else {
                        if (Objects.equals(params.get(i).getKey().toLowerCase(), com.tse.core_application.constants.Constants.SearchCriteriaConstants.TASK_EXP_END_DATE.toLowerCase())) {
                            expEndDateQuery = expEndDateQuery == null ? specs.get(i) : where(expEndDateQuery).and(specs.get(i));
                        } else if (Objects.equals(params.get(i).getKey(), com.tse.core_application.constants.Constants.SearchCriteriaConstants.TASK_TYPE_ID)) {
                            taskTypeResult = taskTypeResult == null ? specs.get(i) : where(taskTypeResult).or(specs.get(i));
                        } else if (Objects.equals(params.get(i).getKey(), "isBug")) {
                            taskTypeResult = taskTypeResult == null ? specs.get(i) : where(taskTypeResult).or(specs.get(i));
                        } else {
                            result = params.get(i).getOrPredicate() != null && params.get(i).getOrPredicate() ? where(result).or(specs.get(i)) : where(result).and(specs.get(i));
                        }
                    }
                }
            }

            if (taskPriorityResult != null) result = where(result).and(taskPriorityResult);
            if (fkAccountIdAssignedResult != null) result = where(result).and(fkAccountIdAssignedResult);
            if (workFlowStatusIDFilter != null) result = where(result).and(workFlowStatusIDFilter);
            if (expEndDateQuery != null) result = where(result).and(expEndDateQuery);
            if (taskProgressSystemResult != null) result = where(result).and(taskProgressSystemResult);
            if (taskTypeResult != null) {
                result = where(result).and(taskTypeResult);
            }
            if (labelSpec != null) {
                result = where(result).and(labelSpec);
            }
            if(starredSpec != null) {
                result = where(result).and(starredSpec);
            }
            if (fkOrgId != null) {
                result = where(result).and(fkOrgId);
            }

            connectWorkflowStatusesAndTaskReqAllOr = where(connectWorkflowStatusesAndTaskReqAllOr).or(workflowCompleteAndReqPredicate(statsRequest, workflowCompletedStatusList));
            connectWorkflowStatusesAndTaskReqAllOr = where(connectWorkflowStatusesAndTaskReqAllOr).or(workflowOtherStatusAndReqPredicate(statsRequest, workflowOtherStatusesListFirst));
            connectWorkflowStatusesAndTaskReqAllOr = where(connectWorkflowStatusesAndTaskReqAllOr).or(workflowBacklogCriticalStatusPredicate(workflowOtherStatusesListSecond));
            connectWorkflowStatusesAndTaskReqAllOr = where(connectWorkflowStatusesAndTaskReqAllOr).or(workflowStatusAndTaskExpReqPredicate(statsRequest, workflowOtherStatusesListThird));

            result = where(result).and(connectWorkflowStatusesAndTaskReqAllOr);

            List<Task> tasks = taskRepository.findAll(result);
            return tasks;
        } else {
            return taskRepository.findAll();
        }
    }


    //todo can we do something with this.
    public boolean isEntityFilterPresentInStatsRequest(StatsRequest statsRequest) {
        boolean isEntityFilterPresent = false;
        if ((statsRequest.getOrgIds() != null && !statsRequest.getOrgIds().isEmpty())) {
            isEntityFilterPresent = true;
        } else {
            if (statsRequest.getBuId() != null) {
                isEntityFilterPresent = true;
            } else {
                if (statsRequest.getProjectId() != null) {
                    isEntityFilterPresent = true;
                } else {
                    if (statsRequest.getTeamId() != null) {
                        boolean isFilterTeamValidated = validateFilterTeamInStatsRequest(statsRequest);
                        if (isFilterTeamValidated) {
                            isEntityFilterPresent = true;
                        }
                    }
                }
            }
        }
        return isEntityFilterPresent;
    }

    // todo we can also club these 2 queries in single one.
    public boolean validateFilterTeamInStatsRequest(StatsRequest statsRequest) {
        boolean isFilterTeamValidated = true;
        List<Long> userAllAccountIds = new ArrayList<>();

        List<UserAccount> userAccountsFoundDb = userAccountService.getAllUserAccountByUserIdAndIsActive(statsRequest.getUserId());
        for (UserAccount userAccount : userAccountsFoundDb) {
            userAllAccountIds.add(userAccount.getAccountId());
        }
        // Todo: add isActive condition
        List<CustomAccessDomain> customAccessDomainsFoundDb = accessDomainService.getAccessDomainsByAccountIdsAndEntityId(statsRequest.getTeamId(), userAllAccountIds);
        if (customAccessDomainsFoundDb.isEmpty()) {
            throw new ValidationFailedException("Selected Team is not Validated");
        }
        return isFilterTeamValidated;
    }


    public List<Task> getTasksForAllTasksStatsCalculation(StatsRequest statsRequest, String filterFor, String accountIds) {
        List<Long> userAllAccountIds = new ArrayList<>();
        List<AccountIdEntityIdRoleId> filterCriteria = new ArrayList<>();
        List<Task> finalTasksFoundDb = new ArrayList<>();
        List<Integer> uniqueEntityIds = new ArrayList<>();
        List<Long> accountIdList = jwtRequestFilter.getAccountIdsFromHeader(accountIds);
        StatsRequest statsRequestCopy = new StatsRequest();
        BeanUtils.copyProperties(statsRequest, statsRequestCopy);

        if (isEntityFilterPresentInStatsRequest(statsRequestCopy)) {
            if (statsRequestCopy.getTeamId() != null)
                filterCriteria = accessDomainRepository.findAccountIdEntityIdRoleIdByAccountIdsEntityIdEntityTypeIdAndActionIds(accountIdList, Constants.EntityTypes.TEAM, statsRequestCopy.getTeamId(), Constants.ActionId.TEAM_TASK_VIEW, Constants.ActionId.TASK_BASIC_UPDATE);
            else
                filterCriteria = accessDomainRepository.findAccountIdEntityIdRoleIdsByAccountIdsAndEntityTypeIdAndActionIds(accountIdList, Constants.EntityTypes.TEAM, Constants.ActionId.TEAM_TASK_VIEW, Constants.ActionId.TASK_BASIC_UPDATE);
            finalTasksFoundDb = getAllTasksForStatCal(filterCriteria, statsRequestCopy, filterFor);
        } else {
//            List<UserAccount> userAccountsFoundDb = userAccountService.getAllUserAccountByUserId(statsRequest.getUserId());
//            for (UserAccount userAccount : userAccountsFoundDb) {
//                userAllAccountIds.add(userAccount.getAccountId());
//            }
//            //check if we can achieve all of this using single query in the database instead of multiple queries here
//            List<CustomAccessDomain> customAccessDomainsFoundDb = accessDomainService.getAccessDomainsByAccountIdsAndEntityTypeId(Constants.EntityTypes.TEAM, userAllAccountIds);
//
//            for (CustomAccessDomain accessDomain : customAccessDomainsFoundDb) {
//                ArrayList<Integer> actionIdsForRoleId = new ArrayList<>();
//                ArrayList<ActionId> actionIdsForRoleIdDb = roleActionService.getActionIdByRoleId(accessDomain.getRoleId());
//
//                for (ActionId actionId : actionIdsForRoleIdDb) {
//                    actionIdsForRoleId.add(actionId.getActionId());
//                }
//
//                if (actionIdsForRoleId.contains(Constants.ActionId.TEAM_TASK_VIEW)) {
//                    AccountIdEntityIdRoleId accountIdEntityIdRoleId = new AccountIdEntityIdRoleId(Long.valueOf(0), accessDomain.getEntityId(), accessDomain.getRoleId());
//                    if (!uniqueEntityIds.contains(accountIdEntityIdRoleId.getEntityId())) {
//                        uniqueEntityIds.add(accountIdEntityIdRoleId.getEntityId());
//                        filterCriteria.add(accountIdEntityIdRoleId);
//                    }
//                } else {
//                    if (actionIdsForRoleId.contains(Constants.ActionId.TASK_BASIC_UPDATE)) {
//                        AccountIdEntityIdRoleId accountIdEntityIdRoleId = new AccountIdEntityIdRoleId(accessDomain.getAccountId(), accessDomain.getEntityId(), accessDomain.getRoleId());
//                        if (!uniqueEntityIds.contains(accountIdEntityIdRoleId.getEntityId())) {
//                            uniqueEntityIds.add(accountIdEntityIdRoleId.getEntityId());
//                            filterCriteria.add(accountIdEntityIdRoleId);
//                        }
//                    }
//                }
//            }
            filterCriteria = userAccountRepository.getAccountIdEntityIdRoleIdForUserId(statsRequestCopy.getUserId(), Constants.EntityTypes.TEAM, Constants.ActionId.TEAM_TASK_VIEW, Constants.ActionId.TASK_BASIC_UPDATE);
            finalTasksFoundDb = getAllTasksForStatCal(filterCriteria, statsRequestCopy, filterFor);
        }
        return finalTasksFoundDb;
    }

    public List<Task> getAllTasksForStatCal(List<AccountIdEntityIdRoleId> filterCriteria, StatsRequest statsRequest, String filterFor) {
        List<Task> allFoundTasksDb = new ArrayList<>();
        List<TaskMaster> taskMasters = new ArrayList<>();

        if (filterCriteria != null) {
            for (AccountIdEntityIdRoleId accountIdEntityIdRoleId : filterCriteria) {
                StatsRequest statsRequestCopy = new StatsRequest();
                BeanUtils.copyProperties(statsRequest, statsRequestCopy);
                statsRequestCopy.setTeamId(accountIdEntityIdRoleId.getEntityId().longValue());
                List<Task> tasks = Collections.emptyList();
                if (accountIdEntityIdRoleId.getAccountId() == 0) {
                    tasks = this.getTasksByFiltersForDashboard(statsRequestCopy, filterFor);
                } else {
                    if (statsRequestCopy.getAccountIdAssigned() != null) {
                        if (Objects.equals(accountIdEntityIdRoleId.getAccountId(), statsRequestCopy.getAccountIdAssigned())) {
                            statsRequestCopy.setAccountIdAssigned(accountIdEntityIdRoleId.getAccountId());
                            tasks = this.getTasksByFiltersForDashboard(statsRequestCopy, filterFor);
                        }
                    } else {
                        statsRequestCopy.setAccountIdAssigned(accountIdEntityIdRoleId.getAccountId());
                        tasks = this.getTasksByFiltersForDashboard(statsRequestCopy, filterFor);
                    }
                }
                boolean isTaskAdded = allFoundTasksDb.addAll(tasks);
            }
        } else {
            List<Task> tasks = this.getTasksByFiltersForDashboard(statsRequest, filterFor);
            boolean isTaskAdded = allFoundTasksDb.addAll(tasks);
        }
        return allFoundTasksDb;
    }

//    public List<TaskMaster> getAllFilteredTaskFromAllTeamForUser(StatsRequest statsRequest, String accountIds, String timeZone) {
//        statsService.setDefaultFromAndToDateToStatsRequest(statsRequest, timeZone);
//        List<TaskMaster> taskMasters = null;
////         ToDo: Don't remove this commented code
////        List<CustomAccessDomain> allTeamAccessDomainsOfAllAccountIds = new ArrayList<>();
////        List<AccountIdEntityIdRoleId> filterCriteria = new ArrayList<>();
////        if (statsRequest.getTeamId() != null) {
////            List<CustomAccessDomain> customAccessDomains = accessDomainService.getAccessDomainByAccountIdAndEntityId(Long.valueOf(accountIds), (int) (long) statsRequest.getTeamId());
////            if (customAccessDomains != null) {
////                for (CustomAccessDomain accessDomain : customAccessDomains) {
////                    if (Objects.equals(accessDomain.getAccountId(), Long.valueOf(accountIds))) {
////                        boolean isCustomAccessDomainAdded = allTeamAccessDomainsOfAllAccountIds.add(accessDomain);
////                    }
////                }
////            }
////        } else {
////            String[] accountIdArray = accountIds.split(",");
////            for (String accountId : accountIdArray) {
////                List<CustomAccessDomain> accessDomains = accessDomainService.findAllActiveAccessDomainByAccountId(Long.valueOf(accountId));
////                for (CustomAccessDomain accessDomain : accessDomains) {
////                    if (Objects.equals(accessDomain.getEntityTypeId(), Constants.EntityTypes.TEAM)) {
////                        boolean isAccessDomainAdded = allTeamAccessDomainsOfAllAccountIds.add(accessDomain);
////                    }
////                }
////            }
////        }
////        for (CustomAccessDomain accessDomain : allTeamAccessDomainsOfAllAccountIds) {
////            ArrayList<Integer> actionIdsForRoleId = new ArrayList<>();
////            ArrayList<ActionId> actionIdsForRoleIdDb = roleActionService.getActionIdByRoleId(accessDomain.getRoleId());
////            for (ActionId actionId : actionIdsForRoleIdDb) {
////                actionIdsForRoleId.add(actionId.getActionId());
////            }
////            if (actionIdsForRoleId.contains(Constants.ActionId.TEAM_TASK_VIEW)) {
////                AccountIdEntityIdRoleId accountIdEntityIdRoleId = new AccountIdEntityIdRoleId(0L, accessDomain.getEntityId(), accessDomain.getRoleId());
////                filterCriteria.add(accountIdEntityIdRoleId);
////            } else {
////                if (actionIdsForRoleId.contains(Constants.ActionId.TASK_BASIC_UPDATE)) {
////                    AccountIdEntityIdRoleId accountIdEntityIdRoleId = new AccountIdEntityIdRoleId(accessDomain.getAccountId(), accessDomain.getEntityId(), accessDomain.getRoleId());
////                    filterCriteria.add(accountIdEntityIdRoleId);
////                }
////            }
////        }
//        List<AccountIdEntityIdRoleId> filterCriteria = new ArrayList<>();
//        String[] accountIdArray = accountIds.split(",");
//        List<Long> accountIdList = Arrays.stream(accountIdArray)
//                .map(Long::valueOf)
//                .collect(Collectors.toList());
////         get all the accounts who have 'basic update' and 'team view' actions
//        if (statsRequest.getTeamId() != null) filterCriteria = accessDomainRepository.findAccountIdEntityIdRoleIdByAccountIdsEntityIdEntityTypeIdAndActionIds(accountIdList, Constants.EntityTypes.TEAM, (int) (long) statsRequest.getTeamId(), Constants.ActionId.TEAM_TASK_VIEW, Constants.ActionId.TASK_BASIC_UPDATE);
//        else filterCriteria = accessDomainRepository.findAccountIdEntityIdRoleIdsByAccountIdsAndEntityTypeIdAndActionIds(accountIdList, Constants.EntityTypes.TEAM, Constants.ActionId.TEAM_TASK_VIEW, Constants.ActionId.TASK_BASIC_UPDATE);
//
//        taskMasters = this.getAllTaskByFilter(filterCriteria, statsRequest, timeZone);
//        taskMasters.forEach(taskMaster -> {
//            Organization organization = organizationRepository.findByOrgId(taskRepository.findByTaskNumber(taskMaster.getTaskNumber()).getFkOrgId().getOrgId());
//            if (organization.getOrganizationName().equalsIgnoreCase(com.tse.core_application.model.Constants.PERSONAL_ORG)) {
//                if (Objects.equals(taskMaster.getTeamName(), com.tse.core_application.model.Constants.PERSONAL_ORG_DEFAULT_TEAM_NAME)) {
//                    if (teamService.getAllTeamsForCreateTask(taskMaster.getEmail(), organization.getOrgId()).size() > 1) {
//                        taskMaster.setTeamName(com.tse.core_application.model.Constants.PERSONAL_ORG_TEAM_DISPLAY_NAME_FOR_MULTIPLE_TEAMS);
//                    } else {
//                        taskMaster.setTeamName(com.tse.core_application.model.Constants.PERSONAL_ORG_TEAM_DISPLAY_NAME);
//                    }
//                }
//            }
//        });
//        return taskMasters;
//    }
//
//    public List<TaskMaster> getAllTaskByFilter(List<AccountIdEntityIdRoleId> filterCriteria, StatsRequest statsRequest, String timeZone) {
////        logger.info(" called getAllTaskByFilter method !");
//        List<Task> allFoundTasksDb = new ArrayList<>();
//        List<TaskMaster> taskMasters = new ArrayList<>();
//        List<Task> allDistinctFoundTaskDb = new ArrayList<>();
//
//        for (AccountIdEntityIdRoleId accountIdEntityIdRoleId : filterCriteria) {
//            statsRequest.setTeamId(accountIdEntityIdRoleId.getEntityId().longValue());
//            List<Task> tasks = Collections.emptyList();
//            StatsRequest statsRequestCopy = new StatsRequest();
//            BeanUtils.copyProperties(statsRequest, statsRequestCopy);
//            if (accountIdEntityIdRoleId.getAccountId() == 0) {
//                tasks = getTasksByFiltersForTaskmaster(statsRequestCopy);
//            } else {
//                if (statsRequestCopy.getAccountIdAssigned() != null) {
//                    if (Objects.equals(accountIdEntityIdRoleId.getAccountId(),statsRequestCopy.getAccountIdAssigned())){
//                        statsRequestCopy.setAccountIdAssigned(accountIdEntityIdRoleId.getAccountId());
//                        tasks = getTasksByFiltersForTaskmaster(statsRequestCopy);
//                    }
//                } else {
//                    statsRequestCopy.setAccountIdAssigned(accountIdEntityIdRoleId.getAccountId());
//                    tasks = getTasksByFiltersForTaskmaster(statsRequestCopy);
//                }
//            }
//            boolean isTaskAdded = allFoundTasksDb.addAll(tasks);
//        }
////        for (Task task : allFoundTasksDb) {
////            if (!allDistinctFoundTaskDb.contains(task)) {
////                allDistinctFoundTaskDb.add(task);
////            }
////        }
//
////        List<Task> filteredTasksByStatName = new ArrayList<>(allDistinctFoundTaskDb);
//        HashSet<Task> filteredTasksByStatName = new HashSet<>(allFoundTasksDb);
//
////        List<Task> filteredTasksOnEffortDate = new ArrayList<>();
////        if(statsRequest.getNewEffortDate() != null) {
////            boolean isEffortDateOfToday = statsRequest.getNewEffortDate().equals(LocalDate.now());
////            for(Task task: tasks) {
////                if(timeSheetRepository.existsByEntityTypeIdAndEntityIdAndNewEffortDate(Constants.EntityTypes.TASK, task.getTaskId(), statsRequest.getNewEffortDate())) {
////                    filteredTasksOnEffortDate.add(task);
////                } else if(isEffortDateOfToday && task.getCurrentActivityIndicator() == 1) {
////                    filteredTasksOnEffortDate.add(task);
////                }
////            }
////            return filteredTasksOnEffortDate;
////        }
//
//        boolean isEffortDateOfToday = statsRequest.getNewEffortDate() != null && statsRequest.getNewEffortDate().equals(LocalDate.now());
//        if (!filteredTasksByStatName.isEmpty()) {
//            for (Task task : filteredTasksByStatName) {
//                TaskMaster taskMaster = new TaskMaster();
//                // filtration based on effort date -- if there is an effortDate provided and there is an effort on any task on that date then we include it. If there is no
//                // effort on that date but the date is today's date and current activity indicator for any task is on then we include it
//                if (statsRequest.getNewEffortDate() != null) {
//                    if (timeSheetRepository.existsByEntityTypeIdAndEntityIdAndNewEffortDate(Constants.EntityTypes.TASK, task.getTaskId(), statsRequest.getNewEffortDate())) {
//                        taskMaster.setNewEffortDate(statsRequest.getNewEffortDate());
//                    } else if (isEffortDateOfToday && task.getCurrentActivityIndicator() == 1) {
//                        taskMaster.setNewEffortDate(null);
//                    } else {
//                        continue;
//                    }
//                }
//
//                taskServiceImpl.convertTaskAllServerDateAndTimeInToLocalTimeZone(task, timeZone);
//                BeanUtils.copyProperties(task, taskMaster);
//                taskMaster.setTaskWorkflowType(task.getFkWorkflowTaskStatus().getFkWorkFlowType().getWorkflowName());
//                taskMaster.setWorkflowTaskStatusType(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus());
//                taskMaster.setTeamName(task.getFkTeamId().getTeamName());
//                if (task.getFkAccountIdAssigned() != null) {
//                    String fullName = task.getFkAccountIdAssigned().getFkUserId().getFirstName() + " " + task.getFkAccountIdAssigned().getFkUserId().getLastName();
//                    String email = task.getFkAccountIdAssigned().getEmail();
//                    taskMaster.setEmail(email);
//                    taskMaster.setFullName(fullName);
//                }
//                boolean isTaskMasterAdded = taskMasters.add(taskMaster);
//            }
//        }
//        return taskMasters;
//    }


    public List<TaskMaster> getAllTaskByFilter(StatsRequest statsRequest, String accountIds, String timeZone) {
        statsService.setDefaultFromAndToDateToStatsRequest(statsRequest, timeZone);
        List<Task> allFoundTasksDb = new ArrayList<>();
        List<TaskMaster> taskMasters = new ArrayList<>();
        List<Long> accountIdsOfUser = CommonUtils.convertToLongList(accountIds);
        //1 need to add dashBoard filter member
        //2 need to add updatedBy filter in taskMaster

        List<Long> teamTaskViewTeamIds = new ArrayList<>();
        List<Long> onlyBasicUpdateTeamIds = new ArrayList<>();
        teamTaskViewTeamIds = accessDomainRepository.findTeamIdsByAccountIdsAndActionId(accountIdsOfUser, Constants.EntityTypes.TEAM, Constants.ActionId.TEAM_TASK_VIEW);
//        List<Long> basicUpdateTeamIds = accessDomainRepository.findTeamIdsByAccountIdsAndActionId(accountIdsOfUser, Constants.EntityTypes.TEAM, Constants.ActionId.TASK_BASIC_UPDATE);
        List<Long> allTeamIdsOfUser = accessDomainRepository.findTeamIdsByAccountIdsAndIsActiveTrue(accountIdsOfUser);
        onlyBasicUpdateTeamIds = new ArrayList<>(allTeamIdsOfUser);
        onlyBasicUpdateTeamIds.removeAll(teamTaskViewTeamIds);

        Long requestedTeamId = statsRequest.getTeamId();
        if (requestedTeamId != null) {
            teamTaskViewTeamIds = teamTaskViewTeamIds.stream()
                    .filter(id -> id.equals(requestedTeamId))
                    .collect(Collectors.toList());
            onlyBasicUpdateTeamIds = onlyBasicUpdateTeamIds.stream()
                    .filter(id -> id.equals(requestedTeamId))
                    .collect(Collectors.toList());
        }


        HashMap<String, List<Long>> map = new HashMap<>();
        map.put("teamView", teamTaskViewTeamIds);
        map.put("basicUpdate", onlyBasicUpdateTeamIds);
        List<Task> tasks = getTasksByFiltersForTaskmaster(statsRequest, map, accountIdsOfUser);
        allFoundTasksDb.addAll(tasks);

        HashSet<Task> filteredTasksByStatName = new HashSet<>(allFoundTasksDb);
        boolean isEffortDateOfToday = statsRequest.getNewEffortDate() != null && statsRequest.getNewEffortDate().equals(LocalDate.now());
        if (!filteredTasksByStatName.isEmpty()) {
            List<UserAccount> allAccountsOfRequester = userAccountRepository.findByAccountIdInAndIsActive(accountIdsOfUser, true);
            Optional<UserAccount> personalAccountOfRequester = allAccountsOfRequester.stream().
                    filter(userAccount -> userAccount.getOrgId().equals(personalOrgId)).findFirst();

            boolean doMultiplePersonalTeamsExist = false;
            if (personalAccountOfRequester.isPresent()) {
                 List<AccessDomain> accessDomains = accessDomainRepository.findByAccountIdAndIsActive(personalAccountOfRequester.get().getAccountId(), true);
                 List<Long> uniqueEntityIds = new ArrayList<>();
                 for (AccessDomain accessDomain : accessDomains) {
                     if (!uniqueEntityIds.contains(accessDomain.getEntityId())) {
                         uniqueEntityIds.add(accessDomain.getEntityId());
                     }
                     if (uniqueEntityIds.size() > 1) {
                         doMultiplePersonalTeamsExist = true;
                         break;
                     }
                 }
            }

            for (Task filteredTask : filteredTasksByStatName) {
                Task task = new Task();
                BeanUtils.copyProperties(filteredTask, task);
                TaskMaster taskMaster = new TaskMaster();
                // filtration based on effort date -- if there is an effortDate provided and there is an effort on any task on that date then we include it. If there is no
                // effort on that date but the date is today's date and current activity indicator for any task is on then we include it
                if (statsRequest.getNewEffortDate() != null) {
                    if (timeSheetRepository.existsByEntityTypeIdAndEntityIdAndNewEffortDate(Constants.EntityTypes.TASK, task.getTaskId(), statsRequest.getNewEffortDate())) {
                        taskMaster.setNewEffortDate(statsRequest.getNewEffortDate());
                    } else if (isEffortDateOfToday && task.getCurrentActivityIndicator() == 1) {
                        taskMaster.setNewEffortDate(null);
                    } else {
                        continue;
                    }
                }

                taskServiceImpl.convertTaskAllServerDateAndTimeInToUserTimeZone(task, timeZone);
                BeanUtils.copyProperties(task, taskMaster);
                taskMaster.setTaskWorkflowType(task.getFkWorkflowTaskStatus().getFkWorkFlowType().getWorkflowName());
                taskMaster.setWorkflowTaskStatusType(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus());
                taskMaster.setTeamName(task.getFkTeamId().getTeamName());
                taskMaster.setTeamCode(task.getFkTeamId().getTeamCode());
                taskMaster.setTeamId(task.getFkTeamId().getTeamId());
                taskMaster.setOrgId(task.getFkOrgId().getOrgId());
                taskMaster.setTaskTypeId(task.getTaskTypeId());
                taskMaster.setTaskType(Constants.taskTypeMap.get(task.getTaskTypeId()));
                if(Objects.equals(task.getIsStarred(),true)) {
                    UserAccount user=task.getFkAccountIdStarredBy();
                    if(user != null && user.getAccountId() != null)
                    {
                        EmailFirstLastAccountIdIsActive starredByUser=userAccountRepository.getEmailFirstNameLastNameAccountIdIsActiveByAccountId(user.getAccountId());
                        taskMaster.setStarredBy(starredByUser);
                    }
                }
                taskMaster.setIsStarred(task.getIsStarred());
                if (task.getFkAccountIdAssigned() != null) {
                    String fullName = task.getFkAccountIdAssigned().getFkUserId().getFirstName() + " " + task.getFkAccountIdAssigned().getFkUserId().getLastName();
                    String email = task.getFkAccountIdAssigned().getEmail();
                    taskMaster.setEmail(email);
                    taskMaster.setFullName(fullName);
                }

                EmailFirstLastAccountId creator = new EmailFirstLastAccountId(task.getFkAccountIdCreator().getEmail(), task.getFkAccountIdCreator().getAccountId(), task.getFkAccountIdCreator().getFkUserId().getFirstName(), task.getFkAccountIdCreator().getFkUserId().getLastName());
                taskMaster.setCreatedBy(creator);

                if (task.getFkAccountIdObserver1() != null) {
                    EmailFirstLastAccountId observer = new EmailFirstLastAccountId(task.getFkAccountIdObserver1().getEmail(), task.getFkAccountIdObserver1().getAccountId(), task.getFkAccountIdObserver1().getFkUserId().getFirstName(), task.getFkAccountIdObserver1().getFkUserId().getLastName());
                    taskMaster.setObserver1(observer);
                }

                if (task.getFkAccountIdObserver2() != null) {
                    EmailFirstLastAccountId observer = new EmailFirstLastAccountId(task.getFkAccountIdObserver2().getEmail(), task.getFkAccountIdObserver2().getAccountId(), task.getFkAccountIdObserver2().getFkUserId().getFirstName(), task.getFkAccountIdObserver2().getFkUserId().getLastName());
                    taskMaster.setObserver2(observer);
                }

                if (task.getFkAccountIdMentor1() != null) {
                    EmailFirstLastAccountId mentor = new EmailFirstLastAccountId(task.getFkAccountIdMentor1().getEmail(), task.getFkAccountIdMentor1().getAccountId(), task.getFkAccountIdMentor1().getFkUserId().getFirstName(), task.getFkAccountIdMentor1().getFkUserId().getLastName());
                    taskMaster.setMentor1(mentor);
                }

                if (task.getFkAccountIdMentor2() != null) {
                    EmailFirstLastAccountId mentor = new EmailFirstLastAccountId(task.getFkAccountIdMentor2().getEmail(), task.getFkAccountIdMentor2().getAccountId(), task.getFkAccountIdMentor2().getFkUserId().getFirstName(), task.getFkAccountIdMentor2().getFkUserId().getLastName());
                    taskMaster.setMentor2(mentor);
                }

                if (task.getFkOrgId().getOrganizationName().equalsIgnoreCase(com.tse.core_application.model.Constants.PERSONAL_ORG)) {
                    if (Objects.equals(task.getFkTeamId().getTeamName(), com.tse.core_application.model.Constants.PERSONAL_ORG_DEFAULT_TEAM_NAME)) {
                        if (doMultiplePersonalTeamsExist) {
                            taskMaster.setTeamName(com.tse.core_application.model.Constants.PERSONAL_ORG_TEAM_DISPLAY_NAME_FOR_MULTIPLE_TEAMS);
                        } else {
                            taskMaster.setTeamName(com.tse.core_application.model.Constants.PERSONAL_ORG_TEAM_DISPLAY_NAME);
                        }
                    }
                }
                if(task.getFkAccountIdBlockedBy() != null)
                {
                    UserAccount user=task.getFkAccountIdBlockedBy();
                    if(user.getAccountId() != null)
                    {
                        EmailFirstLastAccountIdIsActive blockedByUser=userAccountRepository.getEmailFirstNameLastNameAccountIdIsActiveByAccountId(user.getAccountId());
                        taskMaster.setBlockedBy(blockedByUser);
                    }
                }
                if(task.getBlockedReasonTypeId()!=null)
                {
                    taskMaster.setBlockedReasonTypeId(task.getBlockedReasonTypeId());
                }
                if (task.getFkAccountIdLastUpdated() != null) {
                    UserAccount user = task.getFkAccountIdLastUpdated();
                    if (user.getAccountId() != null) {
                        EmailFirstLastAccountIdIsActive lastUpdatedByUser = userAccountRepository.getEmailFirstNameLastNameAccountIdIsActiveByAccountId(user.getAccountId());
                        taskMaster.setLastUpdatedBy(lastUpdatedByUser);
                    }
                }
                taskMasters.add(taskMaster);
            }
        }
        return taskMasters;
    }


    public List<TaskMaster> sortTaskMaster(StatsRequest statsRequest, List<TaskMaster> taskMasters) {
        HashMap<Integer, SortingField> sortingFields = statsRequest.getSortingPriorityList();

        if (sortingFields != null && !sortingFields.isEmpty()) {
            sortTaskListBySortFilters(taskMasters, sortingFields);
        } else if (statsRequest.getSearches() == null || statsRequest.getSearches().isEmpty()) {
            //default sorting of task master tasks
            synchronized (taskMasters) {
                Thread sortThread = new Thread(() -> {
                    taskMasters.sort(Comparator.comparing((TaskMaster task) -> {
                                if (task.getWorkflowTaskStatusType() != null) {
                                    // Assign workflow status values for sorting
                                    String workflowStatusType = task.getWorkflowTaskStatusType().toLowerCase();
                                    switch (workflowStatusType) {
                                        case Constants.WorkFlowTaskStatusConstants.STATUS_STARTED:
                                        case Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED:
                                        case Constants.WorkFlowTaskStatusConstants.STATUS_ON_HOLD:
                                        case Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED:
                                            return 1;
                                        case Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED:
                                            return 2;
                                        case Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG:
                                            return 3;
                                        case Constants.WorkFlowTaskStatusConstants.STATUS_DELETE:
                                            return 4;
                                    }
                                }
                                return 5;
                            }).thenComparing((TaskMaster task) -> {
                                if (task.getTaskPriority() != null) {
                                    // Assign priority values for sorting
                                    switch (task.getTaskPriority()) {
                                        case "P0":
                                            return 1;
                                        case "P1":
                                            return 2;
                                        case "P2":
                                            return 3;
                                        case "P3":
                                            return 4;
                                        case "P4":
                                            return 5;
                                    }
                                }
                                return 6; // Assign a value for tasks without priority
                            }).thenComparing(TaskMaster::getTaskExpEndDate, Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(TaskMaster::getLastUpdatedDateTime, Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(TaskMaster::getCreatedDateTime));
                });
                sortThread.start();

                try {
                    sortThread.join(); // Wait for the thread to complete
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        boolean isEffortDateOfToday = statsRequest.getNewEffortDate() != null && statsRequest.getNewEffortDate().equals(LocalDate.now());

        if (isEffortDateOfToday) {
            // if the effort date filter is provided and effort date is of today, we need to send the sorted order in which tasks with current activity indicator == 1 & effort date is null should come at the last. Here we are not changing the original order achieved in above sorting
            List<TaskMaster> sortedList = taskMasters.stream()
                    .filter(task -> task.getCurrentActivityIndicator() != 1 || task.getNewEffortDate() != null)
                    .collect(Collectors.toList());

            List<TaskMaster> delayedTasks = taskMasters.stream()
                    .filter(task -> task.getCurrentActivityIndicator() == 1 && task.getNewEffortDate() == null)
                    .collect(Collectors.toList());

            sortedList.addAll(delayedTasks);
            return sortedList;
        }

        return taskMasters;
    }

    private Integer getWorkflowStatusValue(String workflowStatusType) {
        if (workflowStatusType == null) return 5;
        switch (workflowStatusType) {
            case Constants.WorkFlowTaskStatusConstants.STATUS_STARTED:
            case Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED:
            case Constants.WorkFlowTaskStatusConstants.STATUS_ON_HOLD:
            case Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED:
                return 1;
            case Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED:
                return 2;
            case Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG:
                return 3;
            case Constants.WorkFlowTaskStatusConstants.STATUS_DELETE:
                return 4;
            default:
                return 5;
        }
    }

    private Integer getPriorityValue(String taskPriority) {
        if (taskPriority == null) return 6;
        switch (taskPriority) {
            case "P0":
                return 1;
            case "P1":
                return 2;
            case "P2":
                return 3;
            case "P3":
                return 4;
            case "P4":
                return 5;
            default:
                return 6;
        }
    }

    public List<Task> getTasks(StatsRequest taskListRequest) {
        List<Task> fetchedTasks = getTasksByFiltersForDashboard(taskListRequest, Constants.flags.STATS_MY_TASKS);

        return fetchedTasks;
    }

    public boolean tempAdd(TaskHistory th) {
        taskHistoryRepository.save(th);
        return true;
    }

    public boolean tempUpdate(Task task) {
//        Timestamp now = new Timestamp(Calendar.getInstance().getTimeInMillis());
//        task.setTaskProgressSystemLastUpdated(now);
        LocalDateTime now = LocalDateTime.now();
        task.setTaskProgressSystemLastUpdated(now);
        taskRepository.save(task);
        return true;
    }

    public List<TaskHistory> getAllTaskHistory() {
        return taskHistoryRepository.findAll();
    }

    public HashMap<String, List<HashMap<String, Object>>> getFiltersForUser(FiltersForUserRequest request) {
        HashMap<String, List<HashMap<String, Object>>> result = new HashMap<String, List<HashMap<String, Object>>>();
        Long userId = request.getUserId();
        List<String> filterFields = request.getFilterFields();
        List<Long> accountIds = userAccountService.getActiveAccountIdsForUserId(userId, null);
        List<Long> orgIds = userAccountService.getOrganizationIdsForUserId(userId);
        if (accountIds != null && accountIds.size() > 0) {
            Iterator<String> filterFieldsIterator = filterFields.iterator();
            while (filterFieldsIterator.hasNext()) {
                String filterField = filterFieldsIterator.next();
                Set<Object> fieldValues = new HashSet<>();
                List<HashMap<String, Object>> resultForField = new ArrayList<HashMap<String, Object>>();
                switch (filterField) {
                    case Constants.FIELDS_FOR_FILTERS.FIELD_PROJECT:
                    case Constants.FIELDS_FOR_FILTERS.FIELD_TEAM:
                        Integer entityTypeId = filterField.equals(Constants.FIELDS_FOR_FILTERS.FIELD_PROJECT) ? Constants.EntityTypes.PROJECT : Constants.EntityTypes.TEAM;
                        List<AccessDomain> mappingResults = accessDomainRepository.findByEntityTypeIdAndAccountIdIn(entityTypeId, accountIds);
                        if (mappingResults != null && mappingResults.size() > 0) {
                            List<Long> projectIds = mappingResults.stream().map(val -> val.getEntityId()).collect(Collectors.toList());

                            List<Project> projects = projectRepository.findByProjectIdIn(projectIds);

                            if (projects != null && projects.size() > 0) {
                                for (Project project : projects) {
                                    HashMap<String, Object> fieldTuple = new HashMap<String, Object>();
                                    fieldTuple.put("id", project.getProjectId());
                                    fieldTuple.put("displayName", project.getProjectName());
                                    resultForField.add(fieldTuple);
                                }
                            }
                        }
                        break;

                    // ToDO: exactly same way with team.

                    case Constants.FIELDS_FOR_FILTERS.FIELD_ORGANIZATION:
                        List<Organization> orgs = organizationRepository.findByOrgIdIn(orgIds);
                        if (orgs != null && orgs.size() > 0) {
                            for (Organization org : orgs) {
                                HashMap<String, Object> fieldTuple = new HashMap<String, Object>();
                                fieldTuple.put("id", org.getOrgId());
                                fieldTuple.put("displayName", org.getOrganizationName());
                                resultForField.add(fieldTuple);
                            }
                        }
                        break;
                }
                result.put(filterField, resultForField);
            }
        }
        return result;
    }

    public void tempEP() {
        HashMap<String, Object> filter = new HashMap<>() {
            {
                put("workflowStatusId", 1);
                put("sprintId", 1);
            }
        };
    }

    // to find the list of all fields that are being updated
//    public ArrayList<String> getFieldsToUpdate(Task task, Long taskId) {
//        Task taskDb = taskRepository.findByTaskId(taskId);
//        ArrayList<String> arrayListFields = new ArrayList<String>();
//        if (taskDb.getTaskExpEndDate() != null && task.getTaskExpEndDate() != null) { //
//            if (taskDb.getTaskExpEndDate().compareTo(task.getTaskExpEndDate()) != 0) {
//                arrayListFields.add("taskExpEndDate");
//            }
//        } else {
//            if (taskDb.getTaskExpEndDate() != null && task.getTaskExpEndDate() == null || taskDb.getTaskExpEndDate() == null && task.getTaskExpEndDate() != null) {
//                arrayListFields.add("taskExpEndDate");
//            }
//        }
//
//        if (taskDb.getTaskExpStartDate() != null && task.getTaskExpStartDate() != null) {
//            if (taskDb.getTaskExpStartDate().compareTo(task.getTaskExpStartDate()) != 0) {
//                arrayListFields.add("taskExpStartDate");
//            }
//        } else {
//            if (taskDb.getTaskExpStartDate() != null && task.getTaskExpStartDate() == null || taskDb.getTaskExpStartDate() == null && task.getTaskExpStartDate() != null) {
//                arrayListFields.add("taskExpStartDate");
//            }
//        }
//
//        if (taskDb.getTaskActEndDate() != null && task.getTaskActEndDate() != null) {
//            if (taskDb.getTaskActEndDate().compareTo(task.getTaskActEndDate()) != 0) {
//                arrayListFields.add("taskActEndDate");
//            }
//        } else {
//            if (taskDb.getTaskActEndDate() != null && task.getTaskActEndDate() == null || taskDb.getTaskActEndDate() == null && task.getTaskActEndDate() != null) {
//                arrayListFields.add("taskActEndDate");
//            }
//        }
//
//        if (taskDb.getTaskActStDate() != null && task.getTaskActStDate() != null) {
//            if (taskDb.getTaskActStDate().compareTo(task.getTaskActStDate()) != 0) {
//                arrayListFields.add("taskActStDate");
//            }
//        } else {
//            if (taskDb.getTaskActStDate() != null && task.getTaskActStDate() == null || taskDb.getTaskActStDate() == null && task.getTaskActStDate() != null) {
//                arrayListFields.add("taskActStDate");
//            }
//        }
//
//        if (taskDb.getTaskCompletionDate() != null && task.getTaskCompletionDate() != null) {
//            if (taskDb.getTaskCompletionDate().compareTo(task.getTaskCompletionDate()) != 0) {
//                arrayListFields.add("taskCompletionDate");
//            }
//        } else {
//            if (taskDb.getTaskCompletionDate() != null && task.getTaskCompletionDate() == null || taskDb.getTaskCompletionDate() == null && task.getTaskCompletionDate() != null) {
//                arrayListFields.add("taskCompletionDate");
//            }
//        }
//
//        if (task.getDependentTaskDetailRequestList() != null && !task.getDependentTaskDetailRequestList().isEmpty()) {
//            arrayListFields.add("dependencyIds");
//        }
//
//        ArrayList<HashMap<String, Object>> arrayList = new ArrayList<HashMap<String, Object>>();
//        HashMap<String, Object> mapTask = objectMapper.convertValue(task, HashMap.class);
//        mapTask.remove("taskExpStartDate");
//        mapTask.remove("taskExpEndDate");
//        mapTask.remove("taskActStDate");
//        mapTask.remove("taskActEndDate");
//        mapTask.remove("taskCompletionDate");
//        HashMap<String, Object> mapDb = objectMapper.convertValue(taskDb, HashMap.class);
//        mapDb.remove("taskExpStartDate");
//        mapDb.remove("taskExpEndDate");
//        mapDb.remove("taskActStDate");
//        mapDb.remove("taskActEndDate");
//        mapDb.remove("taskCompletionDate");
//        arrayList.add(mapDb);
//        arrayList.add(mapTask);
//        for (int i = 0; i < (arrayList.size() - 1); i++) {
//            for (Map.Entry<String, Object> entry : arrayList.get(i).entrySet()) {
//                String key = entry.getKey();
//                Object value1 = entry.getValue();
//                Object value2 = arrayList.get(i + 1).get(key);
//                if (!Objects.equals(value1, value2)) {
//                    arrayListFields.add(key);
//                }
//            }
//        }
//
//        // This condition is added because in some task there is some leading or trailing spaces so in updateFields we will not consider leading or trailing spaces so that Role-1 user will not get error
//        if (arrayListFields.contains("taskTitle") && Objects.equals(taskDb.getTaskTitle().trim(), task.getTaskTitle())) {
//            arrayListFields.remove("taskTitle");
//        }
//        if (arrayListFields.contains("taskDesc") && Objects.equals(taskDb.getTaskDesc().trim(), task.getTaskDesc())) {
//            arrayListFields.remove("taskDesc");
//        }
//        boolean isEqual = (task.getFkAccountIdAssigned() == null && taskDb.getFkAccountIdAssigned() == null) ||
//                (task.getFkAccountIdAssigned() != null && taskDb.getFkAccountIdAssigned() != null &&
//                        Objects.equals(task.getFkAccountIdAssigned().getAccountId(), taskDb.getFkAccountIdAssigned().getAccountId()));
//        if (isEqual) {
//            arrayListFields.remove("fkAccountIdAssigned");
//        }
//        if (task.getFkWorkflowTaskStatus() != null && taskDb.getFkWorkflowTaskStatus() != null && Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatusId(), taskDb.getFkWorkflowTaskStatus().getWorkflowTaskStatusId())) {
//            arrayListFields.remove("fkWorkflowTaskStatus");
//        }
//        return arrayListFields;
//    }

    public ArrayList<String> getFieldsToUpdate(Task task, Long taskId) {
        Task taskDb = taskRepository.findByTaskId(taskId);
        ArrayList<String> arrayListFields = new ArrayList<>();

        // ---- keep your explicit datetime comparisons (or fold into smartEquals if you prefer)
        compareDate("taskExpEndDate", taskDb.getTaskExpEndDate(), task.getTaskExpEndDate(), arrayListFields);
        compareDate("taskExpStartDate", taskDb.getTaskExpStartDate(), task.getTaskExpStartDate(), arrayListFields);
        compareDate("taskActEndDate", taskDb.getTaskActEndDate(), task.getTaskActEndDate(), arrayListFields);
        compareDate("taskActStDate", taskDb.getTaskActStDate(), task.getTaskActStDate(), arrayListFields);
        compareDate("taskCompletionDate", taskDb.getTaskCompletionDate(), task.getTaskCompletionDate(), arrayListFields);

        if (task.getDependentTaskDetailRequestList() != null && !task.getDependentTaskDetailRequestList().isEmpty()) {
            arrayListFields.add("dependencyIds");
        }

        // ---- fields we want to skip entirely (already handled or not persistent)
        Set<String> excluded = Set.of(
                "taskExpStartDate","taskExpEndDate","taskActStDate","taskActEndDate","taskCompletionDate",
                "newEffortTracks","childTaskList","parentTaskResponse","referenceWorkItemList",
                "linkedTaskList","dependentTaskDetailResponseList","dependentTaskDetailRequestList",
                "comments","notes","listOfDeliverablesDelivered"
        );

        Set<String> trimCompare = Set.of("taskTitle", "taskDesc");

        // Reflect over Task so we keep actual types (important for entity instanceof checks)
        for (Field taskField : Task.class.getDeclaredFields()) {
            String name = taskField.getName();
            if (excluded.contains(name)) continue;

            taskField.setAccessible(true);
            Object vDb;
            Object vNew;
            try {
                vDb  = taskField.get(taskDb);
                vNew = taskField.get(task);
            } catch (IllegalAccessException e) {
                // if we can't read it, skip safely
                continue;
            }

            // special: compare by ID only for specific entity types
            if (isUserAccountField(taskField)) {
                if (!Objects.equals(getUserAccountId(vDb), getUserAccountId(vNew))) arrayListFields.add(name);
                continue;
            }
            if (isTeamField(taskField)) {
                if (!Objects.equals(getTeamId(vDb), getTeamId(vNew))) arrayListFields.add(name);
                continue;
            }
            if (isEpicField(taskField)) {
                if (!Objects.equals(getEpicId(vDb), getEpicId(vNew))) arrayListFields.add(name);
                continue;
            }
            if (isProjectField(taskField)) {
                if (!Objects.equals(getProjectId(vDb), getProjectId(vNew))) arrayListFields.add(name);
                continue;
            }
            if (isOrgField(taskField)) {
                if (!Objects.equals(getOrgId(vDb), getOrgId(vNew))) arrayListFields.add(name);
                continue;
            }

            // lists: treat null == empty; ignore order for List<Long> and List<String>
            if (vDb instanceof List<?> || vNew instanceof List<?>) {
                if (!equalListsOrderInsensitive(asList(vDb), asList(vNew))) arrayListFields.add(name);
                continue;
            }

            // strings: optionally trim for selected fields
            if (trimCompare.contains(name) && vDb instanceof String && vNew instanceof String) {
                if (!Objects.equals(((String) vDb).trim(), ((String) vNew))) arrayListFields.add(name);
                continue;
            }

            if (vDb instanceof Integer || vNew instanceof Integer || vDb instanceof Long || vNew instanceof Long) {
                long longDb = (vDb == null) ? 0L : ((Number) vDb).longValue();
                long longNew = (vNew == null) ? 0L : ((Number) vNew).longValue();

                if (longDb != longNew) {
                    arrayListFields.add(name);
                }
                continue;
            }

            // default comparison
            if (!Objects.equals(vDb, vNew)) {
                arrayListFields.add(name);
            }
        }

        removeIfOnlyTrimDiff(arrayListFields, "taskTitle", taskDb.getTaskTitle(), task.getTaskTitle());
        removeIfOnlyTrimDiff(arrayListFields, "taskDesc",  taskDb.getTaskDesc(),  task.getTaskDesc());

        boolean accountIdAssigned =
                (task.getFkAccountIdAssigned() == null && taskDb.getFkAccountIdAssigned() == null) ||
                        (task.getFkAccountIdAssigned() != null && taskDb.getFkAccountIdAssigned() != null &&
                                Objects.equals(
                                        task.getFkAccountIdAssigned().getAccountId(),
                                        taskDb.getFkAccountIdAssigned().getAccountId()
                                )
                        );
        if (accountIdAssigned) arrayListFields.remove("fkAccountIdAssigned");

        if (task.getFkWorkflowTaskStatus() != null && taskDb.getFkWorkflowTaskStatus() != null &&
                Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatusId(),
                        taskDb.getFkWorkflowTaskStatus().getWorkflowTaskStatusId())) {
            arrayListFields.remove("fkWorkflowTaskStatus");
        }
        return arrayListFields;
    }

    private <T extends Comparable<T>> void compareDate(String field, T taskDbDate, T updateTaskDate, List<String> out) {
        if (taskDbDate != null && updateTaskDate != null) {
            if (taskDbDate.compareTo(updateTaskDate) != 0) {
                out.add(field);
            }
        } else if ((taskDbDate == null) != (updateTaskDate == null)) {
            out.add(field);
        }
    }

    private void removeIfOnlyTrimDiff(List<String> changed, String field, String oldField, String newField) {
        if (changed.contains(field)) {
            boolean equalTrim = (oldField == null && newField == null) ||
                    (oldField != null && newField != null && oldField.trim().equals(newField));
            if (equalTrim) changed.remove(field);
        }
    }

    private boolean isUserAccountField(Field field) { return UserAccount.class.isAssignableFrom(field.getType()); }
    private boolean isTeamField(Field field)        { return Team.class.isAssignableFrom(field.getType()); }
    private boolean isEpicField(Field field)        { return Epic.class.isAssignableFrom(field.getType()); }
    private boolean isProjectField(Field field)     { return Project.class.isAssignableFrom(field.getType()); }
    private boolean isOrgField(Field field)         { return Organization.class.isAssignableFrom(field.getType()); }

    private Long getUserAccountId(Object o) { return (o == null) ? null : ((UserAccount)o).getAccountId(); }
    private Long getTeamId(Object o)        { return (o == null) ? null : ((Team)o).getTeamId(); }
    private Long getEpicId(Object o)        { return (o == null) ? null : ((Epic)o).getEpicId(); }
    private Long getProjectId(Object o)     { return (o == null) ? null : ((Project)o).getProjectId(); }
    private Long getOrgId(Object o)         { return (o == null) ? null : ((Organization)o).getOrgId(); }

    /**
     * Treats null and empty list as equal.
     * For List<Long> and List<String>, ignores order.
     * If element type is neither Long nor String, falls back to order-sensitive comparison.
     */
    private boolean equalListsOrderInsensitive(List<?> taskDbList, List<?> updateTaskList) {
        List<?> listOfTaskDb = (taskDbList == null) ? Collections.emptyList() : taskDbList;
        List<?> listOfUpdateTask = (updateTaskList == null) ? Collections.emptyList() : updateTaskList;

        // both empty after normalization => equal
        if (listOfTaskDb.isEmpty() && listOfUpdateTask.isEmpty()) return true;

        // detect element type (best effort)
        Class<?> type = commonElementType(listOfTaskDb, listOfUpdateTask);

        if (type == Long.class) {
            return sortedEquals(mapTo(listOfTaskDb, Long.class), mapTo(listOfUpdateTask, Long.class));
        }
        if (type == String.class) {
            return sortedEquals(mapTo(listOfTaskDb, String.class), mapTo(listOfUpdateTask, String.class));
        }

        // Fallback: order-sensitive deep equals
        return Objects.equals(listOfTaskDb, listOfUpdateTask);
    }

    private Class<?> commonElementType(List<?> listOfTaskDb, List<?> listOfUpdateTask) {
        Class<?> taskDbList = firstNonNullType(listOfTaskDb);
        Class<?> updatedTaskList = firstNonNullType(listOfUpdateTask);
        if (taskDbList == null) return updatedTaskList;
        if (updatedTaskList == null) return taskDbList;
        if (taskDbList.equals(updatedTaskList)) return taskDbList;
        return Object.class;
    }

    private Class<?> firstNonNullType(List<?> l) {
        for (Object o : l) if (o != null) return o.getClass();
        return null;
    }

    private <T extends Comparable<? super T>> boolean sortedEquals(List<T> listOfTaskDb, List<T> listOfUpdateTask) {
        List<T> taskDbList = new ArrayList<>(listOfTaskDb);
        List<T> updatedTaskList = new ArrayList<>(listOfUpdateTask);
        Collections.sort(taskDbList);
        Collections.sort(updatedTaskList);
        return taskDbList.equals(updatedTaskList);
    }

    private <T> List<T> mapTo(List<?> src, Class<T> type) {
        List<T> out = new ArrayList<>(src.size());
        for (Object o : src) {
            if (o == null) continue;
            if (!type.isInstance(o)) throw new ClassCastException("Unexpected element type in list: " + o.getClass());
            out.add(type.cast(o));
        }
        return out;
    }

    private List<?> asList(Object o) {
        if (o == null) return null;
        if (o instanceof List<?>) return (List<?>) o;
        throw new IllegalArgumentException("Expected List, got: " + o.getClass());
    }


    /* This method is used to update some attributes of a task. Attributes such as accountIdCreator, accountIdLastUpdated,
     * taskNumber and accountIdAssignee of the task. */
    public Task initializeTaskNumberSetProperties(Task task) {
        task.setFkAccountIdCreator(task.getFkAccountId());
        task.setFkAccountIdLastUpdated(task.getFkAccountId());

//        TaskNumber taskNumberDb = taskRepository.getMaxTaskNumber();
//        if (taskNumberDb.getTaskNumber() == null) {
//            task.setTaskNumber(ControllerConstants.TaskNumber.taskNum);
//        } else {
//            task.setTaskNumber(taskNumberDb.getTaskNumber() + 1);
//        }
//        Long taskNumber = taskRepository.getNextTaskNumber();
        Long taskIdentifier = getNextTaskIdentifier(task.getFkTeamId().getTeamId());
        task.setTaskIdentifier(taskIdentifier);
        Team team = teamRepository.findByTeamId(task.getFkTeamId().getTeamId());
        task.setTaskNumber(team.getTeamCode() + "-" + taskIdentifier);

        if (task.getFkAccountIdAssigned() != null && task.getFkAccountIdAssigned().getAccountId() != null) {
            task.setFkAccountIdAssignee(task.getFkAccountId());
        }
        return task;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long getNextTaskIdentifier(Long teamId) {
        TaskSequence sequence = taskSequenceRepository.findByTeamIdForUpdate(teamId);

        if (sequence == null) {
            sequence = new TaskSequence(teamId, 0L);
        }
        Long nextTaskIdentifier = sequence.getLastTaskIdentifier() + 1;
        sequence.setLastTaskIdentifier(nextTaskIdentifier);
        taskSequenceRepository.save(sequence);
        return nextTaskIdentifier;
    }
    //  to check if  user is allowed to update
    public boolean isUpdateAllowed(String action, Long accountId, Long teamId) {
//        ArrayList<String> userActionList = actionService.getUserActionList(task.getFkAccountId().getAccountId());
        ArrayList<String> userActionList = actionService.getUserActionList(accountId, teamId);

        if (userActionList.contains(action)) {
            return true;
        } else {
            return false;
        }
    }

    //   commented on 2022-06-20: commented stats having underscore and TODAY in their name because stats with underscore and TODAY in their name will not be sent to frontend.
    public List<TaskMaster> getTaskDetailsForStatus(StatsRequest statsRequest, String timeZone, String accountIds) {
        List<Task> taskList;
        ArrayList<Task> allUniqueTasks = new ArrayList<>();

        statsService.setDefaultFromAndToDateToStatsRequest(statsRequest, timeZone);

        if (statsRequest.getAccountIds() != null) {
            taskList = getTasksByFiltersForDashboard(statsRequest, Constants.flags.STATS_MY_TASKS);
        } else {
            taskList = getTasksForAllTasksStatsCalculation(statsRequest, Constants.flags.STATS_ALL_TASKS, accountIds);
        }

        taskList = statsService.removeBacklogDeleteTasks(taskList);

        if (taskList != null) {
            for (Task task : taskList) {
                if (!allUniqueTasks.contains(task)) allUniqueTasks.add(task);
            }
        }

        // Todo: Stats: We simply need to filter the tasks based on the task progress system already set in the task so if we want all the tasks with task progress system delayed
        // we can simply filter them or better we can retrieve only those tasks from the repository directly where the task progress system is specified in the statsRequest.getStatName()
        List<Task> filteredTaskList = allUniqueTasks.stream().filter((task) -> {
            if (allUniqueTasks.size() > 0) {

                LocalDateTime serverCurrentDateTime = LocalDateTime.now();
                LocalDateTime convertedServerCurrentDateTimeByUserTimeZone = DateTimeUtils.convertServerDateToUserTimezone(serverCurrentDateTime, timeZone);
                LocalDate convertedServerCurrentDateTimeByUserTimeZoneDate = convertedServerCurrentDateTimeByUserTimeZone.toLocalDate();
                LocalDateTime dateTimeStart = LocalDateTime.of(convertedServerCurrentDateTimeByUserTimeZoneDate.getYear(), convertedServerCurrentDateTimeByUserTimeZoneDate.getMonth(), convertedServerCurrentDateTimeByUserTimeZoneDate.getDayOfMonth(), 00, 00, 00);
                LocalDateTime dateTimeEnd = LocalDateTime.of(convertedServerCurrentDateTimeByUserTimeZoneDate.getYear(), convertedServerCurrentDateTimeByUserTimeZoneDate.getMonth(), convertedServerCurrentDateTimeByUserTimeZoneDate.getDayOfMonth(), 23, 59, 59);
                LocalDateTime dateTimeStartInServerTimeZone = DateTimeUtils.convertUserDateToServerTimezone(dateTimeStart, timeZone);
                LocalDateTime dateTimeEndInServerTimeZone = DateTimeUtils.convertUserDateToServerTimezone(dateTimeEnd, timeZone);

                if (task.getTaskProgressSystem() != null) {
//                    StatType statusType = statsRequest.getStatName().get(0);
                    for (StatType statusType : statsRequest.getStatName()) {
                        switch (statusType) {
                            case DELAYED:
                                if (statsRequest.getCurrentDate() != null) {
                                    if ((task.getTaskExpEndDate().isAfter(dateTimeStartInServerTimeZone) || task.getTaskExpEndDate().isEqual(dateTimeStartInServerTimeZone)) && ((task.getTaskExpEndDate().isBefore(dateTimeEndInServerTimeZone) || task.getTaskExpEndDate().isEqual(dateTimeEndInServerTimeZone)))) {
                                        return task.getTaskProgressSystem().equals(StatType.DELAYED);
                                    }
                                } else {
                                    return task.getTaskProgressSystem().equals(StatType.DELAYED);
                                }
                                break;
                            case WATCHLIST:
                                if (statsRequest.getCurrentDate() != null) {
                                    if ((task.getTaskExpEndDate().isAfter(dateTimeStartInServerTimeZone) || task.getTaskExpEndDate().isEqual(dateTimeStartInServerTimeZone)) && ((task.getTaskExpEndDate().isBefore(dateTimeEndInServerTimeZone) || task.getTaskExpEndDate().isEqual(dateTimeEndInServerTimeZone)))) {
                                        return task.getTaskProgressSystem().equals(StatType.WATCHLIST);
                                    }
                                } else {
                                    return task.getTaskProgressSystem().equals(StatType.WATCHLIST);
                                }
                                break;
                            case ONTRACK:
                                if (statsRequest.getCurrentDate() != null) {
                                    if ((task.getTaskExpEndDate().isAfter(dateTimeStartInServerTimeZone) || task.getTaskExpEndDate().isEqual(dateTimeStartInServerTimeZone)) && ((task.getTaskExpEndDate().isBefore(dateTimeEndInServerTimeZone) || task.getTaskExpEndDate().isEqual(dateTimeEndInServerTimeZone)))) {
                                        return task.getTaskProgressSystem().equals(StatType.ONTRACK);
                                    }
                                } else {
                                    return task.getTaskProgressSystem().equals(StatType.ONTRACK);
                                }
                                break;
                            case COMPLETED:
                            case LATE_COMPLETION:
                                if (statsRequest.getCurrentDate() != null) {
                                    /* Below line is commented as comparison needs to be done on actual end date not expected end date for a completed task -- changes made in task 3401 */
//                                    if ((task.getTaskExpEndDate().isAfter(dateTimeStartInServerTimeZone) || task.getTaskExpEndDate().isEqual(dateTimeStartInServerTimeZone)) && ((task.getTaskExpEndDate().isBefore(dateTimeEndInServerTimeZone) || task.getTaskExpEndDate().isEqual(dateTimeEndInServerTimeZone)))) {
                                    if (task.getTaskActEndDate() != null && (task.getTaskActEndDate().isAfter(dateTimeStartInServerTimeZone) || task.getTaskActEndDate().isEqual(dateTimeStartInServerTimeZone)) && ((task.getTaskActEndDate().isBefore(dateTimeEndInServerTimeZone) || task.getTaskActEndDate().isEqual(dateTimeEndInServerTimeZone)))) {
                                        return (task.getTaskProgressSystem().equals(StatType.COMPLETED)) || (task.getTaskProgressSystem().equals(StatType.LATE_COMPLETION));
                                    }
                                } else {
                                    return (task.getTaskProgressSystem().equals(StatType.COMPLETED)) || (task.getTaskProgressSystem().equals(StatType.LATE_COMPLETION));
                                }
                                break;
                            case NOTSTARTED:
                                if (statsRequest.getCurrentDate() != null) {
                                    if ((task.getTaskExpEndDate().isAfter(dateTimeStartInServerTimeZone) || task.getTaskExpEndDate().isEqual(dateTimeStartInServerTimeZone)) && ((task.getTaskExpEndDate().isBefore(dateTimeEndInServerTimeZone) || task.getTaskExpEndDate().isEqual(dateTimeEndInServerTimeZone)))) {
                                        return task.getTaskProgressSystem().equals(StatType.NOTSTARTED);
                                    }
                                } else {
                                    return task.getTaskProgressSystem().equals(StatType.NOTSTARTED);
                                }
                                break;
                        }
                    }
                }
            }
            return false;
        }).collect(Collectors.toList());

        List<TaskMaster> taskStatusDetailsList = new ArrayList<>();
        for (Task filteredTask : filteredTaskList) {
            Task task = new Task();
            BeanUtils.copyProperties(filteredTask, task);

            TaskMaster taskStatusDetails = new TaskMaster();
            taskServiceImpl.convertTaskAllServerDateAndTimeInToUserTimeZone(task, timeZone);
            BeanUtils.copyProperties(task, taskStatusDetails);
            taskStatusDetails.setTaskWorkflowType(task.getFkWorkflowTaskStatus().getFkWorkFlowType().getWorkflowName());
            taskStatusDetails.setWorkflowTaskStatusType(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus());
            taskStatusDetails.setTeamName(task.getFkTeamId().getTeamName());
            taskStatusDetails.setTeamId(task.getFkTeamId().getTeamId());
            taskStatusDetails.setOrgId(task.getFkOrgId().getOrgId());
            taskStatusDetails.setTeamCode(task.getFkTeamId().getTeamCode());
            taskStatusDetails.setTaskTypeId(task.getTaskTypeId());
            taskStatusDetails.setTaskType(Constants.taskTypeMap.get(task.getTaskTypeId()));
            if (task.getFkAccountIdAssigned() != null) {
                String fullName = task.getFkAccountIdAssigned().getFkUserId().getFirstName() + " " + task.getFkAccountIdAssigned().getFkUserId().getLastName();
                String email = task.getFkAccountIdAssigned().getEmail();
                taskStatusDetails.setEmail(email);
                taskStatusDetails.setFullName(fullName);
            }
            taskStatusDetails.setCreatedBy(new EmailFirstLastAccountId(task.getFkAccountIdCreator().getEmail(), task.getFkAccountIdCreator().getAccountId(), task.getFkAccountIdCreator().getFkUserId().getFirstName(), task.getFkAccountIdCreator().getFkUserId().getLastName()));
            taskStatusDetailsList.add(taskStatusDetails);
            if (task.getFkAccountIdObserver1() != null) {
                EmailFirstLastAccountId observer = new EmailFirstLastAccountId(task.getFkAccountIdObserver1().getEmail(), task.getFkAccountIdObserver1().getAccountId(), task.getFkAccountIdObserver1().getFkUserId().getFirstName(), task.getFkAccountIdObserver1().getFkUserId().getLastName());
                taskStatusDetails.setObserver1(observer);
            }

            if (task.getFkAccountIdObserver2() != null) {
                EmailFirstLastAccountId observer = new EmailFirstLastAccountId(task.getFkAccountIdObserver2().getEmail(), task.getFkAccountIdObserver2().getAccountId(), task.getFkAccountIdObserver2().getFkUserId().getFirstName(), task.getFkAccountIdObserver2().getFkUserId().getLastName());
                taskStatusDetails.setObserver2(observer);
            }
            if(Objects.equals(task.getIsStarred(),true)) {
                UserAccount user=task.getFkAccountIdStarredBy();
                if(user != null && user.getAccountId() != null)
                {
                    EmailFirstLastAccountIdIsActive starredByUser=userAccountRepository.getEmailFirstNameLastNameAccountIdIsActiveByAccountId(user.getAccountId());
                    taskStatusDetails.setStarredBy(starredByUser);
                }
            }
            taskStatusDetails.setIsStarred(task.getIsStarred());

            if (task.getFkAccountIdMentor1() != null) {
                EmailFirstLastAccountId mentor = new EmailFirstLastAccountId(task.getFkAccountIdMentor1().getEmail(), task.getFkAccountIdMentor1().getAccountId(), task.getFkAccountIdMentor1().getFkUserId().getFirstName(), task.getFkAccountIdMentor1().getFkUserId().getLastName());
                taskStatusDetails.setMentor1(mentor);
            }

            if (task.getFkAccountIdMentor2() != null) {
                EmailFirstLastAccountId mentor = new EmailFirstLastAccountId(task.getFkAccountIdMentor2().getEmail(), task.getFkAccountIdMentor2().getAccountId(), task.getFkAccountIdMentor2().getFkUserId().getFirstName(), task.getFkAccountIdMentor2().getFkUserId().getLastName());
                taskStatusDetails.setMentor2(mentor);
            }
        }
        if (statsRequest.getSortingPriorityList() != null && !statsRequest.getSortingPriorityList().isEmpty()) {
            HashMap<Integer, SortingField> sortingFields = statsRequest.getSortingPriorityList();
            sortTaskListBySortFilters(taskStatusDetailsList, sortingFields);
        }
        return taskStatusDetailsList;
    }

    //  this method is used to get all users for assigning the task
    public List<EmailFirstLastAccountIdIsActive> getAllUsersAssignForTask(Long orgId, Long teamId, User user) {
        List<EmailFirstLastAccountIdIsActive> responseList = new ArrayList<>();

        if (teamId > 0) {
            if (Objects.equals(teamId, teamRepository.findByTeamName(Constants.PERSONAL_ORG_DEFAULT_TEAM_NAME).getTeamId())) {
                UserAccount userAccount = userAccountRepository.findByOrgIdAndFkUserIdUserIdAndIsActive(orgId, user.getUserId(), true);
                EmailFirstLastAccountIdIsActive response = new EmailFirstLastAccountIdIsActive();
                response.setEmail(userAccount.getEmail());
                response.setAccountId(userAccount.getAccountId());
                response.setFirstName(user.getFirstName());
                response.setLastName(user.getLastName());
                response.setIsActive(userAccount.getIsActive());
                responseList.add(response);
                return responseList;
            }
            List<Integer> teamAdminRoleList = new ArrayList<>(Constants.TEAM_ADMIN_ROLE);
            //teamAdminRoleList.add(RoleEnum.TEAM_VIEWER.getRoleId());
            List<AccountIdIsActive> accountIdsActiveStatus = accessDomainRepository.findDistinctAccountIdIsActiveByEntityTypeIdAndEntityIdAndRoleIdNotIn(Constants.EntityTypes.TEAM, teamId, teamAdminRoleList);

            for (AccountIdIsActive accountIdIsActive : accountIdsActiveStatus) {
                EmailFirstLastAccountId emailFirstLastAccountId = userAccountRepository.getEmailFirstNameLastNameAccountIdByAccountId(accountIdIsActive.getAccountId());

                if (emailFirstLastAccountId != null) {
                    EmailFirstLastAccountIdIsActive response = new EmailFirstLastAccountIdIsActive();
                    response.setEmail(emailFirstLastAccountId.getEmail());
                    response.setAccountId(emailFirstLastAccountId.getAccountId());
                    response.setFirstName(emailFirstLastAccountId.getFirstName());
                    response.setLastName(emailFirstLastAccountId.getLastName());
                    response.setIsActive(accountIdIsActive.getIsActive());

                    responseList.add(response);
                }
            }
        } else {
            UserAccount userAccount = userAccountRepository.findByOrgIdAndFkUserIdUserIdAndIsActive(orgId, user.getUserId(), true);
            EmailFirstLastAccountIdIsActive response = new EmailFirstLastAccountIdIsActive();
            response.setEmail(userAccount.getEmail());
            response.setAccountId(userAccount.getAccountId());
            response.setFirstName(user.getFirstName());
            response.setLastName(user.getLastName());
            response.setIsActive(userAccount.getIsActive());

            responseList.add(response);
        }

        responseList.sort(Comparator
                .comparing((EmailFirstLastAccountIdIsActive status) -> {
                    if (status.getIsActive() == null) return 2;
                    return status.getIsActive() ? 0 : 1;
                })
                .thenComparing(EmailFirstLastAccountIdIsActive::getFirstName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(EmailFirstLastAccountIdIsActive::getLastName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(EmailFirstLastAccountIdIsActive::getAccountId, Comparator.nullsLast(Long::compareTo))
        );

        return responseList;
        // why do we have this else part? how can teamId be negative?
//        else {
//            //add a flag to EmailFirstLastAccountId Map -- this must be returning all accountids
//            List<AccountId> accountIds = userAccountRepository.findAccountIdByOrgId(orgId);
//            List<EmailFirstLastAccountId> emailFirstLastAccountIdList = new ArrayList<>();
//            for (AccountId accountId : accountIds) {
//                EmailFirstLastAccountId emailFirstLastAccountId = userAccountRepository.getEmailFirstNameLastNameAccountIdByAccountId(accountId.getAccountId());
//                emailFirstLastAccountIdList.add(emailFirstLastAccountId);
//            }
//            return emailFirstLastAccountIdList;
//        }
    }

    @Deprecated(since = "2023-01-10")
    public List<EmailNameOrg> getUsersByAllTeamAndOrg(AllTeamAndOrg allTeamAndOrg) {
        List<EmailNameOrg> emailNameOrgList = new ArrayList<>();

        if (allTeamAndOrg.getAllTeam() != null) {
            //found all teams db
            List<Team> allTeams = teamService.getTeamsByTeamIds(allTeamAndOrg.getAllTeam());
            List<TeamIdOrgId> allUniqueOrg = new ArrayList<>();
            for (Team team : allTeams) {
                TeamIdOrgId teamIdOrgId = new TeamIdOrgId();
                teamIdOrgId.setTeamId(team.getTeamId());
                teamIdOrgId.setOrgId(team.getFkOrgId().getOrgId());
                if (!allUniqueOrg.contains(teamIdOrgId)) {
                    allUniqueOrg.add(teamIdOrgId);
                }
            }
            for (TeamIdOrgId teamIdOrgId : allUniqueOrg) {
                List<UserAccount> userAccounts = userAccountService.getAllUserAccountsByOrgId(teamIdOrgId.getOrgId());
                for (UserAccount userAccount : userAccounts) {
                    EmailNameOrg emailNameOrg = new EmailNameOrg();
                    emailNameOrg.setEmail(userAccount.getEmail());
                    emailNameOrg.setAccountId(userAccount.getAccountId());
                    emailNameOrg.setFirstName(userAccount.getFkUserId().getFirstName());
                    emailNameOrg.setLastName(userAccount.getFkUserId().getLastName());
                    OrgIdOrgName organization = organizationService.getOrganizationByOrgId(userAccount.getOrgId());
                    emailNameOrg.setOrg(organization.getOrganizationName());
                    emailNameOrg.setTeamId(teamIdOrgId.getTeamId());
                    emailNameOrgList.add(emailNameOrg);
                }
            }
        }
        return emailNameOrgList;
    }

    /**
     * @param allTeamAndOrg
     * @return List<EmailNameOrgCustomModel>
     * This method receives a list of TeamIds which are basically teams of the user making the request.
     * This method will return details of all accounts in those teams
     */
    public List<EmailNameOrgCustomModel> getAllUsersByAllTeamAndOrg(AllTeamAndOrg allTeamAndOrg, List<Long> accountIdList) {
//        List<EmailNameOrg> emailNameOrgList = new ArrayList<>();

//        if (allTeamAndOrg.getAllTeam() != null) {
//            List<Team> allTeams = teamService.getTeamsByTeamIds(allTeamAndOrg.getAllTeam());
//            List<TeamIdOrgId> allUniqueOrg = new ArrayList<>();
//            for (Team team : allTeams) {
//                TeamIdOrgId teamIdOrgId = new TeamIdOrgId();
//                teamIdOrgId.setTeamId(team.getTeamId());
//                teamIdOrgId.setOrgId(team.getFkOrgId().getOrgId());
//                if (!allUniqueOrg.contains(teamIdOrgId)) {
//                    allUniqueOrg.add(teamIdOrgId);
//                }
//            }
//
//            for (TeamIdOrgId teamIdOrgId : allUniqueOrg) {
//                List<AccountId> distinctAccountIdFoundDb = accessDomainService.findAllDistinctAccountIdByEntityTypeIdAndEntityId(Constants.EntityTypes.TEAM, Math.toIntExact(teamIdOrgId.getTeamId()));
//                List<UserAccount> userAccounts = userAccountService.getAllUserAccountsByAccountIds(distinctAccountIdFoundDb);
//                for (UserAccount userAccount : userAccounts) {
//                    EmailNameOrg emailNameOrg = new EmailNameOrg();
//                    emailNameOrg.setEmail(userAccount.getEmail());
//                    emailNameOrg.setAccountId(userAccount.getAccountId());
//                    emailNameOrg.setFirstName(userAccount.getFkUserId().getFirstName());
//                    emailNameOrg.setLastName(userAccount.getFkUserId().getLastName());
//                    OrgIdOrgName organization = organizationService.getOrganizationByOrgId(userAccount.getOrgId());
//                    emailNameOrg.setOrg(organization.getOrganizationName());
//                    emailNameOrg.setTeamId(teamIdOrgId.getTeamId());
//                    emailNameOrgList.add(emailNameOrg);
//                }
//            }
//        }
//        emailNameOrgList = accessDomainRepository.getEmailNameOrgList(allTeamAndOrg.getAllTeam(), Constants.EntityTypes.TEAM);
//        return emailNameOrgList;
        List<Long> teamIdsList = allTeamAndOrg.getAllTeam();
        //need to make changes here
        List<EmailNameOrgCustomModel> response = new ArrayList<>();
        boolean checkHrAttendenceValidation=false;
        if (attendanceService.checkHrAccessForAttendenceForTeamsAndOrgs(teamIdsList,allTeamAndOrg.getAllOrg(),accountIdList)) {
            checkHrAttendenceValidation = true;
            response = accessDomainRepository.getEmailNameOrgActiveStatusList(teamIdsList, Constants.EntityTypes.TEAM, Constants.PERSONAL_TEAM_ID, accountIdList);
        }
        else {
            List<Long> userTeamIds = accessDomainRepository.findDistinctEntityIdsByEntityTypeIdAndEntityIdInAndAccountIdInAndIsActive(Constants.EntityTypes.TEAM, teamIdsList, accountIdList, true);

            response = accessDomainRepository.getEmailNameOrgActiveStatusList(userTeamIds, Constants.EntityTypes.TEAM, Constants.PERSONAL_TEAM_ID, accountIdList);
        }
        if (response != null && !response.isEmpty()) {
            response.sort(Comparator
                    .comparing((EmailNameOrgCustomModel o) -> {
                        if (o.getIsActive() == null) return 2;
                        return o.getIsActive() ? 0 : 1;
                    })
                    .thenComparing(o -> (o.getFirstName() != null ? o.getFirstName().toString() : null), Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(o -> (o.getLastName() != null ? o.getLastName().toString() : null), Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(EmailNameOrgCustomModel::getAccountId, Comparator.nullsLast(Long::compareTo))
            );
        }

        return response;
    }

    //  this method is used to get all the users for observing the task
    public List<EmailFirstLastAccountIdIsActive> getAllUsersObserverForTask(Long orgId, Long teamId, Long taskId) {
        List<EmailFirstLastAccountIdIsActive> responseList = new ArrayList<>();

        if (teamId > 0) {
            if (Objects.equals(teamId, teamRepository.findByTeamName(Constants.PERSONAL_ORG_DEFAULT_TEAM_NAME).getTeamId())) {
                return Collections.emptyList();
            }
            List<Integer> teamAdminRoleList = new ArrayList<>(Constants.TEAM_ADMIN_ROLE);
            List<AccountIdIsActive> accountIdsActiveStatus = accessDomainRepository.findDistinctAccountIdIsActiveByEntityTypeIdAndEntityIdAndRoleIdNotIn(Constants.EntityTypes.TEAM, teamId, teamAdminRoleList);
            HashSet<Long> accountIdsSet = accountIdsActiveStatus.stream().map(AccountIdIsActive::getAccountId).collect(Collectors.toCollection(HashSet::new));
            Long selfAccountId = null;
            if (taskId != null) {
                Task task = taskRepository.findByTaskId(taskId);
                if (accountIdsSet.contains(task.getFkAccountIdAssigned().getAccountId())) {
                    selfAccountId = task.getFkAccountIdAssigned().getAccountId();
                }
            }

            for (AccountIdIsActive accountIdIsActive : accountIdsActiveStatus) {
                if (selfAccountId != null && Objects.equals(accountIdIsActive.getAccountId(), selfAccountId)) {
                    continue;
                }
                EmailFirstLastAccountId emailFirstLastAccountId = userAccountRepository.getEmailFirstNameLastNameAccountIdByAccountId(accountIdIsActive.getAccountId());

                if (emailFirstLastAccountId != null) {
                    EmailFirstLastAccountIdIsActive response = new EmailFirstLastAccountIdIsActive();
                    response.setEmail(emailFirstLastAccountId.getEmail());
                    response.setAccountId(emailFirstLastAccountId.getAccountId());
                    response.setFirstName(emailFirstLastAccountId.getFirstName());
                    response.setLastName(emailFirstLastAccountId.getLastName());
                    response.setIsActive(accountIdIsActive.getIsActive());

                    responseList.add(response);
                }
            }
        }

        responseList.sort(Comparator
                .comparing((EmailFirstLastAccountIdIsActive status) -> {
                    if (status.getIsActive() == null) return 2;
                    return status.getIsActive() ? 0 : 1;
                })
                .thenComparing(EmailFirstLastAccountIdIsActive::getFirstName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(EmailFirstLastAccountIdIsActive::getLastName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(EmailFirstLastAccountIdIsActive::getAccountId, Comparator.nullsLast(Long::compareTo))
        );

        return responseList;
        // why do we have this else part? teamId can never be negative
//        else {
//
//            List<AccountId> accountIds = userAccountRepository.findAccountIdByOrgId(orgId);
//
//            HashSet<Long> accountIdsSet = accountIds.stream().map(AccountId::getAccountId).collect(Collectors.toCollection(HashSet::new));
//            Long selfAccountId = null;
//            for(Long id : accountIdsList){
//                if(accountIdsSet.contains(id)){
//                    selfAccountId = id;
//                    break;
//                }
//            }
//            List<EmailFirstLastAccountId> emailFirstLastAccountIdList = new ArrayList<>();
//            for (AccountId accountId : accountIds) {
//                if(selfAccountId != null && !Objects.equals(accountId.getAccountId(), selfAccountId)) {
//                    EmailFirstLastAccountId emailFirstLastAccountId = userAccountRepository.getEmailFirstNameLastNameAccountIdByAccountId(accountId.getAccountId());
//                    emailFirstLastAccountIdList.add(emailFirstLastAccountId);
//                }
//            }
//            return emailFirstLastAccountIdList;
//        }
    }

    // this method is used to get all the users for mentoring the task
    public List<EmailFirstLastAccountIdIsActive> getAllUsersMentorForTask(Long orgId, Long teamId, Long taskId) {
        List<EmailFirstLastAccountIdIsActive> responseList = new ArrayList<>();
        if (teamId > 0) {
            if (Objects.equals(teamId, teamRepository.findByTeamName(Constants.PERSONAL_ORG_DEFAULT_TEAM_NAME).getTeamId())) {
                return Collections.emptyList();
            }
            List<Integer> teamAdminRoleList = new ArrayList<>(Constants.TEAM_ADMIN_ROLE);
            List<AccountIdIsActive> accountIdsActiveStatus = accessDomainRepository.findDistinctAccountIdIsActiveByEntityTypeIdAndEntityIdAndRoleIdNotIn(Constants.EntityTypes.TEAM, teamId, teamAdminRoleList);

            HashSet<Long> accountIdsSet = accountIdsActiveStatus.stream().map(AccountIdIsActive::getAccountId).collect(Collectors.toCollection(HashSet::new));
            Long selfAccountId = null;
            if (taskId != null) {
                Task task = taskRepository.findByTaskId(taskId);
                if (accountIdsSet.contains(task.getFkAccountIdAssigned().getAccountId())) {
                    selfAccountId = task.getFkAccountIdAssigned().getAccountId();
                }
            }

            for (AccountIdIsActive accountIdIsActive : accountIdsActiveStatus) {
                if (selfAccountId != null && Objects.equals(accountIdIsActive.getAccountId(), selfAccountId)) {
                    continue;
                }
                EmailFirstLastAccountId emailFirstLastAccountId = userAccountRepository.getEmailFirstNameLastNameAccountIdByAccountId(accountIdIsActive.getAccountId());

                if (emailFirstLastAccountId != null) {
                    EmailFirstLastAccountIdIsActive response = new EmailFirstLastAccountIdIsActive();
                    response.setEmail(emailFirstLastAccountId.getEmail());
                    response.setAccountId(emailFirstLastAccountId.getAccountId());
                    response.setFirstName(emailFirstLastAccountId.getFirstName());
                    response.setLastName(emailFirstLastAccountId.getLastName());
                    response.setIsActive(accountIdIsActive.getIsActive());

                    responseList.add(response);
                }

            }
        }

        responseList.sort(Comparator
                .comparing((EmailFirstLastAccountIdIsActive status) -> {
                    if (status.getIsActive() == null) return 2;
                    return status.getIsActive() ? 0 : 1;
                })
                .thenComparing(EmailFirstLastAccountIdIsActive::getFirstName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(EmailFirstLastAccountIdIsActive::getLastName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(EmailFirstLastAccountIdIsActive::getAccountId, Comparator.nullsLast(Long::compareTo))
        );

        return responseList;

//        why do we need the else part -- teamId can not be negative
//        else {
//            List<AccountId> accountIds = userAccountRepository.findAccountIdByOrgId(orgId);
//
//            HashSet<Long> accountIdsSet = accountIds.stream().map(AccountId::getAccountId).collect(Collectors.toCollection(HashSet::new));
//            Long selfAccountId = null;
//            for(Long id : accountIdsList){
//                if(accountIdsSet.contains(id)){
//                    selfAccountId = id;
//                    break;
//                }
//            }
//            List<EmailFirstLastAccountId> emailFirstLastAccountIdList = new ArrayList<>();
//            for (AccountId accountId : accountIds) {
//                if(selfAccountId != null && !Objects.equals(accountId.getAccountId(), selfAccountId)) {
//
//                    EmailFirstLastAccountId emailFirstLastAccountId = userAccountRepository.getEmailFirstNameLastNameAccountIdByAccountId(accountId.getAccountId());
//                    emailFirstLastAccountIdList.add(emailFirstLastAccountId);
//                }
//            }
//            return emailFirstLastAccountIdList;
//        }
    }

    //    this method is used to get the task by its task number
    public Task getTaskByTaskNumber(TaskNumberRequest taskNumberRequest, String timeZone) {
        Task responseTask = new Task();
        Task foundTaskDb = null;

        foundTaskDb = getTaskByTaskNumberRequest(taskNumberRequest);

        if (foundTaskDb != null) {
            BeanUtils.copyProperties(foundTaskDb, responseTask);
//            List<CommentProjection> commentProjections = commentRepository.findCommentProjectionByTaskId(responseTask.getTaskId());
            List<TaskAttachmentMetadata> taskAttachmentMetadataList = taskAttachmentRepository.findTaskAttachmentMetadataByTaskId(responseTask.getTaskId());

            Map<Long, List<TaskAttachmentMetadata>> taskAttachmentMetadataMap = taskAttachmentMetadataList.stream()
                    .collect(Collectors.groupingBy(TaskAttachmentMetadata::getCommentLogId));

//            List<Comment> comments = commentProjections.stream().map(cp -> {
//                Comment comment = new Comment();
//                comment.setCommentLogId(cp.getCommentLogId());
//                comment.setCommentId(cp.getCommentId());
//                comment.setComment(cp.getComment());
//                comment.setCommentsTags(cp.getCommentsTags());
//                comment.setPostedByAccountId(cp.getPostedByAccountId());
//                comment.setCreatedDateTime(cp.getCreatedDateTime());
//                comment.setLastUpdatedDateTime(cp.getLastUpdatedDateTime());
//
//                List<TaskAttachment> taskAttachments = taskAttachmentMetadataMap.getOrDefault(cp.getCommentLogId(), new ArrayList<>()).stream().map(tam -> {
//                    TaskAttachment taskAttachment = new TaskAttachment();
//                    BeanUtils.copyProperties(tam, taskAttachment);
//                    return taskAttachment;
//                }).collect(Collectors.toList());
//
//                comment.setTaskAttachments(taskAttachments);
//                return comment;
//            }).collect(Collectors.toList());
//
            responseTask.setComments(Collections.emptyList());

            List<Task> linkedTasks = new ArrayList<>();
            switch (responseTask.getTaskTypeId()) {
                case Constants.TaskTypes.PARENT_TASK:
                case Constants.TaskTypes.BUG_TASK:
                    List<Task> childTasks = taskRepository.findByTaskIdIn(responseTask.getChildTaskIds());
                    if (responseTask.getBugTaskRelation() != null && !responseTask.getBugTaskRelation().isEmpty()) {
                        linkedTasks = taskRepository.findByTaskIdIn(responseTask.getBugTaskRelation());
                    }
                    responseTask.setChildTaskList(taskServiceImpl.createChildTaskResponse(childTasks, timeZone));
                    responseTask.setLinkedTaskList(taskServiceImpl.createLinkedTaskResponse(linkedTasks, timeZone));
                    break;

                case Constants.TaskTypes.CHILD_TASK:
                    Task parentTask = taskRepository.findByTaskId(responseTask.getParentTaskId());
                    ParentTaskResponse parentTaskResponse = new ParentTaskResponse();
                    BeanUtils.copyProperties(parentTask, parentTaskResponse);
                    // accountId assigned can be null in case the parent task is in backlog
                    parentTaskResponse.setAccountIdAssigned(parentTask.getFkAccountIdAssigned() != null ? parentTask.getFkAccountIdAssigned().getAccountId() : null);
                    parentTaskResponse.setWorkflowTaskStatus(parentTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus());
                    responseTask.setParentTaskResponse(parentTaskResponse);
                    break;

                case Constants.TaskTypes.TASK:
                    if (responseTask.getBugTaskRelation() != null && !responseTask.getBugTaskRelation().isEmpty()) {
                        linkedTasks = taskRepository.findByTaskIdIn(responseTask.getBugTaskRelation());
                    }
                    responseTask.setLinkedTaskList(taskServiceImpl.createLinkedTaskResponse(linkedTasks, timeZone));
                    break;
            }

            setDependentTaskDetailsResponse(responseTask);
            taskServiceImpl.convertTaskAllServerDateAndTimeInToUserTimeZone(responseTask, timeZone);
            if (responseTask.getNoteId() != null && responseTask.getNotes() != null) {
                responseTask.setNotes(noteService.removeDeletedNotes(responseTask.getNotes()));
//                if(foundTaskDb.getNotes() == null)
//                    foundTaskDb.setNoteId(null);
            }

            // -------------------------------FOR TASK - 2012  REMOVING DELETED DELIVERABLES FROM TASK IN DB---------------------

            if (responseTask.getListOfDeliverablesDeliveredId() != null && responseTask.getListOfDeliverablesDelivered() != null) {
                responseTask.setListOfDeliverablesDelivered(deliverablesDeliveredService.removeDeletedlistOfDeliverablesDelivered(responseTask.getListOfDeliverablesDelivered()));
            }
            // ----------------------------------- ENDS HERE ----------------------------------------------------------------
        } else {
            throw new TaskNotFoundException();
        }

        return responseTask;
    }

    /**
     * Set immediate predecessor and successor details in the get task response
     */
    public void setDependentTaskDetailsResponse(Task foundTaskDb) {
        if (foundTaskDb.getDependencyIds() != null && !foundTaskDb.getDependencyIds().isEmpty()) {
            List<Dependency> relatedDependencies = dependencyRepository.findByDependencyIdInAndIsRemoved(foundTaskDb.getDependencyIds(), false);

            List<DependentTaskDetailResponse> dependentTaskDetails = new ArrayList<>();

            for (Dependency dependency : relatedDependencies) {
                DependentTaskDetailResponse detailResponse = new DependentTaskDetailResponse();
                Task relatedTask;

                if (dependency.getPredecessorTaskId().equals(foundTaskDb.getTaskId())) {
                    detailResponse.setRelationDirection(RelationDirection.SUCCESSOR);
                    relatedTask = taskRepository.findByTaskId(dependency.getSuccessorTaskId());
                } else {
                    detailResponse.setRelationDirection(RelationDirection.PREDECESSOR);
                    relatedTask = taskRepository.findByTaskId(dependency.getPredecessorTaskId());
                }

                BeanUtils.copyProperties(relatedTask, detailResponse);
                detailResponse.setRelationTypeId(dependency.getRelationTypeId());
                detailResponse.setAccountIdAssigned(relatedTask.getFkAccountIdAssigned() != null ? relatedTask.getFkAccountIdAssigned().getAccountId() : null);
                detailResponse.setWorkflowTaskStatus(relatedTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus());
                detailResponse.setDependencyId(dependency.getDependencyId());
                detailResponse.setTeamId(relatedTask.getFkTeamId().getTeamId());
                dependentTaskDetails.add(detailResponse);
            }
            foundTaskDb.setDependentTaskDetailResponseList(dependentTaskDetails);
        }
    }

    public void setReferencedTaskDetailsResponse(Task taskDb){
        List<ReferenceWorkItem> referenceWorkItemList;
        referenceWorkItemList = taskServiceImpl.findReferenceWorkItemList(taskDb);
        taskDb.setReferenceWorkItemList(referenceWorkItemList);
    }

    public boolean isTaskViewAllowed(Task task, String accountIdStr) {
        String[] allAccountIds = accountIdStr.split(",");
        List<Long> accountIds = new ArrayList<>();
        for (String accountId : allAccountIds) {
            accountIds.add(Long.valueOf(accountId));
        }
        boolean isViewTaskAllowed = false;
        if (!Objects.equals(task.getTaskWorkflowId(), Constants.TaskWorkFlowIds.PERSONAL_TASK)) {
            // Todo: add isActive condition
            List<CustomAccessDomain> foundAccessDomainsDb = accessDomainService.getAccessDomainsByAccountIdsAndEntityId(task.getFkTeamId().getTeamId(), accountIds);
            for (CustomAccessDomain accessDomain : foundAccessDomainsDb) {
                ArrayList<Integer> actionIds = new ArrayList<>();
                ArrayList<ActionId> actionIdsForRoleId = roleActionService.getActionIdByRoleId(accessDomain.getRoleId());

                for (ActionId actionId : actionIdsForRoleId) {
                    actionIds.add(actionId.getActionId());
                }

                if (actionIds.contains(Constants.ActionId.TEAM_TASK_VIEW)) {
                    isViewTaskAllowed = true;
                } else {
                    if (actionIds.contains(Constants.ActionId.TASK_BASIC_UPDATE)) {
                        if (task.getFkAccountIdAssigned() != null) {
                            if (accountIds.contains(task.getFkAccountIdAssigned().getAccountId())) {
                                isViewTaskAllowed = true;
                            }
                        }
                    }
                }
            }
        } else {
            isViewTaskAllowed = true;
        }
        return isViewTaskAllowed;
    }

    /*@deprecated: this method was created when custom logics were written to convert the date from local timezone into the UTC timezone*/
    @Deprecated(since = "2022-07-19")
    public Task getTaskByTimeZone(Task task, String timeZone) {
        if (task != null) {
            if (Double.parseDouble(timeZone) > 0) {
                Task addedOneDayToDatesOfTask = addOneDayToDatesOfTask(task);
                Task addedAdditionalOneDayToDate = checkForAdditionOfOneDayToDateForPositiveTimeZone(addedOneDayToDatesOfTask, timeZone);
                return addedAdditionalOneDayToDate;
            } else {
                Task subtractedOneDayFromDate = checkForSubtractionOfOneDayFromDateForNegativeTimeZone(task, timeZone);
                return subtractedOneDayFromDate;
            }
        }
        return null;
    }

    /*@deprecated: this method was created when custom logics were written to convert the date from local timezone into the UTC timezone*/
    @Deprecated(since = "2022-07-19")
    public Task addOneDayToDatesOfTask(Task task) {
        int daysToAdd = 1;
        ArrayList<String> listOfFields = new ArrayList<>();
        listOfFields.add("taskExpStartDate");
        listOfFields.add("taskExpEndDate");
        listOfFields.add("taskActStDate");
        listOfFields.add("taskActEndDate");
//        listOfFields.add("taskCompletionDate");

        for (String fieldName : listOfFields) {
            try {
                Field field = Task.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object fieldValue = field.get(task);
                if (fieldValue != null) {
                    Date date = (Date) fieldValue;
                    Date formattedFieldValue = this.addDays(date, daysToAdd);
                    field.set(task, formattedFieldValue);
                }
            } catch (IllegalArgumentException | IllegalAccessException | SecurityException | NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
        return task;
    }

    /*@deprecated: this method was created when custom logics were written to convert the date from local timezone into the UTC timezone*/
    @Deprecated(since = "2022-07-19")
    public Date conditionalAdditionOfOneDayToDateForNegativeTimeZone(String startingTime, String baseTime, String timeToCheckForInBetween, Date taskDate, String timeZone) {
        Date finalDate = null;
        if (isTimeBetweenTwoTime(startingTime, baseTime, timeToCheckForInBetween, timeZone)) {
            Date originalDate = taskDate;
            finalDate = addDays(originalDate, 1);
        } else {
            Date originalDate = taskDate;
            finalDate = originalDate;
        }
        return finalDate;
    }

    //    @deprecated: this method was created as part of the custom/temporary logic to convert the date of a task from local timezone into the UTC timezone
    @Deprecated(since = "2022-07-19")
    public Task checkForAdditionOfOneDayToDateForNegativeTimeZone(Task task, String timeZone) {
        String startingTime = "24:00:00";
        String timeZoneHours = timeZone.substring(1, 3);
        String timeZoneMinutes = timeZone.substring(3, 5);
        int intTimeZoneHours = 0;
        String timeZoneHoursString = null;
        if (timeZoneMinutes.contains("00")) {
            intTimeZoneHours = 24 - Integer.parseInt(timeZoneHours);
            timeZoneHoursString = intTimeZoneHours + ":" + "00" + ":" + "00";
        } else {
            timeZoneHours = timeZoneHours + 1;
            intTimeZoneHours = 24 - Integer.parseInt(timeZoneHours);
            timeZoneHoursString = intTimeZoneHours + ":" + "30" + ":" + "00";
        }
        String currentTime = null;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        String baseTime = null;
        try {
            java.util.Date date1 = simpleDateFormat.parse(timeZoneHoursString);
            baseTime = simpleDateFormat.format(date1);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if (task.getTaskActStDate() != null && task.getTaskActStTime() != null) {
//            Date updatedDate = conditionalAdditionOfOneDayToDateForNegativeTimeZone(startingTime, baseTime, task.getTaskActStTime().toString(), task.getTaskActStDate(), timeZone);
//            task.setTaskActStDate(updatedDate);
        }

        if (task.getTaskActEndDate() != null && task.getTaskActEndTime() != null) {
//            Date updatedDate = conditionalAdditionOfOneDayToDateForNegativeTimeZone(startingTime, baseTime, task.getTaskActEndTime().toString(), task.getTaskActEndDate(), timeZone);
//            task.setTaskActEndDate(updatedDate);
        }

        if (task.getTaskExpStartDate() != null && task.getTaskExpStartTime() != null) {
//            Date updatedDate = conditionalAdditionOfOneDayToDateForNegativeTimeZone(startingTime, baseTime, task.getTaskExpStartTime().toString(), task.getTaskExpStartDate(), timeZone);
//            task.setTaskExpStartDate(updatedDate);
        }

        if (task.getTaskExpEndDate() != null && task.getTaskExpEndTime() != null) {
//            Date updatedDate = conditionalAdditionOfOneDayToDateForNegativeTimeZone(startingTime, baseTime, task.getTaskExpEndTime().toString(), task.getTaskExpEndDate(), timeZone);
//            task.setTaskExpEndDate(updatedDate);
        }

//        if (task.getTaskCompletionDate() != null && task.getTaskCompletionTime() != null) {
//            Date updatedDate = conditionalAdditionOfOneDayToDateForNegativeTimeZone(startingTime, baseTime, task.getTaskCompletionTime().toString(), task.getTaskCompletionDate(), timeZone);
//            task.setTaskCompletionDate(updatedDate);
//        }
        return task;
    }

    //    @deprecated: this method was written as part of the custom logic to change the time field of a task from local timezone to UTC timezone
    @Deprecated(since = "2022-07-19")
    public static boolean isTimeBetweenTwoTime(String startingTime, String baseTime, String timeToCheckForInBetween, String timeZone) {
        boolean valid = false;

        try {
            java.util.Date startingTimeDate = new SimpleDateFormat("HH:mm:ss").parse(startingTime);
            Calendar calendar1 = Calendar.getInstance();
            calendar1.setTime(startingTimeDate);

            java.util.Date baseTimeDate = new SimpleDateFormat("HH:mm:ss").parse(baseTime);
            Calendar calendar3 = Calendar.getInstance();
            calendar3.setTime(baseTimeDate);

            java.util.Date timeToCheckForInBetweenDate = new SimpleDateFormat("HH:mm:ss").parse(timeToCheckForInBetween);
            Calendar calendar2 = Calendar.getInstance();
            calendar2.setTime(timeToCheckForInBetweenDate);

            if (Double.parseDouble(timeZone) > 0) {
                if (calendar2.before(calendar3)) {
                    valid = true;
                }
            } else {
                if (calendar2.after(calendar3) && calendar2.before(calendar1)) {
                    valid = true;
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return valid;
    }

    /*@deprecated: this method is used to add the given number of days to the given date. this method was created and used as part of the custom logic while
     * changing the task local date into the UTC timezone*/
    @Deprecated(since = "2022-07-19")
    public Date addDays(Date originalDate, int daysToAdd) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(originalDate);
        calendar.add(Calendar.DATE, daysToAdd);
        return new Date(calendar.getTimeInMillis());
    }

    /*@deprecated: this method is used to subtract the given number of days from the given date. this method was created and used as part of the custom logic
     * while converting the task local date into the UTC timezone*/
    @Deprecated(since = "2022-07-19")
    public Date subtractDays(Date originalDate, int daysToSubtract) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(originalDate);
        calendar.add(Calendar.DATE, -daysToSubtract);
        return new Date(calendar.getTimeInMillis());
    }

    /*@deprecated: this method was created and used as part of the custom logic to change the task date from local timezone into the UTC timezone*/
    @Deprecated(since = "2022-07-19")
    public Task checkForSubtractionOfOneDayFromDateForPositiveTimeZone(Task task, String timeZone) {
        String startingTime = "00:00:00";
        String timeZoneHours = timeZone.substring(1, 3);
        String timeZoneMinutes = timeZone.substring(3, 5);
        int intTimeZoneHours = 0;
        String timeZoneHoursString = null;
        if (timeZoneMinutes.contains("00")) {
            intTimeZoneHours = 00 + Integer.parseInt(timeZoneHours);
            timeZoneHoursString = intTimeZoneHours + ":" + "00" + ":" + "00";
        } else {
            intTimeZoneHours = 00 + Integer.parseInt(timeZoneHours);
            timeZoneHoursString = intTimeZoneHours + ":" + "30" + ":" + "00";
        }
        String currentTime = null;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        String baseTime = null;
        try {
            java.util.Date date1 = simpleDateFormat.parse(timeZoneHoursString);
            baseTime = simpleDateFormat.format(date1);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if (task.getTaskActStDate() != null && task.getTaskActStTime() != null) {
//            Date updatedDate = conditionalSubtractionOfOneDayFromDateForPositiveTimeZone(startingTime, baseTime, task.getTaskActStTime().toString(), task.getTaskActStDate(), timeZone);
//            task.setTaskActStDate(updatedDate);
        }

        if (task.getTaskActEndDate() != null && task.getTaskActEndTime() != null) {
//            Date updatedDate = conditionalSubtractionOfOneDayFromDateForPositiveTimeZone(startingTime, baseTime, task.getTaskActEndTime().toString(), task.getTaskActEndDate(), timeZone);
//            task.setTaskActEndDate(updatedDate);
        }

        if (task.getTaskExpStartDate() != null && task.getTaskExpStartTime() != null) {
//            Date updatedDate = conditionalSubtractionOfOneDayFromDateForPositiveTimeZone(startingTime, baseTime, task.getTaskExpStartTime().toString(), task.getTaskExpStartDate(), timeZone);
//            task.setTaskExpStartDate(updatedDate);
        }

        if (task.getTaskExpEndDate() != null && task.getTaskExpEndTime() != null) {
//            Date updatedDate = conditionalSubtractionOfOneDayFromDateForPositiveTimeZone(startingTime, baseTime, task.getTaskExpEndTime().toString(), task.getTaskExpEndDate(), timeZone);
//            task.setTaskExpEndDate(updatedDate);
        }

        if (task.getTaskCompletionDate() != null && task.getTaskCompletionTime() != null) {
//            Date updatedDate = conditionalSubtractionOfOneDayFromDateForPositiveTimeZone(startingTime, baseTime, task.getTaskCompletionTime().toString(), task.getTaskCompletionDate(), timeZone);
//            task.setTaskCompletionDate(updatedDate);
        }

        return task;

    }

    /*@deprecated: this method was created and used as part of the custom logic to change the task date from local timezone into the UTC timezone*/
    @Deprecated(since = "2022-07-19")
    public Date conditionalSubtractionOfOneDayFromDateForPositiveTimeZone(String startingTime, String baseTime, String timeToCheckForInBetween, Date taskDate, String timeZone) {
        Date finalDate = null;
        if (isTimeBetweenTwoTime(startingTime, baseTime, timeToCheckForInBetween, timeZone)) {
            Date originalDate = taskDate;
            finalDate = subtractDays(originalDate, 1);
        } else {
            Date originalDate = taskDate;
            finalDate = originalDate;
        }
        return finalDate;

    }

    /*@deprecated: this method was created and used as part of the custom logic to change the task date from local timezone into the UTC timezone*/
    @Deprecated(since = "2022-07-19")
    public Task checkForAdditionOfOneDayToDateForPositiveTimeZone(Task task, String timeZone) {
        String startingTime = "00:00:00";
        String timeZoneHours = timeZone.substring(1, 3);
        String timeZoneMinutes = timeZone.substring(3, 5);
        int intTimeZoneHours = 0;
        String timeZoneHoursString = null;
        if (timeZoneMinutes.contains("00")) {
            intTimeZoneHours = 00 + Integer.parseInt(timeZoneHours);
            timeZoneHoursString = intTimeZoneHours + ":" + "00" + ":" + "00";
        } else {
            intTimeZoneHours = 00 + Integer.parseInt(timeZoneHours);
            timeZoneHoursString = intTimeZoneHours + ":" + "30" + ":" + "00";
        }
        String currentTime = null;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        String baseTime = null;
        try {
            java.util.Date date1 = simpleDateFormat.parse(timeZoneHoursString);
            baseTime = simpleDateFormat.format(date1);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if (task.getTaskActStDate() != null && task.getTaskActStTime() != null) {
//            Date updatedDate = conditionalAdditionOfOneDayToDateForPositiveTimeZone(startingTime, baseTime, task.getTaskActStTime().toString(), task.getTaskActStDate(), timeZone);
//            task.setTaskActStDate(updatedDate);
        }

        if (task.getTaskActEndDate() != null && task.getTaskActEndTime() != null) {
//            Date updatedDate = conditionalAdditionOfOneDayToDateForPositiveTimeZone(startingTime, baseTime, task.getTaskActEndTime().toString(), task.getTaskActEndDate(), timeZone);
//            task.setTaskActEndDate(updatedDate);
        }

        if (task.getTaskExpStartDate() != null && task.getTaskExpStartTime() != null) {
//            Date updatedDate = conditionalAdditionOfOneDayToDateForPositiveTimeZone(startingTime, baseTime, task.getTaskExpStartTime().toString(), task.getTaskExpStartDate(), timeZone);
//            task.setTaskExpStartDate(updatedDate);
        }

        if (task.getTaskExpEndDate() != null && task.getTaskExpEndTime() != null) {
//            Date updatedDate = conditionalAdditionOfOneDayToDateForPositiveTimeZone(startingTime, baseTime, task.getTaskExpEndTime().toString(), task.getTaskExpEndDate(), timeZone);
//            task.setTaskExpEndDate(updatedDate);
        }

//        if (task.getTaskCompletionDate() != null && task.getTaskCompletionTime() != null) {
//            Date updatedDate = conditionalAdditionOfOneDayToDateForPositiveTimeZone(startingTime, baseTime, task.getTaskCompletionTime().toString(), task.getTaskCompletionDate(), timeZone);
//            task.setTaskCompletionDate(updatedDate);
//        }
        return task;

    }

    /*@deprecated: this method was created and used as part of the custom logic to change the task date from local timezone into the UTC timezone*/
    @Deprecated(since = "2022-07-19")
    public Date conditionalAdditionOfOneDayToDateForPositiveTimeZone(String startingTime, String baseTime, String timeToCheckForInBetween, Date taskDate, String timeZone) {
        Date finalDate = null;
        if (isTimeBetweenTwoTime(startingTime, baseTime, timeToCheckForInBetween, timeZone)) {
            Date originalDate = taskDate;
            finalDate = addDays(originalDate, 1);
        } else {
            Date originalDate = taskDate;
            finalDate = originalDate;
        }
        return finalDate;
    }

    /*@deprecated: this method was created and used as part of the custom logic to change the task date from local timezone into the UTC timezone*/
    @Deprecated(since = "2022-07-19")
    public Task checkForSubtractionOfOneDayFromDateForNegativeTimeZone(Task task, String timeZone) {
        String startingTime = "24:00:00";
        String timeZoneHours = timeZone.substring(1, 3);
        String timeZoneMinutes = timeZone.substring(3, 5);
        int intTimeZoneHours = 0;
        String timeZoneHoursString = null;
        if (timeZoneMinutes.contains("00")) {
            intTimeZoneHours = 24 - Integer.parseInt(timeZoneHours);
            timeZoneHoursString = intTimeZoneHours + ":" + "00" + ":" + "00";
        } else {
            timeZoneHours = timeZoneHours + 1;
            intTimeZoneHours = 24 - Integer.parseInt(timeZoneHours);
            timeZoneHoursString = intTimeZoneHours + ":" + "30" + ":" + "00";
        }
        String currentTime = null;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        String baseTime = null;
        try {
            java.util.Date date1 = simpleDateFormat.parse(timeZoneHoursString);
            baseTime = simpleDateFormat.format(date1);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if (task.getTaskActStDate() != null && task.getTaskActStTime() != null) {
//            Date updatedDate = conditionalSubtractionOfOneDayFromDateForNegativeTimeZone(startingTime, baseTime, task.getTaskActStTime().toString(), task.getTaskActStDate(), timeZone);
//            task.setTaskActStDate(updatedDate);
        }

        if (task.getTaskActEndDate() != null && task.getTaskActEndTime() != null) {
//            Date updatedDate = conditionalSubtractionOfOneDayFromDateForNegativeTimeZone(startingTime, baseTime, task.getTaskActEndTime().toString(), task.getTaskActEndDate(), timeZone);
//            task.setTaskActEndDate(updatedDate);
        }

        if (task.getTaskExpStartDate() != null && task.getTaskExpStartTime() != null) {
//            Date updatedDate = conditionalSubtractionOfOneDayFromDateForNegativeTimeZone(startingTime, baseTime, task.getTaskExpStartTime().toString(), task.getTaskExpStartDate(), timeZone);
//            task.setTaskExpStartDate(updatedDate);
        }

        if (task.getTaskExpEndDate() != null && task.getTaskExpEndTime() != null) {
//            Date updatedDate = conditionalSubtractionOfOneDayFromDateForNegativeTimeZone(startingTime, baseTime, task.getTaskExpEndTime().toString(), task.getTaskExpEndDate(), timeZone);
//            task.setTaskExpEndDate(updatedDate);
        }

//        if (task.getTaskCompletionDate() != null && task.getTaskCompletionTime() != null) {
//            Date updatedDate = conditionalSubtractionOfOneDayFromDateForNegativeTimeZone(startingTime, baseTime, task.getTaskCompletionTime().toString(), task.getTaskCompletionDate(), timeZone);
//            task.setTaskCompletionDate(updatedDate);
//        }

        return task;

    }

    /*@deprecated: this method was created and used as part of the custom logic to change the task date from local timezone into the UTC timezone*/
    @Deprecated(since = "2022-07-19")
    public Date conditionalSubtractionOfOneDayFromDateForNegativeTimeZone(String startingTime, String baseTime, String timeToCheckForInBetween, Date taskDate, String timeZone) {
        Date finalDate = null;
        if (isTimeBetweenTwoTime(startingTime, baseTime, timeToCheckForInBetween, timeZone)) {
            Date originalDate = taskDate;
            finalDate = subtractDays(originalDate, 1);
        } else {
            Date originalDate = taskDate;
            finalDate = originalDate;
        }
        return finalDate;

    }

    public void broadcastMessage(Task task) {
        Notification notification = new Notification(task);
        List<Long> distinctUserIdsToSendMessage = new ArrayList<>();
        List<Long> userIdsToSendMessage = new ArrayList<>();
        if (task.getImmediateAttention() != null) {
            if (!Objects.equals(task.getImmediateAttention(), 0) && task.getImmediateAttentionFrom() != null && task.getImmediateAttentionReason() != null) {
                if (task.getFkAccountIdMentor1() != null) {
                    UserAccount userAccountDb = userAccountService.getActiveUserAccountByAccountId(task.getFkAccountIdMentor1().getAccountId());
                    userIdsToSendMessage.add(userAccountDb.getFkUserId().getUserId());
                }
                if (task.getFkAccountIdMentor2() != null) {
                    UserAccount userAccountDb = userAccountService.getActiveUserAccountByAccountId(task.getFkAccountIdMentor2().getAccountId());
                    userIdsToSendMessage.add(userAccountDb.getFkUserId().getUserId());
                }

                if (task.getFkAccountIdObserver1() != null) {
                    UserAccount userAccountDb = userAccountService.getActiveUserAccountByAccountId(task.getFkAccountIdObserver1().getAccountId());
                    userIdsToSendMessage.add(userAccountDb.getFkUserId().getUserId());
                }

                if (task.getFkAccountIdObserver2() != null) {
                    UserAccount userAccountDb = userAccountService.getActiveUserAccountByAccountId(task.getFkAccountIdObserver2().getAccountId());
                    userIdsToSendMessage.add(userAccountDb.getFkUserId().getUserId());
                }

                if (!Objects.equals(task.getImmediateAttention(), 0) && task.getImmediateAttentionFrom() != null && task.getImmediateAttentionReason() != null) {
                    List<UserAccount> userAccounts = userAccountRepository.findByEmail(task.getImmediateAttentionFrom());
                    userIdsToSendMessage.add(userAccounts.get(0).getFkUserId().getUserId());
                }
            }
        }
        for (Long userId : userIdsToSendMessage) {
            if (!distinctUserIdsToSendMessage.contains(userId)) {
                distinctUserIdsToSendMessage.add(userId);
            }
        }
        for (Long userId : distinctUserIdsToSendMessage) {
            simpMessagingTemplate.convertAndSendToUser(String.valueOf(userId), Constants.WebSocket.SEND_MESSAGE_DESTINATION, notification);
        }

    }

    //  get all users for immediate attention of the task
    public List<EmailFirstLastAccountId> getAllImmediateAttentionUsersForTask(Long orgId, Long teamId, Long taskId) {
        List<EmailFirstLastAccountId> emailFirstLastAccountIdList = new ArrayList<>();

        if (teamId > 0) {
            if (Objects.equals(teamId, teamRepository.findByTeamName(Constants.PERSONAL_ORG_DEFAULT_TEAM_NAME).getTeamId())) {
                return Collections.emptyList();
            }
            List<Integer> teamAdminRoleList = new ArrayList<>(Constants.TEAM_ADMIN_ROLE);
            List<AccountIdIsActive> accountIdsActiveStatus = accessDomainRepository.findDistinctAccountIdIsActiveByEntityTypeIdAndEntityIdAndRoleIdNotIn(Constants.EntityTypes.TEAM, teamId, teamAdminRoleList);
            HashSet<Long> accountIdsSet = accountIdsActiveStatus.stream().map(AccountIdIsActive::getAccountId).collect(Collectors.toCollection(HashSet::new));
            List<AccountId> accountIdsActive = new ArrayList<>();
            for(AccountIdIsActive account:accountIdsActiveStatus) {
                accountIdsActive.add(new AccountId(account.getAccountId()));
            }
            Long selfAccountId = null;
            if (taskId != null) {
                Task task = taskRepository.findByTaskId(taskId);
                if (accountIdsSet.contains(task.getFkAccountIdAssigned().getAccountId())) {
                    selfAccountId = task.getFkAccountIdAssigned().getAccountId();
                }
            }

            for (AccountId accountId : accountIdsActive) {
                if (selfAccountId != null && Objects.equals(accountId.getAccountId(), selfAccountId)) {
                    continue;
                }
                EmailFirstLastAccountId emailFirstLastAccountId = userAccountRepository.getEmailFirstNameLastNameAccountIdByAccountId(accountId.getAccountId());
                emailFirstLastAccountIdList.add(emailFirstLastAccountId);

            }
        } else {
            List<AccountId> accountIds = userAccountRepository.findAccountIdByOrgIdAndIsActive(orgId, true);

            HashSet<Long> accountIdsSet = accountIds.stream().map(AccountId::getAccountId).collect(Collectors.toCollection(HashSet::new));
            Long selfAccountId = null;
            if (taskId != null) {
                Task task = taskRepository.findByTaskId(taskId);
                if (accountIdsSet.contains(task.getFkAccountIdAssigned().getAccountId())) {
                    selfAccountId = task.getFkAccountIdAssigned().getAccountId();
                }
            }

            for (AccountId accountId : accountIds) {
                if (selfAccountId != null && Objects.equals(accountId.getAccountId(), selfAccountId)) {
                    continue;
                }

                EmailFirstLastAccountId emailFirstLastAccountId = userAccountRepository.getEmailFirstNameLastNameAccountIdByAccountId(accountId.getAccountId());
                emailFirstLastAccountIdList.add(emailFirstLastAccountId);

            }
        }

        emailFirstLastAccountIdList.sort(Comparator
                .comparing(EmailFirstLastAccountId::getFirstName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(EmailFirstLastAccountId::getLastName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(EmailFirstLastAccountId::getAccountId, Comparator.nullsLast(Long::compareTo))
        );

        return emailFirstLastAccountIdList;
    }

    public List<ScheduledTaskViewResponse> getAllScheduledTasksForUser(Long userId) {

        List<UserAccount> userAccounts = userAccountService.getAllUserAccountByUserIdAndIsActive(userId);

        List<Long> accountIdList = new ArrayList<>();
        for (UserAccount userAccount : userAccounts) {
            accountIdList.add(userAccount.getAccountId());
        }

        List<Task> tasks = taskRepository.findByFkAccountIdAssignedAccountIdInAndCurrentlyScheduledTaskIndicator(accountIdList, true);

        List<ScheduledTaskViewResponse> activeTasks = new ArrayList<>();
        for (Task task : tasks) {
            ScheduledTaskViewResponse scheduledTaskViewResponse = new ScheduledTaskViewResponse();

            scheduledTaskViewResponse.setAccountIdAssigned(task.getFkAccountIdAssigned().getAccountId());
            scheduledTaskViewResponse.setTaskNumber(task.getTaskNumber());
            scheduledTaskViewResponse.setTaskIdentifier(task.getTaskIdentifier());
            scheduledTaskViewResponse.setTeamId(task.getFkTeamId().getTeamId());
            scheduledTaskViewResponse.setTaskTitle(task.getTaskTitle());
            scheduledTaskViewResponse.setTaskId(task.getTaskId());
            scheduledTaskViewResponse.setTaskTypeId(task.getTaskTypeId());
            scheduledTaskViewResponse.setTaskDesc(task.getTaskDesc());
            scheduledTaskViewResponse.setCurrentlyScheduledTaskIndicator(task.getCurrentlyScheduledTaskIndicator());
            scheduledTaskViewResponse.setCurrentActivityIndicator(task.getCurrentActivityIndicator());
            scheduledTaskViewResponse.setTaskWorkflowId(task.getTaskWorkflowId());
            scheduledTaskViewResponse.setWorkflowTaskStatus(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus());

            activeTasks.add(scheduledTaskViewResponse);
        }

        return activeTasks;

    }

    public List<ScheduledTaskViewResponse> getAllScheduledTasksByFilter(Long userId, ScheduledTasksViewRequest scheduledTasksViewRequest) {

        List<ScheduledTaskViewResponse> tasksByFilter = new ArrayList<>();
        Long orgId = scheduledTasksViewRequest.getOrgId();
        Long buId = scheduledTasksViewRequest.getBuId();
        Long projectId = scheduledTasksViewRequest.getProjectId();
        Long teamId = scheduledTasksViewRequest.getTeamId();

        if ((orgId != null && buId == null && projectId == null && teamId == null) ||
                (orgId == null && buId != null && projectId == null && teamId == null) ||
                (orgId == null && buId == null && projectId != null && teamId == null) ||
                (orgId == null && buId == null && projectId == null && teamId != null)) {
            List<UserAccount> userAccounts = userAccountService.getAllUserAccountByUserIdAndIsActive(userId);

            List<Long> accountIdList = new ArrayList<>();
            for (UserAccount userAccount : userAccounts) {
                accountIdList.add(userAccount.getAccountId());
            }


            List<Task> tasks;

            if (orgId != null) {
                tasks = taskRepository.findByFkOrgIdOrgIdAndFkAccountIdAssignedAccountIdInAndCurrentlyScheduledTaskIndicator(orgId, accountIdList, true);

            } else if (buId != null) {
                tasks = taskRepository.findByBuIdAndFkAccountIdAssignedAccountIdInAndCurrentlyScheduledTaskIndicator(buId, accountIdList, true);


            } else if (projectId != null) {
                tasks = taskRepository.findByFkProjectIdProjectIdAndFkAccountIdAssignedAccountIdInAndCurrentlyScheduledTaskIndicator(projectId, accountIdList, true);

            } else {
                tasks = taskRepository.findByFkTeamIdTeamIdAndFkAccountIdAssignedAccountIdInAndCurrentlyScheduledTaskIndicator(teamId, accountIdList, true);

            }

            for (Task task : tasks) {
                ScheduledTaskViewResponse scheduledTaskViewResponse = new ScheduledTaskViewResponse();

                scheduledTaskViewResponse.setAccountIdAssigned(task.getFkAccountIdAssigned().getAccountId());
                scheduledTaskViewResponse.setTaskNumber(task.getTaskNumber());
                scheduledTaskViewResponse.setTaskIdentifier(task.getTaskIdentifier());
                scheduledTaskViewResponse.setTaskTitle(task.getTaskTitle());
                scheduledTaskViewResponse.setTeamId(task.getFkTeamId().getTeamId());
                scheduledTaskViewResponse.setTaskId(task.getTaskId());
                scheduledTaskViewResponse.setTaskTypeId(task.getTaskTypeId());
                scheduledTaskViewResponse.setTaskDesc(task.getTaskDesc());
                scheduledTaskViewResponse.setCurrentlyScheduledTaskIndicator(task.getCurrentlyScheduledTaskIndicator());
                scheduledTaskViewResponse.setCurrentActivityIndicator(task.getCurrentActivityIndicator());
                scheduledTaskViewResponse.setTaskWorkflowId(task.getTaskWorkflowId());
                scheduledTaskViewResponse.setWorkflowTaskStatus(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus());

                tasksByFilter.add(scheduledTaskViewResponse);
            }
        } else {
            throw new ForbiddenException("All entities are null");

        }

        return tasksByFilter;
    }

    public ScheduledTaskViewResponse setCurrentActivityIndicatorForTask(AllScheduledTaskIdsRequest allScheduledTaskIdsRequest, String timeZone, String accountIds) throws IllegalAccessException {

        Task taskDb = taskRepository.findByTaskId(allScheduledTaskIdsRequest.getTaskId());

        if (taskDb == null) {
            throw new TaskNotFoundException();
        }

        if (!allScheduledTaskIdsRequest.isUserConsent() && allScheduledTaskIdsRequest.getIsCurrentActivityIndicatorOn() == 1) {

            List<Long> accountIdsList = Arrays.stream(accountIds.split(",")).map(Long::valueOf).collect(Collectors.toList());

            int taskCountWithCurrentActivity = taskRepository.getNumberOfTasksWithCurrentActivityIndicatorOn(accountIdsList, 1);
            if (taskCountWithCurrentActivity >= 2) {
                throw new ValidationFailedException("You already have at least 2 current active tasks");
            }
        }

        Task updatedTask = new Task();

        BeanUtils.copyProperties(taskDb, updatedTask);
        updatedTask.setCurrentActivityIndicator(allScheduledTaskIdsRequest.getIsCurrentActivityIndicatorOn());
        if (allScheduledTaskIdsRequest.getIsCurrentActivityIndicatorOn() == 1) {
            updatedTask.setCurrentlyScheduledTaskIndicator(true);
        }

        if (allScheduledTaskIdsRequest.getActualStartDate() != null) {

            LocalDateTime convertedDateTime = DateTimeUtils.convertUserDateToServerTimezone(allScheduledTaskIdsRequest.getActualStartDate(), timeZone);
            updatedTask.setTaskActStDate(convertedDateTime);
            updatedTask.setTaskActStTime(convertedDateTime.toLocalTime());

            WorkFlowTaskStatus workFlowTaskStatus = workFlowTaskStatusRepository.findByWorkflowTaskStatusAndFkWorkFlowTypeWorkflowTypeId("Started", updatedTask.getFkWorkflowTaskStatus().getFkWorkFlowType().getWorkflowTypeId());
            updatedTask.setFkWorkflowTaskStatus(workFlowTaskStatus);
        }

//        UserAccount lastUpdatedByAccount = userAccountRepository.findByAccountId(updatedTask.getFkAccountId().getAccountId());
//      since the API getScheduledTasks shows only tasks that are assigned to this user and have currentlyScheduledIndicator On, we can put the accountId it is assigned to in the FkAccountIdAssigned
        updatedTask.setFkAccountIdLastUpdated(updatedTask.getFkAccountIdAssigned());

        updatedTask = taskServiceImpl.updateFieldsInTaskTable(updatedTask, allScheduledTaskIdsRequest.getTaskId(), timeZone,accountIds);

//        if(allScheduledTaskIdsRequest.getIsCurrentActivityIndicatorOn() == 1){
//            // count all the tasks of the accounts in header for which current activity indicator is ON
//            int tasksWithCurrentActivityIndicator = taskRepository.getNumberOfTasksWithCurrentActivityIndicatorOn(accountIdsList, 1);
//
//            if(tasksWithCurrentActivityIndicator == 2){
//                throw new ValidationFailedException("You cannot turn current activity indicator for more than 2 tasks");
//            }
//
//        }

        // update task and set current activity indicator of the provided task as OFF or ON
        // updatedTask = taskRepository.setCurrentActivityIndicatorByTaskId(allScheduledTaskIdsRequest.getTaskId(), allScheduledTaskIdsRequest.getIsCurrentActivityIndicatorOn());

        ScheduledTaskViewResponse scheduledTaskViewResponse = new ScheduledTaskViewResponse();

        scheduledTaskViewResponse.setAccountIdAssigned(updatedTask.getFkAccountIdAssigned().getAccountId());
        scheduledTaskViewResponse.setTaskNumber(updatedTask.getTaskNumber());
        scheduledTaskViewResponse.setTaskIdentifier(updatedTask.getTaskIdentifier());
        scheduledTaskViewResponse.setTeamId(updatedTask.getFkTeamId().getTeamId());
        scheduledTaskViewResponse.setTaskTitle(updatedTask.getTaskTitle());
        scheduledTaskViewResponse.setTaskId(updatedTask.getTaskId());
        scheduledTaskViewResponse.setTaskDesc(updatedTask.getTaskDesc());
        scheduledTaskViewResponse.setCurrentlyScheduledTaskIndicator(updatedTask.getCurrentlyScheduledTaskIndicator());
        scheduledTaskViewResponse.setCurrentActivityIndicator(updatedTask.getCurrentActivityIndicator());
        scheduledTaskViewResponse.setTaskWorkflowId(updatedTask.getTaskWorkflowId());
        scheduledTaskViewResponse.setWorkflowTaskStatus(updatedTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus());

        return scheduledTaskViewResponse;

    }

    public DuplicateTaskResponse createDuplicateTask(DuplicateTaskRequest duplicateTaskRequest, String timeZone, String accountIds) {
        DuplicateTaskResponse resp = new DuplicateTaskResponse();
        DuplicateTask response = new DuplicateTask();
        Task foundTaskDb = null;
        foundTaskDb = taskServiceImpl.findTaskByTaskNumberAndTeamId(duplicateTaskRequest.getTaskNumber(), duplicateTaskRequest.getTeamId());

        if (foundTaskDb != null) {

            if (!accessDomainService.validateOrgAndTeamInRequest(foundTaskDb.getFkOrgId().getOrgId(), foundTaskDb.getFkTeamId().getTeamId(), accountIds)) {
                throw new ValidationFailedException("Invalid Request: The provided account does not have access to the organization or team associated with the task.");
            }
            // validation: if the task is not assigned to the user then the user must have team task view action
            List<Long> accountIdsList = Arrays.stream(accountIds.split(",")).map(Long::valueOf).collect(Collectors.toList());
            List<AccessDomain> userAccessDomains = accessDomainRepository.findByEntityTypeIdAndEntityIdAndAccountIdInAndIsActive(Constants.EntityTypes.TEAM, foundTaskDb.getFkTeamId().getTeamId(), accountIdsList, true);
            if (!userAccessDomains.isEmpty()) {
                Long accountIdOfUserInTeam = userAccessDomains.get(0).getAccountId();
                Boolean isSelfAssigned = false;
                if (foundTaskDb.getFkAccountIdAssigned() != null && Objects.equals(accountIdOfUserInTeam, foundTaskDb.getFkAccountIdAssigned().getAccountId())) {
                    isSelfAssigned = true;
                }

                if (!isSelfAssigned) {
                    List<Integer> roleIdsOfUserInTeam = new ArrayList<>();
                    for (AccessDomain accessDomain : userAccessDomains) {
                        roleIdsOfUserInTeam.add(accessDomain.getRoleId());
                    }

                    List<Integer> actionIds = roleActionRepository.findActionIdsByRoleIdIn(roleIdsOfUserInTeam);
                    List<String> allActions = actionRepository.findActionNameByActionIdIn(actionIds);
                    if (!allActions.contains(Constants.UpdateTeam.Team_Task_View)) {
                        throw new ValidationFailedException("User doesn't have access to view this task");
                    }
                }
                if (duplicateTaskRequest.getIsParent() != null && duplicateTaskRequest.getIsParent() && duplicateTaskRequest.getCopyChildTasks() != null && duplicateTaskRequest.getCopyChildTasks() && Objects.equals(foundTaskDb.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK)) {
                    String message = "Task was created ";
                    if (Objects.equals(foundTaskDb.getTaskPriority(), Constants.Priorities.PRIORITY_P1) || Objects.equals(foundTaskDb.getTaskPriority(), Constants.Priorities.PRIORITY_P0)) {
                        message = message + "and priority was changed from " + foundTaskDb.getTaskPriority() + " to P2";
                    }
                    Long userId = userAccountRepository.findUserIdByAccountId(accountIdsList.get(0));
                    Map<String, List<String>> parentTaskResponseList = duplicateParentTask(foundTaskDb, duplicateTaskRequest.getCopyParentChildDates(), userId, timeZone, message);
                    resp.setParentTaskNumber(parentTaskResponseList.get("Parent").get(0));
                    message = message + " with Work Item number " + parentTaskResponseList.get("Parent").get(0) + " and sub task numbers : " + parentTaskResponseList.get("Child");
                    resp.setMessage(message);
                    return resp;
                }
                response.setTaskTitle(foundTaskDb.getTaskTitle());
                response.setTaskDesc(foundTaskDb.getTaskDesc());
                response.setTaskEstimate(foundTaskDb.getTaskEstimate());
                response.setTaskPriority(foundTaskDb.getTaskPriority());
                response.setTaskWorkflowId(foundTaskDb.getTaskWorkflowId());
                response.setAcceptanceCriteria(foundTaskDb.getAcceptanceCriteria());
                response.setOrgId(foundTaskDb.getFkOrgId().getOrgId());
                response.setTeamId(foundTaskDb.getFkTeamId().getTeamId());
                response.setTaskExpStartDate(DateTimeUtils.convertServerDateToUserTimezone(foundTaskDb.getTaskExpStartDate(), timeZone));
                response.setTaskExpStartTime(DateTimeUtils.convertServerTimeToUserTimeZone(foundTaskDb.getTaskExpStartTime(), timeZone));
                response.setTaskExpEndDate(DateTimeUtils.convertServerDateToUserTimezone(foundTaskDb.getTaskExpEndDate(), timeZone));
                response.setTaskExpEndTime(DateTimeUtils.convertServerTimeToUserTimeZone(foundTaskDb.getTaskExpEndTime(), timeZone));
                response.setProjectId(foundTaskDb.getFkProjectId().getProjectId());
                response.setTaskTypeId(foundTaskDb.getTaskTypeId());
                setDependentTaskDetailsResponse(foundTaskDb);
                if (foundTaskDb.getDependentTaskDetailResponseList() != null) {
                    List<DependentTaskDetail> dependentTaskDetails = new ArrayList<>();
                    for (DependentTaskDetailResponse dependentTaskDetailResponse : foundTaskDb.getDependentTaskDetailResponseList()) {
                        DependentTaskDetail dependentTaskDetail = new DependentTaskDetail();
                        dependentTaskDetail.setRelatedTaskNumber(dependentTaskDetailResponse.getTaskNumber());
                        dependentTaskDetail.setRelationDirection(dependentTaskDetailResponse.getRelationDirection());
                        dependentTaskDetail.setRelationTypeId(dependentTaskDetailResponse.getRelationTypeId());
                        dependentTaskDetails.add(dependentTaskDetail);
                    }
                    response.setDependentTaskDetailRequestList(dependentTaskDetails);
                }
                if (duplicateTaskRequest.getIsChild() != null && duplicateTaskRequest.getIsChild()) {
                    Task parentTaskDb = new Task();
                    if (duplicateTaskRequest.getParentTaskNumber() != null) {
                        if (Objects.equals(duplicateTaskRequest.getTaskNumber(), duplicateTaskRequest.getParentTaskNumber())) {
                            throw new ValidationFailedException("Invalid Request: The provided Work Item cannot be duplicated as a child task since the task number and parent task number must be different.");
                        }
                        parentTaskDb = taskServiceImpl.findTaskByTaskNumberAndTeamId(duplicateTaskRequest.getParentTaskNumber(), duplicateTaskRequest.getTeamId());
                    } else {
                        parentTaskDb = taskRepository.findByTaskId(foundTaskDb.getParentTaskId());
                    }
                    if (parentTaskDb != null) {
                        if (Objects.equals(parentTaskDb.getTaskTypeId(), Constants.TaskTypes.PARENT_TASK)) {
                            if (Objects.equals(parentTaskDb.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG_TITLE_CASE) || Objects.equals(parentTaskDb.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED_TITLE_CASE) || Objects.equals(parentTaskDb.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_STARTED_TITLE_CASE)) {
                                response.setParentTaskNumber(parentTaskDb.getTaskNumber());
                                if (Objects.equals(parentTaskDb.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG_TITLE_CASE)) {
                                    response.setWorkflowTaskStatus(Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG_TITLE_CASE);
                                } else {
                                    response.setWorkflowTaskStatus(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED_TITLE_CASE);
                                }
                            } else {
                                throw new IllegalArgumentException("To create a subtask, parent task must be in backlog, not started or started state. Work Item number " + parentTaskDb.getTaskNumber() + " is in " + parentTaskDb.getFkWorkflowTaskStatus().getWorkflowTaskStatus() + " state");
                            }
                        } else {
                            throw new ValidationFailedException("Invalid Request: The specified parent task (Task number - " + parentTaskDb.getTaskNumber() + ") is not of type 'Parent Task'.");
                        }
                    }
                } else {
                    response.setWorkflowTaskStatus(foundTaskDb.getFkWorkflowTaskStatus().getWorkflowTaskStatus());
                }
                if (foundTaskDb.getIsBug() != null && foundTaskDb.getIsBug()) {
                    response.setEnvironmentId(foundTaskDb.getEnvironmentId());
                    response.setPlaceOfIdentification(foundTaskDb.getPlaceOfIdentification());
                    response.setSeverityId(foundTaskDb.getSeverityId());
                    response.setCustomerImpact(foundTaskDb.getCustomerImpact());
                    List<Task> linkedTasks = new ArrayList<>();
                    if (foundTaskDb.getBugTaskRelation() != null && !foundTaskDb.getBugTaskRelation().isEmpty()) {
                        linkedTasks = taskRepository.findByTaskIdIn(foundTaskDb.getBugTaskRelation());
                    }
                    response.setLinkedTaskList(taskServiceImpl.createLinkedTaskResponse(linkedTasks, timeZone));
                }
            }

        } else {
            throw new TaskNotFoundException();
        }
        resp.setDuplicateTask(response);
        return resp;
    }

    /** fetch task as per the TaskNumberRequest */
    public Task getTaskByTaskNumberRequest(TaskNumberRequest request) {
        Task foundTaskDb = null;
        if ((request.getOrgId() == null && request.getTeamId() == null && request.getProjectId() == null) ||
                (request.getTaskIdentifier() != null && request.getTeamId() == null) || (request.getTaskIdentifier() == null && request.getTaskNumber() == null)) {
            throw new IllegalArgumentException("Invalid Request. Please check again");
        }

        if (request.getTaskIdentifier() != null) {
            foundTaskDb = taskRepository.findByTaskIdentifierAndFkTeamIdTeamId(request.getTaskIdentifier(), request.getTeamId());
        } else {
            if (request.getTeamId() != null) {
                int hyphenIndex = request.getTaskNumber().indexOf('-');
                String extractedNumber = request.getTaskNumber().substring(hyphenIndex + 1);
                foundTaskDb = taskRepository.findByTaskIdentifierAndFkTeamIdTeamId(Long.valueOf(extractedNumber), request.getTeamId());
            } else if (request.getOrgId() != null) {
                foundTaskDb = taskRepository.findByFkTeamIdFkOrgIdOrgIdAndTaskNumber(request.getOrgId(), request.getTaskNumber());
            } else if (request.getProjectId() != null) {
                foundTaskDb = taskRepository.findByFkProjectIdProjectIdAndTaskNumber(request.getProjectId(), request.getTaskNumber());
            }
        }

        return foundTaskDb;
    }

    //    this method is used to get the task with all the meetings linked to that task by its task number
    public TaskResponse getTaskWithMeetingsByTaskNumber(TaskNumberRequest request, String timeZone, String accountIds) throws IllegalAccessException {
        TaskResponse responseTask = new TaskResponse();
        responseTask.setTask(new Task());
        Task foundTaskDb = null;

        foundTaskDb = getTaskByTaskNumberRequest(request);

        if (foundTaskDb != null) {
            BeanUtils.copyProperties(foundTaskDb, responseTask.getTask());
            List<TaskAttachmentMetadata> taskAttachmentMetadataList = taskAttachmentRepository.findTaskAttachmentMetadataByTaskId(responseTask.getTask().getTaskId());

            Map<Long, List<TaskAttachmentMetadata>> taskAttachmentMetadataMap = taskAttachmentMetadataList.stream()
                    .collect(Collectors.groupingBy(TaskAttachmentMetadata::getCommentLogId));

            responseTask.getTask().setComments(Collections.emptyList());

            List<Task> linkedTasks = new ArrayList<>();
            switch (responseTask.getTask().getTaskTypeId()) {
                case Constants.TaskTypes.PARENT_TASK:
                case Constants.TaskTypes.BUG_TASK:
                    List<Long> allChildTasksIncludingDeleted = new ArrayList<>();
                    if (responseTask.getTask().getChildTaskIds() != null) {
                        allChildTasksIncludingDeleted.addAll(responseTask.getTask().getChildTaskIds());
                    }
                    if (responseTask.getTask().getDeletedChildTaskIds() != null) {
                        allChildTasksIncludingDeleted.addAll(responseTask.getTask().getDeletedChildTaskIds());
                    }

                    if (allChildTasksIncludingDeleted.isEmpty()) {
                        allChildTasksIncludingDeleted.addAll(taskRepository.findTaskIdByParentTaskId(responseTask.getTask().getTaskId()));
                    }
                    List<Task> childTasks = taskRepository.findByTaskIdIn(allChildTasksIncludingDeleted);
                    if (responseTask.getTask().getBugTaskRelation() != null && !responseTask.getTask().getBugTaskRelation().isEmpty()) {
                        linkedTasks = taskRepository.findByTaskIdIn(responseTask.getTask().getBugTaskRelation());
                    }
                    responseTask.getTask().setChildTaskList(taskServiceImpl.createChildTaskResponse(childTasks, timeZone));
                    responseTask.getTask().setLinkedTaskList(taskServiceImpl.createLinkedTaskResponse(linkedTasks, timeZone));
                    break;

                case Constants.TaskTypes.CHILD_TASK:
                    Task parentTaskDb = taskRepository.findByTaskId(responseTask.getTask().getParentTaskId());
                    ParentTaskResponse parentTaskResponse = new ParentTaskResponse();
                    BeanUtils.copyProperties(parentTaskDb, parentTaskResponse);
                    // convert the dates to local time zone
                    parentTaskResponse.setTaskExpEndDate(DateTimeUtils.convertServerDateToUserTimezone(parentTaskDb.getTaskExpEndDate(), timeZone));
                    parentTaskResponse.setTaskExpStartDate(DateTimeUtils.convertServerDateToUserTimezone(parentTaskDb.getTaskExpStartDate(), timeZone));
                    // accountId assigned can be null in case the parent task is in backlog
                    parentTaskResponse.setAccountIdAssigned(parentTaskDb.getFkAccountIdAssigned() != null ? parentTaskDb.getFkAccountIdAssigned().getAccountId() : null);
                    parentTaskResponse.setWorkflowTaskStatus(parentTaskDb.getFkWorkflowTaskStatus().getWorkflowTaskStatus());
                    responseTask.getTask().setParentTaskResponse(parentTaskResponse);
                    if (responseTask.getTask().getBugTaskRelation() != null && !responseTask.getTask().getBugTaskRelation().isEmpty()) {
                        linkedTasks = taskRepository.findByTaskIdIn(responseTask.getTask().getBugTaskRelation());
                    }
                    responseTask.getTask().setLinkedTaskList(taskServiceImpl.createLinkedTaskResponse(linkedTasks, timeZone));
                    break;

                case Constants.TaskTypes.TASK:
                    if (responseTask.getTask().getBugTaskRelation() != null && !responseTask.getTask().getBugTaskRelation().isEmpty()) {
                        linkedTasks = taskRepository.findByTaskIdIn(responseTask.getTask().getBugTaskRelation());
                    }
                    responseTask.getTask().setLinkedTaskList(taskServiceImpl.createLinkedTaskResponse(linkedTasks, timeZone));
                    break;
            }

            setDependentTaskDetailsResponse(responseTask.getTask());
            setReferencedTaskDetailsResponse(responseTask.getTask());
            setRcaMemberAccountIdListResponse(responseTask.getTask());
            taskServiceImpl.convertTaskAllServerDateAndTimeInToUserTimeZone(responseTask.getTask(), timeZone);
            if (responseTask.getTask().getNoteId() != null && responseTask.getTask().getNotes() != null) {
                responseTask.getTask().setNotes(noteService.removeDeletedNotes(responseTask.getTask().getNotes()));
            }

            if (responseTask.getTask().getListOfDeliverablesDeliveredId() != null && responseTask.getTask().getListOfDeliverablesDelivered() != null) {
                responseTask.getTask().setListOfDeliverablesDelivered(deliverablesDeliveredService.removeDeletedlistOfDeliverablesDelivered(responseTask.getTask().getListOfDeliverablesDelivered()));
            }
            List<Meeting> meetingList = meetingRepository.findActiveReferenceMeetingByReferenceEntityTypeIdAndReferenceEntityNumberAndTeamId(Constants.EntityTypes.TASK, foundTaskDb.getTaskNumber(), foundTaskDb.getFkTeamId().getTeamId());
            List<MeetingResponse> responseMeetingList = new ArrayList<>();
            for (Meeting meeting : meetingList) {
                meeting.setReferenceEntityNumber(meeting.getReferenceEntityNumber().toUpperCase());
                if (meeting.getStartDateTime() != null) {
                    MeetingResponse meetingResponse = new MeetingResponse();

                    meetingService.copySimilarFields(meeting, meetingResponse);
                    meetingResponse.setStartDateTime(convertServerDateToUserTimezone(meeting.getStartDateTime(), timeZone));

                    if (meeting.getAttendeeList() != null && !meeting.getAttendeeList().isEmpty()) {
                        if (meetingResponse.getReferenceEntityNumber() != null && Objects.equals(meetingResponse.getReferenceEntityTypeId(), com.tse.core_application.model.Constants.EntityTypes.TASK)) {
                            if (meetingService.validateAttendeeIdAndAssignedToId(meetingResponse.getReferenceEntityNumber(), meetingResponse.getTeamId(), accountIds)) {
                                meetingResponse.setShowUserPerceivedPercentage(Boolean.TRUE);
//                                Task referenceTask = taskRepository.findByTaskNumber(meetingResponse.getReferenceEntityNumber());
                                Task referenceTask = taskServiceImpl.findTaskByTaskNumberAndTeamId(meetingResponse.getReferenceEntityNumber(), meetingResponse.getTeamId());
                                if (referenceTask.getUserPerceivedPercentageTaskCompleted() != null) {
                                    meetingResponse.setReferenceTaskUserPerceivedPercentage(referenceTask.getUserPerceivedPercentageTaskCompleted());
                                }
                            }
                        }

                        //adding only invited attendees in condensed view list
                        List<Attendee> attendeeList = new ArrayList<>();
                        for (Attendee attendee : meeting.getAttendeeList()) {
                            if (Objects.equals(attendee.getAttendeeInvitationStatusId(), com.tse.core_application.constants.Constants.MeetingAttendeeInvitationStatus.ATTENDEE_INVITED_ID)) {
                                attendeeList.add(attendee);
                                if (meetingService.validateIsEditableForAttendee(attendee, accountIds)) {
                                    meetingResponse.setIsEditable(Boolean.TRUE);
                                }
                            }
                        }
                        meetingResponse.setAttendeeRequestList(attendeeList);

                    }

                    meetingResponse.setMeetingType(meetingService.setMeetingType(meeting.getMeetingTypeIndicator()));

                    if (meeting.getTeamId() != null) {
                        String teamName = teamRepository.findTeamNameByTeamId(meeting.getTeamId());
                        meetingResponse.setEntityName(teamName);
                    }

                    if (!meetingService.meetingEditAccess (meeting.getOrganizerAccountId(), meeting.getCreatedAccountId(), meeting.getTeamId(), accountIds)) {
                        meetingResponse.setCanEditMeeting(false);
                    }

                    responseMeetingList.add(meetingResponse);
                }
                responseTask.getTask().setMeetingEffortPreferenceId(meetingService.getMeetingPreferenceId(responseTask.getTask()));
                responseTask.setMeetingList(responseMeetingList);
            }
        } else {
            throw new TaskNotFoundException();
        }

        if (Objects.equals(responseTask.getTask().getFkTeamId().getTeamName(), com.tse.core_application.model.Constants.PERSONAL_ORG_DEFAULT_TEAM_NAME) && Objects.equals(responseTask.getTask().getFkOrgId().getOrgId().intValue(), Constants.OrgIds.PERSONAL)) {
            if (teamService.getAllTeamsForCreateTask(responseTask.getTask().getFkAccountIdAssigned().getEmail(), responseTask.getTask().getFkOrgId().getOrgId()).size() > 1) {
                responseTask.getTask().getFkTeamId().setTeamName(com.tse.core_application.model.Constants.PERSONAL_ORG_TEAM_DISPLAY_NAME_FOR_MULTIPLE_TEAMS);
            } else {
                responseTask.getTask().getFkTeamId().setTeamName(com.tse.core_application.model.Constants.PERSONAL_ORG_TEAM_DISPLAY_NAME);
            }
        }
        return responseTask;
    }

    public List<TaskMaster> getTasksWithPagination(List<TaskMaster> allTasks, Integer pageNumber, Integer pageSize) {
        Integer startIndex = pageNumber * pageSize;
        Integer endIndex = Math.min(startIndex + pageSize, allTasks.size());

        if (startIndex > endIndex) {
            // Page is out of bounds
            return Collections.emptyList();
        }

        return allTasks.subList(startIndex, endIndex);
    }

    public TaskByFilterResponse getTaskByFilterResponse(StatsRequest statsRequest, List<TaskMaster> taskMasterList, Integer pageNumber, Integer pageSize, Integer taskListSize) {
        TaskByFilterResponse taskByFilterResponse = new TaskByFilterResponse();
        taskByFilterResponse.setTaskMasterList(taskMasterList);
        taskByFilterResponse.setPageNumber(pageNumber);
        taskByFilterResponse.setPageSize(pageSize);
        taskByFilterResponse.setHasPagination(statsRequest.getHasPagination());
        taskByFilterResponse.setTotalTasks(taskListSize);
        taskByFilterResponse.setFromDateType(statsRequest.getFromDateType());
        taskByFilterResponse.setFromDate(statsRequest.getFromDate());
        taskByFilterResponse.setToDate(statsRequest.getToDate());
        taskByFilterResponse.setToDateType(statsRequest.getToDateType());

        return taskByFilterResponse;
    }

    public Map<String, List<String>> duplicateParentTask(Task parentTask, Boolean copyDates, Long userId, String timeZone, String message) {
        Task duplicateParentTask = createAddTaskRequest(parentTask, copyDates, userId);
        Task taskUpdated = initializeTaskNumberSetProperties(duplicateParentTask);
        if (Objects.equals(taskUpdated.getTaskPriority(), Constants.Priorities.PRIORITY_P1) || Objects.equals(taskUpdated.getTaskPriority(), Constants.Priorities.PRIORITY_P0)) {
            taskUpdated.setTaskPriority(Constants.Priorities.PRIORITY_P2);
        }
        Task taskAdd = taskServiceImpl.addTaskInTaskTable(taskUpdated, timeZone);
        Map<String, List<String>> parentTaskCopyResponse = new HashMap<>();
        parentTaskCopyResponse.put("Parent", Collections.singletonList(taskAdd.getTaskNumber()));
        if (parentTask.getChildTaskIds() != null && !parentTask.getChildTaskIds().isEmpty()) {
            for (Long childTaskId : parentTask.getChildTaskIds()) {
                Task childTask = createAddTaskRequest(taskRepository.findByTaskId(childTaskId), copyDates, userId);
                childTask.setParentTaskId(taskAdd.getTaskId());
                childTask.setParentTaskTypeId(taskAdd.getTaskTypeId());
                Task childTaskUpdated = initializeTaskNumberSetProperties(childTask);
                if (Objects.equals(childTaskUpdated.getTaskPriority(), Constants.Priorities.PRIORITY_P1) || Objects.equals(childTaskUpdated.getTaskPriority(), Constants.Priorities.PRIORITY_P0)) {
                    childTaskUpdated.setTaskPriority(Constants.Priorities.PRIORITY_P2);
                    if (Objects.equals(message, "Task was created ")) {
                        message = message + "and priority was changed from " + childTask.getTaskPriority() + " to P2";
                    }
                }
                Task childTaskAdd = taskServiceImpl.addTaskInTaskTable(childTaskUpdated, timeZone);
                if (!parentTaskCopyResponse.containsKey("Child")) {
                    parentTaskCopyResponse.put("Child", new ArrayList<>(List.of(childTaskAdd.getTaskNumber())));
                } else {
                    parentTaskCopyResponse.get("Child").add(childTaskAdd.getTaskNumber());
                }
            }
        }
        return parentTaskCopyResponse;
    }

    public Task createAddTaskRequest(Task taskToCopy, Boolean copyDates, Long userId) {
        Task task = new Task();
        task.setFkWorkflowTaskStatus(workFlowTaskStatusRepository.findByWorkflowTaskStatusAndFkWorkFlowTypeWorkflowTypeId(Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG_TITLE_CASE, taskToCopy.getTaskWorkflowId()));
        if (copyDates) {
            task.setTaskExpStartTime(taskToCopy.getTaskExpStartTime());
            task.setTaskExpEndTime(taskToCopy.getTaskExpEndTime());
            task.setTaskExpStartDate(taskToCopy.getTaskExpStartDate());
            task.setTaskExpEndDate(taskToCopy.getTaskExpEndDate());
        }
        task.setTaskEstimate(taskToCopy.getTaskEstimate());
        task.setIsBallparkEstimate(Constants.BooleanValues.BOOLEAN_TRUE);
        task.setIsEstimateSystemGenerated(Constants.BooleanValues.BOOLEAN_TRUE);
        task.setTaskPriority(taskToCopy.getTaskPriority());
        task.setTaskTypeId(taskToCopy.getTaskTypeId());
        task.setTaskDesc(taskToCopy.getTaskDesc());
        task.setTaskTitle(taskToCopy.getTaskTitle());
        task.setAcceptanceCriteria(taskToCopy.getAcceptanceCriteria());
        task.setFkOrgId(taskToCopy.getFkOrgId());
        task.setFkTeamId(taskToCopy.getFkTeamId());
        task.setFkProjectId(taskToCopy.getFkProjectId());
        UserAccount creatorAccountId = userAccountRepository.findByOrgIdAndFkUserIdUserIdAndIsActive(taskToCopy.getFkOrgId().getOrgId(), userId, true);
        task.setFkAccountId(creatorAccountId);
        task.setFkAccountIdCreator(creatorAccountId);
        task.setFkAccountIdLastUpdated(creatorAccountId);
        task.setBuId(taskToCopy.getBuId());
        task.setTaskWorkflowId(taskToCopy.getTaskWorkflowId());
        task.setFkAccountIdAssigned(taskToCopy.getFkAccountIdAssigned());
        task.setFkAccountIdAssignee(creatorAccountId);
        task.setCurrentActivityIndicator(Constants.BooleanValues.BOOLEAN_FALSE);
        return task;
    }

    public List<TaskMaster> filterBySearch(List<TaskMaster> allTasks, List<String> searches) {
        List<TaskMaster> filteredTaskList = new ArrayList<>();
        for (TaskMaster task : allTasks) {
            String title = task.getTaskTitle().toLowerCase();
            for (String word : searches) {
                if (title.contains(word.toLowerCase())) {
                    filteredTaskList.add(task);
                    break;
                }
            }
        }
        return filteredTaskList;
    }

    public void sortTaskListBySortFilters (List<TaskMaster> taskMasters, HashMap<Integer, SortingField> sortingFields) {
        //getting a list of field names by sorting them according to priority
        List<Map.Entry<Integer, SortingField>> sortedEntries = sortingFields.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toList());
        Comparator<TaskMaster> taskComparator = Comparator.comparing(task -> 0); // Initial comparator
        HashMap<Integer,SortingField> newSortingFields = new HashMap<>();
        Integer i = 1;
        for (Map.Entry<Integer, SortingField> entry : sortedEntries) {
            SortingField sortingField = entry.getValue();
            if(sortingField.getFieldName().equals("taskNumber")) {
                SortingField newSortingField = new SortingField();
                newSortingField.setFieldName("taskNumberAlphabetic");
                newSortingField.setAscending(sortingField.isAscending());
                newSortingFields.put(i, newSortingField);
                i++;
                newSortingField.setFieldName("taskNumberNumeric");
                newSortingField.setAscending(sortingField.isAscending());
                newSortingFields.put(i, newSortingField);
                i++;
            }
            else {
                newSortingFields.put(i, sortingField);
                i++;
            }
            if(sortingField.getFieldName().equals("taskActStDate") || sortingField.getFieldName().equals("taskActEndDate")) {
                SortingField newSortingField = new SortingField();
                if(sortingField.getFieldName().equals("taskActStDate"))
                    newSortingField.setFieldName("taskExpStartDate");
                else
                    newSortingField.setFieldName("taskExpEndDate");
                newSortingField.setAscending(sortingField.isAscending());
                newSortingFields.put(i, newSortingField);
                i++;
            }
        }
        List<Map.Entry<Integer, SortingField>> newSortedEntries = newSortingFields.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toList());


        //checking for Comparator and adding fields on the basis of priority
        for (Map.Entry<Integer, SortingField> entry : newSortedEntries) {
            SortingField sortingField = entry.getValue();
            Comparator<TaskMaster> fieldComparator;

            // Define custom sorting logic for specific fields
            switch (sortingField.getFieldName()) {
                case "taskPriority":
                    fieldComparator = Comparator.comparing(task -> getPriorityValue(task.getTaskPriority()), Comparator.nullsLast(Comparator.naturalOrder()));
                    break;
                case "workflowTaskStatusType":
                    fieldComparator = Comparator.comparing(task -> getWorkflowStatusValue(task.getWorkflowTaskStatusType()), Comparator.nullsLast(Comparator.naturalOrder()));
                    break;
                case "taskNumberAlphabetic":
                    fieldComparator = Comparator.comparing(task -> task.getTeamCode(), Comparator.nullsLast(Comparator.naturalOrder()));
                    break;
                case "taskNumberNumeric":
                    fieldComparator = Comparator.comparing(task -> task.getTaskIdentifier(), Comparator.nullsLast(Comparator.naturalOrder()));
                    break;
                case "taskExpStartDate":
                case "taskExpEndDate":
                case "taskActStDate":
                case "taskActEndDate":
                    fieldComparator = Comparator.comparing(task -> {
                        try {
                            Field field = TaskMaster.class.getDeclaredField(sortingField.getFieldName());
                            field.setAccessible(true);
                            return (Comparable) field.get(task);
                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            e.printStackTrace();
                            return null;
                        }
                    }, Comparator.nullsLast(Comparator.naturalOrder()));
                    break;
                default:
                    throw new IllegalArgumentException("Invalid sorting field: " + sortingField.getFieldName());
            }

            if (!sortingField.isAscending()) {
                fieldComparator = fieldComparator.reversed();
            }

            taskComparator = taskComparator.thenComparing(fieldComparator);
        }

        taskMasters.sort(taskComparator);
    }

    public void setRcaMemberAccountIdListResponse (Task task) {
        List<AccountDetailsForBulkResponse> rcaMemberAccountIdList = new ArrayList<>();
        if (task.getRcaIntroducedBy() != null && !task.getRcaIntroducedBy().isEmpty()) {
            for (Long accountId : task.getRcaIntroducedBy()) {
                UserAccount userAccount = userAccountRepository.findByAccountId(accountId);
                if (userAccount != null) {
                    rcaMemberAccountIdList.add(new AccountDetailsForBulkResponse(userAccount.getEmail(), userAccount.getAccountId(), userAccount.getFkUserId().getFirstName(), userAccount.getFkUserId().getLastName(),null));
                }
            }
            task.setRcaMemberAccountIdList(rcaMemberAccountIdList);
        }
    }

    public List<Task> sortSearchTask(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return tasks;
        }
        tasks.sort(Comparator
                .comparing((Task task) -> {
                    if (task.getFkWorkflowTaskStatus().getWorkflowTaskStatus() != null) {
                        String status = task.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase();
                        switch (status) {
                            case Constants.WorkFlowTaskStatusConstants.STATUS_STARTED:
                            case Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED:
                            case Constants.WorkFlowTaskStatusConstants.STATUS_ON_HOLD:
                            case Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED:
                                return 1;
                            case Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED:
                                return 2;
                            case Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG:
                                return 3;
                            case Constants.WorkFlowTaskStatusConstants.STATUS_DELETE:
                                return 4;
                        }
                    }
                    return 5;
                })
                .thenComparing((Task task) -> {
                    if (task.getTaskPriority() != null) {
                        switch (task.getTaskPriority()) {
                            case "P0":
                                return 1;
                            case "P1":
                                return 2;
                            case "P2":
                                return 3;
                            case "P3":
                                return 4;
                            case "P4":
                                return 5;
                            case "P5":
                                return 6;
                        }
                    }
                    return 7;
                })
                .thenComparing(Task::getTaskExpEndDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Task::getLastUpdatedDateTime, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Task::getCreatedDateTime)
        );
        return tasks;
    }

}

