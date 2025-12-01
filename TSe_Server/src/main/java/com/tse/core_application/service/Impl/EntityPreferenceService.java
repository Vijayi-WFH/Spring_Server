package com.tse.core_application.service.Impl;

import com.fasterxml.jackson.databind.util.BeanUtil;
import com.google.firebase.database.utilities.Pair;
import com.tse.core_application.constants.RoleEnum;
import com.tse.core_application.custom.model.LeaveTypeAlias;
import com.tse.core_application.dto.*;
import com.tse.core_application.dto.leave.Response.LeaveTypesResponse;
import com.tse.core_application.exception.ForbiddenException;
import com.tse.core_application.exception.SubTaskDetailsMissingException;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.model.*;
import com.tse.core_application.repository.*;
import com.tse.core_application.utils.CommonUtils;
import com.tse.core_application.utils.DateTimeUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class EntityPreferenceService {

    @Value("${default.file.size}")
    private Long defaultFileSize;

    @Autowired
    private EntityPreferenceRepository entityPreferenceRepository;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private AccessDomainRepository accessDomainRepository;
    @Autowired
    private HolidayOffDayRepository holidayOffDayRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private CapacityService capacityService;

    @Autowired
    private TeamService teamService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private MemberDetailsRepository memberDetailsRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private AttendanceService attendanceService;
    /**
     * This method retrieves the allowed file size for a specific entity based on its type and ID. If a specific setting is not found, it returns a default file size value.
     */
    public Long getAllowedFileSizeForEntity(Integer entityTypeId, Long entityId) {
        Optional<EntityPreference> entityPreferenceOptional = entityPreferenceRepository.findByEntityTypeIdAndEntityId(entityTypeId, entityId);
        if (entityPreferenceOptional.isPresent()) {
            EntityPreference entityPreference = entityPreferenceOptional.get();
            if (entityPreference.getAllowedFileSize() != null) {
                return entityPreference.getAllowedFileSize();
            }
        }
        return defaultFileSize;
    }

    /**
     * This method either adds a new or updates an existing EntityPreference, based on the presence of the entity in the database.
     */
    @Transactional
    public EntityPreferenceResponse saveOrUpdateEntityPreference(EntityPreferenceRequest request, String timeZone, String accountIds) {

        Optional<EntityPreference> entityPreferenceFromDb = entityPreferenceRepository.findByEntityTypeIdAndEntityId(request.getEntityTypeId(), request.getEntityId());
        EntityPreference entityPreferenceToSave = new EntityPreference();
        EntityPreferenceResponse entityPreferenceResponse = new EntityPreferenceResponse();
        Integer minutesOfWorking = null;
        Boolean updateCapacities = false;

        // ZZZZZZ 14-04-2025
        // Changes on 17-10-2025
        if (request.getOfficeHrsStartTime() != null && request.getOfficeHrsEndTime() != null) {
            if (request.getOfficeHrsStartTime().equals(request.getOfficeHrsEndTime())) {
                throw new ValidationFailedException("Office start time and end time cannot be the same");
            }

            Integer totalMinutes = calculateMinutesOfWorking(request.getOfficeHrsStartTime(), request.getOfficeHrsEndTime(), request.getBreakDuration());
            if (totalMinutes <= 0) {
                throw new ValidationFailedException("Total working hours cannot be 0 or negative.");
            }

            if (request.getBreakDuration() != null && request.getBreakDuration() >= totalMinutes) {
                throw new ValidationFailedException("Break duration cannot be greater than or equal to total office duration");
            }
        }

        validateAndNormalizeRequest (request);

        if (entityPreferenceFromDb.isPresent()) {
            if (request.getMeetingEffortPreferenceId() != null && !Objects.equals(entityPreferenceFromDb.get().getMeetingEffortPreferenceId(), request.getMeetingEffortPreferenceId())) {
                capacityService.adjustCapacityForMeetingPreferenceChange(request.getEntityTypeId(), request.getEntityId(), entityPreferenceFromDb.get().getMeetingEffortPreferenceId(),
                        request.getMeetingEffortPreferenceId());
            }

            if ((request.getOfficeHrsStartTime() != null && !Objects.equals(request.getOfficeHrsStartTime(), entityPreferenceFromDb.get().getOfficeHrsStartTime()))
                    || (request.getOfficeHrsEndTime() != null && !Objects.equals(request.getOfficeHrsEndTime(), entityPreferenceFromDb.get().getOfficeHrsEndTime()))
                    || (request.getBreakDuration() != null && !Objects.equals(request.getBreakDuration(), entityPreferenceFromDb.get().getBreakDuration()))) {

                Integer breakDuration = request.getBreakDuration() != null ? request.getBreakDuration() : entityPreferenceFromDb.get().getBreakDuration();
                LocalTime startTime = request.getOfficeHrsStartTime() != null ? request.getOfficeHrsStartTime() : entityPreferenceFromDb.get().getOfficeHrsStartTime();
                LocalTime endTime = request.getOfficeHrsEndTime() != null ? request.getOfficeHrsEndTime() : entityPreferenceFromDb.get().getOfficeHrsEndTime();
                minutesOfWorking = calculateMinutesOfWorking(startTime, endTime, breakDuration);
                updateCapacities = true;
            }

            if (request.getOffDays() != null) {
                updateCapacities = true;
            }

            BeanUtils.copyProperties(entityPreferenceFromDb.get(), entityPreferenceToSave);
            entityPreferenceToSave.setHolidayOffDays(new ArrayList<>(entityPreferenceFromDb.get().getHolidayOffDays()));
            CommonUtils.copyNonNullProperties(request, entityPreferenceToSave);
        } else {
            if (request.getOfficeHrsStartTime() != null && request.getOfficeHrsEndTime() != null) {
                minutesOfWorking = calculateMinutesOfWorking(request.getOfficeHrsStartTime(), request.getOfficeHrsEndTime(), request.getBreakDuration());
            }
            entityPreferenceToSave = new EntityPreference();
            BeanUtils.copyProperties(request, entityPreferenceToSave);
        }

        if (entityPreferenceToSave.getTimeOffAlias() == null || entityPreferenceToSave.getTimeOffAlias().isEmpty()) {
            entityPreferenceToSave.setTimeOffAlias(Constants.LeaveTypeNameConstant.TIME_OFF);
        }

        if (entityPreferenceToSave.getSickLeaveAlias() == null || entityPreferenceToSave.getSickLeaveAlias().isEmpty()) {
            entityPreferenceToSave.setSickLeaveAlias(Constants.LeaveTypeNameConstant.SICK_LEAVE);
        }

        // ZZZZZZ 14-04-2025
        LocalDate officeDate = LocalDate.of(2022, 4, 10);

        if (entityPreferenceToSave.getOfficeHrsStartTime() != null) {
            entityPreferenceToSave.setOfficeHrsStartDateTime(DateTimeUtils.convertUserDateToServerTimezone(LocalDateTime.of(officeDate, entityPreferenceToSave.getOfficeHrsStartTime()), timeZone));
        }

        if (entityPreferenceToSave.getOfficeHrsEndTime() != null) {
            LocalDate endDate = officeDate;

            // If it's a night shift (end before start), push end date to next day
            if (entityPreferenceToSave.getOfficeHrsStartTime() != null && entityPreferenceToSave.getOfficeHrsEndTime().isBefore(entityPreferenceToSave.getOfficeHrsStartTime())) {
                endDate = endDate.plusDays(1);
            }

            entityPreferenceToSave.setOfficeHrsEndDateTime(DateTimeUtils.convertUserDateToServerTimezone(LocalDateTime.of(endDate, entityPreferenceToSave.getOfficeHrsEndTime()), timeZone));
        }

        if (request.getAllowedFileSize() != null && request.getAllowedFileSize() > 0) {
            entityPreferenceToSave.setAllowedFileSize(request.getAllowedFileSize() * 1024 * 1024); // saving in bytes -- front end will send it in Mb
        }

        if (request.getQuickCreateWorkflowStatus() != null && !request.getQuickCreateWorkflowStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG_TITLE_CASE) && !request.getQuickCreateWorkflowStatus().equalsIgnoreCase(Constants.WorkFlowTaskStatusConstants.STATUS_NOT_STARTED_TITLE_CASE)) {
            throw new IllegalStateException("Please provide a valid workflow status");
        }

        if (minutesOfWorking != null) {
            entityPreferenceToSave.setMinutesToWorkDaily(minutesOfWorking);
            List<Long> accountIdList = userAccountRepository.findAllAccountIdByOrgIdAndIsActive(entityPreferenceToSave.getEntityId(), true);
            memberDetailsRepository.updateWorkMinutesForAccountIdIn(minutesOfWorking, accountIdList);
        }

        if(entityPreferenceToSave.getRequireMinimumSignUpDetails() == null)
            entityPreferenceToSave.setRequireMinimumSignUpDetails(false);

        EntityPreference savedEntityPreference = entityPreferenceRepository.save(entityPreferenceToSave);
        updateTeamPrefEntityOnOrgPref(savedEntityPreference); //save all Team preferences when OrgPreference updated.
        BeanUtils.copyProperties(savedEntityPreference, entityPreferenceResponse);

        if (request.getHolidays() != null) { // Handle holidays
            updateHolidays(request.getHolidays(), savedEntityPreference);
        }

        if(savedEntityPreference.getAllowedFileSize() != null) entityPreferenceResponse.setAllowedFileSize(savedEntityPreference.getAllowedFileSize() / 1048576);
        List<HolidayResponse> holidays = holidayOffDayRepository.findCustomHolidayResponseByEntityPreferenceIdAndIsActive(savedEntityPreference.getEntityPreferenceId(), true);
        entityPreferenceResponse.setHolidays(holidays);

        Boolean updateCapacitiesForJustNewHoldiays = !updateCapacities && (request.getHolidays() != null);
        if (updateCapacities || updateCapacitiesForJustNewHoldiays) {
            EntityPreference entityPreference = new EntityPreference();
            BeanUtils.copyProperties(savedEntityPreference, entityPreference);
            entityPreference.setHolidayOffDays(holidayOffDayRepository.findAllByEntityPreferenceIdAndIsActive(entityPreference.getEntityPreferenceId(), true));
            capacityService.updateCapacities(entityPreference, updateCapacitiesForJustNewHoldiays, request.getHolidays(), timeZone);
        }
        auditService.auditForUpdateEntityPreference(userAccountRepository.findByAccountIdInAndOrgIdAndIsActive(CommonUtils.convertToLongList(accountIds), entityPreferenceFromDb.get().getEntityId(), true), entityPreferenceFromDb.get());
        return entityPreferenceResponse;
    }

    private void validateAndNormalizeRequest(EntityPreferenceRequest request) {
        if (request.getStarringWorkItemRoleIdList() == null || request.getStarringWorkItemRoleIdList().isEmpty()) {
            request.setStarringWorkItemRoleIdList(Constants.DEFAULT_ROLE_IDS_FOR_STARRING_WORK_ITEM);
        }
        List<Integer> starringWorkItemRoleIdList = new ArrayList<>(new HashSet<>(request.getStarringWorkItemRoleIdList()));
        List<Integer> teamNonAdminRoleId = Constants.TEAM_NON_ADMIN_ROLE;

        List<Integer> invalidIds = starringWorkItemRoleIdList.stream()
                .filter(id -> !teamNonAdminRoleId.contains(id))
                .collect(Collectors.toList());

        if (!invalidIds.isEmpty()) {
            throw new IllegalArgumentException("Invalid role ID(s) found in Starred Work Item Role ID list: " + invalidIds);
        }
        request.setStarringWorkItemRoleIdList(starringWorkItemRoleIdList);
    }

    /**
     * The updateHolidays method processes a set of HolidayRequest objects to manage holiday records associated with an EntityPreference.
     * It adds new holidays if holidayId is null, and updates or deletes existing holidays based on the isToDelete flag and the provided holidayId
     */
    private List<HolidayOffDay> updateHolidays(Set<HolidayRequest> holidayRequests, EntityPreference savedEntityPreference) {
        List<HolidayOffDay> holidays = new ArrayList<>();
        long uniqueDatesCount = holidayRequests.stream().map(HolidayRequest::getDate).distinct().count();
        if (uniqueDatesCount != holidayRequests.size()) {
            throw new IllegalStateException("Duplicate holiday dates found in the request.");
        }
        for (HolidayRequest request : holidayRequests) {
            if (request.getHolidayId() == null) {
                if (holidayExistsByDate(request.getDate(), savedEntityPreference)) {
                    throw new IllegalStateException("A holiday with the given date " + request.getDate() + " already exists.");
                }
                // Add new holiday
                HolidayOffDay newHoliday = new HolidayOffDay();
                newHoliday.setDate(request.getDate());
                newHoliday.setDescription(request.getDescription());
//                newHoliday.setRecurring(request.isRecurring());
                newHoliday.setActive(true);
                newHoliday.setEntityPreference(savedEntityPreference);
                holidays.add(holidayOffDayRepository.save(newHoliday));
            } else {
                // Update or delete existing holiday
                Optional<HolidayOffDay> existingHolidayOpt = holidayOffDayRepository.findById(request.getHolidayId());
                if (existingHolidayOpt.isPresent()) {
                    HolidayOffDay existingHoliday = existingHolidayOpt.get();
                    if (request.getIsToDelete() != null && request.getIsToDelete()) {
                        //Todo Ask Sawan if we want to hard delete
                        // Option 1: Delete the holiday
//                        holidayOffDayRepository.delete(existingHoliday);
                        // Option 2: Deactivate the holiday
                        existingHoliday.setActive(false);
                        holidays.add(holidayOffDayRepository.save(existingHoliday));
                    } else {
                        // Update existing holiday details (if needed)
                        existingHoliday.setDate(request.getDate());
                        existingHoliday.setDescription(request.getDescription());
//                        existingHoliday.setRecurring(request.isRecurring());
                        holidays.add(holidayOffDayRepository.save(existingHoliday));
                    }
                }
            }
        }
        return holidays;
    }

    /**
     * checks if any holiday already exists by date in the database
     */
    private boolean holidayExistsByDate(LocalDate date, EntityPreference entityPreference) {
        if (entityPreference == null || entityPreference.getHolidayOffDays() == null) {
            return false;
        }

        return entityPreference.getHolidayOffDays().stream()
                .anyMatch(holiday -> holiday.getDate().isEqual(date) && holiday.isActive());
    }


    /**
     * validates that the entity preference request is valid and returns organization name
     */
    public String validateAndGetOrgNameFromEntityPreferenceRequest(EntityPreferenceRequest request, String accountIds) {

        if (!Objects.equals(request.getEntityTypeId(), Constants.EntityTypes.ORG)) {
            throw new ValidationFailedException("Invalid entity type id");
        }
        Organization organization = organizationRepository.findById(request.getEntityId()).orElseThrow(() -> new IllegalArgumentException("Invalid Id: Organization doesn't exist"));
        List<AccessDomain> accessDomainOfOrgAdmin = accessDomainRepository.findByEntityTypeIdAndEntityIdAndIsActive(Constants.EntityTypes.ORG, request.getEntityId(), true);

        // Todo: As of now, we don't have back org admin saved anywhere. When we add backup org admin functionality -- we need to allow back up org admin as well
        if (accessDomainOfOrgAdmin.isEmpty() || !Objects.equals(accessDomainOfOrgAdmin.get(0).getAccountId(), Long.parseLong(accountIds))) {
            throw new ValidationFailedException("Only organization admin/ backup organization admin allowed to modify user preference");
        }

        if (request.getOffDays()!=null && request.getOffDays().size() > 6)
            throw new ForbiddenException("All days cannot be off days.");

        return organization.getOrganizationName();
    }

    /**
     * method to get org preference
     */
    public EntityPreferenceResponse getOrgPreference(Long orgId, String accountIds) {
        EntityPreference entityPreference = new EntityPreference();
        EntityPreferenceResponse entityPreferenceResponse = new EntityPreferenceResponse();
        List<AccessDomain> accessDomainOfOrgAdmin = accessDomainRepository.findByEntityTypeIdAndEntityIdAndIsActive(Constants.EntityTypes.ORG, orgId, true);

        // As of now, we don't have back org admin saved anywhere. When we add backup org admin functionality -- we need to allow back up org admin as well
        if (accessDomainOfOrgAdmin.isEmpty() || !Objects.equals(accessDomainOfOrgAdmin.get(0).getAccountId(), Long.parseLong(accountIds))) {
            throw new ValidationFailedException("Only organization admin/ backup organization admin allowed to modify user preference");
        }

        Optional<EntityPreference> entityPreferenceOptional = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, orgId);
        if (entityPreferenceOptional.isPresent()) {
            entityPreference = entityPreferenceOptional.get();
            filterActiveHolidays(entityPreference);
        } else {
            // send null response with only entityTypeId and entityId set
            entityPreference.setEntityTypeId(Constants.EntityTypes.ORG);
            entityPreference.setEntityId(orgId);
        }
        BeanUtils.copyProperties(entityPreference, entityPreferenceResponse);
        if (entityPreference.getAllowedFileSize() != null)
            entityPreferenceResponse.setAllowedFileSize(entityPreference.getAllowedFileSize() / 1048576);
        if (entityPreference.getHolidayOffDays() != null) {
            List<HolidayResponse> holidays = new ArrayList<>();
            for (HolidayOffDay holidayOffDay : entityPreference.getHolidayOffDays()) {
                if (holidayOffDay.isActive()) {
                    HolidayResponse holidayResponse = new HolidayResponse();
                    BeanUtils.copyProperties(holidayOffDay, holidayResponse);
                    holidays.add(holidayResponse);
                }
            }
            holidays.sort(Comparator.comparing(HolidayResponse::getDate));
            entityPreferenceResponse.setHolidays(holidays);
        }
        return entityPreferenceResponse;
    }

    /**
     * method filters holidays based on isActive filter
     */
    private void filterActiveHolidays(EntityPreference entityPreference) {
        Set<HolidayOffDay> activeHolidays = entityPreference.getHolidayOffDays().stream()
                .filter(HolidayOffDay::isActive)
                .collect(Collectors.toSet());
        entityPreference.setHolidayOffDays(new ArrayList<>(activeHolidays));
    }

    /**
     * this method fetches entity preference based on entityTypeId and entityId
     */
    public EntityPreference fetchEntityPreference(Integer entityTypeId, Long entityId) {
        Optional<EntityPreference> entityPreferenceOptional = entityPreferenceRepository.findByEntityTypeIdAndEntityId(entityTypeId, entityId);
        return entityPreferenceOptional.orElse(null);
    }

    /** This function will return weekly non-working days based on user or entity preference
     * As of now, we are referring only org preference. If no preference is found, by default, Saturday and Sunday are returned.
     */
    public List<String> getWeeklyNonWorkingDaysBasedOnEntityPreference(Task task){
        EntityPreference orgPreference = fetchEntityPreference(Constants.EntityTypes.ORG, task.getFkOrgId().getOrgId());

        if (orgPreference != null && orgPreference.getOffDays() != null && !orgPreference.getOffDays().isEmpty()) {
            return orgPreference.getOffDays().stream()
                    .map(dayInt -> DayOfWeek.of(dayInt).name())
                    .collect(Collectors.toList());
        } else {
            // Default non-working days: Saturday and Sunday
            return Arrays.asList(DayOfWeek.SATURDAY.name(), DayOfWeek.SUNDAY.name());
        }
    }

    /** This function will return active holidays based on user or entity preference
     * As of now, we are referring only org preference */
    public List<LocalDate> getHolidaysBasedOnEntityPreferenceForTask(Task task) {
        List<LocalDate> holidayDates = new ArrayList<>();

        // Fetch EntityPreference for holidays
        EntityPreference orgPreference = fetchEntityPreference(Constants.EntityTypes.ORG, task.getFkOrgId().getOrgId());

        if (orgPreference != null) {
            // Add active holidays to the list
            holidayDates = orgPreference.getHolidayOffDays().stream()
                    .filter(HolidayOffDay::isActive)
                    .map(HolidayOffDay::getDate)
                    .collect(Collectors.toList());
        }

        return holidayDates;
    }

    /** This function will return holidays based on user or entity preference
     * As of now, we are referring only org preference */
    public HashMap<String, LocalTime> getOfficeHrsBasedOnEntityPreference(Task task) {
        HashMap<String, LocalTime> officeHours = new HashMap<>();

        // Fetch EntityPreference for holidays
        EntityPreference orgPreference = fetchEntityPreference(Constants.EntityTypes.ORG, task.getFkOrgId().getOrgId());

        EntityPreference orgPreferenceCopy = new EntityPreference();
        if (orgPreference != null) {
            BeanUtils.copyProperties(orgPreference, orgPreferenceCopy);
        }

        // Set default values
        LocalTime defaultStartTime = LocalTime.of(10, 0); // 10:00 AM
        LocalTime defaultEndTime = LocalTime.of(20, 0); // 08:00 PM

        // Fetching start and end times from EntityPreference
        LocalTime startTime = orgPreference != null && orgPreference.getOfficeHrsStartDateTime() != null
                ? orgPreference.getOfficeHrsStartDateTime().toLocalTime()
                : defaultStartTime;

        LocalTime endTime = orgPreference != null && orgPreference.getOfficeHrsEndDateTime() != null
                ? orgPreference.getOfficeHrsEndDateTime().toLocalTime()
                : defaultEndTime;

        officeHours.put("officeHrsStartTime", startTime);
        officeHours.put("officeHrsEndTime", endTime);

        return officeHours;
    }

    /** gets entityPreference by entityTypeId and entityId */
    public EntityPreference getEntityPreference(Integer entityTypeId, Long entityId) {
        Optional<EntityPreference> entityPreferenceOpt = entityPreferenceRepository.findByEntityTypeIdAndEntityId(entityTypeId, entityId);

        return entityPreferenceOpt.orElse(null);
    }

    /**
     *This method returns a pair containing list of off days and office minutes
     */
    public Pair<List<Integer>, Integer> getOfficeMinutesAndOffDaysFromOrgPreferenceOrDefault(Long orgId) {
        EntityPreference orgPreference = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, orgId)
                .orElse(new EntityPreference());

        List<Integer> offDays = orgPreference.getOffDays() != null ? orgPreference.getOffDays() : Collections.emptyList();

        Integer officeMinutes = orgPreference.getMinutesToWorkDaily();
        if (officeMinutes == null) {
            LocalTime startTime = orgPreference.getOfficeHrsStartTime();
            LocalTime endTime = orgPreference.getOfficeHrsEndTime();
            Integer breakDuration = orgPreference.getBreakDuration();

            if (startTime != null && endTime != null) {
                officeMinutes = calculateMinutesOfWorking(startTime, endTime, breakDuration);
            } else {
                officeMinutes = Constants.DEFAULT_OFFICE_MINUTES;
            }
        }

        return new Pair<>(offDays, officeMinutes);
    }


    public Set<LocalDate> getOfficeHolidaysAndOffDaysFromEntityPreferenceBetweenGivenDates(Integer entityTypeId, Long entityId, LocalDate startDate, LocalDate endDate) {
        EntityPreference entityPreference = getEntityPreference(entityTypeId, entityId);
        return getHolidaysAndOffDaysForEntityPreference(entityPreference, startDate, endDate);
    }

    /** This function will return active holidays based on entity preference*/
    public List<LocalDate> getHolidaysBasedOnEntityPreference(Integer entityTypeId, Long entityId) {
        List<LocalDate> holidayDates = new ArrayList<>();

        // Fetch EntityPreference for holidays
        EntityPreference entityPreference = fetchEntityPreference(Constants.EntityTypes.ORG, entityId);

        if (entityPreference != null) {
            // Add active holidays to the list
            holidayDates = entityPreference.getHolidayOffDays().stream()
                    .filter(HolidayOffDay::isActive)
                    .map(HolidayOffDay::getDate)
                    .collect(Collectors.toList());
        }

        return holidayDates;
    }

    public Boolean getIsMonthlyLeaveUpdateOnProRata (Long teamId, Long orgId) {
        if (teamId != null) {
            Optional<EntityPreference> teamPreferenceOptional = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.TEAM, teamId);
            if (teamPreferenceOptional.isPresent() && teamPreferenceOptional.get().getIsMonthlyLeaveUpdateOnProRata() != null) {
                return teamPreferenceOptional.get().getIsMonthlyLeaveUpdateOnProRata();
            }
        }
        if (orgId != null) {
            Optional<EntityPreference> orgPreferenceOptional = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, orgId);
            if (orgPreferenceOptional.isPresent() && orgPreferenceOptional.get().getIsMonthlyLeaveUpdateOnProRata() != null) {
                return orgPreferenceOptional.get().getIsMonthlyLeaveUpdateOnProRata();
            }
        }
        return true;
    }

    public Boolean getIsYearlyLeaveUpdateOnProRata (Long teamId, Long orgId) {
        if (teamId != null) {
            Optional<EntityPreference> teamPreferenceOptional = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.TEAM, teamId);
            if (teamPreferenceOptional.isPresent() && teamPreferenceOptional.get().getIsYearlyLeaveUpdateOnProRata() != null) {
                return teamPreferenceOptional.get().getIsYearlyLeaveUpdateOnProRata();
            }
        }
        if (orgId != null) {
            Optional<EntityPreference> orgPreferenceOptional = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, orgId);
            if (orgPreferenceOptional.isPresent() && orgPreferenceOptional.get().getIsYearlyLeaveUpdateOnProRata() != null) {
                return orgPreferenceOptional.get().getIsYearlyLeaveUpdateOnProRata();
            }
        }
        return true;
    }

    public List<Integer> getWorkWeek (Long entityId) {
        Optional<EntityPreference> entityPreferenceOptional = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.TEAM, entityId);
        if (entityPreferenceOptional.isPresent()) {
            List<Integer> workWeekDays = new ArrayList<>(Constants.daysOfWeek);
            EntityPreference entityPreference = entityPreferenceOptional.get();
            List<Integer> offDays = entityPreference.getOffDays();
            workWeekDays.removeAll(offDays);
            return workWeekDays;
        }
        return Collections.emptyList();
    }

    /** gets the list of entity preferences for multiple entities*/
    public List<EntityPreference> findEntityPreferenceForEntities(Integer entityTypeId, List<Long> entityIds) {
        List<EntityPreference> entityPreferences = new ArrayList<>();
        if (entityTypeId.equals(Constants.EntityTypes.ORG)) {
            entityPreferences = entityPreferenceRepository.findByEntityTypeIdAndEntityIdIn(Constants.EntityTypes.ORG, entityIds);
        } else if (entityTypeId.equals(Constants.EntityTypes.TEAM)) {
            entityPreferences = entityPreferenceRepository.findByEntityTypeIdAndEntityIdIn(Constants.EntityTypes.ORG, entityIds);
        }

        return entityPreferences;
    }

    // ZZZZZZ 14-04-2025
    public void setDefaultOrgPreference(Long orgId, String timeZone) {
        Optional<EntityPreference> entityPreferenceOptional = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, orgId);
        if (entityPreferenceOptional.isEmpty()) {
            EntityPreference entityPreference = new EntityPreference();
            entityPreference.setEntityTypeId(Constants.EntityTypes.ORG);
            entityPreference.setEntityId(orgId);
            entityPreference.setAllowedFileSize(Constants.EntityPreferenceConstants.DEFAULT_ALLOWED_FILE_SIZE);
            entityPreference.setBreakDuration(Constants.EntityPreferenceConstants.BREAK_DURATION);
            entityPreference.setOffDays(Constants.EntityPreferenceConstants.OFF_DAYS);
            entityPreference.setOfficeHrsStartTime(Constants.EntityPreferenceConstants.OFFICE_START_TIME);
            entityPreference.setOfficeHrsEndTime(Constants.EntityPreferenceConstants.OFFICE_END_TIME);
            // ZZZZZZ 14-04-2025
            LocalDate officeDate = LocalDate.of(2022, 4, 10);
            entityPreference.setOfficeHrsStartDateTime(DateTimeUtils.convertUserDateToServerTimezone(LocalDateTime.of(officeDate, Constants.EntityPreferenceConstants.OFFICE_START_TIME), timeZone));
            entityPreference.setOfficeHrsEndDateTime(DateTimeUtils.convertUserDateToServerTimezone(LocalDateTime.of(officeDate, Constants.EntityPreferenceConstants.OFFICE_END_TIME), timeZone));
            entityPreference.setTaskEffortEditDuration(Constants.EntityPreferenceConstants.TASK_EFFORT_EDIT_DURATION);
            entityPreference.setMeetingEffortEditDuration(Constants.EntityPreferenceConstants.MEETING_EFFORT_EDIT_DURATION);
            entityPreference.setMinutesToWorkDaily(Constants.DEFAULT_OFFICE_MINUTES);
            entityPreference.setLeaveRequesterCancelTime(Constants.EntityPreferenceConstants.OFFICE_START_TIME.plusHours(1));
            entityPreference.setTimeOffAlias(Constants.LeaveTypeNameConstant.TIME_OFF);
            entityPreference.setSickLeaveAlias(Constants.LeaveTypeNameConstant.SICK_LEAVE);

            List<Integer> defaultRoleForStarringWorkItem = Constants.DEFAULT_ROLE_IDS_FOR_STARRING_WORK_ITEM;
            entityPreference.setStarringWorkItemRoleIdList(defaultRoleForStarringWorkItem);

            entityPreferenceRepository.save(entityPreference);
        }
    }

    public LocalTime getOfficeStartTime (Long orgId, Long teamId) {
        if (teamId != null) {
            Optional<EntityPreference> teamPreference = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.TEAM, teamId);
            if (teamPreference.isPresent() && teamPreference.get().getOfficeHrsStartTime() != null ) {
                return teamPreference.get().getOfficeHrsStartTime();
            }
        }

        if (orgId != null) {
            Optional<EntityPreference> orgPreference = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, orgId);
            if (orgPreference.isPresent() && orgPreference.get().getOfficeHrsStartTime() != null ) {
                return orgPreference.get().getOfficeHrsStartTime();
            }
        }

        return Constants.OFFICE_START_TIME.toLocalTime();
    }

    public LocalTime getOfficeEndTime (Long orgId, Long teamId) {
        if (teamId != null) {
            Optional<EntityPreference> teamPreference = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.TEAM, teamId);
            if (teamPreference.isPresent() && teamPreference.get().getOfficeHrsEndTime() != null ) {
                return teamPreference.get().getOfficeHrsEndTime();
            }
        }

        if (orgId != null) {
            Optional<EntityPreference> orgPreference = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, orgId);
            if (orgPreference.isPresent() && orgPreference.get().getOfficeHrsEndTime() != null ) {
                return orgPreference.get().getOfficeHrsEndTime();
            }
        }

        return Constants.OFFICE_END_TIME.toLocalTime();
    }

    public Long getCapacityLimit (Long orgId, Long teamId) {
        if (teamId != null) {
            Optional<EntityPreference> teamPreference = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.TEAM, teamId);
            if (teamPreference.isPresent() && teamPreference.get().getCapacityLimit() != null ) {
                return teamPreference.get().getCapacityLimit();
            }
        }

        if (orgId != null) {
            Optional<EntityPreference> orgPreference = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, orgId);
            if (orgPreference.isPresent() && orgPreference.get().getCapacityLimit() != null ) {
                return orgPreference.get().getCapacityLimit();
            }
        }

        return Constants.CAPACITY_LIMIT;
    }

    public void updateEntityPreferenceFields (Long teamId, HashMap<Long, List<Integer>> entityPrefernceForAuthorizedRoles, HashMap<Long, Boolean> entityPreferenceForDelayedTask) {
        Optional<EntityPreference> entityPreferenceOptional = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.TEAM, teamId);
        if (entityPreferenceOptional.isPresent()) {
            EntityPreference entityPreference = entityPreferenceOptional.get();
            if (entityPreference.getRolesWithStatusInquiryRights() != null && !entityPreference.getRolesWithStatusInquiryRights().isEmpty()) {
                List<Integer> authorizedRoles = entityPreference.getRolesWithStatusInquiryRights();
                entityPrefernceForAuthorizedRoles.put(teamId, authorizedRoles);

            }
            if (entityPreference.getDelayInquiryEnabled() != null) {
                entityPreferenceForDelayedTask.put(teamId, entityPreference.getDelayInquiryEnabled());
            }
        }
        if (!entityPreferenceForDelayedTask.containsKey(teamId)) {
            entityPreferenceForDelayedTask.put(teamId, true);
        }
        if (!entityPrefernceForAuthorizedRoles.containsKey(teamId)) {
            entityPrefernceForAuthorizedRoles.put(teamId, Constants.rolesWithStatusInquiryRights);
        }
    }

    /**
     * This method takes the entity type id and entity Id and returns the workflow status for quick create task. If team preference do not exist org preference is sent
     */
    public CreateUpdateTaskPreferenceResponse getCreateAndUpdateTaskPreference(Integer entityTypeId, Long entityId, String accountIds) {
        CreateUpdateTaskPreferenceResponse createUpdateTaskPreferenceResponse = new CreateUpdateTaskPreferenceResponse();
        Optional<EntityPreference> entityPreferenceOptional = Optional.empty();
        Long orgId = null;
        List<Long> accountIdList = Arrays.stream(accountIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
        if (Objects.equals(entityTypeId, Constants.EntityTypes.TEAM)) {
            boolean hrAccess = attendanceService.checkHrAccessForAttendenceForTeamsAndOrgs(List.of(entityId), null, accountIdList);
            if (!teamService.ifUserExistsInTeam(entityId, accountIds) && !hrAccess) {
                throw new ValidationFailedException("The specified user does not belong to the provided team.");
            }
            entityPreferenceOptional = entityPreferenceRepository.findByEntityTypeIdAndEntityId(entityTypeId, entityId);
            orgId = teamService.getOrgIdByTeamId(entityId);
        } else if (Objects.equals(entityTypeId, Constants.EntityTypes.ORG)) {
            boolean hrAccess = attendanceService.checkHrAccessForAttendenceForTeamsAndOrgs(null, List.of(entityId), accountIdList);
            if (!organizationService.validateOrgUser(entityId, accountIds) && !hrAccess) {
                throw new ValidationFailedException("The specified user does not belong to the provided organization.");
            }
            orgId = entityId;
        } else {
            throw new IllegalStateException("Please provide a valid entity");
        }

        // Check for organization-level preferences if no entity-specific preferences are found
        if (orgId != null) {
            Optional<EntityPreference> orgEntityPreference = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, orgId);
            if (orgEntityPreference.isPresent()) {
                if (entityPreferenceOptional.isEmpty()) {
                    entityPreferenceOptional = orgEntityPreference;
                }
                createUpdateTaskPreferenceResponse.setIsGeoFencingAllowed(orgEntityPreference.get().getIsGeoFencingAllowed());
                createUpdateTaskPreferenceResponse.setIsGeoFencingActive(orgEntityPreference.get().getIsGeoFencingActive());
            }
        }

        // Set workflow status based on preferences
        String quickCreateWorkflowStatus = entityPreferenceOptional
                .map(EntityPreference::getQuickCreateWorkflowStatus)
                .orElse(Constants.WorkFlowTaskStatusConstants.STATUS_BACKLOG_TITLE_CASE);


        createUpdateTaskPreferenceResponse.setQuickCreateWorkflowStatus(quickCreateWorkflowStatus);
        createUpdateTaskPreferenceResponse.setExpEndTime(entityPreferenceOptional.get().getOfficeHrsEndTime().minusMinutes(15L));
        createUpdateTaskPreferenceResponse.setExpStartTime(entityPreferenceOptional.get().getOfficeHrsStartTime());
        return createUpdateTaskPreferenceResponse;
    }

    public List<Integer> getRolesWithPerfNoteRights (Long teamId, Long orgId) {
        if (teamId != null) {
            Optional<EntityPreference> teamPreference = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.TEAM, teamId);
            if (teamPreference.isPresent() && teamPreference.get().getRolesWithPerfNoteRights() != null && !teamPreference.get().getRolesWithPerfNoteRights().isEmpty()) {
                return teamPreference.get().getRolesWithPerfNoteRights();
            }
        }

        if (orgId != null) {
            Optional<EntityPreference> orgPreference = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, orgId);
            if (orgPreference.isPresent() && orgPreference.get().getRolesWithPerfNoteRights() != null && !orgPreference.get().getRolesWithPerfNoteRights().isEmpty()) {
                return orgPreference.get().getRolesWithPerfNoteRights();
            }
        }

        return Constants.defaultRolesWithPerfNoteRights;
    }

    public Integer calculateMinutesOfWorking(LocalTime startTime, LocalTime endTime, Integer breakDuration) {
        int minutes = Math.toIntExact(startTime.until(endTime, ChronoUnit.MINUTES));
        if (startTime.isAfter(endTime)) {
            minutes += 24 * 60;
        }
        if (breakDuration != null) {
            minutes -= breakDuration;
        }
        return minutes;
    }

    public HolidaysResponse getHolidaysForOrg (Long orgId, String accountIds) {
        HolidaysResponse holidaysResponse = new HolidaysResponse();
        if (!organizationService.validateOrgUser(orgId, accountIds)) {
            throw new ValidationFailedException("User not part of provided organization.");
        }
        Optional<EntityPreference> orgPreference = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, orgId);
        if (orgPreference.isEmpty()) {
            throw new IllegalStateException("Cannot get holidays for provided organization.");
        }
        List<HolidayOffDay> allHolidays = orgPreference.get().getHolidayOffDays();
        allHolidays.sort(Comparator.comparing(HolidayOffDay::getDate));

        List<HolidayOffDay> upcomingHolidays = allHolidays.stream()
                .filter(holidayOffDay -> !holidayOffDay.getDate().isBefore(LocalDate.now()))
                .collect(Collectors.toList());

        List<HolidayOffDay> nextTwoHolidays = upcomingHolidays.stream()
                .limit(2)
                .collect(Collectors.toList());
        holidaysResponse.setAllHolidays(allHolidays);
        holidaysResponse.setNextTwoHolidays(nextTwoHolidays);
        return holidaysResponse;
    }

    public Set<LocalDate> getHolidaysAndOffDaysForEntityPreference(EntityPreference entityPreference, LocalDate startDate, LocalDate endDate) {
        Set<LocalDate> holidaysAndOffDays = new HashSet<>();

        if (entityPreference != null) {
            // Process fixed off days
            if (entityPreference.getOffDays() != null) {
                LocalDate date = startDate;
                while (!date.isAfter(endDate)) {
                    int dayOfWeek = date.getDayOfWeek().getValue();
                    if (entityPreference.getOffDays().contains(dayOfWeek)) {
                        holidaysAndOffDays.add(date);
                    }
                    date = date.plusDays(1);
                }
            }

            // Process holiday off days
            if (entityPreference.getHolidayOffDays() != null) {
                for (HolidayOffDay holidayOffDay : entityPreference.getHolidayOffDays()) {
                    if (holidayOffDay.isActive() &&
                            (holidayOffDay.getDate().isEqual(startDate) || (holidayOffDay.getDate().isAfter(startDate) && holidayOffDay.getDate().isBefore(endDate))) ||
                            holidayOffDay.getDate().isEqual(endDate)) {
                        holidaysAndOffDays.add(holidayOffDay.getDate());
                    }
                }
            }
        }

        return holidaysAndOffDays;
    }

    public List<LeaveTypesResponse> getLeaveTypeAlias (Long orgId) {
        List<LeaveTypesResponse> leaveTypesResponseList = new ArrayList<>();

        LeaveTypeAlias leaveTypeAlias = entityPreferenceRepository.findLeaveTypeAliasForEntity(Constants.EntityTypes.ORG, orgId);
        LeaveTypesResponse leaveTypesResponseForTimeOff = new LeaveTypesResponse(Constants.LEAVE_TYPE.get(0), leaveTypeAlias.getTimeOffAlias());
        leaveTypesResponseList.add(leaveTypesResponseForTimeOff);
        LeaveTypesResponse leaveTypesResponseForSickLeave = new LeaveTypesResponse(Constants.LEAVE_TYPE.get(1), leaveTypeAlias.getSickLeaveAlias());
        leaveTypesResponseList.add(leaveTypesResponseForSickLeave);
        return leaveTypesResponseList;
    }

    // ZZZZ Made the following changes in discussion with Rohit sir on 6 april 2025

    public long getBreakTimeOfOrg (Long orgId) {
        EntityPreference orgPreference = fetchEntityPreference(Constants.EntityTypes.ORG, orgId);
        if (orgPreference != null && orgPreference.getBreakDuration() != null) {
            return (long) orgPreference.getBreakDuration() * 60;
        }
        return Constants.BREAK_TIME_IN_DAY;
    }
    // ZZZZ Changes end

    public TeamPreferenceResponse getTeamPreference(Long teamId) {
        if (teamId != null) {
            return mapEntityPrefWithTeamPrefResponse(teamId);
        }
        return null;
    }

    @Transactional
    public String saveTeamPreference(TeamPreferenceRequest preferenceRequest, Long headerAccountId) {

        String response = "Unable to Save Team Preferences";
        if (preferenceRequest.getEntityId() != null) {
            Team teamDb = teamRepository.findByTeamId(preferenceRequest.getEntityId());
            Boolean hasTeamUpdateAccess = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.ORG, teamDb.getFkOrgId().getOrgId(),
                    List.of(headerAccountId), List.of(RoleEnum.ORG_ADMIN.getRoleId()), true);
            if (!hasTeamUpdateAccess)
                hasTeamUpdateAccess = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.BU, teamDb.getFkProjectId().getBuId(),
                        List.of(headerAccountId), List.of(RoleEnum.BU_ADMIN.getRoleId()), true);
            if (!hasTeamUpdateAccess)
                hasTeamUpdateAccess = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.PROJECT, teamDb.getFkProjectId().getProjectId(),
                        List.of(headerAccountId), List.of(RoleEnum.PROJECT_ADMIN.getRoleId(), RoleEnum.BACKUP_PROJECT_ADMIN.getRoleId()), true);
            if (!hasTeamUpdateAccess)
                hasTeamUpdateAccess = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.TEAM, teamDb.getTeamId(),
                        List.of(headerAccountId), List.of(RoleEnum.TEAM_ADMIN.getRoleId(), RoleEnum.BACKUP_TEAM_ADMIN.getRoleId()), true);
            if (!hasTeamUpdateAccess)
                throw new ValidationFailedException("User not authorized to update the provided team");

            TeamPreferenceResponse preferenceResponse = mapEntityPrefWithTeamPrefResponse(preferenceRequest.getEntityId());
            EntityPreference orgEntityPreference = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, teamDb.getFkOrgId().getOrgId()).orElse(null);
            EntityPreference entityPreference = new EntityPreference();

            if (orgEntityPreference != null) {
                BeanUtils.copyProperties(orgEntityPreference, entityPreference);
                entityPreference.setEntityPreferenceId(null);
                entityPreference.setHolidayOffDays(new ArrayList<>(orgEntityPreference.getHolidayOffDays()));
                if (preferenceResponse != null) {
                    BeanUtils.copyProperties(preferenceResponse, entityPreference);
                    if (!Objects.equals(preferenceRequest.getBufferTimeToStartSprintEarly(), preferenceResponse.getBufferTimeToStartSprintEarly())) {
                        entityPreference.setBufferTimeToStartSprintEarly(preferenceRequest.getBufferTimeToStartSprintEarly());
                        //Add other conditions when preference increases.
                    } else {
                        return "No fields to Update";
                    }
                } else {
                    BeanUtils.copyProperties(preferenceRequest, entityPreference);
                    if (entityPreference.getBufferTimeToStartSprintEarly() == null)
                        entityPreference.setBufferTimeToStartSprintEarly(180); // by default, set the time to 3 hours if the value is null.
                }
                entityPreferenceRepository.save(entityPreference); // if there is no preference found, it will save a new Record.
                response = "Team Preference Saved Successfully";
            }
        }
        return response;
    }

    private TeamPreferenceResponse mapEntityPrefWithTeamPrefResponse(Long teamId){
        Optional<EntityPreference> entityPreferenceOpt = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.TEAM, teamId);
        EntityPreference entityPreference = entityPreferenceOpt.orElse(null);
        if(entityPreference==null)
            return null;
        return TeamPreferenceResponse.builder()
                .entityPreferenceId(entityPreference.getEntityPreferenceId())
                .entityId(entityPreference.getEntityId())
                .entityTypeId(entityPreference.getEntityTypeId())
                .bufferTimeToStartSprintEarly(entityPreference.getBufferTimeToStartSprintEarly())
                .build();
    }

    private void updateTeamPrefEntityOnOrgPref(EntityPreference orgPreference){
        if(orgPreference != null && orgPreference.getEntityId() != null) {
            List<Long> teamIds = teamRepository.findTeamIdsByOrgIds(List.of(orgPreference.getEntityId()));
            List<EntityPreference> teamPreferencesDbList = entityPreferenceRepository.findByEntityTypeIdAndEntityIdIn(Constants.EntityTypes.TEAM, teamIds);
            List<EntityPreference> teamPreferencesList = new ArrayList<>();
            for (EntityPreference teamPreferencesDb : teamPreferencesDbList) {
                EntityPreference teamPreference = new EntityPreference();
                BeanUtils.copyProperties(orgPreference, teamPreference);
                teamPreference.setEntityPreferenceId(teamPreferencesDb.getEntityPreferenceId());
                teamPreference.setHolidayOffDays(new ArrayList<>(orgPreference.getHolidayOffDays()));
                teamPreference.setEntityId(teamPreferencesDb.getEntityId());
                teamPreference.setEntityTypeId(teamPreferencesDb.getEntityTypeId());
                teamPreference.setBufferTimeToStartSprintEarly(teamPreferencesDb.getBufferTimeToStartSprintEarly());
                // More to add when specific Team Preference grows.

                teamPreferencesList.add(teamPreference);
            }
            entityPreferenceRepository.saveAll(teamPreferencesList);
        }
    }
}
