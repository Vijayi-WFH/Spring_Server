package com.tse.core_application.service.Impl;

import com.tse.core_application.dto.EmailFirstLastAccountIdIsActive;
import com.tse.core_application.dto.board_view.UpdateBoardRequest;
import com.tse.core_application.dto.board_view.ViewBoardRequest;
import com.tse.core_application.dto.board_view.BoardResponse;
import com.tse.core_application.exception.BoardViewErrorException;
import com.tse.core_application.exception.ForbiddenException;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.model.*;
import com.tse.core_application.repository.*;
import com.tse.core_application.specification.BoardViewSpecification;
import com.tse.core_application.utils.DateTimeUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class BoardService {

    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private UserAccountService userAccountService;
    @Autowired
    private TimeSheetRepository timeSheetRepository;
    @Autowired
    private CalendarDaysRepository calendarDaysRepository;
    @Autowired
    private TaskServiceImpl taskServiceImpl;
    @Autowired
    private WorkflowTaskStatusService workflowTaskStatusService;
    @Autowired
    private ActionService actionService;
    @Autowired
    private AccessDomainRepository accessDomainRepository;
    @Autowired
    private TableColumnsTypeService tableColumnsTypeService;
    @Autowired
    private SprintRepository sprintRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    /**
     *
     *     This method returns a list of tasks that have been completed since the previous working day and the tasks that are currently scheduled and all other tasks assigned to the user/ member in team (that are not in backlog)
     *     The list is returned to the respective order as mentioned above and within the order, it's sorted based on priority.
     */
//
    public List<BoardResponse> getBoardTasks(ViewBoardRequest viewBoardRequest, Long accountIdRequestor, String timeZone) {
        //here also we are using the static table
        LocalDate previousDate = calendarDaysRepository.findPreviousBusinessDayByDate(LocalDate.now()); // previous date based on the current date

        List<String> invalidWorkFlowStatuses = List.of(Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG_TITLE_CASE,
                Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE,
                Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE);

        Specification<Task> spec = BoardViewSpecification.build(viewBoardRequest, previousDate, invalidWorkFlowStatuses);
        List<Task> tasks = taskRepository.findAll(spec);

        List<Long> teamTaskViewTeamIds = accessDomainRepository.findTeamIdsByAccountIdsAndActionId(List.of(accountIdRequestor), Constants.EntityTypes.TEAM, Constants.ActionId.TEAM_TASK_VIEW);
        // Remove tasks that do not satisfy the team task view condition
        tasks.removeIf(task -> !teamTaskViewTeamIds.contains(task.getFkTeamId().getTeamId()) &&
                (task.getFkAccountIdAssigned() == null || !accountIdRequestor.equals(task.getFkAccountIdAssigned().getAccountId())));

        return createBoardResponsesFromTaskList(tasks, previousDate, timeZone, viewBoardRequest.getSortingPriorityList());
    }

    /**
     *
     * @param boardTasks
     * @param previousDate
     * This method creates a List of BoardResponse from the given List of tasks
     */
    List<BoardResponse> createBoardResponsesFromTaskList(List<Task> boardTasks, LocalDate previousDate, String localTimeZone, HashMap<Integer, SortingField> sortingPriorityList){
        LocalDate currentDate = LocalDate.now();
        List<BoardResponse> boardResponses = new ArrayList<>();

        for(Task boardTask: boardTasks){

            BoardResponse boardResponse = new BoardResponse();
            BeanUtils.copyProperties(boardTask, boardResponse);
            boardResponse.setTaskId(boardTask.getTaskId());

            Integer[] previousDayEffortArray = calculateDayEffort(boardTask.getTaskId(), previousDate);
            boardResponse.setPreviousDayEffort(previousDayEffortArray[0]);
            boardResponse.setPreviousDayEffortDate(previousDate);
            boardResponse.setPreviousDayIncreaseInUserPerceivedPercentageTaskCompleted(previousDayEffortArray[1]);

            Integer[] currentDayEffortArray = calculateDayEffort(boardTask.getTaskId(), currentDate);
            boardResponse.setCurrentDayNewRecordedEffort(currentDayEffortArray[0]);
            boardResponse.setCurrentDayNewRecordedEffortDate(currentDate);
            boardResponse.setNewIncreaseInUserPerceivedPercentageTaskCompleted(currentDayEffortArray[1]);

            boardResponse.setWorkflowTaskStatus(boardTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus());
            boardResponse.setAccountIdAssigned(boardTask.getFkAccountIdAssigned().getAccountId());
            boardResponse.setTeamId(boardTask.getFkTeamId().getTeamId());
            boardResponse.setTeamName(boardTask.getFkTeamId().getTeamName());
            boardResponse.setNewEffortTracks(Collections.emptyList());
            boardResponse.setRecordedEffort(boardTask.getRecordedEffort() == null ? 0 : boardTask.getRecordedEffort());
            if(boardTask.getTaskActStDate()!=null){
                boardResponse.setTaskActStDate(DateTimeUtils.convertServerDateToUserTimezone(boardTask.getTaskActStDate(), localTimeZone));
            }
            if(boardTask.getTaskExpEndDate()!=null){
                boardResponse.setTaskExpEndDate(DateTimeUtils.convertServerDateToUserTimezone(boardTask.getTaskExpEndDate(), localTimeZone));
            }
            if(boardTask.getTaskActEndDate()!=null){
                boardResponse.setTaskActEndDate(DateTimeUtils.convertServerDateToUserTimezone(boardTask.getTaskActEndDate(), localTimeZone));
            }
            boardResponse.setIsBug(boardTask.getIsBug());
            //need to make changes here
            if(Objects.equals(boardTask.getIsStarred(),true)) {
                UserAccount user=boardTask.getFkAccountIdStarredBy();
                if(user != null && user.getAccountId() != null)
                {
                    EmailFirstLastAccountIdIsActive starredByUser=userAccountRepository.getEmailFirstNameLastNameAccountIdIsActiveByAccountId(user.getAccountId());
                    boardResponse.setStarredBy(starredByUser);
                }
            }
            boardResponse.setIsStarred(boardTask.getIsStarred());
            boardResponses.add(boardResponse);
        }

        if (sortingPriorityList != null && !sortingPriorityList.isEmpty()) {
            sortBoardTaskListBySortFilters(boardResponses, sortingPriorityList);
        }

        return boardResponses;
    }

    /**
     *
     * This method received a list of task that have been modified at the board view level and updates those task in the task table and the timesheet table
     */
    public List<BoardResponse> updateBoardTasks(UpdateBoardRequest updateBoardRequest, Long accountIdRequester, String timeZone,String headerAccountIds) throws IllegalAccessException {
        // Getting a list of boardResponses (only for the tasks that have been updated)

        if (updateBoardRequest.getBoardResponseList() == null) {
            updateBoardRequest.setBoardResponseList(Collections.emptyList());
        }

        List<String> basicFields = tableColumnsTypeService.getDbBasicFields();
        List<String> essentialFields = tableColumnsTypeService.getDbEssentialFields();

        for (BoardResponse boardResponse : updateBoardRequest.getBoardResponseList()) {

            Task taskDb = taskServiceImpl.findTaskByTaskId(boardResponse.getTaskId());
            if (boardResponse.getUserPerceivedPercentageTaskCompleted() != null && boardResponse.getUserPerceivedPercentageTaskCompleted() == 0) {
                boardResponse.setUserPerceivedPercentageTaskCompleted(null);
            }
            List<String> updatedFields = getUpdatedFields(taskDb, boardResponse);
            updatedFields.removeAll(Arrays.asList("commentId", "fkTeamId", "fkOrgId", "fkProjectId", "fkAccountIdCreator",
                    "fkAccountIdAssignee", "fkAccountIdLastUpdated", "fkAccountId", "newEffortTracks", "attachments",
                    "systemDerivedEndTs", "childTaskIds", "childTaskList", "parentTaskResponse", "linkedTaskList",
                    "bugTaskRelation", "dependentTaskDetailResponseList", "labels", "dependencyIds", "estimateTimeLogEvaluation",
                    "taskCompletionImpact", "isSprintChanged"
            ));

            isUpdateAuthorized(taskDb, accountIdRequester, updatedFields, basicFields, essentialFields);

            Task taskToUpdate = new Task();
            BeanUtils.copyProperties(taskDb, taskToUpdate);
            String taskPreviousWorkflowStatus = taskDb.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase();
            taskServiceImpl.updateCurrentlyScheduledTaskIndicatorForTask(taskToUpdate);
            // validate conditions for workflow status
            validateWorkFlowTaskStatus(boardResponse, taskPreviousWorkflowStatus);

            Sprint sprint = null;
            if (taskDb.getSprintId() != null) {
                sprint = sprintRepository.findBySprintId(taskDb.getSprintId());
            }
            if (boardResponse.getWorkflowTaskStatus() != null && !boardResponse.getWorkflowTaskStatus().equalsIgnoreCase(taskPreviousWorkflowStatus) && sprint != null && Objects.equals(sprint.getSprintStatus(), Constants.SprintStatusEnum.NOT_STARTED.getSprintStatusId())) {
                throw new ValidationFailedException("Work flow status of Work Item can't be changed in Not-Started sprint");
            }
            if (boardResponse.getNewEffortTracks() != null && !boardResponse.getNewEffortTracks().isEmpty()) {

                // validate User Perceived Percentage Task Completed is greater than previous value for that task
                if (taskToUpdate.getUserPerceivedPercentageTaskCompleted() != null) {
                    if (boardResponse.getUserPerceivedPercentageTaskCompleted() == null || (boardResponse.getUserPerceivedPercentageTaskCompleted() != null && boardResponse.getUserPerceivedPercentageTaskCompleted() < taskToUpdate.getUserPerceivedPercentageTaskCompleted())) {
                        throw new BoardViewErrorException("Incorrect values for user perceived percentage Work Item completed in Work Item# " + boardResponse.getTaskNumber());
                    }
                }

                Integer increaseInUserPerceivedPercent = boardResponse.getNewIncreaseInUserPerceivedPercentageTaskCompleted();
                if (increaseInUserPerceivedPercent == null || increaseInUserPerceivedPercent < 0 || increaseInUserPerceivedPercent > 100) {
                    throw new BoardViewErrorException("Invalid value for increase in user perceived percentage Work Item completed. Work Item# " + boardResponse.getTaskNumber());
                } else {
                    taskToUpdate.setIncreaseInUserPerceivedPercentageTaskCompleted(boardResponse.getNewIncreaseInUserPerceivedPercentageTaskCompleted());
                }
                taskToUpdate.setNewEffortTracks(boardResponse.getNewEffortTracks());
            } else if(boardResponse.getUserPerceivedPercentageTaskCompleted()!=null && boardResponse.getNewIncreaseInUserPerceivedPercentageTaskCompleted() != null && boardResponse.getNewIncreaseInUserPerceivedPercentageTaskCompleted() > 0) {
                throw new BoardViewErrorException("Efforts can't be empty when only user perceived percentage been changed for Work Item# " + boardResponse.getTaskNumber());
            }
            taskToUpdate.setCurrentlyScheduledTaskIndicator(boardResponse.getCurrentlyScheduledTaskIndicator());
            taskToUpdate.setUserPerceivedPercentageTaskCompleted(boardResponse.getUserPerceivedPercentageTaskCompleted());
            WorkFlowTaskStatus workFlowTaskStatus = workflowTaskStatusService.getWorkFlowTaskStatusByWorkflowTaskStatusAndWorkflowTypeId(boardResponse.getWorkflowTaskStatus(), boardResponse.getTaskWorkflowId());
            taskToUpdate.setFkWorkflowTaskStatus(workFlowTaskStatus);
            taskToUpdate.setTaskState(workFlowTaskStatus.getWorkflowTaskState());
            taskToUpdate.setTaskPriority(boardResponse.getTaskPriority());
            taskToUpdate.setFkAccountIdLastUpdated(userAccountService.getActiveUserAccountByAccountId(accountIdRequester));
            LocalDateTime convertedActualStartDate = null, convertedActualEndDate = null;
            if (boardResponse.getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED_TITLE_CASE)) {
                taskToUpdate.setBlockedReasonTypeId(boardResponse.getBlockedReasonTypeId());
                taskToUpdate.setBlockedReason(boardResponse.getBlockedReason());
                taskToUpdate.setFkAccountIdRespondent(boardResponse.getFkAccountIdRespondent());
                taskToUpdate.setReminderInterval(boardResponse.getReminderInterval());
            }
            if (boardResponse.getTaskActStDate() != null && boardResponse.getTaskActStTime() != null) {
                convertedActualStartDate = DateTimeUtils.convertUserDateToServerTimezone(boardResponse.getTaskActStDate(), timeZone);
            }
            if (boardResponse.getTaskActEndDate() != null && boardResponse.getTaskActEndTime() != null) {
                convertedActualEndDate = DateTimeUtils.convertUserDateToServerTimezone(boardResponse.getTaskActEndDate(), timeZone);
            }
            taskToUpdate.setTaskActEndDate(convertedActualEndDate);
            taskToUpdate.setTaskActEndTime(convertedActualEndDate != null ? convertedActualEndDate.toLocalTime() : null);
            taskToUpdate.setTaskActStDate(convertedActualStartDate);
            taskToUpdate.setTaskActStTime(convertedActualStartDate != null ? convertedActualStartDate.toLocalTime() : null);

            if (taskDb.getTaskActStDate() != null && !Objects.equals(taskDb.getTaskActStDate(), taskToUpdate.getTaskActStDate())) {
                throw new ValidationFailedException("Actual start date time of work item can't be changed");
            }
            if (taskDb.getTaskActEndDate() != null && !Objects.equals(taskDb.getTaskActEndDate(), taskToUpdate.getTaskActEndDate())) {
                throw new ValidationFailedException("Actual end date time of work item can't be changed");
            }

//            boolean isTaskDataValidated = taskServiceImpl.validateTaskWorkflowType(taskToUpdate);
            boolean isTaskValidatedByWorkflowStatus = taskServiceImpl.validateTaskByWorkflowStatus(taskDb, taskToUpdate, headerAccountIds);
//            boolean isTaskValidated = taskServiceImpl.validateTaskForWorkflowStatus(taskDb, taskToUpdate);
//            boolean isTaskValidatedForDateAndTimePairs = taskServiceImpl.validateAllDateAndTimeForPairs(taskToUpdate);
            if (!isTaskValidatedByWorkflowStatus) {
                throw new ForbiddenException("user not allowed to update the Work Item");
            }
            taskServiceImpl.updateTimeSheetAndRecordedEffort(taskToUpdate, timeZone);
            taskServiceImpl.updateFieldsInTaskTable(taskToUpdate, taskToUpdate.getTaskId(), timeZone,headerAccountIds);
        }

        /* If the update is happening for only active tasks (/active-tasks API) then we return only active tasks list -- the update board request will have all accountIds of the user*/
        if (updateBoardRequest.getOrgId() == null && updateBoardRequest.getTeamId() == null) {
            return getCurrentlyScheduledTasks(updateBoardRequest.getAccountIds(), timeZone, updateBoardRequest.getSortingPriorityList());
        }

        ViewBoardRequest viewBoardRequest = new ViewBoardRequest(updateBoardRequest.getAccountIds(), updateBoardRequest.getOrgId(), updateBoardRequest.getTeamId(), updateBoardRequest.getProjectId(), updateBoardRequest.getSprintId(), updateBoardRequest.getLabelIds(), updateBoardRequest.getCurrentTaskTimeSheetIndicator(), updateBoardRequest.getSortingPriorityList());
        return getBoardTasks(viewBoardRequest, accountIdRequester, timeZone);
    }

    /**
     *
     * This method sorts a list<task> in 3 categories defined in getBoardTask and within the categories it sorts the tasks based on priority
     */
    @Deprecated
    public List<Task> getSortedTasks(List<Task> boardTasks, LocalDate previousDate){
        List<Long> taskIds = new ArrayList<>();

        List<String> invalidStatuses = List.of(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE, Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE);

        List<List<Task>> tasklists = new ArrayList<>();
        int taskListSize = 13;
        for (int i=0; i<taskListSize; i++){
            tasklists.add(new ArrayList<>());
        }

        for(Task boardTask: boardTasks){

            LocalDate actualEndDate = boardTask.getTaskActEndDate() == null ? null : boardTask.getTaskActEndDate().toLocalDate();

            // remove tasks where workflow status == completed or deleted and actual end date is less than previous working day
            if (invalidStatuses.contains(boardTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus())) {
                if (actualEndDate == null || !actualEndDate.isAfter(previousDate.minusDays(1))) {
                    continue;
                }
            }

            /* List Array 0-4 will have lists tasks that have actual end date greater or equal to the previous working day sorted asc P0 to P4
               List Array 5 - 9 will have tasks that have currently scheduled task indicator as true sorted asc P0 to P4
               List Array 10-12 will have all other tasks sorted asc (p2, p3, p4) */
            String priority = boardTask.getTaskPriority();
            if(actualEndDate != null && actualEndDate.isAfter(previousDate.minusDays(1))){
                switch(priority){
                    case Constants.Priorities.PRIORITY_P0:
                        tasklists.get(0).add(boardTask); break;
                    case Constants.Priorities.PRIORITY_P1:
                        tasklists.get(1).add(boardTask); break;
                    case Constants.Priorities.PRIORITY_P2:
                        tasklists.get(2).add(boardTask); break;
                    case Constants.Priorities.PRIORITY_P3:
                        tasklists.get(3).add(boardTask); break;
                    case Constants.Priorities.PRIORITY_P4:
                        tasklists.get(4).add(boardTask); break;
                    default: break;
                }
            }

            else if(boardTask.getCurrentlyScheduledTaskIndicator() != null && boardTask.getCurrentlyScheduledTaskIndicator()) {
                // assumption that task that are completed have currently Scheduled Task indicator as off
                assert actualEndDate == null;
                switch (priority) {
                    case Constants.Priorities.PRIORITY_P0:
                        tasklists.get(5).add(boardTask); break;
                    case Constants.Priorities.PRIORITY_P1:
                        tasklists.get(6).add(boardTask); break;
                    case Constants.Priorities.PRIORITY_P2:
                        tasklists.get(7).add(boardTask); break;
                    case Constants.Priorities.PRIORITY_P3:
                        tasklists.get(8).add(boardTask); break;
                    case Constants.Priorities.PRIORITY_P4:
                        tasklists.get(9).add(boardTask); break;
                    default:
                        break;
                }
            }

            else if(actualEndDate == null && ((boardTask.getCurrentlyScheduledTaskIndicator()!=null && !boardTask.getCurrentlyScheduledTaskIndicator())) || boardTask.getCurrentlyScheduledTaskIndicator() == null) {
                // assumption: priority p0 & p1 will have current scheduled task indicator as true
                switch (priority) {
                    case Constants.Priorities.PRIORITY_P0:
                    case Constants.Priorities.PRIORITY_P1:
                        throw new BoardViewErrorException("Work Item with Priority P0 & P1 (Not Completed/ Deleted) can not have currently scheduled Work Item indicator as false");
                    case Constants.Priorities.PRIORITY_P2:
                        tasklists.get(10).add(boardTask);
                        break;
                    case Constants.Priorities.PRIORITY_P3:
                        tasklists.get(11).add(boardTask);
                        break;
                    case Constants.Priorities.PRIORITY_P4:
                        tasklists.get(12).add(boardTask);
                        break;
                    default:
                        break;
                }

            }
        }

        List<Task> allSortedTasks = new ArrayList<>();
        for (List<Task> innerList : tasklists) {
            allSortedTasks.addAll(innerList);
        }

        return allSortedTasks;


    }

    /**
     *
     * This helper method is used to calculate sum of efforts recorded for a given date
     */
    public Integer[] calculateDayEffort(Long taskId, LocalDate effortDate){

        // assuming not for personal task
        List<TimeSheet> tsRecords = timeSheetRepository.findAllByEntityIdAndNewEffortDateAndTaskTypeNotPersonal(taskId, effortDate);

        int sumOfEfforts = 0, sumofIncreaseInUserPerceivedPercentage = 0;
        for(TimeSheet tsRecord: tsRecords){
            sumOfEfforts += tsRecord.getNewEffort();
            int increaseInUserPerceived = tsRecord.getIncreaseInUserPerceivedPercentageTaskCompleted() == null ? 0 : tsRecord.getIncreaseInUserPerceivedPercentageTaskCompleted();
            sumofIncreaseInUserPerceivedPercentage += increaseInUserPerceived;
        }
        return new Integer[]{sumOfEfforts, sumofIncreaseInUserPerceivedPercentage};
    }

    /**
     * This helper method validates the conditions based on workFlowStatus in the boardResponse
     */
    public void validateWorkFlowTaskStatus(BoardResponse boardResponse, String taskPrevWorkflowStatus){
        String workflowTaskStatus = boardResponse.getWorkflowTaskStatus().toLowerCase();
        switch (workflowTaskStatus) {
            case Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED:
                if(boardResponse.getNewEffortTracks() == null || (boardResponse.getNewEffortTracks()!=null && boardResponse.getNewEffortTracks().isEmpty()) ||
                        boardResponse.getCurrentlyScheduledTaskIndicator() || boardResponse.getUserPerceivedPercentageTaskCompleted() == null ||
                        boardResponse.getTaskActEndDate() == null || boardResponse.getTaskActEndTime()==null || boardResponse.getTaskActStDate() == null || boardResponse.getTaskActStTime()==null){
                    throw new BoardViewErrorException("Invalid Request for Work Item with workFlow status completed. Work Item# " + boardResponse.getTaskNumber());
                }
                break;

            case Constants.WorkFlowTaskStatusConstants.STATUS_STARTED:
                List<String> invalidWorkflowStatusForStarted = Arrays.asList(Constants.WorkFlowTaskStatusConstants.STATUS_DELETE, Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED);
                if(boardResponse.getTaskActStDate() == null || boardResponse.getTaskActStTime()==null || invalidWorkflowStatusForStarted.contains(taskPrevWorkflowStatus)){
                    throw new BoardViewErrorException("Invalid Request for Work Item with workFlow status started. Work Item# " + boardResponse.getTaskNumber());
                }
                break;

            case Constants.WorkFlowTaskStatusConstants.STATUS_DELETE:
            case Constants.WorkFlowTaskStatusConstants.STATUS_ON_HOLD:
            case Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED:
                if((boardResponse.getNewEffortTracks() != null && !boardResponse.getNewEffortTracks().isEmpty()) || boardResponse.getCurrentlyScheduledTaskIndicator() || boardResponse.getTaskActEndDate() != null || boardResponse.getTaskActEndTime()!=null){
                    throw new BoardViewErrorException("Invalid Request for Task with workFlow status delete/ on hold/ blocked. Task# " + boardResponse.getTaskNumber());
                }
                break;

            case Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG:
                throw new BoardViewErrorException("Backlog Task not allowed in boardView. Task# " + boardResponse.getTaskNumber());

            case Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED:
                List<String> invalidWorkflowStatusForNotStarted = Arrays.asList(Constants.WorkFlowTaskStatusConstants.STATUS_STARTED, Constants.WorkFlowTaskStatusConstants.STATUS_ON_HOLD, Constants.WorkFlowTaskStatusConstants.STATUS_DELETE, Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED);
                if (taskPrevWorkflowStatus.equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED) && boardResponse.getTaskActStDate() != null) {
                    throw new BoardViewErrorException("Invalid Request for Task with workFlow status not started. Task# " + boardResponse.getTaskNumber());
                }
                if((boardResponse.getNewEffortTracks() != null && !boardResponse.getNewEffortTracks().isEmpty()) || boardResponse.getTaskActEndDate() != null || boardResponse.getTaskActEndTime()!=null || boardResponse.getTaskActStTime()!=null || boardResponse.getTaskActStDate()!=null || invalidWorkflowStatusForNotStarted.contains(taskPrevWorkflowStatus)){
                    throw new BoardViewErrorException("Invalid Request for Task with workFlow status not started. Task# " + boardResponse.getTaskNumber());
                }
                break;
        }
    }

    /**
     *
     * @param accountIds
     * This method expects all accountIds of the user in the parameter. It gets all the tasks assigned to the user where the currently scheduled
     * indicator is on (irrespective of the org). It sorts the tasks based on the expected end date time and returns the list of board responses created for those tasks
     */
    public List<BoardResponse> getCurrentlyScheduledTasks(List<Long> accountIds, String timeZone, HashMap<Integer, SortingField> sortingPriorityList){

        LocalDate currentDate = LocalDate.now();
        //here we are using calender days need to replace something dynamic
        LocalDate previousDate = calendarDaysRepository.findPreviousBusinessDayByDate(currentDate);

        // get all tasks of the user where the currentlyScheduledIndicator is on (for all organizations)
        List<Task> boardTasks = taskRepository.findByFkAccountIdAssignedAccountIdInAndCurrentlyScheduledTaskIndicatorOrderByTaskPriority(accountIds, true);
        if(!boardTasks.isEmpty()) boardTasks.sort(Comparator.comparing(Task::getTaskExpEndDate));
        return createBoardResponsesFromTaskList(boardTasks, previousDate, timeZone, sortingPriorityList);
    }

    /**
     * get updated Fields in the updated task in the board request
     */
    public List<String> getUpdatedFields(Task originalTask, BoardResponse boardResponse) {
        List<String> updatedFields = new ArrayList<>();

        // Compare task fields
        if (!Objects.equals(originalTask.getTaskExpEndDate(), boardResponse.getTaskExpEndDate())) {
            updatedFields.add("taskExpEndDate");
        }
        if (!Objects.equals(originalTask.getTaskActEndDate(), boardResponse.getTaskActEndDate())) {
            updatedFields.add("taskActEndDate");
        }
        if (!Objects.equals(originalTask.getTaskActEndTime(), boardResponse.getTaskActEndTime())) {
            updatedFields.add("taskActEndTime");
        }
        if (!Objects.equals(originalTask.getTaskActStDate(), boardResponse.getTaskActStDate())) {
            updatedFields.add("taskActStDate");
        }
        if (!Objects.equals(originalTask.getTaskActStTime(), boardResponse.getTaskActStTime())) {
            updatedFields.add("taskActStTime");
        }
        if (!Objects.equals(originalTask.getTaskEstimate(), boardResponse.getTaskEstimate())) {
            updatedFields.add("taskEstimate");
        }
        if (!Objects.equals(originalTask.getCurrentlyScheduledTaskIndicator(), boardResponse.getCurrentlyScheduledTaskIndicator())) {
            updatedFields.add("currentlyScheduledTaskIndicator");
        }
        if (!Objects.equals(originalTask.getUserPerceivedPercentageTaskCompleted(), boardResponse.getUserPerceivedPercentageTaskCompleted())) {
            updatedFields.add("userPerceivedPercentageTaskCompleted");
        }
        if (!Objects.equals(originalTask.getTaskWorkflowId(), boardResponse.getTaskWorkflowId())) {
            updatedFields.add("taskWorkflowId");
        }
        if (!Objects.equals(originalTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), boardResponse.getWorkflowTaskStatus())) {
            updatedFields.add("fkWorkflowTaskStatus");
        }
        if (!Objects.equals(originalTask.getRecordedEffort(), boardResponse.getRecordedEffort())) {
            updatedFields.add("recordedEffort");
        }
        if (!Objects.equals(originalTask.getSystemDerivedEndTs(), boardResponse.getSystemDerivedEndTs())) {
            updatedFields.add("systemDerivedEndTs");
        }

        // Compare associated entities
        if (!Objects.equals(originalTask.getFkAccountIdAssigned().getAccountId(), boardResponse.getAccountIdAssigned())) {
            updatedFields.add("fkAccountIdAssigned");
        }
        if (!Objects.equals(originalTask.getFkTeamId().getTeamId(), boardResponse.getTeamId())) {
            updatedFields.add("fkTeamId");
        }
        if (boardResponse.getNewEffortTracks() != null && !boardResponse.getNewEffortTracks().isEmpty()) {
            updatedFields.add("newEffortTracks");
        }

        // Compare additional fields from BoardResponse
        if (!Objects.equals(originalTask.getTaskId(), boardResponse.getTaskId())) {
            updatedFields.add("taskId");
        }
        if (!Objects.equals(originalTask.getTaskTypeId(), boardResponse.getTaskTypeId())) {
            updatedFields.add("taskTypeId");
        }
        if (!Objects.equals(originalTask.getTaskNumber(), boardResponse.getTaskNumber())) {
            updatedFields.add("taskNumber");
        }
        if (!Objects.equals(originalTask.getTaskTitle(), boardResponse.getTaskTitle())) {
            updatedFields.add("taskTitle");
        }
        if (!Objects.equals(originalTask.getTaskPriority(), boardResponse.getTaskPriority())) {
            updatedFields.add("taskPriority");
        }
        if (!Objects.equals(originalTask.getTaskProgressSystem(), boardResponse.getTaskProgressSystem())) {
            updatedFields.add("taskProgressSystem");
        }

        return updatedFields;
    }

    public void isUpdateAuthorized(Task task, Long accountIdRequester, List<String> updatedFields, List<String> basicFields, List<String> essentialFields) {

        List<String> userActions = actionService.getUserActionList(accountIdRequester, task.getFkTeamId().getTeamId());

        // Calculate if the task is self-assigned
        boolean isSelfAssigned = false;
        if (task.getFkAccountIdAssigned() != null) {
            if (Objects.equals(accountIdRequester, task.getFkAccountIdAssigned().getAccountId())) {
                isSelfAssigned = true;
            }
        }

        // Separate updated fields into basic, essential, and non-update categories
        List<String> finalBasicUpdate = new ArrayList<>();
        List<String> finalEssentialUpdate = new ArrayList<>();
        List<String> finalNotUpdate = new ArrayList<>();
        for (String updateFieldByUser : updatedFields) {
            if (basicFields.contains(updateFieldByUser)) {
                finalBasicUpdate.add(updateFieldByUser);
            } else if (essentialFields.contains(updateFieldByUser)) {
                finalEssentialUpdate.add(updateFieldByUser);
            } else {
                finalNotUpdate.add(updateFieldByUser);
            }
        }

        // Check authorization based on user's actions and whether the task is self-assigned
        if (isSelfAssigned) {
            if (!finalNotUpdate.isEmpty() ||
                    (!finalEssentialUpdate.isEmpty() && !userActions.contains(Constants.UpdateTeam.Task_Essential_Update)) ||
                    (!finalBasicUpdate.isEmpty() && !userActions.contains(Constants.UpdateTeam.Task_Basic_Update))) {
                throw new ValidationFailedException("You're not authorized to update the task# " + task.getTaskNumber());
            }
        } else {
            if (!finalNotUpdate.isEmpty() ||
                    (!finalEssentialUpdate.isEmpty() && !userActions.contains(Constants.UpdateTeam.All_Task_Essential_Update)) ||
                    (!finalBasicUpdate.isEmpty() && !userActions.contains(Constants.UpdateTeam.All_Task_Basic_Update))) {
                throw new ValidationFailedException("You're not authorized to update the task# " + task.getTaskNumber());
            }
        }
    }

    public void sortBoardTaskListBySortFilters (List<BoardResponse> boardResponses, HashMap<Integer, SortingField> sortingFields) {
        //getting a list of field names by sorting them according to priority
        List<Map.Entry<Integer, SortingField>> sortedEntries = sortingFields.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toList());
        Comparator<BoardResponse> taskComparator = Comparator.comparing(task -> 0); // Initial comparator

        //checking for Comparator and adding fields on the basis of priority
        for (Map.Entry<Integer, SortingField> entry : sortedEntries) {
            SortingField sortingField = entry.getValue();
            Comparator<BoardResponse> fieldComparator;

            // Define custom sorting logic for specific fields
            switch (sortingField.getFieldName()) {
                case "taskPriority":
                    fieldComparator = Comparator.comparing(task -> getPriorityValue(task.getTaskPriority()), Comparator.nullsLast(Comparator.naturalOrder()));
                    break;
                case "workflowTaskStatusType":
                    fieldComparator = Comparator.comparing(task -> getWorkflowStatusValue(task.getWorkflowTaskStatus()), Comparator.nullsLast(Comparator.naturalOrder()));
                    break;
                case "taskNumber":
                case "taskExpStartDate":
                case "taskExpEndDate":
                case "taskActStDate":
                case "taskActEndDate":
                    fieldComparator = Comparator.comparing(task -> {
                        try {
                            Field field = BoardResponse.class.getDeclaredField(sortingField.getFieldName());
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

        boardResponses.sort(taskComparator);
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
}
