package com.tse.core_application.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.database.utilities.Pair;
import com.opencsv.CSVWriter;
import com.tse.core_application.constants.RoleEnum;
import com.tse.core_application.custom.model.AccountId;
import com.tse.core_application.custom.model.EmailFirstLastAccountId;
import com.tse.core_application.dto.*;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.filters.JwtRequestFilter;
import com.tse.core_application.model.*;
import com.tse.core_application.repository.*;
import com.tse.core_application.utils.CommonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.Query;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    private static final Logger logger = LogManager.getLogger(AttendanceService.class.getName());
    ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TimeSheetRepository timesheetRepository;

    @Autowired
    private EntityPreferenceRepository entityPreferenceRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private InviteService inviteService;

    @Autowired
    private AccessDomainRepository accessDomainRepository;

    @Autowired
    private HolidayOffDayRepository holidayOffDayRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private EntityPreferenceService entityPreferenceService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private LeaveApplicationRepository leaveApplicationRepository;

    @Autowired
    private UserFeatureAccessRepository userFeatureAccessRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * This functions creates attendance response and returns to the api
     */
    public AttendanceResponseDTO getAttendanceData(AttendanceRequestDTO attendanceRequest, String headeraccountIds) throws IllegalAccessException {
        List<Long> headerAccountIdsList = jwtRequestFilter.getAccountIdsFromHeader(headeraccountIds);
        normalizeRequest(attendanceRequest);
        validateAccess(attendanceRequest, headerAccountIdsList);

        //getting org and all teams belonging to org preference
        Pair<List<Integer>, Integer> orgPreference = entityPreferenceService.getOfficeMinutesAndOffDaysFromOrgPreferenceOrDefault(attendanceRequest.getOrgId());
        Map<Long, Pair<List<Integer>, Integer>> teamPreference = getTeamPreference(attendanceRequest.getOrgId());

        List<LocalDate> holidayDateList = entityPreferenceRepository.getListOfHolidayDatesByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, attendanceRequest.getOrgId());

        //setting default offDays and expected work minutes as of org
        List<Integer> offDays = orgPreference.getFirst();
        Integer expectedWorkMins = orgPreference.getSecond();

        List<TimeSheet> timesheetEntries;

        //creating a map of entities filter to get timesheets according to filter
        Map<Integer, Long> entityFilters = new HashMap<>();
        entityFilters.put(Constants.EntityTypes.ORG, attendanceRequest.getOrgId());

        if (attendanceRequest.getTeamId() != null) {
            entityFilters.put(Constants.EntityTypes.TEAM, attendanceRequest.getTeamId());
            //seeting off days and expected work minutes if present in team preference
            if (teamPreference.keySet().contains(attendanceRequest.getTeamId())) {
                offDays = teamPreference.get(attendanceRequest.getTeamId()).getFirst() != null ? teamPreference.get(attendanceRequest.getTeamId()).getFirst() : orgPreference.getFirst();
                expectedWorkMins = teamPreference.get(attendanceRequest.getTeamId()).getSecond() != null ? teamPreference.get(attendanceRequest.getTeamId()).getSecond() : orgPreference.getSecond();
            }
        }
        final Integer finalExpectedWorkMins = expectedWorkMins;
        
        if (attendanceRequest.getProjectId() != null) {
            entityFilters.put(Constants.EntityTypes.PROJECT, attendanceRequest.getProjectId());
        }

        //get timesheet list
        timesheetEntries = getTimesheetListForEntity(entityFilters, attendanceRequest.getAccountIds(), attendanceRequest.getStartDate(), attendanceRequest.getEndDate());

        // Construct response DTO
        AttendanceResponseDTO response = new AttendanceResponseDTO();
        response.setOrgId(attendanceRequest.getOrgId());
        response.setProjectId(attendanceRequest.getProjectId());
        response.setTeamId(attendanceRequest.getTeamId());
        response.setAccountIds(attendanceRequest.getAccountIds());
        response.setStartDate(attendanceRequest.getStartDate());
        response.setEndDate(attendanceRequest.getEndDate());

        // Initialize counters for holidays and off days
        int totalHolidays = 0;
        int totalOffDays = 0;

        LocalDate startDate = attendanceRequest.getStartDate();

        // Initialize map to store attendance data for each date, account, and attendance type
        Map<LocalDate, Map<Long, AttendanceDataDTO>> attendanceMap = new TreeMap<>();

        // Process each timesheet entry
        for (TimeSheet entry : timesheetEntries) {
            LocalDate date = entry.getNewEffortDate();
            Long userAccountId = entry.getAccountId();
            Integer minutesWorked = entry.getNewEffort();
            Integer expectedWork = expectedWorkMins;
            //getting expected work minutes according to team preference, if present
            if (teamPreference.keySet().contains(entry.getTeamId()) && teamPreference.get(entry.getTeamId()).getSecond() != null) {
                expectedWork = teamPreference.get(entry.getTeamId()).getSecond();
            }

            // Check if the day is a holiday, off day, or leave
            boolean isHoliday = false;
            boolean isOffDay = false;
            boolean isLeave = false;

            //checking for off days
            if (teamPreference.keySet().contains(entry.getTeamId()) && teamPreference.get(entry.getTeamId()).getFirst().contains(date.getDayOfWeek().getValue())) {
                isOffDay = true;
            } else if (offDays.contains(date.getDayOfWeek().getValue())) {
                isOffDay = true;
            }

            if (Objects.equals(entry.getEntityTypeId(), Constants.EntityTypes.LEAVE)) {
                isLeave = true;
            }
            if (Objects.equals(entry.getEntityTypeId(), Constants.EntityTypes.HOLIDAY)) {
                isHoliday = true;
            }

            //checking if attendance map contains user account id
            if (!attendanceMap.isEmpty() && attendanceMap.containsKey(date) && attendanceMap.get(date).containsKey(userAccountId)) {
                AttendanceDataDTO attendanceData = attendanceMap.get(date).get(userAccountId);
                attendanceData.setMinsWorked(attendanceData.getMinsWorked() + minutesWorked);
                if (!attendanceData.getTypeName().equalsIgnoreCase(Constants.AttendanceTypeEnum.OFF_DAY.getAttendanceType()) && !attendanceData.getTypeName().equalsIgnoreCase(Constants.AttendanceTypeEnum.HOLIDAY.getAttendanceType()) && !attendanceData.getTypeName().equalsIgnoreCase(Constants.AttendanceTypeEnum.LEAVE.getAttendanceType())) {
                    double percentageWorked = (double) attendanceData.getMinsWorked() / expectedWork * 100;
                    if (percentageWorked >= 75) {
                        attendanceData.setTypeName(Constants.AttendanceTypeEnum.PRESENT.getAttendanceType());
                        attendanceData.setTypeId(Constants.AttendanceTypeEnum.PRESENT.getAttendanceTypeId());
                    } else {
                        attendanceData.setTypeName(Constants.AttendanceTypeEnum.PARTIAL.getAttendanceType());
                        attendanceData.setTypeId(Constants.AttendanceTypeEnum.PARTIAL.getAttendanceTypeId());
                    }
                }
                continue;
            }

            // Determine attendance type
            String attendanceType;
            Integer typeId;

            if (isHoliday) {
                attendanceType = Constants.AttendanceTypeEnum.HOLIDAY.getAttendanceType();
                typeId = Constants.AttendanceTypeEnum.HOLIDAY.getAttendanceTypeId();
                totalHolidays++;
                minutesWorked = 0;
                expectedWork = 0;
            } else if (isLeave) {
                attendanceType = Constants.AttendanceTypeEnum.LEAVE.getAttendanceType();
                typeId = Constants.AttendanceTypeEnum.LEAVE.getAttendanceTypeId();
                if (expectedWork > minutesWorked) {
                    expectedWork -= minutesWorked;
                    minutesWorked = 0;
                } else {
                    minutesWorked = 0;
                    expectedWork = 0;
                }
            } else if (isOffDay) {
                attendanceType = Constants.AttendanceTypeEnum.OFF_DAY.getAttendanceType();
                typeId = Constants.AttendanceTypeEnum.OFF_DAY.getAttendanceTypeId();
                totalOffDays++;
                expectedWork = 0;
            } else {
                // Calculate percentage of expected work hours filled
                double percentageWorked = (double) minutesWorked / expectedWork * 100;

                // Determine attendance type based on percentage
                if (percentageWorked >= 75) {
                    attendanceType = Constants.AttendanceTypeEnum.PRESENT.getAttendanceType();
                    typeId = Constants.AttendanceTypeEnum.PRESENT.getAttendanceTypeId();
                } else {
                    attendanceType = Constants.AttendanceTypeEnum.PARTIAL.getAttendanceType();
                    typeId = Constants.AttendanceTypeEnum.PARTIAL.getAttendanceTypeId();
                }
            }

            // Create an AttendanceDataDTO and add it to the map
            AttendanceDataDTO attendanceDataDTO = new AttendanceDataDTO();
            attendanceDataDTO.setAccountId(userAccountId);
            attendanceDataDTO.setMinsWorked(minutesWorked);
            attendanceDataDTO.setTypeName(attendanceType);
            attendanceDataDTO.setTypeId(typeId);
            attendanceDataDTO.setExpectedWorkMins(expectedWork);

            attendanceMap
                    .computeIfAbsent(date, k -> new HashMap<>())
                    .put(userAccountId, attendanceDataDTO);
        }

        //adding offdays and absents
        while (!startDate.isAfter(attendanceRequest.getEndDate())) {
            //adding not added date
            if (!attendanceMap.containsKey(startDate)) {
                Map<Long, AttendanceDataDTO> attendanceRecords = new HashMap<>();
                // Iterate through each account
                for (Long account : attendanceRequest.getAccountIds()) {
                    // Create an Off Day AttendanceDataDTO
                    AttendanceDataDTO otherAttendance = new AttendanceDataDTO();
                    otherAttendance.setAccountId(account);
                    if (offDays.contains(startDate.getDayOfWeek().getValue())) {
                        otherAttendance.setTypeName(Constants.AttendanceTypeEnum.OFF_DAY.getAttendanceType());
                        otherAttendance.setTypeId(Constants.AttendanceTypeEnum.OFF_DAY.getAttendanceTypeId());
                        totalOffDays++;
                    } else if (startDate.isEqual(LocalDate.now()) || startDate.isAfter(LocalDate.now())) {
                        if (holidayDateList.contains(startDate)) {
                            otherAttendance.setTypeName(Constants.AttendanceTypeEnum.HOLIDAY.getAttendanceType());
                            otherAttendance.setTypeId(Constants.AttendanceTypeEnum.HOLIDAY.getAttendanceTypeId());
                            otherAttendance.setExpectedWorkMins(0);
                            totalHolidays++;
                        } else {
                            otherAttendance.setTypeName(Constants.AttendanceTypeEnum.EMPTY.getAttendanceType());
                            otherAttendance.setTypeId(Constants.AttendanceTypeEnum.EMPTY.getAttendanceTypeId());
                            otherAttendance.setExpectedWorkMins(expectedWorkMins);
                        }
                    } else {
                        otherAttendance.setTypeName(Constants.AttendanceTypeEnum.ABSENT.getAttendanceType());
                        otherAttendance.setTypeId(Constants.AttendanceTypeEnum.ABSENT.getAttendanceTypeId());
                        otherAttendance.setExpectedWorkMins(expectedWorkMins);
                    }
                    otherAttendance.setMinsWorked(0);

                    // Add the Off Day record to the map
                    attendanceRecords.put(account, otherAttendance);
                }
                attendanceMap.put(
                        startDate,
                        attendanceRecords
                );
            }
            else {
                attendanceMap.get(startDate).values().forEach(attendance -> {
                    if(attendance.getTypeId().equals(Constants.AttendanceTypeEnum.LEAVE.getAttendanceTypeId()) && attendance.getMinsWorked() < 0){
                        attendance.setMinsWorked(0);
                        attendance.setTypeId(Constants.AttendanceTypeEnum.EMPTY.getAttendanceTypeId());
                        attendance.setExpectedWorkMins(finalExpectedWorkMins);
                    }
                });
            }
            Map<Long, AttendanceDataDTO> attendanceDataList = attendanceMap.get(startDate);

            // Get a set of account IDs already present for this date
            Set<Long> existingAccountIds = attendanceDataList.keySet();

            // Iterate through all account IDs and check if they are already in the map
            for (Long acc : attendanceRequest.getAccountIds()) {
                if (!existingAccountIds.contains(acc)) {
                    // Create an Absent AttendanceDataDTO
                    AttendanceDataDTO absentAttendance = new AttendanceDataDTO();
                    absentAttendance.setAccountId(acc);
                    absentAttendance.setMinsWorked(0);
                    absentAttendance.setExpectedWorkMins(expectedWorkMins);
                    //check if day was off else mark absent
                    if (offDays.contains(startDate.getDayOfWeek().getValue())) {
                        absentAttendance.setTypeName(Constants.AttendanceTypeEnum.OFF_DAY.getAttendanceType());
                        absentAttendance.setTypeId(Constants.AttendanceTypeEnum.OFF_DAY.getAttendanceTypeId());
                        absentAttendance.setExpectedWorkMins(0);
                    } else if (startDate.isEqual(LocalDate.now()) || startDate.isAfter(LocalDate.now())) {
                        if (holidayDateList.contains(startDate)) {
                            absentAttendance.setTypeName(Constants.AttendanceTypeEnum.HOLIDAY.getAttendanceType());
                            absentAttendance.setTypeId(Constants.AttendanceTypeEnum.HOLIDAY.getAttendanceTypeId());
                            absentAttendance.setExpectedWorkMins(0);
                        } else {
                            absentAttendance.setTypeName(Constants.AttendanceTypeEnum.EMPTY.getAttendanceType());
                            absentAttendance.setTypeId(Constants.AttendanceTypeEnum.EMPTY.getAttendanceTypeId());
                        }
                    } else {
                        absentAttendance.setTypeName(Constants.AttendanceTypeEnum.ABSENT.getAttendanceType());
                        absentAttendance.setTypeId(Constants.AttendanceTypeEnum.ABSENT.getAttendanceTypeId());
                    }
                    // Add the Absent record to the map for this date
                    attendanceDataList.put(acc, absentAttendance);
                }
            }
            startDate = startDate.plusDays(1);
        }

        // Set calculated values in the response DTO
        response.setTotalHolidays(totalHolidays);
        response.setTotalOffDays(totalOffDays);
        response.setAttendance(attendanceMap);

        return response;
    }

    /**
     * Normalizes the attendance request by setting default values for unspecified parameters.
     */
    private void normalizeRequest(AttendanceRequestDTO request) {
        if (request.getStartDate() == null) {
            request.setStartDate(LocalDate.now().minusMonths(2));
        }
        if (request.getEndDate() == null) {
            request.setEndDate(LocalDate.now());
        }
        if(request.getStartDate().isAfter(request.getEndDate())){
            throw new ValidationFailedException("Requested dates are not valid, fromDate is after toDate.");
        }

        //adding account id list
        if (request.getAccountIds() == null || request.getAccountIds().isEmpty()) {
            List<AccountId> accountIdList = new ArrayList<>();
            if (request.getTeamId() != null) {
                accountIdList = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdAndIsActive(Constants.EntityTypes.TEAM, request.getTeamId(), true);
            } else if (request.getProjectId() != null) {
                accountIdList = projectService.getprojectMembersAccountIdList(List.of(request.getProjectId()));
            } else if (request.getOrgId() != null) {
                accountIdList = userAccountRepository.findAccountIdByOrgIdAndIsActive(request.getOrgId(), true);
            }
            // Convert List<AccountId> to List<Long>
            List<Long> accountIds = accountIdList.stream()
                    .map(AccountId::getAccountId)
                    .collect(Collectors.toList());

            request.setAccountIds(accountIds);
        }
    }

    /**
     * This method return map of team id with pair containg list of off days and office minutes
     */
    private Map<Long, Pair<List<Integer>, Integer>> getTeamPreference(Long orgId) {
        Map<Long, Pair<List<Integer>, Integer>> teamPreferenceMap = new HashMap<>();
        List<Long> teamIdList = teamRepository.findTeamIdsByOrgId(orgId);
        List<EntityPreference> teamPreferenceList = entityPreferenceRepository.findByEntityTypeIdAndEntityIdIn(Constants.EntityTypes.TEAM, teamIdList);
        for (EntityPreference teamPreference : teamPreferenceList) {
//            Optional<EntityPreference> teamPreference = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.TEAM, teamId);
            if (teamPreference != null) {
                long officeMinutes = teamPreference.getOfficeHrsStartTime().until(teamPreference.getOfficeHrsEndTime(), ChronoUnit.MINUTES);
                teamPreferenceMap.put(teamPreference.getEntityId(), new Pair<>(teamPreference.getOffDays(), (int) officeMinutes));
            }
        }
        return teamPreferenceMap;
    }

    /**
     * Validates the access rights of the user to view attendance based on the provided request parameters.
     *
     * @throws IllegalAccessException If the user does not have the necessary authorization to view attendance.
     *                                This exception is thrown with a message suggesting the user to check their access rights.
     */
    private void validateAccess(AttendanceRequestDTO request, List<Long> accountIds) throws IllegalAccessException {
        List<Integer> rolIdList = new ArrayList<>();
        List<AccountId> accountIdList = new ArrayList<>();
        boolean checkHrAttendeceValiadtion=false;
        if (request.getTeamId() != null) {
            //checks if team is associated with org
            if (!teamRepository.existsByFkOrgIdOrgIdAndTeamIdAndIsDisabled(request.getOrgId(), request.getTeamId(), false)) {
                throw new IllegalAccessException("The specified team does not belong to the organization provided. Please verify your request and ensure that the team is associated with the correct organization.");
            }
            if (checkHrAccessForAttendence(request, accountIds)) {
                checkHrAttendeceValiadtion=true;
                accountIdList.addAll(accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdAndIsActive(Constants.EntityTypes.TEAM, request.getTeamId(), true));
            } else {
                rolIdList.add(RoleEnum.PROJECT_MANAGER_NON_SPRINT.getRoleId());
                rolIdList.add(RoleEnum.TEAM_MANAGER_NON_SPRINT.getRoleId());
                rolIdList.add(RoleEnum.TEAM_MANAGER_SPRINT.getRoleId());
                rolIdList.add(RoleEnum.PROJECT_MANAGER_SPRINT.getRoleId());
                accountIdList.addAll(accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(Constants.EntityTypes.TEAM, request.getTeamId(), rolIdList, true));
            }
        } else if (request.getProjectId() != null) {
            //checks if project is associated with org
            if (!projectRepository.existsByOrgIdAndProjectIdAndIsDisabled(request.getOrgId(), request.getProjectId(), false)) {
                throw new IllegalAccessException("The specified project does not belong to the organization provided. Please verify your request and ensure that the project is associated with the correct organization.");
            }
            //checks if team is associated with project
            if (request.getTeamId() != null && !teamRepository.existsByFkProjectIdProjectIdAndTeamIdAndIsDisabled(request.getProjectId(), request.getTeamId(), false)) {
                throw new IllegalAccessException("The specified team does not belong to the project provided. Please verify your request and ensure that the team is associated with the correct project.");
            }
            if (checkHrAccessForAttendence(request, accountIds)) {
                checkHrAttendeceValiadtion=true;
                accountIdList.addAll(accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdAndIsActive(Constants.EntityTypes.PROJECT, request.getProjectId(), true));
            } else {
                rolIdList.add(RoleEnum.PROJECT_ADMIN.getRoleId());
                rolIdList.add(RoleEnum.BACKUP_PROJECT_ADMIN.getRoleId());
                accountIdList.addAll(accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(Constants.EntityTypes.PROJECT, request.getProjectId(), rolIdList, true));
            }
        }
        else if (request.getOrgId() != null) {
            if (checkHrAccessForAttendence(request, accountIds)) {
                checkHrAttendeceValiadtion=true;
                accountIdList.addAll(accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdAndIsActive(Constants.EntityTypes.ORG, request.getOrgId(), true));
            } else {
                rolIdList.add(RoleEnum.PROJECT_ADMIN.getRoleId());
                rolIdList.add(RoleEnum.BACKUP_PROJECT_ADMIN.getRoleId());
                accountIdList.addAll(accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(Constants.EntityTypes.PROJECT, request.getProjectId(), rolIdList, true));
            }
                rolIdList.add(RoleEnum.ORG_ADMIN.getRoleId());
                rolIdList.add(RoleEnum.BACKUP_ORG_ADMIN.getRoleId());
                accountIdList.addAll(accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(Constants.EntityTypes.ORG, request.getOrgId(), rolIdList, true));
            }

            List<Long> authorizedAccountIds = accountIdList.stream()
                    .map(AccountId::getAccountId)
                    .collect(Collectors.toList());

            //checking if account is authorized Team roles take precedence over project roles, and project roles take precedence over organization roles.
            if (!CommonUtils.containsAny(authorizedAccountIds, accountIds) && !checkHrAttendeceValiadtion ) {
                throw new IllegalAccessException("You do not have the necessary authorization to view attendance. Please check your access rights.");
            }
        }

    public boolean checkHrAccessForAttendence(AttendanceRequestDTO request,List<Long>headerAccountIds) {
        if (request.getTeamId()!=null) {
            boolean hasOrgAccess = userFeatureAccessRepository
                    .existsByEntityTypeIdAndEntityIdAndUserAccountIdAndActionIdAndIsDeletedFalse(
                            Constants.EntityTypes.ORG, request.getOrgId(), headerAccountIds, Constants.ActionId.VIEW_ATTENDENCE);
            boolean hasProjectAccess = userFeatureAccessRepository
                    .existsByEntityTypeIdAndEntityIdAndUserAccountIdAndActionIdAndIsDeletedFalse(
                            Constants.EntityTypes.PROJECT, request.getProjectId(), headerAccountIds, Constants.ActionId.VIEW_ATTENDENCE);
            boolean hasTeamAccess = userFeatureAccessRepository
                    .existsByEntityTypeIdAndEntityIdAndUserAccountIdAndActionIdAndIsDeletedFalse(
                            Constants.EntityTypes.TEAM, request.getTeamId(), headerAccountIds, Constants.ActionId.VIEW_ATTENDENCE);
            if (hasOrgAccess || hasProjectAccess || hasTeamAccess) {
                return true;
            }
        } else if (request.getProjectId() != null) {
            boolean hasOrgAccess = userFeatureAccessRepository
                    .existsByEntityTypeIdAndEntityIdAndUserAccountIdAndActionIdAndIsDeletedFalse(
                            Constants.EntityTypes.ORG, request.getOrgId(), headerAccountIds, Constants.ActionId.VIEW_ATTENDENCE);
            boolean hasProjectAccess = userFeatureAccessRepository
                    .existsByEntityTypeIdAndEntityIdAndUserAccountIdAndActionIdAndIsDeletedFalse(
                            Constants.EntityTypes.PROJECT, request.getProjectId(), headerAccountIds, Constants.ActionId.VIEW_ATTENDENCE);
            if (hasOrgAccess || hasProjectAccess) {
                return true;
            }
        } else if (request.getOrgId() != null) {
            boolean hasOrgAccess = userFeatureAccessRepository
                    .existsByEntityTypeIdAndEntityIdAndUserAccountIdAndActionIdAndIsDeletedFalse(
                            Constants.EntityTypes.ORG, request.getOrgId(), headerAccountIds, Constants.ActionId.VIEW_ATTENDENCE);
            if (hasOrgAccess) {
                return true;
            }
        }
        return false;
    }
    public boolean checkHrAccessForAttendenceForTeamsAndOrgs(List<Long>teamIds, List<Long>orgIds,List<Long>headerAccountIds) {
        if (teamIds != null && !teamIds.isEmpty()) {
            for (Long teamId : teamIds) {
                Team team = teamRepository.findByTeamId(teamId);
                if (team != null) {
                    boolean hasOrgAccess = userFeatureAccessRepository
                            .existsByEntityTypeIdAndEntityIdAndUserAccountIdAndActionIdAndIsDeletedFalse(
                                    Constants.EntityTypes.ORG, team.getFkOrgId().getOrgId(), headerAccountIds, Constants.ActionId.VIEW_ATTENDENCE);
                    boolean hasProjectAccess = userFeatureAccessRepository
                            .existsByEntityTypeIdAndEntityIdAndUserAccountIdAndActionIdAndIsDeletedFalse(
                                    Constants.EntityTypes.PROJECT, team.getFkProjectId().getProjectId(), headerAccountIds, Constants.ActionId.VIEW_ATTENDENCE);
                    boolean hasTeamAccess = userFeatureAccessRepository
                            .existsByEntityTypeIdAndEntityIdAndUserAccountIdAndActionIdAndIsDeletedFalse(
                                    Constants.EntityTypes.TEAM, team.getTeamId(), headerAccountIds, Constants.ActionId.VIEW_ATTENDENCE);
                    if (hasOrgAccess || hasProjectAccess || hasTeamAccess) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Retrieves a list of time sheet entries based on the specified entity type, entity ID, account IDs, and date range.
     */
    private List<TimeSheet> getTimesheetListForEntity(Map<Integer, Long> entityFilter, List<Long> accountIdsList, LocalDate startDate, LocalDate endDate) {
        StringBuilder queryBuilder = new StringBuilder("SELECT DISTINCT * FROM tse.time_tracking WHERE ");

        queryBuilder.append("account_id IN :accountIds AND new_effort_date BETWEEN :startDate AND :endDate AND org_id = :orgId ");

        Set<Integer> entityTypeSet = entityFilter.keySet();
        //adding or project id is null to get holidays and leaves
        if (entityTypeSet.contains(Constants.EntityTypes.PROJECT)) {
            queryBuilder.append("AND (project_id = :projectId OR project_id IS NULL) ");
        }
        //adding or team id is null to get holidays and leaves
        if (entityTypeSet.contains(Constants.EntityTypes.TEAM)) {
            queryBuilder.append("AND (team_id = :teamId OR team_id IS NULL) ");
        }

        Query nativeQuery = entityManager.createNativeQuery(queryBuilder.toString(), TimeSheet.class);

        // Set parameters
        nativeQuery.setParameter("accountIds", accountIdsList);
        nativeQuery.setParameter("startDate", startDate);
        nativeQuery.setParameter("endDate", endDate);
        nativeQuery.setParameter("orgId", entityFilter.get(Constants.EntityTypes.ORG));
        if (entityTypeSet.contains(Constants.EntityTypes.PROJECT)) {
            nativeQuery.setParameter("projectId", entityFilter.get(Constants.EntityTypes.PROJECT));
        }
        if (entityTypeSet.contains(Constants.EntityTypes.TEAM)) {
            nativeQuery.setParameter("teamId", entityFilter.get(Constants.EntityTypes.TEAM));
        }

        List<TimeSheet> timeSheetList = nativeQuery.getResultList();

        return timeSheetList;
    }

    public byte[] convertToCsv(AttendanceResponseDTO attendanceData) throws IOException {
        Set<LocalDate> dateSet = attendanceData.getAttendance().keySet();
        String[] dateStrings = dateSet.stream()
                .map(date -> new SimpleDateFormat("yyyy-MM-dd").format(java.sql.Date.valueOf(date)))
                .toArray(String[]::new);

        // Create header row with name and dates
        String[] headerRow = new String[dateStrings.length + 1];
        headerRow[0] = "name";  // First field is the name
        System.arraycopy(dateStrings, 0, headerRow, 1, dateStrings.length);

        try (StringWriter stringWriter = new StringWriter();
             CSVWriter csvWriter = new CSVWriter(stringWriter)) {

            // Write header row
            csvWriter.writeNext(headerRow);

            // Iterate through attendance data and write each user's row
            attendanceData.getAccountIds().forEach(accountId -> {
                String[] userRow = new String[dateStrings.length + 1];
                UserAccount user = userAccountRepository.findByAccountId(accountId);
                userRow[0] = user.getFkUserId().getFirstName() + user.getFkUserId().getLastName();

                for (int i = 0; i < dateStrings.length; i++) {
                    LocalDate currentDate = LocalDate.parse(dateStrings[i]);
                    Map<Long, AttendanceDataDTO> userAttendance = attendanceData.getAttendance().get(currentDate);
                    AttendanceDataDTO attendanceDataDTO = userAttendance.get(accountId);

                    // Assuming AttendanceDataDTO has a method getStatus() that returns the user's status
                    userRow[i + 1] = attendanceDataDTO != null ? attendanceDataDTO.getTypeName() : "Absent";
                }

                // Write user row
                csvWriter.writeNext(userRow);
            });

            csvWriter.flush();
            return stringWriter.toString().getBytes();

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void validateLeaveAttendanceAccess(LeaveAttendanceRequest request, List<Long> accountIdList) throws IllegalAccessException {

        if (request.getFromDate().isAfter(request.getToDate())) {
            throw new ValidationFailedException("From date can't be before To date");
        }
        AttendanceRequestDTO attendanceRequestDTO = new AttendanceRequestDTO();

        switch (request.getEntityTypeId()) {
            case Constants.EntityTypes.TEAM:
                setTeamAndProjectAndOrgIds(attendanceRequestDTO, request.getEntityId());
                break;

            case Constants.EntityTypes.PROJECT:
                setProjectAndOrgIds(attendanceRequestDTO, request.getEntityId());
                break;

            case Constants.EntityTypes.ORG:
                validateOrganizationExists(request.getEntityId());
                attendanceRequestDTO.setOrgId(request.getEntityId());
                break;

            default:
                throw new EntityNotFoundException("Invalid entity type: " + request.getEntityTypeId());
        }

        validateAccess(attendanceRequestDTO, accountIdList);
    }

    private void setTeamAndProjectAndOrgIds(AttendanceRequestDTO attendanceRequestDTO, Long teamId) {
        Team team = teamRepository.findByTeamId(teamId);
        if (team == null) {
            throw new EntityExistsException("Team doesn't exist");
        }

        attendanceRequestDTO.setTeamId(team.getTeamId());
        attendanceRequestDTO.setProjectId(team.getFkProjectId().getProjectId());
        attendanceRequestDTO.setOrgId(team.getFkOrgId().getOrgId());
    }

    private void setProjectAndOrgIds(AttendanceRequestDTO attendanceRequestDTO, Long projectId) {
        Project project = projectRepository.findByProjectId(projectId);
        if (project == null) {
            throw new EntityExistsException("Project doesn't exist");
        }

        attendanceRequestDTO.setProjectId(project.getProjectId());
        attendanceRequestDTO.setOrgId(project.getOrgId());
    }

    private void validateOrganizationExists(Long orgId) {
        Organization organization = organizationRepository.findByOrgId(orgId);
        if (!organizationRepository.existsByOrgId(orgId)) {
            throw new EntityExistsException("Organization doesn't exist");
        }
    }

    public List<LeaveAttendanceResponse> getMemberOnLeaveForAttendance(LeaveAttendanceRequest request, String accountIds, String timeZone) throws IllegalAccessException {
        List<LeaveAttendanceResponse> responseList = new ArrayList<>();
        List<Long> headerAccountIdList = CommonUtils.convertToLongList(accountIds);

        validateLeaveAttendanceAccess(request, headerAccountIdList);

        Set<Long> eligibleAccountIds = fetchEligibleAccountIds(request);
        if (eligibleAccountIds.isEmpty()) {
            return responseList;
        }

        List<Long> targetAccountIds = resolveTargetAccountIds(request.getAccountIdList(), eligibleAccountIds);
        if (targetAccountIds == null || targetAccountIds.isEmpty()) {
            return responseList;
        }

        // Get user info
        List<EmailFirstLastAccountId> userDetailsList = userAccountRepository.getEmailFirstNameLastNameAccountIdByAccountIdIn(targetAccountIds);
        Map<Long, EmailFirstLastAccountId> userMap = userDetailsList.stream()
                .collect(Collectors.toMap(
                        EmailFirstLastAccountId::getAccountId,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        // Get entity preferences (aliases)
        Long orgId = userAccountRepository.findOrgIdByAccountIdAndIsActive(targetAccountIds.get(0), true).getOrgId();
        Optional<EntityPreference> optionalPref = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, orgId);
        String timeOffAlias = Constants.LeaveTypeNameConstant.TIME_OFF;
        String sickLeaveAlias = Constants.LeaveTypeNameConstant.SICK_LEAVE;
        if (optionalPref.isPresent()) {
            EntityPreference pref = optionalPref.get();
            if (pref.getTimeOffAlias() != null) timeOffAlias = pref.getTimeOffAlias();
            if (pref.getSickLeaveAlias() != null) sickLeaveAlias = pref.getSickLeaveAlias();
        }

        Map<Long, List<UserLeaveAttendanceResponse>> userLeaveMap = new HashMap<>();

        LocalDate fromDate = request.getFromDate();
        LocalDate toDate = request.getToDate();

        for (Long accountId : targetAccountIds) {
            List<LeaveApplication> leaveApplications = leaveApplicationRepository.findByAccountIdAndOverlappingDateRange(
                    accountId,
                    fromDate,
                    toDate,
                    List.of(Constants.LeaveApplicationStatusIds.CONSUMED_LEAVE_APPLICATION_STATUS_ID, Constants.LeaveApplicationStatusIds.APPROVED_LEAVE_APPLICATION_STATUS_ID)
            );
            EmailFirstLastAccountId user = userMap.get(accountId);
            LeaveAttendanceResponse userResponse = new LeaveAttendanceResponse();
            userResponse.setAccountId(accountId);
            userResponse.setFirstName(user.getFirstName());
            userResponse.setLastName(user.getLastName());

            if (!leaveApplications.isEmpty()) {
                List<UserLeaveAttendanceResponse> leaveDetails = new ArrayList<>();

                for (LeaveApplication leave : leaveApplications) {
                    LocalDate leaveStart = leave.getFromDate();
                    LocalDate leaveEnd = leave.getToDate();

                    // Calculate overlap range between leave and request
                    LocalDate overlapStart = leaveStart.isBefore(fromDate) ? fromDate : leaveStart;
                    LocalDate overlapEnd = leaveEnd.isAfter(toDate) ? toDate : leaveEnd;

                    while (!overlapStart.isAfter(overlapEnd)) {
                        UserLeaveAttendanceResponse userLeave = new UserLeaveAttendanceResponse();
                        userLeave.setDate(overlapStart);
                        userLeave.setLeaveTypeId((int) leave.getLeaveTypeId());
                        userLeave.setLeaveTypeName(leave.getLeaveTypeId() == 1 ? timeOffAlias : sickLeaveAlias);
                        userLeave.setIsHalfDayLeave(leave.getIsLeaveForHalfDay());
                        userLeave.setLeaveApplicationId(leave.getLeaveApplicationId());

                        leaveDetails.add(userLeave);
                        overlapStart = overlapStart.plusDays(1);
                    }
                }

                if (!leaveDetails.isEmpty()) {
                    leaveDetails.sort(Comparator.comparing(UserLeaveAttendanceResponse::getDate));
                    userResponse.setUserLeaveAttendanceResponseList(leaveDetails);
                }
            }
            responseList.add(userResponse);
        }

        responseList.sort(Comparator
                .comparing(LeaveAttendanceResponse::getFirstName, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(LeaveAttendanceResponse::getLastName, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(LeaveAttendanceResponse::getAccountId));

        return responseList;
    }


    private Set<Long> fetchEligibleAccountIds(LeaveAttendanceRequest request) {
        Set<Long> accountIds = new HashSet<>();
        Long entityId = request.getEntityId();
        Integer entityType = request.getEntityTypeId();

        switch (entityType) {
            case Constants.EntityTypes.TEAM:
                accountIds.addAll(
                        accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdAndIsActive(Constants.EntityTypes.TEAM, entityId, true)
                                .stream()
                                .map(AccountId::getAccountId)
                                .collect(Collectors.toSet())
                );
                break;

            case Constants.EntityTypes.PROJECT:
                accountIds.addAll(
                        projectService.getprojectMembersAccountIdList(List.of(entityId))
                                .stream()
                                .map(AccountId::getAccountId)
                                .collect(Collectors.toSet())
                );
                break;

            case Constants.EntityTypes.ORG:
                accountIds.addAll(
                        userAccountRepository.findAccountIdByOrgIdAndIsActive(entityId, true)
                                .stream()
                                .map(AccountId::getAccountId)
                                .collect(Collectors.toSet())
                );
                break;
        }

        return accountIds;
    }

    private List<Long> resolveTargetAccountIds(List<Long> requestedAccountIds, Set<Long> eligibleAccountIds) {
        if (requestedAccountIds == null || requestedAccountIds.isEmpty()) {
            return new ArrayList<>(eligibleAccountIds);
        }

        return requestedAccountIds.stream()
                .filter(eligibleAccountIds::contains)
                .collect(Collectors.toList());
    }

    public AttendanceResponseV2Dto getAttendanceDataV2(AttendanceRequestDTO attendanceRequest, String headerAccountIds) throws IllegalAccessException {
        List<Long> headerAccountIdsList = jwtRequestFilter.getAccountIdsFromHeader(headerAccountIds);
        normalizeRequest(attendanceRequest);
        validateAccess(attendanceRequest, headerAccountIdsList);

        //getting org and all teams belonging to org preference
        Pair<List<Integer>, Integer> orgPreference = entityPreferenceService.getOfficeMinutesAndOffDaysFromOrgPreferenceOrDefault(attendanceRequest.getOrgId());
        Map<Long, Pair<List<Integer>, Integer>> teamPreference;
        if (attendanceRequest.getTeamId() != null) {
            teamPreference = getTeamPreference(attendanceRequest.getOrgId());
        } else {
            teamPreference = new HashMap<>();
        }

        Optional<EntityPreference> optionalPref = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, attendanceRequest.getOrgId());
        String timeOffAlias;
        String sickLeaveAlias;

        if (optionalPref.isPresent()) {
            EntityPreference pref = optionalPref.get();
            if (pref.getTimeOffAlias() != null) timeOffAlias = pref.getTimeOffAlias();
            else {
                timeOffAlias = Constants.LeaveTypeNameConstant.TIME_OFF;
            }
            if (pref.getSickLeaveAlias() != null) sickLeaveAlias = pref.getSickLeaveAlias();
            else {
                sickLeaveAlias = Constants.LeaveTypeNameConstant.SICK_LEAVE;
            }
        } else {
            timeOffAlias = Constants.LeaveTypeNameConstant.TIME_OFF;
            sickLeaveAlias = Constants.LeaveTypeNameConstant.SICK_LEAVE;
        }

        List<UserListResponse> userList = userRepository.getEmailAccountIdFirstMiddleAndLastNameByOrgId(attendanceRequest.getOrgId());
        Map<Long, UserAccountAttendanceDetails> userAttendanceMap = userList.stream()
                .filter(userListResponse -> attendanceRequest.getAccountIds().contains(userListResponse.getAccountId()))
                .map(userListResponse -> UserAccountAttendanceDetails.builder()
                        .firstName(userListResponse.getFirstName())
                        .lastName(userListResponse.getLastName())
                        .email(userListResponse.getEmail())
                        .accountId(userListResponse.getAccountId())
                        .isActive(userListResponse.getIsActive())
                        .attendanceRecord(new ArrayList<>())
                        .build()
                ).collect(Collectors.toMap(UserAccountAttendanceDetails::getAccountId, userAccountAttendanceDetails -> userAccountAttendanceDetails));

        List<Long> targetedAccountIds = new ArrayList<>(userAttendanceMap.keySet());
        Map<LocalDate, HolidayOffDay> holidayDateListMap = entityPreferenceRepository.fetchAllHolidayDatesByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, attendanceRequest.getOrgId())
                .orElse(new ArrayList<>()).parallelStream().collect(Collectors.toMap(HolidayOffDay::getDate, holidayOffDay -> holidayOffDay));
        List<LocalDate> holidayDateList = new ArrayList<>(holidayDateListMap.keySet());

        //setting default offDays and expected work minutes as of org
        List<Integer> offDays = orgPreference.getFirst();
        Integer expectedWorkMins = orgPreference.getSecond();

        //creating a map of entity filters to get timesheets, according to filter
        Map<Integer, Long> entityFilters = new HashMap<>();
        entityFilters.put(Constants.EntityTypes.ORG, attendanceRequest.getOrgId());

        if (attendanceRequest.getTeamId() != null) {
            entityFilters.put(Constants.EntityTypes.TEAM, attendanceRequest.getTeamId());
            //setting off days and expected work minutes if present in team preference
            if (!teamPreference.isEmpty() && teamPreference.containsKey(attendanceRequest.getTeamId())) {
                offDays = teamPreference.get(attendanceRequest.getTeamId()).getFirst() != null ? teamPreference.get(attendanceRequest.getTeamId()).getFirst() : orgPreference.getFirst();
                expectedWorkMins = teamPreference.get(attendanceRequest.getTeamId()).getSecond() != null ? teamPreference.get(attendanceRequest.getTeamId()).getSecond() : orgPreference.getSecond();
            }
        }
        if (attendanceRequest.getProjectId() != null) {
            entityFilters.put(Constants.EntityTypes.PROJECT, attendanceRequest.getProjectId());
        }

        List<TimeSheet> timesheetEntries = getTimesheetListForEntity(entityFilters, attendanceRequest.getAccountIds(), attendanceRequest.getStartDate(), attendanceRequest.getEndDate());

        Map<Long, List<TimeSheet>> timesheetMapWithAccountId = timesheetEntries.stream().collect(Collectors.groupingBy(TimeSheet::getAccountId));
        // Construct response DTO
        AttendanceResponseV2Dto response = AttendanceResponseV2Dto.builder()
                .orgId(attendanceRequest.getOrgId())
                .projectId(attendanceRequest.getProjectId())
                .teamId(attendanceRequest.getTeamId())
                .build();

        // Initialize counters for holidays and off days
        AtomicInteger totalHolidays = new AtomicInteger();
        AtomicInteger totalOffDays = new AtomicInteger();

        LocalDate startDate = attendanceRequest.getStartDate();

        // Initialize map to store attendance data for each date, account, and attendance type
        // Process each timesheet entry

        // filled the AttendanceRecordDto for every timesheet entry associated to accountId.
        for (Long accountId : timesheetMapWithAccountId.keySet()) {
            List<TimeSheet> timeSheetList = timesheetMapWithAccountId.get(accountId);
            UserAccountAttendanceDetails userAccountAttendanceDetails = userAttendanceMap.get(accountId);
            List<AttendanceRecordDto> recordDataListByAccountId = new ArrayList<>();
            if (!timeSheetList.isEmpty() && userAccountAttendanceDetails != null) {
                AtomicInteger expectedWorkMinsCopy = new AtomicInteger(expectedWorkMins);
                List<Integer> finalOffDays = offDays;
                Set<LocalDate> processedDates = new HashSet<>();
                timeSheetList.forEach(
                        timeSheet -> {
                            LocalDate date = timeSheet.getNewEffortDate();
                            String attendanceType = "";
                            Integer typeId = 0;
                            Integer minsWorkedCopy = timeSheet.getNewEffort();
                            String description = "";

                            if (!processedDates.contains(date)) {
                                boolean isHoliday = false;
                                boolean isOffDay = false;
                                //checking for off days
                                if ((teamPreference.containsKey(timeSheet.getTeamId()) && teamPreference.get(timeSheet.getTeamId()).getFirst().contains(date.getDayOfWeek().getValue()))
                                        || finalOffDays.contains(date.getDayOfWeek().getValue())) {
                                    isOffDay = true;
                                }
                                if (Objects.equals(timeSheet.getEntityTypeId(), Constants.EntityTypes.HOLIDAY)) {
                                    isHoliday = true;
                                }
                                if (isHoliday) {
                                    attendanceType = Constants.AttendanceTypeEnum.HOLIDAY.getAttendanceType();
                                    typeId = Constants.AttendanceTypeEnum.HOLIDAY.getAttendanceTypeId();
                                    description = timeSheet.getEntityTitle();
                                    totalHolidays.getAndIncrement();
                                    minsWorkedCopy = 0;
                                    expectedWorkMinsCopy.set(0);
                                }
                                else if (isOffDay) {
                                    attendanceType = Constants.AttendanceTypeEnum.OFF_DAY.getAttendanceType();
                                    typeId = Constants.AttendanceTypeEnum.OFF_DAY.getAttendanceTypeId();
                                    description = timeSheet.getEntityTitle();
                                    totalOffDays.getAndIncrement();
                                    expectedWorkMinsCopy.set(0);
                                }
                            }
                            AttendanceRecordDto attendance = AttendanceRecordDto.builder()
                                    .dateTime(date)
                                    .expectedWorkMins(expectedWorkMinsCopy.get())
                                    .minsWorked(minsWorkedCopy)
                                    .typeName(!attendanceType.isEmpty() ? new ArrayList<>(Collections.singleton(attendanceType)) : null)
                                    .typeId(typeId > 0 ? List.of(typeId) : null)
                                    .description(description.isEmpty() ? null : new ArrayList<>(Collections.singleton(description)))
                                    .build();
                            recordDataListByAccountId.add(attendance);
                            processedDates.add(date);
                        }
                );
                totalHolidays.getAndAdd(totalHolidays.get());
                totalOffDays.getAndAdd(totalOffDays.get());
                userAccountAttendanceDetails.setAttendanceRecord(recordDataListByAccountId);
            }
        }
        for (UserAccountAttendanceDetails userAccountAttendanceDetails : userAttendanceMap.values()) {
            startDate = attendanceRequest.getStartDate();
            List<AttendanceRecordDto> recordList = new ArrayList<>();
            List<LocalDate> processesTimesheetDates = new ArrayList<>();
            Map<LocalDate, Optional<AttendanceRecordDto>> dateAttendaceMap = userAccountAttendanceDetails.getAttendanceRecord().stream()
                    .collect(Collectors.groupingBy(AttendanceRecordDto::getDateTime,
                            Collectors.reducing((r1, r2) -> {
                                AttendanceRecordDto copyDto = new AttendanceRecordDto();
                                copyDto.setDateTime(r1.getDateTime());
                                copyDto.setExpectedWorkMins(r1.getExpectedWorkMins());
                                copyDto.setMinsWorked(r1.getMinsWorked() + r2.getMinsWorked());

                                List<String> mergedNames = new ArrayList<>();
                                if (r1.getTypeName() != null) mergedNames.addAll(r1.getTypeName());
                                if (r2.getTypeName() != null) mergedNames.addAll(r2.getTypeName());
                                copyDto.setTypeName(mergedNames);

                                List<Integer> mergedIds = new ArrayList<>();
                                if (r1.getTypeId() != null) mergedIds.addAll(r1.getTypeId());
                                if (r2.getTypeId() != null) mergedIds.addAll(r2.getTypeId());
                                copyDto.setTypeId(mergedIds);
                                return copyDto;
                            })));
            Integer finalExpectedWorkMins = expectedWorkMins;
            recordList = dateAttendaceMap.values().stream().map(attendanceRecordDto -> {
                                AttendanceRecordDto dto = attendanceRecordDto.get();
                                String attendanceType = null;
                                Integer typeId = null;
                                List<String> typeNames = new ArrayList<>();
                                if (dto.getTypeName() != null) {
                                    typeNames.addAll(dto.getTypeName());
                                }
                                dto.setTypeName(typeNames);

                                List<Integer> typeIds = new ArrayList<>();
                                if (dto.getTypeId() != null) {
                                    typeIds.addAll(dto.getTypeId());
                                }
                                dto.setTypeId(typeIds);
                                // Calculate the percentage of expected work hours filled
                                if (dto.getExpectedWorkMins() <= 0) dto.setExpectedWorkMins(finalExpectedWorkMins);
                                double percentageWorked = (double) dto.getMinsWorked() / dto.getExpectedWorkMins() * 100;

                                // Determine an attendance type based on percentage
                                if (percentageWorked >= 75) {
                                    attendanceType = Constants.AttendanceTypeEnum.PRESENT.getAttendanceType();
                                    typeId = Constants.AttendanceTypeEnum.PRESENT.getAttendanceTypeId();
                                } else if (percentageWorked > 0 && percentageWorked < 50) {
                                    attendanceType = Constants.AttendanceTypeEnum.PARTIAL_LESS_THAN_50.getAttendanceType();
                                    typeId = Constants.AttendanceTypeEnum.PARTIAL_LESS_THAN_50.getAttendanceTypeId();
                                } else if (percentageWorked >= 50) {
                                    attendanceType = Constants.AttendanceTypeEnum.PARTIAL_MORE_THAN_50.getAttendanceType();
                                    typeId = Constants.AttendanceTypeEnum.PARTIAL_MORE_THAN_50.getAttendanceTypeId();
                                }
                                else {
                                    attendanceType = Constants.AttendanceTypeEnum.PARTIAL.getAttendanceType();
                                    typeId = Constants.AttendanceTypeEnum.PARTIAL.getAttendanceTypeId();
                                }
                                dto.getTypeId().add(typeId);
                                dto.getTypeName().add(attendanceType);
                                processesTimesheetDates.add(dto.getDateTime());
                                if (dto.getDescription() == null) {
                                    dto.setDescription(new ArrayList<>());
                                }
                                return dto;
                            }
                    )
                    .collect(Collectors.toList());

            // processing remaining dates which do not have timesheet entries.
            while (!startDate.isAfter(attendanceRequest.getEndDate())) {
                if (!processesTimesheetDates.contains(startDate)) {
                    AttendanceRecordDto recordDto = new AttendanceRecordDto();
                    recordDto.setDateTime(startDate);
                    recordDto.setTypeName(new ArrayList<>());
                    recordDto.setTypeId(new ArrayList<>());
                    recordDto.setDescription(new ArrayList<>());
                    if (offDays.contains(startDate.getDayOfWeek().getValue())) {
                        recordDto.getTypeName().add(Constants.AttendanceTypeEnum.OFF_DAY.getAttendanceType());
                        recordDto.getTypeId().add(Constants.AttendanceTypeEnum.OFF_DAY.getAttendanceTypeId());
                        recordDto.setExpectedWorkMins(0);
                        totalOffDays.getAndIncrement();
                    }
                    else if (!holidayDateList.isEmpty() && holidayDateList.contains(startDate)) {
                        recordDto.getTypeName().add(Constants.AttendanceTypeEnum.HOLIDAY.getAttendanceType());
                        recordDto.getTypeId().add(Constants.AttendanceTypeEnum.HOLIDAY.getAttendanceTypeId());
                        recordDto.setExpectedWorkMins(0);
                        recordDto.getDescription().add(holidayDateListMap.get(startDate).getDescription());
                        totalHolidays.getAndIncrement();
                    }
                    else if (startDate.isEqual(LocalDate.now()) || startDate.isAfter(LocalDate.now())) {
                        recordDto.getTypeName().add(Constants.AttendanceTypeEnum.EMPTY.getAttendanceType());
                        recordDto.getTypeId().add(Constants.AttendanceTypeEnum.EMPTY.getAttendanceTypeId());
                        recordDto.setExpectedWorkMins(expectedWorkMins);
                    } else {
                        recordDto.getTypeName().add(Constants.AttendanceTypeEnum.ABSENT.getAttendanceType());
                        recordDto.getTypeId().add(Constants.AttendanceTypeEnum.ABSENT.getAttendanceTypeId());
                        recordDto.setExpectedWorkMins(expectedWorkMins);
                    }
                    recordDto.setMinsWorked(0);
                    recordList.add(recordDto);
                }
                startDate = startDate.plusDays(1);
            }
            userAccountAttendanceDetails.setAttendanceRecord(recordList);
        }

        Map<Long, Map<LocalDate, UserOffDaysResponse>> leavesMap = usersLeavesMap(targetedAccountIds, attendanceRequest.getStartDate(), attendanceRequest.getEndDate(), sickLeaveAlias, timeOffAlias);
        if(!leavesMap.isEmpty()) {
            for (UserAccountAttendanceDetails accountAttendanceDetails : userAttendanceMap.values()) {
                Map<LocalDate, UserOffDaysResponse> leaveData = leavesMap.get(accountAttendanceDetails.getAccountId());
                accountAttendanceDetails.getAttendanceRecord().forEach(recordDto -> {
                    if (leaveData.containsKey(recordDto.getDateTime())) {
                        UserOffDaysResponse leaveResponse = leaveData.get(recordDto.getDateTime());
                        if (recordDto.getTypeName().size() == 1 && (recordDto.getTypeName().contains(Constants.AttendanceTypeEnum.PARTIAL.getAttendanceType())
                                || recordDto.getTypeName().contains(Constants.AttendanceTypeEnum.EMPTY.getAttendanceType()))) {
                            recordDto.setTypeName(List.of(leaveResponse.getLeaveTypeName()));
                            if (leaveResponse.getLeaveTypeId() == 1) {
                                recordDto.setTypeId(List.of(Constants.AttendanceTypeEnum.TIME_OFF_LEAVE.getAttendanceTypeId()));
                            } else if (leaveResponse.getLeaveTypeId() == 2) {
                                recordDto.setTypeId(List.of(Constants.AttendanceTypeEnum.SICK_LEAVE.getAttendanceTypeId()));
                            }
                            recordDto.setDescription(List.of(leaveResponse.getLeaveDescription()));
                        } else {
                            recordDto.getTypeName().add(leaveResponse.getLeaveTypeName());
                            if (leaveResponse.getLeaveTypeId() == 1) {
                                recordDto.getTypeId().add(Constants.AttendanceTypeEnum.TIME_OFF_LEAVE.getAttendanceTypeId());
                            } else if (leaveResponse.getLeaveTypeId() == 2) {
                                recordDto.getTypeId().add(Constants.AttendanceTypeEnum.SICK_LEAVE.getAttendanceTypeId());
                            }
                            if (leaveResponse.getLeaveDescription() != null) {
                                if (recordDto.getDescription() != null) {
                                    recordDto.getDescription().add(leaveResponse.getLeaveDescription());
                                } else {
                                    recordDto.setDescription(List.of(leaveResponse.getLeaveDescription()));
                                }
                            }
                        }
                    }
                });
            }
        }
        AtomicReference<Integer> totalHolidaysFromData = new AtomicReference<>(0);
        AtomicReference<Integer> totalOffDaysFromData = new AtomicReference<>(0);
        //sorting ascending date wise
        for (UserAccountAttendanceDetails accountAttendanceDetails : userAttendanceMap.values()) {
            userAttendanceMap.get(accountAttendanceDetails.getAccountId())
                    .setAttendanceRecord(accountAttendanceDetails.getAttendanceRecord().stream()
                            .sorted(Comparator.comparing(AttendanceRecordDto::getDateTime)).collect(Collectors.toList()));

            accountAttendanceDetails.getAttendanceRecord().forEach(attendanceRecordDto -> {
                if (attendanceRecordDto.getTypeId().contains(Constants.AttendanceTypeEnum.OFF_DAY.getAttendanceTypeId()))
                    totalOffDaysFromData.getAndSet(totalOffDaysFromData.get() + 1);
                if (attendanceRecordDto.getTypeId().contains(Constants.AttendanceTypeEnum.HOLIDAY.getAttendanceTypeId()))
                    totalHolidaysFromData.getAndSet(totalHolidaysFromData.get() + 1);
            });
        }

        response.setUsersDetails(new ArrayList<>(userAttendanceMap.values()));
        response.setTotalHolidays(totalHolidaysFromData.get());
        response.setTotalOffDays(totalOffDaysFromData.get());

        return response;
    }

    public List<OffDaysResponse> getOffDaysForAttendance(LeaveAttendanceRequest request, String accountIds, String timeZone) throws IllegalAccessException {
        List<OffDaysResponse> responseList = new ArrayList<>();
        List<Long> headerAccountIdList = CommonUtils.convertToLongList(accountIds);
        validateLeaveAttendanceAccess(request, headerAccountIdList);

        Set<Long> eligibleAccountIds = fetchEligibleAccountIds(request);
        if (eligibleAccountIds.isEmpty()) {
            return responseList;
        }
        List<Long> targetAccountIds = resolveTargetAccountIds(request.getAccountIdList(), eligibleAccountIds);
        if (targetAccountIds == null || targetAccountIds.isEmpty()) {
            return responseList;
        }

        List<EmailFirstLastAccountIdIsActive> userDetailsList = userAccountRepository.getEmailFirstNameLastNameAccountIdIsActiveByAccountIdIn(targetAccountIds);
        Map<Long, EmailFirstLastAccountIdIsActive> userMap = userDetailsList.stream()
                .collect(Collectors.toMap(EmailFirstLastAccountIdIsActive::getAccountId, temp -> temp));

        Long orgId = userAccountRepository.findOrgIdByAccountIdAndIsActive(targetAccountIds.get(0), true).getOrgId();
        Optional<EntityPreference> optionalPref = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, orgId);
        String timeOffAlias = Constants.LeaveTypeNameConstant.TIME_OFF;
        String sickLeaveAlias = Constants.LeaveTypeNameConstant.SICK_LEAVE;

        if (optionalPref.isPresent()) {
            EntityPreference pref = optionalPref.get();
            if (pref.getTimeOffAlias() != null) timeOffAlias = pref.getTimeOffAlias();
            if (pref.getSickLeaveAlias() != null) sickLeaveAlias = pref.getSickLeaveAlias();
        }
        List<HolidayResponse> holidayDateList = holidayOffDayRepository.findCustomHolidayResponseByEntityPreferenceIdAndIsActive(orgId, true);
        Pair<List<Integer>, Integer> orgPreference = entityPreferenceService.getOfficeMinutesAndOffDaysFromOrgPreferenceOrDefault(orgId);
        List<Integer> offDays = orgPreference.getFirst();

        Map<LocalDate, HolidayResponse> holidaysMap = holidayDateList.stream()
                .filter(holiday -> !holiday.getDate().isBefore(request.getFromDate()) && !holiday.getDate().isAfter(request.getToDate()))
                .collect(Collectors.toMap(HolidayResponse::getDate, holiday -> holiday));

        LocalDate fromDate = request.getFromDate();
        LocalDate toDate = request.getToDate();

        for (Long accountId : targetAccountIds) {
            EmailFirstLastAccountIdIsActive user = userMap.get(accountId);
            OffDaysResponse userResponse = OffDaysResponse.builder()
                    .accountId(accountId)
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .isActive(user.getIsActive())
                    .startDate(request.getFromDate())
                    .endDate(request.getToDate())
                    .userOffDaysResponses(new ArrayList<>())
                    .build();

            List<LeaveApplication> leaveApplications = leaveApplicationRepository.findByAccountIdAndOverlappingDateRange(
                    accountId,
                    fromDate,
                    toDate,
                    List.of(Constants.LeaveApplicationStatusIds.CONSUMED_LEAVE_APPLICATION_STATUS_ID, Constants.LeaveApplicationStatusIds.APPROVED_LEAVE_APPLICATION_STATUS_ID)
            );

            if (!leaveApplications.isEmpty()) {
                List<UserOffDaysResponse> leaveDetails = new ArrayList<>();

                for (LeaveApplication leave : leaveApplications) {
                    LocalDate leaveStart = leave.getFromDate();
                    LocalDate leaveEnd = leave.getToDate();

                    // Calculate overlap range between leave and request
                    LocalDate overlapStart = leaveStart.isBefore(fromDate) ? fromDate : leaveStart;
                    LocalDate overlapEnd = leaveEnd.isAfter(toDate) ? toDate : leaveEnd;

                    while (!overlapStart.isAfter(overlapEnd)) {
                        UserOffDaysResponse userLeave = UserOffDaysResponse.builder()
                                .date(overlapStart)
                                .leaveTypeId((int) leave.getLeaveTypeId())
                                .leaveTypeName(leave.getLeaveTypeId() == 1 ? timeOffAlias : sickLeaveAlias)
                                .isHalfDayLeave(leave.getIsLeaveForHalfDay() != null ? leave.getIsLeaveForHalfDay() : false)
                                .leaveApplicationId(leave.getLeaveApplicationId())
                                .build();
                        leaveDetails.add(userLeave);
                        overlapStart = overlapStart.plusDays(1);
                    }
                }

                if (!leaveDetails.isEmpty()) {
                    leaveDetails.sort(Comparator.comparing(UserOffDaysResponse::getDate));
                    userResponse.setUserOffDaysResponses(leaveDetails);
                }
            }
            responseList.add(userResponse);
        }

        LocalDate startDate = request.getFromDate();
        List<UserOffDaysResponse> userOffDaysResponseList = new ArrayList<>();
        while(!startDate.isAfter(request.getToDate())){
            if(holidaysMap.containsKey(startDate)){
                HolidayResponse holidayResponse = holidaysMap.get(startDate);
                UserOffDaysResponse userOffDaysResponse = UserOffDaysResponse.builder()
                        .date(startDate)
                        .leaveApplicationId(holidayResponse.getHolidayId())
                        .leaveTypeId(Constants.AttendanceTypeEnum.HOLIDAY.getAttendanceTypeId())
                        .leaveTypeName(Constants.AttendanceTypeEnum.HOLIDAY.getAttendanceType())
                        .leaveDescription(holidayResponse.getDescription())
                        .isHalfDayLeave(false)
                        .build();
                userOffDaysResponseList.add(userOffDaysResponse);
            }
            if(offDays.contains(startDate.getDayOfWeek().getValue())){
                UserOffDaysResponse offDaysResponse = UserOffDaysResponse.builder()
                        .date(startDate)
                        .leaveTypeId(Constants.AttendanceTypeEnum.OFF_DAY.getAttendanceTypeId())
                        .leaveTypeName(Constants.AttendanceTypeEnum.OFF_DAY.getAttendanceType())
                        .leaveDescription("Org. Off Day")
                        .isHalfDayLeave(false)
                        .build();
                userOffDaysResponseList.add(offDaysResponse);
            }
            startDate = startDate.plusDays(1);
        }
        return responseList.stream().peek(off -> off.getUserOffDaysResponses().addAll(userOffDaysResponseList)).sorted(Comparator
                .comparing(OffDaysResponse::getFirstName, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(OffDaysResponse::getLastName, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(OffDaysResponse::getAccountId)).collect(Collectors.toList());
    }

    private Map<Long, Map<LocalDate, UserOffDaysResponse>> usersLeavesMap(List<Long> targetAccountIds, LocalDate fromDate, LocalDate toDate, String sickLeaveAlias, String timeOffAlias) {

        Map<Long, Map<LocalDate, UserOffDaysResponse>> responseMap = new HashMap<>();
        List<LeaveApplication> leaveApplications = leaveApplicationRepository.findByAccountIdInAndOverlappingDateRange(
                targetAccountIds, fromDate, toDate,
                List.of(Constants.LeaveApplicationStatusIds.CONSUMED_LEAVE_APPLICATION_STATUS_ID, Constants.LeaveApplicationStatusIds.APPROVED_LEAVE_APPLICATION_STATUS_ID)
        );
        Map<Long, List<LeaveApplication>> leaveApplicationMap = leaveApplications.parallelStream().collect(Collectors.groupingBy(LeaveApplication::getAccountId));
        for (Long accountId : targetAccountIds) {
            List<LeaveApplication> leaveApplicationListDb = leaveApplicationMap.get(accountId);
            Map<LocalDate, UserOffDaysResponse> offDaysResponseMap = new HashMap<>();

            if (leaveApplicationListDb != null && !leaveApplicationListDb.isEmpty()) {
                for (LeaveApplication leave : leaveApplicationListDb) {
                    LocalDate leaveStart = leave.getFromDate();
                    LocalDate leaveEnd = leave.getToDate();

                    // Calculate overlap range between leave and request
                    LocalDate overlapStart = leaveStart.isBefore(fromDate) ? fromDate : leaveStart;
                    LocalDate overlapEnd = leaveEnd.isAfter(toDate) ? toDate : leaveEnd;

                    while (!overlapStart.isAfter(overlapEnd)) {
                        UserOffDaysResponse userLeave = UserOffDaysResponse.builder()
                                .date(overlapStart)
                                .leaveTypeId((int) leave.getLeaveTypeId())
                                .leaveTypeName(leave.getLeaveTypeId() == 1 ? timeOffAlias : sickLeaveAlias)
                                .isHalfDayLeave(leave.getIsLeaveForHalfDay())
                                .leaveApplicationId(leave.getLeaveApplicationId())
                                .leaveDescription(leave.getLeaveReason())
                                .build();
                        offDaysResponseMap.put(overlapStart, userLeave);
                        overlapStart = overlapStart.plusDays(1);
                    }
                }
                if (!offDaysResponseMap.isEmpty()) {
                    responseMap.put(accountId, offDaysResponseMap);
                }
            }
        }
        return responseMap;
    }
}
