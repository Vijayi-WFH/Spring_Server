package com.tse.core_application.service.Impl;

import com.tse.core_application.custom.model.EmailFirstLastAccountId;


import com.tse.core_application.dto.performance_notes.PerfNoteFilters;
import com.tse.core_application.dto.performance_notes.PerfNotesRequest;
import com.tse.core_application.dto.performance_notes.PerfNotesResponse;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.model.Constants;
import com.tse.core_application.model.Task;
import com.tse.core_application.model.UserAccount;
import com.tse.core_application.model.WorkFlowTaskStatus;
import com.tse.core_application.model.performance_notes.PerfNote;
import com.tse.core_application.model.performance_notes.PerfNoteHistory;
import com.tse.core_application.model.performance_notes.TaskRating;
import com.tse.core_application.repository.*;
import com.tse.core_application.utils.CommonUtils;
import com.tse.core_application.utils.DateTimeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.Query;
import java.util.*;

@Service
public class PerfNoteService {
    private static final Logger logger = LogManager.getLogger(PerfNoteService.class.getName());

    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private TaskRatingRepository taskRatingRepository;
    @Autowired
    private PerfNoteRepository perfNoteRepository;
    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private PerfNoteHistoryRepository perfNoteHistoryRepository;

    @Autowired
    private EntityPreferenceService entityPreferenceService;

    @Autowired
    private AccessDomainRepository accessDomainRepository;

    @Autowired
    private MeetingService meetingService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AuditService auditService;

    /**
     * This method checks if the user have permission to add performance note and add the performance note to the task
     */
    public PerfNotesResponse addPerfNote (PerfNotesRequest perfNotesRequest, String accountIds, String timeZone) throws IllegalAccessException {
        List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
        if (!headerAccountIds.contains(perfNotesRequest.getPostedByAccountId())) {
            throw new ValidationFailedException("The account ID provided is not authorized to post this performance note.");
        }
        Task task = taskRepository.findByTaskId(perfNotesRequest.getTaskId());
        if (task == null) {
            throw new EntityNotFoundException("Task not found");
        }
        validateUserRole(headerAccountIds, task.getFkTeamId().getTeamId(), task.getFkOrgId().getOrgId());
        WorkFlowTaskStatus workFlowTaskStatus = task.getFkWorkflowTaskStatus();
        if (Objects.equals(workFlowTaskStatus.getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG_TITLE_CASE)) {
            throw new ValidationFailedException("User not allowed to add performance note for a backlog Work Item");
        }
        if (Objects.equals(task.getFkAccountIdAssigned().getAccountId(), perfNotesRequest.getPostedByAccountId())) {
            throw new IllegalAccessException("User not authorized to rate his/her own performance");
        }
        if (perfNoteRepository.existsByTaskIdAndFkPostedByAccountIdAccountIdAndIsDeletedFalse(task.getTaskId(), perfNotesRequest.getPostedByAccountId())) {
            throw new ValidationFailedException("User can only post one performance note per Work Item. Please update your existing performance note instead.");
        }
        if ((perfNotesRequest.getPerfNote() == null || perfNotesRequest.getPerfNote().isEmpty())
                && perfNotesRequest.getTaskRatingId() == null) {
            String errorMessage = "Please provide performance note";
            if (Objects.equals(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE, workFlowTaskStatus.getWorkflowTaskStatus())) {
                errorMessage += " or Work Item rating";
            }
            throw new IllegalStateException(errorMessage);
        }
        PerfNote perfNote = new PerfNote();
        CommonUtils.copyNonNullProperties(perfNotesRequest, perfNote);
        UserAccount postedByUser = userAccountRepository.findByAccountIdAndIsActive(perfNotesRequest.getPostedByAccountId(), true);
        UserAccount assignedToUser = userAccountRepository.findByAccountIdAndIsActive(perfNotesRequest.getAssignedToAccountId(), true);
        if (workFlowTaskStatus.getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE)
                && perfNotesRequest.getTaskRatingId() != null) {
            TaskRating taskRating = taskRatingRepository.findByTaskRatingId(perfNotesRequest.getTaskRatingId());
            if (taskRating != null) {
                perfNote.setFkTaskRatingId(taskRating);
            } else {
                throw new IllegalStateException("Work Item rating don't exists");
            }
        }
        perfNote.setFkPostedByAccountId(postedByUser);
        perfNote.setFkAssignedToAccountId(assignedToUser);
        PerfNote savedPerfNote = perfNoteRepository.save(perfNote);
        auditService.auditForAddingPerfNote(postedByUser, savedPerfNote);
        PerfNoteHistory perfNoteHistory = new PerfNoteHistory();
        BeanUtils.copyProperties(savedPerfNote, perfNoteHistory);
        perfNoteHistoryRepository.save(perfNoteHistory);
        return generatePerfNoteResponse(savedPerfNote, timeZone, savedPerfNote.getVersion());
    }

    /**
     * This method generates the response for the performance note object, which is later sent to frontend.
     */
    private PerfNotesResponse generatePerfNoteResponse (PerfNote perfNote, String timeZone, int version) {
        PerfNotesResponse perfNotesResponse = new PerfNotesResponse();
        BeanUtils.copyProperties(perfNote, perfNotesResponse);
        perfNotesResponse.setCreatedDateTime(DateTimeUtils.convertServerDateToUserTimezone(perfNote.getCreatedDateTime(), timeZone));
        perfNotesResponse.setLastUpdatedDateTime(perfNote.getLastUpdatedDateTime() != null ?
                DateTimeUtils.convertServerDateToUserTimezone(perfNote.getLastUpdatedDateTime(), timeZone) : null);

        perfNotesResponse.setPostedByAccount(new EmailFirstLastAccountId(perfNote.getFkPostedByAccountId().getEmail(),
                perfNote.getFkPostedByAccountId().getAccountId(), perfNote.getFkPostedByAccountId().getFkUserId().getFirstName(),
                perfNote.getFkPostedByAccountId().getFkUserId().getLastName()));

        perfNotesResponse.setAssignedToAccount(new EmailFirstLastAccountId(perfNote.getFkAssignedToAccountId().getEmail(),
                perfNote.getFkAssignedToAccountId().getAccountId(), perfNote.getFkAssignedToAccountId().getFkUserId().getFirstName(),
                perfNote.getFkAssignedToAccountId().getFkUserId().getLastName()));

        if (perfNote.getFkModifiedByAccountId() != null) {
            perfNotesResponse.setModifiedByAccount(new EmailFirstLastAccountId(perfNote.getFkModifiedByAccountId().getEmail(),
                    perfNote.getFkModifiedByAccountId().getAccountId(), perfNote.getFkModifiedByAccountId().getFkUserId().getFirstName(),
                    perfNote.getFkModifiedByAccountId().getFkUserId().getLastName()));
        }
        perfNotesResponse.setTaskInfo(taskRepository.getTaskBasicDetailsByTaskId(perfNote.getTaskId()));
        perfNotesResponse.setVersion(version);
        return perfNotesResponse;
    }

    /**
     * This method updates the performance note for a task by checking if modifier and posted users are same
     */
    public PerfNotesResponse updatePerfNote (Long perfNoteId, PerfNotesRequest perfNotesRequest, String accountIds, String timeZone) {
        PerfNote perfNote = perfNoteRepository.findByPerfNoteId(perfNoteId);
        PerfNote perfNoteCopy = new PerfNote();
        BeanUtils.copyProperties(perfNote, perfNoteCopy);
        List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
        if (!Objects.equals(perfNote.getFkPostedByAccountId().getAccountId(), perfNotesRequest.getModifiedByAccountId()) && !headerAccountIds.contains(perfNotesRequest.getModifiedByAccountId())) {
            throw new ValidationFailedException("User not authorized to update performance note");
        }
        CommonUtils.copyNonNullProperties(perfNotesRequest, perfNote);
        if (perfNotesRequest.getPostedByAccountId() != null && !Objects.equals(perfNote.getFkPostedByAccountId().getAccountId(), perfNotesRequest.getPostedByAccountId())) {
            throw new ValidationFailedException("User not authorized to update posted by user for a performance note");
        }

        if (perfNotesRequest.getTaskId() != null && !Objects.equals(perfNote.getTaskId(), perfNotesRequest.getTaskId())) {
            throw new IllegalStateException("User not authorized to change Work Item for performance note");
        }
        Task task=taskRepository.findByTaskId(perfNotesRequest.getTaskId());
        if (task!=null && Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus().toLowerCase(),Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE.toLowerCase())) {
            throw new ValidationFailedException("Performance notes cannot be created or updated for deleted Work Item.");
        }

        if (perfNotesRequest.getAssignedToAccountId() != null && !Objects.equals(perfNote.getFkAssignedToAccountId().getAccountId(), perfNotesRequest.getAssignedToAccountId())) {
            throw new ValidationFailedException("User not allowed to change the user associated with this performance note.");
        }
        WorkFlowTaskStatus workFlowTaskStatus = taskRepository.findAllFkWorkflowTaskStatusByTaskId(perfNote.getTaskId());
        if ((perfNotesRequest.getPerfNote() == null || perfNotesRequest.getPerfNote().isEmpty())
                && perfNotesRequest.getTaskRatingId() == null) {
            String errorMessage = "Please provide performance note";
            if (Objects.equals(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE, workFlowTaskStatus.getWorkflowTaskStatus())) {
                errorMessage += " or Work Item rating";
            }
            throw new IllegalStateException(errorMessage);
        }
        if (!workFlowTaskStatus.getWorkflowTaskStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE)
                && perfNotesRequest.getTaskRatingId() != null) {
            throw new IllegalStateException("Work Item rating is only allowed when Work Item is completed");
        }
        if (perfNotesRequest.getTaskRatingId() != null && (perfNote.getFkTaskRatingId() == null || !Objects.equals(perfNote.getFkTaskRatingId().getTaskRatingId(), perfNotesRequest.getTaskRatingId()))) {
            TaskRating taskRating = taskRatingRepository.findByTaskRatingId(perfNotesRequest.getTaskRatingId());
            if (taskRating != null) {
                perfNote.setFkTaskRatingId(taskRating);
            } else {
                throw new IllegalStateException("Work Item rating don't exists");
            }
        }
        UserAccount modifiedUser = userAccountRepository.findByAccountIdAndIsActive(perfNotesRequest.getModifiedByAccountId(), true);
        perfNote.setFkModifiedByAccountId(modifiedUser);
        PerfNote savedPerfNote = perfNoteRepository.save(perfNote);
        auditService.auditForUpdatingPerfNote(modifiedUser, perfNote);
        int version = savedPerfNote.getVersion();
        if (!Objects.equals(perfNoteCopy, savedPerfNote)) {
            version++;
            PerfNoteHistory perfNoteHistory = new PerfNoteHistory();
            BeanUtils.copyProperties(savedPerfNote, perfNoteHistory);
            perfNoteHistory.setVersion(version);
            perfNoteHistoryRepository.save(perfNoteHistory);
        }
        return generatePerfNoteResponse(savedPerfNote, timeZone, version);
    }

    public List<PerfNotesResponse> getPerfNoteForTask (Long taskId, Long accountId, String timeZone) {
        Task task = taskRepository.findByTaskId(taskId);
        if (task == null) {
            throw new EntityNotFoundException("Task not found");
        }
        HashMap<Long, Integer> accountToRoleMap = new HashMap<>();
        Integer userTeamRole = accessDomainRepository.getMaxRoleIdForAccountIdAndTeamIdAndIsActive(accountId, Constants.EntityTypes.TEAM, task.getFkTeamId().getTeamId(), true);
        userTeamRole = userTeamRole != null ? userTeamRole : 0;
        accountToRoleMap.put(accountId, userTeamRole);
        Set<Long> stakeHolderAccountIds = meetingService.getTaskStakeHolders(task, false);
        List<PerfNote> perfNoteList = perfNoteRepository.findAllByTaskIdAndIsDeletedFalse(taskId);
        List<PerfNotesResponse> perfNotesResponseList = new ArrayList<>();
        for (PerfNote perfNote : perfNoteList) {
            Long postedByAccountId = perfNote.getFkPostedByAccountId().getAccountId();
            Integer postedByUserRole = accountToRoleMap.containsKey(postedByAccountId) ? accountToRoleMap.get(postedByAccountId)
                    : accessDomainRepository.getMaxRoleIdForAccountIdAndTeamIdAndIsActive(postedByAccountId, Constants.EntityTypes.TEAM, task.getFkTeamId().getTeamId(), true); accountToRoleMap.put(postedByAccountId, postedByUserRole);
            postedByUserRole = postedByUserRole != null ? postedByUserRole : 0;
            boolean isSameUser = Objects.equals(accountId, postedByAccountId);
            boolean isHigherRole = userTeamRole > postedByUserRole && !perfNote.getIsPrivate();
            boolean isStakeHolder = stakeHolderAccountIds.contains(accountId) && perfNote.getIsShared();

            if (isSameUser || isHigherRole || isStakeHolder) {
                perfNotesResponseList.add(generatePerfNoteResponse(perfNote, timeZone, perfNote.getVersion()));
            }
        }
        perfNotesResponseList.sort(Comparator.comparing(PerfNotesResponse::getCreatedDateTime, Comparator.reverseOrder()));
        return perfNotesResponseList;
    }

    public List<PerfNotesResponse> getPerfNoteByFilter (PerfNoteFilters request, Long accountId, String timeZone) {
        Long taskId = null;
        Long orgId = userAccountRepository.findOrgIdByAccountIdAndIsActive(accountId, true).getOrgId();
        if (request.getTaskNumber() != null) {
            Task taskDb = taskRepository.findByFkTeamIdFkOrgIdOrgIdAndTaskNumber(orgId, request.getTaskNumber());
            if (taskDb == null) {
                throw new EntityNotFoundException("Task does not exist");
            }
            taskId = taskDb.getTaskId();
        }
        String nativeQuery = getNativeQuery(request, taskId);
        Query query = entityManager.createNativeQuery(nativeQuery, PerfNote.class);
        setQueryParameters(request, query, taskId);
        List<PerfNote> perfNoteList = query.getResultList();
        List<PerfNotesResponse> perfNotesResponseList = new ArrayList<>();
        Map<Long, Task> taskMap = new HashMap<>();
        Map<Long, Map<Long, Integer>> userToTeamRolesMap = new HashMap<>();
        for (PerfNote perfNote : perfNoteList) {
            Task task = taskMap.containsKey(perfNote.getTaskId()) ? taskMap.get(perfNote.getTaskId())
                    : taskRepository.findByTaskId(perfNote.getTaskId()); taskMap.put(task.getTaskId(), task);
            if(task.getFkTeamId().getIsDeleted() != null && task.getFkTeamId().getIsDeleted()) {
                continue;
            }
            Integer userTeamRole;
            if (userToTeamRolesMap.containsKey(accountId)) {
                Map<Long, Integer> userRoleMap = userToTeamRolesMap.get(accountId);
                userTeamRole = userRoleMap.containsKey(task.getFkTeamId().getTeamId()) ? userRoleMap.get(task.getFkTeamId().getTeamId())
                        : accessDomainRepository.getMaxRoleIdForAccountIdAndTeamIdAndIsActive(accountId, Constants.EntityTypes.TEAM, task.getFkTeamId().getTeamId(), true);
                            userRoleMap.put(task.getFkTeamId().getTeamId(), userTeamRole);
                            userToTeamRolesMap.put(accountId, userRoleMap);
            } else {
                userTeamRole = accessDomainRepository.getMaxRoleIdForAccountIdAndTeamIdAndIsActive(accountId, Constants.EntityTypes.TEAM, task.getFkTeamId().getTeamId(), true);
                Map<Long, Integer> userRoleMap = new HashMap<>();

                userRoleMap.put(task.getFkTeamId().getTeamId(), userTeamRole);
                userToTeamRolesMap.put(accountId, userRoleMap);
            }
            userTeamRole = userTeamRole != null ? userTeamRole : 0;
            Set<Long> stackHoldersList = meetingService.getTaskStakeHolders(task, false);
            Long postedByAccountId = perfNote.getFkPostedByAccountId().getAccountId();
            Integer postedByRole;
            if (userToTeamRolesMap.containsKey(postedByAccountId)) {
                Map<Long, Integer> userRoleMap = userToTeamRolesMap.get(postedByAccountId);
                postedByRole = userRoleMap.containsKey(task.getFkTeamId().getTeamId()) ? userRoleMap.get(task.getFkTeamId().getTeamId())
                        : accessDomainRepository.getMaxRoleIdForAccountIdAndTeamIdAndIsActive(postedByAccountId, Constants.EntityTypes.TEAM, task.getFkTeamId().getTeamId(), true);
                            userRoleMap.put(task.getFkTeamId().getTeamId(), postedByRole);
                            userToTeamRolesMap.put(postedByAccountId, userRoleMap);
            } else {
                postedByRole = accessDomainRepository.getMaxRoleIdForAccountIdAndTeamIdAndIsActive(postedByAccountId, Constants.EntityTypes.TEAM, task.getFkTeamId().getTeamId(), true);
                Map<Long, Integer> userRoleMap = new HashMap<>();
                userRoleMap.put(task.getFkTeamId().getTeamId(), postedByRole);
                userToTeamRolesMap.put(postedByAccountId, userRoleMap);
            }
            postedByRole = postedByRole != null ? postedByRole : 0;

            boolean isSameUser = Objects.equals(accountId, postedByAccountId);
            boolean isHigherRole = userTeamRole > postedByRole && !perfNote.getIsPrivate();
            boolean isStakeHolder = stackHoldersList.contains(accountId) && perfNote.getIsShared();

            if (isSameUser || isHigherRole || isStakeHolder) {
                perfNotesResponseList.add(generatePerfNoteResponse(perfNote, timeZone, perfNote.getVersion()));
            }
        }
        perfNotesResponseList.sort(Comparator.comparing(PerfNotesResponse::getCreatedDateTime, Comparator.reverseOrder()));
        return perfNotesResponseList;
    }

    /**
     * This method verifies if user have the role for performance notes
     */
    private void validateUserRole (List<Long> accountIdsList, Long teamId, Long orgId) throws IllegalAccessException {
        List<Integer> rolesWithPerfNoteRights = entityPreferenceService.getRolesWithPerfNoteRights(teamId, orgId);
        if (!accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.TEAM,
                teamId, accountIdsList, rolesWithPerfNoteRights, true)) {
            throw new IllegalAccessException("User not authorized to add performance notes");
        }
    }

    private String getNativeQuery (PerfNoteFilters request, Long taskId) {
        String nativeQuery = "SELECT * FROM tse.perf_note WHERE is_deleted = false ";
        if (request.getAssignedTo() != null) {
            nativeQuery += "AND assigned_to_account_id = :assignedToAccountId ";
        }
        if (request.getTaskNumber() != null && taskId != null) {
            nativeQuery += "AND task_id = :taskId ";
        }
        if (request.getPostedBy() != null) {
            nativeQuery += "AND posted_by_account_id = :postedByAccountId ";
        }
        if (request.getFromDate() != null) {
            nativeQuery += "AND DATE(created_date_time) >= :fromDate ";
        }
        if (request.getToDate() != null) {
            nativeQuery += "AND DATE(created_date_time) <= :toDate";
        }
        return nativeQuery;
    }

    private void setQueryParameters (PerfNoteFilters request, Query query, Long taskId) {
        if (request.getAssignedTo() != null) {
            query.setParameter("assignedToAccountId", request.getAssignedTo());
        }
        if (request.getPostedBy() != null) {
            query.setParameter("postedByAccountId", request.getPostedBy());
        }
        if (request.getTaskNumber() != null && taskId != null) {
            query.setParameter("taskId", taskId);
        }
        if (request.getFromDate() != null) {
            query.setParameter("fromDate", request.getFromDate());
        }
        if (request.getToDate() != null) {
            query.setParameter("toDate", request.getToDate());
        }
    }

    public List<TaskRating> getAllTaskRating () {
        return taskRatingRepository.findAll();
    }
}
