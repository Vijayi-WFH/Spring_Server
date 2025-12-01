package com.tse.core.service;

import com.tse.core.custom.model.*;
import com.tse.core.dto.TimeSheetResponse;
import com.tse.core.handlers.StackTraceHandler;
import com.tse.core.model.Constants;
import com.tse.core.model.TimeSheet;
import com.tse.core.model.leave.LeaveApplication;
import com.tse.core.model.supplements.EntityPreference;
import com.tse.core.model.supplements.Task;
import com.tse.core.model.supplements.User;
import com.tse.core.model.supplements.UserAccount;
import com.tse.core.repository.TimeSheetRepository;
import com.tse.core.repository.leaves.LeaveApplicationRepository;
import com.tse.core.repository.leaves.LeavePolicyRepository;
import com.tse.core.repository.leaves.LeaveRemainingRepository;
import com.tse.core.repository.supplements.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.xml.bind.ValidationException;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TimeSheetService {

    @Autowired
    private TimeSheetRepository timeSheetRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private AccessDomainRepository accessDomainRepository;

    @Autowired
    private OfficeHoursRepository officeHoursRepository;
    @Autowired
    private CalendarDaysRepository calendarDaysRepository;
    @Autowired
    private LeavePolicyRepository leavePolicyRepository;
    @Autowired
    private LeaveRemainingRepository leaveRemainingRepository;
    @Autowired
    private BURepository buRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private EntityPreferenceRepository entityPreferenceRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EntityPreferenceService entityPreferenceService;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private LeaveApplicationRepository leaveApplicationRepository;

    private static final Logger logger = LogManager.getLogger(TimeSheetService.class.getName());
    private static final DecimalFormat decfor = new DecimalFormat("0.00");

    /**
     * get user's local dates from user's timezone -- required when dates are not passed explicitly by the user
     */
    public LocalDate[] getDatesForUserTimeZone(String userTimeZone, String period) {

        LocalDate fromDate, toDate;
        ZoneId userZone = ZoneId.of(userTimeZone);
        LocalDate today = LocalDate.now(userZone);

        switch (period) {
            case "thisMonth":
                fromDate = today.withDayOfMonth(1);
                toDate = today.with(TemporalAdjusters.lastDayOfMonth());
                break;

            case "lastMonth":
                LocalDate lastDateOfPrevMonth = today.withDayOfMonth(1).minusDays(1);
                fromDate = lastDateOfPrevMonth.withDayOfMonth(1);
                toDate = lastDateOfPrevMonth.with(TemporalAdjusters.lastDayOfMonth());
                break;

            case "thisWeek":
                Integer dayOfWeek = today.getDayOfWeek().getValue();
                // first day of week
                fromDate = today.minusDays((dayOfWeek - 1) % 7);
                // last day of week
                toDate = today.plusDays(7 - dayOfWeek);
                break;

            case "today":
                fromDate = today;
                toDate = today;
                break;

            default:
                String allStackTraces = StackTraceHandler.getAllStackTraces(new IllegalArgumentException("Incorrect TimePeriod for TimeSheet"));
                logger.error("Incorrect TimePeriod for TimeSheet", new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw new IllegalArgumentException("Incorrect TimePeriod for TimeSheet");
        }

        return new LocalDate[]{fromDate, toDate};
    }


    /**
     * check what fields are not null in the timesheet request
     */
    public List<String> getNotNullFields(TimeSheetRequest tsRequest) throws IllegalAccessException {

        List<String> notNullFields = new ArrayList<>();

        Field[] fields = tsRequest.getClass().getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);

            if (field.get(tsRequest) != null) {
                notNullFields.add(field.getName());
            }
        }

        return notNullFields;

    }


    /**
     * get the time sheets for user's request
     */

    public List<TimeSheetResponse> getTimeSheet(TimeSheetRequest tsRequest, String userID, String timeZone)
            throws IllegalAccessException, ValidationException {

        Long userIdLong = Long.parseLong(userID);
        List<String> notNullFields = getNotNullFields(tsRequest);

        // Validation
        LocalDate[] dateRange = resolveDateRange(tsRequest, timeZone);
        LocalDate fromDate = dateRange[0];
        LocalDate toDate = dateRange[1];

        List<TimeSheet> recordsFromDb;
        List<AccountId> accountIds = new ArrayList<>();

        if (isMyTimeSheet(tsRequest, notNullFields)) {
            recordsFromDb = getMyTimeSheetRecords(userIdLong, fromDate, toDate, accountIds);
        } else if (isTeamTimeSheet(tsRequest, notNullFields)) {
            recordsFromDb = getTeamTimeSheetRecords(tsRequest, fromDate, toDate, accountIds);
        } else if (isEntityTimeSheet(tsRequest, notNullFields)) {
            recordsFromDb = getEntityTimeSheetRecords(tsRequest, fromDate, toDate);
        } else if (isProjectTimeSheet(tsRequest, notNullFields)) {
            recordsFromDb = getProjectOrBuOrOrgTimeSheet(Constants.EntityTypes.PROJECT, tsRequest.getProjectId(), teamRepository::findTeamIdsByProjectId,
                    tsRequest, fromDate, toDate, accountIds);
        } else if (isBuTimeSheet(tsRequest, notNullFields)) {
            recordsFromDb = getProjectOrBuOrOrgTimeSheet(Constants.EntityTypes.BU, tsRequest.getBuId(), teamRepository::findTeamIdsByBuId,
                    tsRequest, fromDate, toDate, accountIds);
        } else if (isOrgTimeSheet(tsRequest, notNullFields)) {
            recordsFromDb = getProjectOrBuOrOrgTimeSheet(Constants.EntityTypes.ORG, tsRequest.getOrgId(), teamRepository::findTeamIdsByOrgId,
                    tsRequest, fromDate, toDate, accountIds);
        } else {
            logAndThrow("Invalid TimeSheet request parameters");
            return Collections.emptyList();
        }

        return getFormattedTimeSheetResponse(recordsFromDb, new LocalDate[]{fromDate, toDate}, accountIds, tsRequest);
    }

    private LocalDate[] resolveDateRange(TimeSheetRequest tsRequest, String timeZone) throws ValidationException {
        if (tsRequest.getTimePeriod() != null) {
            if (tsRequest.getFromDate() != null || tsRequest.getToDate() != null) {
                logAndThrow("date and time period can't be provided together in the timesheet request");
            }
            return getDatesForUserTimeZone(timeZone, tsRequest.getTimePeriod());
        } else if (tsRequest.getFromDate() != null && tsRequest.getToDate() != null) {
            if (tsRequest.getFromDate().isAfter(tsRequest.getToDate())) {
                logAndThrow("fromDate cannot be greater than toDate");
            }
            return new LocalDate[]{tsRequest.getFromDate(), tsRequest.getToDate()};
        } else if (tsRequest.getFromDate() != null || tsRequest.getToDate() != null) {
            logAndThrow("Both fromDate and toDate should be selected");
        }
        return getDatesForUserTimeZone(timeZone, "thisWeek");
    }

    private boolean isMyTimeSheet(TimeSheetRequest ts, List<String> fields) {
        return fields.isEmpty() ||
                (ts.getFromDate() != null && ts.getToDate() != null && fields.size() == 2) ||
                (ts.getTimePeriod() != null && fields.size() == 1);
    }

    private boolean isTeamTimeSheet(TimeSheetRequest ts, List<String> fields) {
        return ts.getTeamId() != null && isValidCombination(ts, fields.size());
    }

    private boolean isEntityTimeSheet(TimeSheetRequest ts, List<String> fields) {
        return ts.getEntityId() != null && isValidCombination(ts, fields.size());
    }

    private boolean isProjectTimeSheet(TimeSheetRequest ts, List<String> fields) {
        return ts.getProjectId() != null && isValidCombination(ts, fields.size());
    }

    private boolean isBuTimeSheet(TimeSheetRequest ts, List<String> fields) {
        return ts.getBuId() != null && isValidCombination(ts, fields.size());
    }

    private boolean isOrgTimeSheet(TimeSheetRequest ts, List<String> fields) {
        return ts.getOrgId() != null && isValidCombination(ts, fields.size());
    }

    private boolean isValidCombination(TimeSheetRequest ts, int size) {
        return size == 1 ||
                (ts.getAccountIdList() != null && size == 2) ||
                (ts.getFromDate() != null && ts.getToDate() != null && (size == 3 || (ts.getAccountIdList() != null && size == 4))) ||
                (ts.getTimePeriod() != null && (size == 2 || (ts.getAccountIdList() != null && size == 3)));
    }

    private List<TimeSheet> getMyTimeSheetRecords(Long userId, LocalDate from, LocalDate to, List<AccountId> accountIds) {
        User user = userRepository.findByUserId(userId);
        List<Long> userIds = new ArrayList<>();
        userIds.add(userId);

        if (user != null && Boolean.TRUE.equals(user.getIsUserManaging())) {
            userIds.addAll(userRepository.findAllUserIdByManagingUserId(user.getUserId()));
        }

        accountIds.addAll(userAccountRepository.findDistinctAccountIdByFkUserIdUserIdIn(userIds));
        return timeSheetRepository.findByUserIdInAndNewEffortDateBetween(userIds, from, to);
    }

    private List<TimeSheet> getTeamTimeSheetRecords(TimeSheetRequest ts, LocalDate from, LocalDate to, List<AccountId> accountIds) {
        if (ts.getAccountIdList() != null && !ts.getAccountIdList().isEmpty()) {
            accountIds.addAll(ts.getAccountIdList().stream().map(AccountId::new).collect(Collectors.toList()));
        } else {
            accountIds.addAll(accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityId(Constants.TEAM_TYPE_ID, ts.getTeamId()));
        }

        List<Long> accountIdValues = accountIds.stream().map(AccountId::getAccountId).collect(Collectors.toList());
        List<TimeSheet> records = accountIdValues.isEmpty()
                ? timeSheetRepository.findByTeamIdAndNewEffortDateBetween(ts.getTeamId(), from, to)
                : timeSheetRepository.findByTeamIdInAndNewEffortDateBetweenAndAccountIdIn(List.of(ts.getTeamId()), from, to, accountIdValues);

        Long projectId = teamRepository.findFkProjectIdProjectIdByTeamId (ts.getTeamId());
        records.addAll(accountIdValues.isEmpty()
                ? timeSheetRepository.findTimeSheetAtProjectLevelByProjectIdInAndNewEffortDateBetween(List.of(projectId), from, to)
                : timeSheetRepository.findTimeSheetAtProjectLevelByTeamIdInAndNewEffortDateBetweenAndAccountIdIn(List.of(projectId), from, to, accountIdValues));

        Long buId = teamRepository.findFkProjectIdBuIdByTeamId(ts.getTeamId());
        records.addAll(accountIdValues.isEmpty()
                ? timeSheetRepository.findTimeSheetAtBuLevelByBuIdInAndNewEffortDateBetween(List.of(buId), from, to)
                : timeSheetRepository.findTimeSheetAtBuLevelByBuIdInAndNewEffortDateBetweenAndAccountIdIn(List.of(buId), from, to, accountIdValues));

        Long orgId = teamRepository.findFkOrgIdOrgIdByTeamId(ts.getTeamId());
        records.addAll(accountIdValues.isEmpty()
                ? timeSheetRepository.findTimeSheetAtOrgLevelByOrgIdInAndNewEffortDateBetween(List.of(orgId), from, to)
                : timeSheetRepository.findTimeSheetAtOrgLevelByOrgIdInAndNewEffortDateBetweenAndAccountIdIn(List.of(orgId), from, to, accountIdValues));

        return records;
    }

    private List<TimeSheet> getProjectOrBuOrOrgTimeSheet(Integer entityTypeId, Long id, Function<Long, List<Long>> teamResolver,
                                                         TimeSheetRequest ts, LocalDate from, LocalDate to,
                                                         List<AccountId> accountIds) {
        List<Long> teamIds = teamResolver.apply(id);

        if (ts.getAccountIdList() != null && !ts.getAccountIdList().isEmpty()) {
            accountIds.addAll(ts.getAccountIdList().stream().map(AccountId::new).collect(Collectors.toList()));
        } else {
            accountIds.addAll(accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdInAndIsActive(Constants.TEAM_TYPE_ID, teamIds, true));
        }

        List<Long> accountIdValues = accountIds.stream().map(AccountId::getAccountId).collect(Collectors.toList());
        List<TimeSheet> records = accountIdValues.isEmpty()
                ? timeSheetRepository.findByTeamIdInAndNewEffortDateBetween(teamIds, from, to)
                : timeSheetRepository.findByTeamIdInAndNewEffortDateBetweenAndAccountIdIn(teamIds, from, to, accountIdValues);

        if (Objects.equals(Constants.EntityTypes.ORG, entityTypeId)) {
            List<Long> projectIdList = projectRepository.findByOrgId(id).stream().map(ProjectIdProjectName::getProjectId).collect(Collectors.toList());
            if (projectIdList != null && !projectIdList.isEmpty()) {
                records.addAll(accountIdValues.isEmpty()
                        ? timeSheetRepository.findTimeSheetAtProjectLevelByProjectIdInAndNewEffortDateBetween(projectIdList, from, to)
                        : timeSheetRepository.findTimeSheetAtProjectLevelByTeamIdInAndNewEffortDateBetweenAndAccountIdIn(projectIdList, from, to, accountIdValues));
            }
            List<Long> buIdList = buRepository.findBuIdsByOrgId(id);
            if (buIdList != null && !buIdList.isEmpty()) {
                records.addAll(accountIdValues.isEmpty()
                        ? timeSheetRepository.findTimeSheetAtBuLevelByBuIdInAndNewEffortDateBetween(buIdList, from, to)
                        : timeSheetRepository.findTimeSheetAtBuLevelByBuIdInAndNewEffortDateBetweenAndAccountIdIn(buIdList, from, to, accountIdValues));
            }
            records.addAll(accountIdValues.isEmpty()
                    ? timeSheetRepository.findTimeSheetAtOrgLevelByOrgIdInAndNewEffortDateBetween(List.of(id), from, to)
                    : timeSheetRepository.findTimeSheetAtOrgLevelByOrgIdInAndNewEffortDateBetweenAndAccountIdIn(List.of(id), from, to, accountIdValues));
        }
        else if (Objects.equals(Constants.EntityTypes.BU, entityTypeId)) {
            List<Long> projectIdList = projectRepository.findProjectIdsByBuId(id);
            if (projectIdList != null && !projectIdList.isEmpty()) {
                records.addAll(accountIdValues.isEmpty()
                        ? timeSheetRepository.findTimeSheetAtProjectLevelByProjectIdInAndNewEffortDateBetween(projectIdList, from, to)
                        : timeSheetRepository.findTimeSheetAtProjectLevelByTeamIdInAndNewEffortDateBetweenAndAccountIdIn(projectIdList, from, to, accountIdValues));
            }
            records.addAll(accountIdValues.isEmpty()
                    ? timeSheetRepository.findTimeSheetAtBuLevelByBuIdInAndNewEffortDateBetween(List.of(id), from, to)
                    : timeSheetRepository.findTimeSheetAtBuLevelByBuIdInAndNewEffortDateBetweenAndAccountIdIn(List.of(id), from, to, accountIdValues));

            Long orgId = buRepository.findOrgIdByBuId(id);
            records.addAll(accountIdValues.isEmpty()
                    ? timeSheetRepository.findTimeSheetAtOrgLevelByOrgIdInAndNewEffortDateBetween(List.of(orgId), from, to)
                    : timeSheetRepository.findTimeSheetAtOrgLevelByOrgIdInAndNewEffortDateBetweenAndAccountIdIn(List.of(orgId), from, to, accountIdValues));
        }
        else if (Objects.equals(Constants.EntityTypes.PROJECT, entityTypeId)) {
            records.addAll(accountIdValues.isEmpty()
                    ? timeSheetRepository.findTimeSheetAtProjectLevelByProjectIdInAndNewEffortDateBetween(List.of(id), from, to)
                    : timeSheetRepository.findTimeSheetAtProjectLevelByTeamIdInAndNewEffortDateBetweenAndAccountIdIn(List.of(id), from, to, accountIdValues));

            Long buId = projectRepository.findBuIdByProjectId(id);
            records.addAll(accountIdValues.isEmpty()
                    ? timeSheetRepository.findTimeSheetAtBuLevelByBuIdInAndNewEffortDateBetween(List.of(buId), from, to)
                    : timeSheetRepository.findTimeSheetAtBuLevelByBuIdInAndNewEffortDateBetweenAndAccountIdIn(List.of(buId), from, to, accountIdValues));

            Long orgId = projectRepository.findOrgIdByProjectId(id);
            records.addAll(accountIdValues.isEmpty()
                    ? timeSheetRepository.findTimeSheetAtOrgLevelByOrgIdInAndNewEffortDateBetween(List.of(orgId), from, to)
                    : timeSheetRepository.findTimeSheetAtOrgLevelByOrgIdInAndNewEffortDateBetweenAndAccountIdIn(List.of(orgId), from, to, accountIdValues));
        }
        return records;
    }

    private List<TimeSheet> getEntityTimeSheetRecords(TimeSheetRequest tsRequest, LocalDate fromDate, LocalDate toDate) {
        List<Long> accountIds = new ArrayList<>();

        if (tsRequest.getAccountIdList() != null && !tsRequest.getAccountIdList().isEmpty()) {
            accountIds = new ArrayList<>(tsRequest.getAccountIdList());

            return timeSheetRepository.findByTeamIdInAndNewEffortDateBetweenAndAccountIdIn(
                    List.of(tsRequest.getTeamId()), fromDate, toDate, accountIds);
        } else {
            return timeSheetRepository.findByEntityIdAndNewEffortDateBetween(
                    tsRequest.getEntityId(), fromDate, toDate);
        }
    }

    private void logAndThrow(String message) throws ValidationException {
        String stackTraces = StackTraceHandler.getAllStackTraces(new ValidationException(message));
        logger.error(message, new Throwable(stackTraces));
        ThreadContext.clearMap();
        throw new ValidationException(message);
    }

//    private List<TimeSheet> findLeaveRecords(List<Long> allAccountIdsList, LocalDate fromDate, LocalDate toDate) {
//        return timeSheetRepository.findByEntityTypeIdAndAccountIdListAndNewEffortDateBetween(List.of(Constants.EntityTypes.LEAVE, Constants.EntityTypes.HOLIDAY), allAccountIdsList,fromDate,toDate);
//    }

    public List<TimeSheetResponse> getFormattedTimeSheetResponse(List<TimeSheet> recordsFromDb, LocalDate[] timeInterval, List<AccountId> allAccountIdsList, TimeSheetRequest tsRequest) {

        // create a map between account id and corresponding records from recordsFromDb
        Map<Long, List<TimeSheet>> mapAccountIdToRecord = new HashMap<>();
        Map<Long, EntityPreference> entityPreferenceWithOrgMapping = new HashMap<>();
        for (TimeSheet tsRecord : recordsFromDb) {
            Long accountId = tsRecord.getAccountId();
            if (mapAccountIdToRecord.containsKey(accountId)) {
                mapAccountIdToRecord.get(accountId).add(tsRecord);
            } else {
                List<TimeSheet> timeSheetListForAccount = new ArrayList<>();
                timeSheetListForAccount.add(tsRecord);
                mapAccountIdToRecord.put(accountId, timeSheetListForAccount);
            }
        }

        // create a set of all unique account ids found in recordsFromDb
        HashSet<Long> uniqueAccountIdsFromRecords =  new HashSet();

        // create a list of object AccountId which stores all Account Ids with no added effort
        List<AccountId> zeroEffortAccIds = new ArrayList<>();

        for(TimeSheet tsRecord : recordsFromDb)
        {
            uniqueAccountIdsFromRecords.add(tsRecord.getAccountId());
        }

        // loop to add account ids from allAccountIdsList not present in hash set to zeroEffortAccIds.
        for(AccountId accId : allAccountIdsList)
        {
            if(!uniqueAccountIdsFromRecords.contains(accId.getAccountId()))
            {
                AccountId newAccId = new AccountId(accId.getAccountId());
                zeroEffortAccIds.add(newAccId);
            }
        }

        // create a map of account id to map of {effortDate, TotalEffort}
        Map<Long, Map<LocalDate, TotalEffort>> mapAccountIdToDateAndTotalEffort = new HashMap<>();

        // For all accountIds in zeroEffortAccIds list set , 0 effort in mapAccountToEfforts map.

        if(!zeroEffortAccIds.isEmpty())
        {
            for(AccountId accId : zeroEffortAccIds)
            {
                Map<LocalDate, TotalEffort> mapDateToTotalEffort = new TreeMap<>();

                LocalDate currentDate = timeInterval[0], toDate = timeInterval[1];
                while(currentDate.isBefore(toDate.plusDays(1))){
                    if(!mapDateToTotalEffort.containsKey(currentDate)){
                        mapDateToTotalEffort.put(currentDate, new TotalEffort(0, 0, currentDate, Collections.emptyList()));
                    }
                    currentDate = currentDate.plusDays(1);
                }

                mapAccountIdToDateAndTotalEffort.put(accId.getAccountId(), mapDateToTotalEffort);

            }
        }


        // ------------------------------------------------------------------------------------------------- //

        AtomicReference<Boolean> isLeaveValid = new AtomicReference<>(true);
        mapAccountIdToRecord.forEach((accountId, timeSheets) -> {

            Map<LocalDate, TotalEffort> mapDateToTotalEffort = new TreeMap<>();

            // for given account id loop through all timesheet records and create a map of date to TotalEffort
            for (TimeSheet timeSheet : timeSheets) {
                LocalDate newEffortDate = timeSheet.getNewEffortDate();
                Integer newEffort = timeSheet.getNewEffort();
                Integer earnedTime = timeSheet.getEarnedTime();
                if(earnedTime==null) {
                    earnedTime = 0;
                }

                if (mapDateToTotalEffort.containsKey(newEffortDate)) {
                    TotalEffort totalEffort = mapDateToTotalEffort.get(newEffortDate);

                    int currentEffortTotal = totalEffort.getTotalEffortMins() == null ? 0 : mapDateToTotalEffort.get(newEffortDate).getTotalEffortMins();
                    int currentEarnedTime = totalEffort.getTotalEarnedTime() == null ? 0 : mapDateToTotalEffort.get(newEffortDate).getTotalEarnedTime();
                    totalEffort.setTotalEffortMins(currentEffortTotal + newEffort);
                    totalEffort.setTotalEarnedTime(currentEarnedTime + earnedTime);
                    if(totalEffort.getEffortOnEntityList()!=null && !totalEffort.getEffortOnEntityList().isEmpty()){
                        boolean isEntityAvailable = false;
                        for(EffortOnEntity effortOnEntity: totalEffort.getEffortOnEntityList()){
                            if(Objects.equals(effortOnEntity.getEntityNumber(), timeSheet.getEntityNumber())){
                                effortOnEntity.setEntityEffortMins(effortOnEntity.getEntityEffortMins() + timeSheet.getNewEffort());
                                if(timeSheet.getEarnedTime()!=null){
                                    effortOnEntity.setEntityEarnedTime(effortOnEntity.getEntityEarnedTime() + timeSheet.getEarnedTime());
                                }
                                isEntityAvailable = true;
                                break;
                            }
                        }
                        if(!isEntityAvailable){
                            EffortOnEntity newEffortOnEntity = new EffortOnEntity();
                            newEffortOnEntity.setEntityTypeId(timeSheet.getEntityTypeId());
                            newEffortOnEntity.setEntityId(timeSheet.getEntityId());
                            newEffortOnEntity.setEntityNumber(timeSheet.getEntityNumber());
                            newEffortOnEntity.setTeamId(timeSheet.getTeamId());
                            newEffortOnEntity.setEntityTitle(timeSheet.getEntityTitle());
                            newEffortOnEntity.setEntityEffortMins(timeSheet.getNewEffort());
                            newEffortOnEntity.setProjectId(timeSheet.getProjectId());
                            if(timeSheet.getEarnedTime()!=null){
                                newEffortOnEntity.setEntityEarnedTime(timeSheet.getEarnedTime());
                            }
                            if (Objects.equals(Constants.EntityTypes.TASK, timeSheet.getEntityTypeId())) {
                                Task task = taskRepository.findByTaskId(timeSheet.getEntityId());
                                if (task != null) {
                                    newEffortOnEntity.setTaskTypeId(task.getTaskTypeId());
                                    if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.CHILD_TASK)) {
                                        Task parentTask = taskRepository.findByTaskId(task.getParentTaskId());
                                        newEffortOnEntity.setIsBug(parentTask.getIsBug());
                                    }
                                    else {
                                        newEffortOnEntity.setIsBug(task.getIsBug());
                                    }
                                }
                            }
                            else if (Objects.equals(Constants.EntityTypes.LEAVE, timeSheet.getEntityTypeId())) {
                                isLeaveValid.set(true);
                                setLeaveDetailsInTimeSheet (timeSheet, newEffortOnEntity, entityPreferenceWithOrgMapping, isLeaveValid);
                                if (!isLeaveValid.get()) {
                                    continue;
                                }
                            }
                            newEffortOnEntity.setOrgId(timeSheet.getOrgId());
                            totalEffort.getEffortOnEntityList().add(newEffortOnEntity);
                        }
                    }
                } else {
                    EffortOnEntity effortOnEntity = new EffortOnEntity();
                    effortOnEntity.setEntityTypeId(timeSheet.getEntityTypeId());
                    effortOnEntity.setEntityId(timeSheet.getEntityId());
                    effortOnEntity.setEntityNumber(timeSheet.getEntityNumber());
                    effortOnEntity.setTeamId(timeSheet.getTeamId());
                    effortOnEntity.setEntityTitle(timeSheet.getEntityTitle());
                    effortOnEntity.setEntityEffortMins(newEffort);
                    effortOnEntity.setEntityEarnedTime(earnedTime);
                    effortOnEntity.setProjectId(timeSheet.getProjectId());
                    if (Objects.equals(Constants.EntityTypes.TASK, timeSheet.getEntityTypeId())) {
                        Task task = taskRepository.findByTaskId(timeSheet.getEntityId());
                        if (task != null) {
                            effortOnEntity.setTaskTypeId(task.getTaskTypeId());
                            if (Objects.equals(task.getTaskTypeId(), Constants.TaskTypes.CHILD_TASK)) {
                                Task parentTask = taskRepository.findByTaskId(task.getParentTaskId());
                                effortOnEntity.setIsBug(parentTask.getIsBug());
                            }
                            else {
                                effortOnEntity.setIsBug(task.getIsBug());
                            }
                        }
                    }
                    else if (Objects.equals(Constants.EntityTypes.LEAVE, timeSheet.getEntityTypeId())) {
                        isLeaveValid.set(true);
                        setLeaveDetailsInTimeSheet (timeSheet, effortOnEntity, entityPreferenceWithOrgMapping, isLeaveValid);
                        if (!isLeaveValid.get()) {
                            continue;
                        }
                    }
                    effortOnEntity.setOrgId(timeSheet.getOrgId());
                    List<EffortOnEntity> effortOnEntities = new ArrayList<>();
                    effortOnEntities.add(effortOnEntity);
                    mapDateToTotalEffort.put(newEffortDate, new TotalEffort(newEffort, earnedTime, newEffortDate, effortOnEntities));
                }
            }

            // insert into map the dates for which no effort is recorded
            LocalDate currentDate = timeInterval[0], toDate = timeInterval[1];
            while(currentDate.isBefore(toDate.plusDays(1))){
                if(!mapDateToTotalEffort.containsKey(currentDate)){
                    mapDateToTotalEffort.put(currentDate, new TotalEffort(0, 0, currentDate, Collections.emptyList()));
                }
                currentDate = currentDate.plusDays(1);
            }

            mapAccountIdToDateAndTotalEffort.put(accountId, mapDateToTotalEffort);
        });


        // create a list of TimeSheetResponse to return -- convert mapAccountIdToTotalEffort to list of total efforts
        List<TimeSheetResponse> timeSheetResponses = new ArrayList<>();

        mapAccountIdToDateAndTotalEffort.forEach((accountId, dateToEffortsMap) -> {

            TimeSheetResponse timeSheetResponse = new TimeSheetResponse();
            Long orgId = userAccountRepository.findOrgIdByAccountId(accountId);
            timeSheetResponse.setMaxDailyWorkingHrs(Math.round((float) getMaxWorkingMinutes(tsRequest.getTeamId(), orgId) / 60));
            timeSheetResponse.setDailyExpectedWorkingMinutes(getMaxWorkingMinutes(tsRequest.getTeamId(), orgId));
            timeSheetResponse.setAccount_Id(accountId);
            timeSheetResponse.setEstimatedEntityTimeAchieved(null);

            List<TotalEffort> totalEffortList = new ArrayList<>();

            dateToEffortsMap.forEach((totalEffortDate, totalEffort) -> {
                totalEffortList.add(totalEffort);
            });

            timeSheetResponse.setEfforts(totalEffortList);

            timeSheetResponse.setOffDays(entityPreferenceService.getOffDaysByOrgId(orgId));

            timeSheetResponses.add(timeSheetResponse);
        });

        return timeSheetResponses;
    }

    /**
     * If the logged time is of Leave then set the details of that leave in timesheet
     * @param timeSheet
     * @param newEffortOnEntity
     * @param entityPreferenceWithOrgMapping
     */
    public void setLeaveDetailsInTimeSheet (TimeSheet timeSheet, EffortOnEntity newEffortOnEntity, Map<Long, EntityPreference> entityPreferenceWithOrgMapping, AtomicReference<Boolean> isLeaveValid) {
        LeaveApplication leaveApplication = leaveApplicationRepository.findByLeaveApplicationId(timeSheet.getEntityId());
        List<Short> consumedAndApprovedLeaveStatusId = List.of(Constants.Leave.APPROVED_LEAVE_APPLICATION_STATUS_ID, Constants.Leave.CONSUMED_LEAVE_APPLICATION_STATUS_ID);
        if (leaveApplication != null && consumedAndApprovedLeaveStatusId.contains(leaveApplication.getLeaveApplicationStatusId())) {
            if (leaveApplication.getIsLeaveForHalfDay() == null || !leaveApplication.getIsLeaveForHalfDay()) {
                newEffortOnEntity.setIsHalfDayLeave(false);
            }
            else {
                newEffortOnEntity.setIsHalfDayLeave(true);
                newEffortOnEntity.setHalfDayLeaveType(leaveApplication.getHalfDayLeaveType());
            }
            if (entityPreferenceWithOrgMapping.containsKey(timeSheet.getOrgId())) {
                if (Objects.equals(Constants.Leave.SICK_LEAVE_TYPE_ID, leaveApplication.getLeaveTypeId())) {
                    newEffortOnEntity.setLeaveAlias(entityPreferenceWithOrgMapping.get(timeSheet.getOrgId()).getSickLeaveAlias());
                }
                else {
                    newEffortOnEntity.setLeaveAlias(entityPreferenceWithOrgMapping.get(timeSheet.getOrgId()).getTimeOffAlias());
                }
            }
            else {
                Optional<EntityPreference> entityPreferenceOptional = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, timeSheet.getOrgId());
                if (entityPreferenceOptional != null && entityPreferenceOptional.isPresent()) {
                    entityPreferenceWithOrgMapping.put(timeSheet.getOrgId(), entityPreferenceOptional.get());
                    if (Objects.equals(Constants.Leave.SICK_LEAVE_TYPE_ID, leaveApplication.getLeaveTypeId())) {
                        newEffortOnEntity.setLeaveAlias(entityPreferenceWithOrgMapping.get(timeSheet.getOrgId()).getSickLeaveAlias());
                    } else {
                        newEffortOnEntity.setLeaveAlias(entityPreferenceWithOrgMapping.get(timeSheet.getOrgId()).getTimeOffAlias());
                    }
                }
            }
        }
        else {
            isLeaveValid.set(false);
        }
    }

    /**
     * @param leaveApplication
     * @param isLeaveApplicationNew it notifies if this entry of leaveApplication is new enter or entry after cancellation
     * @param totalLeaveHours
     * @throws ParseException
     */
    public void addLeaveToTimesheet(LeaveApplication leaveApplication, Boolean isLeaveApplicationNew, Float totalLeaveHours, Float officeHours) throws ParseException {
        boolean checkNonBusinessDays= leavePolicyRepository.findByLeavePolicyId(leaveRemainingRepository.findByAccountIdAndLeaveType(leaveApplication.getAccountId(), leaveApplication.getLeaveTypeId()).getLeavePolicyId()).getIncludeNonBusinessDaysInLeave();
        Long orgId = userAccountRepository.findOrgIdByAccountId(leaveApplication.getAccountId());
        if(Objects.equals(leaveApplication.getFromDate(),leaveApplication.getToDate())){
            if (!checkNonBusinessDays) {
                if (!validateHolidayDate(leaveApplication.getAccountId(), leaveApplication.getToDate()) && !entityPreferenceService.validateWeekendDate(orgId, leaveApplication.getToDate())) {
                    createAndFillTimesheetForLeave(leaveApplication, totalLeaveHours,leaveApplication.getFromDate(),isLeaveApplicationNew,officeHours);
                }
            }
            else {
                createAndFillTimesheetForLeave(leaveApplication, totalLeaveHours,leaveApplication.getFromDate(),isLeaveApplicationNew,officeHours);
            }
        }
        else{
            LocalDate date = leaveApplication.getFromDate();
            if(!checkNonBusinessDays) {
                while (!date.isAfter(leaveApplication.getToDate())) {
                    if (!validateHolidayDate(leaveApplication.getAccountId(), date) && !entityPreferenceService.validateWeekendDate(orgId, date)) {
                        createAndFillTimesheetForLeave(leaveApplication, officeHours, date,isLeaveApplicationNew,officeHours);
                    }
                    date = date.plusDays(1);
                }
            }
            else {
                while(!date.isAfter(leaveApplication.getToDate())) {
                    createAndFillTimesheetForLeave(leaveApplication, officeHours, date,isLeaveApplicationNew,officeHours);
                    date = date.plusDays(1);
                }
            }

        }
    }

    /**
     * @param leaveApplication
     * @param totalLeaveHours
     * @param date
     * @param isLeaveApplicationNew
     * @param officeHours
     * @Function: creates entry of leave in timesheet
     */
    private void createAndFillTimesheetForLeave(LeaveApplication leaveApplication, Float totalLeaveHours, LocalDate date,boolean isLeaveApplicationNew, Float officeHours){
        UserAccount userAccount = userAccountRepository.findByAccountId(leaveApplication.getAccountId());
        TimeSheet timeSheet = new TimeSheet();
        if (isLeaveApplicationNew) {
            if(totalLeaveHours<1){
                timeSheet.setNewEffort((int)(Float.parseFloat(decfor.format(totalLeaveHours*officeHours))*60));
            }
            else {
                timeSheet.setNewEffort((int) (officeHours* 60));
            }
        } else {
            if(totalLeaveHours<1){
                timeSheet.setNewEffort((-1) *((int)(Float.parseFloat(decfor.format(totalLeaveHours*officeHours))*60)));
            }
            else {
                timeSheet.setNewEffort((-1) * ((int) (officeHours* 60)));
            }
        }
        timeSheet.setAccountId(leaveApplication.getAccountId());

        timeSheet.setUserId(userAccount.getFkUserId().getUserId());
        timeSheet.setOrgId(userAccount.getOrgId());
        timeSheet.setBuId(null);
        timeSheet.setProjectId(null);
        timeSheet.setTeamId(null);
        timeSheet.setEntityId(leaveApplication.getLeaveApplicationId());
        timeSheet.setEntityTypeId(8);
        timeSheet.setEntityTitle(leaveApplication.getLeaveReason());
        timeSheet.setNewEffortDate(date);
        timeSheet.setEarnedTime(timeSheet.getNewEffort());
        timeSheetRepository.save(timeSheet);
    }

    public Integer getMaxWorkingMinutes (Long teamId, Long orgId) {
        if (teamId != null) {
            Optional<EntityPreference> teamPreferenceDb = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.TEAM, teamId);
            if (teamPreferenceDb.isPresent() && teamPreferenceDb.get().getOfficeHrsStartTime() != null && teamPreferenceDb.get().getOfficeHrsEndTime() != null) {

                if (teamPreferenceDb.get().getMinutesToWorkDaily() != null) {
                    return teamPreferenceDb.get().getMinutesToWorkDaily();
                }

                int minutes = Math.toIntExact(teamPreferenceDb.get().getOfficeHrsStartTime().until(teamPreferenceDb.get().getOfficeHrsEndTime(), ChronoUnit.MINUTES));
                if (teamPreferenceDb.get().getOfficeHrsStartTime().isAfter(teamPreferenceDb.get().getOfficeHrsEndTime())) {
                    minutes += 24 * 60;
                }
                if (teamPreferenceDb.get().getBreakDuration() != null) {
                    minutes -= teamPreferenceDb.get().getBreakDuration();
                }
                return minutes;
            }
        }
        if (orgId != null) {
            Optional<EntityPreference> orgPreferenceDb = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, orgId);
            if (orgPreferenceDb.isPresent() && orgPreferenceDb.get().getOfficeHrsStartTime() != null && orgPreferenceDb.get().getOfficeHrsEndTime() != null) {
                EntityPreference orgPreference = orgPreferenceDb.get();

                if (orgPreference.getMinutesToWorkDaily() != null) {
                    return orgPreference.getMinutesToWorkDaily();
                }

                int minutes = Math.toIntExact(orgPreferenceDb.get().getOfficeHrsStartTime().until(orgPreferenceDb.get().getOfficeHrsEndTime(), ChronoUnit.MINUTES));
                if (orgPreferenceDb.get().getOfficeHrsStartTime().isAfter(orgPreferenceDb.get().getOfficeHrsEndTime())) {
                    minutes += 24 * 60;
                }

                if (orgPreferenceDb.get().getBreakDuration() != null) {
                    minutes -= orgPreferenceDb.get().getBreakDuration();
                }
                return minutes;            }
        }
        return Constants.DEFAULT_OFFICE_MINUTES;
    }
    public Boolean validateHolidayDate (Long accountId, LocalDate date) {
        TimeSheet timeSheet = timeSheetRepository.findByAccountIdAndNewEffortDateAndEntityTypeId(accountId, date, Constants.HOLIDAY_TYPE_ID);
        return timeSheet != null;
    }
}

// create a map of account id to map of (effortDate - totalEffort, totalEarnedTime )
//        Map<Long, Map<LocalDate, List<Integer>>> mapAccountIdToEfforts = new HashMap<>();
//
//        mapAccountIdToRecord.forEach((accountId, timeSheets) -> {
//
//              Map<LocalDate, List<Integer>> mapDateToTotalEffortTotalEarnedTime = new HashMap<>();
//
//            // for given account id loop through all timesheet records and create a map of date to [List<total effort, total earned time> and List<EffortOnEntity>]
//            for (TimeSheet timeSheet : timeSheets) {
//                LocalDate newEffortDate = timeSheet.getNewEffortDate();
//                Integer newEffort = timeSheet.getNewEffort();
//                Integer earnedTime = timeSheet.getEarnedTime();
//                if(earnedTime==null) {
//                    earnedTime = 0;
//                }
//
//                if (mapDateToTotalEffortTotalEarnedTime.containsKey(newEffortDate)) {
//                    int currentEffortTotal = mapDateToTotalEffortTotalEarnedTime.get(newEffortDate).get(0);
//                    int currentEarnedTime = mapDateToTotalEffortTotalEarnedTime.get(newEffortDate).get(1);
//                    mapDateToTotalEffortTotalEarnedTime.put(newEffortDate, List.of(currentEffortTotal + newEffort, currentEarnedTime + earnedTime));
//                } else {
//                    mapDateToTotalEffortTotalEarnedTime.put(newEffortDate, List.of(newEffort, earnedTime));
//                }
//            }
//
//            // insert into map the dates for which no effort is recorded
//            LocalDate currentDate = timeInterval[0], toDate = timeInterval[1];
//            while(currentDate.isBefore(toDate.plusDays(1))){
//                if(!mapDateToTotalEffortTotalEarnedTime.containsKey(currentDate)){
//                 mapDateToTotalEffortTotalEarnedTime.put(currentDate, List.of(0, 0));
//                }
//                currentDate = currentDate.plusDays(1);
//            }
//
//            // add this mapDateToTotalEffortTotalEarnedTime to mapAccountIdToEfforts
//            mapAccountIdToEfforts.put(accountId, mapDateToTotalEffortTotalEarnedTime);
//        });
//
//
//        // create a list of TimeSheetResponse to return -- convert mapAccountIdToEfforts to list of total efforts
//        List<TimeSheetResponse> timeSheetResponses = new ArrayList<>();
//
//        mapAccountIdToEfforts.forEach((accountId, dateToEffortsMap) -> {
//
//            TimeSheetResponse timeSheetResponse = new TimeSheetResponse();
//            timeSheetResponse.setMaxDailyWorkingHrs(9);
//            timeSheetResponse.setAccount_Id(accountId);
//            timeSheetResponse.setEstimatedEntityTimeAchieved(null);
//
//            List<TotalEffort> totalEffortList = new ArrayList<>();
//
//            dateToEffortsMap.forEach((totalEffortDate, totalEffort) -> {
//                TotalEffort effort = new TotalEffort(totalEffort.get(0), totalEffort.get(1), totalEffortDate);
//                totalEffortList.add(effort);
//            });
//
//            timeSheetResponse.setEfforts(totalEffortList);
//
//            timeSheetResponses.add(timeSheetResponse);
//        });
