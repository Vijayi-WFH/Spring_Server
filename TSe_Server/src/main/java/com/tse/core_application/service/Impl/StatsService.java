package com.tse.core_application.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.database.utilities.Pair;
import com.tse.core_application.config.DebugConfig;
import com.tse.core_application.custom.model.EmailFirstLastAccountId;
import com.tse.core_application.custom.model.MeetingDetails;
import com.tse.core_application.custom.model.SprintResponseForFilter;
import com.tse.core_application.custom.model.TaskMaster;
import com.tse.core_application.dto.*;
import com.tse.core_application.exception.ExceptionByTask;
import com.tse.core_application.exception.ForbiddenException;
import com.tse.core_application.filters.JwtRequestFilter;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.*;
import com.tse.core_application.model.personal_task.PersonalTask;
import com.tse.core_application.repository.*;
import com.tse.core_application.utils.CommonUtils;
import com.tse.core_application.utils.DateTimeUtils;
import com.tse.core_application.utils.JWTUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class StatsService {

    private static final Logger logger = LogManager.getLogger(StatsService.class.getName());
    @Autowired
    TaskService taskService;

    @Autowired
    TaskHistoryRepository taskHistoryRepository;

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private TaskHistoryService taskHistoryService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private OfficeHoursService officeHoursService;

    @Autowired
    private WorkflowTaskStatusService workflowTaskStatusService;

    @Autowired
    private TaskHistoryMetadataService taskHistoryMetadataService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TaskServiceImpl taskServiceImpl;

    @Autowired
    private TimeSheetRepository timeSheetRepository;

    private LocalDateTime currentDate;

    private Time currentTime;

    private StatsRequest statsRequest;

    @Autowired
    private DependencyRepository dependencyRepository;

    ObjectMapper objectMapper = new ObjectMapper();

    private LocalDateTime systemDerivedEndDateTime;
    @Autowired
    private EntityPreferenceService entityPreferenceService;
    @Autowired
    private LeaveService leaveService;
    @Autowired
    private PersonalTaskService personalTaskService;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private AccessDomainRepository accessDomainRepository;
    @Autowired
    private JwtRequestFilter jwtRequestFilter;
    @Autowired
    private SprintRepository sprintRepository;
    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private PersonalTaskRepository personalTaskRepository;

    @Autowired
    private ProjectRepository projectRepository;


    //variable to memoize the task histories, to avoid repeated queries.
    private HashMap<Long, List<TaskHistory>> taskWiseHistories = new HashMap<Long, List<TaskHistory>>();

    //Inner class for data computation
    public static class DataHelpers {

        public static boolean isTaskCreatedToday(Task task) {
            boolean result = false;

            // some business logic was present

            return result;
        }

        public static boolean isTaskJustStarted(Task task) {
            //Comparing task's actual start time with current time.
            boolean isTaskJustStarted = false;
            // some business logic was present
            return isTaskJustStarted;
        }

        // change 07-04-2023: as discussed b/w sir and mohan -- added buffer for estimate > 16 hrs
        public static Long getBufferedEstimateForEstimate(Long estimate) {
            // some business logic was present
            return null;
        }

        //if 10% of taskEstimate or 30 mins, capped at 4 hrs, whichever is greater that is returned from this function.
        public static Long getBufferTimeForNonDelayedBasedOnComputedRemainingTimeMS(Task task) {
            // some business logic was present
            return null;
        }

        /* This is the old version/way to derive the estimate of the tasks which do not have any estimates. This method has been
         * replaced with the new method with name "deriveTimeEstimateForTask()"  in the stats_algorithm to derive the estimate of the task
         * based on the new logic.*/
        @Deprecated(since = "2022-08-26")
        public static Long deriveTimeEstimateForTask(Task task) {
            Long estimate = null;
            // some business logic was present
            return estimate;
        }
    }

    public Long deriveTimeEstimateForTask(Task task) {
        Long estimate = null;
        // some business logic was present
        return estimate;
    }

    /**
     *
     * @param timeDiff
     * @return offSet time based on the remaining time in the day (to accommodate breaks & extra time)
     */
    public Long calculateOffSetTimeBasedOnRemainingTime(Long timeDiff){
        Long offSetTime = 0L;
        // some business logic was present
        return offSetTime;
    }

    public Long deriveTotalWorkingTimeRemaining(Task task) {
        Long totalWorkingTime = null;
        // some business logic was present
        return totalWorkingTime;
    }

    /* This method is used to derive the expected end time of the task by using the priority of that
     * task, if the expected end time of the task is not given. */
    public LocalTime deriveEndTimeForTask(Task task) {

        Time endTime = new Time(Constants.TaskStatConstants.END_TIME_P0_BUFFER);
        // some business logic was present
        return endTime.toLocalTime();
    }

    /*function with rules for delayed for each priority level*/
    public boolean isStartedTaskDelayed(Task task, LocalTime endTime, Long estimate) {
        boolean isDelayed = false;
        // some business logic was present
        return isDelayed;
    }


    public boolean isTodoTaskDelayed(Task task) {
        boolean isDelayed = false;
        // some business logic was present
        return isDelayed;
    }


    // @deprecated: this method has incomplete and wrong logic to calculate the status of the task. this method is used by /getStats api
    @Deprecated(since = "2022-07-19")
    public List<Task> filterTasksForStats(StatType statName, List<Task> allTasks) {

        List<Task> filteredTask = new ArrayList<>();
        // some business logic was present
        return filteredTask;
    }

    private Stat computeStat(StatType stat, Integer displayOrder, List<Task> allTasks) {

        // some business logic was present
        return null;
    }

    /*function to decide weather last update on task is of task*/
    public boolean isLastUpdateDelayed(Long taskId, List<TaskHistory> taskHistories) {
        boolean isLastUpdateDelayed = false;
        // some business logic was present
        return isLastUpdateDelayed;
    }

    public HashMap<String, Object> updateNewEndTsBasedOnBufferedEstimate(Task task, Long bufferedEstimate) {

        HashMap<String, Object> response = new HashMap<>();
        // some business logic was present
        return response;
    }

    /**
     *
     * @param currentDate
     * @param daysToAdd
     * @param offDays
     * @return new Date after advancing currentDate by daysToAdd but skipping the non-working days
     */
    public LocalDate skipNonWorkingDays(LocalDate currentDate, Integer daysToAdd, List<String> offDays){
        int n = daysToAdd;
        LocalDate newDate = currentDate;
        // some business logic was present
        return newDate;
    }



    public HashMap<String, Object> getTaskStatusForTasksEndingOnCurrentDate(Task task) {
        HashMap<String, Object> response = new HashMap<>();
        // some business logic was present
        return response;
//        return st;
    }


    public LocalDateTime getLastRecordedEffortDateTime(Task task) {
        // some business logic was present
        return null;
    }



    public Long getTimeRemainingForToday(Task task) {

        // some business logic was present
        return null;
    }

    //(Flow XY of algo) This function derives total time remaining, compares it with task's estimate.
    public HashMap<String, Object> getTaskStatusBasedOnComputedRemainingTime(Task task) {
        HashMap<String, Object> response = new HashMap<>();
        // some business logic was present
        return response;
    }

    /**
     *
     * @return calculated new date time after adding timeToAddToCurrentDateTime to current date time and considering business days and office times
     */
    public LocalDateTime calculateAdjustedDateTime(Task task, Long timeToAddToCurrentDateTime){
        // some business logic was present
        return null;
    }


    //This function checks if task expected end date is current date or not (invoking flow X of algorithm).
    public HashMap<String , Object> getTaskStatBasedOnTaskEndDate(Task task) {
        HashMap<String, Object> result = null;
        // some business logic was present
        return result;
    }

    public boolean taskExistsForPriority(TaskPriority p, StatsRequest req) {
        // some business logic was present
        return false;
    }


    public StatType getTaskStatusBasedOnCreateTime(Task task) {
        StatType st = null;
        // some business logic was present
        return st;
    }

    @Deprecated(since = "14-06-2023")
    public StatType computeAndUpdateStatForBacklogTask(Task task) {
        StatType statType = null;
        // some business logic was present
        return statType;
    }

    public StatType computeAndUpdateStatForCompletedTask(Task task, StatType initialStat) {
        StatType statType = null;
        // some business logic was present
        return statType;
    }

    public HashMap<String, Object> computeAndUpdateStat(Task task, boolean isStatComputeOnUpdateTaskAction) {
        StatType st = null;
        HashMap<String, Object> response = new HashMap<>();
        // some business logic was present
        return response;
//        return st;
    }


    public List<Task> removeBacklogDeleteTasks(List<Task> tasks) {
        ArrayList<Task> tasksList = null;
        // some business logic was present
        return tasksList;
    }


    public HashMap<Long, StatType> computeAndUpdateStatsViaUpdateTask(Task task, boolean isComputeStatOnUpdateTaskAction) {
        HashMap<Long, StatType> taskWiseStats = new HashMap<>();
        // some business logic was present
        return taskWiseStats;
    }


    public void computeAndUpdateStatForAddTask(Task task) {

    }

//    public HashMap<Long, StatType> computeAndUpdateStatsViaDashboard(List<Task> tasks, boolean isStatComputeOnUpdateTaskAction, List<Task> childTasksToRemove, Map<Long, Task> tasksWithDependenciesMap, List<Long> accounIdList) {
    public HashMap<Long, StatType> computeAndUpdateStatsViaDashboard(List<Task> tasks, boolean isStatComputeOnUpdateTaskAction, Map<Long, Task> tasksWithDependenciesMap, List<Long> accounIdList) {
        this.statsRequest = statsRequest;
        HashMap<Long, StatType> taskWiseStats = new HashMap<>();
        // some business logic was present
        return taskWiseStats;
    }


    @Deprecated(since = "2022-07-28")
    public StatsResponse computeStats(StatsRequest statsRequest, String accountIds) {
        StatsResponse resp = new StatsResponse();
        // some business logic was present
        return resp;
    }

    public void setDefaultFromAndToDateToStatsRequest(StatsRequest statsRequest, String timeZone) {
        // some business logic was present
    }

    /* todayStats will come for tasks having (taskExpEndDate >= currentDate 00:00:00 and taskExpEndDate <= currentDate 23:59:00) */
    public StatsResponse computeStatsForGetStatsV3(StatsRequest statsRequest, String timeZone, String accountIds) {
        StatsResponse resp = new StatsResponse();
        // some business logic was present
        return resp;
    }

    /** this method process the flagged tasks and creates a response if they have any successors*/
    private FlaggedTaskInfo processPotentialFlaggedTask(Task currentTask) {
        return null;
    }

    public Stat computeStatForGetStatsV3(StatType stat, Integer displayOrder, int totalNoOfTasks, Integer noOfTasksForStat) {
        return null;

    }

    private static class StatPercentHolder {
        StatType statType;
        int displayOrder;
        int count;
        double rawPercent;
        int intPercentage;

        StatPercentHolder(StatType statType, int displayOrder, int count, double rawPercent) {
            this.statType = statType;
            this.displayOrder = displayOrder;
            this.count = count;
            this.rawPercent = rawPercent;
            this.intPercentage = (int) Math.floor(rawPercent);
        }

        double fractional() {
            return rawPercent - Math.floor(rawPercent);
        }
    }

    private void adjustPercentagesToSum100ForStats(List<StatPercentHolder> holders) {

    }

    public Boolean validateMyTaskAccountIds(StatsRequest statsRequest, String token) {
        boolean isAccountIdsValidated = true;
        return isAccountIdsValidated;
    }

    public UserProgressResponse getUserProgress (String accountIds, UserProgressRequest userProgressRequest) {
        return null;
    }

    public TodayFocusResponse getTodayFocus (String accountIds, TodayFocusRequest todayFocusRequest) {
        TodayFocusResponse todayFocusResponse = new TodayFocusResponse();
        return todayFocusResponse;
    }
}
