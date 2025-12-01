package com.tse.core_application.service;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.tse.core_application.dto.PushNotificationRequest;
import com.tse.core_application.dto.QuickCreateTaskRequest;
import com.tse.core_application.exception.FirebaseNotificationException;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.exception.WorkflowTypeDoesNotExistException;
import com.tse.core_application.model.*;

import com.tse.core_application.repository.*;
import com.tse.core_application.service.Impl.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class TaskServiceImplTest {
    @Mock
    private FirebaseTokenService firebaseTokenService;

    @Mock
    private FCMService fcmService;

    @Mock
    private UserAccountService userAccountService;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditRepository auditRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private WorkFlowTaskStatusRepository workFlowTaskStatusRepository;
    @Spy
    @InjectMocks
    private TaskServiceImpl taskServiceImpl;

    @Mock
    private QuickCreateTaskRequest quickCreateTaskRequest;
    @Mock
    private WorkFlowTaskStatusRepository workflowTaskStatusRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private BURepository buRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private OrganizationRepository organizationRepository;

    @Test(expected = WorkflowTypeDoesNotExistException.class)
    public void testPopulateTask_invalidDefaultWorkflow() {
        QuickCreateTaskRequest quickCreateTaskRequest = new QuickCreateTaskRequest();
        quickCreateTaskRequest.setTaskTitle("Test Task");
        quickCreateTaskRequest.setTaskDesc("Test Desc");
        quickCreateTaskRequest.setTaskPriority("P2");
        quickCreateTaskRequest.setTaskWorkFlowId(0);
        String accountId = "1";
        taskServiceImpl.populateTask(quickCreateTaskRequest, accountId);
    }

    @Test
    public void testPopulateTask_validDefaultWorkflow() {
        QuickCreateTaskRequest quickCreateTaskRequest = new QuickCreateTaskRequest();
        quickCreateTaskRequest.setTaskTitle("Test Task");
        quickCreateTaskRequest.setTaskDesc("Test Desc");
        quickCreateTaskRequest.setTaskPriority("P2");
        quickCreateTaskRequest.setTaskWorkFlowId(3);
        quickCreateTaskRequest.setTaskWorkFlowStatus(15);
        quickCreateTaskRequest.setTeamId(1L);
        quickCreateTaskRequest.setEstimate(10);
        quickCreateTaskRequest.setExpStartDateTime(LocalDateTime.now());
        quickCreateTaskRequest.setExpEndDateTime(LocalDateTime.now().plusHours(2L));
        String accountId = "1";

        Team team = new Team();
        team.setTeamId(1L);
        Project project = new Project();
        project.setProjectId(1L);
        Long buId = 1L;
        project.setBuId(buId);
        Organization organization = new Organization();
        organization.setOrgId(1L);
        team.setFkOrgId(organization);
        team.setFkProjectId(project);
        WorkFlowTaskStatus workFlowTaskStatus = new WorkFlowTaskStatus();
        workFlowTaskStatus.setWorkflowTaskStatusId(15);
        when(workFlowTaskStatusRepository.findWorkflowTaskStatusByWorkflowTaskStatusId(anyInt())).thenReturn(workFlowTaskStatus);
        UserAccount userAccount = new UserAccount();
        userAccount.setAccountId(1L);
        when(userAccountRepository.findByAccountId(anyLong())).thenReturn(userAccount);
        when(teamRepository.findByTeamId(anyLong())).thenReturn(team);
        Task returnedTask = taskServiceImpl.populateTask(quickCreateTaskRequest, accountId);

        assertThat(returnedTask.getTaskPriority()).isEqualTo(quickCreateTaskRequest.getTaskPriority());
        assertThat(returnedTask.getTaskWorkflowId()).isEqualTo(quickCreateTaskRequest.getTaskWorkFlowId());
        assertThat(returnedTask.getFkWorkflowTaskStatus().getWorkflowTaskStatusId()).isEqualTo(quickCreateTaskRequest.getTaskWorkFlowStatus());
        assertThat(returnedTask.getFkTeamId().getTeamId()).isEqualTo(team.getTeamId());
        assertThat(returnedTask.getFkProjectId().getProjectId()).isEqualTo(project.getProjectId());
        assertThat(returnedTask.getBuId()).isEqualTo(buId);
        assertThat(returnedTask.getFkOrgId().getOrgId()).isEqualTo(organization.getOrgId());

    }


    /**
     * Case - when taskActStDate and taskActStTime are given but taskExpStDate and taskExpStTime are not given.
     * The method which is under test. @link #{validateTaskActStartDateTimeForWorkflowStatusBlocked(Task task)}.
     */
    @Test
    public void testValidateTaskActStartDateTimeForWorkflowStatusBlocked_SuccessCase1() {
        WorkFlowTaskStatus workFlowTaskStatus = new WorkFlowTaskStatus();
        workFlowTaskStatus.setWorkflowTaskStatusId(1);
        workFlowTaskStatus.setWorkflowTaskStatus("blocked");

        Task task1 = new Task();
        task1.setTaskActStDate(LocalDateTime.now());
        task1.setTaskActStTime(LocalTime.now());
        task1.setFkWorkflowTaskStatus(workFlowTaskStatus);

        boolean actual = taskServiceImpl.validateTaskActStartDateTimeForWorkflowStatusBlocked(task1);
        assertThat(actual).isEqualTo(true);

    }


    /**
     * Case - when taskActStDate and taskActStTime are given but one of taskExpStDate or taskExpStTime
     * is not given.
     * The method which is under test. @link #{validateTaskActStartDateTimeForWorkflowStatusBlocked(Task task)}.
     */
    @Test
    public void testValidateTaskActStartDateTimeForWorkflowStatusBlocked_SuccessCase2() {
        WorkFlowTaskStatus workFlowTaskStatus = new WorkFlowTaskStatus();
        workFlowTaskStatus.setWorkflowTaskStatusId(1);
        workFlowTaskStatus.setWorkflowTaskStatus("blocked");

        Task task1 = new Task();
        task1.setTaskActStDate(LocalDateTime.now());
        task1.setTaskActStTime(LocalTime.now());
        task1.setFkWorkflowTaskStatus(workFlowTaskStatus);
        task1.setTaskExpStartDate(LocalDateTime.now());

        boolean actual = taskServiceImpl.validateTaskActStartDateTimeForWorkflowStatusBlocked(task1);
        assertThat(task1.getTaskExpStartDate()).isNotNull();
        assertThat(task1.getTaskExpStartTime()).isNotNull();
        assertThat(actual).isEqualTo(true);

    }


    /**
     * Case - when task is null.
     * The method which is under test. @link #{validateTaskActStartDateTimeForWorkflowStatusBlocked(Task task)}.
     */
    @Test
    public void testValidateTaskActStartDateTimeForWorkflowStatusBlocked_ElseCase1() {
        boolean actual = taskServiceImpl.validateTaskActStartDateTimeForWorkflowStatusBlocked(null);
        assertThat(actual).isEqualTo(false);

    }

    /**
     * Case - workflowTaskStatus is not blocked.
     * The method which is under test. @link #{validateTaskActStartDateTimeForWorkflowStatusBlocked(Task task)}.
     */
    @Test
    public void testValidateTaskActStartDateTimeForWorkflowStatusBlocked_ElseCase2() {
        Task task1 = new Task();
        WorkFlowTaskStatus workFlowTaskStatus = new WorkFlowTaskStatus();
        workFlowTaskStatus.setWorkflowTaskStatusId(1);
        workFlowTaskStatus.setWorkflowTaskStatus("not-started");
        task1.setFkWorkflowTaskStatus(workFlowTaskStatus);

        boolean actual = taskServiceImpl.validateTaskActStartDateTimeForWorkflowStatusBlocked(task1);
        assertThat(actual).isEqualTo(false);

    }

    /**
     * Case - when one of the taskActStDate or taskActStTime is not given.
     * The method which is under test. @link #{validateTaskActStartDateTimeForWorkflowStatusBlocked(Task task)}.
     */
    @Test
    public void testValidateTaskActStartDateTimeForWorkflowStatusBlocked_ElseCase3() {
        Task task1 = new Task();
        task1.setTaskActStDate(LocalDateTime.now());
        WorkFlowTaskStatus workFlowTaskStatus = new WorkFlowTaskStatus();
        workFlowTaskStatus.setWorkflowTaskStatusId(1);
        workFlowTaskStatus.setWorkflowTaskStatus("blocked");
        task1.setFkWorkflowTaskStatus(workFlowTaskStatus);

        boolean actual = taskServiceImpl.validateTaskActStartDateTimeForWorkflowStatusBlocked(task1);
        assertThat(actual).isEqualTo(false);

    }

    /**
     * Case - both taskActStDate and taskActStTime are given but taskExpStDate is null
     * and taskExpStTime is not null.
     * The method which is under test. @link #{validateTaskActStartDateTimeForWorkflowStatusBlocked(Task task)}.
     */
    @Test
    public void testValidateTaskActStartDateTimeForWorkflowStatusBlocked_ElseCase4_ifCase1() {
        Task task1 = new Task();
        task1.setTaskActStDate(LocalDateTime.now());
        task1.setTaskActStTime(LocalTime.now());
        task1.setTaskExpStartDate(null);
        task1.setTaskExpStartTime(LocalTime.now());
        WorkFlowTaskStatus workFlowTaskStatus = new WorkFlowTaskStatus();
        workFlowTaskStatus.setWorkflowTaskStatusId(1);
        workFlowTaskStatus.setWorkflowTaskStatus("blocked");
        task1.setFkWorkflowTaskStatus(workFlowTaskStatus);

        boolean actual = taskServiceImpl.validateTaskActStartDateTimeForWorkflowStatusBlocked(task1);
        assertThat(task1.getTaskExpStartDate()).isEqualTo(task1.getTaskActStDate());
        assertThat(task1.getTaskExpStartTime()).isEqualTo(task1.getTaskActStTime());

    }

    /**
     * Case - both taskActStDate and taskActStTime are given and taskExpStDate is given
     * but taskExpStTime is not given.
     * The method which is under test. @link #{validateTaskActStartDateTimeForWorkflowStatusBlocked(Task task)}.
     */
    @Test
    public void testValidateTaskActStartDateTimeForWorkflowStatusBlocked_ElseCase4_ifCase2() {
        Task task1 = new Task();
        task1.setTaskActStDate(LocalDateTime.now());
        task1.setTaskActStTime(LocalTime.now());
        task1.setTaskExpStartDate(LocalDateTime.now());
        WorkFlowTaskStatus workFlowTaskStatus = new WorkFlowTaskStatus();
        workFlowTaskStatus.setWorkflowTaskStatusId(1);
        workFlowTaskStatus.setWorkflowTaskStatus("blocked");
        task1.setFkWorkflowTaskStatus(workFlowTaskStatus);

        boolean actual = taskServiceImpl.validateTaskActStartDateTimeForWorkflowStatusBlocked(task1);
        assertThat(task1.getTaskExpStartDate()).isNotNull();
        assertThat(task1.getTaskExpStartTime()).isNotNull();
        assertThat(actual).isEqualTo(true);

    }

    /**
     * Case - when all conditions are true.
     * The method which is under test. @link #{validatePreviousWorkflowStatusForBacklogTask(Task taskToUpdate, Task updatedTask)}.
     */
    @Test
    public void testValidatePreviousWorkflowStatusForBacklogTask_Success() {
        Task taskToUpdate = new Task();
        Task updatedTask = new Task();
        WorkFlowTaskStatus workFlowTaskStatusUpdated = new WorkFlowTaskStatus();
        workFlowTaskStatusUpdated.setWorkflowTaskStatusId(1);
        workFlowTaskStatusUpdated.setWorkflowTaskStatus("backlog");
        updatedTask.setFkWorkflowTaskStatus(workFlowTaskStatusUpdated);

        WorkFlowTaskStatus workFlowTaskStatusToUpdate = new WorkFlowTaskStatus();
        workFlowTaskStatusToUpdate.setWorkflowTaskStatusId(1);
        workFlowTaskStatusToUpdate.setWorkflowTaskStatus("started");
        taskToUpdate.setFkWorkflowTaskStatus(workFlowTaskStatusToUpdate);

        boolean actual = taskServiceImpl.validatePreviousWorkflowStatusForBacklogTask(taskToUpdate, updatedTask);
        assertThat(actual).isEqualTo(true);

    }

    /**
     * Case - when one of the task is null.
     * The method which is under test. @link #{validatePreviousWorkflowStatusForBacklogTask(Task taskToUpdate, Task updatedTask)}.
     */
    @Test
    public void testValidatePreviousWorkflowStatusForBacklogTask_Else1() {
        Task taskToUpdate = new Task();

        boolean actual = taskServiceImpl.validatePreviousWorkflowStatusForBacklogTask(taskToUpdate, null);
        assertThat(actual).isEqualTo(false);

    }

    /**
     * Case - when updated task is not backlog.
     * The method which is under test. @link #{validatePreviousWorkflowStatusForBacklogTask(Task taskToUpdate, Task updatedTask)}.
     */
    @Test
    public void testValidatePreviousWorkflowStatusForBacklogTask_Else2() {
        Task taskToUpdate = new Task();
        Task updatedTask = new Task();
        WorkFlowTaskStatus workFlowTaskStatusUpdated = new WorkFlowTaskStatus();
        workFlowTaskStatusUpdated.setWorkflowTaskStatusId(1);
        workFlowTaskStatusUpdated.setWorkflowTaskStatus("completed");
        updatedTask.setFkWorkflowTaskStatus(workFlowTaskStatusUpdated);

        WorkFlowTaskStatus workFlowTaskStatusToUpdate = new WorkFlowTaskStatus();
        workFlowTaskStatusToUpdate.setWorkflowTaskStatusId(1);
        workFlowTaskStatusToUpdate.setWorkflowTaskStatus("started");
        taskToUpdate.setFkWorkflowTaskStatus(workFlowTaskStatusToUpdate);

        boolean actual = taskServiceImpl.validatePreviousWorkflowStatusForBacklogTask(taskToUpdate, updatedTask);
        assertThat(actual).isEqualTo(false);

    }

    /**
     * Case - when both task are in backlog.
     * The method which is under test. @link #{validatePreviousWorkflowStatusForBacklogTask(Task taskToUpdate, Task updatedTask)}.
     */
    @Test
    public void testValidatePreviousWorkflowStatusForBacklogTask_Else3() {
        Task taskToUpdate = new Task();
        Task updatedTask = new Task();
        WorkFlowTaskStatus workFlowTaskStatusUpdated = new WorkFlowTaskStatus();
        workFlowTaskStatusUpdated.setWorkflowTaskStatusId(1);
        workFlowTaskStatusUpdated.setWorkflowTaskStatus("backlog");
        updatedTask.setFkWorkflowTaskStatus(workFlowTaskStatusUpdated);

        WorkFlowTaskStatus workFlowTaskStatusToUpdate = new WorkFlowTaskStatus();
        workFlowTaskStatusToUpdate.setWorkflowTaskStatusId(1);
        workFlowTaskStatusToUpdate.setWorkflowTaskStatus("backlog");
        taskToUpdate.setFkWorkflowTaskStatus(workFlowTaskStatusToUpdate);

        boolean actual = taskServiceImpl.validatePreviousWorkflowStatusForBacklogTask(taskToUpdate, updatedTask);
        assertThat(actual).isEqualTo(true);

    }

    /**
     * Case - taskToUpdate is not started.
     * The method which is under test. @link #{validatePreviousWorkflowStatusForBacklogTask(Task taskToUpdate, Task updatedTask)}.
     */
    @Test
    public void testValidatePreviousWorkflowStatusForBacklogTask_Else4() {
        Task taskToUpdate = new Task();
        Task updatedTask = new Task();
        WorkFlowTaskStatus workFlowTaskStatusUpdated = new WorkFlowTaskStatus();
        workFlowTaskStatusUpdated.setWorkflowTaskStatusId(1);
        workFlowTaskStatusUpdated.setWorkflowTaskStatus("backlog");
        updatedTask.setFkWorkflowTaskStatus(workFlowTaskStatusUpdated);

        WorkFlowTaskStatus workFlowTaskStatusToUpdate = new WorkFlowTaskStatus();
        workFlowTaskStatusToUpdate.setWorkflowTaskStatusId(1);
        workFlowTaskStatusToUpdate.setWorkflowTaskStatus("not-started");
        taskToUpdate.setFkWorkflowTaskStatus(workFlowTaskStatusToUpdate);

        boolean actual = taskServiceImpl.validatePreviousWorkflowStatusForBacklogTask(taskToUpdate, updatedTask);
        assertThat(actual).isEqualTo(false);

    }

    /**
     * Case - when all conditions are true.
     * The method which is under test. @link #{validatePreviousWorkflowStatusForOnHoldTask(Task taskToUpdate, Task updatedTask)}.
     */
    @Test
    public void testValidatePreviousWorkflowStatusForOnHoldTask_Success() {
        Task taskToUpdate = new Task();
        Task updatedTask = new Task();

        WorkFlowTaskStatus workFlowTaskStatusUpdated = new WorkFlowTaskStatus();
        workFlowTaskStatusUpdated.setWorkflowTaskStatusId(1);
        workFlowTaskStatusUpdated.setWorkflowTaskStatus("on-hold");
        updatedTask.setFkWorkflowTaskStatus(workFlowTaskStatusUpdated);

        WorkFlowTaskStatus workFlowTaskStatusToUpdate = new WorkFlowTaskStatus();
        workFlowTaskStatusToUpdate.setWorkflowTaskStatusId(1);
        workFlowTaskStatusToUpdate.setWorkflowTaskStatus("not-started");
        taskToUpdate.setFkWorkflowTaskStatus(workFlowTaskStatusToUpdate);

        boolean actual = taskServiceImpl.validatePreviousWorkflowStatusForOnHoldTask(taskToUpdate, updatedTask);
        assertThat(actual).isEqualTo(true);

    }

    /**
     * Case - when one of the task is null.
     * The method which is under test. @link #{validatePreviousWorkflowStatusForOnHoldTask(Task taskToUpdate, Task updatedTask)}.
     */
    @Test
    public void testValidatePreviousWorkflowStatusForOnHoldTask_Else1() {
        Task taskToUpdate = new Task();

        boolean actual = taskServiceImpl.validatePreviousWorkflowStatusForOnHoldTask(taskToUpdate, null);
        assertThat(actual).isEqualTo(false);

    }

    /**
     * Case - when updated task is not on hold.
     * The method which is under test. @link #{validatePreviousWorkflowStatusForOnHoldTask(Task taskToUpdate, Task updatedTask)}.
     */
    @Test
    public void testValidatePreviousWorkflowStatusForOnHoldTask_Else2() {
        Task taskToUpdate = new Task();
        Task updatedTask = new Task();

        WorkFlowTaskStatus workFlowTaskStatusUpdated = new WorkFlowTaskStatus();
        workFlowTaskStatusUpdated.setWorkflowTaskStatusId(1);
        workFlowTaskStatusUpdated.setWorkflowTaskStatus("backlog");
        updatedTask.setFkWorkflowTaskStatus(workFlowTaskStatusUpdated);

        WorkFlowTaskStatus workFlowTaskStatusToUpdate = new WorkFlowTaskStatus();
        workFlowTaskStatusToUpdate.setWorkflowTaskStatusId(1);
        workFlowTaskStatusToUpdate.setWorkflowTaskStatus("not-started");
        taskToUpdate.setFkWorkflowTaskStatus(workFlowTaskStatusToUpdate);

        boolean actual = taskServiceImpl.validatePreviousWorkflowStatusForOnHoldTask(taskToUpdate, updatedTask);
        assertThat(actual).isEqualTo(false);

    }

    /**
     * Case - when both tasks are on hold.
     * The method which is under test. @link #{validatePreviousWorkflowStatusForOnHoldTask(Task taskToUpdate, Task updatedTask)}.
     */
    @Test
    public void testValidatePreviousWorkflowStatusForOnHoldTask_Else3() {
        Task taskToUpdate = new Task();
        Task updatedTask = new Task();

        WorkFlowTaskStatus workFlowTaskStatusUpdated = new WorkFlowTaskStatus();
        workFlowTaskStatusUpdated.setWorkflowTaskStatusId(1);
        workFlowTaskStatusUpdated.setWorkflowTaskStatus("on-hold");
        updatedTask.setFkWorkflowTaskStatus(workFlowTaskStatusUpdated);

        WorkFlowTaskStatus workFlowTaskStatusToUpdate = new WorkFlowTaskStatus();
        workFlowTaskStatusToUpdate.setWorkflowTaskStatusId(1);
        workFlowTaskStatusToUpdate.setWorkflowTaskStatus("on-hold");
        taskToUpdate.setFkWorkflowTaskStatus(workFlowTaskStatusToUpdate);

        boolean actual = taskServiceImpl.validatePreviousWorkflowStatusForOnHoldTask(taskToUpdate, updatedTask);
        assertThat(actual).isEqualTo(true);

    }

    /**
     * Case - when taskToUpdate is in backlog.
     * The method which is under test. @link #{validatePreviousWorkflowStatusForOnHoldTask(Task taskToUpdate, Task updatedTask)}.
     */
    @Test
    public void testValidatePreviousWorkflowStatusForOnHoldTask_Else4() {
        Task taskToUpdate = new Task();
        Task updatedTask = new Task();

        WorkFlowTaskStatus workFlowTaskStatusUpdated = new WorkFlowTaskStatus();
        workFlowTaskStatusUpdated.setWorkflowTaskStatusId(1);
        workFlowTaskStatusUpdated.setWorkflowTaskStatus("on-hold");
        updatedTask.setFkWorkflowTaskStatus(workFlowTaskStatusUpdated);

        WorkFlowTaskStatus workFlowTaskStatusToUpdate = new WorkFlowTaskStatus();
        workFlowTaskStatusToUpdate.setWorkflowTaskStatusId(1);
        workFlowTaskStatusToUpdate.setWorkflowTaskStatus("backlog");
        taskToUpdate.setFkWorkflowTaskStatus(workFlowTaskStatusToUpdate);

        boolean actual = taskServiceImpl.validatePreviousWorkflowStatusForOnHoldTask(taskToUpdate, updatedTask);
        assertThat(actual).isEqualTo(false);

    }

    /**
     * Case - when all conditions are true. i.e. both taskActStDate and taskActStTime are present but
     * one of the taskExpStDate or taskExpStTime is not present or both are not present.
     * The method which is under test. @link #{validateTaskActStartDateTimeForWorkflowStatusOnHold(Task task)}.
     */
    @Test
    public void testValidateTaskActStartDateTimeForWorkflowStatusOnHold_Success() {
        Task task = new Task();

        WorkFlowTaskStatus workFlowTaskStatusUpdated = new WorkFlowTaskStatus();
        workFlowTaskStatusUpdated.setWorkflowTaskStatusId(1);
        workFlowTaskStatusUpdated.setWorkflowTaskStatus("on-hold");
        task.setFkWorkflowTaskStatus(workFlowTaskStatusUpdated);

        task.setTaskActStDate(LocalDateTime.now());
        task.setTaskActStTime(LocalTime.now());

        boolean actual = taskServiceImpl.validateTaskActStartDateTimeForWorkflowStatusOnHold(task);
        assertThat(actual).isEqualTo(true);

    }

    /**
     * Case - when task is null.
     * The method which is under test. @link #{validateTaskActStartDateTimeForWorkflowStatusOnHold(Task task)}.
     */
    @Test
    public void testValidateTaskActStartDateTimeForWorkflowStatusOnHold_ElseCase1() {

        boolean actual = taskServiceImpl.validateTaskActStartDateTimeForWorkflowStatusOnHold(null);
        assertThat(actual).isEqualTo(false);

    }

    /**
     * Case - when the task workflow status is not on hold.
     * The method which is under test. @link #{validateTaskActStartDateTimeForWorkflowStatusOnHold(Task task)}.
     */
    @Test
    public void testValidateTaskActStartDateTimeForWorkflowStatusOnHold_ElseCase2() {
        Task task = new Task();

        WorkFlowTaskStatus workFlowTaskStatusUpdated = new WorkFlowTaskStatus();
        workFlowTaskStatusUpdated.setWorkflowTaskStatusId(1);
        workFlowTaskStatusUpdated.setWorkflowTaskStatus("started");
        task.setFkWorkflowTaskStatus(workFlowTaskStatusUpdated);

        task.setTaskActStDate(LocalDateTime.now());
        task.setTaskActStTime(LocalTime.now());

        boolean actual = taskServiceImpl.validateTaskActStartDateTimeForWorkflowStatusOnHold(task);
        assertThat(actual).isEqualTo(false);

    }

    /**
     * Case - when one of the taskActStDate or taskActStTime is null or both are null.
     * The method which is under test. @link #{validateTaskActStartDateTimeForWorkflowStatusOnHold(Task task)}.
     */
    @Test
    public void testValidateTaskActStartDateTimeForWorkflowStatusOnHold_ElseCase3() {
        Task task = new Task();

        WorkFlowTaskStatus workFlowTaskStatusUpdated = new WorkFlowTaskStatus();
        workFlowTaskStatusUpdated.setWorkflowTaskStatusId(1);
        workFlowTaskStatusUpdated.setWorkflowTaskStatus("on-hold");
        task.setFkWorkflowTaskStatus(workFlowTaskStatusUpdated);

        boolean actual = taskServiceImpl.validateTaskActStartDateTimeForWorkflowStatusOnHold(task);
        assertThat(actual).isEqualTo(false);

    }

    /**
     * Case - when both taskExpStartDate and taskExpStartTime are present.
     * The method which is under test. @link #{validateTaskActStartDateTimeForWorkflowStatusOnHold(Task task)}.
     */
    @Test
    public void testValidateTaskActStartDateTimeForWorkflowStatusOnHold_ElseCase4() {
        Task task = new Task();

        WorkFlowTaskStatus workFlowTaskStatusUpdated = new WorkFlowTaskStatus();
        workFlowTaskStatusUpdated.setWorkflowTaskStatusId(1);
        workFlowTaskStatusUpdated.setWorkflowTaskStatus("on-hold");
        task.setFkWorkflowTaskStatus(workFlowTaskStatusUpdated);

        task.setTaskActStDate(LocalDateTime.now());
        task.setTaskActStTime(LocalTime.now());

        task.setTaskExpStartDate(LocalDateTime.now());
        task.setTaskExpStartTime(LocalTime.now());

        boolean actual = taskServiceImpl.validateTaskActStartDateTimeForWorkflowStatusOnHold(task);
        assertThat(actual).isEqualTo(true);

    }

    /**
     * Case - when all conditions are true.
     * The method which is under test. @link #{validatePreviousWorkflowStatusForBlockedTask(Task taskToUpdate, Task updatedTask)}.
     */
    @Test
    public void testValidatePreviousWorkflowStatusForBlockedTask_Success() {
        Task taskToUpdate = new Task();
        Task updatedTask = new Task();

        WorkFlowTaskStatus workFlowTaskStatusUpdated = new WorkFlowTaskStatus();
        workFlowTaskStatusUpdated.setWorkflowTaskStatusId(1);
        workFlowTaskStatusUpdated.setWorkflowTaskStatus("blocked");
        updatedTask.setFkWorkflowTaskStatus(workFlowTaskStatusUpdated);

        WorkFlowTaskStatus workFlowTaskStatusToUpdate = new WorkFlowTaskStatus();
        workFlowTaskStatusToUpdate.setWorkflowTaskStatusId(1);
        workFlowTaskStatusToUpdate.setWorkflowTaskStatus("completed");
        taskToUpdate.setFkWorkflowTaskStatus(workFlowTaskStatusToUpdate);

        boolean actual = taskServiceImpl.validatePreviousWorkflowStatusForBlockedTask(taskToUpdate, updatedTask);
        assertThat(actual).isEqualTo(true);

    }

    /**
     * Case - when one of the task is null or both tasks are null.
     * The method which is under test. @link #{validatePreviousWorkflowStatusForBlockedTask(Task taskToUpdate, Task updatedTask)}.
     */
    @Test
    public void testValidatePreviousWorkflowStatusForBlockedTask_ElseCase1() {

        boolean actual = taskServiceImpl.validatePreviousWorkflowStatusForBlockedTask(null, null);
        assertThat(actual).isEqualTo(false);

    }

    /**
     * Case - when workflow of the updated task is not blocked.
     * The method which is under test. @link #{validatePreviousWorkflowStatusForBlockedTask(Task taskToUpdate, Task updatedTask)}.
     */
    @Test
    public void testValidatePreviousWorkflowStatusForBlockedTask_ElseCase2() {
        Task taskToUpdate = new Task();
        Task updatedTask = new Task();

        WorkFlowTaskStatus workFlowTaskStatusUpdated = new WorkFlowTaskStatus();
        workFlowTaskStatusUpdated.setWorkflowTaskStatusId(1);
        workFlowTaskStatusUpdated.setWorkflowTaskStatus("backlog");
        updatedTask.setFkWorkflowTaskStatus(workFlowTaskStatusUpdated);

        boolean actual = taskServiceImpl.validatePreviousWorkflowStatusForBlockedTask(taskToUpdate, updatedTask);
        assertThat(actual).isEqualTo(false);

    }

    /**
     * Case - when taskToUpdate task is in backlog.
     * The method which is under test. @link #{validatePreviousWorkflowStatusForBlockedTask(Task taskToUpdate, Task updatedTask)}.
     */
    @Test
    public void testValidatePreviousWorkflowStatusForBlockedTask_ElseCase3() {
        Task taskToUpdate = new Task();
        Task updatedTask = new Task();

        WorkFlowTaskStatus workFlowTaskStatusUpdated = new WorkFlowTaskStatus();
        workFlowTaskStatusUpdated.setWorkflowTaskStatusId(1);
        workFlowTaskStatusUpdated.setWorkflowTaskStatus("blocked");
        updatedTask.setFkWorkflowTaskStatus(workFlowTaskStatusUpdated);

        WorkFlowTaskStatus workFlowTaskStatusToUpdate = new WorkFlowTaskStatus();
        workFlowTaskStatusToUpdate.setWorkflowTaskStatusId(1);
        workFlowTaskStatusToUpdate.setWorkflowTaskStatus("backlog");
        taskToUpdate.setFkWorkflowTaskStatus(workFlowTaskStatusToUpdate);

        boolean actual = taskServiceImpl.validatePreviousWorkflowStatusForBlockedTask(taskToUpdate, updatedTask);
        assertThat(actual).isEqualTo(false);

    }

    /**
     * Case - when all conditions are true.
     * The method which is under test. @link #{validatePriorityForBlockedTask(Task task)}.
     */
    @Test
    public void testValidatePriorityForBlockedTask_Success() {
        Task task = new Task();

        task.setTaskPriority("P0");

        boolean actual = taskServiceImpl.validatePriorityForBlockedTask(task);
        assertThat(actual).isEqualTo(true);
    }

    /**
     * Case - when task is null.
     * The method which is under test. @link #{validatePriorityForBlockedTask(Task task)}.
     */
    @Test
    public void testValidatePriorityForBlockedTask_ElseCase1() {

        boolean actual = taskServiceImpl.validatePriorityForBlockedTask(null);
        assertThat(actual).isEqualTo(false);
    }

    /**
     * Case - when exception is thrown.
     * The method which is under test. @link #{validatePriorityForBlockedTask(Task task)}.
     */
    @Test
    public void testValidatePriorityForBlockedTask_ElseCase2() {
        Task task = new Task();

        try {
            boolean actual = taskServiceImpl.validatePriorityForBlockedTask(task);
        } catch (ValidationFailedException e) {
            assertThat(e.getMessage()).containsIgnoringCase("Task Priority is mandatory for BLOCKED task.");
        }
    }

    /**
     * Case - when all conditions are true.
     * The method which is under test. @link #{validatePriorityForOnHoldTask(Task task)}.
     */
    @Test
    public void testValidatePriorityForOnHoldTask_Success() {
        Task task = new Task();

        task.setTaskPriority("P0");

        boolean actual = taskServiceImpl.validatePriorityForOnHoldTask(task);
        assertThat(actual).isEqualTo(true);
    }

    /**
     * Case - when task is null.
     * The method which is under test. @link #{validatePriorityForOnHoldTask(Task task)}.
     */
    @Test
    public void testValidatePriorityForOnHoldTask_ElseCase1() {

        boolean actual = taskServiceImpl.validatePriorityForOnHoldTask(null);
        assertThat(actual).isEqualTo(false);
    }

    /**
     * Case - when exception is thrown.
     * The method which is under test. @link #{validatePriorityForOnHoldTask(Task task)}.
     */
    @Test
    public void testValidatePriorityForOnHoldTask_ElseCase2() {
        Task task = new Task();

        try {
            boolean actual = taskServiceImpl.validatePriorityForOnHoldTask(task);
        } catch (ValidationFailedException e) {
            assertThat(e.getMessage()).containsIgnoringCase("Task Priority is mandatory for on hold task.");
        }
    }

    /**
     * Case - when all conditions are true. i.e. when priority is null.
     * The method which is under test. @link #{isDefaultPriorityAssignedToCompletedTask(Task taskToUpdate, Task updatedTask)}.
     */
    @Test
    public void testIsDefaultPriorityAssignedToCompletedTask_SuccessCase1() {
        Task taskToUpdate = new Task();
        Task updatedTask = new Task();

        WorkFlowTaskStatus workFlowTaskStatusUpdated = new WorkFlowTaskStatus();
        workFlowTaskStatusUpdated.setWorkflowTaskStatusId(1);
        workFlowTaskStatusUpdated.setWorkflowTaskStatus("completed");
        updatedTask.setFkWorkflowTaskStatus(workFlowTaskStatusUpdated);

        boolean actual = taskServiceImpl.isDefaultPriorityAssignedToCompletedTask(taskToUpdate, updatedTask);
        assertThat(actual).isEqualTo(true);

    }

    /**
     * Case - when all conditions are true. i.e. when priority is null. Checking for the default priority "P3".
     * The method which is under test. @link #{isDefaultPriorityAssignedToCompletedTask(Task taskToUpdate, Task updatedTask)}.
     */
    @Test
    public void testIsDefaultPriorityAssignedToCompletedTask_SuccessCase2() {
        Task taskToUpdate = new Task();
        Task updatedTask = new Task();

        WorkFlowTaskStatus workFlowTaskStatusUpdated = new WorkFlowTaskStatus();
        workFlowTaskStatusUpdated.setWorkflowTaskStatusId(1);
        workFlowTaskStatusUpdated.setWorkflowTaskStatus("completed");
        updatedTask.setFkWorkflowTaskStatus(workFlowTaskStatusUpdated);

        boolean actual = taskServiceImpl.isDefaultPriorityAssignedToCompletedTask(taskToUpdate, updatedTask);
        assertThat(actual).isEqualTo(true);
        assertThat(updatedTask.getTaskPriority()).containsIgnoringCase("P3");

    }

    /**
     * Case - when one of the task is null or both tasks are null.
     * The method which is under test. @link #{isDefaultPriorityAssignedToCompletedTask(Task taskToUpdate, Task updatedTask)}.
     */
    @Test
    public void testIsDefaultPriorityAssignedToCompletedTask_ElseCase1() {

        boolean actual = taskServiceImpl.isDefaultPriorityAssignedToCompletedTask(null, null);
        assertThat(actual).isEqualTo(false);

    }

    /**
     * Case - when updated task is not completed.
     * The method which is under test. @link #{isDefaultPriorityAssignedToCompletedTask(Task taskToUpdate, Task updatedTask)}.
     */
    @Test
    public void testIsDefaultPriorityAssignedToCompletedTask_ElseCase2() {
        Task taskToUpdate = new Task();
        Task updatedTask = new Task();

        WorkFlowTaskStatus workFlowTaskStatusUpdated = new WorkFlowTaskStatus();
        workFlowTaskStatusUpdated.setWorkflowTaskStatusId(1);
        workFlowTaskStatusUpdated.setWorkflowTaskStatus("started");
        updatedTask.setFkWorkflowTaskStatus(workFlowTaskStatusUpdated);

        boolean actual = taskServiceImpl.isDefaultPriorityAssignedToCompletedTask(taskToUpdate, updatedTask);
        assertThat(actual).isEqualTo(false);

    }

    /**
     * Case - when priority is present.
     * The method which is under test. @link #{isDefaultPriorityAssignedToCompletedTask(Task taskToUpdate, Task updatedTask)}.
     */
    @Test
    public void testIsDefaultPriorityAssignedToCompletedTask_ElseCase3() {
        Task taskToUpdate = new Task();
        Task updatedTask = new Task();

        WorkFlowTaskStatus workFlowTaskStatusUpdated = new WorkFlowTaskStatus();
        workFlowTaskStatusUpdated.setWorkflowTaskStatusId(1);
        workFlowTaskStatusUpdated.setWorkflowTaskStatus("completed");
        updatedTask.setFkWorkflowTaskStatus(workFlowTaskStatusUpdated);

        updatedTask.setTaskPriority("P0");

        boolean actual = taskServiceImpl.isDefaultPriorityAssignedToCompletedTask(taskToUpdate, updatedTask);
        assertThat(actual).isEqualTo(true);

    }

    /**
     * Case - when all conditions are true.
     * The method which is under test. @link #{validateTaskActStartDateTimeForWorkflowStatusBacklog(Task task)}.
     */
    @Test
    public void testValidateTaskActStartDateTimeForWorkflowStatusBacklog_Success() {
        Task task = new Task();

        WorkFlowTaskStatus workFlowTaskStatusUpdated = new WorkFlowTaskStatus();
        workFlowTaskStatusUpdated.setWorkflowTaskStatusId(1);
        workFlowTaskStatusUpdated.setWorkflowTaskStatus("backlog");
        task.setFkWorkflowTaskStatus(workFlowTaskStatusUpdated);

        boolean actual = taskServiceImpl.validateTaskActStartDateTimeForWorkflowStatusBacklog(task);
        assertThat(actual).isEqualTo(true);
    }

    /**
     * Case - when task is null.
     * The method which is under test. @link #{validateTaskActStartDateTimeForWorkflowStatusBacklog(Task task)}.
     */
    @Test
    public void testValidateTaskActStartDateTimeForWorkflowStatusBacklog_ElseCase1() {

        boolean actual = taskServiceImpl.validateTaskActStartDateTimeForWorkflowStatusBacklog(null);
        assertThat(actual).isEqualTo(false);
    }

    /**
     * Case - when workflow status of the task is not backlog.
     * The method which is under test. @link #{validateTaskActStartDateTimeForWorkflowStatusBacklog(Task task)}.
     */
    @Test
    public void testValidateTaskActStartDateTimeForWorkflowStatusBacklog_ElseCase2() {
        Task task = new Task();

        WorkFlowTaskStatus workFlowTaskStatusUpdated = new WorkFlowTaskStatus();
        workFlowTaskStatusUpdated.setWorkflowTaskStatusId(1);
        workFlowTaskStatusUpdated.setWorkflowTaskStatus("completed.");
        task.setFkWorkflowTaskStatus(workFlowTaskStatusUpdated);

        boolean actual = taskServiceImpl.validateTaskActStartDateTimeForWorkflowStatusBacklog(task);
        assertThat(actual).isEqualTo(false);
    }

    /**
     * Case - when one of the taskActStDate or taskActStTime is present or both are present.
     * The method which is under test. @link #{validateTaskActStartDateTimeForWorkflowStatusBacklog(Task task)}.
     */
    @Test
    public void testValidateTaskActStartDateTimeForWorkflowStatusBacklog_ElseCase3() {
        Task task = new Task();

        WorkFlowTaskStatus workFlowTaskStatusUpdated = new WorkFlowTaskStatus();
        workFlowTaskStatusUpdated.setWorkflowTaskStatusId(1);
        workFlowTaskStatusUpdated.setWorkflowTaskStatus("backlog");
        task.setFkWorkflowTaskStatus(workFlowTaskStatusUpdated);

        task.setTaskActStDate(LocalDateTime.now());
        task.setTaskActStTime(LocalTime.now());

        boolean actual = taskServiceImpl.validateTaskActStartDateTimeForWorkflowStatusBacklog(task);
        assertThat(actual).isEqualTo(false);
    }

    /**
     * Case - when all conditions are true.
     * The method which is under test. @link #{validateTaskActStartDateTimeForWorkflowStatusDelete(Task task)}.
     */
    @Test
    public void testValidateTaskActStartDateTimeForWorkflowStatusDelete_Success() {
        Task task = new Task();

        WorkFlowTaskStatus workFlowTaskStatusUpdated = new WorkFlowTaskStatus();
        workFlowTaskStatusUpdated.setWorkflowTaskStatusId(1);
        workFlowTaskStatusUpdated.setWorkflowTaskStatus("deleted");
        task.setFkWorkflowTaskStatus(workFlowTaskStatusUpdated);

        boolean actual = taskServiceImpl.validateTaskActStartDateTimeForWorkflowStatusDelete(task);
        assertThat(actual).isEqualTo(true);
    }

    /**
     * Case - when task is null.
     * The method which is under test. @link #{validateTaskActStartDateTimeForWorkflowStatusDelete(Task task)}.
     */
    @Test
    public void testValidateTaskActStartDateTimeForWorkflowStatusDelete_ElseCase1() {

        boolean actual = taskServiceImpl.validateTaskActStartDateTimeForWorkflowStatusDelete(null);
        assertThat(actual).isEqualTo(false);
    }

    /**
     * Case - when workflow status of the task is not deleted.
     * The method which is under test. @link #{validateTaskActStartDateTimeForWorkflowStatusDelete(Task task)}.
     */
    @Test
    public void testValidateTaskActStartDateTimeForWorkflowStatusDelete_ElseCase2() {
        Task task = new Task();

        WorkFlowTaskStatus workFlowTaskStatusUpdated = new WorkFlowTaskStatus();
        workFlowTaskStatusUpdated.setWorkflowTaskStatusId(1);
        workFlowTaskStatusUpdated.setWorkflowTaskStatus("started");
        task.setFkWorkflowTaskStatus(workFlowTaskStatusUpdated);

        boolean actual = taskServiceImpl.validateTaskActStartDateTimeForWorkflowStatusDelete(task);
        assertThat(actual).isEqualTo(false);
    }

    /**
     * Case - when one of the taskActStDate or taskActStTime is present or both are present.
     * The method which is under test. @link #{validateTaskActStartDateTimeForWorkflowStatusDelete(Task task)}.
     */
    @Test
    public void testValidateTaskActStartDateTimeForWorkflowStatusDelete_ElseCase3() {
        Task task = new Task();

        WorkFlowTaskStatus workFlowTaskStatusUpdated = new WorkFlowTaskStatus();
        workFlowTaskStatusUpdated.setWorkflowTaskStatusId(1);
        workFlowTaskStatusUpdated.setWorkflowTaskStatus("deleted");
        task.setFkWorkflowTaskStatus(workFlowTaskStatusUpdated);

        task.setTaskActStDate(LocalDateTime.now());
        task.setTaskActStTime(LocalTime.now());

        boolean actual = taskServiceImpl.validateTaskActStartDateTimeForWorkflowStatusDelete(task);
        assertThat(actual).isEqualTo(false);
    }

    /** This method tests sendFcmNotification method for all valid values */
    @Test
    public void testSendFcmNotification_retrievesCorrectFirebaseTokens() throws FirebaseMessagingException {
        Long userId = 1L;
        FirebaseToken firebaseToken = new FirebaseToken();
        firebaseToken.setToken("test_token");
        List<FirebaseToken> firebaseTokenList = List.of(firebaseToken);
        HashMap<String, String> payload = new HashMap<>();
        payload.put("accountId", "1");

        when(firebaseTokenService.getFirebaseTokenByUserId(userId)).thenReturn(firebaseTokenList);
        when(fcmService.sendMessageToToken(any(PushNotificationRequest.class))).thenReturn("dummy string");
        taskServiceImpl.sendFcmNotification(userId, payload);

        verify(firebaseTokenService).getFirebaseTokenByUserId(userId);
        verify(fcmService).sendMessageToToken(any(PushNotificationRequest.class));
    }

    /** This method tests sendFcmNotification method for exception condition */
    @Test(expected = Exception.class)
    public void testSendFcmNotification_firebaseMessagingException() throws FirebaseMessagingException {
        Long userId = 1L;
        HashMap<String, String> payload = new HashMap<>();
        FirebaseToken firebaseToken = new FirebaseToken();
        firebaseToken.setToken("test_token");
        List<FirebaseToken> firebaseTokenList = List.of(firebaseToken);
        when(fcmService.sendMessageToToken(any(PushNotificationRequest.class))).thenThrow(new Exception());
        taskServiceImpl.sendFcmNotification(userId, payload);
    }

    /** test sendPushNotification section in addTaskInTaskTable method */
    @Test
    public void testAddTaskInTaskTable_sendPushNotification(){

        Task task = new Task();
        task.setTaskNumber("10L");
        task.setTaskDesc("Test task");

        UserAccount userAccountLastUpdated = new UserAccount();
        User userLastUpdated = new User();
        userLastUpdated.setUserId(1L);
        userAccountLastUpdated.setFkUserId(userLastUpdated);
        userAccountLastUpdated.setAccountId(1L);

        UserAccount userAccountAssigned = new UserAccount();
        User userAssigned = new User();
        userAssigned.setUserId(2L);
        userAccountAssigned.setFkUserId(userAssigned);
        userAccountAssigned.setAccountId(2L);

        HashMap<String, String> payload = new HashMap<>();
        payload.put("accountId", "1");
        List<HashMap<String, String>> listOfPayload = List.of(payload);

        task.setFkAccountIdLastUpdated(userAccountLastUpdated);
        task.setFkAccountIdAssigned(userAccountAssigned);

        when(taskRepository.save(task)).thenReturn(task);
        when(userAccountService.getActiveUserAccountByAccountId(1L)).thenReturn(userAccountLastUpdated);
        when(userAccountService.getActiveUserAccountByAccountId(2L)).thenReturn(userAccountAssigned);
        when(notificationService.createNewTaskNotification(task)).thenReturn(listOfPayload);
        doNothing().when(taskServiceImpl).setBUToTask(task);
        doNothing().when(taskServiceImpl).sendPushNotification(listOfPayload);
        Audit audit = new Audit();
        when(auditService.createAudit(task, 2, null, null)).thenReturn(audit);
        when(auditRepository.save(audit)).thenReturn(audit);

        taskServiceImpl.addTaskInTaskTable(task, "Asia/Kolkata");
        verify(taskServiceImpl).sendPushNotification(listOfPayload);
    }

    @Test
    public void testSendPushNotification() throws FirebaseNotificationException {

        List<HashMap<String, String>> payloadList = new ArrayList<>();

        HashMap<String, String> payload1 = new HashMap<>();
        payload1.put("accountId", "1");
        payloadList.add(payload1);

        HashMap<String, String> payload2 = new HashMap<>();
        payload2.put("accountId", "2");
        payloadList.add(payload2);

        UserAccount userAccount1 = new UserAccount();
        userAccount1.setAccountId(1L);
        User user1 = new User();
        user1.setUserId(10L);
        userAccount1.setFkUserId(user1);

        UserAccount userAccount2 = new UserAccount();
        userAccount2.setAccountId(2L);
        User user2 = new User();
        user2.setUserId(20L);
        userAccount2.setFkUserId(user2);

        when(userAccountRepository.findByAccountId(1L)).thenReturn(userAccount1);
        when(userAccountRepository.findByAccountId(2L)).thenReturn(userAccount2);

        taskServiceImpl.sendPushNotification(payloadList);
        verify(taskServiceImpl, times(1)).sendFcmNotification(10L, payload1);
        verify(taskServiceImpl, times(1)).sendFcmNotification(20L, payload2);
    }


}
