package com.tse.core_application.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tse.core_application.configuration.DataEncryptionConverter;
import com.tse.core_application.custom.model.NewEffortTrack;
import com.tse.core_application.custom.model.OrgIdOrgName;
import com.tse.core_application.custom.model.TaskMaster;
import com.tse.core_application.dto.*;
import com.tse.core_application.dto.personal_task.*;
import com.tse.core_application.exception.DeleteTaskException;
import com.tse.core_application.exception.FileNameException;
import com.tse.core_application.exception.FileNotFoundException;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.model.*;
import com.tse.core_application.model.User;
import com.tse.core_application.model.personal_task.PersonalAttachment;
import com.tse.core_application.model.personal_task.PersonalNote;
import com.tse.core_application.model.personal_task.PersonalTask;
import com.tse.core_application.repository.*;
import com.tse.core_application.utils.CommonUtils;
import com.tse.core_application.utils.ComponentUtils;
import com.tse.core_application.utils.DateTimeUtils;
import com.tse.core_application.utils.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.Query;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static com.tse.core_application.model.Constants.PERSONAL_ORG;
import static com.tse.core_application.model.Constants.PERSONAL_TEAM_ID;
import static com.tse.core_application.model.Constants.WorkFlowTaskStatusConstants.*;

@Service
public class PersonalTaskService {

    private static final Logger logger = LogManager.getLogger(PersonalTaskService.class.getName());

    @Autowired
    private WorkFlowTaskStatusRepository workFlowTaskStatusRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private WorkflowTaskStatusService workflowTaskStatusService;
    @Autowired
    private PersonalTaskRepository personalTaskRepository;
    @Autowired
    private AuditService auditService;
    @Autowired
    private NoteService noteService;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private PersonalAttachmentRepository personalAttachmentRepository;
    @Autowired
    private RecurrenceService recurrenceService;
    @Value("${personal.org.id}")
    private Long personalOrgId;
    @Autowired
    private StatsService statsService;
    @Autowired
    private TimeSheetRepository timeSheetRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private PersonalTaskSequenceRepository personalTaskSequenceRepository;


    // ************** methods related to add personal task *********************


    /**
     * validates the dates and workflow validations on add personal task request
     * 1. Priority is mandatory in Not started
     * 2. Mandatory Expected End Date: If the workflow status is "Not Started" and the priority is "P0" or "P1",
     * the expected end date must be provided by the user
     * 3. if both expected start date and end dates are present make sure end date is after start date
     * 4. only not started and 'due date not provided' (backlog) status are allowed
     * 5. validate the accountId provided in the request belongs to the personal org
     */
    public void validateAndNormalizeAddPersonalTaskRequest(PersonalTaskAddRequest request, Long updatedByAccountId) {
        // validate that the accountId belongs to the Personal Org
        validateAccountAssociateWithPersonalOrg(request, updatedByAccountId);
        validateAndNormalizeWorkflowStatusAndPriorityOnAddPersonalTask(request);
        validateDateForAddPersonalTask(request);
        if (request.getTaskTitle() != null) {
            request.setTaskTitle(request.getTaskTitle().trim());
        }
        if (request.getTaskDesc() != null) {
            request.setTaskDesc(request.getTaskDesc().trim());
        }
        if(request.getTaskEstimate()!=null && request.getTaskEstimate().equals(0))
            request.setTaskEstimate(null);
    }


    /**
     * validate the workflow status on add personal task
     */
    private void validateAndNormalizeWorkflowStatusAndPriorityOnAddPersonalTask(PersonalTaskAddRequest request) {
        WorkFlowTaskStatus foundWorkflowTaskStatus = workflowTaskStatusService.getWorkFlowTaskStatusByWorkflowTaskStatusAndWorkflowTypeId(
                request.getWorkflowStatus(), Constants.DEFAULT_WORKFLOW_TYPE_PERSONAL_TASK);

        List<String> allowedWorkflowStatuses = Arrays.asList(
                Constants.WorkFlowTaskStatusConstants.STATUS_DUE_DATE_NOT_PROVIDED,
                STATUS_NOT_STARTED_TITLE_CASE);

        // Validate the found workflow task status
        if (foundWorkflowTaskStatus == null || !allowedWorkflowStatuses.contains(request.getWorkflowStatus())) {
            throw new ValidationFailedException("Invalid workflow status");
        }

        // normalization: if expected end date is present move the task to Not Started
        if (request.getTaskExpEndDate() != null && request.getWorkflowStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_DUE_DATE_NOT_PROVIDED)) {
            request.setWorkflowStatus(STATUS_NOT_STARTED_TITLE_CASE);
        }

        if (request.getTaskPriority() == null) {
            request.setTaskPriority(Constants.Priorities.PRIORITY_P2);
        }

        // Additional validation for the 'Not Started' status
        if (STATUS_NOT_STARTED_TITLE_CASE.equals(request.getWorkflowStatus())) {
            validateNotStartedStatus(request);
        }
    }


    /**
     * Separate method to encapsulate the validation logic for 'Not Started' status
     */
    private void validateNotStartedStatus(PersonalTaskAddRequest request) {
        // Validate expected end date for specific priorities -- P1 should have both estimate and end date time
        if (Constants.PRIORITY_P1.equals(request.getTaskPriority()) || Constants.PRIORITY_P0.equals(request.getTaskPriority())) {
            if (request.getTaskExpEndDate() == null) {
                throw new ValidationFailedException("Expected End Date is mandatory when the workflow status is 'Not Started' and " +
                        "Priority is 'P0' or 'P1'");
            }

            if (request.getTaskEstimate() == null) {
                throw new ValidationFailedException("Estimate is mandatory for Priority 'P1'");
            }
        }
    }


    /**
     * validate dates on add personal task
     */
    private void validateDateForAddPersonalTask(PersonalTaskAddRequest request) {
        if (request.getTaskExpEndDate() != null && request.getTaskExpStartDate() != null) {
            if (request.getTaskExpEndDate().isBefore(request.getTaskExpStartDate())) {
                throw new ValidationFailedException("Expected start date can't be after expected end date");
            }
        }
    }


    /**
     * Validates that the account belong to the personal organization
     */
    private void validateAccountAssociateWithPersonalOrg(PersonalTaskAddRequest request, Long updatedByAccountId) {
//        if (!Objects.equals(updatedByAccountId, request.getAccountId())) throw new ValidationFailedException("Incorrect Account in header");
        UserAccount userAccount = userAccountRepository.findById(updatedByAccountId).orElseThrow(() -> new EntityNotFoundException("User account doesn't exist"));
        OrgIdOrgName orgIdOrgName = organizationService.getOrganizationByOrgId(userAccount.getOrgId());
        if (orgIdOrgName == null || !Objects.equals(orgIdOrgName.getOrganizationName(), PERSONAL_ORG)) {
            throw new ValidationFailedException("Account is not a personal account");
        }
    }


    /**
     * set task state as per the workflow status
     */
    private void setPersonalTaskRequestStateByWorkflowTaskStatus(PersonalTask task) {
        String workflowTaskStatus = task.getFkWorkflowTaskStatus().getWorkflowTaskStatus();
        Integer taskWorkflowId = Constants.TaskWorkFlowIds.PERSONAL_TASK;

        if (workflowTaskStatus != null && !workflowTaskStatus.isEmpty()) {
            WorkFlowTaskStatus taskStatus = workFlowTaskStatusRepository.findByWorkflowTaskStatusAndFkWorkFlowTypeWorkflowTypeId(workflowTaskStatus, taskWorkflowId);
            task.setTaskState(taskStatus.getWorkflowTaskState());
        }
    }


    /**
     * the mapping should either be done on the backend or frontend. We are assuming the frontend will send us
     * the value as we receive in the case of Task
     */
    private void modifyPersonalTaskPriority(PersonalTask task) {
        if (task.getTaskPriority() != null) {
            String taskPriority = task.getTaskPriority();
            String modifiedTaskPriority = taskPriority.substring(0, 2);
            task.setTaskPriority(modifiedTaskPriority);
        }
    }


    /**
     * method assigns a unique task number to a new task. It is max value present in the personal task table for
     * the task number + 1
     */
    public void initializeTaskNumberSetProperties(PersonalTask personalTask) {
//        Long taskNumber = personalTaskRepository.getMaxTaskNumber();
        Long taskIdentifier = getNextTaskIdentifier(personalTask.getFkAccountId().getAccountId());
        personalTask.setPersonalTaskIdentifier(taskIdentifier);
//        String initials = getAccountInitials(personalTask.getFkAccountId().getAccountId());
        personalTask.setPersonalTaskNumber("P-" + taskIdentifier);
    }

    /** gets the account initials from the first and last name of the user*/
//    private String getAccountInitials(Long accountId) {
//        UserAccount userAccount = userAccountRepository.findByAccountIdAndIsActive(accountId, true);
//        User user = userAccount.getFkUserId();
//        return String.valueOf(user.getFirstName().charAt(0)) + user.getLastName().charAt(0);
//    }

    /** method to generate a unique task identifier for a given account*/
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long getNextTaskIdentifier(Long accountId) {
        PersonalTaskSequence sequence = personalTaskSequenceRepository.findByAccountIdForUpdate(accountId);

        if (sequence == null) {
            sequence = new PersonalTaskSequence(accountId, 0L);
        }
        Long nextTaskIdentifier = sequence.getLastTaskIdentifier() + 1;
        sequence.setLastTaskIdentifier(nextTaskIdentifier);
        personalTaskSequenceRepository.save(sequence);
        return nextTaskIdentifier;
    }

    /**
     * saves a new personal task in the personal task table
     */
    public PersonalTaskResponse savePersonalTask(PersonalTaskAddRequest request, Long accountId, String timeZone) {
        PersonalTask personalTask = populatePersonalTaskObject(request, accountId, timeZone);
        computeStats(personalTask);
        PersonalTask savedTask = personalTaskRepository.save(personalTask);

        List<PersonalNote> notes = new ArrayList<>();
        if (request.getNoteRequestList() != null) {
            notes = noteService.saveAllNotesOnAddPersonalTask(request.getNoteRequestList(), savedTask);
        }
        auditService.createAuditForAddPersonalTask(savedTask);
        return createPersonalTaskResponse(savedTask, timeZone, notes);
    }


    /**
     * this method is used to save the task Object and create audit
     */
    public PersonalTask saveTask(PersonalTask task) {
        PersonalTask savedTask = personalTaskRepository.save(task);
        auditService.createAuditForAddPersonalTask(savedTask);
        return savedTask;
    }


    /**
     * creates a personal Task object from the personal task add request
     */
    private PersonalTask populatePersonalTaskObject(PersonalTaskAddRequest request, Long accountId, String timeZone) {
        PersonalTask personalTask = new PersonalTask();
        personalTask.setTaskTypeId(Constants.TaskTypes.PERSONAL_TASK);
        personalTask.setTaskWorkflowId(Constants.DEFAULT_WORKFLOW_TYPE_PERSONAL_TASK);
        BeanUtils.copyProperties(request, personalTask);
        WorkFlowTaskStatus workFlowTaskStatus = workflowTaskStatusService.getWorkFlowTaskStatusByWorkflowTaskStatusAndWorkflowTypeId(
                request.getWorkflowStatus(), Constants.DEFAULT_WORKFLOW_TYPE_PERSONAL_TASK);
        personalTask.setFkWorkflowTaskStatus(workFlowTaskStatus);
        UserAccount userAccount = userAccountRepository.findByAccountIdAndIsActive(accountId, true);
        personalTask.setFkAccountId(userAccount);
        initializeTaskNumberSetProperties(personalTask);

        modifyPersonalTaskPriority(personalTask);
        convertPersonalTaskAllLocalDateAndTimeToServerTimeZone(personalTask, timeZone);
        setPersonalTaskRequestStateByWorkflowTaskStatus(personalTask);
        return personalTask;
    }


    /**
     * creates the response from personal task
     */
    private PersonalTaskResponse createPersonalTaskResponse(PersonalTask personalTask, String timeZone, List<PersonalNote> updatedNotes) {
        PersonalTask task = new PersonalTask();
        BeanUtils.copyProperties(personalTask, task);
        convertTaskAllServerDateAndTimeInToLocalTimeZone(task, timeZone);
        PersonalTaskResponse response = new PersonalTaskResponse();
        BeanUtils.copyProperties(task, response);
        response.setWorkflowStatus(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus());
        response.setAccountId(task.getFkAccountId().getAccountId());
        response.setEmail(task.getFkAccountId().getEmail());
        response.setFullName(getFullNameOfAccount(task.getFkAccountId()));

        if (task.getNotes() != null || (updatedNotes != null && updatedNotes.isEmpty())) {
            List<PersonalNote> currentNotes = task.getNotes() != null ? task.getNotes() : new ArrayList<>();
            // remove the notes that are deleted (isDelete = true)
            List<PersonalNote> filteredNotes = currentNotes.stream().filter(note -> !note.getIsDeleted()).collect(Collectors.toList());

            if (updatedNotes != null && !updatedNotes.isEmpty()) {
                // Remove notes from currentNotes if they are marked as deleted in updatedNotes
                filteredNotes.removeIf(note -> updatedNotes.stream()
                        .anyMatch(updatedNote -> updatedNote.getIsDeleted() && updatedNote.getNoteId().equals(note.getNoteId())));

                // Update existing notes and add new ones
                for (PersonalNote updatedNote : updatedNotes) {
                    if (updatedNote.getIsDeleted()) {
                        continue;  // Skip deleted notes
                    }

                    // Find the note in currentNotes to update, or null if it's a new note
                    PersonalNote existingNote = filteredNotes.stream()
                            .filter(note -> note.getNoteId().equals(updatedNote.getNoteId()))
                            .findFirst()
                            .orElse(null);

                    if (existingNote != null) {
                        // Update the existing note's content if it's found
                        int index = currentNotes.indexOf(existingNote);
                        currentNotes.set(index, updatedNote);
                    } else {
                        // Add the new note to currentNotes
                        currentNotes.add(updatedNote);
                    }
                }
            }

            response.setNotes(filteredNotes);
        }
        return response;
    }


    // ************** methods related to update personal task *********************


    /**
     * validates the update task request before updating the personal task
     */
    public void validateAndNormalizeUpdatePersonalTaskRequest(PersonalTaskUpdateRequest request, String timeZone, Long updatedByAccountId) {
        PersonalTask task = personalTaskRepository.findById(request.getPersonalTaskId())
                .orElseThrow(() -> new EntityNotFoundException("Personal Work Item not found"));

        hasPermissionToUpdatePersonalTask(task, updatedByAccountId);
        validateWorkflowStatusOnUpdatePersonalTask(task, request);
        validateAndConvertDateForUpdatePersonalTask(request, timeZone);
        if (request.getTaskTitle() != null) {
            request.setTaskTitle(request.getTaskTitle().trim());
        }
        if (request.getTaskDesc() != null) {
            request.setTaskDesc(request.getTaskDesc().trim());
        }
    }


    /**
     * validates conditions on dates for update personal task and converts the dates in request to server timeZone
     */
    private void validateAndConvertDateForUpdatePersonalTask(PersonalTaskUpdateRequest request, String timeZone) {
        if (request.getTaskExpEndDate() != null && request.getTaskExpStartDate() != null) {
            if (request.getTaskExpEndDate().isBefore(request.getTaskExpStartDate())) {
                throw new ValidationFailedException("Expected start date can't be after expected end date");
            }
        }

        if (request.getTaskActStDate() != null && request.getTaskActEndDate() != null) {
            if (request.getTaskActEndDate().isBefore(request.getTaskActStDate())) {
                throw new ValidationFailedException("Actual start date can't be after actual end date");
            }
        }

        if (request.getTaskExpEndDate() != null) {
            request.setTaskExpEndDate(DateTimeUtils.convertUserDateToServerTimezone(request.getTaskExpEndDate(), timeZone));
        }

        if (request.getTaskExpStartDate() != null) {
            request.setTaskExpStartDate(DateTimeUtils.convertUserDateToServerTimezone(request.getTaskExpStartDate(), timeZone));
        }

        if (request.getTaskActStDate() != null) {
            request.setTaskActStDate(DateTimeUtils.convertUserDateToServerTimezone(request.getTaskActStDate(), timeZone));
        }

        if (request.getTaskActEndDate() != null) {
            request.setTaskActEndDate(DateTimeUtils.convertUserDateToServerTimezone(request.getTaskActEndDate(), timeZone));
        }
    }


    /**
     * checks if the user has the permission to update the task
     */
    private void hasPermissionToUpdatePersonalTask(PersonalTask task, Long updatedByAccountId) {
        if (!Objects.equals(task.getFkAccountId().getAccountId(), updatedByAccountId)) {
            throw new ValidationFailedException("You're not authorized to update this Work Item");
        }
    }


    /**
     * validates the workflow status for the update personal task request
     */
    private void validateWorkflowStatusOnUpdatePersonalTask(PersonalTask personalTaskDb, PersonalTaskUpdateRequest request) {
        // If the workflow status is getting updated
        if (request.getWorkflowStatus() != null) {
            WorkFlowTaskStatus newWorkflowTaskStatus = workflowTaskStatusService.getWorkFlowTaskStatusByWorkflowTaskStatusAndWorkflowTypeId(
                    request.getWorkflowStatus(), Constants.DEFAULT_WORKFLOW_TYPE_PERSONAL_TASK);

            if (newWorkflowTaskStatus == null) {
                throw new ValidationFailedException("Invalid workflow status");
            }

            String currentStatus = personalTaskDb.getFkWorkflowTaskStatus().getWorkflowTaskStatus();
            if (Objects.equals(Constants.WorkFlowTaskStatusConstants.STATUS_DUE_DATE_NOT_PROVIDED, currentStatus) && (Objects.equals(Constants.PRIORITY_P1, request.getTaskPriority()) || Objects.equals(Constants.PRIORITY_P0, request.getTaskPriority()) ||
                    Objects.equals(personalTaskDb.getTaskPriority(), Constants.PRIORITY_P1) || Objects.equals(personalTaskDb.getTaskPriority(), Constants.PRIORITY_P0) || request.getTaskExpEndDate() != null)) {
                request.setWorkflowStatus(STATUS_NOT_STARTED_TITLE_CASE);
            }
            // Validate the transition to the new workflow status
            validateTransitionToNewWorkflowStatus(personalTaskDb, newWorkflowTaskStatus, request);
        }
    }


    /**
     * validates conditions when workflow status is updated
     */
    private void validateTransitionToNewWorkflowStatus(PersonalTask personalTaskDb, WorkFlowTaskStatus newWorkflowTaskStatus, PersonalTaskUpdateRequest request) {
        String newStatus = newWorkflowTaskStatus.getWorkflowTaskStatus();
        String currentStatus = personalTaskDb.getFkWorkflowTaskStatus().getWorkflowTaskStatus();

        // Transitioning from Completed or Deleted is restricted
        if (Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE.equals(currentStatus) ||
                Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE.equals(currentStatus)) {
            throw new ValidationFailedException("Cannot transition from " + currentStatus + " status");
        }

        switch (newStatus) {
            case STATUS_DUE_DATE_NOT_PROVIDED:
                validateTransitionToDueDateNotProvided(personalTaskDb, request);
                break;
            case STATUS_NOT_STARTED_TITLE_CASE:
                validateNotStartedTransition(personalTaskDb, request);
                break;
            case STATUS_STARTED_TITLE_CASE:
                validateStartedTransition(personalTaskDb, request);
                break;
            case STATUS_BLOCKED_TITLE_CASE:
            case Constants.WorkFlowTaskStatusConstants.STATUS_ON_HOLD_TITLE_CASE:
                validateBlockedOrOnHoldTransition(personalTaskDb, request);
                break;
            case Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE:
                validateCompletedTransition(personalTaskDb, request);
                break;
            default:
                throw new IllegalArgumentException("Invalid Workflow Status");
        }
    }


    /**
     * validates conditions when the workflow status is getting modified to 'Due Date Not Provided' (Backlog)
     */
    private void validateTransitionToDueDateNotProvided(PersonalTask personalTaskDb, PersonalTaskUpdateRequest request) {
        String currentStatus = personalTaskDb.getFkWorkflowTaskStatus().getWorkflowTaskStatus();

        // Cannot transition to 'Due Date Not Provided' from any status
        if (!STATUS_DUE_DATE_NOT_PROVIDED.equals(currentStatus)) {
            throw new ValidationFailedException("Cannot transition to 'Due Date Not Provided' workflow status from " + currentStatus);
        }

        // Actual Start/ End dates must be null in case of Due Date Not Provided workflow
        if (request.getTaskActStDate() != null || request.getTaskActEndDate() != null) {
            throw new ValidationFailedException("Cannot provide actual start/end dates for 'Due Date Not Provided' workflow status");
        }
    }


    /**
     * validates conditions when the workflow status is getting modified to Not Started
     */
    private void validateNotStartedTransition(PersonalTask personalTaskDb, PersonalTaskUpdateRequest request) {
        String currentStatus = personalTaskDb.getFkWorkflowTaskStatus().getWorkflowTaskStatus();
        // Can transition to Not Started only from Due Date Not Provided
        if (!STATUS_DUE_DATE_NOT_PROVIDED.equals(currentStatus)) {
            throw new ValidationFailedException("Can only transition to Not Started from 'Due Date Not Provided' workflow status");
        }

        // Priority is mandatory for Not Started
        if (personalTaskDb.getTaskPriority() == null && request.getTaskPriority() == null) {
            throw new ValidationFailedException("Priority is mandatory for workflow status 'Not Started'");
        }

        // Validate expected end date for specific priorities
        if (Objects.equals(Constants.PRIORITY_P1, request.getTaskPriority()) || Objects.equals(Constants.PRIORITY_P0, request.getTaskPriority()) ||
                Objects.equals(personalTaskDb.getTaskPriority(), Constants.PRIORITY_P1) || Objects.equals(personalTaskDb.getTaskPriority(), Constants.PRIORITY_P0)) {
            if (request.getTaskExpEndDate() == null) {
                throw new ValidationFailedException("Expected End Date is mandatory when the workflow status is 'Not Started' and " +
                        "Priority is 'P0' or 'P1'");
            }
        }

        // Actual Start/ End dates must be null in case of Not Started workflow status
        if (request.getTaskActStDate() != null || request.getTaskActEndDate() != null) {
            throw new ValidationFailedException("Cannot provide actual start/end dates for Not Started workflow status");
        }
    }


    /**
     * validates conditions when the workflow status is getting modified to Started
     */
    private void validateStartedTransition(PersonalTask personalTaskDb, PersonalTaskUpdateRequest request) {
        // Validate that transition to Started is only from 'Due Date Not Provided' or Not Started
        String currentStatus = personalTaskDb.getFkWorkflowTaskStatus().getWorkflowTaskStatus();

        if (!STATUS_DUE_DATE_NOT_PROVIDED.equals(currentStatus) &&
                !STATUS_NOT_STARTED_TITLE_CASE.equals(currentStatus)) {
            throw new ValidationFailedException("Transition to 'Started' workflow status is only allowed from 'Due Date Not Provided' or 'Not-Started' statuses");
        }

        // If priority is P0 or P1, expected end date is mandatory
        String priority = request.getTaskPriority() != null ? request.getTaskPriority() : personalTaskDb.getTaskPriority();
        if (Constants.PRIORITY_P0.equals(priority) || Constants.PRIORITY_P1.equals(priority)) {
            LocalDateTime expEndDate = request.getTaskExpEndDate() != null ? request.getTaskExpEndDate() : personalTaskDb.getTaskExpEndDate();
            if (expEndDate == null) {
                throw new ValidationFailedException("Expected End Date is mandatory for priority P0 or P1 when workflow status is Started");
            }
        }

        // Actual End Date should be null
        if (request.getTaskActEndDate() != null) {
            throw new ValidationFailedException("Actual End Date should be null when transitioning to Started Workflow Status");
        }

        // If the status is transitioning to Started, and actual start date is provided, it's valid
//        if (request.getTaskActStDate() == null) {
//            throw new ValidationFailedException("Actual start date must be provided when transitioning to Started Workflow Status");
//        }
    }


    /**
     * validates conditions when the workflow status is getting modified to Blocked or On Hold
     */
    private void validateBlockedOrOnHoldTransition(PersonalTask personalTaskDb, PersonalTaskUpdateRequest request) {
        // Validate that transition is not from Completed or Deleted -- already covered in the main method
//        String currentStatus = personalTaskDb.getFkWorkflowTaskStatus().getWorkflowTaskStatus();
//        if (Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE.equals(currentStatus) ||
//                Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE.equals(currentStatus)) {
//            throw new ValidationFailedException("Cannot transition to Blocked or On Hold from " + currentStatus);
//        }

        // Actual End Date should be null
        if (request.getTaskActEndDate() != null) {
            throw new ValidationFailedException("Actual End Date should be null when transitioning to Blocked or On Hold");
        }
    }


    /**
     * validates conditions when the workflow status is getting modified to Completed
     */
    private void validateCompletedTransition(PersonalTask personalTaskDb, PersonalTaskUpdateRequest request) {
        // If the task is already in Completed status, restrict updates to specific fields
        String currentStatus = personalTaskDb.getFkWorkflowTaskStatus().getWorkflowTaskStatus();
        if (Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE.equals(currentStatus)) {
            // Allow updates only to certain fields when the task is already Completed
            if (request.getTaskTitle() != null || request.getTaskPriority() != null || request.getTaskEstimate() != null ||
                    request.getWorkflowStatus() != null || request.getTaskExpStartDate() != null || request.getTaskExpEndDate() != null ||
                    request.getTaskActStDate() != null || request.getTaskActEndDate() != null || request.getCurrentActivityIndicator() != null ||
                    request.getCurrentlyScheduledTaskIndicator() != null || request.getNewEffortTracks() != null ||
                    request.getUserPerceivedPercentageTaskCompleted() != null) {
                throw new ValidationFailedException("Updates to certain fields are not allowed when the Work Item is in Completed status");
            }
        }

        // If the task is transitioning to Completed, we need to check if there are any validations
    }


    /**
     * returns full name of the user derived from the user account object
     */
    private String getFullNameOfAccount(UserAccount account) {
        String fullName = "";
        if (account != null) {
            User user = account.getFkUserId();
            fullName = user.getFirstName() +
                    (user.getMiddleName() != null ? " " + user.getMiddleName() + " " : " ") +
                    (user.getLastName() != null ? user.getLastName() : "");
        }
        return fullName;
    }


    /**
     * method to update the task after it has been validated
     */
    public PersonalTaskResponse updatePersonalTask(PersonalTaskUpdateRequest request, String timeZone) {
        PersonalTask task = personalTaskRepository.findById(request.getPersonalTaskId())
                .orElseThrow(() -> new EntityNotFoundException("Personal Task not found"));

        PersonalTask taskToUpdate = new PersonalTask();
        BeanUtils.copyProperties(task, taskToUpdate);

        List<String> statAffectingFields = Arrays.asList("taskEstimate", "userPerceivedPercentageTaskCompleted", "taskExpEndDate", "fkWorkflowTaskStatus");

        // Update only non-null fields from the request
        List<String> updatedFields = CommonUtils.getNonNullFieldNames(request);
        updatedFields.remove("personalTaskId");
        CommonUtils.copyNonNullProperties(request, taskToUpdate);

        if (updatedFields.contains("workflowStatus")) {
            WorkFlowTaskStatus workFlowTaskStatus = workFlowTaskStatusRepository.findByWorkflowTaskStatusAndFkWorkFlowTypeWorkflowTypeId(request.getWorkflowStatus(), Constants.DEFAULT_WORKFLOW_TYPE_PERSONAL_TASK);
            taskToUpdate.setFkWorkflowTaskStatus(workFlowTaskStatus);
        }

        // Apply common modifications and validations
        modifyPersonalTaskPriority(taskToUpdate);
        setPersonalTaskRequestStateByWorkflowTaskStatus(taskToUpdate);
        if (CommonUtils.containsAny(statAffectingFields, updatedFields)) {
            computeStats(taskToUpdate);
        }

        // update recorded effort, earned time and timesheet
        if (request.getNewEffortTracks() != null && !request.getNewEffortTracks().isEmpty()) {
            updateRecordedEffortEarnedTimeAndTimeSheet(request, taskToUpdate, timeZone);
        }

        // Save the updated task, update notes and audit
        PersonalTask updatedTask = personalTaskRepository.save(taskToUpdate);

        List<PersonalNote> updatedNotes = new ArrayList<>();
        if (request.getNoteRequestList() != null && !request.getNoteRequestList().isEmpty()) {
            updatedNotes = noteService.addUpdateNotesOnUpdatePersonalTask(request.getNoteRequestList(), updatedTask);
        }

//        entityManager.refresh(updatedTask);
        auditService.createAuditForUpdatePersonalTask(updatedTask, String.join(", ", updatedFields));

        return createPersonalTaskResponse(updatedTask, timeZone, updatedNotes);
    }


    // *********************** View Task *********************************


    /**
     * return the custom task response for the given taskId for viewing
     */
    public PersonalTaskResponse getPersonalTask(Long personalTaskIdentifier, String timeZone, Long updatedByAccountId) {

        PersonalTask task = personalTaskRepository.findByPersonalTaskIdentifierAndFkAccountIdAccountId(personalTaskIdentifier, updatedByAccountId);

        if (task == null) throw new EntityNotFoundException("Work Item not found");

        if (!Objects.equals(task.getFkAccountId().getAccountId(), updatedByAccountId))
            throw new ValidationFailedException("You're not authorized to view this Work Item");

        return createPersonalTaskResponse(task, timeZone, null);
    }


    // ********************* Attachment Methods ************************


    /**
     * this method is used to save attachment files in a personal task
     */
    public HashMap<String, Object> saveFiles(List<MultipartFile> files, Long personalTaskId, Long uploaderAccountId) throws IOException {

        List<PersonalAttachment> taskAttachmentsToSave = new ArrayList<>();
        List<UploadAttachmentResponse> taskAttachmentNotSavedDb = new ArrayList<>();
        List<UploadAttachmentResponse> taskAttachmentDuplicateNotSaved = new ArrayList<>();
        List<PersonalFileMetadata> allTaskAttachmentsFoundWithFileStatusA = personalAttachmentRepository.findFileMetadataByPersonalTaskIdAndFileStatus(personalTaskId, com.tse.core_application.constants.Constants.FileAttachmentStatus.A);
        List<String> allTaskAttachmentsNamesFoundWithFileStatusA = allTaskAttachmentsFoundWithFileStatusA.stream().map(PersonalFileMetadata::getFileName).collect(Collectors.toList());


        for (MultipartFile file : files) {
            ScanResult scanResult = ComponentUtils.scanFile(file);
            if (!scanResult.getStatus().equals("PASSED")) {
                throw new ValidationFailedException("File scan failed for: " + file.getOriginalFilename() + ".The file might be infected or corrupted.");
            }

            if (file.getOriginalFilename() != null) {
                String filename = StringUtils.cleanPath(file.getOriginalFilename());
                if (!FileUtils.isFilenameValidated(filename) || !FileUtils.isFileExtensionValidated(filename)) {
                    taskAttachmentNotSavedDb.add(new UploadAttachmentResponse(filename, (double) file.getSize()));
                } else {
                    if (allTaskAttachmentsNamesFoundWithFileStatusA.contains(filename)) {
                        taskAttachmentDuplicateNotSaved.add(new UploadAttachmentResponse(filename, (double) file.getSize()));
                    } else {
                        PersonalAttachment taskAttachment = new PersonalAttachment();
                        taskAttachment.setPersonalTaskId(personalTaskId);
                        taskAttachment.setFileContent(file.getBytes());
                        taskAttachment.setFileName(filename);
                        taskAttachment.setFileSize((double) file.getSize());
                        taskAttachment.setFileStatus(com.tse.core_application.constants.Constants.FileAttachmentStatus.A);
                        taskAttachment.setFileType(file.getContentType());
                        taskAttachment.setAccountId(uploaderAccountId);
                        taskAttachmentsToSave.add(taskAttachment);
                    }
                }
            } else {
                throw new FileNameException();
            }
        }

        List<PersonalAttachment> taskAttachmentsSavedDb = personalAttachmentRepository.saveAll(taskAttachmentsToSave);
        ArrayList<UploadAttachmentResponse> savedDbAttachmentsResponse = new ArrayList<>();

        for (PersonalAttachment taskAttachment : taskAttachmentsSavedDb) {
            UploadAttachmentResponse attachmentResponse = new UploadAttachmentResponse();
            attachmentResponse.setFileSize(taskAttachment.getFileSize());
            attachmentResponse.setFileFullName(taskAttachment.getFileName());
            savedDbAttachmentsResponse.add(attachmentResponse);
        }

        List<PersonalFileMetadata> allActiveTaskAttachmentsFound = personalAttachmentRepository.findFileMetadataByPersonalTaskIdAndFileStatus(personalTaskId, com.tse.core_application.constants.Constants.FileAttachmentStatus.A);

        ObjectMapper objectMapper = new ObjectMapper();
        personalTaskRepository.setTaskAttachmentsByTaskId(objectMapper.writeValueAsString(allActiveTaskAttachmentsFound), personalTaskId);
        HashMap<String, Object> objectHashMap = new HashMap<>();
        objectHashMap.put("success", savedDbAttachmentsResponse);
        objectHashMap.put("fail", taskAttachmentNotSavedDb);
        objectHashMap.put("duplicate", taskAttachmentDuplicateNotSaved);
        return objectHashMap;
    }


    /**
     * This is the method which finds the attachment for the given task. The attachment will only be found for the
     * task, if the file status is "A" for the given task.
     */
    public DownloadAttachmentResponse getTaskAttachmentByTaskIDAndFileNameAndFileStatus(Long taskId, String fileName, Character fileStatus) {

        Optional<PersonalAttachment> personalAttachment = personalAttachmentRepository.findByPersonalTaskIdAndFileNameAndFileStatus(taskId, fileName, fileStatus);
//        .orElseThrow(() -> new EntityNotFoundException("Attachment not found"));
        if (personalAttachment.isPresent()) {
            return new DownloadAttachmentResponse(personalAttachment.get().getFileName(), personalAttachment.get().getFileContent());
        } else {
            throw new FileNotFoundException(fileName);
        }
    }


    /**
     * validate access to view or update attachment
     */
    public void validateAccess(Long personalTaskId, Long updatedByAccountId) {
        PersonalTask task = personalTaskRepository.findById(personalTaskId).orElseThrow(() -> new EntityNotFoundException("Task not found"));

        if (!Objects.equals(task.getFkAccountId().getAccountId(), updatedByAccountId)) {
            throw new ValidationFailedException("You don't have access for this action");
        }
    }


    /**
     * method to delete all or single personal task attachment
     */
    public String deleteAttachment(Long personalTaskId, String fileName, String optionIndicator, Long removerAccountId) throws IOException {
        String message = null;
        ObjectMapper objectMapper = new ObjectMapper();

        if (optionIndicator != null) {
            if (optionIndicator.equalsIgnoreCase(com.tse.core_application.constants.Constants.FileAttachmentOptionIndicator.OPTION_INDICATOR_ALL) && fileName.isEmpty()) {

                personalAttachmentRepository.updateAllPersonalTaskAttachmentsStatusByTaskId(personalTaskId, removerAccountId, com.tse.core_application.constants.Constants.FileAttachmentStatus.D);
                personalTaskRepository.setTaskAttachmentsByTaskId(null, personalTaskId);
                message = "success";
            } else if (optionIndicator.equalsIgnoreCase(com.tse.core_application.constants.Constants.FileAttachmentOptionIndicator.OPTION_INDICATOR_SINGLE) && fileName != null && !fileName.isEmpty()) {

                personalAttachmentRepository.updatePersonalTaskAttachmentStatusByTaskIAndFileName(personalTaskId, fileName, removerAccountId, com.tse.core_application.constants.Constants.FileAttachmentStatus.D);

                List<PersonalFileMetadata> allActiveTaskAttachmentsFound = personalAttachmentRepository.findFileMetadataByPersonalTaskIdAndFileStatus(personalTaskId, com.tse.core_application.constants.Constants.FileAttachmentStatus.A);
                personalTaskRepository.setTaskAttachmentsByTaskId(objectMapper.writeValueAsString(allActiveTaskAttachmentsFound), personalTaskId);
                message = "success";
            } else {
                throw new FileNameException(fileName, optionIndicator);
            }
        } else {
            message = "Invalid Option Indicator";
        }

        return message;
    }


    // ********************* Duplicate Task, Recurring Task ******************************


    /**
     * method is to create a duplicate task response from the given task
     */
    public DuplicatePersonalTaskResponse createDuplicatePersonalTask(Long personalTaskId, String timeZone) {
        DuplicatePersonalTaskResponse response = new DuplicatePersonalTaskResponse();
        PersonalTask taskFoundDb = personalTaskRepository.findById(personalTaskId).orElseThrow(() -> new EntityNotFoundException("Task not found"));
        BeanUtils.copyProperties(taskFoundDb, response);
        response.setWorkflowTaskStatus(taskFoundDb.getFkWorkflowTaskStatus().getWorkflowTaskStatus());
        if (taskFoundDb.getTaskExpStartDate() != null) {
            response.setTaskExpStartDate(DateTimeUtils.convertServerDateToUserTimezone(taskFoundDb.getTaskExpStartDate(), timeZone));
        }
        if (taskFoundDb.getTaskExpEndDate() != null) {
            response.setTaskExpEndDate(DateTimeUtils.convertServerDateToUserTimezone(taskFoundDb.getTaskExpEndDate(), timeZone));
        }
        return response;
    }


    /**
     * method to create recurring tasks based on the recurrence schedule request
     */
    public String createPersonalRecurringTasks(RecurrencePersonalTaskDTO request, Long accountIdOfUser, String timeZone) {
        validateRecurrenceRequest(request, timeZone);
        List<LocalDate[]> generatedExpectedDates = recurrenceService.generateRecurringDates(request.getRecurrenceSchedule(), accountIdOfUser);

        StringBuilder msgToReturn = new StringBuilder("The following tasks are created successfully: ");
        StringBuilder createdTasksString = new StringBuilder();
        if (generatedExpectedDates.isEmpty())
            return "No Dates generated for the given filters";

        for (LocalDate[] expectedDates : generatedExpectedDates) {
            PersonalTask task = populateRecurringTask(request, accountIdOfUser, expectedDates[0], expectedDates[1], timeZone);
            initializeTaskNumberSetProperties(task);
            PersonalTask taskAdd = saveTask(task);
            createdTasksString.append(", ");
            createdTasksString.append(taskAdd.getPersonalTaskNumber());
        }

        msgToReturn.append(createdTasksString);
        return msgToReturn.toString();
    }


    /**
     * method to validate recurrence request
     */
    private void validateRecurrenceRequest(RecurrencePersonalTaskDTO request, String timeZone) {
        // only allowed values for workflow status is not started and 'Due Date Not Provided'
        if (!Objects.equals(request.getTaskWorkFlowStatus(), STATUS_DUE_DATE_NOT_PROVIDED)
                && !Objects.equals(request.getTaskWorkFlowStatus(), STATUS_NOT_STARTED_TITLE_CASE)) {
            throw new IllegalArgumentException("Only allowed values for workflow status are 'Due Date Not Provided' and 'Not-Started'");
        }

        // Validate either startDate & endDate are provided or numberOfOccurrences, but not both
        boolean isDateRangeProvided = request.getRecurrenceSchedule().getStartDate() != null && request.getRecurrenceSchedule().getEndDate() != null;
        boolean isNumberOfOccurrencesProvided = request.getRecurrenceSchedule().getNumberOfOccurrences() != null;

        if (isDateRangeProvided == isNumberOfOccurrencesProvided) {
            throw new ValidationFailedException("Either 'start date and end date' or 'number of occurrences' must be provided, but not both");
        }

        // Validate selectedDate or startDate is greater than the current date
        LocalDate relevantDate = request.getRecurrenceSchedule().getSelectedDate() != null ? request.getRecurrenceSchedule().getSelectedDate() : request.getRecurrenceSchedule().getStartDate();
        if (relevantDate != null && !relevantDate.isAfter(DateTimeUtils.convertServerDateToUserTimezone(LocalDateTime.now().minusDays(1), timeZone).toLocalDate())) {
            throw new ValidationFailedException("Selected date or Start date must be greater than the current date");
        }

        // Additional validation for recurDays based on recurrenceType
        if (request.getRecurrenceSchedule().getRecurDays() != null && !request.getRecurrenceSchedule().getRecurDays().isEmpty()) {
            switch (request.getRecurrenceSchedule().getRecurrenceType()) {
                case WEEKLY:
                    RecurrenceScheduleDTO.validateWeeklyRecurDays(request.getRecurrenceSchedule().getRecurDays());
                    break;
                case MONTHLY:
                    RecurrenceScheduleDTO.validateMonthlyRecurDays(request.getRecurrenceSchedule().getRecurDays());
                    break;
                case YEARLY:
                    RecurrenceScheduleDTO.validateYearlyRecurDays(request.getRecurrenceSchedule().getRecurDays());
                    break;
            }
        }
    }


    /**
     * populate a Personal Task object from the recurrence request and expected dates
     */
    private PersonalTask populateRecurringTask(RecurrencePersonalTaskDTO recurringTaskRequest, Long accountIdOfUser, LocalDate taskExpStartDate, LocalDate taskExpEndDate, String timeZone) {
        PersonalTask task = new PersonalTask();
        task.setTaskTypeId(Constants.TaskTypes.TASK);

        task.setTaskTitle(recurringTaskRequest.getTaskTitle());
        task.setTaskDesc(recurringTaskRequest.getTaskDesc());

        if (recurringTaskRequest.getTaskPriority() != null)
            task.setTaskPriority(recurringTaskRequest.getTaskPriority().substring(0, 2));

        task.setTaskWorkflowId(Constants.DEFAULT_WORKFLOW_TYPE_PERSONAL_TASK);
        task.setFkWorkflowTaskStatus(workFlowTaskStatusRepository.findByWorkflowTaskStatusAndFkWorkFlowTypeWorkflowTypeId(recurringTaskRequest.getTaskWorkFlowStatus(), Constants.DEFAULT_WORKFLOW_TYPE_PERSONAL_TASK));

        LocalDateTime taskExpStartDateTime = DateTimeUtils.convertUserDateToServerTimezone(
                LocalDateTime.of(taskExpStartDate, recurringTaskRequest.getTaskExpStartTime()), timeZone);
        LocalDateTime taskExpEndDateTime = DateTimeUtils.convertUserDateToServerTimezone(
                LocalDateTime.of(taskExpEndDate, recurringTaskRequest.getTaskExpEndTime()), timeZone);
        task.setTaskExpStartDate(taskExpStartDateTime);
        task.setTaskExpEndDate(taskExpEndDateTime);

        task.setFkAccountId(userAccountRepository.findByAccountId(accountIdOfUser));
        task.setTaskEstimate(recurringTaskRequest.getTaskEstimate());
        task.setCurrentActivityIndicator(false);
        task.setCurrentlyScheduledTaskIndicator(false);
        return task;
    }


    // ****************************** Search Task ******************************************


    /**
     * Searches for tasks by Full Text Search (FTS) and similarity,  It prioritizes results based on FTS relevance and
     * then by trigram similarity score for the specified search term. Based on the search task request, it combines
     * all filters to create a native query.
     */
    public List<SearchTaskResponse> searchTasksByFTSAndTrigram(UserAccount accountOfUser, SearchTaskRequest request) {

        List<SearchTaskResponse> responseList = new ArrayList<>();
        if ((request.getOrgId() != null && !Objects.equals(request.getOrgId(), accountOfUser.getOrgId())) || request.getBuId() != null ||
                request.getProjectId() != null || (request.getAccountIdAssigned() != null && !Objects.equals(request.getAccountIdAssigned(), accountOfUser.getAccountId())) ||
                request.getSprintId() != null || request.getTeamId() != null) {
            return responseList;
        }

        String nativeQuery = "SELECT * FROM tse.personal_task " +
                "WHERE (search_vector @@ plainto_tsquery(:searchTerm) OR SIMILARITY(task_title, :searchTerm) > 0.2) " +
                "AND account_id = :accountIdOfUser ";

        nativeQuery = applyFiltersToSearchQuery(request, nativeQuery);

        nativeQuery += "ORDER BY " +
                "CASE WHEN search_vector @@ plainto_tsquery(:searchTerm) THEN " +
                "ts_rank(search_vector, plainto_tsquery(:searchTerm)) ELSE 0 END DESC, " +
                "SIMILARITY(task_title, :searchTerm) DESC";

        Query query = entityManager.createNativeQuery(nativeQuery, PersonalTask.class);
        setParametersInSearchQuery(request, query, accountOfUser);
        List<PersonalTask> tasks = query.getResultList();

        return createSearchTaskResponse(tasks, accountOfUser);
    }


    /**
     * based on the search request, this method appends filter criteria string to the native query
     */
    private String applyFiltersToSearchQuery(SearchTaskRequest request, String nativeQuery) {
        List<Integer> workflowStatusIds = new ArrayList<>();

        if (request.getOrgId() != null) {
            nativeQuery = nativeQuery + "AND org_id = :orgId ";
        }

        if (request.getWorkflowStatuses() != null && !request.getWorkflowStatuses().isEmpty()) {
            List<String> workflowStatuses = request.getWorkflowStatuses();
            workflowStatusIds = workFlowTaskStatusRepository.findWorkflowTaskStatusIdByWorkflowTaskStatusIn(workflowStatuses);
            request.setWorkflowStatusIds(workflowStatusIds);
            nativeQuery += "AND workflow_task_status_id IN (:workflowStatusIds) ";
        }

        if (request.getTaskProgressSystems() != null && !request.getTaskProgressSystems().isEmpty()) {
            nativeQuery += "AND task_progress_system IN (:taskProgressSystems) ";
        }

        if (request.getTaskNumbersToSkip() != null && !request.getTaskNumbersToSkip().isEmpty()) {
            nativeQuery += "AND personal_task_number NOT IN (:taskNumbersToSkip) ";
        }

        if (request.getCurrentlyScheduledTaskIndicator() != null) {
            nativeQuery += "AND currently_scheduled_task_indicator = :currentlyScheduledTaskIndicator ";
        }

        if (request.getCurrentActivityIndicator() != null) {
            nativeQuery += "AND current_activity_indicator = :currentActivityIndicator ";
        }
        return nativeQuery;
    }


    /**
     * This method sets the required parameters in the native query
     */
    private void setParametersInSearchQuery(SearchTaskRequest request, Query query, UserAccount accountOfUser) {
        query.setParameter("searchTerm", request.getSearchTerm());
        query.setParameter("accountIdOfUser", accountOfUser.getAccountId());

        if (request.getOrgId() != null) {
            query.setParameter("orgId", request.getOrgId());
        }
        if (request.getWorkflowStatuses() != null && !request.getWorkflowStatuses().isEmpty()) {
            query.setParameter("workflowStatusIds", request.getWorkflowStatusIds());
        }
        if (request.getTaskProgressSystems() != null && !request.getTaskProgressSystems().isEmpty()) {
            query.setParameter("taskProgressSystems", request.getTaskProgressSystems());
        }
        if (request.getTaskNumbersToSkip() != null && !request.getTaskNumbersToSkip().isEmpty()) {
            query.setParameter("taskNumbersToSkip", request.getTaskNumbersToSkip());
        }
        if (request.getCurrentlyScheduledTaskIndicator() != null) {
            query.setParameter("currentlyScheduledTaskIndicator", request.getCurrentlyScheduledTaskIndicator());
        }
        if (request.getCurrentActivityIndicator() != null) {
            query.setParameter("currentActivityIndicator", request.getCurrentActivityIndicator());
        }
    }


    /**
     * create a search Task Response from a list of tasks
     */
    private List<SearchTaskResponse> createSearchTaskResponse(List<PersonalTask> tasks, UserAccount accountOfUser) {
        List<SearchTaskResponse> responseList = new ArrayList<>();
        for (PersonalTask task : tasks) {
            String fullName = accountOfUser.getFkUserId().getFirstName() + " " + accountOfUser.getFkUserId().getLastName();
            String email = accountOfUser.getEmail();

            SearchTaskResponse searchTaskResponse = new SearchTaskResponse();
            BeanUtils.copyProperties(task, searchTaskResponse);
            searchTaskResponse.setTaskId(task.getPersonalTaskId());
            searchTaskResponse.setTaskNumber(task.getPersonalTaskNumber());
            searchTaskResponse.setWorkflowTaskStatus(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus());
            searchTaskResponse.setEmail(email);
            searchTaskResponse.setFullName(fullName);
            searchTaskResponse.setIsPersonalTask(true);

            responseList.add(searchTaskResponse);
        }
        return responseList;
    }


    // ******************************** Task Master, Get Stats ******************************************

    /**
     * get filtered tasks for task master based on the filters in the stats request
     */
    public List<TaskMaster> getAllFilteredTaskForPersonalUser(StatsRequest statsRequest, String accountIds, String timeZone) {

        List<TaskMaster> taskMasters = new ArrayList<>();
        List<Long> accountIdsRequester = CommonUtils.convertToLongList(accountIds);
        UserAccount personalUserAccount = userAccountRepository.findByAccountIdInAndOrgIdAndIsActive(accountIdsRequester, personalOrgId, true);
        if (personalUserAccount == null) return taskMasters;

        boolean isPersonalOrgSelected = statsRequest.getOrgIds() == null || (!statsRequest.getOrgIds().isEmpty() && statsRequest.getOrgIds().contains(personalOrgId));
        boolean isRequesterOwnPersonalAccountSelected = (statsRequest.getAccountIds() != null && !statsRequest.getAccountIds().isEmpty() && statsRequest.getAccountIds().contains(personalUserAccount.getAccountId())) ||
                Objects.equals(statsRequest.getAccountIdAssigned(), personalUserAccount.getAccountId()) ||
                Objects.equals(statsRequest.getUserId(), personalUserAccount.getFkUserId().getUserId());
        boolean isOtherUserPersonalAccountSelected = (statsRequest.getAccountIds() != null && !statsRequest.getAccountIds().isEmpty() && !statsRequest.getAccountIds().contains(personalUserAccount.getAccountId())) ||
                (statsRequest.getAccountIdAssigned() != null && !Objects.equals(statsRequest.getAccountIdAssigned(), personalUserAccount.getAccountId())) ||
                (statsRequest.getUserId() != null && !Objects.equals(statsRequest.getUserId(), personalUserAccount.getFkUserId().getUserId()));
        boolean isTeamIdZero = Objects.equals(statsRequest.getTeamId(), PERSONAL_TEAM_ID);

        if (isOtherUserPersonalAccountSelected || !(isPersonalOrgSelected && isRequesterOwnPersonalAccountSelected) || !isTeamIdZero) {
            return taskMasters;
        } else {
            statsService.setDefaultFromAndToDateToStatsRequest(statsRequest, timeZone);
            Specification<PersonalTask> spec = personalTaskSpecification(statsRequest, personalUserAccount);
            List<PersonalTask> personalTasksFound = personalTaskRepository.findAll(spec);
            return createTaskMasterResponse(personalTasksFound, timeZone);
        }
    }


    public List<TaskMaster> getPersonalTaskDetailsForGivenStatus(StatsRequest statsRequest, String timeZone, UserAccount personalUserAccount) {
        List<TaskMaster> taskMasters = new ArrayList<>();
        statsRequest.setAccountIdAssigned(personalUserAccount.getAccountId());

        boolean isOtherUserPersonalAccountSelected = (statsRequest.getAccountIds() != null && !statsRequest.getAccountIds().isEmpty() && !statsRequest.getAccountIds().contains(personalUserAccount.getAccountId())) ||
                (statsRequest.getAccountIdAssigned() != null && !Objects.equals(statsRequest.getAccountIdAssigned(), personalUserAccount.getAccountId())) ||
                (statsRequest.getUserId() != null && !Objects.equals(statsRequest.getUserId(), personalUserAccount.getFkUserId().getUserId()));

        if (isOtherUserPersonalAccountSelected) {
            return taskMasters;
        } else {
            Specification<PersonalTask> spec = personalTaskSpecification(statsRequest, personalUserAccount);
            List<PersonalTask> personalTasksFound = personalTaskRepository.findAll(spec);
            return createTaskMasterResponse(personalTasksFound, timeZone);
        }
    }


    /**
     * method to build specification for searching the personal task based on the stats request for task master
     */
    public Specification<PersonalTask> personalTaskSpecification(StatsRequest statsRequest, UserAccount personalUserAccount) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Example: Filter by account ID
            if (statsRequest.getAccountIds() != null && !statsRequest.getAccountIds().isEmpty() && statsRequest.getAccountIds().contains(personalUserAccount.getAccountId())) {
                predicates.add(criteriaBuilder.equal(root.get("fkAccountId").get("accountId"), personalUserAccount.getAccountId()));
            } else if (statsRequest.getUserId() != null && statsRequest.getUserId().equals(personalUserAccount.getFkUserId().getUserId())) {
                predicates.add(criteriaBuilder.equal(root.get("fkAccountId").get("accountId"), personalUserAccount.getAccountId()));
            } else if (statsRequest.getAccountIdAssigned() != null && Objects.equals(statsRequest.getAccountIdAssigned(), personalUserAccount.getAccountId())) {
                predicates.add(criteriaBuilder.equal(root.get("fkAccountId").get("accountId"), personalUserAccount.getAccountId()));
            }

            // Filter by task priority
            if (statsRequest.getTaskPriority() != null && !statsRequest.getTaskPriority().isEmpty()) {
                List<String> taskPriorityList = statsRequest.getTaskPriority().stream()
                        .map(TaskPriority::name)
                        .collect(Collectors.toList());
                predicates.add(root.get("taskPriority").in(taskPriorityList));
            }

            // Example: Filter by currentlyScheduledTaskIndicator
            if (statsRequest.getCurrentlyScheduledTaskIndicator() != null) {
                predicates.add(criteriaBuilder.equal(root.get("currentlyScheduledTaskIndicator"), statsRequest.getCurrentlyScheduledTaskIndicator()));
            }

            // Example: Filter by taskWorkflowId
            if (statsRequest.getTaskWorkflowId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("taskWorkflowId"), statsRequest.getTaskWorkflowId()));
            }

            // Example: Filter by taskProgressSystem
            if (statsRequest.getStatName() != null && !statsRequest.getStatName().isEmpty()) {
                predicates.add(root.get("taskProgressSystem").in(statsRequest.getStatName()));
            }

            // Date range filters
            Path<LocalDateTime> datePath = root.get(statsRequest.getFromDateType());
            if (statsRequest.getFromDate() != null) {
                Predicate datePredicate = statsRequest.isFirstTypeLessThan() ?
                        criteriaBuilder.lessThan(datePath, statsRequest.getFromDate()) :
                        criteriaBuilder.greaterThanOrEqualTo(datePath, statsRequest.getFromDate());
                predicates.add(datePredicate);
            }

            if (statsRequest.getToDate() != null) {
                Predicate datePredicate = statsRequest.isSecondTypeLessThan() ?
                        criteriaBuilder.lessThan(datePath, statsRequest.getToDate()) :
                        criteriaBuilder.greaterThanOrEqualTo(datePath, statsRequest.getToDate());
                predicates.add(datePredicate);
            }

            // NoOfDays filter
            if (statsRequest.getNoOfDays() != null) {
                LocalDateTime newEndDate = LocalDateTime.now().minusDays(statsRequest.getNoOfDays());
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("taskExpEndDate"), newEndDate));
            }

            // Workflow status filter
            if (statsRequest.getWorkflowTaskStatus() != null && !statsRequest.getWorkflowTaskStatus().isEmpty()) {
                List<Integer> workflowStatusIds = statsRequest.getWorkflowTaskStatus().stream()
                        .flatMap(status -> workflowTaskStatusService.getAllWorkflowTaskStatusByWorkflowTaskStatus(status).stream())
                        .map(WorkFlowTaskStatus::getWorkflowTaskStatusId)
                        .collect(Collectors.toList());

                if (!workflowStatusIds.isEmpty()) {
                    predicates.add(root.get("fkWorkflowTaskStatus").get("workflowTaskStatusId").in(workflowStatusIds));
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }


    /**
     * converts filtered tasks to task master response
     */
    private List<TaskMaster> createTaskMasterResponse(List<PersonalTask> filteredTasks, String timeZone) {
        List<TaskMaster> taskMasters = new ArrayList<>();
        if (!filteredTasks.isEmpty()) {
            for (PersonalTask task : filteredTasks) {
                TaskMaster taskMaster = new TaskMaster();
                // filtration based on effort date -- if there is an effortDate provided and there is an effort on any task on that date then we include it. If there is no
                // effort on that date but the date is today's date and current activity indicator for any task is on then we include it
//                if (statsRequest.getNewEffortDate() != null) {
//                    if (timeSheetRepository.existsByEntityTypeIdAndEntityIdAndNewEffortDate(Constants.EntityTypes.TASK, task.getTaskId(), statsRequest.getNewEffortDate())) {
//                        taskMaster.setNewEffortDate(statsRequest.getNewEffortDate());
//                    } else if (isEffortDateOfToday && task.getCurrentActivityIndicator() == 1) {
//                        taskMaster.setNewEffortDate(null);
//                    } else {
//                        continue;
//                    }
//                }
                // ToDo: we will need another type for entry in the timesheet

                convertTaskAllServerDateAndTimeInToLocalTimeZone(task, timeZone);
                BeanUtils.copyProperties(task, taskMaster);
                taskMaster.setTaskId(task.getPersonalTaskId());
                taskMaster.setTaskNumber(task.getPersonalTaskNumber());
                taskMaster.setTaskWorkflowType(task.getFkWorkflowTaskStatus().getFkWorkFlowType().getWorkflowName());
                taskMaster.setWorkflowTaskStatusType(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus());
                taskMaster.setOrgId(personalOrgId);
                taskMaster.setIsPersonalTask(true);
                taskMaster.setTeamId(PERSONAL_TEAM_ID);
                taskMaster.setTeamName(com.tse.core_application.model.Constants.PERSONAL_ORG_TEAM_DISPLAY_NAME_FOR_MULTIPLE_TEAMS);
                taskMaster.setTaskTypeId(task.getTaskTypeId());
                taskMaster.setTaskType(Constants.taskTypeMap.get(task.getTaskTypeId()));
                if (task.getFkAccountId() != null) {
                    String fullName = task.getFkAccountId().getFkUserId().getFirstName() + " " + task.getFkAccountId().getFkUserId().getLastName();
                    String email = task.getFkAccountId().getEmail();
                    taskMaster.setEmail(email);
                    taskMaster.setFullName(fullName);
                }
                boolean isTaskMasterAdded = taskMasters.add(taskMaster);
            }
        }
        return taskMasters;
    }


    // ********************************** Date And Time Conversion ************************************************


    /**
     * this method will convert all the server date and server time of a task into the corresponding date and
     * time according to the local timeZone.
     */
    public void convertTaskAllServerDateAndTimeInToLocalTimeZone(PersonalTask task, String localTimeZone) {
        if (task != null) {
            if (task.getTaskExpStartDate() != null) {
                LocalDateTime convertedDate = DateTimeUtils.convertServerDateToUserTimezone(task.getTaskExpStartDate(), localTimeZone);
                task.setTaskExpStartDate(convertedDate);
            }
            if (task.getTaskExpEndDate() != null) {
                LocalDateTime convertedDate = DateTimeUtils.convertServerDateToUserTimezone(task.getTaskExpEndDate(), localTimeZone);
                task.setTaskExpEndDate(convertedDate);
            }
            if (task.getTaskActStDate() != null) {
                LocalDateTime convertedDate = DateTimeUtils.convertServerDateToUserTimezone(task.getTaskActStDate(), localTimeZone);
                task.setTaskActStDate(convertedDate);
            }
            if (task.getTaskActEndDate() != null) {
                LocalDateTime convertedDate = DateTimeUtils.convertServerDateToUserTimezone(task.getTaskActEndDate(), localTimeZone);
                task.setTaskActEndDate(convertedDate);
            }
            if (task.getCreatedDateTime() != null) {
                LocalDateTime convertedDate = DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(task.getCreatedDateTime(), localTimeZone);
                task.setCreatedDateTime(convertedDate);
            }
            if (task.getLastUpdatedDateTime() != null) {
                LocalDateTime convertedDate = DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(task.getLastUpdatedDateTime(), localTimeZone);
                task.setLastUpdatedDateTime(convertedDate);
            }
            if (task.getNotes() != null) {
                convertNotesServerDateAndTimeToLocalTimeZone(task, localTimeZone);
            }

        }
    }


    /**
     * method to convert date and time in notes in personal task from server date time to local date time
     */
    private void convertNotesServerDateAndTimeToLocalTimeZone(PersonalTask task, String localTimeZone) {
        for (int note = 0; note < task.getNotes().size(); note++) {
            if (task.getNotes().get(note).getCreatedDateTime() != null) {
                LocalDateTime convertedCreatedDateTime = DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(task.getNotes().get(note).getCreatedDateTime(), localTimeZone);
                task.getNotes().get(note).setCreatedDateTime(convertedCreatedDateTime);
            }
            if (task.getNotes().get(note).getLastUpdatedDateTime() != null) {
                LocalDateTime convertedLastUpdatedDateTime = DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(task.getNotes().get(note).getLastUpdatedDateTime(), localTimeZone);
                task.getNotes().get(note).setLastUpdatedDateTime(convertedLastUpdatedDateTime);
            }
        }
    }


    /**
     * method to convert all dates in personal task request to server time zone
     */
    public void convertPersonalTaskAllLocalDateAndTimeToServerTimeZone(PersonalTask task, String localTimeZone) {
        if (task != null) {
            if (task.getTaskExpStartDate() != null) {
                LocalDateTime convertedDate = DateTimeUtils.convertUserDateToServerTimezone(task.getTaskExpStartDate(), localTimeZone);
                task.setTaskExpStartDate(convertedDate);
            }
            if (task.getTaskExpEndDate() != null) {
                LocalDateTime convertedDate = DateTimeUtils.convertUserDateToServerTimezone(task.getTaskExpEndDate(), localTimeZone);
                task.setTaskExpEndDate(convertedDate);
            }
            if (task.getTaskActStDate() != null) {
                LocalDateTime convertedDate = DateTimeUtils.convertUserDateToServerTimezone(task.getTaskActStDate(), localTimeZone);
                task.setTaskActStDate(convertedDate);
            }
            if (task.getTaskActEndDate() != null) {
                LocalDateTime convertedDate = DateTimeUtils.convertUserDateToServerTimezone(task.getTaskActEndDate(), localTimeZone);
                task.setTaskActEndDate(convertedDate);
            }
            if (task.getCreatedDateTime() != null) {
                LocalDateTime convertedDate = DateTimeUtils.convertUserDateToServerTimezoneWithSeconds(task.getCreatedDateTime(), localTimeZone);
                task.setCreatedDateTime(convertedDate);
            }
            if (task.getLastUpdatedDateTime() != null) {
                LocalDateTime convertedDate = DateTimeUtils.convertUserDateToServerTimezoneWithSeconds(task.getLastUpdatedDateTime(), localTimeZone);
                task.setLastUpdatedDateTime(convertedDate);
            }

            if (task.getNotes() != null) {
                convertNotesLocalDateAndTimeToServerTimeZone(task, localTimeZone);
            }
        }
    }


    /**
     * method to convert date and time in notes in personal task from local date time to server date time
     */
    private void convertNotesLocalDateAndTimeToServerTimeZone(PersonalTask task, String localTimeZone) {
        for (int note = 0; note < task.getNotes().size(); note++) {
            if (task.getNotes().get(note).getCreatedDateTime() != null) {
                LocalDateTime convertedCreatedDateTime = DateTimeUtils.convertUserDateToServerTimezoneWithSeconds(task.getNotes().get(note).getCreatedDateTime(), localTimeZone);
                task.getNotes().get(note).setCreatedDateTime(convertedCreatedDateTime);
            }
            if (task.getNotes().get(note).getLastUpdatedDateTime() != null) {
                LocalDateTime convertedLastUpdatedDateTime = DateTimeUtils.convertUserDateToServerTimezoneWithSeconds(task.getNotes().get(note).getLastUpdatedDateTime(), localTimeZone);
                task.getNotes().get(note).setLastUpdatedDateTime(convertedLastUpdatedDateTime);
            }
        }
    }


    // *************************************** Scheduled Task *********************************************


    /**
     * get all scheduled tasks (current scheduled indicator is true) for given account Id
     */
    public List<ScheduledTaskViewResponse> getAllScheduledPersonalTasks(UserAccount personalUserAccount) {
        List<ScheduledTaskViewResponse> activeTasks = new ArrayList<>();
        if (personalUserAccount != null) {
            List<PersonalTask> tasks = personalTaskRepository.findByFkAccountIdAccountIdAndCurrentlyScheduledTaskIndicator(personalUserAccount.getAccountId(), true);

            for (PersonalTask task : tasks) {
                ScheduledTaskViewResponse scheduledTaskViewResponse = new ScheduledTaskViewResponse();

                scheduledTaskViewResponse.setAccountIdAssigned(task.getFkAccountId().getAccountId());
                scheduledTaskViewResponse.setTaskNumber(task.getPersonalTaskNumber());
                scheduledTaskViewResponse.setTaskTitle(task.getTaskTitle());
                scheduledTaskViewResponse.setTaskId(task.getPersonalTaskId());
                scheduledTaskViewResponse.setTaskTypeId(task.getTaskTypeId());
                scheduledTaskViewResponse.setTaskDesc(task.getTaskDesc());
                scheduledTaskViewResponse.setCurrentlyScheduledTaskIndicator(task.getCurrentlyScheduledTaskIndicator());
                scheduledTaskViewResponse.setCurrentActivityIndicator(!task.getCurrentActivityIndicator() ? 0 : 1);
                scheduledTaskViewResponse.setTaskWorkflowId(task.getTaskWorkflowId());
                scheduledTaskViewResponse.setWorkflowTaskStatus(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus());
                scheduledTaskViewResponse.setIsPersonalTask(true);

                activeTasks.add(scheduledTaskViewResponse);
            }
        }
        return activeTasks;
    }


    // *************************************** Stats *********************************************

    /**
     * calls the computeAndUpdateStat method and set the updated stats in the task
     */
    private void computeStats(PersonalTask personalTask) {
        // some business logic was present
    }


    /**
     * compute stats for the given personal task
     */
    private HashMap<String, Object> computeAndUpdateStat(PersonalTask personalTask) {
        // some business logic was present

        return null;
    }


    /**
     * Retrieves personal tasks for the dashboard based on the provided stats request, account IDs, and time zone
     */
    private List<PersonalTask> getPersonalTasksForDashboard(StatsRequest statsRequest, String accountIds, String timeZone) {

        List<PersonalTask> responseTaskList = new ArrayList<>();
        List<Long> accountIdsRequester = CommonUtils.convertToLongList(accountIds);
        UserAccount personalUserAccount = userAccountRepository.findByAccountIdInAndOrgIdAndIsActive(accountIdsRequester, personalOrgId, true);
        if (personalUserAccount == null) return responseTaskList;

        boolean isPersonalOrgSelected = statsRequest.getOrgIds() == null || (!statsRequest.getOrgIds().isEmpty() && statsRequest.getOrgIds().contains(personalOrgId));
        boolean isRequesterOwnPersonalAccountSelected = (statsRequest.getAccountIds() != null && !statsRequest.getAccountIds().isEmpty() && statsRequest.getAccountIds().contains(personalUserAccount.getAccountId())) ||
                Objects.equals(statsRequest.getAccountIdAssigned(), personalUserAccount.getAccountId()) ||
                Objects.equals(statsRequest.getUserId(), personalUserAccount.getFkUserId().getUserId());
        boolean isOtherUserPersonalAccountSelected = (statsRequest.getAccountIds() != null && !statsRequest.getAccountIds().isEmpty() && !statsRequest.getAccountIds().contains(personalUserAccount.getAccountId())) ||
                (statsRequest.getAccountIdAssigned() != null && !Objects.equals(statsRequest.getAccountIdAssigned(), personalUserAccount.getAccountId())) ||
                (statsRequest.getUserId() != null && !Objects.equals(statsRequest.getUserId(), personalUserAccount.getFkUserId().getUserId()));

        if (isOtherUserPersonalAccountSelected || !(isPersonalOrgSelected && isRequesterOwnPersonalAccountSelected)) {
            return responseTaskList;
        } else {
            Specification<PersonalTask> spec = personalTaskSpecification(statsRequest, personalUserAccount);
            return personalTaskRepository.findAll(spec);
        }
    }


    /**
     * Calculates stats for personal tasks based on the provided stats request and account IDs,
     * updates the tasks with the updated stats, and returns a map containing the computed statistics.
     */
    public HashMap<String, Integer> calculateAndUpdateStats(StatsRequest statsRequest, String accountIds, String timeZone) {
        HashMap<String, Integer> statsMap = new HashMap<>();
        // some business logic was present

        return statsMap;
    }


    // ************************** Earned Effort & TimeSheet *******************************************


    /** updates the recorded effort, earned time for new efforts provided in a task. Also, create the timesheet entry for each new effort provided*/
    private void updateRecordedEffortEarnedTimeAndTimeSheet(PersonalTaskUpdateRequest request, PersonalTask taskToUpdate, String localTimeZone) {
        if (request.getNewEffortTracks() != null && !request.getNewEffortTracks().isEmpty()) {

            PersonalTask taskDb = personalTaskRepository.findById(taskToUpdate.getPersonalTaskId()).orElseThrow(EntityNotFoundException::new);
            DataEncryptionConverter dataEncryptionConverter = new DataEncryptionConverter();
            List<TimeSheet> tsRecords = new ArrayList<>();

            int newEffortSum = 0, newEarnedTimeSum = 0;
            List<Integer> efforts = new ArrayList<>();
            for (NewEffortTrack newEffortTrack : request.getNewEffortTracks()) {
                newEffortSum += newEffortTrack.getNewEffort();
                efforts.add(newEffortTrack.getNewEffort());
            }

            int userPerceivedPercentageDifference = taskToUpdate.getUserPerceivedPercentageTaskCompleted() -
                    (taskDb.getUserPerceivedPercentageTaskCompleted() != null ? taskDb.getUserPerceivedPercentageTaskCompleted() : 0);

            if (taskToUpdate.getTaskEstimate() != null) {
                // Todo: assuming the estimate is constant --> we need to handle the case when the estimate is changing
                newEarnedTimeSum = taskToUpdate.getTaskEstimate() * userPerceivedPercentageDifference / 100;
            } else {
                newEarnedTimeSum = newEffortSum; // When estimate is not given, earned time is equal to the effort.
            }

            for (int i = 0; i < request.getNewEffortTracks().size(); i++) {
                NewEffortTrack newEffortTrack = request.getNewEffortTracks().get(i);
                validateNewEffortDate(newEffortTrack, taskToUpdate, localTimeZone);

                TimeSheet timeSheet = createTimeSheetRecord(newEffortTrack, taskToUpdate, dataEncryptionConverter, taskDb);

                double effortRatio = (double) efforts.get(i) / newEffortSum;
                if (taskToUpdate.getTaskEstimate() != null) {
                    int proportionalEarnedTime = (int) Math.round(effortRatio * newEarnedTimeSum);
                    timeSheet.setEarnedTime(proportionalEarnedTime);
                } else {
                    timeSheet.setEarnedTime(newEffortTrack.getNewEffort());
                }

                // Set increase in user perceived percentage for each time sheet
                int increaseInUserPerceivedPercentage = (int) Math.round(effortRatio * userPerceivedPercentageDifference);
                timeSheet.setIncreaseInUserPerceivedPercentageTaskCompleted(increaseInUserPerceivedPercentage);

                tsRecords.add(timeSheet);
            }

            Integer totalEarnedTime = taskDb.getEarnedTimeTask() != null ? taskDb.getEarnedTimeTask() + newEarnedTimeSum : newEarnedTimeSum;
            Integer totalEffort = taskDb.getRecordedEffort() == null ? newEffortSum : taskDb.getRecordedEffort() + newEffortSum;

            taskToUpdate.setRecordedEffort(totalEffort);
            taskToUpdate.setEarnedTimeTask(totalEarnedTime);

            timeSheetRepository.saveAll(tsRecords);
        }
    }

    /** validate the dates for the new efforts provided for a task*/
    private void validateNewEffortDate(NewEffortTrack newEffortTrack, PersonalTask taskToUpdate, String localTimeZone) {
        LocalDate dateFromRequest = newEffortTrack.getNewEffortDate();
        LocalDateTime clientDate = DateTimeUtils.convertServerDateToUserTimezone(LocalDateTime.now(), localTimeZone);
        if (dateFromRequest.isAfter(clientDate.toLocalDate())) {
            throw new ValidationFailedException("new effort date can not be greater than today's date");
        }

        // validation: newEffortDate can't be prior to Task actual start date and later than Task actual end date
        LocalDateTime clientActEndDate = null, clientActStDate = null;
        if (taskToUpdate.getTaskActEndDate() != null) {
            clientActEndDate = DateTimeUtils.convertServerDateToUserTimezone(taskToUpdate.getTaskActEndDate(), localTimeZone);
        }
        if (taskToUpdate.getTaskActStDate() != null)
            clientActStDate = DateTimeUtils.convertServerDateToUserTimezone(taskToUpdate.getTaskActStDate(), localTimeZone);
        if ((clientActStDate != null && dateFromRequest.isBefore(clientActStDate.toLocalDate())) || (clientActEndDate != null && dateFromRequest.isAfter(clientActEndDate.toLocalDate()))) {
            throw new ValidationFailedException("new effort date can not be prior to Work Item actual start date or later than Work Item actual end date");
        }

    }


    /** creates a timesheet object for a given new effort in a task*/
    private TimeSheet createTimeSheetRecord(NewEffortTrack newEffortTrack, PersonalTask taskToUpdate, DataEncryptionConverter dataEncryptionConverter, PersonalTask taskDb) {
        TimeSheet timeSheet = new TimeSheet();
        UserAccount userAccount = taskDb.getFkAccountId();

        timeSheet.setNewEffort(newEffortTrack.getNewEffort());
        timeSheet.setNewEffortDate(newEffortTrack.getNewEffortDate());
        timeSheet.setEntityId(taskToUpdate.getPersonalTaskId());
        timeSheet.setEntityNumber(taskToUpdate.getPersonalTaskNumber());
        timeSheet.setEntityTitle(dataEncryptionConverter.convertToDatabaseColumn(taskToUpdate.getTaskTitle()));
        timeSheet.setEntityTypeId(Constants.EntityTypes.TASK);
        timeSheet.setTaskTypeId(taskToUpdate.getTaskTypeId());

        timeSheet.setBuId(Constants.PERSONAL_BU_ID);
        timeSheet.setProjectId(Constants.PERSONAL_PROJECT_ID);
        timeSheet.setTeamId(PERSONAL_TEAM_ID);
        timeSheet.setAccountId(taskToUpdate.getFkAccountId().getAccountId());
        timeSheet.setOrgId(personalOrgId);
        timeSheet.setUserId(taskToUpdate.getFkAccountId().getFkUserId().getUserId());
        return timeSheet;
    }


    // ****************************** Other Methods **************************************************


    /** this to mark the workflow status of the task as deleted */
    public void deleteTaskByTaskId(Long personalTaskId, String accountIds, String timeZone) {
        PersonalTask task = personalTaskRepository.findById(personalTaskId).orElseThrow(EntityNotFoundException::new);

        // validation: only the user to whom the personal task is assigned can delete that task
        if (!Objects.equals(task.getFkAccountId().getAccountId(), Long.parseLong(accountIds))) {
            throw new ValidationFailedException("You're not authorized to delete this task");
        }

        // validation: deletion of completed or already deleted task not allowed
        if (Constants.WorkFlowStatusIds.COMPLETED.contains(task.getFkWorkflowTaskStatus().getWorkflowTaskStatusId())) {
            throw new DeleteTaskException("Completed tasks can not be deleted.");
        } else if (Constants.WorkFlowStatusIds.DELETED.contains(task.getFkWorkflowTaskStatus().getWorkflowTaskStatusId())) {
            throw new DeleteTaskException("Work Item is already marked deleted.");
        }

        WorkFlowTaskStatus workFlowTaskStatus = workFlowTaskStatusRepository.findByWorkflowTaskStatusAndFkWorkFlowTypeWorkflowTypeId(Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE, task.getTaskWorkflowId());
        task.setFkWorkflowTaskStatus(workFlowTaskStatus);
        task.setTaskState(workFlowTaskStatus.getWorkflowTaskState());
        task.setCurrentlyScheduledTaskIndicator(false);
        task.setCurrentActivityIndicator(false);

        personalTaskRepository.save(task);
    }

}
