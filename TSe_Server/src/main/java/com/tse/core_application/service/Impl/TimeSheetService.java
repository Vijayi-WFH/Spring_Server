package com.tse.core_application.service.Impl;

import com.opencsv.CSVWriter;
import com.tse.core_application.constants.ControllerConstants;
import com.tse.core_application.custom.model.EmailFirstLastAccountId;
import com.tse.core_application.custom.model.timesheet.EffortOnEntity;
import com.tse.core_application.custom.model.timesheet.TotalEffort;
import com.tse.core_application.dto.*;
import com.tse.core_application.exception.HttpMessageNotReadableException;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.filters.JwtRequestFilter;
import com.tse.core_application.model.*;
import com.tse.core_application.repository.*;
import com.tse.core_application.utils.CommonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.StringWriter;
import java.time.LocalDate;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TimeSheetService {

    @Autowired
    private TimeSheetRepository timeSheetRepository;
    @Autowired
    private AccessDomainRepository accessDomainRepository;
    @Autowired
    private JwtRequestFilter jwtRequestFilter;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private TeamService teamService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private UserFeatureAccessRepository userFeatureAccessRepository;

    @Value("${tseHr.application.root.path}")
    private String tseHrBaseUrl;

    public List<TimeSheet> saveAllTimeSheetRecords(List<TimeSheet> tsRecords){
        timeSheetRepository.saveAll(tsRecords);
        return tsRecords;
    }

    public void filterTimeSheetForRoles(TimeSheetRequest timeSheetRequest,
                                        List<TimeSheetResponse> timeSheetList,
                                        String accountIds) {
        List<Long> headerAccountIds = jwtRequestFilter.getAccountIdsFromHeader(accountIds);
        if (timeSheetRequest.getTeamId() != null) {
            Team teamDb = teamRepository.findByTeamId(timeSheetRequest.getTeamId());
            Long orgId = teamDb.getFkOrgId().getOrgId();
            Long projectId = teamDb.getFkProjectId().getProjectId();
            boolean hasOrgAccess = userFeatureAccessRepository
                    .existsByEntityTypeIdAndEntityIdAndUserAccountIdAndActionIdAndIsDeletedFalse(
                            Constants.EntityTypes.ORG, orgId, headerAccountIds, Constants.ActionId.VIEW_TIMESHEET);
            boolean hasProjectAccess = userFeatureAccessRepository
                    .existsByEntityTypeIdAndEntityIdAndUserAccountIdAndActionIdAndIsDeletedFalse(
                            Constants.EntityTypes.PROJECT, projectId, headerAccountIds, Constants.ActionId.VIEW_TIMESHEET);
            boolean hasTeamAccess = userFeatureAccessRepository
                    .existsByEntityTypeIdAndEntityIdAndUserAccountIdAndActionIdAndIsDeletedFalse(
                            Constants.EntityTypes.TEAM, timeSheetRequest.getTeamId(), headerAccountIds, Constants.ActionId.VIEW_TIMESHEET);
            if (hasOrgAccess || hasProjectAccess || hasTeamAccess) {
                return;
            }
            List<Long> teamTaskViewTeamIds = accessDomainRepository.findTeamIdsByAccountIdsAndActionId(headerAccountIds, Constants.EntityTypes.TEAM, Constants.ActionId.TEAM_TASK_VIEW);
            timeSheetList.removeIf(timeSheet -> !teamTaskViewTeamIds.contains(timeSheetRequest.getTeamId()) &&
                    (timeSheet.getAccount_Id() == null || !headerAccountIds.contains(timeSheet.getAccount_Id())));
        }
    }

    public void removeTimeSheetForLeaveCancellation (Long leaveApplicationId) {
        timeSheetRepository.deleteByEntityTypeIdAndEntityId(Constants.EntityTypes.LEAVE, leaveApplicationId);
    }

    public TimeSheetResponseWithHourDistribution findAndSetHourDistribution(List<TimeSheetResponse> timeSheetResponses) {
        Map<String, EntityTypeHourDistribution> entityTypeMap = initializeEntityTypeMap();
        Map<Long, OrgHourDistribution> orgMap = new HashMap<>();

        for (TimeSheetResponse response : timeSheetResponses) {
            List<TotalEffort> efforts = response.getEfforts();
            if (efforts != null) {
                for (TotalEffort totalEffort : efforts) {
                    List<EffortOnEntity> effortOnEntities = totalEffort.getEffortOnEntityList();
                    if (effortOnEntities != null) {
                        for (EffortOnEntity effort : effortOnEntities) {
                            // Process Entity Type Distribution
                            if (effort.getEntityTypeId() == Constants.EntityTypes.TASK) {
                                if (Boolean.TRUE.equals(effort.getIsBug())) {
                                    addEffortToEntityType(entityTypeMap.get(Constants.HourDistributionEntityTypes.BUG), effort);
                                } else {
                                    addEffortToEntityType(entityTypeMap.get(Constants.HourDistributionEntityTypes.WORK_ITEM), effort);
                                }
                            } else if (effort.getEntityTypeId() == Constants.EntityTypes.MEETING) {
                                addEffortToEntityType(entityTypeMap.get(Constants.HourDistributionEntityTypes.MEETING), effort);
                            } else {
                                addEffortToEntityType(entityTypeMap.get(Constants.HourDistributionEntityTypes.OTHER_ENTITY), effort);
                            }

                            // Process of Organization Distribution
                            Long orgId = effort.getOrgId();
                            if (orgId != null) {
                                OrgHourDistribution orgHour = orgMap.computeIfAbsent(orgId, id -> new OrgHourDistribution());
                                orgHour.setOrgId(orgId);
                                orgHour.setTotalBurnedEffortMin(orgHour.getTotalBurnedEffortMin() + effort.getEntityEffortMins());
                                orgHour.setTotalEarnedEffortMin(orgHour.getTotalEarnedEffortMin() + effort.getEntityEarnedTime());
                            }
                        }
                    }
                }
            }
        }

        // Calculate Percentages for Entity Types
        calculatePercentages(entityTypeMap);

        // Set Organization Names and Calculate Percentages
        for (Map.Entry<Long, OrgHourDistribution> entry : orgMap.entrySet()) {
            Long orgId = entry.getKey();
            OrgHourDistribution orgHour = entry.getValue();
            orgHour.setOrgName(getOrgNameById(orgId));
        }
        calculatePercentages(orgMap);

        // Prepare Response
        TimeSheetResponseWithHourDistribution response = new TimeSheetResponseWithHourDistribution();
        response.setTimeSheetResponsesList(timeSheetResponses);
        response.setEntityTypeHourDistributionsList(new ArrayList<>(entityTypeMap.values()));
        response.setOrgHourDistributionList(new ArrayList<>(orgMap.values()));

        return response;
    }

    private Map<String, EntityTypeHourDistribution> initializeEntityTypeMap() {
        Map<String, EntityTypeHourDistribution> entityTypeMap = new LinkedHashMap<>();
        entityTypeMap.put(Constants.HourDistributionEntityTypes.WORK_ITEM, new EntityTypeHourDistribution(Constants.HourDistributionEntityTypes.WORK_ITEM, 0, 0, 0, 0));
        entityTypeMap.put(Constants.HourDistributionEntityTypes.BUG, new EntityTypeHourDistribution(Constants.HourDistributionEntityTypes.BUG, 0, 0, 0, 0));
        entityTypeMap.put(Constants.HourDistributionEntityTypes.MEETING, new EntityTypeHourDistribution(Constants.HourDistributionEntityTypes.MEETING, 0, 0, 0, 0));
        entityTypeMap.put(Constants.HourDistributionEntityTypes.OTHER_ENTITY, new EntityTypeHourDistribution(Constants.HourDistributionEntityTypes.OTHER_ENTITY, 0, 0, 0, 0));
        return entityTypeMap;
    }

    private void addEffortToEntityType(EntityTypeHourDistribution entityType, EffortOnEntity effort) {
        if (entityType != null) {
            entityType.setTotalBurnedEffortMin(
                    entityType.getTotalBurnedEffortMin() + effort.getEntityEffortMins());
            entityType.setTotalEarnedEffortMin(
                    entityType.getTotalEarnedEffortMin() + effort.getEntityEarnedTime());
        }
    }

    private void calculatePercentages(Map<?, ? extends HourDistribution> map) {
        int totalBurnedEffort = map.values().stream()
                .mapToInt(HourDistribution::getTotalBurnedEffortMin)
                .sum();
        int totalEarnedEffort = map.values().stream()
                .mapToInt(HourDistribution::getTotalEarnedEffortMin)
                .sum();

        if (totalBurnedEffort == 0) {
            // If no burned effort, set all percentages to 0
            map.values().forEach(d -> d.setPercentageOfBurnedEffort(0));
        } else {
            // Calculate raw percentages for burned effort
            List<PercentageHolder> burnedEffortList = map.values().stream()
                    .map(d -> new PercentageHolder(d, (double) d.getTotalBurnedEffortMin() / totalBurnedEffort * 100))
                    .collect(Collectors.toList());

            // Adjust burned percentages to sum to 100
            adjustPercentagesToSum100(burnedEffortList, HourDistribution::setPercentageOfBurnedEffort);
        }

        if (totalEarnedEffort == 0) {
            // If no earned effort, set all percentages to 0
            map.values().forEach(d -> d.setPercentageOfEarnedEffort(0));
        } else {
            // Calculate raw percentages for earned effort
            List<PercentageHolder> earnedEffortList = map.values().stream()
                    .map(d -> new PercentageHolder(d, (double) d.getTotalEarnedEffortMin() / totalEarnedEffort * 100))
                    .collect(Collectors.toList());

            // Adjust earned percentages to sum to 100
            adjustPercentagesToSum100(earnedEffortList, HourDistribution::setPercentageOfEarnedEffort);
        }
    }

    private void adjustPercentagesToSum100(
            List<PercentageHolder> percentageList,
            BiConsumer<HourDistribution, Integer> setter
    ) {
        // Separate integer and fractional parts
        int totalIntegerSum = percentageList.stream()
                .mapToInt(ph -> (int) ph.percentage)
                .sum();

        int difference = 100 - totalIntegerSum;

        // Sort by fractional part in descending order
        percentageList.sort(Comparator.comparingDouble(ph -> -(ph.percentage % 1))); // Descending sort

        // Limit iterations to the size of the percentageList
        int iterations = Math.min(difference, percentageList.size());
        for (int i = 0; i < iterations; i++) {
            PercentageHolder ph = percentageList.get(i);
            ph.integerPart++; // Increment the integer part of the largest fractional values
        }

        // Set the final adjusted percentages
        for (PercentageHolder ph : percentageList) {
            setter.accept(ph.hourDistribution, ph.integerPart);
        }
    }


    // Helper class to hold percentage data
    private static class PercentageHolder {
        HourDistribution hourDistribution;
        double percentage;
        int integerPart;

        PercentageHolder(HourDistribution hourDistribution, double percentage) {
            this.hourDistribution = hourDistribution;
            this.percentage = percentage;
            this.integerPart = (int) percentage;
        }
    }

    private String getOrgNameById(Long orgId) {
        return organizationRepository.findById(orgId)
                .map(Organization::getOrganizationName)
                .orElse("Unknown");
    }

    public Long findOrgIdByEntityTypeIdAndEntityId (Integer entityTypeId, Long entityId) {
        Long orgId = null;
        if (Objects.equals(Constants.EntityTypes.ORG, entityTypeId)) {
            orgId = entityId;
        }
        else if(Objects.equals(Constants.EntityTypes.PROJECT, entityTypeId)) {
            orgId = projectRepository.findByProjectId(entityId).getOrgId();
        }
        else if(Objects.equals(Constants.EntityTypes.TEAM, entityTypeId)) {
            orgId = teamRepository.findFkOrgIdOrgIdByTeamId(entityId);
        }
        else {
            throw new ValidationFailedException("EntityType is not valid");
        }
        return orgId;
    }

    public byte[] exportTimesheetToCSV(TimesheetExportToCSVRequest request, String accountIds, String userId, String timeZone) {
        if (request.getToDate().isBefore(request.getFromDate())) {
            throw new ValidationFailedException("To date can't be before from date");
        }
        List<Long> accountIdList = CommonUtils.convertToLongList(accountIds);
        Long orgId = findOrgIdByEntityTypeIdAndEntityId(request.getEntityTypeId(), request.getEntityId());

        validateAccess(accountIdList, orgId, request);

        // null-safe fetch
        List<TimeSheetResponse> timeSheetResponses = Optional.ofNullable(
                fetchTimeSheetData(request, accountIdList, userId, timeZone)
        ).orElse(Collections.emptyList());

        filterTimeSheetForRoles(prepareTimeSheetRequest(request), timeSheetResponses, accountIds);

        Map<Long, String> accountIdToName  = getAccountIdToUserNameMap(timeSheetResponses, accountIdList);
        Map<Long, String> accountIdToEmail = getAccountIdToEmailMap(timeSheetResponses, accountIdList); // NEW
        Map<Long, String> teamIdToName     = fetchTeamNames(timeSheetResponses);
        Map<Long, String> projectIdToName  = fetchProjectNames(timeSheetResponses);
        Map<Long, String> orgIdToName      = fetchOrgNames(timeSheetResponses);

        Map<String, TimesheetCSVRow> csvRowMap = buildCSVRows(request, timeSheetResponses, accountIdToName);

        // Collect and sort by Name asc, then Email asc
        List<TimesheetCSVRow> rows = new ArrayList<>(csvRowMap.values());
        rows.sort(Comparator
                .comparing((TimesheetCSVRow r) -> Optional.ofNullable(r.getFullName()).orElse(""), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(r -> Optional.ofNullable(accountIdToEmail.get(r.getAccountId())).orElse(""), String.CASE_INSENSITIVE_ORDER)
        );

        // Decide which columns to include; null means TRUE (include)
        boolean includeLogged = request.getGetLoggedEffort() == null || request.getGetLoggedEffort();
        boolean includeEarned = request.getGetEarnedEffort() == null || request.getGetEarnedEffort();

        return Boolean.TRUE.equals(request.getFileAtDateLevel())
                ? generateDateWiseCsv(rows, request.getFromDate(), request.getToDate(),
                teamIdToName, projectIdToName, orgIdToName,
                accountIdToEmail, includeLogged, includeEarned) // UPDATED
                : generateSummaryCsv(rows, request.getFromDate(), request.getToDate(),
                teamIdToName, projectIdToName, orgIdToName,
                accountIdToEmail, includeLogged, includeEarned); // UPDATED
    }


    private void validateAccess(List<Long> requesterAccountIds, Long orgId, TimesheetExportToCSVRequest request) {
        if (!userAccountRepository.existsByAccountIdInAndOrgIdAndIsActive(requesterAccountIds, orgId, true)) {
            throw new ValidationFailedException("You are not part of the organization");
        }

        if (!accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndIsActive(
                request.getEntityTypeId(),
                request.getEntityId(),
                requesterAccountIds,
                true)) {
            throw new ValidationFailedException("You do not have any role in selected entity");
        }
    }

    private String formatMinutesToHourMin(Integer mins) {
        if (mins == null || mins <= 0) return "0h 0m";
        int hours = mins / 60;
        int remaining = mins % 60;
        return hours + "h " + remaining + "m";
    }

    private Map<Long, String> fetchTeamNames(List<TimeSheetResponse> responses) {
        Set<Long> teamIds = new HashSet<>();
        for (TimeSheetResponse res : responses) {
            for (TotalEffort effort : Optional.ofNullable(res.getEfforts()).orElse(Collections.emptyList())) {
                for (EffortOnEntity e : Optional.ofNullable(effort.getEffortOnEntityList()).orElse(Collections.emptyList())) {
                    if (e.getTeamId() != null) teamIds.add(e.getTeamId());
                }
            }
        }
        return teamService.getTeamNameByIds(teamIds);
    }

    private Map<Long, String> fetchProjectNames(List<TimeSheetResponse> responses) {
        Set<Long> projectIds = new HashSet<>();
        for (TimeSheetResponse res : responses) {
            for (TotalEffort effort : Optional.ofNullable(res.getEfforts()).orElse(Collections.emptyList())) {
                for (EffortOnEntity e : Optional.ofNullable(effort.getEffortOnEntityList()).orElse(Collections.emptyList())) {
                    if (e.getProjectId() != null) projectIds.add(e.getProjectId());
                }
            }
        }
        return projectService.getProjectNameByIds(projectIds);
    }

    private Map<Long, String> fetchOrgNames(List<TimeSheetResponse> responses) {
        Set<Long> orgIds = new HashSet<>();
        for (TimeSheetResponse res : responses) {
            for (TotalEffort effort : Optional.ofNullable(res.getEfforts()).orElse(Collections.emptyList())) {
                for (EffortOnEntity e : Optional.ofNullable(effort.getEffortOnEntityList()).orElse(Collections.emptyList())) {
                    if (e.getOrgId() != null) orgIds.add(e.getOrgId());
                }
            }
        }
        return organizationService.getOrgNamesByIds(orgIds);
    }

    private List<TimeSheetResponse> fetchTimeSheetData(TimesheetExportToCSVRequest request, List<Long> accountIds, String userId, String timeZone) {
        TimeSheetRequest timeSheetRequest = new TimeSheetRequest();
        timeSheetRequest.setAccountIdList(request.getAccountIdList());
        timeSheetRequest.setFromDate(request.getFromDate());
        timeSheetRequest.setToDate(request.getToDate());

        if (Objects.equals(Constants.EntityTypes.ORG, request.getEntityTypeId())) {
            timeSheetRequest.setOrgId(request.getEntityId());
        } else if (Objects.equals(Constants.EntityTypes.PROJECT, request.getEntityTypeId())) {
            timeSheetRequest.setProjectId(request.getEntityId());
        } else {
            timeSheetRequest.setTeamId(request.getEntityId());
        }

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("userId", userId);
        headers.add("timeZone", timeZone);

        HttpEntity<TimeSheetRequest> httpEntity = new HttpEntity<>(timeSheetRequest, headers);

        try {
            ResponseEntity<List<TimeSheetResponse>> responseEntity = new RestTemplate().exchange(
                    tseHrBaseUrl + ControllerConstants.TseHr.getTimeSheetUrl,
                    HttpMethod.POST,
                    httpEntity,
                    new ParameterizedTypeReference<List<TimeSheetResponse>>() {}
            );
            return Optional.ofNullable(responseEntity.getBody()).orElse(Collections.emptyList());
        } catch (Exception ex) {
            // Fail-safe: return empty list so export still completes with 0 rows instead of 500
            return Collections.emptyList();
        }
    }

    private Map<Long, String> getAccountIdToUserNameMap(List<TimeSheetResponse> timeSheetResponses, List<Long> initialAccountIds) {
        Set<Long> allAccountIds = new HashSet<>();
        if (initialAccountIds != null) allAccountIds.addAll(initialAccountIds);
        for (TimeSheetResponse res : Optional.ofNullable(timeSheetResponses).orElse(Collections.emptyList())) {
            if (res != null && res.getAccount_Id() != null) allAccountIds.add(res.getAccount_Id());
        }

        Map<Long, String> resultMap = new HashMap<>();
        if (allAccountIds.isEmpty()) return resultMap;

        List<EmailFirstLastAccountId> fetched =
                userAccountRepository.getEmailFirstNameLastNameAccountIdByAccountIdIn(new ArrayList<>(allAccountIds));

        for (EmailFirstLastAccountId entry : Optional.ofNullable(fetched).orElse(Collections.emptyList())) {
            String firstName = Optional.ofNullable(entry.getFirstName()).orElse("").trim();
            String lastName = Optional.ofNullable(entry.getLastName()).orElse("").trim();
            String fullName = (firstName + " " + lastName).trim();
            resultMap.put(entry.getAccountId(), fullName.isEmpty() ? "No Name" : fullName);
        }

        for (Long accId : allAccountIds) {
            resultMap.computeIfAbsent(accId, id -> {
                EmailFirstLastAccountId emailFirstLastAccountId = userAccountRepository.getEmailFirstNameLastNameAccountIdByAccountId(id);
                if (emailFirstLastAccountId != null) {
                    String firstName = Optional.ofNullable(emailFirstLastAccountId.getFirstName()).orElse("").trim();
                    String lastName = Optional.ofNullable(emailFirstLastAccountId.getLastName()).orElse("").trim();
                    String fullName = (firstName + " " + lastName).trim();
                    return fullName.isEmpty() ? "No Name" : fullName;
                }
                return "No Name";
            });
        }
        return resultMap;
    }

    private Map<Long, String> getAccountIdToEmailMap(List<TimeSheetResponse> timeSheetResponses, List<Long> initialAccountIds) {
        Set<Long> allAccountIds = new HashSet<>();
        if (initialAccountIds != null) allAccountIds.addAll(initialAccountIds);
        for (TimeSheetResponse res : Optional.ofNullable(timeSheetResponses).orElse(Collections.emptyList())) {
            if (res != null && res.getAccount_Id() != null) {
                allAccountIds.add(res.getAccount_Id());
            }
        }

        Map<Long, String> resultMap = new HashMap<>();
        if (allAccountIds.isEmpty()) return resultMap;

        List<EmailFirstLastAccountId> fetched =
                userAccountRepository.getEmailFirstNameLastNameAccountIdByAccountIdIn(new ArrayList<>(allAccountIds));
        for (EmailFirstLastAccountId entry : Optional.ofNullable(fetched).orElse(Collections.emptyList())) {
            resultMap.put(entry.getAccountId(), entry.getEmail());
        }

        // fill any missing via single fetch
        for (Long id : allAccountIds) {
            resultMap.computeIfAbsent(id, k -> {
                EmailFirstLastAccountId one = userAccountRepository.getEmailFirstNameLastNameAccountIdByAccountId(k);
                return one != null ? one.getEmail() : "";
            });
        }
        return resultMap;
    }


    private TimeSheetRequest prepareTimeSheetRequest(TimesheetExportToCSVRequest exportRequest) {
        TimeSheetRequest req = new TimeSheetRequest();
        req.setAccountIdList(exportRequest.getAccountIdList());
        req.setFromDate(exportRequest.getFromDate());
        req.setToDate(exportRequest.getToDate());

        if (Objects.equals(Constants.EntityTypes.ORG, exportRequest.getEntityTypeId())) {
            req.setOrgId(exportRequest.getEntityId());
        } else if (Objects.equals(Constants.EntityTypes.PROJECT, exportRequest.getEntityTypeId())) {
            req.setProjectId(exportRequest.getEntityId());
        } else {
            req.setTeamId(exportRequest.getEntityId());
        }

        return req;
    }

    private String getGroupKey(TimesheetExportToCSVRequest request, Long accountId, EffortOnEntity e) {
        if (Boolean.TRUE.equals(request.getFileAtTeamLevel())) {
            if (e.getTeamId() != null) return accountId + "_T_" + e.getTeamId();
            if (e.getProjectId() != null) return accountId + "_P_" + e.getProjectId();
            return accountId + "_O_" + e.getOrgId();
        } else {
            if (Objects.equals(Constants.EntityTypes.TEAM, request.getEntityTypeId())) {
                return accountId + "_T_" + request.getEntityId();
            } else if (Objects.equals(Constants.EntityTypes.PROJECT, request.getEntityTypeId())) {
                return accountId + "_P_" + request.getEntityId();
            } else {
                return accountId + "_O_" + request.getEntityId();
            }
        }
    }

    public void validateTimeSheetDateRequest(TimeSheetRequest tsRequest){
        if (tsRequest.getToDate() != null || tsRequest.getFromDate() != null) {
            if(tsRequest.getToDate() != null && tsRequest.getFromDate() == null){
                tsRequest.setFromDate(tsRequest.getToDate());
            }
            else if(tsRequest.getToDate() == null){
                tsRequest.setToDate(tsRequest.getFromDate());
            }
        }
    }

    private Map<String, TimesheetCSVRow> buildCSVRows(TimesheetExportToCSVRequest request, List<TimeSheetResponse> responses, Map<Long, String> nameMap) {
        Map<String, TimesheetCSVRow> rowMap = new LinkedHashMap<>();

        for (TimeSheetResponse res : responses) {
            for (TotalEffort totalEffort : Optional.ofNullable(res.getEfforts()).orElse(Collections.emptyList())) {
                for (EffortOnEntity e : Optional.ofNullable(totalEffort.getEffortOnEntityList()).orElse(Collections.emptyList())) {
                    String key = getGroupKey(request, res.getAccount_Id(), e);

                    TimesheetCSVRow row = rowMap.computeIfAbsent(key, k -> {
                        TimesheetCSVRow timesheetCSVRow = new TimesheetCSVRow();
                        timesheetCSVRow.setAccountId(res.getAccount_Id());
                        timesheetCSVRow.setFullName(nameMap.getOrDefault(res.getAccount_Id(), ""));
                        timesheetCSVRow.setTeamId(e.getTeamId());
                        timesheetCSVRow.setProjectId(e.getProjectId());
                        timesheetCSVRow.setOrgId(e.getOrgId());

                        int workingDays = (int) request.getFromDate().datesUntil(request.getToDate().plusDays(1))
                                .filter(date -> {
                                    List<Integer> off = Optional.ofNullable(res.getOffDays()).orElse(Collections.emptyList());
                                    return !off.contains(date.getDayOfWeek().getValue());
                                })
                                .count();
                        timesheetCSVRow.setExpectedWorkTime(res.getMaxDailyWorkingHrs() * workingDays);
                        return timesheetCSVRow;
                    });

                    LocalDate date = totalEffort.getTotalEffortDate();
                    row.setTotalBurnedEffort(row.getTotalBurnedEffort() + Optional.ofNullable(e.getEntityEffortMins()).orElse(0));
                    row.setTotalEarnedEffort(row.getTotalEarnedEffort() + Optional.ofNullable(e.getEntityEarnedTime()).orElse(0));
                    row.getDateBurnedEffortMap().merge(date, Optional.ofNullable(e.getEntityEffortMins()).orElse(0), Integer::sum);
                    row.getDateEarnedEffortMap().merge(date, Optional.ofNullable(e.getEntityEarnedTime()).orElse(0), Integer::sum);
                }
            }
        }

        return rowMap;
    }

    private byte[] generateSummaryCsv(List<TimesheetCSVRow> rows, LocalDate from, LocalDate to,
                                      Map<Long, String> teamMap, Map<Long, String> projMap, Map<Long, String> orgMap,
                                      Map<Long, String> emailMap,
                                      boolean includeLogged, boolean includeEarned) {
        try (StringWriter sw = new StringWriter(); CSVWriter writer = new CSVWriter(sw)) {

            List<String> header = new ArrayList<>();
            header.add("Serial No.");
            header.add("Name");
            header.add("Email"); // NEW
            header.add("From Date");
            header.add("To Date");
            if (includeLogged) header.add("Total Logged Effort");  // renamed/conditional
            if (includeEarned) header.add("Total Earned Effort");  // conditional
            header.add("Expected Work Time");
            header.add("Team Name");
            header.add("Project Name");
            header.add("Org Name");
            writer.writeNext(header.toArray(new String[0]));

            int i = 1;
            for (TimesheetCSVRow r : rows) {
                List<String> line = new ArrayList<>();
                line.add(String.valueOf(i++));
                line.add(Optional.ofNullable(r.getFullName()).orElse(""));
                line.add(Optional.ofNullable(emailMap.get(r.getAccountId())).orElse(""));  // NEW
                line.add(from.toString());
                line.add(to.toString());
                if (includeLogged) line.add(formatMinutesToHourMin(Optional.ofNullable(r.getTotalBurnedEffort()).orElse(0)));
                if (includeEarned) line.add(formatMinutesToHourMin(Optional.ofNullable(r.getTotalEarnedEffort()).orElse(0)));
                line.add(Optional.ofNullable(r.getExpectedWorkTime()).orElse(0) + "h");
                line.add(Optional.ofNullable(teamMap.get(r.getTeamId())).orElse(""));
                line.add(Optional.ofNullable(projMap.get(r.getProjectId()))
                        .filter(p -> !com.tse.core_application.constants.Constants.PROJECT_NAME.equals(p))
                        .orElse("NA"));
                line.add(Optional.ofNullable(orgMap.get(r.getOrgId())).orElse(""));
                writer.writeNext(line.toArray(new String[0]));
            }

            writer.flush();
            return sw.toString().getBytes();
        } catch (Exception e) {
            throw new RuntimeException("Error generating CSV", e);
        }
    }


    private byte[] generateDateWiseCsv(List<TimesheetCSVRow> rows, LocalDate from, LocalDate to,
                                       Map<Long, String> teamMap, Map<Long, String> projMap, Map<Long, String> orgMap,
                                       Map<Long, String> emailMap,
                                       boolean includeLogged, boolean includeEarned) {
        try (StringWriter sw = new StringWriter(); CSVWriter writer = new CSVWriter(sw)) {

            List<String> headers = new ArrayList<>(List.of("Serial No.", "Name", "Email"));
            List<LocalDate> allDates = from.datesUntil(to.plusDays(1)).collect(Collectors.toList());

            // Per-day headers: use Logged (rename), add Earned conditionally
            for (LocalDate d : allDates) {
                if (includeLogged) headers.add(d + " Logged Effort");
                if (includeEarned) headers.add(d + " Earned Effort");
            }

            if (includeLogged) headers.add("Total Logged Effort");
            if (includeEarned) headers.add("Total Earned Effort");
            headers.add("Expected Work Time");
            headers.add("Team Name");
            headers.add("Project Name");
            headers.add("Org Name");

            writer.writeNext(headers.toArray(new String[0]));

            int i = 1;
            for (TimesheetCSVRow r : rows) {
                List<String> row = new ArrayList<>();
                row.add(String.valueOf(i++));
                row.add(Optional.ofNullable(r.getFullName()).orElse(""));
                row.add(Optional.ofNullable(emailMap.get(r.getAccountId())).orElse("")); // NEW

                for (LocalDate d : allDates) {
                    Integer dayLogged  = Optional.ofNullable(r.getDateBurnedEffortMap().get(d)).orElse(0); // data unchanged
                    Integer dayEarned  = Optional.ofNullable(r.getDateEarnedEffortMap().get(d)).orElse(0);
                    if (includeLogged) row.add(formatMinutesToHourMin(dayLogged));
                    if (includeEarned) row.add(formatMinutesToHourMin(dayEarned));
                }

                if (includeLogged) row.add(formatMinutesToHourMin(Optional.ofNullable(r.getTotalBurnedEffort()).orElse(0)));
                if (includeEarned) row.add(formatMinutesToHourMin(Optional.ofNullable(r.getTotalEarnedEffort()).orElse(0)));
                row.add(Optional.ofNullable(r.getExpectedWorkTime()).orElse(0) + "h");
                row.add(Optional.ofNullable(teamMap.get(r.getTeamId())).orElse(""));
                row.add(Optional.ofNullable(projMap.get(r.getProjectId()))
                        .filter(p -> !com.tse.core_application.constants.Constants.PROJECT_NAME.equals(p))
                        .orElse("NA"));
                row.add(Optional.ofNullable(orgMap.get(r.getOrgId())).orElse(""));

                writer.writeNext(row.toArray(new String[0]));
            }

            writer.flush();
            return sw.toString().getBytes();
        } catch (Exception e) {
            throw new RuntimeException("Error generating date-wise CSV", e);
        }
    }
}
