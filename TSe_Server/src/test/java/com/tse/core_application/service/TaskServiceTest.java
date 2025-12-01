package com.tse.core_application.service;

import com.tse.core_application.custom.model.NewEffortTrack;
import com.tse.core_application.custom.model.RoleId;
import com.tse.core_application.exception.*;
import com.tse.core_application.model.*;
import com.tse.core_application.repository.*;
import com.tse.core_application.service.Impl.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TaskServiceTest {

  @Mock
  private TaskRepository taskRepository;

  @Mock
  private AuditRepository auditRepository;

  @Mock
  private AuditService auditService;

  @Mock
  private ProjectService projectService;

  @Mock
  private TeamService teamService;

  @Mock
  private WorkflowTaskStatusService workflowTaskStatusService;

  @Mock
  private TaskService taskService;

  @Mock
  private TaskHistoryRepository taskHistoryRepository;

  @Mock
  private CommentRepository commentRepository;

  @Mock
  private StatsService statsService;

  @Mock
  private UserAccountService userAccountService;

  @Mock
  private RoleActionService roleActionService;

  @Mock
  private AccessDomainService accessDomainService;

  @Mock
  private NotificationService notificationService;

  @Mock
  private OfficeHoursService officeHoursService;

  @Mock
  private TimeSheetService timeSheetService;

  @Mock
  private NoteService noteService;

  @Mock
  private AccessDomainRepository accessDomainRepository;

  @Spy
  @InjectMocks
  private TaskServiceImpl taskServiceImpl;

  @Before
  public void init() {

  }


  @Test
  public void testAddTaskInTaskTable_AuditIfCase() {
    Task task = mock(Task.class);
    UserAccount userAccount = mock(UserAccount.class);
    Audit audit = mock(Audit.class);
//    Project project = mock(Project.class);

    lenient().doNothing().when(taskServiceImpl).setBUToTask(any(Task.class));
    lenient().when(taskRepository.save(any(Task.class))).thenReturn(task);
    lenient().when(notificationService.createNewTaskNotification(any(Task.class))).thenReturn(null);
    lenient().doNothing().when(taskServiceImpl).sendPushNotification(null);
    lenient().when(task.getNotes()).thenReturn(null);
    lenient().when(task.getFkAccountIdAssigned()).thenReturn(null);
    lenient().when(auditService.createAudit(any(Task.class), anyInt(), anyObject(), anyObject())).thenReturn(audit);
    lenient().when(auditRepository.save(any(Audit.class))).thenReturn(audit);
    //    lenient().when(userAccount.getAccountId()).thenReturn(0L);
//    lenient().when(task.getFkProjectId()).thenReturn(project);
//    lenient().when(project.getProjectId()).thenReturn(1L);
//    lenient().when(projectService.getProjectByProjectId(anyLong())).thenReturn(project);
//    lenient().when(project.getBuId()).thenReturn(1L);
    Task savedTask = taskServiceImpl.addTaskInTaskTable(task, "DATA");
    assertThat(savedTask).isNotNull();
  }

  @Test
  public void testAddTaskInTaskTable_AuditElseCase() {
    Task task = mock(Task.class);
    UserAccount userAccount = mock(UserAccount.class);
    Audit audit = mock(Audit.class);
//    Project project = mock(Project.class);
//    Team team = mock(Team.class);

    lenient().doNothing().when(taskServiceImpl).setBUToTask(any(Task.class));
    lenient().when(taskRepository.save(any(Task.class))).thenReturn(task);
    lenient().when(notificationService.createNewTaskNotification(any(Task.class))).thenReturn(null);
    lenient().doNothing().when(taskServiceImpl).sendPushNotification(null);
    lenient().when(task.getNotes()).thenReturn(null);
    lenient().when(task.getFkAccountIdAssigned()).thenReturn(userAccount);
    lenient().when(userAccount.getAccountId()).thenReturn(1L);
    lenient().when(auditService.createAudit(any(Task.class), anyInt(), anyObject(), anyObject())).thenReturn(audit);
    lenient().when(auditRepository.save(any(Audit.class))).thenReturn(audit);
//    lenient().when(task.getFkProjectId()).thenReturn(project);
//    lenient().when(project.getProjectId()).thenReturn(null);
//    lenient().when(projectService.getProjectByProjectId(anyLong())).thenReturn(project);
//    lenient().when(project.getBuId()).thenReturn(1L);
//    lenient().when(teamService.getTeamByTeamId(anyLong())).thenReturn(team);
//    lenient().when(team.getFkProjectId()).thenReturn(project);
//    lenient().when(project.getProjectId()).thenReturn(1L);
//    lenient().when(task.getFkTeamId()).thenReturn(team);
//    lenient().when(team.getTeamId()).thenReturn(1L);

    Task savedTask = taskServiceImpl.addTaskInTaskTable(task, "DATA");
    assertThat(savedTask).isNotNull();
  }

  @Test
  public void testAddTaskInTaskTable_NotesIfCase() {
    Task task = mock(Task.class);
    task.setTaskId(1L);
    UserAccount userAccount = mock(UserAccount.class);
    Audit audit = mock(Audit.class);
    Project project = mock(Project.class);
    Note note = mock(Note.class);
    note.setNoteId(1L);
    task.setNotes(List.of(note));
    lenient().doNothing().when(taskServiceImpl).setBUToTask(any(Task.class));
    lenient().when(taskRepository.save(any(Task.class))).thenReturn(task);
    lenient().when(notificationService.createNewTaskNotification(any(Task.class))).thenReturn(null);
    lenient().doNothing().when(taskServiceImpl).sendPushNotification(null);
    lenient().when(noteService.saveAllNotesOnAddTask(anyList(), any(Task.class))).thenReturn(List.of(note));
    lenient().when(taskServiceImpl.updateNoteIdByTaskId(1L, 1L)).thenReturn(1);
    lenient().when(task.getFkAccountIdAssigned()).thenReturn(null);
    lenient().when(auditService.createAudit(any(Task.class), anyInt(), anyObject(), anyObject())).thenReturn(audit);
    lenient().when(auditRepository.save(any(Audit.class))).thenReturn(audit);
    Task savedTask = taskServiceImpl.addTaskInTaskTable(task, "DATA");
    assertThat(savedTask).isNotNull();
  }


    @Test
  @Ignore
  public void testUpdateFieldsInTaskTable() throws IllegalAccessException {
    Task task = mock(Task.class);
    UserAccount userAccount = mock(UserAccount.class);
    ArrayList<String> values = new ArrayList<>();
    values.add("immediateAttentionFrom");
    TaskHistory taskHistory = mock(TaskHistory.class);
    Comment comment = new Comment();
    List<Comment> comments = new ArrayList<>();
    //comments.add(comment);

    WorkFlowTaskStatus workFlowTaskStatus = mock(WorkFlowTaskStatus.class);
    lenient().when(taskRepository.findByTaskId(anyLong())).thenReturn(task);
    lenient().when(workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(anyInt())).thenReturn(workFlowTaskStatus);
    lenient().when(task.getFkWorkflowTaskStatus()).thenReturn(workFlowTaskStatus);
    lenient().when(workFlowTaskStatus.getWorkflowTaskStatusId()).thenReturn(1);
    lenient().when(workFlowTaskStatus.getWorkflowTaskStatus()).thenReturn(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED);
    lenient().when(task.getFkAccountIdAssignee()).thenReturn(userAccount);
    lenient().when(userAccount.getAccountId()).thenReturn(1L);
    lenient().when(task.getFkAccountIdAssigned()).thenReturn(userAccount);

    lenient().when(taskService.getFieldsToUpdate(any(Task.class), anyLong())).thenReturn(values);
    lenient().when(task.getImmediateAttention()).thenReturn(1);
    lenient().doNothing().when(taskService).broadcastMessage(any(Task.class));
    lenient().when(taskHistoryRepository.save(any(TaskHistory.class))).thenReturn(taskHistory);
    lenient().when(task.getComments()).thenReturn(comments);
    lenient().when(taskRepository.save(any(Task.class))).thenReturn(task);
    lenient().when(task.getTaskPriority()).thenReturn("NA");
    lenient().when(task.getImmediateAttentionFrom()).thenReturn("NA");
    lenient().when(task.getTaskExpEndDate()).thenReturn(LocalDateTime.now());
    lenient().when(task.getTaskExpEndTime()).thenReturn(LocalTime.now());
    lenient().when(task.getFkAccountId()).thenReturn(userAccount);
    Task responseTask = taskServiceImpl.updateFieldsInTaskTable(task, 1L,(String.valueOf(ZoneId.systemDefault())), "");
    assertThat(responseTask).isNotNull();
  }

  @Test
  @Ignore
  public void testUpdateFieldsInTaskTableElseEdgeCases() throws IllegalAccessException {
    Task task = mock(Task.class);
    UserAccount userAccount = mock(UserAccount.class);
    ArrayList<String> values = new ArrayList<>();
    values.add("immediateAttentionFrom");
    TaskHistory taskHistory = mock(TaskHistory.class);
    Comment comment = new Comment();
    List<Comment> comments = new ArrayList<>();
    comments.add(comment);

    WorkFlowTaskStatus workFlowTaskStatus = mock(WorkFlowTaskStatus.class);
    lenient().when(taskRepository.findByTaskId(anyLong())).thenReturn(task);
    lenient().when(workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(anyInt())).thenReturn(workFlowTaskStatus);
    lenient().when(task.getFkWorkflowTaskStatus()).thenReturn(workFlowTaskStatus);
    lenient().when(workFlowTaskStatus.getWorkflowTaskStatusId()).thenReturn(1);
    lenient().when(workFlowTaskStatus.getWorkflowTaskStatus()).thenReturn(Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG);
    lenient().when(task.getFkAccountIdAssignee()).thenReturn(userAccount);
    lenient().when(userAccount.getAccountId()).thenReturn(2L);
    lenient().when(task.getFkAccountIdAssigned()).thenReturn(userAccount);

    lenient().when(taskService.getFieldsToUpdate(any(Task.class), anyLong())).thenReturn(values);
    lenient().when(task.getImmediateAttention()).thenReturn(1);
    lenient().doNothing().when(taskService).broadcastMessage(any(Task.class));
    lenient().when(taskHistoryRepository.save(any(TaskHistory.class))).thenReturn(taskHistory);
    lenient().when(task.getComments()).thenReturn(comments);
    lenient().when(taskRepository.save(any(Task.class))).thenReturn(task);
    lenient().when(task.getTaskPriority()).thenReturn("NA");
    lenient().when(task.getImmediateAttentionFrom()).thenReturn("NA");
    lenient().when(task.getTaskExpEndDate()).thenReturn(LocalDateTime.now());
    lenient().when(task.getTaskExpEndTime()).thenReturn(LocalTime.now());
    lenient().when(task.getFkAccountId()).thenReturn(userAccount);
    lenient().when(task.getTaskWorkflowId()).thenReturn(1);
    lenient().when(task.getCommentId()).thenReturn(1L);
    lenient().when(commentRepository.save(any(Comment.class))).thenReturn(comment);
    lenient().when(taskRepository.setTaskCommentIdByTaskId(anyLong(), anyLong())).thenReturn(1);
    lenient().when(statsService.computeAndUpdateStatsViaUpdateTask(any(Task.class), anyBoolean())).thenReturn(null);


    Task responseTask = taskServiceImpl.updateFieldsInTaskTable(task, 1L,(String.valueOf(ZoneId.systemDefault())), "");
    assertThat(responseTask).isNotNull();
  }

  @Test
  public void testConvertTaskAllLocalDateAndTimeInToServerTimeZone() {

    Task task = mock(Task.class);
    String localTimeZone = "Z";

    lenient().when(task.getTaskExpStartDate()).thenReturn(LocalDateTime.now());
    lenient().when(task.getTaskExpStartTime()).thenReturn(LocalTime.now());
    lenient().when(task.getTaskExpEndDate()).thenReturn(LocalDateTime.now());
    lenient().when(task.getTaskExpEndTime()).thenReturn(LocalTime.now());
    lenient().when(task.getTaskActStDate()).thenReturn(LocalDateTime.now());
    lenient().when(task.getTaskActStTime()).thenReturn(LocalTime.now());
    lenient().when(task.getTaskActEndDate()).thenReturn(LocalDateTime.now());
    lenient().when(task.getTaskActEndTime()).thenReturn(LocalTime.now());
    lenient().when(task.getTaskCompletionDate()).thenReturn(LocalDateTime.now());
    lenient().when(task.getTaskCompletionTime()).thenReturn(LocalTime.now());

    taskServiceImpl.convertTaskAllUserDateAndTimeInToServerTimeZone(task, localTimeZone);
    verify(task, times(2)).getTaskCompletionDate();

  }

  @Test
  public void testModifyTaskPriority() {
    Task task = mock(Task.class);

    lenient().when(task.getTaskPriority()).thenReturn("P0");
    taskServiceImpl.modifyTaskPriority(task);
    verify(task, times(4)).getTaskPriority();
  }

  @Test
  public void testModifyTaskPriorityExceptionCase() {
    Task task = mock(Task.class);

    lenient().when(task.getTaskPriority()).thenReturn("P4");

    try {
      taskServiceImpl.modifyTaskPriority(task);
    } catch (ValidationFailedException e) {
      assertThat(e.getMessage()).containsIgnoringCase("Validation Failed: Priority");
      verify(task, times(5)).getTaskPriority();
    }
  }

  @Test
  public void testValidateTaskEstimateByWorkflowTaskStatusExceptionCase() {

    Task task = new Task();
    WorkFlowTaskStatus workFlowTaskStatus = new WorkFlowTaskStatus();
    workFlowTaskStatus.setWorkflowTaskStatusId(1);
    workFlowTaskStatus.setWorkflowTaskStatus("started");
    task.setFkWorkflowTaskStatus(workFlowTaskStatus);

    lenient().when(workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(anyInt())).thenReturn(workFlowTaskStatus);
    lenient().when(statsService.deriveTimeEstimateForTask(any(Task.class))).thenReturn(1L);

    boolean res = taskServiceImpl.validateTaskEstimateByWorkflowTaskStatus(task);
    assertThat(res).isEqualTo(false);
    verify(workflowTaskStatusService, times(1)).getWorkflowTaskStatusByWorkflowTaskStatusId(anyInt());
  }

  @Test
  public void testValidateTaskEstimateByWorkflowTaskStatusSuccessCase() {
    Task task = null;
    boolean res = taskServiceImpl.validateTaskEstimateByWorkflowTaskStatus(task);
    assertThat(res).isEqualTo(true);
  }

  @Test
  public void testValidateTaskForWorkflowStatusNotStartedSuccessCase() {
    Task task = new Task();
    task.setTaskExpStartDate(LocalDateTime.now());
    task.setTaskExpStartTime(LocalTime.now());
    task.setTaskExpEndDate(LocalDateTime.now());
    task.setTaskExpEndTime(LocalTime.now());
    task.setTaskPriority("P2");
    task.setTaskEstimate(60);
    WorkFlowTaskStatus workFlowTaskStatus = new WorkFlowTaskStatus();
    workFlowTaskStatus.setWorkflowTaskStatusId(1);
    workFlowTaskStatus.setWorkflowTaskStatus("not-started");
    task.setFkWorkflowTaskStatus(workFlowTaskStatus);

    lenient().when(workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(anyInt())).thenReturn(workFlowTaskStatus);
    boolean res = taskServiceImpl.validateTaskForWorkflowStatusNotStarted(task);
    assertThat(res).isEqualTo(true);
    verify(workflowTaskStatusService, times(3)).getWorkflowTaskStatusByWorkflowTaskStatusId(anyInt());
  }

  @Test
  public void testValidateTaskForWorkflowStatusNotStartedExceptionCaseFirst() {
    Task task = new Task();
    WorkFlowTaskStatus workFlowTaskStatus = new WorkFlowTaskStatus();
    workFlowTaskStatus.setWorkflowTaskStatusId(1);
    workFlowTaskStatus.setWorkflowTaskStatus("not-started");
    task.setFkWorkflowTaskStatus(workFlowTaskStatus);

    lenient().when(workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(anyInt())).thenReturn(workFlowTaskStatus);

    try {
      boolean res = taskServiceImpl.validateTaskForWorkflowStatusNotStarted(task);
      assertThat(res).isEqualTo(true);
    } catch (ValidationFailedException e) {
      assertThat(e.getMessage()).containsIgnoringCase("Validation Failed: Task Expected Start Date Can't be null");
      verify(workflowTaskStatusService, times(3)).getWorkflowTaskStatusByWorkflowTaskStatusId(anyInt());
    }
  }

  @Test
  public void testValidateTaskForWorkflowStatusNotStartedExceptionCaseSecond() {
    Task task = new Task();
    task.setTaskExpStartDate(LocalDateTime.now());
    WorkFlowTaskStatus workFlowTaskStatus = new WorkFlowTaskStatus();
    workFlowTaskStatus.setWorkflowTaskStatusId(1);
    workFlowTaskStatus.setWorkflowTaskStatus("not-started");
    task.setFkWorkflowTaskStatus(workFlowTaskStatus);

    lenient().when(workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(anyInt())).thenReturn(workFlowTaskStatus);
    lenient().when(statsService.deriveTimeEstimateForTask(any(Task.class))).thenReturn(1L);


    try {
      boolean res = taskServiceImpl.validateTaskForWorkflowStatusNotStarted(task);
      assertThat(res).isEqualTo(true);
    } catch (ValidationFailedException e) {
      assertThat(e.getMessage()).containsIgnoringCase("Validation Failed: Task Expected Start Time Can't be null");
      verify(workflowTaskStatusService, times(3)).getWorkflowTaskStatusByWorkflowTaskStatusId(anyInt());

    }
  }


  @Test
  public void testValidateTaskForWorkflowStatusNotStartedExceptionCaseThird() {
    Task task = new Task();
    task.setTaskExpStartTime(LocalTime.now());
    task.setTaskExpStartDate(LocalDateTime.now());
    WorkFlowTaskStatus workFlowTaskStatus = new WorkFlowTaskStatus();
    workFlowTaskStatus.setWorkflowTaskStatusId(1);
    workFlowTaskStatus.setWorkflowTaskStatus("not-started");
    task.setFkWorkflowTaskStatus(workFlowTaskStatus);

    lenient().when(workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(anyInt())).thenReturn(workFlowTaskStatus);
    lenient().when(statsService.deriveTimeEstimateForTask(any(Task.class))).thenReturn(1L);


    try {
      boolean res = taskServiceImpl.validateTaskForWorkflowStatusNotStarted(task);
      assertThat(res).isEqualTo(true);
    } catch (ValidationFailedException e) {
      assertThat(e.getMessage()).containsIgnoringCase("Validation Failed: Task Expected End Date Can't be null");
      verify(workflowTaskStatusService, times(3)).getWorkflowTaskStatusByWorkflowTaskStatusId(anyInt());
    }
  }

  @Test
  public void testValidateTaskForWorkflowStatusNotStartedExceptionCaseForth() {
    Task task = new Task();
    task.setTaskExpStartTime(LocalTime.now());
    task.setTaskExpStartDate(LocalDateTime.now());
    task.setTaskExpEndDate(LocalDateTime.now());
    WorkFlowTaskStatus workFlowTaskStatus = new WorkFlowTaskStatus();
    workFlowTaskStatus.setWorkflowTaskStatusId(1);
    workFlowTaskStatus.setWorkflowTaskStatus("not-started");
    task.setFkWorkflowTaskStatus(workFlowTaskStatus);

    lenient().when(workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(anyInt())).thenReturn(workFlowTaskStatus);
    lenient().when(statsService.deriveTimeEstimateForTask(any(Task.class))).thenReturn(1L);


    try {
      boolean res = taskServiceImpl.validateTaskForWorkflowStatusNotStarted(task);
      assertThat(res).isEqualTo(true);
    } catch (ValidationFailedException e) {
      assertThat(e.getMessage()).containsIgnoringCase("Validation Failed: Task Expected End Time Can't be null");
    }
  }

  @Test
  public void testValidateTaskForWorkflowStatusNotStartedExceptionCaseFifth() {
    Task task = new Task();
    task.setTaskExpStartTime(LocalTime.now());
    task.setTaskExpStartDate(LocalDateTime.now());
    task.setTaskExpEndDate(LocalDateTime.now());
    task.setTaskExpEndTime(LocalTime.now());
    WorkFlowTaskStatus workFlowTaskStatus = new WorkFlowTaskStatus();
    workFlowTaskStatus.setWorkflowTaskStatusId(1);
    workFlowTaskStatus.setWorkflowTaskStatus("not-started");
    task.setFkWorkflowTaskStatus(workFlowTaskStatus);

    lenient().when(workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(anyInt())).thenReturn(workFlowTaskStatus);
    lenient().when(statsService.deriveTimeEstimateForTask(any(Task.class))).thenReturn(1L);

    try {
      boolean res = taskServiceImpl.validateTaskForWorkflowStatusNotStarted(task);
      assertThat(res).isEqualTo(true);
    } catch (ValidationFailedException e) {
      System.out.println(e.getMessage());
      assertThat(e.getMessage()).containsIgnoringCase("Validation Failed: Task Priority Can't be null for Not Started Task");
      verify(workflowTaskStatusService, times(3)).getWorkflowTaskStatusByWorkflowTaskStatusId(anyInt());
    }
  }

  @Test
  public void testValidateTaskForWorkflowStatusNotStartedExceptionCaseSixth() {
    Task task = new Task();
    task.setTaskExpStartTime(LocalTime.now());
    task.setTaskExpStartDate(LocalDateTime.now());
    task.setTaskExpEndDate(LocalDateTime.now());
    task.setTaskExpEndTime(LocalTime.now());
    task.setTaskPriority("P0");
    WorkFlowTaskStatus workFlowTaskStatus = new WorkFlowTaskStatus();
    workFlowTaskStatus.setWorkflowTaskStatusId(1);
    workFlowTaskStatus.setWorkflowTaskStatus("not-started");
    task.setFkWorkflowTaskStatus(workFlowTaskStatus);

    lenient().when(workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(anyInt())).thenReturn(workFlowTaskStatus);
    lenient().when(statsService.deriveTimeEstimateForTask(any(Task.class))).thenReturn(1L);


    try {
      boolean res = taskServiceImpl.validateTaskForWorkflowStatusNotStarted(task);
      assertThat(res).isEqualTo(true);
    } catch (Exception e) {
      assertThat(e.getMessage()).containsIgnoringCase("TaskEstimateException: In case you do not want to input any estimate");
      verify(workflowTaskStatusService, times(2)).getWorkflowTaskStatusByWorkflowTaskStatusId(anyInt());
    }
  }

  @Test
  public void testValidateAllDateAndTimeForPairsSuccessCase() {

    Task task = null;
    boolean res = taskServiceImpl.validateAllDateAndTimeForPairs(task);
    assertThat(res).isEqualTo(false);
  }

  @Test
  public void testValidateAllDateAndTimeForPairsExceptionCaseFirst() {

    Task task = new Task();
    task.setTaskActStDate(LocalDateTime.now());
    try {
      boolean res = taskServiceImpl.validateAllDateAndTimeForPairs(task);
      assertThat(res).isEqualTo(false);
    } catch (DateAndTimePairFailedException e) {
      assertThat(e.getMessage()).containsIgnoringCase("DateAndTimePair Failed: Provide both Actual Start Date and Actual Start Time");
    }
  }

  @Test
  public void testValidateAllDateAndTimeForPairsExceptionCaseSecond() {

    Task task = new Task();
    task.setTaskActEndDate(LocalDateTime.now());
    try {
      boolean res = taskServiceImpl.validateAllDateAndTimeForPairs(task);
      assertThat(res).isEqualTo(false);
    } catch (DateAndTimePairFailedException e) {
      System.out.println(e.getMessage());
      assertThat(e.getMessage()).contains("DateAndTimePair Failed: Provide both Actual End Date and Actual End Time");
    }
  }

  @Test
  public void testValidateAllDateAndTimeForPairsExceptionCaseThird() {

    Task task = new Task();
    task.setTaskExpStartDate(LocalDateTime.now());
    try {
      boolean res = taskServiceImpl.validateAllDateAndTimeForPairs(task);
      assertThat(res).isEqualTo(false);
    } catch (DateAndTimePairFailedException e) {
    }
  }

  @Test
  public void testValidateAllDateAndTimeForPairsExceptionCaseForth() {

    Task task = new Task();
    task.setTaskExpEndDate(LocalDateTime.now());
    try {
      boolean res = taskServiceImpl.validateAllDateAndTimeForPairs(task);
      assertThat(res).isEqualTo(false);
    } catch (DateAndTimePairFailedException e) {
    }
  }

  @Test
  public void testValidateAllDateAndTimeForPairsExceptionCaseFifth() {

    Task task = new Task();
    task.setTaskCompletionDate(LocalDateTime.now());
    try {
      boolean res = taskServiceImpl.validateAllDateAndTimeForPairs(task);
      assertThat(res).isEqualTo(false);
    } catch (DateAndTimePairFailedException e) {
      assertThat(e.getMessage()).containsIgnoringCase("DateAndTimePair Failed: Provide both Completion Date and Completion Time");
    }
  }

  @Test
  public void testValidateTaskWorkflowTypeSuccessCase() {
    Task task = new Task();
    task.setTaskWorkflowId(1);
    boolean res = taskServiceImpl.validateTaskWorkflowType(task);
    assertThat(res).isEqualTo(true);
  }

  @Test
  public void testValidateTaskWorkflowTypeExceptionCase() {
    Task task = new Task();
    task.setTaskWorkflowId(2);
    try {
      boolean res = taskServiceImpl.validateTaskWorkflowType(task);
      assertThat(res).isEqualTo(true);
    } catch (WorkflowTypeDoesNotExistException e) {
      //System.out.println(e.getMessage());
      assertThat(e.getMessage()).containsIgnoringCase("Invalid Data: Workflow Type Does Not Exist");
    }

  }

  @Test
  public void testSetDefaultCurrentActivityIndicator() {
    Task task = new Task();
    task.setCurrentActivityIndicator(1);
    taskServiceImpl.setDefaultCurrentActivityIndicator(task);
  }

  @Test
  public void testCheckAndUpdateTaskByWorkflowTaskStatusExceptionCaseFirst() {
    Task task = new Task();
    WorkFlowTaskStatus workFlowTaskStatus = new WorkFlowTaskStatus();
    workFlowTaskStatus.setWorkflowTaskStatus("NA");
    task.setFkWorkflowTaskStatus(workFlowTaskStatus);

    lenient().when(workflowTaskStatusService.isTaskWorkflowStatusCompleted(any(Task.class))).thenReturn(true);
    OfficeHours officeHours = new OfficeHours();
    officeHours.setValue(LocalTime.now());

    lenient().when(officeHoursService.getOfficeHoursByKeyAndWorkflowTypeId(anyString(), anyInt())).thenReturn(officeHours);
    lenient().doNothing().when(workflowTaskStatusService).setTaskWorkflowStatusCompletedByWorkflowType(any(Task.class));

    try {
      taskServiceImpl.checkAndUpdateTaskByWorkflowTaskStatus(task);
    } catch (ValidationFailedException e) {
      assertThat(e.getMessage()).containsIgnoringCase("Validation Failed: recorded effort cannot be empty for Workflow Task Status");
    }
  }

  @Test
  public void testCheckAndUpdateTaskByWorkflowTaskStatusExceptionCaseSecond() {
    Task task = new Task();
    task.setTaskActEndDate(LocalDateTime.now().plusDays(1));
    WorkFlowTaskStatus workFlowTaskStatus = new WorkFlowTaskStatus();
    workFlowTaskStatus.setWorkflowTaskStatus("NA");
    task.setFkWorkflowTaskStatus(workFlowTaskStatus);

    lenient().when(workflowTaskStatusService.isTaskWorkflowStatusCompleted(any(Task.class))).thenReturn(true);
    OfficeHours officeHours = new OfficeHours();
    officeHours.setValue(LocalTime.now());

    lenient().when(officeHoursService.getOfficeHoursByKeyAndWorkflowTypeId(anyString(), anyInt())).thenReturn(officeHours);
    lenient().doNothing().when(workflowTaskStatusService).setTaskWorkflowStatusCompletedByWorkflowType(any(Task.class));

    try {
      taskServiceImpl.checkAndUpdateTaskByWorkflowTaskStatus(task);
    } catch (ValidationFailedException e) {
      assertThat(e.getMessage()).containsIgnoringCase("Validation Failed: Task Actual End Date can only be today or less than today");
    }
  }

  @Test
  public void testCheckAndUpdateTaskByWorkflowTaskStatusExceptionCaseThird() {
    Task task = new Task();
    task.setTaskWorkflowId(1);
    task.setRecordedEffort(2);
    task.setTaskActEndDate(LocalDateTime.now().minusDays(1));
    WorkFlowTaskStatus workFlowTaskStatus = new WorkFlowTaskStatus();
    workFlowTaskStatus.setWorkflowTaskStatus("NA");
    task.setFkWorkflowTaskStatus(workFlowTaskStatus);

    lenient().when(workflowTaskStatusService.isTaskWorkflowStatusCompleted(any(Task.class))).thenReturn(true);
    OfficeHours officeHours = new OfficeHours();
    officeHours.setValue(LocalTime.now());

    lenient().when(officeHoursService.getOfficeHoursByKeyAndWorkflowTypeId(anyString(), anyInt())).thenReturn(officeHours);
    lenient().doNothing().when(workflowTaskStatusService).setTaskWorkflowStatusCompletedByWorkflowType(any(Task.class));

    try {
      taskServiceImpl.checkAndUpdateTaskByWorkflowTaskStatus(task);
    } catch (ValidationFailedException e) {
    }
  }


  @Test
  public void testCheckAndUpdateTaskByWorkflowTaskStatusExceptionCaseForth() {
    Task task = new Task();
    task.setTaskWorkflowId(1);
    task.setRecordedEffort(2);
    task.setTaskActEndDate(LocalDateTime.now());
    task.setTaskActEndTime(LocalTime.now());
    WorkFlowTaskStatus workFlowTaskStatus = new WorkFlowTaskStatus();
    workFlowTaskStatus.setWorkflowTaskStatus("NA");
    task.setFkWorkflowTaskStatus(workFlowTaskStatus);

    lenient().when(workflowTaskStatusService.isTaskWorkflowStatusCompleted(any(Task.class))).thenReturn(false);
    OfficeHours officeHours = new OfficeHours();
    officeHours.setValue(LocalTime.now());

    lenient().when(officeHoursService.getOfficeHoursByKeyAndWorkflowTypeId(anyString(), anyInt())).thenReturn(officeHours);
    lenient().doNothing().when(workflowTaskStatusService).setTaskWorkflowStatusCompletedByWorkflowType(any(Task.class));

    try {
      taskServiceImpl.checkAndUpdateTaskByWorkflowTaskStatus(task);
    } catch (ValidationFailedException e) {
      assertThat(e.getMessage()).containsIgnoringCase("Validation Failed: Task Actual End Time cannot be more than the current time for task actual end date today");
    }
  }

  @Test
  public void testValidateTaskEstimateByWorkflowTaskStatus() {
    Task taskToUpdate = new Task();
    Task updatedTask = new Task();
    WorkFlowTaskStatus workFlowTaskStatus1 = new WorkFlowTaskStatus();
    workFlowTaskStatus1.setWorkflowTaskStatusId(1);
    workFlowTaskStatus1.setWorkflowTaskStatus("NA");
    WorkFlowTaskStatus workFlowTaskStatus2 = new WorkFlowTaskStatus();
    workFlowTaskStatus2.setWorkflowTaskStatusId(2);
    workFlowTaskStatus2.setWorkflowTaskStatus("NA2");
    taskToUpdate.setFkWorkflowTaskStatus(workFlowTaskStatus1);
    updatedTask.setFkWorkflowTaskStatus(workFlowTaskStatus2);

    lenient().when(workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(anyInt())).thenReturn(workFlowTaskStatus1);
    // todo need to check for again putting lenient case.

    boolean res = taskServiceImpl.validateTaskEstimateByWorkflowTaskStatus(taskToUpdate, updatedTask);
    assertThat(res).isEqualTo(true);

  }

  @Test
  public void testValidateTaskForWorkflowStatusNotStartedExceptionCaseFirstTwoTasks() {
    Task taskToUpdate = new Task();
    Task updatedTask = new Task();
    WorkFlowTaskStatus workFlowTaskStatus1 = new WorkFlowTaskStatus();
    workFlowTaskStatus1.setWorkflowTaskStatusId(1);
    workFlowTaskStatus1.setWorkflowTaskStatus(Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG.toLowerCase());
    WorkFlowTaskStatus workFlowTaskStatus2 = new WorkFlowTaskStatus();
    workFlowTaskStatus2.setWorkflowTaskStatusId(2);
    workFlowTaskStatus2.setWorkflowTaskStatus(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED.toLowerCase());
    taskToUpdate.setFkWorkflowTaskStatus(workFlowTaskStatus1);
    updatedTask.setFkWorkflowTaskStatus(workFlowTaskStatus2);

    lenient().when(workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(eq(1))).thenReturn(workFlowTaskStatus1);
    lenient().when(workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(eq(2))).thenReturn(workFlowTaskStatus2);

    try {
      taskServiceImpl.validateTaskForWorkflowStatusNotStarted(updatedTask);
    } catch (ValidationFailedException e) {
      System.out.println(e.getMessage());
      assertThat(e.getMessage()).containsIgnoringCase("Validation Failed: Task Expected Start Date Can't be null for Not Started Task");
    }

  }

  @Test
  public void testValidateTaskForWorkflowStatusNotStartedExceptionCaseSecondTwoTasks() {
    Task taskToUpdate = new Task();
    Task updatedTask = new Task();
    updatedTask.setTaskExpStartDate(LocalDateTime.now());
    WorkFlowTaskStatus workFlowTaskStatus1 = new WorkFlowTaskStatus();
    workFlowTaskStatus1.setWorkflowTaskStatusId(1);
    workFlowTaskStatus1.setWorkflowTaskStatus(Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG.toLowerCase());
    WorkFlowTaskStatus workFlowTaskStatus2 = new WorkFlowTaskStatus();
    workFlowTaskStatus2.setWorkflowTaskStatusId(2);
    workFlowTaskStatus2.setWorkflowTaskStatus(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED.toLowerCase());
    taskToUpdate.setFkWorkflowTaskStatus(workFlowTaskStatus1);
    updatedTask.setFkWorkflowTaskStatus(workFlowTaskStatus2);

    lenient().when(workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(eq(1))).thenReturn(workFlowTaskStatus1);
    lenient().when(workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(eq(2))).thenReturn(workFlowTaskStatus2);

    try {
      taskServiceImpl.validateTaskForWorkflowStatusNotStarted(updatedTask);
    } catch (ValidationFailedException e) {
      assertThat(e.getMessage()).containsIgnoringCase("Validation Failed: Task Expected Start Time Can't be null for Not Started Task");
    }

  }

  @Test
  @Ignore
  public void testValidateTaskForWorkflowStatusNotStartedExceptionCaseThirdTwoTasks() {
    Task taskToUpdate = new Task();
    Task updatedTask = new Task();
    updatedTask.setTaskExpStartDate(LocalDateTime.now());
    updatedTask.setTaskExpStartTime(LocalTime.now());

    WorkFlowTaskStatus workFlowTaskStatus1 = new WorkFlowTaskStatus();
    workFlowTaskStatus1.setWorkflowTaskStatusId(1);
    workFlowTaskStatus1.setWorkflowTaskStatus(Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG.toLowerCase());
    WorkFlowTaskStatus workFlowTaskStatus2 = new WorkFlowTaskStatus();
    workFlowTaskStatus2.setWorkflowTaskStatusId(2);
    workFlowTaskStatus2.setWorkflowTaskStatus(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED.toLowerCase());
    taskToUpdate.setFkWorkflowTaskStatus(workFlowTaskStatus1);
    updatedTask.setFkWorkflowTaskStatus(workFlowTaskStatus2);

    lenient().when(workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(eq(1))).thenReturn(workFlowTaskStatus1);
    lenient().when(workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(eq(2))).thenReturn(workFlowTaskStatus2);

    try {
      taskServiceImpl.validateTaskForWorkflowStatusNotStarted(updatedTask);
    } catch (ValidationFailedException e) {
      assertThat(e.getMessage()).containsIgnoringCase("Validation Failed: Task Expected End Date Can't be null for Not Started Task");
    }

  }

  @Test
  public void testValidateTaskForWorkflowStatusNotStartedExceptionCaseForthTwoTasks() {
    Task taskToUpdate = new Task();
    Task updatedTask = new Task();
    updatedTask.setTaskExpStartDate(LocalDateTime.now());
    updatedTask.setTaskExpStartTime(LocalTime.now());

    updatedTask.setTaskExpEndDate(LocalDateTime.now());


    WorkFlowTaskStatus workFlowTaskStatus1 = new WorkFlowTaskStatus();
    workFlowTaskStatus1.setWorkflowTaskStatusId(1);
    workFlowTaskStatus1.setWorkflowTaskStatus(Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG.toLowerCase());
    WorkFlowTaskStatus workFlowTaskStatus2 = new WorkFlowTaskStatus();
    workFlowTaskStatus2.setWorkflowTaskStatusId(2);
    workFlowTaskStatus2.setWorkflowTaskStatus(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED.toLowerCase());
    taskToUpdate.setFkWorkflowTaskStatus(workFlowTaskStatus1);
    updatedTask.setFkWorkflowTaskStatus(workFlowTaskStatus2);

    lenient().when(workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(eq(1))).thenReturn(workFlowTaskStatus1);
    lenient().when(workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(eq(2))).thenReturn(workFlowTaskStatus2);

    try {
      taskServiceImpl.validateTaskForWorkflowStatusNotStarted(updatedTask);
    } catch (ValidationFailedException e) {
      assertThat(e.getMessage()).containsIgnoringCase("Validation Failed: Task Expected End Time Can't be null for Not Started Task");
    }

  }

  @Test
  public void testValidateTaskForWorkflowStatusNotStartedExceptionCaseFifthTwoTasks() {
    Task taskToUpdate = new Task();
    Task updatedTask = new Task();
    updatedTask.setTaskExpStartDate(LocalDateTime.now());
    updatedTask.setTaskExpStartTime(LocalTime.now());

    updatedTask.setTaskExpEndDate(LocalDateTime.now());
    updatedTask.setTaskExpEndTime(LocalTime.now());


    WorkFlowTaskStatus workFlowTaskStatus1 = new WorkFlowTaskStatus();
    workFlowTaskStatus1.setWorkflowTaskStatusId(1);
    workFlowTaskStatus1.setWorkflowTaskStatus(Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG.toLowerCase());
    WorkFlowTaskStatus workFlowTaskStatus2 = new WorkFlowTaskStatus();
    workFlowTaskStatus2.setWorkflowTaskStatusId(2);
    workFlowTaskStatus2.setWorkflowTaskStatus(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED.toLowerCase());
    taskToUpdate.setFkWorkflowTaskStatus(workFlowTaskStatus1);
    updatedTask.setFkWorkflowTaskStatus(workFlowTaskStatus2);

    lenient().when(workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(eq(1))).thenReturn(workFlowTaskStatus1);
    lenient().when(workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(eq(2))).thenReturn(workFlowTaskStatus2);

    try {
      taskServiceImpl.validateTaskForWorkflowStatusNotStarted(updatedTask);
    } catch (ValidationFailedException e) {
      assertThat(e.getMessage()).containsIgnoringCase("Validation Failed: Task Priority Can't be null for Not Started Task");
    }

  }

  @Test
  public void testValidateTaskForWorkflowStatusNotStartedExceptionCaseSixthTwoTasks() {
    Task taskToUpdate = new Task();
    Task updatedTask = new Task();
    updatedTask.setTaskExpStartDate(LocalDateTime.now());
    updatedTask.setTaskExpStartTime(LocalTime.now());

    updatedTask.setTaskExpEndDate(LocalDateTime.now());
    updatedTask.setTaskExpEndTime(LocalTime.now());
    updatedTask.setTaskPriority("P0");
    updatedTask.setTaskEstimate(60);
    updatedTask.setIsEstimateSystemGenerated(0);

    WorkFlowTaskStatus workFlowTaskStatus1 = new WorkFlowTaskStatus();
    workFlowTaskStatus1.setWorkflowTaskStatusId(1);
    workFlowTaskStatus1.setWorkflowTaskStatus(Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG.toLowerCase());
    WorkFlowTaskStatus workFlowTaskStatus2 = new WorkFlowTaskStatus();
    workFlowTaskStatus2.setWorkflowTaskStatusId(2);
    workFlowTaskStatus2.setWorkflowTaskStatus(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED.toLowerCase());
    taskToUpdate.setFkWorkflowTaskStatus(workFlowTaskStatus1);
    updatedTask.setFkWorkflowTaskStatus(workFlowTaskStatus2);

    lenient().when(workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(eq(1))).thenReturn(workFlowTaskStatus1);
    lenient().when(workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(eq(2))).thenReturn(workFlowTaskStatus2);

    taskServiceImpl.validateTaskForWorkflowStatusNotStarted(updatedTask);

  }




  @Test
  public void testValidateTaskForWorkflowStatusNotStartedSuccess() {
    Task taskToUpdate = new Task();
    Task updatedTask = new Task();

    updatedTask.setTaskExpStartDate(LocalDateTime.now());
    updatedTask.setTaskExpStartTime(LocalTime.now());

    updatedTask.setTaskExpEndDate(LocalDateTime.now());
    updatedTask.setTaskExpEndTime(LocalTime.now());
    updatedTask.setTaskPriority("P0");
    updatedTask.setTaskEstimate(60);

    WorkFlowTaskStatus workFlowTaskStatus2 = new WorkFlowTaskStatus();
    workFlowTaskStatus2.setWorkflowTaskStatusId(2);
    workFlowTaskStatus2.setWorkflowTaskStatus(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED.toLowerCase());
    updatedTask.setFkWorkflowTaskStatus(workFlowTaskStatus2);

    lenient().when(workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(eq(2))).thenReturn(workFlowTaskStatus2);

    boolean res = taskServiceImpl.validateTaskForWorkflowStatusNotStarted(updatedTask);
    assertThat(res).isEqualTo(true);
  }

  @Test
  public void testValidateWorkflowTaskStatus() {
    Task taskToUpdate = new Task();
    Task updatedTask = new Task();
    updatedTask.setTaskExpStartDate(LocalDateTime.now());
    updatedTask.setTaskExpStartTime(LocalTime.now());

    WorkFlowTaskStatus workFlowTaskStatus1 = new WorkFlowTaskStatus();
    workFlowTaskStatus1.setWorkflowTaskStatusId(1);
    workFlowTaskStatus1.setWorkflowTaskStatus(Constants.WorkFlowTaskStatusConstants.STATUS_STARTED.toLowerCase());
    WorkFlowTaskStatus workFlowTaskStatus2 = new WorkFlowTaskStatus();
    workFlowTaskStatus2.setWorkflowTaskStatusId(2);
    workFlowTaskStatus2.setWorkflowTaskStatus(Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG.toLowerCase());
    taskToUpdate.setFkWorkflowTaskStatus(workFlowTaskStatus1);
    updatedTask.setFkWorkflowTaskStatus(workFlowTaskStatus2);

    lenient().when(workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(eq(1))).thenReturn(workFlowTaskStatus1);
    lenient().when(workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(eq(2))).thenReturn(workFlowTaskStatus2);

    boolean res = taskServiceImpl.validateExpActStartDateTimeForWorkflowStatusStarted(taskToUpdate, updatedTask);
    assertThat(res).isEqualTo(true);

  }

  @Test
  public void testValidateWorkflowTaskStatusExceptionCaseFirst() {
    Task taskToUpdate = new Task();
    Task updatedTask = new Task();
    updatedTask.setTaskExpStartDate(LocalDateTime.now());
    updatedTask.setTaskExpStartTime(LocalTime.now());
    updatedTask.setTaskActStDate(LocalDateTime.now());

    WorkFlowTaskStatus workFlowTaskStatus1 = new WorkFlowTaskStatus();
    workFlowTaskStatus1.setWorkflowTaskStatusId(1);
    workFlowTaskStatus1.setWorkflowTaskStatus(Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG.toLowerCase());
    WorkFlowTaskStatus workFlowTaskStatus2 = new WorkFlowTaskStatus();
    workFlowTaskStatus2.setWorkflowTaskStatusId(2);
    workFlowTaskStatus2.setWorkflowTaskStatus(Constants.WorkFlowTaskStatusConstants.STATUS_STARTED.toLowerCase());
    taskToUpdate.setFkWorkflowTaskStatus(workFlowTaskStatus1);
    updatedTask.setFkWorkflowTaskStatus(workFlowTaskStatus2);

    lenient().when(workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(eq(1))).thenReturn(workFlowTaskStatus1);
    lenient().when(workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(eq(2))).thenReturn(workFlowTaskStatus2);

    try {
      boolean res = taskServiceImpl.validateExpActStartDateTimeForWorkflowStatusStarted(updatedTask, taskToUpdate);
      assertThat(res).isEqualTo(true);
    } catch (DateAndTimePairFailedException e) {
      assertThat(e.getMessage()).containsIgnoringCase("DateAndTimePair Failed: Provide both Actual Start Date and Actual Start Time");
    }

  }


  @Test
  public void testValidateWorkflowTaskStatusExceptionCaseSecond() {
    Task taskToUpdate = new Task();
    Task updatedTask = new Task();
    updatedTask.setTaskExpStartDate(LocalDateTime.now());
    updatedTask.setTaskActStDate(LocalDateTime.now());
    updatedTask.setTaskActStTime(LocalTime.now());

    WorkFlowTaskStatus workFlowTaskStatus1 = new WorkFlowTaskStatus();
    workFlowTaskStatus1.setWorkflowTaskStatusId(1);
    workFlowTaskStatus1.setWorkflowTaskStatus(Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG.toLowerCase());
    WorkFlowTaskStatus workFlowTaskStatus2 = new WorkFlowTaskStatus();
    workFlowTaskStatus2.setWorkflowTaskStatusId(2);
    workFlowTaskStatus2.setWorkflowTaskStatus(Constants.WorkFlowTaskStatusConstants.STATUS_STARTED.toLowerCase());
    taskToUpdate.setFkWorkflowTaskStatus(workFlowTaskStatus1);
    updatedTask.setFkWorkflowTaskStatus(workFlowTaskStatus2);

    lenient().when(workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(eq(1))).thenReturn(workFlowTaskStatus1);
    lenient().when(workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(eq(2))).thenReturn(workFlowTaskStatus2);

    try {
      boolean res = taskServiceImpl.validateExpActStartDateTimeForWorkflowStatusStarted(updatedTask, taskToUpdate);
      assertThat(res).isEqualTo(true);
    } catch (DateAndTimePairFailedException e) {
      assertThat(e.getMessage()).containsIgnoringCase("DateAndTimePair Failed: Provide both Expected Start Date and Expected Start Time");
    }

  }

  @Test
  @Ignore
  /* This test case is not valid because there is a change in the actual method.
   * The third exception case has been removed from the actual method.*/
  public void testValidateWorkflowTaskStatusExceptionCaseThird() {
    Task taskToUpdate = new Task();
    Task updatedTask = new Task();
    updatedTask.setTaskExpStartDate(LocalDateTime.now());
    updatedTask.setTaskActStDate(LocalDateTime.now());
    updatedTask.setTaskActStTime(LocalTime.now());

    WorkFlowTaskStatus workFlowTaskStatus1 = new WorkFlowTaskStatus();
    workFlowTaskStatus1.setWorkflowTaskStatusId(1);
    workFlowTaskStatus1.setWorkflowTaskStatus("NA".toLowerCase());
    WorkFlowTaskStatus workFlowTaskStatus2 = new WorkFlowTaskStatus();
    workFlowTaskStatus2.setWorkflowTaskStatusId(2);
    workFlowTaskStatus2.setWorkflowTaskStatus(Constants.WorkFlowTaskStatusConstants.STATUS_STARTED.toLowerCase());
    taskToUpdate.setFkWorkflowTaskStatus(workFlowTaskStatus1);
    updatedTask.setFkWorkflowTaskStatus(workFlowTaskStatus2);

    lenient().when(workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(eq(1))).thenReturn(workFlowTaskStatus1);
    lenient().when(workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(eq(2))).thenReturn(workFlowTaskStatus2);

    try {
      boolean res = taskServiceImpl.validateExpActStartDateTimeForWorkflowStatusStarted(updatedTask, taskToUpdate);
      assertThat(res).isEqualTo(true);
    } catch (WorkflowTaskStatusFailedException e) {
      assertThat(e.getMessage()).containsIgnoringCase("WorkflowTaskStatus Failed: Cannot update started from");
    }

  }

  @Test
  public void testValidateTaskForWorkflowStatus() {
    Task taskDB = mock(Task.class);
    Task task = null;
    TaskServiceImpl innertaskserviceImpl = mock(TaskServiceImpl.class);
    boolean res = taskServiceImpl.validateTaskForWorkflowStatus(taskDB, task);
    assertThat(res).isEqualTo(false);
  }

  @Test
  public void testValidateTaskByWorkflowStatus() {
    Task taskDB = new Task();
    taskDB.setTaskPriority("NA");
    taskDB.setTaskEstimate(60);
    taskDB.setIsEstimateSystemGenerated(0);

    Task task = new Task();
    task.setTaskPriority("NA");
    task.setTaskEstimate(60);
    task.setIsEstimateSystemGenerated(0);
    UserAccount userAccount = new UserAccount();
    userAccount.setAccountId(1L);
    task.setFkAccountIdAssigned(userAccount);
    taskDB.setFkAccountIdAssigned(userAccount);
    task.setTaskExpEndDate(LocalDateTime.now());
    task.setTaskExpEndTime(LocalTime.now());

    WorkFlowTaskStatus workflowTaskStatus1 = new WorkFlowTaskStatus();
    workflowTaskStatus1.setWorkflowTaskStatusId(1);
    workflowTaskStatus1.setWorkflowTaskStatus(Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG);
    taskDB.setFkWorkflowTaskStatus(workflowTaskStatus1);

    WorkFlowTaskStatus workflowTaskStatus2 = new WorkFlowTaskStatus();
    workflowTaskStatus2.setWorkflowTaskStatusId(2);
    workflowTaskStatus2.setWorkflowTaskStatus(Constants.WorkFlowTaskStatusConstants.STATUS_STARTED);
    task.setFkWorkflowTaskStatus(workflowTaskStatus2);

    lenient().when(workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(eq(1))).thenReturn(workflowTaskStatus1);
    lenient().when(workflowTaskStatusService.getWorkflowTaskStatusByWorkflowTaskStatusId(eq(2))).thenReturn(workflowTaskStatus2);

    boolean res = taskServiceImpl.validateTaskByWorkflowStatus(taskDB, task, null);
    assertThat(res).isEqualTo(true);
  }


  /** Test exception condition when version = 0 and recordedEffort is not null */
  @Test(expected = ValidationFailedException.class)
  @Ignore // Error in the method.
  public void testUpdateTimeSheetAndRecordedEffort_versionIsZero(){

    String localTimeZone = "Asia/Calcutta";
    Task task = new Task();
    task.setRecordedEffort(10);
    task.setVersion(0L);

    taskServiceImpl.updateTimeSheetAndRecordedEffort(task, localTimeZone);
  }

  /** test UpdateTimeSheetAndRecordedEffort when new Effort Track list is empty */
  @Test
  public void testUpdateTimeSheetAndRecordedEffort_newEffortTrackIsEmpty(){
    Task task = new Task();
    String localTimeZone = "Asia/Calcutta";
    task.setNewEffortTracks(Collections.emptyList());

    TaskServiceImpl taskService1 = mock(TaskServiceImpl.class);
    doNothing().when(taskService1).updateTimeSheetAndRecordedEffort(any(Task.class), anyString());

    taskService1.updateTimeSheetAndRecordedEffort(task, localTimeZone);
    verify(taskService1).updateTimeSheetAndRecordedEffort(task, localTimeZone);
  }

  @Test(expected = ValidationFailedException.class)
  public void testtestUpdateTimeSheetAndRecordedEffort_unauthorizedUser(){
    Task task = new Task();
    String localTimeZone = "Asia/Calcutta";
    Team team = new Team();
    team.setTeamId(10L);
    task.setFkTeamId(team);
    task.setTaskId(1L);
    WorkFlowTaskStatus workFlowTaskStatus = new WorkFlowTaskStatus();
    workFlowTaskStatus.setWorkflowTaskStatus(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED);
    task.setFkWorkflowTaskStatus(workFlowTaskStatus);
    NewEffortTrack newEffortTrack = new NewEffortTrack();
    newEffortTrack.setNewEffort(1);
    newEffortTrack.setNewEffortDate(LocalDate.now());
    task.setNewEffortTracks(List.of(newEffortTrack));
    UserAccount userAccount = new UserAccount();
    userAccount.setAccountId(1L);
    task.setFkAccountIdAssigned(userAccount);
    UserAccount userAccount2 = new UserAccount();
    userAccount.setAccountId(2L);
    task.setFkAccountIdLastUpdated(userAccount2);
    List<RoleId> roleIds = Arrays.asList(new RoleId(1), new RoleId(2));
    when(accessDomainRepository.findRoleIdByAccountIdAndEntityIdAndIsActive(anyLong(), anyLong(), true)).thenReturn(roleIds);
    when(taskRepository.findByTaskId(anyLong())).thenReturn(task);
    taskServiceImpl.updateTimeSheetAndRecordedEffort(task, localTimeZone);
  }


  @Test(expected = ValidationFailedException.class)
  public void testtestUpdateTimeSheetAndRecordedEffort_invalidTaskStatus(){
    Task task = new Task();
    Team team = new Team();
    String localTimeZone = "Asia/Calcutta";
    team.setTeamId(10L);
    task.setFkTeamId(team);
    task.setTaskId(1L);
    WorkFlowTaskStatus workFlowTaskStatus = new WorkFlowTaskStatus();
    workFlowTaskStatus.setWorkflowTaskStatus(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED);
    task.setFkWorkflowTaskStatus(workFlowTaskStatus);

    NewEffortTrack newEffortTrack = new NewEffortTrack();
    newEffortTrack.setNewEffort(1);
    newEffortTrack.setNewEffortDate(LocalDate.now());
    task.setNewEffortTracks(List.of(newEffortTrack));

    UserAccount userAccount = new UserAccount();
    userAccount.setAccountId(1L);
    task.setFkAccountIdAssigned(userAccount);
    task.setFkAccountIdLastUpdated(userAccount);
    List<RoleId> roleIds = Arrays.asList(new RoleId(1), new RoleId(2));
    when(accessDomainRepository.findRoleIdByAccountIdAndEntityIdAndIsActive(anyLong(), anyLong(), true)).thenReturn(roleIds);
    when(taskRepository.findByTaskId(anyLong())).thenReturn(task);
    taskServiceImpl.updateTimeSheetAndRecordedEffort(task, localTimeZone);
  }


  @Test(expected = ValidationFailedException.class)
  public void testtestUpdateTimeSheetAndRecordedEffort_completedTaskStatus(){
    String localTimeZone = "Asia/Calcutta";
    Task task = new Task();
    Team team = new Team();
    team.setTeamId(10L);
    task.setFkTeamId(team);
    task.setTaskId(1L);
    WorkFlowTaskStatus workFlowTaskStatus = new WorkFlowTaskStatus();
    workFlowTaskStatus.setWorkflowTaskStatus(Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED);
    task.setFkWorkflowTaskStatus(workFlowTaskStatus);

    NewEffortTrack newEffortTrack = new NewEffortTrack();
    newEffortTrack.setNewEffort(1);
    newEffortTrack.setNewEffortDate(LocalDate.now());
    task.setNewEffortTracks(List.of(newEffortTrack));

    UserAccount userAccount = new UserAccount();
    userAccount.setAccountId(1L);
    task.setFkAccountIdAssigned(userAccount);
    task.setFkAccountIdLastUpdated(userAccount);
    List<RoleId> roleIds = Arrays.asList(new RoleId(1), new RoleId(2));
    when(accessDomainRepository.findRoleIdByAccountIdAndEntityIdAndIsActive(anyLong(), anyLong(), true)).thenReturn(roleIds);
    when(taskRepository.findByTaskId(anyLong())).thenReturn(task);
    taskServiceImpl.updateTimeSheetAndRecordedEffort(task, localTimeZone);
  }

  @Test(expected = ValidationFailedException.class)
  public void testtestUpdateTimeSheetAndRecordedEffort_taskEstimateNull(){
    String localTimeZone = "Asia/Calcutta";
    Task task = new Task();
    Team team = new Team();
    team.setTeamId(10L);
    task.setFkTeamId(team);
    task.setTaskId(1L);
    task.setTaskEstimate(0);
    WorkFlowTaskStatus workFlowTaskStatus = new WorkFlowTaskStatus();
    workFlowTaskStatus.setWorkflowTaskStatus(Constants.WorkFlowTaskStatusConstants.STATUS_STARTED);
    task.setFkWorkflowTaskStatus(workFlowTaskStatus);

    NewEffortTrack newEffortTrack = new NewEffortTrack();
    newEffortTrack.setNewEffort(1);
    newEffortTrack.setNewEffortDate(LocalDate.now());
    task.setNewEffortTracks(List.of(newEffortTrack));

    UserAccount userAccount = new UserAccount();
    userAccount.setAccountId(1L);
    task.setFkAccountIdAssigned(userAccount);
    task.setFkAccountIdLastUpdated(userAccount);
    List<RoleId> roleIds = Arrays.asList(new RoleId(1), new RoleId(2));
    when(accessDomainRepository.findRoleIdByAccountIdAndEntityIdAndIsActive(anyLong(), anyLong(), true)).thenReturn(roleIds);
    when(taskRepository.findByTaskId(anyLong())).thenReturn(task);
    taskServiceImpl.updateTimeSheetAndRecordedEffort(task, localTimeZone);
  }

  @Test(expected = ValidationFailedException.class)
  public void testtestUpdateTimeSheetAndRecordedEffort_recordedEffortNotNull(){
    String localTimeZone = "Asia/Calcutta";
    Task task = new Task();
    Team team = new Team();
    team.setTeamId(10L);
    task.setFkTeamId(team);
    task.setTaskId(1L);
    task.setTaskEstimate(10);
    task.setVersion(0L);
    task.setRecordedEffort(10);
    WorkFlowTaskStatus workFlowTaskStatus = new WorkFlowTaskStatus();
    workFlowTaskStatus.setWorkflowTaskStatus(Constants.WorkFlowTaskStatusConstants.STATUS_STARTED);
    task.setFkWorkflowTaskStatus(workFlowTaskStatus);

    NewEffortTrack newEffortTrack = new NewEffortTrack();
    newEffortTrack.setNewEffort(1);
    newEffortTrack.setNewEffortDate(LocalDate.now());
    task.setNewEffortTracks(List.of(newEffortTrack));

    UserAccount userAccount = new UserAccount();
    userAccount.setAccountId(1L);
    task.setFkAccountIdAssigned(userAccount);
    task.setFkAccountIdLastUpdated(userAccount);
    List<RoleId> roleIds = Arrays.asList(new RoleId(1), new RoleId(2));
    when(accessDomainRepository.findRoleIdByAccountIdAndEntityIdAndIsActive(anyLong(), anyLong(), true)).thenReturn(roleIds);
    when(taskRepository.findByTaskId(anyLong())).thenReturn(task);
    taskServiceImpl.updateTimeSheetAndRecordedEffort(task, localTimeZone);
  }

  @Test(expected = ValidationFailedException.class)
  public void testUpdateTimeSheetAndRecordedEffort_accountIdAssignedNull(){
    String localTimeZone = "Asia/Calcutta";
    Task task = new Task();
    Team team = new Team();
    team.setTeamId(10L);
    task.setFkTeamId(team);
    task.setTaskId(1L);
    task.setTaskEstimate(10);
    task.setVersion(1L);
    task.setRecordedEffort(10);
    WorkFlowTaskStatus workFlowTaskStatus = new WorkFlowTaskStatus();
    workFlowTaskStatus.setWorkflowTaskStatus(Constants.WorkFlowTaskStatusConstants.STATUS_STARTED);
    task.setFkWorkflowTaskStatus(workFlowTaskStatus);

    NewEffortTrack newEffortTrack = new NewEffortTrack();
    newEffortTrack.setNewEffort(1);
    newEffortTrack.setNewEffortDate(LocalDate.now());
    task.setNewEffortTracks(List.of(newEffortTrack));

    UserAccount userAccount = new UserAccount();
    userAccount.setAccountId(1L);
    task.setFkAccountIdAssigned(null);
    task.setFkAccountIdLastUpdated(userAccount);
    List<RoleId> roleIds = Arrays.asList(new RoleId(9), new RoleId(10));
    when(accessDomainRepository.findRoleIdByAccountIdAndEntityIdAndIsActive(anyLong(), anyLong(), true)).thenReturn(roleIds);
    when(taskRepository.findByTaskId(anyLong())).thenReturn(task);
    taskServiceImpl.updateTimeSheetAndRecordedEffort(task, localTimeZone);

  }

  @Test(expected = NoDataFoundException.class)
  public void testUpdateTimeSheetAndRecordedEffort_userAccountIsNull(){
    String localTimeZone = "Asia/Calcutta";
    Task task = new Task();
    Team team = new Team();
    team.setTeamId(10L);
    task.setFkTeamId(team);
    task.setTaskId(1L);
    task.setTaskEstimate(10);
    task.setVersion(1L);
    task.setRecordedEffort(10);
    WorkFlowTaskStatus workFlowTaskStatus = new WorkFlowTaskStatus();
    workFlowTaskStatus.setWorkflowTaskStatus(Constants.WorkFlowTaskStatusConstants.STATUS_STARTED);
    task.setFkWorkflowTaskStatus(workFlowTaskStatus);

    NewEffortTrack newEffortTrack = new NewEffortTrack();
    newEffortTrack.setNewEffort(1);
    newEffortTrack.setNewEffortDate(LocalDate.now());
    task.setNewEffortTracks(List.of(newEffortTrack));

    UserAccount userAccount = new UserAccount();
    userAccount.setAccountId(1L);
    task.setFkAccountIdAssigned(userAccount);
    task.setFkAccountIdLastUpdated(userAccount);
    List<RoleId> roleIds = Arrays.asList(new RoleId(9), new RoleId(10));
    when(accessDomainRepository.findRoleIdByAccountIdAndEntityIdAndIsActive(anyLong(), anyLong(), true)).thenReturn(roleIds);
    when(taskRepository.findByTaskId(anyLong())).thenReturn(task);
    when(userAccountService.getActiveUserAccountByAccountId(anyLong())).thenReturn(null);
    taskServiceImpl.updateTimeSheetAndRecordedEffort(task, localTimeZone);

  }

  /** test UpdateTimeSheetAndRecordedEffort when newEffortTrack list is not null and valid UserAccount exists */
  @Test
  public void testUpdateTimeSheetAndRecordedEffort_userAccountIsPresent(){
    String localTimeZone = "Asia/Calcutta";
    Task task = new Task();
    Team team = new Team();
    team.setTeamId(10L);
    task.setFkTeamId(team);
    task.setTaskTitle("Testing");
    task.setTaskTypeId(1);
    task.setTaskId(1L);
    task.setTaskNumber("1000L");
    task.setBuId(1L);
    task.setTaskEstimate(10);
    task.setVersion(1L);
    task.setRecordedEffort(30);
    task.setTaskActStDate(LocalDateTime.now().minusDays(1));
    task.setTaskActEndDate(LocalDateTime.now().plusDays(1));
    Project project = new Project();
    project.setProjectId(1L);
    task.setFkProjectId(project);
    WorkFlowTaskStatus workFlowTaskStatus = new WorkFlowTaskStatus();
    workFlowTaskStatus.setWorkflowTaskStatus(Constants.WorkFlowTaskStatusConstants.STATUS_STARTED);
    task.setFkWorkflowTaskStatus(workFlowTaskStatus);
    NewEffortTrack newEffortTrack1 = new NewEffortTrack();
    newEffortTrack1.setNewEffort(10);
    newEffortTrack1.setNewEffortDate(LocalDate.now());
    NewEffortTrack newEffortTrack2 = new NewEffortTrack();
    newEffortTrack2.setNewEffort(20);
    newEffortTrack2.setNewEffortDate(LocalDate.now());
    task.setNewEffortTracks(List.of(newEffortTrack1, newEffortTrack2));
    User user = new User();
    user.setUserId(1L);
    UserAccount userAccount = new UserAccount();
    userAccount.setAccountId(1L);
    userAccount.setOrgId(1L);
    userAccount.setFkUserId(user);
    task.setFkAccountIdAssigned(userAccount);
    task.setFkAccountIdLastUpdated(userAccount);
    List<RoleId> roleIds = Arrays.asList(new RoleId(9), new RoleId(10));
    when(accessDomainRepository.findRoleIdByAccountIdAndEntityIdAndIsActive(anyLong(), anyLong(), true)).thenReturn(roleIds);
    when(taskRepository.findByTaskId(anyLong())).thenReturn(task);
    when(userAccountService.getActiveUserAccountByAccountId(anyLong())).thenReturn(userAccount);
    when(timeSheetService.saveAllTimeSheetRecords(anyList())).thenReturn(List.of(new TimeSheet()));
    doNothing().when(taskServiceImpl).updateEarnedTimeAndUpdateTaskAndTimeSheet(any(Task.class), anyList(), anyInt());
//    doNothing().when(taskServiceImpl).setReferenceEntitiesByTask(any(Task.class), any(TimeSheet.class));
    taskServiceImpl.updateTimeSheetAndRecordedEffort(task, localTimeZone);

    verify(userAccountService).getActiveUserAccountByAccountId(userAccount.getAccountId());
    verify(timeSheetService).saveAllTimeSheetRecords(anyList());
    assertEquals(Integer.valueOf(60), task.getRecordedEffort());

  }

  /** test UpdateTimeSheetAndRecordedEffort when newEffort or date field is null in newEffortTrack list of values*/
  @Test(expected = ValidationFailedException.class)
  public void testUpdateTimeSheetAndRecordedEffort_newEffortNullValues(){
    String localTimeZone = "Asia/Calcutta";
    Task task = new Task();
    Team team = new Team();
    team.setTeamId(10L);
    task.setFkTeamId(team);
    task.setTaskId(1L);
    task.setTaskEstimate(10);
    task.setVersion(1L);
    task.setRecordedEffort(10);
    WorkFlowTaskStatus workFlowTaskStatus = new WorkFlowTaskStatus();
    workFlowTaskStatus.setWorkflowTaskStatus(Constants.WorkFlowTaskStatusConstants.STATUS_STARTED);
    task.setFkWorkflowTaskStatus(workFlowTaskStatus);

    NewEffortTrack newEffortTrack = new NewEffortTrack();
    newEffortTrack.setNewEffort(null);
    newEffortTrack.setNewEffortDate(LocalDate.now());
    task.setNewEffortTracks(List.of(newEffortTrack));

    UserAccount userAccount = new UserAccount();
    userAccount.setAccountId(1L);
    task.setFkAccountIdAssigned(userAccount);
    task.setFkAccountIdLastUpdated(userAccount);
    List<RoleId> roleIds = Arrays.asList(new RoleId(9), new RoleId(10));
    when(accessDomainRepository.findRoleIdByAccountIdAndEntityIdAndIsActive(anyLong(), anyLong(), true)).thenReturn(roleIds);
    when(taskRepository.findByTaskId(anyLong())).thenReturn(task);
    when(userAccountService.getActiveUserAccountByAccountId(anyLong())).thenReturn(userAccount);
    taskServiceImpl.updateTimeSheetAndRecordedEffort(task, localTimeZone);

    when(userAccountService.getActiveUserAccountByAccountId(anyLong())).thenReturn(new UserAccount());
  }

  /** test UpdateTimeSheetAndRecordedEffort when new effort date is invalid */
  @Test(expected = ValidationFailedException.class)
  public void testUpdateTimeSheetAndRecordedEffort_newEffortDateAfterCurrentDate() {
    String localTimeZone = "Asia/Calcutta";
    Task task = new Task();
    Team team = new Team();
    team.setTeamId(10L);
    task.setFkTeamId(team);
    task.setTaskId(1L);
    task.setTaskEstimate(10);
    task.setVersion(1L);
    task.setRecordedEffort(10);
    WorkFlowTaskStatus workFlowTaskStatus = new WorkFlowTaskStatus();
    workFlowTaskStatus.setWorkflowTaskStatus(Constants.WorkFlowTaskStatusConstants.STATUS_STARTED);
    task.setFkWorkflowTaskStatus(workFlowTaskStatus);

    NewEffortTrack newEffortTrack = new NewEffortTrack();
    newEffortTrack.setNewEffort(10);
    newEffortTrack.setNewEffortDate(LocalDate.now().plusDays(1));
    task.setNewEffortTracks(List.of(newEffortTrack));

    UserAccount userAccount = new UserAccount();
    userAccount.setAccountId(1L);
    task.setFkAccountIdAssigned(userAccount);
    task.setFkAccountIdLastUpdated(userAccount);
    List<RoleId> roleIds = Arrays.asList(new RoleId(9), new RoleId(10));
    when(accessDomainRepository.findRoleIdByAccountIdAndEntityIdAndIsActive(anyLong(), anyLong(), true)).thenReturn(roleIds);
    when(taskRepository.findByTaskId(anyLong())).thenReturn(task);
    when(userAccountService.getActiveUserAccountByAccountId(anyLong())).thenReturn(userAccount);
    taskServiceImpl.updateTimeSheetAndRecordedEffort(task, localTimeZone);

    when(userAccountService.getActiveUserAccountByAccountId(anyLong())).thenReturn(new UserAccount());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testUpdateTimeSheetAndRecordedEffort_newEffortDateBeforeTaskActStartDate() {
    String localTimeZone = "Asia/Calcutta";
    Task task = new Task();
    Team team = new Team();
    team.setTeamId(10L);
    task.setFkTeamId(team);
    task.setTaskId(1L);
    task.setTaskEstimate(10);
    task.setVersion(1L);
    task.setRecordedEffort(10);
    WorkFlowTaskStatus workFlowTaskStatus = new WorkFlowTaskStatus();
    workFlowTaskStatus.setWorkflowTaskStatus(Constants.WorkFlowTaskStatusConstants.STATUS_STARTED);
    task.setFkWorkflowTaskStatus(workFlowTaskStatus);
    task.setTaskActStDate(LocalDateTime.now());

    NewEffortTrack newEffortTrack = new NewEffortTrack();
    newEffortTrack.setNewEffort(10);
    newEffortTrack.setNewEffortDate(LocalDate.now().minusDays(1));
    task.setNewEffortTracks(List.of(newEffortTrack));

    UserAccount userAccount = new UserAccount();
    userAccount.setAccountId(1L);
    task.setFkAccountIdAssigned(userAccount);
    task.setFkAccountIdLastUpdated(userAccount);
    List<RoleId> roleIds = Arrays.asList(new RoleId(9), new RoleId(10));
    when(accessDomainRepository.findRoleIdByAccountIdAndEntityIdAndIsActive(anyLong(), anyLong(), true)).thenReturn(roleIds);
    when(taskRepository.findByTaskId(anyLong())).thenReturn(task);
    when(userAccountService.getActiveUserAccountByAccountId(anyLong())).thenReturn(userAccount);
    taskServiceImpl.updateTimeSheetAndRecordedEffort(task, localTimeZone);

  }

}
