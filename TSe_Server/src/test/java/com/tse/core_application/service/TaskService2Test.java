package com.tse.core_application.service;

import com.tse.core_application.custom.model.ActionId;
import com.tse.core_application.custom.model.CustomAccessDomain;
import com.tse.core_application.custom.model.TaskMaster;
import com.tse.core_application.dto.StatsRequest;
import com.tse.core_application.exception.InvalidStatsRequestFilterException;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.model.*;
import com.tse.core_application.repository.TaskRepository;
import com.tse.core_application.service.Impl.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.tse.core_application.model.Constants.WorkFlowTaskStatusConstants.*;
import static com.tse.core_application.model.StatType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class TaskService2Test {

  @Mock
  private UserAccountService userAccountService;

  @Mock
  private RoleActionService roleActionService;


  @Mock
  private WorkflowTaskStatusService workflowTaskStatusService;

  @Mock
  private TaskRepository taskRepository;

  @Mock
  private TaskServiceImpl taskServiceImpl;

  @Mock
  private StatsService statsService;

  @Mock
  private AccessDomainService accessDomainService;

  @InjectMocks
  private TaskService taskService;

  @Test
  public void testValidateStatsRequestFiltersForMyTask() {
    StatsRequest statsRequest = mock(StatsRequest.class);
    UserAccount userAccount = new UserAccount();
    userAccount.setAccountId(1L);
    List<UserAccount> userAccounts = new ArrayList<>();
    userAccounts.add(userAccount);
    CustomAccessDomain customAccessDomain = new CustomAccessDomain();
    List<CustomAccessDomain> accessDomainsFoundDb = new ArrayList<>();
    accessDomainsFoundDb.add(customAccessDomain);


    lenient().when(statsRequest.getTeamId()).thenReturn(1L);
    lenient().when(statsRequest.getUserId()).thenReturn(1L);
    lenient().when(userAccountService.getAllUserAccountByUserIdAndIsActive(anyLong())).thenReturn(userAccounts);
    lenient().when(accessDomainService.getAccessDomainsByAccountIdsAndEntityId(anyLong(), anyList())).thenReturn(accessDomainsFoundDb);
    boolean res = taskService.validateStatsRequestFiltersForMyTask(statsRequest);
    assertThat(res).isNotNull();
    assertThat(res).isEqualTo(true);

  }

  @Test
  public void testValidateStatsRequestFiltersForMyTaskExceptionThrows() {
    StatsRequest statsRequest = mock(StatsRequest.class);
    UserAccount userAccount = new UserAccount();
    userAccount.setAccountId(1L);
    List<UserAccount> userAccounts = new ArrayList<>();
    userAccounts.add(userAccount);
    List<CustomAccessDomain> accessDomainsFoundDb = new ArrayList<>();

    lenient().when(statsRequest.getTeamId()).thenReturn(1L);
    lenient().when(statsRequest.getUserId()).thenReturn(1L);
    lenient().when(userAccountService.getAllUserAccountByUserIdAndIsActive(anyLong())).thenReturn(userAccounts);
    lenient().when(accessDomainService.getAccessDomainsByAccountIdsAndEntityId(anyLong(), anyList())).thenReturn(accessDomainsFoundDb);
    try {
      boolean res = taskService.validateStatsRequestFiltersForMyTask(statsRequest);
      assertThat(res).isNotNull();
      assertThat(res).isEqualTo(true);
    } catch (ValidationFailedException e) {
      assertThat(e.getMessage()).containsIgnoringCase("Validation Failed: Selected Team is not Validated");
    }
  }


  @Test
  public void testGetAllFilteredTaskFromAllTeamForUser() {

    StatsRequest statsRequest = mock(StatsRequest.class);
    String accountIds = "1";
    String timeZone = "Z";
    CustomAccessDomain customAccessDomain = mock(CustomAccessDomain.class);
    List<CustomAccessDomain> customAccessDomains = new ArrayList<>();
    ActionId actionId = new ActionId(1);
    customAccessDomains.add(customAccessDomain);

    List<Long> values = new ArrayList<>();
    values.add(1L);
    TaskPriority taskPriority = TaskPriority.P1;

    List<TaskPriority> taskPriorities = new ArrayList<>();
    taskPriorities.add(taskPriority);

    ArrayList<ActionId> actionIdList = new ArrayList<>();
    actionIdList.add(actionId);
    WorkFlowTaskStatus workFlowTaskStatus = mock(WorkFlowTaskStatus.class);
    List<WorkFlowTaskStatus> workFlowTaskStatuses = new ArrayList<>();
    workFlowTaskStatuses.add(workFlowTaskStatus);
    Task task = mock(Task.class);
    List<Task> tasks = new ArrayList<>();
    tasks.add(task);
    WorkFlowType workFlowType = mock(WorkFlowType.class);
    Team team = mock(Team.class);
    UserAccount userAccount = mock(UserAccount.class);
    User user = mock(User.class);

    lenient().doNothing().when(statsService).setDefaultFromAndToDateToStatsRequest(any(StatsRequest.class), anyString());
    lenient().when(statsRequest.getTeamId()).thenReturn(1L);
    lenient().when(customAccessDomain.getAccountId()).thenReturn(1L);
    lenient().when(roleActionService.getActionIdByRoleId(anyInt())).thenReturn(actionIdList);
    lenient().when(customAccessDomain.getEntityId()).thenReturn(1L);
    lenient().when(customAccessDomain.getRoleId()).thenReturn(1);
    lenient().when(statsRequest.getAccountIdAssigned()).thenReturn(1L);
    lenient().when(statsRequest.getNoOfDays()).thenReturn(1L);
    lenient().when(statsRequest.getAccountIds()).thenReturn(values);
    lenient().when(statsRequest.getOrgIds()).thenReturn(values);
    lenient().when(statsRequest.getBuId()).thenReturn(null);
    lenient().when(statsRequest.getProjectId()).thenReturn(1L);
    lenient().when(statsRequest.getTaskPriority()).thenReturn(taskPriorities);
    lenient().when(statsRequest.getTaskWorkflowId()).thenReturn(1L);
    lenient().when(statsRequest.getWorkflowTaskStatus()).thenReturn(List.of("TEST"));
    lenient().when(workflowTaskStatusService.getAllWorkflowTaskStatusByWorkflowTaskStatus(anyString())).thenReturn(workFlowTaskStatuses);
    lenient().when(accessDomainService.getAccessDomainByAccountIdAndEntityId(anyLong(), anyLong())).thenReturn(customAccessDomains);
    lenient().when(statsRequest.getStatName()).thenReturn(List.of(COMPLETED));
    lenient().when(statsRequest.getFromDate()).thenReturn(LocalDateTime.now());
    lenient().when(statsRequest.getToDate()).thenReturn(LocalDateTime.now());
    lenient().when(statsRequest.getFromDateType()).thenReturn("TEST");
    lenient().when(statsRequest.getToDateType()).thenReturn("TEST");
    lenient().when(statsRequest.getSearches()).thenReturn(Arrays.asList("My","Task","DESC"));
    lenient().when(taskRepository.findAll(any(Specification.class))).thenReturn(tasks);
    lenient().doNothing().when(taskServiceImpl).convertTaskAllServerDateAndTimeInToUserTimeZone(any(Task.class), anyString());
    lenient().when(task.getFkWorkflowTaskStatus()).thenReturn(workFlowTaskStatus);
    lenient().when(workFlowTaskStatus.getFkWorkFlowType()).thenReturn(workFlowType);
    lenient().when(workFlowType.getWorkflowName()).thenReturn("TEST");
    lenient().when(task.getFkTeamId()).thenReturn(team);
    lenient().when(team.getTeamName()).thenReturn("TEST");
    lenient().when(task.getFkAccountIdAssigned()).thenReturn(userAccount);
    lenient().when(userAccount.getFkUserId()).thenReturn(user);
    lenient().when(userAccount.getEmail()).thenReturn("TEST@gmail.com");
    lenient().when(user.getFirstName()).thenReturn("TEST");
    lenient().when(user.getLastName()).thenReturn("TEST");


    List<TaskMaster> res = taskService.getAllTaskByFilter(statsRequest, accountIds, timeZone);
    assertThat(res).isNotNull();
    assertThat(res).isNotEmpty();
    assertThat(res.size()).isEqualTo(1);
  }


  @Test
  public void testGetAllFilteredTaskFromAllTeamForUserTeamIdNullAndInvalidStatsRequestFilterExceptionCase() {

    StatsRequest statsRequest = mock(StatsRequest.class);
    String accountIds = "1";
    String timeZone = "Z";
    CustomAccessDomain customAccessDomain = mock(CustomAccessDomain.class);
    List<CustomAccessDomain> customAccessDomains = new ArrayList<>();
    //ActionId actionId = mock(ActionId.class);
    ActionId actionId = new ActionId(2);
    customAccessDomains.add(customAccessDomain);

    List<Long> values = new ArrayList<>();
    values.add(1L);
    //TaskPriority taskPriority = mock(TaskPriority.class);
    TaskPriority taskPriority = TaskPriority.P1;

    List<TaskPriority> taskPriorities = new ArrayList<>();
    taskPriorities.add(taskPriority);

    ArrayList<ActionId> actionIdList = new ArrayList<>();
    actionIdList.add(actionId);
    WorkFlowTaskStatus workFlowTaskStatus = mock(WorkFlowTaskStatus.class);
    List<WorkFlowTaskStatus> workFlowTaskStatuses = new ArrayList<>();
    workFlowTaskStatuses.add(workFlowTaskStatus);
    Task task = mock(Task.class);
    List<Task> tasks = new ArrayList<>();
    tasks.add(task);
    WorkFlowType workFlowType = mock(WorkFlowType.class);
    Team team = mock(Team.class);
    UserAccount userAccount = mock(UserAccount.class);
    User user = mock(User.class);


    lenient().doNothing().when(statsService).setDefaultFromAndToDateToStatsRequest(any(StatsRequest.class), anyString());
    lenient().when(statsRequest.getTeamId()).thenReturn(null);
    lenient().when(customAccessDomain.getAccountId()).thenReturn(1L);
    lenient().when(roleActionService.getActionIdByRoleId(anyInt())).thenReturn(actionIdList);
    lenient().when(customAccessDomain.getEntityId()).thenReturn(1L);
    lenient().when(customAccessDomain.getRoleId()).thenReturn(1);
    lenient().when(statsRequest.getAccountIdAssigned()).thenReturn(1L);
    lenient().when(accessDomainService.findAllActiveAccessDomainByAccountId(anyLong())).thenReturn(customAccessDomains);
    lenient().when(statsRequest.getNoOfDays()).thenReturn(1L);
    lenient().when(statsRequest.getAccountIds()).thenReturn(values);
    lenient().when(statsRequest.getOrgIds()).thenReturn(values);
    lenient().when(statsRequest.getProjectId()).thenReturn(1L);
    lenient().when(statsRequest.getTaskPriority()).thenReturn(taskPriorities);
    lenient().when(statsRequest.getTaskWorkflowId()).thenReturn(1L);
    lenient().when(statsRequest.getWorkflowTaskStatus()).thenReturn(List.of("TEST"));
    lenient().when(statsRequest.getSearches()).thenReturn(Arrays.asList("My","Task","DESC"));
    lenient().when(workflowTaskStatusService.getAllWorkflowTaskStatusByWorkflowTaskStatus(anyString())).thenReturn(workFlowTaskStatuses);
    lenient().when(accessDomainService.getAccessDomainByAccountIdAndEntityId(anyLong(), anyLong())).thenReturn(customAccessDomains);
    lenient().when(statsRequest.getStatName()).thenReturn(List.of(COMPLETED));
    lenient().when(customAccessDomain.getEntityTypeId()).thenReturn(5);
    lenient().when(statsRequest.getFromDate()).thenReturn(LocalDateTime.now());
    lenient().when(statsRequest.getToDate()).thenReturn(LocalDateTime.now());
    lenient().when(statsRequest.getFromDateType()).thenReturn("TEST");
    lenient().when(statsRequest.getToDateType()).thenReturn("TEST");
    lenient().when(taskRepository.findAll(any(Specification.class))).thenReturn(tasks);
    lenient().doNothing().when(taskServiceImpl).convertTaskAllServerDateAndTimeInToUserTimeZone(any(Task.class), anyString());
    lenient().when(task.getFkWorkflowTaskStatus()).thenReturn(workFlowTaskStatus);
    lenient().when(workFlowTaskStatus.getFkWorkFlowType()).thenReturn(workFlowType);
    lenient().when(workFlowType.getWorkflowName()).thenReturn("TEST");
    lenient().when(task.getFkTeamId()).thenReturn(team);
    lenient().when(team.getTeamName()).thenReturn("TEST");
    lenient().when(task.getFkAccountIdAssigned()).thenReturn(userAccount);
    lenient().when(userAccount.getFkUserId()).thenReturn(user);
    lenient().when(userAccount.getEmail()).thenReturn("TEST@gmail.com");
    lenient().when(user.getFirstName()).thenReturn("TEST");
    lenient().when(user.getLastName()).thenReturn("TEST");


    try {
      List<TaskMaster> res = taskService.getAllTaskByFilter(statsRequest, accountIds, timeZone);
      assertThat(res).isNotNull();
      assertThat(res).isNotEmpty();
      assertThat(res.size()).isEqualTo(1);
    } catch (InvalidStatsRequestFilterException e) {
      assertThat(e.getMessage()).containsIgnoringCase("BU filter is not available");
    }
  }


  @Test
  public void testGetAllFilteredTaskFromAllTeamForUserZeroSpecsCase() {

    StatsRequest statsRequest = mock(StatsRequest.class);
    String accountIds = "1";
    String timeZone = "Z";
    CustomAccessDomain customAccessDomain = mock(CustomAccessDomain.class);
    List<CustomAccessDomain> customAccessDomains = new ArrayList<>();
    //ActionId actionId = mock(ActionId.class);
    ActionId actionId = new ActionId(1);
    customAccessDomains.add(customAccessDomain);

    List<Long> values = new ArrayList<>();
    //values.add(1L);
    //TaskPriority taskPriority = mock(TaskPriority.class);
    TaskPriority taskPriority = TaskPriority.P1;

    List<TaskPriority> taskPriorities = new ArrayList<>();
    taskPriorities.add(taskPriority);

    ArrayList<ActionId> actionIdList = new ArrayList<>();
    actionIdList.add(actionId);
    WorkFlowTaskStatus workFlowTaskStatus = mock(WorkFlowTaskStatus.class);
    List<WorkFlowTaskStatus> workFlowTaskStatuses = new ArrayList<>();
    workFlowTaskStatuses.add(workFlowTaskStatus);
    Task task = mock(Task.class);
    List<Task> tasks = new ArrayList<>();
    tasks.add(task);
    WorkFlowType workFlowType = mock(WorkFlowType.class);
    Team team = mock(Team.class);
    UserAccount userAccount = mock(UserAccount.class);
    User user = mock(User.class);


    lenient().doNothing().when(statsService).setDefaultFromAndToDateToStatsRequest(any(StatsRequest.class), anyString());
    lenient().when(statsRequest.getTeamId()).thenReturn(null);

    lenient().when(customAccessDomain.getAccountId()).thenReturn(1L);
    lenient().when(roleActionService.getActionIdByRoleId(anyInt())).thenReturn(actionIdList);
    lenient().when(customAccessDomain.getEntityId()).thenReturn(1L);
    lenient().when(customAccessDomain.getRoleId()).thenReturn(1);
    lenient().when(statsRequest.getAccountIdAssigned()).thenReturn(1L);
    lenient().when(statsRequest.getNoOfDays()).thenReturn(null);
    lenient().when(statsRequest.getAccountIds()).thenReturn(values);
    lenient().when(statsRequest.getOrgIds()).thenReturn(values);
    lenient().when(statsRequest.getBuId()).thenReturn(null);
    lenient().when(statsRequest.getProjectId()).thenReturn(null);
    lenient().when(statsRequest.getTaskPriority()).thenReturn(null);
    lenient().when(accessDomainService.findAllActiveAccessDomainByAccountId(anyLong())).thenReturn(customAccessDomains);
    lenient().when(statsRequest.getTaskWorkflowId()).thenReturn(null);
    lenient().when(statsRequest.getWorkflowTaskStatus()).thenReturn(null);
    lenient().when(statsRequest.getSearches()).thenReturn(Arrays.asList("My","Task","DESC"));
    lenient().when(workflowTaskStatusService.getAllWorkflowTaskStatusByWorkflowTaskStatus(anyString())).thenReturn(workFlowTaskStatuses);
    lenient().when(accessDomainService.getAccessDomainByAccountIdAndEntityId(anyLong(), anyLong())).thenReturn(customAccessDomains);
    lenient().when(statsRequest.getStatName()).thenReturn(null);
    lenient().when(customAccessDomain.getEntityTypeId()).thenReturn(5);
    lenient().when(statsRequest.getFromDate()).thenReturn(LocalDateTime.now());
    lenient().when(statsRequest.getToDate()).thenReturn(LocalDateTime.now());
    lenient().when(statsRequest.getFromDateType()).thenReturn(null);
    lenient().when(statsRequest.getToDateType()).thenReturn(null);
    lenient().when(taskRepository.findAll()).thenReturn(tasks);
    lenient().doNothing().when(taskServiceImpl).convertTaskAllServerDateAndTimeInToUserTimeZone(any(Task.class), anyString());
    lenient().when(task.getFkWorkflowTaskStatus()).thenReturn(workFlowTaskStatus);
    lenient().when(workFlowTaskStatus.getFkWorkFlowType()).thenReturn(workFlowType);
    lenient().when(workFlowType.getWorkflowName()).thenReturn("TEST");
    lenient().when(task.getFkTeamId()).thenReturn(team);
    lenient().when(team.getTeamName()).thenReturn("TEST");
    lenient().when(task.getFkAccountIdAssigned()).thenReturn(userAccount);
    lenient().when(userAccount.getFkUserId()).thenReturn(user);
    lenient().when(userAccount.getEmail()).thenReturn("TEST@gmail.com");
    lenient().when(user.getFirstName()).thenReturn("TEST");
    lenient().when(user.getLastName()).thenReturn("TEST");


    List<TaskMaster> res = taskService.getAllTaskByFilter(statsRequest, accountIds, timeZone);
    assertThat(res).isNotNull();
    assertThat(res).isNotEmpty();
    assertThat(res.size()).isEqualTo(1);
  }

  @Test
  public void testGetTaskDetailsForStatus() {

    StatsRequest statsRequest = mock(StatsRequest.class);
    String timeZone = "Z";
    List<Long> values = List.of(1L);
    List<TaskPriority> taskPriorities = List.of(TaskPriority.P0);
    WorkFlowType workFlowType = new WorkFlowType();
    workFlowType.setWorkflowName("NA");
    Team team = new Team();
    team.setTeamId(1L);
    UserAccount userAccount = new UserAccount();
    userAccount.setAccountId(1L);
    User user = new User();
    user.setFirstName("NA");
    user.setLastName("NA");
    userAccount.setFkUserId(user);
    userAccount.setEmail("NA@gmail.com");
    List<UserAccount> userAccounts = new ArrayList<>();
    userAccounts.add(userAccount);

    List<StatType> types = List.of(DELAYED);
    List<WorkFlowTaskStatus> workFlowTaskStatuses = new ArrayList<>();

    WorkFlowTaskStatus workFlowTaskStatus = new WorkFlowTaskStatus();
    workFlowTaskStatus.setWorkflowTaskStatus(STATUS_STARTED);
    workFlowTaskStatus.setFkWorkFlowType(workFlowType);
    workFlowTaskStatus.setWorkflowTaskStatusId(1);

    WorkFlowTaskStatus workFlowTaskStatus2 = new WorkFlowTaskStatus();
    workFlowTaskStatus2.setWorkflowTaskStatus(STATUS_BACKLOG);
    workFlowTaskStatus2.setFkWorkFlowType(workFlowType);
    workFlowTaskStatus2.setWorkflowTaskStatusId(2);

    WorkFlowTaskStatus workFlowTaskStatus3 = new WorkFlowTaskStatus();
    workFlowTaskStatus3.setWorkflowTaskStatus(STATUS_BLOCKED);
    workFlowTaskStatus3.setFkWorkFlowType(workFlowType);
    workFlowTaskStatus3.setWorkflowTaskStatusId(3);


    WorkFlowTaskStatus workFlowTaskStatus4 = new WorkFlowTaskStatus();
    workFlowTaskStatus4.setWorkflowTaskStatus(STATUS_BLOCKED);
    workFlowTaskStatus4.setFkWorkFlowType(workFlowType);
    workFlowTaskStatus4.setWorkflowTaskStatusId(4);

    WorkFlowTaskStatus workFlowTaskStatus5 = new WorkFlowTaskStatus();
    workFlowTaskStatus5.setFkWorkFlowType(workFlowType);
    workFlowTaskStatus5.setWorkflowTaskStatusId(5);
    workFlowTaskStatus5.setWorkflowTaskStatus(STATUS_ON_HOLD);

    WorkFlowTaskStatus workFlowTaskStatus6 = new WorkFlowTaskStatus();
    workFlowTaskStatus6.setFkWorkFlowType(workFlowType);
    workFlowTaskStatus6.setWorkflowTaskStatusId(6);
    workFlowTaskStatus6.setWorkflowTaskStatus(STATUS_COMPLETED);

    workFlowTaskStatuses.add(workFlowTaskStatus);
    workFlowTaskStatuses.add(workFlowTaskStatus2);
    workFlowTaskStatuses.add(workFlowTaskStatus3);
    workFlowTaskStatuses.add(workFlowTaskStatus4);
    workFlowTaskStatuses.add(workFlowTaskStatus3);
    workFlowTaskStatuses.add(workFlowTaskStatus5);
    workFlowTaskStatuses.add(workFlowTaskStatus6);

    Task task = new Task();
    task.setFkWorkflowTaskStatus(workFlowTaskStatus);
    task.setTaskProgressSystem(DELAYED);
    task.setTaskExpEndDate(LocalDateTime.of(LocalDate.now(), LocalTime.of(23, 59)));
    List<Task> tasks = new ArrayList<>();
    task.setFkTeamId(team);
    task.setFkAccountIdAssigned(userAccount);
    tasks.add(task);


    CustomAccessDomain customAccessDomain = mock(CustomAccessDomain.class);
    List<CustomAccessDomain> accessDomainsFoundDb = new ArrayList<>();
    accessDomainsFoundDb.add(customAccessDomain);

    lenient().doNothing().when(statsService).setDefaultFromAndToDateToStatsRequest(any(StatsRequest.class), anyString());
    lenient().when(statsRequest.getAccountIds()).thenReturn(values);
    lenient().when(statsRequest.getAccountIdAssigned()).thenReturn(1L);
    lenient().when(statsRequest.getNoOfDays()).thenReturn(1L);
    lenient().when(statsRequest.getOrgIds()).thenReturn(values);
    lenient().when(statsRequest.getProjectId()).thenReturn(1L);
    lenient().when(statsRequest.getTeamId()).thenReturn(1L);
    lenient().when(statsRequest.getBuId()).thenReturn(null);
    lenient().when(statsRequest.getTaskPriority()).thenReturn(taskPriorities);
    lenient().when(statsRequest.getTaskWorkflowId()).thenReturn(1L);
    lenient().when(statsRequest.getStatName()).thenReturn(types);
    lenient().when(userAccountService.getAllUserAccountByUserIdAndIsActive(anyLong())).thenReturn(userAccounts);
    lenient().when(accessDomainService.getAccessDomainsByAccountIdsAndEntityId(anyLong(), anyList())).thenReturn(accessDomainsFoundDb);
    lenient().when(statsRequest.getWorkflowTaskStatus()).thenReturn(List.of("completed"));

    lenient().when(workflowTaskStatusService.getAllWorkflowTaskStatus()).thenReturn(workFlowTaskStatuses);
    lenient().when(taskRepository.findAll(any(Specification.class))).thenReturn(tasks);
    lenient().when(statsService.removeBacklogDeleteTasks(anyList())).thenReturn(tasks);
    lenient().when(statsRequest.getCurrentDate()).thenReturn(LocalDateTime.now());
    lenient().doNothing().when(taskServiceImpl).convertTaskAllServerDateAndTimeInToUserTimeZone(any(Task.class), anyString());
    List<TaskMaster> taskMasters = taskService.getTaskDetailsForStatus(statsRequest, timeZone, anyString());
    assertThat(taskMasters).isNotNull();
//    assertThat(taskMasters.size()).isNotZero();

  }

  @Test
  public void testGetTaskDetailsForStatusNotStartedCase() {

    StatsRequest statsRequest = mock(StatsRequest.class);
    String timeZone = "Z";
    List<Long> values = List.of(1L);
    List<TaskPriority> taskPriorities = List.of(TaskPriority.P0);
    WorkFlowType workFlowType = new WorkFlowType();
    workFlowType.setWorkflowName("NA");
    Team team = new Team();
    team.setTeamId(1L);
    UserAccount userAccount = new UserAccount();
    userAccount.setAccountId(1L);
    User user = new User();
    user.setFirstName("NA");
    user.setLastName("NA");
    userAccount.setFkUserId(user);
    userAccount.setEmail("NA@gmail.com");
    List<UserAccount> userAccounts = new ArrayList<>();
    userAccounts.add(userAccount);

    List<StatType> types = List.of(NOTSTARTED);
    List<WorkFlowTaskStatus> workFlowTaskStatuses = new ArrayList<>();

    WorkFlowTaskStatus workFlowTaskStatus = new WorkFlowTaskStatus();
    workFlowTaskStatus.setWorkflowTaskStatus(STATUS_STARTED);
    workFlowTaskStatus.setFkWorkFlowType(workFlowType);
    workFlowTaskStatus.setWorkflowTaskStatusId(1);

    WorkFlowTaskStatus workFlowTaskStatus2 = new WorkFlowTaskStatus();
    workFlowTaskStatus2.setWorkflowTaskStatus(STATUS_BACKLOG);
    workFlowTaskStatus2.setFkWorkFlowType(workFlowType);
    workFlowTaskStatus2.setWorkflowTaskStatusId(2);

    WorkFlowTaskStatus workFlowTaskStatus3 = new WorkFlowTaskStatus();
    workFlowTaskStatus3.setWorkflowTaskStatus(STATUS_BLOCKED);
    workFlowTaskStatus3.setFkWorkFlowType(workFlowType);
    workFlowTaskStatus3.setWorkflowTaskStatusId(3);


    WorkFlowTaskStatus workFlowTaskStatus4 = new WorkFlowTaskStatus();
    workFlowTaskStatus4.setWorkflowTaskStatus(STATUS_BLOCKED);
    workFlowTaskStatus4.setFkWorkFlowType(workFlowType);
    workFlowTaskStatus4.setWorkflowTaskStatusId(4);

    WorkFlowTaskStatus workFlowTaskStatus5 = new WorkFlowTaskStatus();
    workFlowTaskStatus5.setFkWorkFlowType(workFlowType);
    workFlowTaskStatus5.setWorkflowTaskStatusId(5);
    workFlowTaskStatus5.setWorkflowTaskStatus(STATUS_ON_HOLD);

    WorkFlowTaskStatus workFlowTaskStatus6 = new WorkFlowTaskStatus();
    workFlowTaskStatus6.setFkWorkFlowType(workFlowType);
    workFlowTaskStatus6.setWorkflowTaskStatusId(6);
    workFlowTaskStatus6.setWorkflowTaskStatus(STATUS_COMPLETED);

    workFlowTaskStatuses.add(workFlowTaskStatus);
    workFlowTaskStatuses.add(workFlowTaskStatus2);
    workFlowTaskStatuses.add(workFlowTaskStatus3);
    workFlowTaskStatuses.add(workFlowTaskStatus4);
    workFlowTaskStatuses.add(workFlowTaskStatus3);
    workFlowTaskStatuses.add(workFlowTaskStatus5);
    workFlowTaskStatuses.add(workFlowTaskStatus6);

    Task task = new Task();
    task.setFkWorkflowTaskStatus(workFlowTaskStatus);
    task.setTaskProgressSystem(NOTSTARTED);
    task.setTaskExpEndDate(LocalDateTime.of(LocalDate.now(), LocalTime.of(23, 59)));
    List<Task> tasks = new ArrayList<>();
    task.setFkTeamId(team);
    task.setFkAccountIdAssigned(userAccount);
    tasks.add(task);


    CustomAccessDomain customAccessDomain = mock(CustomAccessDomain.class);
    List<CustomAccessDomain> accessDomainsFoundDb = new ArrayList<>();
    accessDomainsFoundDb.add(customAccessDomain);

    lenient().doNothing().when(statsService).setDefaultFromAndToDateToStatsRequest(any(StatsRequest.class), anyString());
    lenient().when(statsRequest.getAccountIds()).thenReturn(null);
    lenient().when(statsRequest.getAccountIdAssigned()).thenReturn(1L);
    lenient().when(statsRequest.getNoOfDays()).thenReturn(1L);
    lenient().when(statsRequest.getOrgIds()).thenReturn(values);
    lenient().when(statsRequest.getProjectId()).thenReturn(1L);
    lenient().when(statsRequest.getTeamId()).thenReturn(1L);
    lenient().when(statsRequest.getBuId()).thenReturn(null);
    lenient().when(statsRequest.getTaskPriority()).thenReturn(taskPriorities);
    lenient().when(statsRequest.getTaskWorkflowId()).thenReturn(1L);
    lenient().when(statsRequest.getStatName()).thenReturn(types);
    lenient().when(userAccountService.getAllUserAccountByUserIdAndIsActive(anyLong())).thenReturn(userAccounts);
    lenient().when(accessDomainService.getAccessDomainsByAccountIdsAndEntityId(anyLong(), anyList())).thenReturn(accessDomainsFoundDb);
    lenient().when(statsRequest.getWorkflowTaskStatus()).thenReturn(List.of("completed"));

    lenient().when(workflowTaskStatusService.getAllWorkflowTaskStatus()).thenReturn(workFlowTaskStatuses);
    lenient().when(taskRepository.findAll(any(Specification.class))).thenReturn(tasks);
    lenient().when(statsService.removeBacklogDeleteTasks(anyList())).thenReturn(tasks);
    lenient().when(statsRequest.getCurrentDate()).thenReturn(LocalDateTime.now());
    lenient().doNothing().when(taskServiceImpl).convertTaskAllServerDateAndTimeInToUserTimeZone(any(Task.class), anyString());
    List<TaskMaster> taskMasters = taskService.getTaskDetailsForStatus(statsRequest, timeZone, anyString());
    assertThat(taskMasters).isNotNull();
//    assertThat(taskMasters.size()).isNotZero();

  }

  @Test
  public void testGetTaskDetailsForStatusOntrackCase() {

    StatsRequest statsRequest = mock(StatsRequest.class);
    String timeZone = "Z";
    List<Long> values = List.of(1L);
    List<TaskPriority> taskPriorities = List.of(TaskPriority.P0);
    WorkFlowType workFlowType = new WorkFlowType();
    workFlowType.setWorkflowName("NA");
    Team team = new Team();
    team.setTeamId(1L);
    UserAccount userAccount = new UserAccount();
    userAccount.setAccountId(1L);
    User user = new User();
    user.setFirstName("NA");
    user.setLastName("NA");
    userAccount.setFkUserId(user);
    userAccount.setEmail("NA@gmail.com");
    List<UserAccount> userAccounts = new ArrayList<>();
    userAccounts.add(userAccount);

    List<StatType> types = List.of(ONTRACK);
    List<WorkFlowTaskStatus> workFlowTaskStatuses = new ArrayList<>();

    WorkFlowTaskStatus workFlowTaskStatus = new WorkFlowTaskStatus();
    workFlowTaskStatus.setWorkflowTaskStatus(STATUS_STARTED);
    workFlowTaskStatus.setFkWorkFlowType(workFlowType);
    workFlowTaskStatus.setWorkflowTaskStatusId(1);

    WorkFlowTaskStatus workFlowTaskStatus2 = new WorkFlowTaskStatus();
    workFlowTaskStatus2.setWorkflowTaskStatus(STATUS_BACKLOG);
    workFlowTaskStatus2.setFkWorkFlowType(workFlowType);
    workFlowTaskStatus2.setWorkflowTaskStatusId(2);

    WorkFlowTaskStatus workFlowTaskStatus3 = new WorkFlowTaskStatus();
    workFlowTaskStatus3.setWorkflowTaskStatus(STATUS_BLOCKED);
    workFlowTaskStatus3.setFkWorkFlowType(workFlowType);
    workFlowTaskStatus3.setWorkflowTaskStatusId(3);


    WorkFlowTaskStatus workFlowTaskStatus4 = new WorkFlowTaskStatus();
    workFlowTaskStatus4.setWorkflowTaskStatus(STATUS_BLOCKED);
    workFlowTaskStatus4.setFkWorkFlowType(workFlowType);
    workFlowTaskStatus4.setWorkflowTaskStatusId(4);

    WorkFlowTaskStatus workFlowTaskStatus5 = new WorkFlowTaskStatus();
    workFlowTaskStatus5.setFkWorkFlowType(workFlowType);
    workFlowTaskStatus5.setWorkflowTaskStatusId(5);
    workFlowTaskStatus5.setWorkflowTaskStatus(STATUS_ON_HOLD);

    WorkFlowTaskStatus workFlowTaskStatus6 = new WorkFlowTaskStatus();
    workFlowTaskStatus6.setFkWorkFlowType(workFlowType);
    workFlowTaskStatus6.setWorkflowTaskStatusId(6);
    workFlowTaskStatus6.setWorkflowTaskStatus(STATUS_COMPLETED);

    workFlowTaskStatuses.add(workFlowTaskStatus);
    workFlowTaskStatuses.add(workFlowTaskStatus2);
    workFlowTaskStatuses.add(workFlowTaskStatus3);
    workFlowTaskStatuses.add(workFlowTaskStatus4);
    workFlowTaskStatuses.add(workFlowTaskStatus3);
    workFlowTaskStatuses.add(workFlowTaskStatus5);
    workFlowTaskStatuses.add(workFlowTaskStatus6);

    Task task = new Task();
    task.setFkWorkflowTaskStatus(workFlowTaskStatus);
    task.setTaskProgressSystem(ONTRACK);
    task.setTaskExpEndDate(LocalDateTime.of(LocalDate.now(), LocalTime.of(23, 59)));
    List<Task> tasks = new ArrayList<>();
    task.setFkTeamId(team);
    task.setFkAccountIdAssigned(userAccount);
    tasks.add(task);

    ArrayList<ActionId> actionIds = new ArrayList<>();
    actionIds.add(new ActionId(2));


    CustomAccessDomain customAccessDomain = mock(CustomAccessDomain.class);
    List<CustomAccessDomain> accessDomainsFoundDb = new ArrayList<>();
    accessDomainsFoundDb.add(customAccessDomain);

    lenient().doNothing().when(statsService).setDefaultFromAndToDateToStatsRequest(any(StatsRequest.class), anyString());
    lenient().when(statsRequest.getAccountIds()).thenReturn(null);
    lenient().when(statsRequest.getAccountIdAssigned()).thenReturn(1L);
    lenient().when(statsRequest.getNoOfDays()).thenReturn(1L);
    lenient().when(accessDomainService.getAccessDomainsByAccountIdsAndEntityTypeIdAndIsActive(anyInt(), anyList())).thenReturn(accessDomainsFoundDb);
    lenient().when(roleActionService.getActionIdByRoleId(anyInt())).thenReturn(actionIds);
    lenient().when(statsRequest.getBuId()).thenReturn(null);
    lenient().when(statsRequest.getProjectId()).thenReturn(null);
    lenient().when(statsRequest.getTeamId()).thenReturn(null);

    lenient().when(statsRequest.getTaskPriority()).thenReturn(taskPriorities);
    lenient().when(statsRequest.getTaskWorkflowId()).thenReturn(1L);
    lenient().when(statsRequest.getStatName()).thenReturn(types);
    lenient().when(userAccountService.getAllUserAccountByUserIdAndIsActive(anyLong())).thenReturn(userAccounts);
    lenient().when(accessDomainService.getAccessDomainsByAccountIdsAndEntityId(anyLong(), anyList())).thenReturn(accessDomainsFoundDb);
    lenient().when(statsRequest.getWorkflowTaskStatus()).thenReturn(List.of("completed"));

    lenient().when(workflowTaskStatusService.getAllWorkflowTaskStatus()).thenReturn(workFlowTaskStatuses);
    lenient().when(taskRepository.findAll(any(Specification.class))).thenReturn(tasks);
    lenient().when(statsService.removeBacklogDeleteTasks(anyList())).thenReturn(tasks);
    lenient().when(statsRequest.getCurrentDate()).thenReturn(LocalDateTime.now());
    lenient().doNothing().when(taskServiceImpl).convertTaskAllServerDateAndTimeInToUserTimeZone(any(Task.class), anyString());
    List<TaskMaster> taskMasters = taskService.getTaskDetailsForStatus(statsRequest, timeZone, anyString());
    assertThat(taskMasters).isNotNull();
//    assertThat(taskMasters.size()).isNotZero();

  }

  @Test
  public void testGetTaskDetailsForStatusOnCompletedCase() {

    StatsRequest statsRequest = mock(StatsRequest.class);
    String timeZone = "Z";
    List<Long> values = List.of(1L);
    List<TaskPriority> taskPriorities = List.of(TaskPriority.P0);
    WorkFlowType workFlowType = new WorkFlowType();
    workFlowType.setWorkflowName("NA");
    Team team = new Team();
    team.setTeamId(1L);
    UserAccount userAccount = new UserAccount();
    userAccount.setAccountId(1L);
    User user = new User();
    user.setFirstName("NA");
    user.setLastName("NA");
    userAccount.setFkUserId(user);
    userAccount.setEmail("NA@gmail.com");
    List<UserAccount> userAccounts = new ArrayList<>();
    userAccounts.add(userAccount);

    List<StatType> types = List.of(COMPLETED);
    List<WorkFlowTaskStatus> workFlowTaskStatuses = new ArrayList<>();

    WorkFlowTaskStatus workFlowTaskStatus = new WorkFlowTaskStatus();
    workFlowTaskStatus.setWorkflowTaskStatus(STATUS_STARTED);
    workFlowTaskStatus.setFkWorkFlowType(workFlowType);
    workFlowTaskStatus.setWorkflowTaskStatusId(1);

    WorkFlowTaskStatus workFlowTaskStatus2 = new WorkFlowTaskStatus();
    workFlowTaskStatus2.setWorkflowTaskStatus(STATUS_BACKLOG);
    workFlowTaskStatus2.setFkWorkFlowType(workFlowType);
    workFlowTaskStatus2.setWorkflowTaskStatusId(2);

    WorkFlowTaskStatus workFlowTaskStatus3 = new WorkFlowTaskStatus();
    workFlowTaskStatus3.setWorkflowTaskStatus(STATUS_BLOCKED);
    workFlowTaskStatus3.setFkWorkFlowType(workFlowType);
    workFlowTaskStatus3.setWorkflowTaskStatusId(3);


    WorkFlowTaskStatus workFlowTaskStatus4 = new WorkFlowTaskStatus();
    workFlowTaskStatus4.setWorkflowTaskStatus(STATUS_BLOCKED);
    workFlowTaskStatus4.setFkWorkFlowType(workFlowType);
    workFlowTaskStatus4.setWorkflowTaskStatusId(4);

    WorkFlowTaskStatus workFlowTaskStatus5 = new WorkFlowTaskStatus();
    workFlowTaskStatus5.setFkWorkFlowType(workFlowType);
    workFlowTaskStatus5.setWorkflowTaskStatusId(5);
    workFlowTaskStatus5.setWorkflowTaskStatus(STATUS_ON_HOLD);

    WorkFlowTaskStatus workFlowTaskStatus6 = new WorkFlowTaskStatus();
    workFlowTaskStatus6.setFkWorkFlowType(workFlowType);
    workFlowTaskStatus6.setWorkflowTaskStatusId(6);
    workFlowTaskStatus6.setWorkflowTaskStatus(STATUS_COMPLETED);

    workFlowTaskStatuses.add(workFlowTaskStatus);
    workFlowTaskStatuses.add(workFlowTaskStatus2);
    workFlowTaskStatuses.add(workFlowTaskStatus3);
    workFlowTaskStatuses.add(workFlowTaskStatus4);
    workFlowTaskStatuses.add(workFlowTaskStatus3);
    workFlowTaskStatuses.add(workFlowTaskStatus5);
    workFlowTaskStatuses.add(workFlowTaskStatus6);

    Task task = new Task();
    task.setFkWorkflowTaskStatus(workFlowTaskStatus);
    task.setTaskProgressSystem(COMPLETED);
    task.setTaskExpEndDate(LocalDateTime.of(LocalDate.now(), LocalTime.of(23, 59)));
    List<Task> tasks = new ArrayList<>();
    task.setFkTeamId(team);
    task.setFkAccountIdAssigned(userAccount);
    tasks.add(task);

    ArrayList<ActionId> actionIds = new ArrayList<>();
    actionIds.add(new ActionId(1));


    CustomAccessDomain customAccessDomain = mock(CustomAccessDomain.class);
    List<CustomAccessDomain> accessDomainsFoundDb = new ArrayList<>();
    accessDomainsFoundDb.add(customAccessDomain);

    lenient().doNothing().when(statsService).setDefaultFromAndToDateToStatsRequest(any(StatsRequest.class), anyString());
    lenient().when(statsRequest.getAccountIds()).thenReturn(null);
    lenient().when(statsRequest.getAccountIdAssigned()).thenReturn(1L);
    lenient().when(statsRequest.getNoOfDays()).thenReturn(1L);
    lenient().when(accessDomainService.getAccessDomainsByAccountIdsAndEntityTypeIdAndIsActive(anyInt(), anyList())).thenReturn(accessDomainsFoundDb);
    lenient().when(roleActionService.getActionIdByRoleId(anyInt())).thenReturn(actionIds);
    lenient().when(statsRequest.getBuId()).thenReturn(null);
    lenient().when(statsRequest.getProjectId()).thenReturn(null);
    lenient().when(statsRequest.getTeamId()).thenReturn(null);

    lenient().when(statsRequest.getTaskPriority()).thenReturn(taskPriorities);
    lenient().when(statsRequest.getTaskWorkflowId()).thenReturn(1L);
    lenient().when(statsRequest.getStatName()).thenReturn(types);
    lenient().when(userAccountService.getAllUserAccountByUserIdAndIsActive(anyLong())).thenReturn(userAccounts);
    lenient().when(accessDomainService.getAccessDomainsByAccountIdsAndEntityId(anyLong(), anyList())).thenReturn(accessDomainsFoundDb);
    lenient().when(statsRequest.getWorkflowTaskStatus()).thenReturn(List.of("completed"));

    lenient().when(workflowTaskStatusService.getAllWorkflowTaskStatus()).thenReturn(workFlowTaskStatuses);
    lenient().when(taskRepository.findAll(any(Specification.class))).thenReturn(tasks);
    lenient().when(statsService.removeBacklogDeleteTasks(anyList())).thenReturn(tasks);
    lenient().when(statsRequest.getCurrentDate()).thenReturn(LocalDateTime.now());
    lenient().doNothing().when(taskServiceImpl).convertTaskAllServerDateAndTimeInToUserTimeZone(any(Task.class), anyString());
    List<TaskMaster> taskMasters = taskService.getTaskDetailsForStatus(statsRequest, timeZone, anyString());
    assertThat(taskMasters).isNotNull();
//    assertThat(taskMasters.size()).isNotZero();

  }

  @Test
  public void testGetTaskDetailsForStatusOnNotStartedCase() {

    StatsRequest statsRequest = mock(StatsRequest.class);
    String timeZone = "Z";
    List<Long> values = List.of(1L);
    List<TaskPriority> taskPriorities = List.of(TaskPriority.P0);
    WorkFlowType workFlowType = new WorkFlowType();
    workFlowType.setWorkflowName("NA");
    Team team = new Team();
    team.setTeamId(1L);
    UserAccount userAccount = new UserAccount();
    userAccount.setAccountId(1L);
    User user = new User();
    user.setFirstName("NA");
    user.setLastName("NA");
    userAccount.setFkUserId(user);
    userAccount.setEmail("NA@gmail.com");
    List<UserAccount> userAccounts = new ArrayList<>();
    userAccounts.add(userAccount);

    List<StatType> types = List.of(NOTSTARTED);
    List<WorkFlowTaskStatus> workFlowTaskStatuses = new ArrayList<>();

    WorkFlowTaskStatus workFlowTaskStatus = new WorkFlowTaskStatus();
    workFlowTaskStatus.setWorkflowTaskStatus(STATUS_STARTED);
    workFlowTaskStatus.setFkWorkFlowType(workFlowType);
    workFlowTaskStatus.setWorkflowTaskStatusId(1);

    WorkFlowTaskStatus workFlowTaskStatus2 = new WorkFlowTaskStatus();
    workFlowTaskStatus2.setWorkflowTaskStatus(STATUS_BACKLOG);
    workFlowTaskStatus2.setFkWorkFlowType(workFlowType);
    workFlowTaskStatus2.setWorkflowTaskStatusId(2);

    WorkFlowTaskStatus workFlowTaskStatus3 = new WorkFlowTaskStatus();
    workFlowTaskStatus3.setWorkflowTaskStatus(STATUS_BLOCKED);
    workFlowTaskStatus3.setFkWorkFlowType(workFlowType);
    workFlowTaskStatus3.setWorkflowTaskStatusId(3);


    WorkFlowTaskStatus workFlowTaskStatus4 = new WorkFlowTaskStatus();
    workFlowTaskStatus4.setWorkflowTaskStatus(STATUS_BLOCKED);
    workFlowTaskStatus4.setFkWorkFlowType(workFlowType);
    workFlowTaskStatus4.setWorkflowTaskStatusId(4);

    WorkFlowTaskStatus workFlowTaskStatus5 = new WorkFlowTaskStatus();
    workFlowTaskStatus5.setFkWorkFlowType(workFlowType);
    workFlowTaskStatus5.setWorkflowTaskStatusId(5);
    workFlowTaskStatus5.setWorkflowTaskStatus(STATUS_ON_HOLD);

    WorkFlowTaskStatus workFlowTaskStatus6 = new WorkFlowTaskStatus();
    workFlowTaskStatus6.setFkWorkFlowType(workFlowType);
    workFlowTaskStatus6.setWorkflowTaskStatusId(6);
    workFlowTaskStatus6.setWorkflowTaskStatus(STATUS_COMPLETED);

    workFlowTaskStatuses.add(workFlowTaskStatus);
    workFlowTaskStatuses.add(workFlowTaskStatus2);
    workFlowTaskStatuses.add(workFlowTaskStatus3);
    workFlowTaskStatuses.add(workFlowTaskStatus4);
    workFlowTaskStatuses.add(workFlowTaskStatus3);
    workFlowTaskStatuses.add(workFlowTaskStatus5);
    workFlowTaskStatuses.add(workFlowTaskStatus6);

    Task task = new Task();
    task.setFkWorkflowTaskStatus(workFlowTaskStatus);
    task.setTaskProgressSystem(NOTSTARTED);
    task.setTaskExpEndDate(LocalDateTime.of(LocalDate.now(), LocalTime.of(23, 59)));
    List<Task> tasks = new ArrayList<>();
    task.setFkTeamId(team);
    task.setFkAccountIdAssigned(userAccount);
    tasks.add(task);
    ArrayList<ActionId> actionIds = new ArrayList<>();
    actionIds.add(new ActionId(1));
    CustomAccessDomain customAccessDomain = mock(CustomAccessDomain.class);
    List<CustomAccessDomain> accessDomainsFoundDb = new ArrayList<>();
    accessDomainsFoundDb.add(customAccessDomain);
    lenient().doNothing().when(statsService).setDefaultFromAndToDateToStatsRequest(any(StatsRequest.class), anyString());
    lenient().when(statsRequest.getAccountIds()).thenReturn(null);
    lenient().when(statsRequest.getAccountIdAssigned()).thenReturn(1L);
    lenient().when(statsRequest.getNoOfDays()).thenReturn(1L);
    lenient().when(accessDomainService.getAccessDomainsByAccountIdsAndEntityTypeIdAndIsActive(anyInt(), anyList())).thenReturn(accessDomainsFoundDb);
    lenient().when(roleActionService.getActionIdByRoleId(anyInt())).thenReturn(actionIds);
    lenient().when(statsRequest.getBuId()).thenReturn(null);
    lenient().when(statsRequest.getProjectId()).thenReturn(null);
    lenient().when(statsRequest.getTeamId()).thenReturn(null);
    lenient().when(statsRequest.getTaskPriority()).thenReturn(taskPriorities);
    lenient().when(statsRequest.getTaskWorkflowId()).thenReturn(1L);
    lenient().when(statsRequest.getStatName()).thenReturn(types);
    lenient().when(userAccountService.getAllUserAccountByUserIdAndIsActive(anyLong())).thenReturn(userAccounts);
    lenient().when(accessDomainService.getAccessDomainsByAccountIdsAndEntityId(anyLong(), anyList())).thenReturn(accessDomainsFoundDb);
    lenient().when(statsRequest.getWorkflowTaskStatus()).thenReturn(List.of("completed"));
    lenient().when(workflowTaskStatusService.getAllWorkflowTaskStatus()).thenReturn(workFlowTaskStatuses);
    lenient().when(taskRepository.findAll(any(Specification.class))).thenReturn(tasks);
    lenient().when(statsService.removeBacklogDeleteTasks(anyList())).thenReturn(tasks);
    lenient().when(statsRequest.getCurrentDate()).thenReturn(LocalDateTime.now());
    lenient().doNothing().when(taskServiceImpl).convertTaskAllServerDateAndTimeInToUserTimeZone(any(Task.class), anyString());
    List<TaskMaster> taskMasters = taskService.getTaskDetailsForStatus(statsRequest, timeZone, anyString());
    assertThat(taskMasters).isNotNull();
//    assertThat(taskMasters.size()).isNotZero();

  }

}
