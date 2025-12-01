package com.tse.core_application.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tse.core_application.config.DebugConfig;
import com.tse.core_application.configuration.DataEncryptionConverter;
import com.tse.core_application.constants.Constants;
import com.tse.core_application.constants.RoleEnum;
import com.tse.core_application.custom.model.*;
import com.tse.core_application.dto.*;
import com.tse.core_application.dto.label.LabelResponse;
import com.tse.core_application.dto.meeting.*;
import com.tse.core_application.exception.*;
import com.tse.core_application.filters.JwtRequestFilter;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.*;
import com.tse.core_application.model.User;
import com.tse.core_application.repository.*;
import com.tse.core_application.utils.CommonUtils;
import com.tse.core_application.utils.DateTimeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.quartz.CronExpression;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.lang.NumberFormatException;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

import static com.tse.core_application.utils.DateTimeUtils.*;

@Service
public class MeetingService {
    private static final Logger logger = LogManager.getLogger(MeetingService.class.getName());
    @Autowired
    private AttendeeService attendeeService;
    @Autowired
    private MeetingRepository meetingRepository;
    @Autowired
    private AttendeeRepository attendeeRepository;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private BURepository buRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private TaskServiceImpl taskServiceImpl;
    @Autowired
    private LabelRepository labelRepository;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private UserAccountService userAccountService;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private TimeSheetRepository timeSheetRepository;
    @Autowired
    private RecurringMeetingRepository recurringMeetingRepository;
    @Autowired
    private AccessDomainRepository accessDomainRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private SchedulingService schedulingService;
    @Autowired
    private ActionItemService actionItemService;
    @Autowired
    private EntityPreferenceRepository entityPreferenceRepository;
    @Autowired
    private AuditService auditService;
    @Autowired
    private TaskHistoryService taskHistoryService;
    @Autowired
    private TaskHistoryMetadataService taskHistoryMetadataService;
    @Autowired
    private AuditRepository auditRepository;
    @Autowired
    private JwtRequestFilter jwtRequestFilter;
    @Autowired
    private MeetingService meetingService;
    @Autowired
    private MeetingSequenceRepository meetingSequenceRepository;
    @Autowired
    private RecurringMeetingSequenceRepository recurringMeetingSequenceRepository;
    @Autowired
    private CapacityService capacityService;
    @Autowired
    private EntityPreferenceService entityPreferenceService;
    @Autowired
    private ActionItemRepository actionItemRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private MeetingAnalysisUploadedFileRepository meetingAnalysisUploadedFileRepository;
    @Autowired
    private MeetingNoteService meetingNoteService;
    @Autowired
    private MeetingNoteRepository meetingNoteRepository;

    @Value("${tse.search.multiplier}")
    private double similarityThreshold;

    @Value("${workitem.meeting.reference.size.limit}")
    private int workItemMeetingsSizeLimit;

    @Value("${comment.api.key}")
    private String environmentApiKey;

    @Value("${default.effort.edit.time.meeting}")
    private Integer defaultEffortEditTime;

    ObjectMapper objectMapper = new ObjectMapper();


    @Transactional(readOnly = true)
    public void convertAllMeetingLocalDateAndTimeToServerTimeZone(MeetingRequest meeting, String LocalTimeZone) {

        if (meeting != null) {
            if (meeting.getStartDateTime() != null) {
                LocalDateTime convertedDate = DateTimeUtils.convertUserDateToServerTimezone(meeting.getStartDateTime(), LocalTimeZone);
                meeting.setStartDateTime(convertedDate);
            }

            if (meeting.getEndDateTime() != null) {
                LocalDateTime convertedDate = DateTimeUtils.convertUserDateToServerTimezone(meeting.getEndDateTime(), LocalTimeZone);
                meeting.setEndDateTime(convertedDate);
            }
        }
    }

    /**
     * method to validate if the accountId has the edit rights for a meeting
     */
    public boolean hasEditPermissions(Long accountId, Meeting meeting) {
        if (!Objects.equals(accountId, meeting.getOrganizerAccountId())) {
            return false;
        }
        return true;
    }

    /**
     * method to validate if the accountId has the edit rights for a recurring meeting
     */
    public boolean hasEditPermissionsForRecurringMeeting(Long accountId, RecurringMeeting recurringMeeting) {
        return Objects.equals(accountId, recurringMeeting.getOrganizerAccountId());
    }

    @Transactional(readOnly = true)
    public void convertAllMeetingServerDateAndTimeToLocalTimeZone(MeetingResponse meeting, String LocalTimeZone) {

        if (meeting != null) {
            if (meeting.getStartDateTime() != null) {
                LocalDateTime convertedDate = DateTimeUtils.convertServerDateToUserTimezone(meeting.getStartDateTime(), LocalTimeZone);
                meeting.setStartDateTime(convertedDate);
            }

            if (meeting.getEndDateTime() != null) {
                LocalDateTime convertedDate = DateTimeUtils.convertServerDateToUserTimezone(meeting.getEndDateTime(), LocalTimeZone);
                meeting.setEndDateTime(convertedDate);
            }

            if (meeting.getCreatedDateTime() != null) {
                LocalDateTime convertedDate = DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(meeting.getCreatedDateTime(), LocalTimeZone);
                meeting.setCreatedDateTime(convertedDate);
            }

            if (meeting.getLastUpdatedDateTime() != null) {
                LocalDateTime convertedDate = DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(meeting.getLastUpdatedDateTime(), LocalTimeZone);
                meeting.setLastUpdatedDateTime(convertedDate);
            }

        }
    }

    /**
     * This method compares any two objects of any types for similar fields only using a list containing fields to compare
     * NOTE: this method can throw errors if both objects contain such fields with same name but different data types
     */
    public static boolean compareObjects(Object obj1, Object obj2, List<String> fieldsToCompare) throws IllegalAccessException {
        Class<?> clazz1 = obj1.getClass();
        Class<?> clazz2 = obj2.getClass();

        for (String fieldName : fieldsToCompare) {
            try {
                Field field1 = clazz1.getDeclaredField(fieldName);
                Field field2 = clazz2.getDeclaredField(fieldName);

                // Ensure both fields are accessible
                field1.setAccessible(true);
                field2.setAccessible(true);

                // Compare the values of the fields
                Object value1 = field1.get(obj1);
                Object value2 = field2.get(obj2);

                if ((value1 == null && value2 != null) || (value2 == null && value1 != null)) {
                    return false;
                }
                if (!value1.equals(value2)) {
                    return false;
                }

            } catch (NoSuchFieldException e) {
                // Ignore fields that don't exist in either object
            }
        }

        return true;
    }

    /**
     * This method copy fields of one object to another if the fields are same otherwise omit different fields and set them as null
     */

    public static void copySimilarFields(Object obj1, Object obj2) {
        // Get the Class objects of both obj1 and obj2
        Class<?> class1 = obj1.getClass();
        Class<?> class2 = obj2.getClass();

        // Get an array of Field objects from the Class object of obj1
        Field[] fields1 = class1.getDeclaredFields();

        // Loop through the array of Field objects and check if the field exists in obj2
        Arrays.stream(fields1).forEach(field1 -> {
            try {
                Field field2 = class2.getDeclaredField(field1.getName());

                // Set the value of the field in obj2 to the value of the corresponding field in obj1
                field1.setAccessible(true);
                field2.setAccessible(true);
                field2.set(obj2, field1.get(obj1));
            } catch (Exception e) {
                // The field does not exist in obj2, so skip it
            }
        });
    }

    public MeetingResponse createMeetingResponseFromMeeting(Meeting meetingDb, String desiredTimeZone) {

        MeetingResponse meetingResponse = new MeetingResponse();
        copySimilarFields(meetingDb, meetingResponse);

        List<LabelResponse> labels = new ArrayList<>();
        if (meetingDb.getMeetingLabels() != null && !meetingDb.getMeetingLabels().isEmpty()) {
            meetingDb.getMeetingLabels().parallelStream().forEach( label -> {
                LabelResponse labelResponse = new LabelResponse();
                labelResponse.setLabelName(label.getLabelName());
                labelResponse.setLabelId(label.getLabelId());
                labels.add(labelResponse);
            });
            meetingResponse.setLabels(labels);
        }

        convertAllMeetingServerDateAndTimeToLocalTimeZone(meetingResponse, desiredTimeZone);

        List<ActionItem> actionItems = actionItemRepository.findByFkMeetingIdMeetingIdAndIsDeleted(meetingDb.getMeetingId(), false);
        meetingResponse.setActionItems(getActionItemsForResponse(actionItems, desiredTimeZone));

        // added as correction in meeting iteration 2
        if (meetingDb.getRecurringMeeting() != null) {
            meetingResponse.setRecurringMeetingId(meetingDb.getRecurringMeeting().getRecurringMeetingId());
            meetingResponse.setRecurEvery(meetingDb.getRecurringMeeting().getRecurEvery());
            meetingResponse.setRecurDays(meetingDb.getRecurringMeeting().getRecurDays());
        }

        // set entity name (team name) for the meeting
        if (meetingResponse.getTeamId() != null) {
            String teamName = teamRepository.findTeamNameByTeamId(meetingResponse.getTeamId());
            meetingResponse.setEntityName(teamName);
        } else if (meetingResponse.getProjectId() != null) {
            String projectName = projectRepository.findProjectNameByProjectId(meetingResponse.getProjectId());
            meetingResponse.setEntityName(projectName);
        } else {
            String orgName = organizationRepository.findOrganizationNameByOrgId(meetingResponse.getOrgId());
            meetingResponse.setEntityName(orgName);
        }

        // set meetingType in meetingResponse
        meetingResponse.setMeetingType(setMeetingType(meetingDb.getMeetingTypeIndicator()));

        // set attendees List in meetingResponse
        if (meetingDb.getAttendeeId() != null && meetingDb.getAttendeeList() != null) {
            meetingResponse.setAttendeeRequestList(attendeeService.removeDeletedAttendees(meetingDb.getAttendeeList()));
        } else {     // added an else part in task 2676 to return empty attendee list.
            meetingResponse.setAttendeeRequestList(Collections.emptyList());
        }
        meetingResponse.setModelFetchedList(meetingDb.getModelFetchedList());
        List<MeetingNote> meetingNoteList = meetingDb.getMeetingNotes();
        if (meetingNoteList != null && !meetingNoteList.isEmpty()) {
            meetingResponse.setMeetingNoteResponseList(getMeetingNotesForResponse (meetingNoteList, desiredTimeZone));
        }
        meetingResponse.setUploadFileForModelResponseList(getUploadedFileForModel (meetingDb, desiredTimeZone));
        meetingResponse.setViewTranscription(meetingDb.getViewTranscription());

        return meetingResponse;

    }

    public List<MeetingResponse> bulkCreateMeetingResponseFromMeeting(List<Meeting> meetingDbList, String desiredTimeZone) {

        List<MeetingResponse> meetingResponses = new ArrayList<>();

        Set<Long> teamIds = new HashSet<>();
        Map<Long, Team> teamMap = new HashMap<>();

        Set<Long> projectIds = new HashSet<>();
        Map<Long, Project> projectMap = new HashMap<>();

        Set<Long> orgIds = new HashSet<>();
        Map<Long, Organization> orgMap = new HashMap<>();

        meetingDbList.forEach(meeting -> {
            if (meeting.getTeamId() != null) {
                teamIds.add(meeting.getTeamId());
            } else if (meeting.getProjectId() != null) {
                projectIds.add(meeting.getProjectId());
            } else {
                orgIds.add(meeting.getOrgId());
            }
        });
        if (!teamIds.isEmpty()) {
            List<Team> teamList = teamRepository.findByTeamIdIn(new ArrayList<>(teamIds));
            teamMap = teamList.stream().collect(Collectors.toMap(Team::getTeamId, team -> team));
        }
        if (!projectIds.isEmpty()) {
            List<Project> projectList = projectRepository.findByProjectIdIn(new ArrayList<>(projectIds));
            projectMap = projectList.stream().collect(Collectors.toMap(Project::getProjectId, project -> project));
        }
        if (!orgIds.isEmpty()) {
            List<Organization> orgList = organizationRepository.findByOrgIdIn(new ArrayList<>(orgIds));
            orgMap = orgList.stream().collect(Collectors.toMap(Organization::getOrgId, org -> org));
        }
        List<Long> meetingIdList = meetingDbList.stream().map(Meeting::getMeetingId).collect(Collectors.toList());
        List<ActionItem> actionItemList = actionItemRepository.findActionsByMeetingIdInAndIsDeleted(meetingIdList, false);
        Map<Long, List<ActionItem>> meetingIdActionItemMap = actionItemList.stream().collect(Collectors.groupingBy(action -> action.getMeeting().getMeetingId()));



        for(Meeting meetingDb : meetingDbList){
            MeetingResponse meetingResponse = new MeetingResponse();
            copySimilarFields(meetingDb, meetingResponse);

            List<LabelResponse> labels = new ArrayList<>();
            if (meetingDb.getMeetingLabels() != null && !meetingDb.getMeetingLabels().isEmpty()) {
                meetingDb.getMeetingLabels().forEach(label -> {
                    LabelResponse labelResponse = new LabelResponse();
                    labelResponse.setLabelName(label.getLabelName());
                    labelResponse.setLabelId(label.getLabelId());
                    labels.add(labelResponse);
                });
                meetingResponse.setLabels(labels);
            }

            convertAllMeetingServerDateAndTimeToLocalTimeZone(meetingResponse, desiredTimeZone);

//            List<ActionItem> actionItems = new ArrayList<>();
            meetingResponse.setActionItems(meetingIdActionItemMap.get(meetingResponse.getMeetingId()) != null ? getActionItemsForResponse(meetingIdActionItemMap.get(meetingResponse.getMeetingId()), desiredTimeZone) : new ArrayList<>());

            // added as correction in meeting iteration 2
            if (meetingDb.getRecurringMeeting() != null) {
                meetingResponse.setRecurringMeetingId(meetingDb.getRecurringMeeting().getRecurringMeetingId());
                meetingResponse.setRecurEvery(meetingDb.getRecurringMeeting().getRecurEvery());
                meetingResponse.setRecurDays(meetingDb.getRecurringMeeting().getRecurDays());
            }

            // set entity name (team name) for the meeting
            if (meetingResponse.getTeamId() != null) {
                String teamName = teamMap.get(meetingResponse.getTeamId()).getTeamName();
                meetingResponse.setEntityName(teamName);
            } else if (meetingResponse.getProjectId() != null) {
                String projectName = projectMap.get(meetingResponse.getProjectId()).getProjectName();
                meetingResponse.setEntityName(projectName);
            } else {
                String orgName = orgMap.get(meetingResponse.getOrgId()).getOrganizationName();
                meetingResponse.setEntityName(orgName);
            }

            // set meetingType in meetingResponse
            meetingResponse.setMeetingType(setMeetingType(meetingDb.getMeetingTypeIndicator()));

            // set attendees List in meetingResponse
            if (meetingDb.getAttendeeId() != null && meetingDb.getAttendeeList() != null) {
                meetingResponse.setAttendeeRequestList(attendeeService.removeDeletedAttendees(meetingDb.getAttendeeList()));
            } else {     // added an else part in task 2676 to return empty attendee list.
                meetingResponse.setAttendeeRequestList(Collections.emptyList());
            }
            meetingResponse.setModelFetchedList(meetingDb.getModelFetchedList());
            List<MeetingNote> meetingNoteList = meetingDb.getMeetingNotes();
            if (meetingNoteList != null && !meetingNoteList.isEmpty()) {
                meetingResponse.setMeetingNoteResponseList(getMeetingNotesForResponse(meetingNoteList, desiredTimeZone));
            }
            meetingResponse.setUploadFileForModelResponseList(getUploadedFileForModel(meetingDb, desiredTimeZone));
            meetingResponse.setViewTranscription(meetingDb.getViewTranscription());
            meetingResponses.add(meetingResponse);
        }
        return meetingResponses;

    }

    private List<UploadFileForModelResponse> getUploadedFileForModel(Meeting meeting, String timeZone) {
        List<MeetingAnalysisUploadedFile> meetingAnalysisUploadedFileList = meetingAnalysisUploadedFileRepository.findByMeetingId(meeting.getMeetingId());
        List<UploadFileForModelResponse> uploadFileForModelResponseList = new ArrayList<>();
        if (meetingAnalysisUploadedFileList != null && !meetingAnalysisUploadedFileList.isEmpty()) {
            for (MeetingAnalysisUploadedFile meetingAnalysisUploadedFile : meetingAnalysisUploadedFileList) {
                UploadFileForModelResponse uploadFileForModelResponse = new UploadFileForModelResponse();
                BeanUtils.copyProperties(meetingAnalysisUploadedFile, uploadFileForModelResponse);

                uploadFileForModelResponse.setUploadedDateTime(DateTimeUtils.convertServerDateToUserTimezone(uploadFileForModelResponse.getUploadedDateTime(), timeZone));
                EmailFirstLastAccountIdIsActive emailFirstLastAccountIdIsActive = userAccountRepository.getEmailFirstNameLastNameAccountIdIsActiveByAccountId(meetingAnalysisUploadedFile.getUploaderAccountId());
                uploadFileForModelResponse.setUploaderUserAccountDetails(emailFirstLastAccountIdIsActive);
                uploadFileForModelResponse.setModelFetchedDtoList(meeting.getModelFetchedList());
                uploadFileForModelResponseList.add(uploadFileForModelResponse);
            }
        }
        return uploadFileForModelResponseList;
    }

    /**
     * This method is used to validate fields of a Meeting and updates reference task
     */
    public boolean validateMeetingAndUpdateReferenceTask(MeetingRequest meeting, String accountIds, Meeting meetDb) {

        //validate isFetched
        if (meeting.getIsFetched() == null) {
            meeting.setIsFetched(false);
        }

        // Validate Meeting Type
        if (meeting.getMeetingType() != null) {
            if (!Objects.equals(meeting.getMeetingType(), MeetingType.ONLINE) && !Objects.equals(meeting.getMeetingType(), MeetingType.OFFLINE) && !Objects.equals(meeting.getMeetingType(), MeetingType.HYBRID)) {
                throw new ValidationFailedException("Invalid meeting type");
            }

            // Validate Meeting key
            if ((meeting.getMeetingType().equals(MeetingType.ONLINE) || meeting.getMeetingType().equals(MeetingType.HYBRID)) && (meeting.getMeetingKey() == null || meeting.getMeetingKey().length() < 3)) {
                throw new ValidationFailedException("Meeting key cannot be null or less than 3 character for an ONLINE or HYBRID meeting");
            } else if ((meeting.getMeetingType().equals(MeetingType.OFFLINE) || meeting.getMeetingType().equals(MeetingType.HYBRID)) && (meeting.getVenue() == null || meeting.getVenue().length() < 3)) {
                throw new ValidationFailedException("meeting venue cannot be null or less than 3 character for a OFFLINE or HYBRID meeting");
            } else if (meeting.getMeetingType().equals(MeetingType.OFFLINE) && meeting.getMeetingKey() != null) {
                throw new ValidationFailedException("Meeting key must be null for an offline meeting");
            }
        }

        // Validate reference task
        if (meeting.getReferenceEntityNumber() != null) {
            Task referenceTask = taskServiceImpl.findTaskByTeamIdAndTaskNumber(meeting.getTeamId(), meeting.getReferenceEntityNumber());

            if (referenceTask != null) {

                if (referenceTask.getChildTaskIds() != null && !referenceTask.getChildTaskIds().isEmpty())
                    throw new ForbiddenException("Currently, we can’t attach a reference meeting to a Parent Task. In case this is required, create a Child Task and attach this reference meeting to that Child Task.");
                List<String> disallowedWorkflowStatus = Arrays.asList(com.tse.core_application.model.Constants.WorkFlowTaskStatusConstants.STATUS_COMPLETED_TITLE_CASE, com.tse.core_application.model.Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE);
                if (disallowedWorkflowStatus.contains(referenceTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus())) {
                    if (meeting.getIsCancelled() == null || !meeting.getIsCancelled())
                        throw new ValidationFailedException("You can not create a reference meeting for Work Item in workflow status: " + referenceTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus());
                }

                if (meeting.getReferencedMeetingReasonId() != null && com.tse.core_application.model.Constants.ReferencedMeetingReasonEnum.getById(meeting.getReferencedMeetingReasonId()) != null) {
                    if (!isMeetingCreatorValid(referenceTask, accountIds)) {
                        throw new ValidationFailedException("You are not authorized to create or update meeting for referenced Work Item number : " + referenceTask.getTaskNumber());
                    }
                    if (meeting.getTeamId() == null) {
                        throw new ValidationFailedException("A reference meeting must be associated with a team.");
                    }
                    if (!Objects.equals(referenceTask.getFkTeamId().getTeamId(), meeting.getTeamId())) {
                        throw new ValidationFailedException("The reference Work Item does not belong to the same team as the meeting. Please ensure that both Work Item and meeting are associated with the correct team.");
                    }
                } else {
                    throw new IllegalStateException("Please provide a reason to create meeting associated to the Work Item " + referenceTask.getTaskNumber());
                }
                if (meeting.getDuration() != null && meeting.getStartDateTime() != null) {
                    if ((referenceTask.getTaskExpStartDate() != null && meeting.getStartDateTime().isBefore(referenceTask.getTaskExpStartDate())) ||
                            referenceTask.getTaskExpEndDate() != null && meeting.getEndDateTime().isAfter(referenceTask.getTaskExpEndDate())) {
                        throw new ValidationFailedException("Meeting date range is outside of Work item exp date range");
                    }
                }
                //meetDB null means request is for create meeting
                //Other cases are update meeting case: in that inner validation will only happen if referenceEntityNumber in updated meeting is not same as db meeting.
                if (Objects.isNull(meetDb) || (Objects.nonNull(meetDb) && Objects.isNull(meetDb.getReferenceEntityNumber())) || (Objects.nonNull(meetDb) && Objects.nonNull(meetDb.getReferenceEntityNumber()) && !meetDb.getReferenceEntityNumber().equalsIgnoreCase(meeting.getReferenceEntityNumber()))) {
                    List<Long> updatedMeetingList = referenceTask.getMeetingList() != null ? new ArrayList<>(referenceTask.getMeetingList()) : new ArrayList<>();
                    if (updatedMeetingList.size() == workItemMeetingsSizeLimit && (Objects.isNull(meeting.getIsCancelled()) || (Objects.nonNull(meeting.getIsCancelled()) && !meeting.getIsCancelled()))) {
                        throw new ValidationFailedException("You can not schedule more meetings for this work-item. As maximum limit of " + workItemMeetingsSizeLimit + " meetings already reached!");
                    }
                }
                if (Objects.isNull(meeting.getTeamId()) && (Objects.isNull(meeting.getIsCancelled()) || (Objects.nonNull(meeting.getIsCancelled()) && !meeting.getIsCancelled()))) {
                    throw new ValidationFailedException("You cannot associate a work item in the meeting which is at Project or Organization level");
                }
            } else {
                throw new ValidationFailedException("There is no Work Item with Work Item number " + meeting.getReferenceEntityNumber());
            }
        }

        LocalDate currDate = LocalDate.now();
        LocalDate startDate = meeting.getStartDateTime().toLocalDate();

        LocalDate maxValidDate = currDate.minusDays(Constants.Meeting_Preferrences.PAST_MEETING_DAYS_LIMIT);

        if (startDate.isBefore(maxValidDate)) {
            throw new ValidationFailedException("You cannot create/update meeting for past date before 8 days from now");
        }

        return true;
    }

    /**
     * This method is used to validate of a Recurring Meeting
     */
    public boolean validateRecurringMeeting(RecurringMeetingRequest recurringMeeting) {

        if (recurringMeeting.getMeetingType() != null) {
            if (!Objects.equals(recurringMeeting.getMeetingType(), MeetingType.ONLINE) && !Objects.equals(recurringMeeting.getMeetingType(), MeetingType.OFFLINE) && !Objects.equals(recurringMeeting.getMeetingType(), MeetingType.HYBRID)) {
                throw new ValidationFailedException("Invalid meeting type");
            }


            // Validate Meeting key
            if ((recurringMeeting.getMeetingType().equals(MeetingType.ONLINE) || recurringMeeting.getMeetingType().equals(MeetingType.HYBRID)) && recurringMeeting.getMeetingKey() == null) {
                throw new ValidationFailedException("Meeting key cannot be null for an ONLINE or HYBRID meeting");
            } else if ((recurringMeeting.getMeetingType().equals(MeetingType.OFFLINE) || recurringMeeting.getMeetingType().equals(MeetingType.HYBRID)) && recurringMeeting.getVenue() == null) {
                throw new ValidationFailedException("meeting venue cannot be null for a OFFLINE or HYBRID meeting");
            } else if (recurringMeeting.getMeetingType().equals(MeetingType.OFFLINE) && recurringMeeting.getMeetingKey() != null) {
                throw new ValidationFailedException("Meeting key must be null for an offline meeting");
            }


        }

        return true;

    }

    /**
     * This method is used to calculate endDateTime based on the duration and startDateTime in request
     */
    public LocalDateTime calculateEndDateTimeForMeeting(LocalDateTime startDateTime, Integer meetingDuration) {

        Duration duration = Duration.ofMinutes(meetingDuration);
        return startDateTime.plus(duration);
    }

    /**
     * This method add meeting in meeting table and also adds attendee in attendee table
     */
    public MeetingResponse addMeeting(MeetingRequest meeting, RecurringMeeting savedRecurringMeeting, String timeZone, boolean allowNotification) throws IllegalAccessException {
        if (meeting.getReferenceEntityNumber() != null)
            meeting.setReferenceEntityNumber(meeting.getReferenceEntityNumber().toUpperCase());
        Meeting meetingToSave = new Meeting();
        Meeting savedMeeting = null;
        MeetingResponse response = new MeetingResponse();
        // set default value as false for isCancelled
        meeting.setIsCancelled(false);

        //set default value of isFetched false
        meeting.setIsFetched(false);
        Long meetingSequence = getNextMeetingIdentifier(meeting.getOrgId());
        if (meeting.getMeetingTypeId().equals(com.tse.core_application.constants.MeetingType.MEETING.getValue())) {
            meeting.setMeetingNumber("M-" + meetingSequence);
        } else if (meeting.getMeetingTypeId().equals(com.tse.core_application.constants.MeetingType.COLLABORATION.getValue())) {
            meeting.setMeetingNumber("C-" + meetingSequence);
        }
        copySimilarFields(meeting, meetingToSave);

        //set Meeting progress
        if (meetingToSave.getStartDateTime() != null) {
            meetingToSave.setMeetingProgress(MeetingStats.MEETING_SCHEDULED);
        }

        // compute & set endDateTime
        meetingToSave.setEndDateTime(calculateEndDateTimeForMeeting(meeting.getStartDateTime(), meeting.getDuration()));

        // set meetingTypeIndicator
        meetingToSave.setMeetingTypeIndicator(getMeetingTypeIndicator(meeting.getMeetingType()));

        // Added in task 2676 for recurring meeting
        if (savedRecurringMeeting != null) {
            meetingToSave.setRecurringMeeting(savedRecurringMeeting);
        }

        //To pass attendees List to notification for meeting invitation
        List<Attendee> savedAttendee = new ArrayList<>();
        addLabelsToMeeting(meetingToSave, meeting.getLabelsToAdd());
        savedMeeting = meetingRepository.save(meetingToSave);
        if (savedMeeting.getMeetingId() != null) {
            savedAttendee = attendeeService.addAttendee(meeting.getAttendeeRequestList(), savedMeeting);
            savedMeeting.setAttendeeList(savedAttendee);
            if (!savedAttendee.isEmpty()) {
                Integer resultSet = updateAttendeeIdByMeetingId(savedAttendee.get(0).getAttendeeId(), savedMeeting.getMeetingId());
            }
        }

        // Capacity calculation for reference meeting
        if (meeting.getReferenceEntityNumber() != null) {
            updateMeetingIdsInTaskOnCreateMeeting(meeting.getReferenceEntityNumber(), savedMeeting);
        }

        auditService.auditForMeeting(userAccountRepository.findByAccountIdAndIsActive(savedMeeting.getCreatedAccountId(), true), savedMeeting, false);

        if (allowNotification) {
            try {
                //create meeting invite payload
                List<HashMap<String, String>> meetingPayload = notificationService.newMeetingNotification(savedMeeting, timeZone, savedAttendee);
                //  pass this payload to fcm for notification
                taskServiceImpl.sendPushNotification(meetingPayload);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        response = createMeetingResponseFromMeeting(savedMeeting, timeZone);
        return response;
    }

    private void updateMeetingIdsInTaskOnCreateMeeting(String referenceTaskNumber, Meeting savedMeeting) {
        Task referenceTask = getReferenceTaskFromMeeting(savedMeeting);
        List<Long> meetingIds = new ArrayList<>();
        List<Long> updatedMeetingList = new ArrayList<>();

        if (referenceTask != null) {
            updatedMeetingList = referenceTask.getMeetingList() != null ? new ArrayList<>(referenceTask.getMeetingList()) : new ArrayList<>();
            Task referenceTaskCopy = new Task();
            BeanUtils.copyProperties(referenceTask, referenceTaskCopy);
            taskHistoryService.addTaskHistoryOnSystemUpdate(referenceTaskCopy);
            updatedMeetingList.add(savedMeeting.getMeetingId());
            referenceTask.setMeetingList(updatedMeetingList);
            taskRepository.save(referenceTask);
            List<String> updateFields = new ArrayList<>();
            updateFields.add(com.tse.core_application.model.Constants.TaskFields.REFERENCE_MEETING);
            taskHistoryMetadataService.addTaskHistoryMetadata(updateFields, referenceTask);
        }
        capacityService.updateReferenceMeetingCapacity(referenceTask, referenceTask.getSprintId(), savedMeeting);
    }

    //To remove the meeting-Id from meeting list of work-item and update capacity
    private void removeMeetingIdInTaskOnUpdateMeeting(Meeting meetingDb) {
        Task oldReferenceTask = getReferenceTaskFromMeeting(meetingDb);
        List<Long> updatedMeetingList = new ArrayList<>();
        if (oldReferenceTask != null) {
            updatedMeetingList = oldReferenceTask.getMeetingList() != null ? new ArrayList<>(oldReferenceTask.getMeetingList()) : new ArrayList<>();
            Task referenceTaskCopy = new Task();
            BeanUtils.copyProperties(oldReferenceTask, referenceTaskCopy);
            taskHistoryService.addTaskHistoryOnSystemUpdate(referenceTaskCopy);
            if (!updatedMeetingList.isEmpty() && updatedMeetingList.contains(meetingDb.getMeetingId())) {
                Integer effortsToRemove = meetingDb.getAttendeeList().stream().filter(attendee -> attendee.getAttendeeDuration()!=null && attendee.getAttendeeDuration() > 0)
                        .map(Attendee::getAttendeeDuration).reduce(0, Integer::sum);
                updatedMeetingList.remove(meetingDb.getMeetingId());
                oldReferenceTask.setMeetingList(updatedMeetingList);
                oldReferenceTask.setTotalEffort(oldReferenceTask.getTotalEffort() != null ? Math.max(oldReferenceTask.getTotalEffort() - effortsToRemove, 0) : null);
                oldReferenceTask.setTotalMeetingEffort(oldReferenceTask.getTotalMeetingEffort() != null ? Math.max(oldReferenceTask.getTotalMeetingEffort() - effortsToRemove, 0) : null);
                taskRepository.save(oldReferenceTask);
            }
            List<String> updateFields = new ArrayList<>();
            updateFields.add(com.tse.core_application.model.Constants.TaskFields.REFERENCE_MEETING);
            taskHistoryMetadataService.addTaskHistoryMetadata(updateFields, oldReferenceTask);
            capacityService.updateReferenceMeetingCapacityOnRemoveTaskFromSprint(oldReferenceTask, oldReferenceTask.getSprintId());
        }
    }

    //To update the meeting-Id from meeting list of work-item and update capacity
    private void updateMeetingIdsInTaskOnUpdateMeeting(Meeting meetingDb, Meeting meetingDbUpdated) {
        //Do not do anything when both the meetings do not have any work-item reference or both the meetings have same work-items
        if ((Objects.isNull(meetingDb.getReferenceEntityNumber()) && Objects.isNull(meetingDbUpdated.getReferenceEntityNumber())) || (Objects.nonNull(meetingDb.getReferenceEntityNumber()) && Objects.nonNull(meetingDbUpdated.getReferenceEntityNumber()) && meetingDb.getReferenceEntityNumber().equalsIgnoreCase(meetingDbUpdated.getReferenceEntityNumber()))) {
            return;
        }
        //Remove work-item from old meeting if it exists
        if (Objects.nonNull(meetingDb.getReferenceEntityNumber())) {
            removeMeetingIdInTaskOnUpdateMeeting(meetingDb);
        }
        //Update work-item from meeting request if it exists
        if (Objects.nonNull(meetingDbUpdated.getReferenceEntityNumber())) {
            //Update work-item in the meeting if their do not exist any work-item in the meeting earlier
            Task updatedReferenceTask = getReferenceTaskFromMeeting(meetingDbUpdated);
            List<Long> updatedMeetingList = new ArrayList<>();
            if (updatedReferenceTask != null) {
                updatedMeetingList = updatedReferenceTask.getMeetingList() != null ? new ArrayList<>(updatedReferenceTask.getMeetingList()) : new ArrayList<>();
                Task referenceTaskCopy = new Task();
                BeanUtils.copyProperties(updatedReferenceTask, referenceTaskCopy);
                taskHistoryService.addTaskHistoryOnSystemUpdate(referenceTaskCopy);
                if (updatedMeetingList.isEmpty() || (Objects.nonNull(updatedMeetingList) && !updatedMeetingList.isEmpty() && !updatedMeetingList.contains(meetingDbUpdated.getMeetingId()))) {
                    updatedMeetingList.add(meetingDbUpdated.getMeetingId());
                    updatedReferenceTask.setMeetingList(updatedMeetingList);
                    if(meetingDbUpdated.getAttendeeList()!=null && !meetingDbUpdated.getAttendeeList().isEmpty()){
                        Integer effortsToAdd = meetingDbUpdated.getAttendeeList().stream().filter(attendee -> attendee.getAttendeeDuration()!=null && attendee.getAttendeeDuration() > 0)
                                .map(Attendee::getAttendeeDuration).reduce(0, Integer::sum);
                        updatedReferenceTask.setTotalEffort((updatedReferenceTask.getTotalEffort() != null ? updatedReferenceTask.getTotalEffort() : 0) + effortsToAdd);
                        updatedReferenceTask.setTotalMeetingEffort((updatedReferenceTask.getTotalMeetingEffort() != null ? updatedReferenceTask.getTotalMeetingEffort() : 0) + effortsToAdd);
                    }
                    taskRepository.save(updatedReferenceTask);
                }
                List<String> updateFields = new ArrayList<>();
                updateFields.add(com.tse.core_application.model.Constants.TaskFields.REFERENCE_MEETING);
                taskHistoryMetadataService.addTaskHistoryMetadata(updateFields, updatedReferenceTask);
            }
            capacityService.updateReferenceMeetingCapacity(updatedReferenceTask, updatedReferenceTask.getSprintId(), meetingDbUpdated);
        }
    }

    /**
     * method to get the next number in sequence for the new meeting in the organization
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long getNextMeetingIdentifier(Long orgId) {
        //previous code logic using the Pessimistic Locking of the sequence which caused Locking Exception
        // on key When the meeting created at the same time in the organization.
        //To-Do: same thing we have to do in the recurring meeting sequence generator.
        String sql = " INSERT INTO tse.meeting_sequence as ms (org_id, last_meeting_identifier) " +
                " VALUES (:orgId, 1) " +
                " ON CONFLICT (org_id) " +
                " DO UPDATE SET last_meeting_identifier = ms.last_meeting_identifier + 1 " +
                " RETURNING last_meeting_identifier ";

        Number result = (Number) entityManager.createNativeQuery(sql)
                .setParameter("orgId", orgId)
                .getSingleResult();
        return result.longValue();
    }

    /**
     * method to get the next number in sequence for the new recurring meeting in the organization
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long getNextRecurringMeetingIdentifier(Long orgId) {
        RecurringMeetingSequence sequence = recurringMeetingSequenceRepository.findByOrgIdForUpdate(orgId);

        if (sequence == null) {
            sequence = new RecurringMeetingSequence(orgId, 0L);
        }
        Long nextRecurringMeetingIdentifier = sequence.getLastRecurringMeetingIdentifier() + 1;
        sequence.setLastRecurringMeetingIdentifier(nextRecurringMeetingIdentifier);
        recurringMeetingSequenceRepository.save(sequence);
        return nextRecurringMeetingIdentifier;
    }

    /**
     * this methdd is used to add labels to a meeting and save the label object
     */
    private void addLabelsToMeeting(Meeting meeting, List<String> labelsToAdd) {
        if (labelsToAdd == null || labelsToAdd.isEmpty()) return;

        List<Label> currentLabels = meeting.getMeetingLabels();
        if (currentLabels == null) {
            currentLabels = new ArrayList<>();
        }
        List<Label> processedLabels = new ArrayList<>();
        List<String> labelsInRequest = new ArrayList<>();
        Long foundEntityId = 0L;
        Integer foundEntityTypeId = 0;
        if (meeting.getTeamId() != null) {
            foundEntityId = meeting.getTeamId();
            foundEntityTypeId = com.tse.core_application.model.Constants.EntityTypes.TEAM;
        } else if (meeting.getProjectId() != null) {
            foundEntityId = meeting.getProjectId();
            foundEntityTypeId = com.tse.core_application.model.Constants.EntityTypes.PROJECT;
        } else if (meeting.getBuId() != null) {
            foundEntityId = meeting.getBuId();
            foundEntityTypeId = com.tse.core_application.model.Constants.EntityTypes.BU;
        } else if (meeting.getOrgId() != null) {
            foundEntityId = meeting.getOrgId();
            foundEntityTypeId = com.tse.core_application.model.Constants.EntityTypes.ORG;
        } else {
            throw new ValidationFailedException("Entity does not exist!!");
        }
        for (String labelName : labelsToAdd) {
            final String formattedLabelName = labelName.trim().replaceAll("\\s+", " ");
//            final String formattedLabelName = Arrays.stream(labelName.toLowerCase().split(" "))
//                    .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
//                    .collect(Collectors.joining(" "));

            boolean labelExists = currentLabels.stream()
                    .anyMatch(label -> label.getLabelName().equalsIgnoreCase(formattedLabelName)) || labelsInRequest.stream()
                    .anyMatch(label -> label.equalsIgnoreCase(formattedLabelName));

            labelsInRequest.add(formattedLabelName);

            if (!labelExists) {
                Label label = labelRepository.findByLabelNameIgnoreCaseAndEntityTypeIdAndEntityId(labelName, foundEntityTypeId, foundEntityId);
                if (label == null) {
                    label = new Label();
                    label.setLabelName(labelName);
                    label.setEntityId(foundEntityId);
                    label.setEntityTypeId(foundEntityTypeId);
                    label.getMeetings().add(meeting);
                    label = labelRepository.save(label);
                } else {
                    label.getMeetings().add(meeting);
                }
                processedLabels.add(label);
            }
//            currentLabels.addAll(processedLabels);
//            processedLabels.add(label);
        }
        currentLabels.addAll(processedLabels);
//        labelRepository.saveAll(processedLabels);
        meeting.setMeetingLabels(currentLabels);
    }


    /**
     * Used to get meetingTypeIndicator using meetingType from Meeting in request
     */
    public Integer getMeetingTypeIndicator(String meetingType) {

        if (Objects.equals(meetingType.toLowerCase(), MeetingType.ONLINE)) {
            return Constants.Meeting_Type_Indicator.ONLINE;
        } else if (Objects.equals(meetingType.toLowerCase(), MeetingType.OFFLINE)) {
            return Constants.Meeting_Type_Indicator.OFFLINE;
        } else if (Objects.equals(meetingType.toLowerCase(), MeetingType.HYBRID)) {
            return Constants.Meeting_Type_Indicator.HYBRID;
        } else {
            throw new InvalidMeetingTypeException();
        }
    }

    public String setMeetingType(Integer meetingTypeIndicator) {
        if (meetingTypeIndicator == Constants.Meeting_Type_Indicator.ONLINE) {
            return MeetingType.ONLINE;
        } else if (meetingTypeIndicator == Constants.Meeting_Type_Indicator.OFFLINE) {
            return MeetingType.OFFLINE;
        }
        return MeetingType.HYBRID;
    }

    public LocalDateTime getMeetingStartDateTime(LocalDate meetingStartDate, LocalTime meetingStartTime, String desiredTimeZone) {
        LocalDateTime localDateTime = null;
        if (meetingStartDate != null && meetingStartTime != null) {
            localDateTime = LocalDateTime.of(meetingStartDate, meetingStartTime);
        } else {
            localDateTime = LocalDateTime.now();
        }
        LocalDateTime convertedLocalDateTime = convertUserDateToServerTimezone(localDateTime, desiredTimeZone);

        return convertedLocalDateTime;
    }

    public LocalDateTime getMeetingEndDateTime(LocalDate meetingEndDate, LocalTime meetingEndTime, String desiredTimeZone) {
        LocalDateTime localDateTime = null;

        if (meetingEndDate != null && meetingEndTime != null) {
            localDateTime = LocalDateTime.of(meetingEndDate, meetingEndTime);
        } else {
            localDateTime = LocalDateTime.now();
        }
        LocalDateTime convertedLocalDateTime = convertUserDateToServerTimezone(localDateTime, desiredTimeZone);

        return convertedLocalDateTime;
    }

    /**
     * This method is used to get all scheduled meetings between a fromDate to toDate
     * This method selects the meetings based on their start dates only
     */

    public List<MeetingResponse> getAllScheduledMeetings(GetScheduledMeetingRequest getScheduledMeetingRequest, String desiredTimeZone) throws IllegalAccessException {
        List<MeetingResponse> convertedMeetings = new ArrayList<>();

        Long accountId = getScheduledMeetingRequest.getAccountId();
        LocalDateTime fromDate = getScheduledMeetingRequest.getFromDate();
        LocalDateTime toDate = getScheduledMeetingRequest.getToDate();
        if (fromDate == null) {
            fromDate = LocalDateTime.now();
        }
        if (toDate == null) {
            toDate = LocalDateTime.now();
        }
        LocalDateTime convertedFromDate = convertUserDateToServerTimezone(fromDate, desiredTimeZone);
        LocalDateTime convertedToDate = convertUserDateToServerTimezone(toDate, desiredTimeZone);
        LocalDateTime convertedFromEndDateTime = convertedFromDate.minusSeconds(1);
        LocalDateTime convertedToEndDateTime = convertedToDate.plusSeconds(1);
        List<Long> attendeeIds = meetingRepository.findByAccountId(accountId);
//        List<Meeting> meetings = meetingRepository.findByStartDateTimeBetweenAndEndDateTimeBetweenAndAttendeeIdIn(convertedFromDate, convertedToDate, convertedFromEndDateTime, convertedToEndDateTime, attendeeIds);
        List<Meeting> meetings = meetingRepository.findByStartDateTimeGreaterThanEqualAndStartDateTimeLessThanEqual(convertedFromDate, convertedToDate);

        for (Meeting meeting : meetings) {
            MeetingResponse convertedMeeting = new MeetingResponse();
            copySimilarFields(meeting, convertedMeeting);

            List<LabelResponse> labels = new ArrayList<>();
            if (meeting.getMeetingLabels() != null) {
                for (Label label : meeting.getMeetingLabels()) {
                    LabelResponse labelResponse = new LabelResponse();
                    labelResponse.setLabelName(label.getLabelName());
                    labelResponse.setLabelId(label.getLabelId());
                    labels.add(labelResponse);
                }
            }
            convertedMeeting.setLabels(labels);

            convertAllMeetingServerDateAndTimeToLocalTimeZone(convertedMeeting, desiredTimeZone);
            convertedMeeting.setMeetingType(setMeetingType(meeting.getMeetingTypeIndicator()));

            // set attendees List in meetingResponse
            if (meeting.getAttendeeId() != null && meeting.getAttendeeList() != null) {
                convertedMeeting.setAttendeeRequestList(attendeeService.removeDeletedAttendees(meeting.getAttendeeList()));
            }

            convertedMeetings.add(convertedMeeting);
        }
        return convertedMeetings;
    }

    /**
     * This method updates the attendeeId associated with a meeting in Meeting table
     */
    public Integer updateAttendeeIdByMeetingId(Long attendeeId, Long meetingId) {
        return meetingRepository.setAttendeeIdByMeetingId(attendeeId, meetingId);
    }

    /**
     * This method is used to retrieve a meeting through meeting Id from database and also gets the updated attendee list
     */
    public MeetingResponse getMeetingByMeetingId(Long meetingId, String accountIds, String desiredTimeZone) throws IllegalAccessException {

        MeetingResponse meetingToGet = null;

        Meeting foundMeetingDb = meetingRepository.findByMeetingId(meetingId);
        if (foundMeetingDb != null) {
            List<Long> accounIdList = CommonUtils.convertToLongList(accountIds);
            if (foundMeetingDb.getTeamId() != null) {
                // team id validation to get meeting
                List<Long> teamIds = List.of(foundMeetingDb.getTeamId());
                List<Long> validTeamIds = getAllValidTeamIdsByInputFilters(null, null, accountIds);
                HashSet<Long> validTeamIdsSet = new HashSet<>(validTeamIds);

                if (!validTeamIdsSet.contains(foundMeetingDb.getTeamId())) {
                    throw new ValidationFailedException("You are not authorized to access the meeting");
                }
            } else if (foundMeetingDb.getProjectId() != null) {
                List<Long> validProjectIds = accessDomainRepository.getProjectInfoByAccountIdsAndIsActiveTrue(accounIdList).stream().map(Project::getProjectId).collect(Collectors.toList());
                if (!validProjectIds.contains(foundMeetingDb.getProjectId())) {
                    throw new ValidationFailedException("You are not authorized to access the meeting");
                }
            } else {
                if (!userAccountRepository.existsByAccountIdInAndOrgIdAndIsActive(accounIdList, foundMeetingDb.getOrgId(), true)) {
                    throw new ValidationFailedException("You are not authorized to access the meeting");
                }
            }

            meetingToGet = createMeetingResponseFromMeeting(foundMeetingDb, desiredTimeZone);
            List<Attendee> attendees = attendeeRepository.findByAttendeeId(foundMeetingDb.getAttendeeId());
            for (Attendee attendee : attendees) {
                if (validateIsEditableForAttendee(attendee, accountIds)) {
                    meetingToGet.setIsEditable(Boolean.TRUE);
                }
            }
            if (!meetingEditAccess(foundMeetingDb.getOrganizerAccountId(), foundMeetingDb.getCreatedAccountId(), foundMeetingDb.getTeamId(), accountIds)) {
                meetingToGet.setCanEditMeeting(false);
            }
            meetingToGet.setAttendeeRequestList(getFilteredAttendees(foundMeetingDb));
            return meetingToGet;
        }
        else {
            return null;
        }


    }


    public List<Attendee> getFilteredAttendees(Meeting meeting) {
        List<Attendee>attendeesFromDb=attendeeRepository.findByAttendeeId(meeting.getAttendeeId());
        List<Attendee> currentAttendeeList = meeting.getAttendeeList();
        List<Attendee> attendeeList = new ArrayList<>();
        if (currentAttendeeList != null && !currentAttendeeList.isEmpty()) {
            for (Attendee currentAttendee : currentAttendeeList) {
                Attendee attendee = attendeeRepository.findByAccountIdAndMeetingMeetingId(currentAttendee.getAccountId(), meeting.getMeetingId());
                if (attendee != null) {
                    attendeeList.add(attendee);
                }
            }
        }
        List<Attendee> removeAttendeeList = attendeeService.removeDeletedAttendees(attendeeList);

        if (removeAttendeeList == null || removeAttendeeList.isEmpty()) {
            return attendeesFromDb.stream()
                    .filter(attendee -> Objects.equals(attendee.getAttendeeInvitationStatusId(), Constants.MeetingAttendeeInvitationStatus.ATTENDEE_INVITED_ID))
                    .collect(Collectors.toList());
        } else {
            return removeAttendeeList.stream()
                    .filter(attendee -> Objects.equals(attendee.getAttendeeInvitationStatusId(), Constants.MeetingAttendeeInvitationStatus.ATTENDEE_INVITED_ID))
                    .collect(Collectors.toList());
        }
    }

    /** This method returns a list of all account ids of attendees which are deleted in the request but present in the database */
    public List<Long> getdeletedAttendeesAccountIdList(MeetingRequest meeting, Meeting meetDb){

        List<Long> deletedAccountIds = new ArrayList<>();
        List<Long> accountIdsDb = new ArrayList<>();
        if(meeting.getAttendeeRequestList().isEmpty())
        {
            throw new ValidationFailedException("Organizer cannot be removed from Attendee List in Meeting!!");
        }
        if(meetDb.getAttendeeList() == null || meetDb.getAttendeeList().isEmpty()){
            return deletedAccountIds;
        }
        //       accountIdsDb = meetDb.getAttendeeList().stream().map(Attendee::getAccountId).collect(Collectors.toList());


        for(Attendee attendeeDb : meetDb.getAttendeeList()){
            if(attendeeDb.getAttendeeInvitationStatusId() != Constants.MeetingAttendeeInvitationStatus.ATTENDEE_DISINVITED_ID){
                accountIdsDb.add(attendeeDb.getAccountId());
            }
        }



        if(meeting.getAttendeeRequestList() != null && !meeting.getAttendeeRequestList().isEmpty()){

            HashSet<Long> accountIdsRequest = meeting.getAttendeeRequestList().stream().map(AttendeeRequest::getAccountId).collect(Collectors.toCollection(HashSet::new));
            Long organizerAccountId=meeting.getOrganizerAccountId();
            if(!accountIdsRequest.contains(organizerAccountId))
            {
                throw new ValidationFailedException("Organizer cannot be removed from Attendee List in Meeting!");
            }


            for(Long accountId : accountIdsDb){
                if(!accountIdsRequest.contains(accountId)){
                    deletedAccountIds.add(accountId);
                }
            }

            return deletedAccountIds;
        }
        else{
            return accountIdsDb;
        }
    }


    /** This method is used to get all fields that are to be updated and are updatable also */

    public ArrayList<String> getMeetingFieldsToUpdate(MeetingRequest meeting, Long meetingId, List<Long> deletedAccountIds){
        Meeting meetingDb = meetingRepository.findByMeetingId(meetingId);

        ArrayList<String> arrayListFields = new ArrayList<String>();

//         LocalDateTime meetingStartDateTime = getMeetingStartDateTime(meeting.getMeetingStartDate(), meeting.getMeetingStartTime(), (String.valueOf(ZoneId.systemDefault())));
//         LocalDateTime meetingEndDateTime = getMeetingEndDateTime(meeting.getMeetingEndDate(), meeting.getMeetingEndTime(),(String.valueOf(ZoneId.systemDefault())));
//         LocalDateTime meetingStartDateTime = convertUserDateToServerTimezone(meeting.getStartDateTime(), (String.valueOf(ZoneId.systemDefault())));
//         LocalDateTime meetingEndDateTime = convertUserDateToServerTimezone(meeting.getEndDateTime(), (String.valueOf(ZoneId.systemDefault())));
//
        if (meetingDb.getStartDateTime() != null && meeting.getStartDateTime() != null){

            if (meetingDb.getStartDateTime().compareTo(meeting.getStartDateTime()) != 0) {
                arrayListFields.add("startDateTime");
            }
        } else {
            if ((meetingDb.getStartDateTime() != null || meeting.getStartDateTime() != null)) {
                arrayListFields.add("startDateTime");
            }
        }

        if (meetingDb.getDuration() != null && meeting.getDuration() != null) {

            if (meetingDb.getDuration().compareTo(meeting.getDuration()) != 0) {
                arrayListFields.add("endDateTime");
            }
        } else {
            if (meetingDb.getDuration() != null  ||  meeting.getDuration() != null) {
                arrayListFields.add("endDateTime");
            }
        }

        if(!Objects.equals(setMeetingType(meetingDb.getMeetingTypeIndicator()), meeting.getMeetingType())){
            arrayListFields.add("meetingTypeIndicator");
        }


        ArrayList<HashMap<String, Object>> arrayList = new ArrayList<HashMap<String, Object>>();
        HashMap<String, Object> mapMeeting = objectMapper.convertValue(meeting, HashMap.class);
        mapMeeting.remove("startDateTime");
        mapMeeting.remove("endDateTime");

        HashMap<String, Object> mapMeetingDb = objectMapper.convertValue(meetingDb, HashMap.class);
        mapMeetingDb.remove("startDateTime");
        mapMeetingDb.remove("endDateTime");

        for(Map.Entry<String, Object> entry : mapMeetingDb.entrySet())
        {
            Object value1 = entry.getValue();
            Object value2;
            String key = entry.getKey();
            if(mapMeeting.containsKey(key)) {
                value2 = mapMeeting.get(key);
                if(value1 == null && value2 == null){
                    continue;
                }
                if (!Objects.equals(value1, value2)){
                    arrayListFields.add(entry.getKey());
                }
            }
        }

        if(meeting.getLabelsToAdd() != null && !meeting.getLabelsToAdd().isEmpty()) {
            arrayListFields.add("labelsToAdd");
        }

        if(meeting.getAttendeeRequestList() != null) {
            if (meeting.getAttendeeRequestList().size() != meetingDb.getAttendeeList().size()) {
                arrayListFields.add("attendeeRequestList");
            } else {
//           List<String> fieldsToCompare = List.of( "attendeeInvitationStatus");
//            try {
//                for (int i = 0; i < meeting.getAttendeeResponseList().size(); ++i) {
//                   if(!compareObjects(meeting.getAttendeeResponseList().get(i), meetingDb.getAttendeeList().get(i), fieldsToCompare)){
//                       arrayListFields.add("attendeeResponseList");
//                   }
//                }
//            }catch (IllegalAccessException e){
//                e.printStackTrace();
//            }
//            try {
//                HashMap<Long, Object> mapAttendeesDb = new HashMap<>();
//                for (Attendee a : meetingDb.getAttendeeList()) {
//                    mapAttendeesDb.put(a.getAccountId(), a);
//                }
//                for (AttendeeRequest attendee : meeting.getAttendeeResponseList()) {
//                    if (attendee.getAttendeeLogId() != null &&  mapAttendeesDb.containsKey(attendee.getAccountId())) {
//                        if (!compareObjects(attendee, mapAttendeesDb.get(attendee.getAccountId()), fieldsToCompare)) {
//                            arrayListFields.add("attendeeResponseList");
//                            break;
//                        }
//                    } else {
//                        arrayListFields.add("attendeeResponseList");
//                        break;
//                    }
//                }
//            }catch (IllegalAccessException e){
//                e.printStackTrace();
//            }
//
//            if(!deletedAccountIds.isEmpty()){
//                arrayListFields.add("attendeeResponseList");
//            }

                for (AttendeeRequest attendee : meeting.getAttendeeRequestList()) {
                    if (attendee.getAttendeeLogId() == null) {
                        arrayListFields.add("attendeeRequestList");
                        break;
                    }
                }
            }
        }

        if (DebugConfig.getInstance().isDebug()) {
            System.out.println("From getFieldsToUpdate = " + arrayListFields);
        }
        return arrayListFields;

    }

   /** This method updates all fields in meeting table and also in attendee table */

    public Meeting updateAllFieldsInMeetingTable(MeetingRequest meeting , Long meetingId, ArrayList<String> updatedMeetingsFieldsByUser, List<Long> deletedAccountIds, RecurringMeeting recurringMeetingUpdated, String timeZone, Boolean allowNotification){

        Meeting meetingDb = meetingRepository.findByMeetingId(meetingId);
        MeetingResponse meetingUpdated = new MeetingResponse();
        Meeting meetingDbUpdated = new Meeting();

// ----------------------------------------------------------------------------------------------------------------------------
        /* Below commented code can be used in future if date and time are given separate in request */
//        LocalDateTime meetingStartDateTime = getMeetingStartDateTime(meeting.getMeetingStartDate(), meeting.getMeetingStartTime(), (String.valueOf(ZoneId.systemDefault())));
//        LocalDateTime meetingEndDateTime = getMeetingEndDateTime(meeting.getMeetingEndDate(), meeting.getMeetingEndTime(),(String.valueOf(ZoneId.systemDefault())));
//
//        if (meetingDb.getStartDateTime() != null && meetingStartDateTime != null) {
//            if (meetingDb.getStartDateTime().compareTo(meetingStartDateTime) != 0) {
//                meetingDb.setStartDateTime(LocalDateTime.of(meeting.getMeetingStartDate(), meeting.getMeetingStartTime()));
//                meetingDb.setStartDateTime(LocalDateTime.of(meeting.getMeetingStartDate(), meeting.getMeetingStartTime()));
//            }
//        } else {
//            if ((meetingDb.getStartDateTime() != null && meetingStartDateTime == null) || (meetingDb.getStartDateTime() == null && meetingStartDateTime != null)) {
//                meetingDb.setStartDateTime(LocalDateTime.of(meeting.getMeetingStartDate(), meeting.getMeetingStartTime()));
//            }
//        }
//
//        if (meetingDb.getEndDateTime() != null && meetingEndDateTime != null) {
//            if (meetingDb.getEndDateTime().compareTo(meetingEndDateTime) != 0) {
//                meetingDb.setEndDateTime(LocalDateTime.of(meeting.getMeetingEndDate(), meeting.getMeetingEndTime()));
//            }
//        } else {
//            if (meetingDb.getEndDateTime() != null && meetingEndDateTime == null || meetingDb.getEndDateTime() == null && meetingEndDateTime != null) {
//                meetingDb.setEndDateTime(LocalDateTime.of(meeting.getMeetingEndDate(), meeting.getMeetingEndTime()));
//            }
//        }

//         if(updatedMeetingsFieldsByUser.contains("startDateTime")){
//             meetingDbUpdated.setStartDateTime(meeting.getStartDateTime());
//             updatedMeetingsFieldsByUser.remove("startDateTime");
//         }
//
//        if(updatedMeetingsFieldsByUser.contains("endDateTime")){
//            meetingDbUpdated.setEndDateTime(meeting.getEndDateTime());
//            updatedMeetingsFieldsByUser.remove("endDateTime");
//        }
   // -----------------------------------------------------------------------------------------------------------------------------

        if (!updatedMeetingsFieldsByUser.isEmpty()) {

            HashMap<String, Object> mapMeeting = objectMapper.convertValue(meeting, HashMap.class);
            HashMap<String, Object> mapMeetingDb = objectMapper.convertValue(meetingDb, HashMap.class);

            for (String field : updatedMeetingsFieldsByUser) {
                if (mapMeetingDb.containsKey(field)) {
                    mapMeetingDb.put(field, mapMeeting.get(field));
                }
            }

            /* These fields are removed from this map as object mapper function used below to convert map to object of meeting cannot deserialize LocalDateTime field, thus all fields with LocalDateTime are removed and set separately.*/
            mapMeetingDb.remove("startDateTime");
            mapMeetingDb.remove("createdDateTime");
            mapMeetingDb.remove("lastUpdatedDateTime");
            mapMeetingDb.remove("endDateTime");
            mapMeetingDb.remove("actualStartDateTime");  // added in task 2851
            mapMeetingDb.remove("actualEndDateTime");
            mapMeetingDb.remove("actionItems");
            mapMeetingDb.remove("attendeeList");
            mapMeetingDb.remove("meetingNotes");

            meetingDbUpdated = objectMapper.convertValue(mapMeetingDb, Meeting.class);

            meetingDbUpdated.setCreatedDateTime(meetingDb.getCreatedDateTime());
            meetingDbUpdated.setStartDateTime(meeting.getStartDateTime());
            meetingDbUpdated.setActualStartDateTime(meetingDb.getActualStartDateTime()); // added in task 2851
            meetingDbUpdated.setActualEndDateTime(meetingDb.getActualEndDateTime());
            meetingDbUpdated.setActionItems(meetingDb.getActionItems());
            meetingDbUpdated.setAttendeeList(meetingDb.getAttendeeList());

            // changed here to calculate end date time based on duration
            meetingDbUpdated.setEndDateTime(calculateEndDateTimeForMeeting(meeting.getStartDateTime(), meeting.getDuration()));
        }

        if(meetingDb.getMeetingLabels() != null) {
            List<Label> savedLabels = meetingDb.getMeetingLabels();
            meetingDbUpdated.setMeetingLabels(savedLabels);
        }

        if(meetingDb.getMeetingNotes() != null){
            meetingDbUpdated.setMeetingNotes(meetingDb.getMeetingNotes());
        }

        if(meetingDbUpdated.getStartDateTime() != null && meetingDbUpdated.getMeetingProgress() == null){
            meetingDbUpdated.setMeetingProgress(MeetingStats.MEETING_SCHEDULED);
        }
        Integer meetingTypeIndicator = getMeetingTypeIndicator(meeting.getMeetingType());
        if(!Objects.equals(meetingDb.getMeetingTypeIndicator(), meetingTypeIndicator)){
            meetingDbUpdated.setMeetingTypeIndicator(meetingTypeIndicator);
        }

        if(recurringMeetingUpdated != null){
            meetingDbUpdated.setRecurringMeeting(recurringMeetingUpdated);
        }

        addLabelsToMeeting(meetingDbUpdated, meeting.getLabelsToAdd());

        // update capacities in case of reference meeting is updated
        boolean isCapacityChangeRequiredForAttendeeModification = false;
        List<Long> accountIdsToUpdateForCapacity = new ArrayList<>();
        Task referenceTask = new Task();
        Set<Long> accountIdsOldMembers = new HashSet<>();

        updateMeetingIdsInTaskOnUpdateMeeting(meetingDb,meetingDbUpdated);

        if (meeting.getReferenceEntityNumber() != null) {
            referenceTask = getReferenceTaskFromMeeting(meetingDbUpdated);

            if (referenceTask != null && referenceTask.getSprintId() != null) {
                EntityPreference orgPreference = entityPreferenceService.fetchEntityPreference(com.tse.core_application.model.Constants.EntityTypes.ORG, referenceTask.getFkTeamId().getFkOrgId().getOrgId());
                accountIdsToUpdateForCapacity = capacityService.getAccountIdsPerMeetingPreference(meetingDbUpdated, referenceTask, referenceTask.getSprintId(), orgPreference.getMeetingEffortPreferenceId());

                if (updatedMeetingsFieldsByUser.contains("duration")) {
                    int adjustedDuration = meetingDbUpdated.getDuration() - meetingDb.getDuration();
                    capacityService.updateReferenceMeetingCapacityOnAddTaskToSprint(referenceTask.getSprintId(), adjustedDuration, accountIdsToUpdateForCapacity);
                }
                else if (updatedMeetingsFieldsByUser.contains("attendeeRequestList")) {
                    isCapacityChangeRequiredForAttendeeModification = true;
                    accountIdsOldMembers = meetingDb.getAttendeeList().stream()
                            .filter(attendee -> "invited".equals(attendee.getAttendeeInvitationStatus()))
                            .map(Attendee::getAccountId)
                            .collect(Collectors.toSet());

                }
            }
        }

        meetingDbUpdated = meetingRepository.save(meetingDbUpdated);

        //Long recurringID = meetingDbUpdated.getRecurringMeeting().getRecurringMeetingId();
        //To pass attendees List to notification for meeting invitation
        List<Attendee> updatedAttendeeList = new ArrayList<>();

        if((meeting.getAttendeeRequestList() != null && !meeting.getAttendeeRequestList().isEmpty()) || !deletedAccountIds.isEmpty())
        {
            updatedAttendeeList = attendeeService.updateAllAttendees(meeting.getAttendeeRequestList(), meetingDbUpdated, deletedAccountIds);
            EntityPreference orgPreference = entityPreferenceService.fetchEntityPreference(com.tse.core_application.model.Constants.EntityTypes.ORG, meetingDb.getOrgId());

            // capacity handling for newly added members
            if (isCapacityChangeRequiredForAttendeeModification) {

                Set<Long> accountIdsOfNewMembers = updatedAttendeeList.stream().map(Attendee::getAccountId).collect(Collectors.toSet());

                // new accountIds that are added
                List<Long> newAccountIds = new ArrayList<>();
                List<Long> removedAccountIds = new ArrayList<>();

                for (Attendee attendee : updatedAttendeeList) {
                    if (!accountIdsOldMembers.contains(attendee.getAccountId())) {
                        newAccountIds.add(attendee.getAccountId());
                    }
                }

                for (Long accountId : accountIdsOldMembers) {
                    if (!accountIdsOfNewMembers.contains(accountId)) {
                        removedAccountIds.add(accountId);
                    }
                }

                if (!newAccountIds.isEmpty()) {
                    // assuming the attendeeList is updated in the meeting - accountIdsToUpdateForCapacity contains the accountIds of the attendees for which capacity should have been included
                    List<Long> accountIdsForUpdate = newAccountIds.stream().filter(accountIdsToUpdateForCapacity::contains).collect(Collectors.toList());
                    capacityService.updateReferenceMeetingCapacityOnAddTaskToSprint(referenceTask.getSprintId(), meeting.getDuration(), accountIdsForUpdate);
                }

                if (!removedAccountIds.isEmpty()) {
                    List<Long> accountIdsToUpdate = new ArrayList<>();
                    // get accountIds to Update as per old meeting
                    List<Long> accountIdsInCapacityOldMeeting = capacityService.getAccountIdsPerMeetingPreference(meetingDb, referenceTask, referenceTask.getSprintId(), orgPreference.getMeetingEffortPreferenceId());
                    for (Long accountId : accountIdsInCapacityOldMeeting) {
                        if (removedAccountIds.contains(accountId)) {
                            accountIdsToUpdate.add(accountId);
                        }
                    }

                    capacityService.updateReferenceMeetingCapacityOnAddTaskToSprint(referenceTask.getSprintId(), -meeting.getDuration(), accountIdsToUpdate);
                }
            }

            meetingDbUpdated.setAttendeeList(updatedAttendeeList);
           if(meetingDb.getAttendeeId() == null) {
               updateAttendeeIdByMeetingId(updatedAttendeeList.get(0).getAttendeeId(), meetingDbUpdated.getMeetingId());
           }
        }

//        List<ActionItem> actionItemsDeleted = actionItemService.getDeletedActionItems(meeting.getActionItems(), meetingDbUpdated.getActionItems());
        if (meeting.getActionItems() != null && !meeting.getActionItems().isEmpty()) {
            List<ActionItem> actionItemsUpdated = actionItemService.updateActionItems(meeting.getActionItems(), meetingDbUpdated);
        }

        if(updatedMeetingsFieldsByUser.contains("title")){
            List<TimeSheet> timeSheets = timeSheetRepository.findByEntityTypeIdAndEntityId(com.tse.core_application.model.Constants.EntityTypes.MEETING, meetingDbUpdated.getMeetingId());
            if(timeSheets == null) timeSheets = Collections.emptyList();
            DataEncryptionConverter dataEncryptionConverter = new DataEncryptionConverter();
            String updatedTaskTitle = dataEncryptionConverter.convertToDatabaseColumn(meetingDbUpdated.getTitle());
            for(TimeSheet timeSheet: timeSheets){
                timeSheet.setEntityTitle(updatedTaskTitle);
            }
            timeSheetRepository.saveAll(timeSheets);
        }

        if(allowNotification) {
            try {
                //create meeting invite payload
                List<HashMap<String, String>> meetingPayload = notificationService.updateMeetingNotification(meetingDbUpdated, meetingDb, timeZone, updatedAttendeeList, meetingDb.getAttendeeList(), updatedMeetingsFieldsByUser);
                //  pass this payload to fcm for notification
                taskServiceImpl.sendPushNotification(meetingPayload);
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        auditService.auditForMeeting(userAccountRepository.findByAccountIdAndIsActive(meetingDbUpdated.getUpdatedAccountId(), true), meetingDbUpdated, true);
        return meetingDbUpdated;
    }

    public Task getReferenceTaskFromMeeting(Meeting meeting) {
        Task referenceTask = null;
        if (meeting.getReferenceEntityNumber() != null) {
            Long taskIdentifier = taskServiceImpl.getTaskIdentifierFromTaskNumber(meeting.getReferenceEntityNumber());
            if (meeting.getTeamId() != null) {
                referenceTask = taskRepository.findByTaskIdentifierAndFkTeamIdTeamId(taskIdentifier, meeting.getTeamId());
            } else if (meeting.getProjectId() != null) {
                referenceTask = taskRepository.findByTaskIdentifierAndFkProjectIdProjectId(taskIdentifier, meeting.getProjectId());
            }
        }
        return referenceTask;
    }

    /** This method sets the reference fields of the time tracking table for an attendee's meeting record in cases when we bill their efforts in task*/
    public void setReferenceEntitiesInTimesheet(Meeting meeting, TimeSheet timeSheet){
        timeSheet.setReferenceEntityNum(meeting.getReferenceEntityNumber());
        timeSheet.setReferenceEntityTypeId(meeting.getReferenceEntityTypeId());

        if(meeting.getReferenceEntityNumber() != null) {
            if(Objects.equals(meeting.getReferenceEntityTypeId(), com.tse.core_application.model.Constants.EntityTypes.TASK)){

                Task taskFromDB = null;
                Long taskIdentifier = taskServiceImpl.getTaskIdentifierFromTaskNumber(meeting.getReferenceEntityNumber());
                if (meeting.getTeamId() != null) {
                    taskFromDB = taskRepository.findByTaskIdentifierAndFkTeamIdTeamId(taskIdentifier, meeting.getTeamId());
                } else if (meeting.getProjectId() != null) {
                    taskFromDB = taskRepository.findByTaskIdentifierAndFkProjectIdProjectId(taskIdentifier, meeting.getProjectId());
                }

                timeSheet.setReferenceEntityId(taskFromDB.getTaskId());
                timeSheet.setReferenceTaskTypeId(taskFromDB.getTaskTypeId());
                timeSheet.setReferenceEntityTitle(taskFromDB.getTaskTitle());
            }
            else if(Objects.equals(meeting.getReferenceEntityTypeId(), com.tse.core_application.model.Constants.EntityTypes.MEETING)){
                // Todo: need to change meeting number format
//                Meeting referenceMeeting = meetingRepository.findByMeetingNumber(meeting.getReferenceEntityNumber());
//                timeSheet.setReferenceEntityId(referenceMeeting.getMeetingId());
//                timeSheet.setReferenceTaskTypeId(null);
//                timeSheet.setReferenceEntityTitle(referenceMeeting.getTitle());
            }

        }

    }

    /* This method updates the time tracking table for a meeting record when attendee duration is added for that meeting */

    /** This method populates the attendees response in time sheet for all attendees and organiser according to the time duration of each attendee */
    public void updateTimeSheetForAttendeeDurationInMeeting(AttendeeParticipationRequest attendeeRequest, Attendee attendeeDb, String desiredTimeZone) throws IllegalAccessException {

        User user = null;
        UserAccount userAccount = null;
        Meeting meetingDb;
        DataEncryptionConverter dataEncryptionConverter = new DataEncryptionConverter();
        meetingDb = meetingRepository.findByMeetingId(attendeeRequest.getMeetingId());
        Optional<UserAccount> userAccountOptional = Optional.ofNullable(userAccountService.getActiveUserAccountByAccountId(attendeeRequest.getAccountId()));

        if (userAccountOptional.isPresent()) {
            userAccount = userAccountOptional.get();
            user = userAccount.getFkUserId();
        } else {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new NoDataFoundException());
            logger.error("user account not found", new Throwable(allStackTraces));
            ThreadContext.clearMap();
            throw new NoDataFoundException();
        }
        TimeSheet timeSheet;
        TimeSheet splitTimeSheet = new TimeSheet();
        List<TimeSheet> timeSheetList = timeSheetRepository.findAllByAccountIdAndEntityIdandEntityTypeIdAndOrderByStartDate(userAccount.getAccountId(), com.tse.core_application.model.Constants.EntityTypes.MEETING, attendeeRequest.getMeetingId());
        Integer efforts = 0;
        //timesheet list size cannot be greater than 2 as we have limited users from adding efforts more than 24 hours.
        int earnedTime = attendeeDb.getAttendeeDuration() > meetingDb.getDuration() ? meetingDb.getDuration() : attendeeDb.getAttendeeDuration();
        if (!timeSheetList.isEmpty()) {
            timeSheet = timeSheetList.get(0);
            if (timeSheetList.size() > 1) {
                splitTimeSheet = timeSheetList.get(1);
            }
            timeSheet.setEarnedTime(earnedTime);

            if (meetingDb.getReferenceEntityNumber() != null) {
                updateTaskEffortForMeeting(timeSheet, meetingDb, attendeeRequest);
            } else {
                timeSheet.setNewEffort(attendeeRequest.getAttendeeDuration());
            }
            splitMeetingEfforts(attendeeRequest, meetingDb, timeSheet, splitTimeSheet, desiredTimeZone);
            efforts = attendeeRequest.getAttendeeDuration() - timeSheet.getNewEffort();
        } else {
            timeSheet = new TimeSheet();
            timeSheet.setEarnedTime(earnedTime);
            if (meetingDb.getReferenceEntityNumber() != null) {
                updateTaskEffortForMeeting(timeSheet, meetingDb, attendeeRequest);
            } else {
                timeSheet.setNewEffort(attendeeRequest.getAttendeeDuration());
            }
            timeSheet.setEntityId(attendeeRequest.getMeetingId());
            // Todo: to be handled properly when we change the meeting number from long to string
            timeSheet.setEntityNumber(meetingDb.getMeetingNumber().toString());
            timeSheet.setEntityTitle(dataEncryptionConverter.convertToDatabaseColumn(meetingDb.getTitle()));
            timeSheet.setEntityTypeId(com.tse.core_application.model.Constants.EntityTypes.MEETING);
            timeSheet.setTaskTypeId(null);
            timeSheet.setBuId(attendeeDb.getBuId());
            timeSheet.setProjectId(attendeeDb.getProjectId());
            timeSheet.setTeamId(attendeeDb.getTeamId());
            timeSheet.setAccountId(userAccount.getAccountId());
            timeSheet.setOrgId(userAccount.getOrgId());
            timeSheet.setUserId(user.getUserId());
            splitMeetingEfforts(attendeeRequest, meetingDb, timeSheet, splitTimeSheet, desiredTimeZone);
            timeSheetList.add(timeSheet);
            efforts = attendeeRequest.getAttendeeDuration();
        }
        NewEffortTrack newEffortTrack = new NewEffortTrack(efforts, timeSheet.getNewEffortDate());
        Integer effortsCheck = taskServiceImpl.effortsWithin24(attendeeRequest.getAccountId(), newEffortTrack);
        if (splitTimeSheet.getNewEffort() != null) {
            NewEffortTrack newEffortTrackForSplitTimesheet = new NewEffortTrack(splitTimeSheet.getNewEffort()+effortsCheck, splitTimeSheet.getNewEffortDate());
            effortsCheck = taskServiceImpl.effortsWithin24(attendeeRequest.getAccountId(), newEffortTrackForSplitTimesheet);
            if (effortsCheck>0) {
                String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("Effort cannot be more than 24 hours for a single day"));
                logger.error("Effort cannot be more than 24 hours for a single day ", new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw new ValidationFailedException("Effort cannot be more than 24 hours for a single day");
            }
            if (!timeSheetList.contains(splitTimeSheet)) {
                timeSheetList.add(splitTimeSheet);
            }
            if (Objects.equals(splitTimeSheet.getNewEffort(), 0)) {
                timeSheetList.remove(splitTimeSheet);
                timeSheetRepository.delete(splitTimeSheet);
            }
        }
        else if (effortsCheck>0) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("Effort cannot be more than 24 hours for a single day"));
            logger.error("Effort cannot be more than 24 hours for a single day ", new Throwable(allStackTraces));
            ThreadContext.clearMap();
            throw new ValidationFailedException("Effort cannot be more than 24 hours for a single day");
        }
        timeSheetRepository.saveAll(timeSheetList);

    }

    /** This method creates a recurring meeting based on the recurrence pattern provided in the request*/
    public RecurringMeetingResponse createRecurringMeeting(RecurringMeetingRequest recurringMeeting, String desiredTimeZone) throws IllegalAccessException{

        List<Meeting> meetingList = new ArrayList<>();
        RecurringMeeting recurringMeetingToSave = new RecurringMeeting();
        // set default value of recurring meeting
        recurringMeeting.setIsCancelled(false);

        // get all the recurring dates for recurring meeting
        String cronExp = generateCronExpressionForRecurringMeeting(recurringMeeting);
        List<LocalDateTime> allDates = getTriggerOccurrences(cronExp, recurringMeeting.getRecurringMeetingStartDateTime(), recurringMeeting.getRecurringMeetingEndDateTime());
        if (DebugConfig.getInstance().isDebug()) {
            System.out.println(allDates.toString());
        }

//        List<LocalDateTime> allConvertedDates = new ArrayList<>();
//        for(LocalDateTime dateToConvert : allDates){
//            allConvertedDates.add(convertUserDateToServerTimezone(dateToConvert, desiredTimeZone));
//        }

        // convert recurring meeting dates to server time zone.
      LocalDateTime convertedRecurringStartDate =  convertUserDateToServerTimezone(recurringMeeting.getRecurringMeetingStartDateTime(), desiredTimeZone);
      LocalDateTime convertedRecurringEndDate   =  convertUserDateToServerTimezone(recurringMeeting.getRecurringMeetingEndDateTime(), desiredTimeZone);
      LocalTime convertedMeetingStartTime = convertUserTimeToServerTimeZone(recurringMeeting.getMeetingStartTime(), desiredTimeZone);

      if (recurringMeeting.getReferenceEntityNumber() != null) {
          Task referenceTask = null;
          Long taskIdentifier = taskServiceImpl.getTaskIdentifierFromTaskNumber(recurringMeeting.getReferenceEntityNumber());
          if (recurringMeeting.getTeamId() != null) {
              referenceTask = taskRepository.findByTaskIdentifierAndFkTeamIdTeamId(taskIdentifier, recurringMeeting.getTeamId());
          } else if (recurringMeeting.getProjectId() != null) {
              referenceTask = taskRepository.findByTaskIdentifierAndFkProjectIdProjectId(taskIdentifier, recurringMeeting.getProjectId());
          }
          if (referenceTask != null) {
              if ((referenceTask.getTaskExpStartDate() != null && convertedRecurringStartDate != null && convertedRecurringStartDate.isBefore(referenceTask.getTaskExpStartDate())) ||
                      referenceTask.getTaskExpEndDate() != null && convertedRecurringEndDate != null && convertedRecurringEndDate.isAfter(referenceTask.getTaskExpEndDate())) {
                  throw new ValidationFailedException("Meeting date range is outside of Work item exp date range");
              }
          }
      }

        // set recurring meeting fields

        copySimilarFields(recurringMeeting, recurringMeetingToSave);

        recurringMeetingToSave.setRecurringMeetingStartDateTime(convertedRecurringStartDate);
        recurringMeetingToSave.setRecurringMeetingEndDateTime(convertedRecurringEndDate);
        recurringMeetingToSave.setMeetingStartTime(convertedMeetingStartTime);
        addLabelsToRecurringMeeting(recurringMeetingToSave, recurringMeeting.getLabelsToAdd());

//        recurringMeetingToSave.setRecurringMeetingStartDateTime(convertedRecurringStartDate);
//        recurringMeetingToSave.setRecurringMeetingEndDateTime(convertedRecurringEndDate);
//        recurringMeetingToSave.setRecurringFrequencyIndicator(recurringMeeting.getRecurringFrequencyIndicator());
//        recurringMeetingToSave.setRecurDays(recurringMeeting.getRecurDays());
//        recurringMeetingToSave.setRecurEvery(recurringMeeting.getRecurEvery());
//        recurringMeetingToSave.setNumOfOccurrences(recurringMeeting.getNumOfOccurrences());
//        recurringMeetingToSave.setTitle(recurringMeeting.getTitle());
//        recurringMeetingToSave.setMeetingStartDateTimeList(allDates);

        Long recurringMeetingIdentifier = getNextRecurringMeetingIdentifier(recurringMeeting.getOrgId());
        recurringMeetingToSave.setRecurringMeetingNumber("R-" + recurringMeetingIdentifier);

        List<Long> attendeeAccounts = new ArrayList<>();
        // store account ids of all the attendees in the meeting in recurring meeting table.
        if(recurringMeeting.getAttendeeRequestList() != null  && !recurringMeeting.getAttendeeRequestList().isEmpty()){
            List<AttendeeRequest> attendeeRequestList = recurringMeeting.getAttendeeRequestList();
            attendeeAccounts = attendeeRequestList.stream().map(AttendeeRequest::getAccountId).collect(Collectors.toList());
        }
        if(!attendeeAccounts.contains(recurringMeeting.getOrganizerAccountId())) {
            attendeeAccounts.add(recurringMeeting.getOrganizerAccountId());
        }
        recurringMeetingToSave.setAttendeeAccounts(attendeeAccounts);


        RecurringMeeting recurringMeetingSaved =  recurringMeetingRepository.save(recurringMeetingToSave);

        // generate all meetings related to recurring meeting based on allDates

        MeetingRequest newMeetingRequest = new MeetingRequest();
        copySimilarFields(recurringMeeting, newMeetingRequest);
//        newMeetingRequest.setRecurringMeetingId(recurringMeetingSaved.getRecurringMeetingId());
//        newMeetingRequest.setRecurringMeeting(recurringMeetingSaved);

        MeetingResponse savedMeeting = new MeetingResponse();

        if(allDates.isEmpty()){
            throw new ValidationFailedException("Please enter valid recurrence pattern");
        }
            int count = 0;
            for (LocalDateTime startDateTime : allDates) {
                count++;
//            MeetingRequest newMeetingRequest = new MeetingRequest();

//            copySimilarFields(recurringMeeting, newMeetingRequest);

                newMeetingRequest.setStartDateTime(startDateTime);

//            newMeetingRequest.setRecurringMeetingId(recurringMeetingSaved.getRecurringMeetingId());

                newMeetingRequest.setEndDateTime(calculateEndDateTimeForMeeting(startDateTime, recurringMeeting.getDuration()));

                convertAllMeetingLocalDateAndTimeToServerTimeZone(newMeetingRequest, desiredTimeZone);

//            newMeetingRequest.setRecurringMeeting(recurringMeetingSaved);
                if(count > 1) {
                    savedMeeting = addMeeting(newMeetingRequest, recurringMeetingSaved, desiredTimeZone, false);
                } else {
                    savedMeeting = addMeeting(newMeetingRequest, recurringMeetingSaved, desiredTimeZone, true);
                }
            }

        RecurringMeetingResponse response = new RecurringMeetingResponse();
        copySimilarFields(recurringMeetingSaved, response);

        response.setRecurringMeetingStartDateTime(recurringMeeting.getRecurringMeetingStartDateTime());
        response.setRecurringMeetingEndDateTime(recurringMeeting.getRecurringMeetingEndDateTime());
        response.setMeetingStartTime(recurringMeeting.getMeetingStartTime());
        response.setAttendeeRequestList(savedMeeting.getAttendeeRequestList());
        if(recurringMeetingSaved.getRecurMeetingLabels() != null) {
            List<Label> labelsSaved = recurringMeetingSaved.getRecurMeetingLabels();
            List<LabelResponse> labelResponseList = new ArrayList<>();
            for(Label label: labelsSaved) {
                LabelResponse labelResponse = new LabelResponse();
                labelResponse.setLabelId(label.getLabelId());
                labelResponse.setLabelName(label.getLabelName());
                labelResponseList.add(labelResponse);
            }
            response.setLabels(labelResponseList);
        }
        if(recurringMeetingSaved.getTeamId() != null){
            String teamName = teamRepository.findTeamNameByTeamId(recurringMeetingSaved.getTeamId());
            response.setEntityName(teamName);
        }

        return response;
    }

    /**
     * this methdd is used to add labels to a recurring meeting and save the label object
     * @param recurringMeeting
     * @param labelsToAdd
     */
    private void addLabelsToRecurringMeeting(RecurringMeeting recurringMeeting, List<String> labelsToAdd) {
        if (labelsToAdd == null || labelsToAdd.isEmpty()) return;

        List<Label> currentLabels = recurringMeeting.getRecurMeetingLabels();
        if (currentLabels == null) {
            currentLabels = new ArrayList<>();
        }
        List<Label> processedLabels = new ArrayList<>();
        List<String> labelsInRequest = new ArrayList<>();
        Long foundEntityId = 0L;
        Integer foundEntityTypeId = 0;
        if (recurringMeeting.getTeamId() != null) {
            foundEntityId = recurringMeeting.getTeamId();
            foundEntityTypeId = com.tse.core_application.model.Constants.EntityTypes.TEAM;
        } else if (recurringMeeting.getProjectId() != null) {
            foundEntityId = recurringMeeting.getProjectId();
            foundEntityTypeId = com.tse.core_application.model.Constants.EntityTypes.PROJECT;
        } else if (recurringMeeting.getBuId() != null) {
            foundEntityId = recurringMeeting.getBuId();
            foundEntityTypeId = com.tse.core_application.model.Constants.EntityTypes.BU;
        } else if (recurringMeeting.getOrgId() != null) {
            foundEntityId = recurringMeeting.getOrgId();
            foundEntityTypeId = com.tse.core_application.model.Constants.EntityTypes.ORG;
        } else {
            throw new ValidationFailedException("Entity does not exist!!");
        }
        for (String labelName : labelsToAdd) {
            final String formattedLabelName = labelName.trim().replaceAll("\\s+", " ");
//            final String formattedLabelName = Arrays.stream(labelName.toLowerCase().split(" "))
//                    .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
//                    .collect(Collectors.joining(" "));
//            boolean labelExists = currentLabels.stream().anyMatch(label -> label.getLabelName().equalsIgnoreCase(formattedLabelName));

            boolean labelExists = currentLabels.stream()
                    .anyMatch(label -> label.getLabelName().equalsIgnoreCase(formattedLabelName)) || labelsInRequest.stream()
                    .anyMatch(label -> label.equalsIgnoreCase(formattedLabelName));
            labelsInRequest.add(formattedLabelName);

            if(!labelExists) {
                Label label = labelRepository.findByLabelNameIgnoreCaseAndEntityTypeIdAndEntityId(labelName, foundEntityTypeId, foundEntityId);
                if (label == null) {
                    label = new Label();
                    label.setLabelName(labelName);
                    label.setEntityId(foundEntityId);
                    label.setEntityTypeId(foundEntityTypeId);
                    label.getRecurringMeetings().add(recurringMeeting);
                    label = labelRepository.save(label);
                } else {
                    label.getRecurringMeetings().add(recurringMeeting);
                }
                processedLabels.add(label);
            }
        }
        currentLabels.addAll(processedLabels);
        recurringMeeting.setRecurMeetingLabels(currentLabels);
    }

    /** This method generates recurrence pattern string for recurring meeting*/
    public String generateCronExpressionForRecurringMeeting(RecurringMeetingRequest recurringMeeting){

        String cronExpression = null;
        LocalTime startTime = recurringMeeting.getMeetingStartTime();
        String hour  = String.valueOf(startTime.getHour());
        String min  = String.valueOf(startTime.getMinute());
        String timeString = "0 "+min+" "+hour;

        switch (recurringMeeting.getRecurringFrequencyIndicator()){
            case 0:   // Daily case : two sub cases in this - either meeting can be repeated on one or more of days of week or can recur according to recurEvery.
                // 0 0 10 ? * 2,3,4,5,6 --> daily meet from monday to friday at 10 am
                if(recurringMeeting.getRecurEvery() != null){
                    if(!recurringMeeting.getRecurDays().isEmpty()){
                        throw new ValidationFailedException("you cannot select recurring days with recurEvery");
                    }

                    String recurInEvery = String.valueOf(recurringMeeting.getRecurEvery());
                    cronExpression = timeString + " 1/"+recurInEvery + " * ?";
                }
                else{
                    if(recurringMeeting.getRecurDays().isEmpty()){
                        recurringMeeting.setRecurDays("1-7");
                    }
                    cronExpression = timeString + " ? * " + recurringMeeting.getRecurDays();
                    if (DebugConfig.getInstance().isDebug()) {
                        System.out.println("CRON EXPRESSION PRINT: " + cronExpression);
                    }
                }
                break;
            case 1:  // weekly case : only one recurring day of week is accepted
                if(recurringMeeting.getRecurDays().length() != 1){
                    throw new ValidationFailedException("You cannot enter more than 1 week in a day for a weekly meeting");
                }
                cronExpression = timeString + " ? * " + recurringMeeting.getRecurDays();
                break;
            default:
                throw new ValidationFailedException("Invalid frequency indicator input");
        }

        return cronExpression;
    }

    /** This method is used to get all the recurring dates for recurring meeting based on the recurrence pattern */
    public static List<LocalDateTime> getTriggerOccurrences(String cronExpression, LocalDateTime startDate, LocalDateTime endDate) {
        CronExpression cron;
        List<LocalDateTime> occurrences = new ArrayList<>();
        try {
            cron = new CronExpression(cronExpression);
            cron.setTimeZone(TimeZone.getDefault());
        } catch (ParseException e) {
            e.printStackTrace();
            return occurrences;
        }

        LocalDateTime nextValidTime = startDate;
        while (!nextValidTime.isAfter(endDate)) {
            Date nextDate = cron.getNextValidTimeAfter(Date.from(nextValidTime.atZone(ZoneId.systemDefault()).toInstant()));
            if (nextDate == null) {
                break;
            }
            LocalDateTime nextOccurrence = LocalDateTime.ofInstant(nextDate.toInstant(), ZoneId.systemDefault());
            if (!nextOccurrence.isBefore(endDate)) {
                break;
            }
            occurrences.add(nextOccurrence);
            nextValidTime = nextOccurrence;
        }

        return occurrences;
    }

    /** This method is used to get a recurring meeting by recurring meeting id . The response do not involve all individual meetings */
    public RecurringMeetingResponse getRecurringMeetingByRecurringMeetingId(Long recurringMeetingId, String accountIds, String desiredTimeZone) throws IllegalAccessException{

        RecurringMeetingResponse meetingToGet;

        RecurringMeeting foundMeetingDb = recurringMeetingRepository.findByRecurringMeetingId(recurringMeetingId);
        if(foundMeetingDb != null) {
            List<Long> accounIdList = CommonUtils.convertToLongList(accountIds);
            if (foundMeetingDb.getTeamId() != null) {
                // team id validation to get meeting
                List<Long> teamIds = List.of(foundMeetingDb.getTeamId());
                List<Long> validTeamIds = getAllValidTeamIdsByInputFilters(null, null, accountIds);
                HashSet<Long> validTeamIdsSet = new HashSet<>(validTeamIds);

                if (!validTeamIdsSet.contains(foundMeetingDb.getTeamId())) {
                    throw new ValidationFailedException("You are not authorized to access the meeting");
                }
            } else if (foundMeetingDb.getProjectId() != null) {
                List<Long> validProjectIds = accessDomainRepository.getProjectInfoByAccountIdsAndIsActiveTrue(accounIdList).stream().map(Project::getProjectId).collect(Collectors.toList());
                if (!validProjectIds.contains(foundMeetingDb.getProjectId())) {
                    throw new ValidationFailedException("You are not authorized to access the meeting");
                }
            } else {
                if (!userAccountRepository.existsByAccountIdInAndOrgIdAndIsActive(accounIdList, foundMeetingDb.getOrgId(), true)) {
                    throw new ValidationFailedException("You are not authorized to access the meeting");
                }
            }

            meetingToGet = new RecurringMeetingResponse();

            LocalDateTime recurringStartDate = convertServerDateToUserTimezone(foundMeetingDb.getRecurringMeetingStartDateTime(), desiredTimeZone);
            LocalDateTime recurringEndDate = convertServerDateToUserTimezone(foundMeetingDb.getRecurringMeetingEndDateTime(), desiredTimeZone);
            LocalTime meetingStartTime = convertServerTimeToUserTimeZone(foundMeetingDb.getMeetingStartTime(), desiredTimeZone);

            foundMeetingDb.setRecurringMeetingStartDateTime(recurringStartDate);
            foundMeetingDb.setRecurringMeetingEndDateTime(recurringEndDate);
            foundMeetingDb.setMeetingStartTime(meetingStartTime);

            copySimilarFields(foundMeetingDb, meetingToGet);

            // set entityName in meetingResponse
            if(meetingToGet.getTeamId() != null){
                String teamName = teamRepository.findTeamNameByTeamId(meetingToGet.getTeamId());
                meetingToGet.setEntityName(teamName);
            }
            else if(meetingToGet.getProjectId() != null){
                meetingToGet.setEntityName(projectRepository.findProjectNameByProjectId(meetingToGet.getProjectId()));
            }
            else if(meetingToGet.getBuId() != null){
                meetingToGet.setEntityName(buRepository.findBuNameByBuId(meetingToGet.getBuId()));
            }
            else{
                meetingToGet.setEntityName(organizationRepository.findOrganizationNameByOrgId(meetingToGet.getOrgId()));
            }

            // set meetingType in meetingResponse
            meetingToGet.setMeetingType(foundMeetingDb.getMeetingType());

            List<LabelResponse> labelResponseList = new ArrayList<>();
            if(foundMeetingDb.getRecurMeetingLabels() != null) {
                List<Label> labelsList = foundMeetingDb.getRecurMeetingLabels();
                for(Label label : labelsList) {
                    LabelResponse labelResponse = new LabelResponse();
                    labelResponse.setLabelName(label.getLabelName());
                    labelResponse.setLabelId(label.getLabelId());
                    labelResponseList.add(labelResponse);
                }
                meetingToGet.setLabels(labelResponseList);
            }


            List<Attendee> attendeeListFromDb = new ArrayList<>();

            if(foundMeetingDb.getAttendeeAccounts() != null && !foundMeetingDb.getAttendeeAccounts().isEmpty()){

                for(Long attendeeAccountId : foundMeetingDb.getAttendeeAccounts()){

                     Attendee attendee = new Attendee();
                     attendee.setAccountId(attendeeAccountId);
                     attendee.setTeamId(meetingToGet.getTeamId());
                     attendee.setBuId(meetingToGet.getBuId());
                     attendee.setProjectId(meetingToGet.getProjectId());
                     attendeeListFromDb.add(attendee);

                }

                meetingToGet.setAttendeeRequestList(attendeeListFromDb);
            }

            return meetingToGet;
        }
        else{
            return null;
        }


    }

    /** This method is used to get  all the individual meetings in a recurring meeting using pagination */
    public List<MeetingResponse> getAllRecurringMeetingsList(Long recurringMeetingId, Integer pageNumber , Integer pageSize, String desiredTimeZone, PageInfoPagination pageInfo) throws IllegalAccessException{

   //     Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("startDateTime").ascending());
        org.springframework.data.domain.Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("startDateTime").ascending());
        Page<Meeting> meetingsPage = meetingRepository.findByRecurringMeetingRecurringMeetingId(recurringMeetingId, pageable);
        List<Meeting> meetingList = meetingsPage.getContent();

        pageInfo.setPageNumber(meetingsPage.getNumber());
        pageInfo.setPageSize(meetingsPage.getSize());
        pageInfo.setTotalPages(meetingsPage.getTotalPages());
        pageInfo.setTotalElements(meetingsPage.getTotalElements());
        pageInfo.setLastPage(meetingsPage.isLast());

        List<MeetingResponse> meetingResponseList = new ArrayList<>();
        for(Meeting meetingDb : meetingList){

            MeetingResponse meetingResponse = new MeetingResponse();

            meetingResponse = createMeetingResponseFromMeeting(meetingDb, desiredTimeZone);

//            convertAllMeetingServerDateAndTimeToLocalTimeZone(meetingDb, desiredTimeZone);
//
//            copySimilarFields(meetingDb, meetingResponse);
//
//            if(meetingResponse.getTeamId() != null){
//                String teamName = teamRepository.findTeamNameByTeamId(meetingResponse.getTeamId());
//                meetingResponse.setEntityName(teamName);
//            }
//
//            // set meetingType in meetingResponse
//            meetingResponse.setMeetingType(setMeetingType(meetingDb.getMeetingTypeIndicator()));
//
//            // set recurringMeetingId
//            meetingResponse.setRecurringMeetingId(meetingDb.getRecurringMeeting().getRecurringMeetingId());
//
//            // set attendees List in meetingResponse
//            if (meetingDb.getAttendeeId() != null && meetingDb.getAttendeeList() != null) {
//                meetingResponse.setAttendeeRequestList(attendeeService.removeDeletedAttendees(meetingDb.getAttendeeList()));
//            }else{     // added an else part in task 2676 to return empty attendee list.
//                meetingResponse.setAttendeeRequestList(Collections.emptyList());
//            }

            meetingResponseList.add(meetingResponse);

        }

        return meetingResponseList;

    }

    /** This method is used to get the recurring meeting by recurring meeting id and also all the individual meetings in it using pagination */
    public RecurringMeetingListResponse getRecurringMeetingByRecurringMeetingIdAndPageNumber(Long recurringMeetingId, String accountIds, Integer pageNumber, Integer pageSize, String desiredTimeZone) throws IllegalAccessException{

        RecurringMeetingListResponse recurringMeetingToGet = new RecurringMeetingListResponse();
        PageInfoPagination pageInfo = new PageInfoPagination();

        RecurringMeeting recurringMeetingDb = recurringMeetingRepository.findByRecurringMeetingId(recurringMeetingId);
        if(recurringMeetingDb == null){
            throw new MeetingNotFoundException();
        }
        List<Long> accounIdList = CommonUtils.convertToLongList(accountIds);

        if (recurringMeetingDb.getTeamId() != null) {
            List<Long> teamIds = List.of(recurringMeetingDb.getTeamId());
            List<Long> validTeamIds = getAllValidTeamIdsByInputFilters(null, null, accountIds);
            HashSet<Long> validTeamIdsSet = new HashSet<>(validTeamIds);

            if (!validTeamIdsSet.contains(recurringMeetingDb.getTeamId())) {
                throw new ValidationFailedException("You are not authorized to access the meeting");
            }
        } else if (recurringMeetingDb.getProjectId() != null) {
            List<Long> validProjectIds = accessDomainRepository.getProjectInfoByAccountIdsAndIsActiveTrue(accounIdList).stream().map(Project::getProjectId).collect(Collectors.toList());
            if (!validProjectIds.contains(recurringMeetingDb.getProjectId())) {
                throw new ValidationFailedException("You are not authorized to access the meeting");
            }
        } else {
            if (!userAccountRepository.existsByAccountIdInAndOrgIdAndIsActive(accounIdList, recurringMeetingDb.getOrgId(), true)) {
                throw new ValidationFailedException("You are not authorized to access the meeting");
            }
        }

        // For test
//        List<LocalDateTime> allDates = new ArrayList<>(recurringMeetingDb.getMeetingStartDateTimeList());
//        System.out.println("ALL START DATES :"+ allDates);

        recurringMeetingDb.setRecurringMeetingStartDateTime(convertServerDateToUserTimezone(recurringMeetingDb.getRecurringMeetingStartDateTime(), desiredTimeZone));
        recurringMeetingDb.setRecurringMeetingEndDateTime(convertServerDateToUserTimezone(recurringMeetingDb.getRecurringMeetingEndDateTime(), desiredTimeZone));
        recurringMeetingDb.setMeetingStartTime(convertServerTimeToUserTimeZone(recurringMeetingDb.getMeetingStartTime(), desiredTimeZone));

        copySimilarFields(recurringMeetingDb , recurringMeetingToGet);

        List<LabelResponse> labelResponseList = new ArrayList<>();
        if(recurringMeetingDb.getRecurMeetingLabels() != null) {
            List<Label> labelsList = recurringMeetingDb.getRecurMeetingLabels();
            for(Label label : labelsList) {
                LabelResponse labelResponse = new LabelResponse();
                labelResponse.setLabelName(label.getLabelName());
                labelResponse.setLabelId(label.getLabelId());
                labelResponseList.add(labelResponse);
            }
            recurringMeetingToGet.setLabels(labelResponseList);
        }

        List<MeetingResponse> allMeetingResponses = new ArrayList<>();

        allMeetingResponses = getAllRecurringMeetingsList(recurringMeetingId, pageNumber, pageSize, desiredTimeZone, pageInfo);

        recurringMeetingToGet.setMeetingResponseList(allMeetingResponses);

        recurringMeetingToGet.setPageNumber(pageInfo.getPageNumber());
        recurringMeetingToGet.setPageSize(pageInfo.getPageSize());
        recurringMeetingToGet.setTotalMeetingsInList(pageInfo.getTotalElements());
        recurringMeetingToGet.setTotalPages(pageInfo.getTotalPages());
        recurringMeetingToGet.setLastPage(pageInfo.isLastPage());

        return recurringMeetingToGet;

    }

    /** This meeting is used to get all the valid team ids of the user based on the input filters of orgIds , teamIds and accountIds of user */
    public List<Long> getAllValidTeamIdsByInputFilters(Long orgId, List<Long> teamIds, String accountIds){

        List<Long> accountIdsList = Arrays.stream(accountIds.split(",")).map(Long::valueOf).collect(Collectors.toList());
        List<Long> finalTeamIdsList = new ArrayList<>();

        List<EntityDesc> teamList  = accessDomainRepository.findEntityIdByAccountIdInAndEntityTypeIdAndIsActive(accountIdsList, com.tse.core_application.model.Constants.EntityTypes.TEAM, true);
        Map<Long, List<EntityDesc>> teamListMap = teamList.parallelStream().collect(Collectors.groupingBy(EntityDesc::getAccountId));
        for(Long accountId : accountIdsList){

            if(teamListMap.get(accountId) == null){
                continue;
            }
            List<Long> validTeamIdsList = teamListMap.get(accountId).stream().map(EntityDesc::getEntityId).collect(Collectors.toList());
            HashSet<Long> validTeamIdsSet  = new HashSet<>(validTeamIdsList);

            OrgId orgIdObject = userAccountRepository.findOrgIdByAccountIdAndIsActive(accountId, true);
            Long validOrgId = orgIdObject.getOrgId();

            // find all teams related to the account id


            if(orgId != null) {
                if (!orgId.equals(validOrgId)) {
                    continue;
                } else {
                    if (teamIds != null && !teamIds.isEmpty()) {
                        finalTeamIdsList.addAll(teamIds);
                    } else {
                        List<Long> teamIdsByOrgList = teamRepository.findTeamIdsByOrgId(orgId);
                        for (Long teamIdInOrg : teamIdsByOrgList) {
                            if (validTeamIdsSet.contains(teamIdInOrg)) {
                                finalTeamIdsList.add(teamIdInOrg);
                            }

                            finalTeamIdsList.addAll(validTeamIdsList);
                        }
                    }
                }
            }
            else{
                if(teamIds != null && !teamIds.isEmpty()){
                    for(Long teamIdInRequest : teamIds){
                        if(validTeamIdsSet.contains(teamIdInRequest)){
                            finalTeamIdsList.add(teamIdInRequest);
                        }
                    }
                }
                else{
                    finalTeamIdsList.addAll(validTeamIdsList);
                }
            }

        }

        return finalTeamIdsList;
    }

    /** This method returns whether the user with input accountIds and teamIds has higher roles or not (higher roles defined in constants)*/
    public boolean isHigherRolePresent(List<Long> accountIdsList, List<Long> validTeamIds){

        List<Integer> allRoleIds = accessDomainRepository.findAllRoleIdsByAccountIdsEntityTypeIdAndEntityIdsAndIsActive(accountIdsList, com.tse.core_application.model.Constants.EntityTypes.TEAM, validTeamIds, true);

        HashSet<Integer> allRoleIdsSet = new HashSet<>(allRoleIds);

        for(Integer roleId : com.tse.core_application.model.Constants.HIGHER_ROLE_IDS){
            if(allRoleIdsSet.contains(roleId)){
                return true;
            }
        }
        return false;

    }

    /** This method returns all the meeting ids from the attendee table using input accountIds and validTeamIds of the user */
    public List<Long> getAllMeetingIdsFromAttendeeTableByTeamIdAndAccountIds(String accountIds, List<Long> validTeamIds){

        List<Long> accountIdsList = Arrays.stream(accountIds.split(",")).map(Long::valueOf).collect(Collectors.toList());

        List<Long> allMeetingIds = attendeeRepository.findAllMeetingIdsByAccountIdsAndTeamIds(accountIdsList,validTeamIds, Constants.MeetingAttendeeInvitationStatus.ATTENDEE_INVITED_ID);

        return allMeetingIds;
    }

    /** This method returns a condensed meeting response having all the recurring meetings and one time meetings with distinction using input filters in request */
    public List<AllMeetingsCondensedResponse> getAllMeetingsCondensedResponse(MeetingCondensedViewRequest condensedViewRequest, String accountIds, String desiredTimeZone) throws IllegalAccessException{

        LocalDateTime fromDate = condensedViewRequest.getFromDate();
        LocalDateTime toDate = condensedViewRequest.getToDate();
        List<Long> accountIdList = CommonUtils.convertToLongList(accountIds);
        List<Long> validTeamIds = getAllValidTeamIdsByInputFilters(condensedViewRequest.getOrgId(),condensedViewRequest.getTeamIds(),accountIds);
        HashSet<Long> validTeamIdsSet = new HashSet<>(validTeamIds);
        List<Long> validProjectIds = accessDomainRepository.getProjectInfoByAccountIdsAndIsActiveTrue(accountIdList).stream().map(Project::getProjectId).collect(Collectors.toList());

        LocalDateTime fromDateServer = null , toDateServer = null;
        if(condensedViewRequest.getFromDate() != null)
        fromDateServer = convertUserDateToServerTimezone(condensedViewRequest.getFromDate(), desiredTimeZone);
        if(condensedViewRequest.getToDate() != null)
         toDateServer = convertUserDateToServerTimezone(condensedViewRequest.getToDate(), desiredTimeZone);

        List<AllMeetingsCondensedResponse> allMeetingsList = new ArrayList<>();

        List<RecurringMeeting> recurringMeetingDbList = null;
        if(fromDateServer != null && toDateServer != null) {
            // TODO This query needs to be modified by adding condition for team id and org id also
             recurringMeetingDbList = recurringMeetingRepository.findByRecurringMeetingEndDateTimeGreaterThanEqualAndRecurringMeetingStartDateTimeLessThanEqual(fromDateServer, toDateServer);
        }
        else{
            recurringMeetingDbList = recurringMeetingRepository.findAll(Sort.by("recurringMeetingId").descending());
        }

        if(recurringMeetingDbList !=null && !recurringMeetingDbList.isEmpty()) {
            for (RecurringMeeting recurringMeetingDb : recurringMeetingDbList) {

                if(!validTeamIdsSet.contains(recurringMeetingDb.getTeamId())){
                   continue;
                }

                List<LocalDateTime> recurringMeetingStartDates = new ArrayList<>();

                recurringMeetingStartDates = meetingRepository.findStartDateTimeByRecurringMeetingId(recurringMeetingDb.getRecurringMeetingId(), fromDateServer, toDateServer);

                if(!recurringMeetingStartDates.isEmpty()) {
                    recurringMeetingStartDates = recurringMeetingStartDates.stream().map(date -> convertServerDateToUserTimezone(date, desiredTimeZone)).collect(Collectors.toList());
//                    List<LocalDateTime> recurringMeetingStartDatesDb = recurringMeetingDb.getMeetingStartDateTimeList();
//
//                    // This loop is filtering only those dates in recurring meetings dates list that lies between fromDate and toDate.
//                    recurringMeetingStartDates = recurringMeetingStartDatesDb.stream().filter(date -> (date.isAfter(fromDateServer) || date.isEqual(fromDateServer)) && (date.isBefore(toDateServer) || date.isEqual(toDateServer)))
//                            .map(date -> convertServerDateToUserTimezone(date, desiredTimeZone))
//                            .collect(Collectors.toList());

                    AllMeetingsCondensedResponse allMeetingsCondensedResponse = new AllMeetingsCondensedResponse();

                    copySimilarFields(recurringMeetingDb, allMeetingsCondensedResponse);

                    List<Attendee> attendeeListFromDb = new ArrayList<>();

                    if(recurringMeetingDb.getAttendeeAccounts() != null && !recurringMeetingDb.getAttendeeAccounts().isEmpty()){

                        for(Long attendeeAccountId : recurringMeetingDb.getAttendeeAccounts()){

                            Attendee attendee = new Attendee();
                            attendee.setAccountId(attendeeAccountId);
                            attendee.setTeamId(recurringMeetingDb.getTeamId());
                            attendee.setBuId(recurringMeetingDb.getBuId());
                            attendee.setProjectId(recurringMeetingDb.getProjectId());
                            attendeeListFromDb.add(attendee);

                        }

                        allMeetingsCondensedResponse.setAttendeeResponseList(attendeeListFromDb);

                    }

                    allMeetingsCondensedResponse.setRecurringMeetingsStartDates(recurringMeetingStartDates);

                    if (recurringMeetingDb.getTeamId() != null) {
                        String teamName = teamRepository.findTeamNameByTeamId(recurringMeetingDb.getTeamId());
                        allMeetingsCondensedResponse.setEntityName(teamName);
                    }

                    allMeetingsList.add(allMeetingsCondensedResponse);
                }

            }
        }


        boolean isUserHigherRole = false;
        List<Long> accountIdsList = Arrays.stream(accountIds.split(",")).map(Long::valueOf).collect(Collectors.toList());
        if(isHigherRolePresent(accountIdsList,validTeamIds)){
            isUserHigherRole = true;
        }

        List<Meeting> meetingList = null;
        if(fromDateServer !=null && toDateServer != null)
           // TODO these queries needed to be modified by adding team id and org id filters to fetch only related meetings
         meetingList = meetingRepository.findByStartDateTimeGreaterThanEqualAndStartDateTimeLessThanEqualAndRecurringMeetingRecurringMeetingIdIsNullOrderByStartDateTimeAsc(fromDateServer, toDateServer);
        else{
            meetingList = meetingRepository.findByRecurringMeetingRecurringMeetingIdIsNullOrderByMeetingIdDesc();
        }

        List<Long> allMeetingIdsFromAttendee = getAllMeetingIdsFromAttendeeTableByAccountIds(accountIds);
        List<Meeting> filteredMeetingList = new ArrayList<>();

        for(Meeting meeting : meetingList){
            if(isUserHigherRole){
                if(validTeamIdsSet.contains(meeting.getTeamId()) || (meeting.getTeamId() == null && validProjectIds.contains(meeting.getProjectId()))){
                    filteredMeetingList.add(meeting);
                }
            }else {
                if (allMeetingIdsFromAttendee != null && !allMeetingIdsFromAttendee.isEmpty() &&  allMeetingIdsFromAttendee.contains(meeting.getMeetingId())){
                    filteredMeetingList.add(meeting);
                }
            }
        }

        if(!filteredMeetingList.isEmpty()) {
             for (Meeting meeting : filteredMeetingList) {

                 if(meeting.getStartDateTime() != null) {
                     AllMeetingsCondensedResponse allMeetingsCondensedResponse = new AllMeetingsCondensedResponse();

                     meeting.setStartDateTime(convertServerDateToUserTimezone(meeting.getStartDateTime(), desiredTimeZone));
                     List<LocalDateTime> meetingStartDateTime = new ArrayList<>();
                     meetingStartDateTime.add(meeting.getStartDateTime());

                     copySimilarFields(meeting, allMeetingsCondensedResponse);

                     if(meeting.getAttendeeList() != null && !meeting.getAttendeeList().isEmpty()){

                         //adding only invited attendees in condensed view list
                         List<Attendee> attendeeList = new ArrayList<>();
                         for (Attendee attendee:meeting.getAttendeeList()) {
                            if (Objects.equals(attendee.getAttendeeInvitationStatusId(),Constants.MeetingAttendeeInvitationStatus.ATTENDEE_INVITED_ID)) {
                                attendeeList.add(attendee);
                            }
                         }
                         allMeetingsCondensedResponse.setAttendeeResponseList(attendeeList);

                     }
                     allMeetingsCondensedResponse.setRecurringMeetingsStartDates(meetingStartDateTime);

                     allMeetingsCondensedResponse.setMeetingType(setMeetingType(meeting.getMeetingTypeIndicator()));

                     if (meeting.getTeamId() != null) {
                         String teamName = teamRepository.findTeamNameByTeamId(meeting.getTeamId());
                         allMeetingsCondensedResponse.setEntityName(teamName);
                     } else if (meeting.getProjectId() != null) {
                         String projectName = projectRepository.findProjectNameByProjectId(meeting.getProjectId());
                         allMeetingsCondensedResponse.setEntityName(projectName);
                     } else {
                         String orgName = organizationRepository.findOrganizationNameByOrgId(meeting.getOrgId());
                         allMeetingsCondensedResponse.setEntityName(orgName);
                     }

                     allMeetingsList.add(allMeetingsCondensedResponse);
                 }

            }
        }
            if(!allMeetingsList.isEmpty()){
            allMeetingsList.sort(Comparator.comparing(o -> o.getRecurringMeetingsStartDates().isEmpty() ? null :  o.getRecurringMeetingsStartDates().get(0)));

//            allMeetingsList.sort(new Comparator<AllMeetingsCondensedResponse>() {
//                @Override
//                public int compare(AllMeetingsCondensedResponse o1, AllMeetingsCondensedResponse o2) {
//                    return o1.getRecurringMeetingsStartDates().get(0).compareTo(o2.getRecurringMeetingsStartDates().get(0));
//                }
//            });
            }

        return allMeetingsList;
    }

    /** This method returns expanded meeting response having all the detailed meeting responses from meeting table  based on the input filters*/
    public  AllMeetingsExpandedResponse getAllMeetingsExpandedView(MeetingExpandedViewRequest expandedViewRequest, String accountIds, String desiredTimeZone) throws IllegalAccessException{

        LocalDateTime fromDateServer = null, toDateServer = null;
        if(expandedViewRequest.getFromDate() != null && expandedViewRequest.getToDate() != null) {
            fromDateServer = convertUserDateToServerTimezone(expandedViewRequest.getFromDate(), desiredTimeZone);
            toDateServer = convertUserDateToServerTimezone(expandedViewRequest.getToDate(), desiredTimeZone);
        }

        MeetingFiltersRequest meetingFiltersRequest = new MeetingFiltersRequest();
        meetingFiltersRequest.setFromDate(fromDateServer);
        meetingFiltersRequest.setToDate(toDateServer);
        meetingFiltersRequest.setOrgId(expandedViewRequest.getOrgId());
        meetingFiltersRequest.setTeamIds(expandedViewRequest.getTeamIds());
        meetingFiltersRequest.setCreatedAccountId(expandedViewRequest.getCreatedAccountId());
        meetingFiltersRequest.setOrganizerAccountId(expandedViewRequest.getOrganizerAccountId());
        meetingFiltersRequest.setAttendeeAccountIds(expandedViewRequest.getAttendeeAccountIds());

        List<MeetingResponse> meetingResponseList = new ArrayList<>();

        meetingResponseList = getAllMeetingsByFilters(meetingFiltersRequest, accountIds, desiredTimeZone);

        List<Long> accountIdsLong = Arrays.stream(accountIds.split(",")).map(Long::valueOf).collect(Collectors.toList());

        List<MeetingResponse> meetingWithReferenceTasks = meetingResponseList.stream().filter(meeting ->
                meeting.getReferenceEntityNumber() != null && Objects.equals(meeting.getReferenceEntityTypeId(), com.tse.core_application.model.Constants.EntityTypes.TASK)
        ).collect(Collectors.toList());

        if (!meetingWithReferenceTasks.isEmpty()) {
            List<String> referenceTasks = new ArrayList<>();
            List<Long> teamIds = new ArrayList<>();
            meetingWithReferenceTasks.forEach(meeting -> {
                referenceTasks.add(meeting.getReferenceEntityNumber());
                teamIds.add(meeting.getTeamId());
            });
            List<TaskValidationDto> taskValidationDtoList = taskRepository.findByTaskIdentifierInAndFkTeamIdTeamIdIn(referenceTasks, teamIds);
            if (!taskValidationDtoList.isEmpty()) {
                Map<Long, TaskValidationDto> taskValidationDtoMap = taskValidationDtoList.stream().collect(Collectors.toMap(TaskValidationDto::getMeetingId, taskValidationDto -> taskValidationDto));
                meetingResponseList.forEach(meetingResponse ->
                        {
                            TaskValidationDto taskValidationDto = taskValidationDtoMap.get(meetingResponse.getMeetingId());
                            if (taskValidationDto != null && taskValidationDto.getAssignedToAccountId() != null && accountIdsLong.contains(taskValidationDto.getAssignedToAccountId())) {
                                meetingResponse.setShowUserPerceivedPercentage(Boolean.TRUE);
                            }
                            if (taskValidationDto != null && taskValidationDto.getUserPerceivePercentageTaskCompleted() != null) {
                                meetingResponse.setReferenceTaskUserPerceivedPercentage(taskValidationDto.getUserPerceivePercentageTaskCompleted());
                            }
                        }
                );
            }
        }
        //it will set "isEditable" & "canEditMeetingAccess"
        bulkValidateIsEditableForAttendee(meetingResponseList, accountIdsLong);

        AllMeetingsExpandedResponse allMeetingsExpandedResponse = new AllMeetingsExpandedResponse();

        allMeetingsExpandedResponse.setMeetingResponses(meetingResponseList);
        allMeetingsExpandedResponse.setFromDate(expandedViewRequest.getFromDate());
        allMeetingsExpandedResponse.setToDate(expandedViewRequest.getToDate());

        return allMeetingsExpandedResponse;

    }

    /** This method updates the recurring meeting using recurring meeting id. It does not update the meeting start time*/
    public RecurringMeetingResponse updateRecurringMeetingByRecurringId(RecurringMeetingRequest recurringMeetingRequest, Long recurringMeetingId, String desiredTimeZone, String accountIds) throws IllegalAccessException{

        RecurringMeeting recurringMeetingDb = recurringMeetingRepository.findByRecurringMeetingId(recurringMeetingId);

        if(recurringMeetingDb == null){
            throw new MeetingNotFoundException();
        }
        LocalDate currDate = LocalDate.now();
        LocalDate startDates = recurringMeetingDb.getRecurringMeetingStartDateTime().toLocalDate();
        LocalDate maxValidDate = currDate.minusDays(Constants.Meeting_Preferrences.PAST_MEETING_DAYS_LIMIT);
        if(startDates.isBefore(maxValidDate)){
            throw new ValidationFailedException("You cannot update meeting for past date before 8 days from now");
        }

        if (!meetingEditAccess(recurringMeetingDb.getOrganizerAccountId(), recurringMeetingDb.getCreatedAccountId(), recurringMeetingDb.getTeamId(), accountIds)) {
            throw new ValidationFailedException("You are not authorized to update this meeting");
        }

        LocalTime convertedStartTime = null;

        if(recurringMeetingRequest.getIsCancelled() != null && recurringMeetingRequest.getIsCancelled()){
            recurringMeetingDb.setIsCancelled(true);
        }
        else {

            convertedStartTime = convertUserTimeToServerTimeZone(recurringMeetingRequest.getMeetingStartTime(), desiredTimeZone);

            recurringMeetingDb.setTitle(recurringMeetingRequest.getTitle());
            recurringMeetingDb.setAgenda(recurringMeetingRequest.getAgenda());
            recurringMeetingDb.setDuration(recurringMeetingRequest.getDuration());
            recurringMeetingDb.setMeetingType(recurringMeetingRequest.getMeetingType());
            recurringMeetingDb.setMeetingKey(recurringMeetingRequest.getMeetingKey());
            recurringMeetingDb.setOrganizerAccountId(recurringMeetingRequest.getOrganizerAccountId());
            recurringMeetingDb.setReferenceEntityNumber(recurringMeetingRequest.getReferenceEntityNumber());
            recurringMeetingDb.setReferenceEntityTypeId(recurringMeetingRequest.getReferenceEntityTypeId());
            recurringMeetingDb.setVenue(recurringMeetingRequest.getVenue());
            recurringMeetingDb.setUpdatedAccountId(recurringMeetingRequest.getUpdatedAccountId());
            recurringMeetingDb.setReminderTime(recurringMeetingRequest.getReminderTime());
            recurringMeetingDb.setMeetingStartTime(convertedStartTime);

            if(recurringMeetingRequest.getAttendeeRequestList() != null && !recurringMeetingRequest.getAttendeeRequestList().isEmpty()){
                List<AttendeeRequest> attendeeRequestList = recurringMeetingRequest.getAttendeeRequestList();
                List<Long> attendeeAccounts = attendeeRequestList.stream().map(AttendeeRequest::getAccountId).collect(Collectors.toList());
                if(!attendeeAccounts.contains(recurringMeetingRequest.getOrganizerAccountId())) {
                    attendeeAccounts.add(recurringMeetingRequest.getOrganizerAccountId());
                }

                recurringMeetingDb.setAttendeeAccounts(attendeeAccounts);
            }
        }
        addLabelsToRecurringMeeting(recurringMeetingDb, recurringMeetingRequest.getLabelsToAdd());
        recurringMeetingRepository.save(recurringMeetingDb);

        List<Long> meetingIdsList = meetingRepository.findMeetingIdsByRecurringMeetingId(recurringMeetingId);

        RecurringMeetingResponse response = new RecurringMeetingResponse();
        MeetingRequest meetingRequest = new MeetingRequest();
        copySimilarFields(recurringMeetingRequest, meetingRequest);

        // TO DO : set attendees in meeting request.

//        convertAllMeetingLocalDateAndTimeToServerTimeZone(meetingRequest, desiredTimeZone);
//
//        validateMeeting(meetingRequest);

        Meeting updatedMeeting = new Meeting();
        int trackForNotification = 0;
        for(Long meetingId : meetingIdsList){
            trackForNotification++;
            Meeting meetDb = meetingRepository.findByMeetingId(meetingId);

            if(meetDb == null){
                throw new MeetingNotFoundException();
            }
            // Start dates cannot be changed in individual meetings but time can be changed so we are taking start date from DB but start time can be changed and its updated valued is taken
            LocalDate startDate = meetDb.getStartDateTime().toLocalDate();
            LocalTime startTime = recurringMeetingDb.getMeetingStartTime();
            if(convertedStartTime != null ) {
                startTime = convertedStartTime;
            }
            meetingRequest.setStartDateTime(LocalDateTime.of(startDate, startTime));
            meetingRequest.setMeetingNumber(meetDb.getMeetingNumber());
            meetingRequest.setMeetingId(meetingId);
//            meetingRequest.setRecurringMeeting(recurringMeetingDb);
//            meetingRequest.setRecurringMeetingId(recurringMeetingId);

            List<Long> deletedAttendeesAccountIds = getdeletedAttendeesAccountIdList(meetingRequest, meetDb);
            ArrayList<String> updateMeetingFieldsByUser = getMeetingFieldsToUpdate(meetingRequest, meetingId, deletedAttendeesAccountIds);
            updateMeetingFieldsByUser.remove("startDateTime");

            if (!updateMeetingFieldsByUser.isEmpty()) {
                if(trackForNotification > 1) {
                    updatedMeeting = updateAllFieldsInMeetingTable(meetingRequest, meetingId, updateMeetingFieldsByUser, deletedAttendeesAccountIds,recurringMeetingDb, desiredTimeZone, false);
                } else {
                    updatedMeeting = updateAllFieldsInMeetingTable(meetingRequest, meetingId, updateMeetingFieldsByUser, deletedAttendeesAccountIds,recurringMeetingDb, desiredTimeZone, true);
                }
            }
        }

        copySimilarFields(recurringMeetingDb, response);

        List<Attendee> updatedAttendees = new ArrayList<>();

        for(AttendeeRequest attendee : recurringMeetingRequest.getAttendeeRequestList()){
            Attendee a = new Attendee();
            a.setAccountId(attendee.getAccountId());
            a.setTeamId(attendee.getTeamId());
            a.setBuId(attendee.getBuId());
            a.setProjectId(attendee.getProjectId());
            updatedAttendees.add(a);
        }

        response.setAttendeeRequestList(updatedAttendees);

        if(recurringMeetingDb.getRecurMeetingLabels() != null) {
            List<Label> labelsSaved = recurringMeetingDb.getRecurMeetingLabels();
            List<LabelResponse> labelResponseList = new ArrayList<>();
            for(Label label: labelsSaved) {
                LabelResponse labelResponse = new LabelResponse();
                labelResponse.setLabelId(label.getLabelId());
                labelResponse.setLabelName(label.getLabelName());
                labelResponseList.add(labelResponse);
            }
            response.setLabels(labelResponseList);
        }

        if(recurringMeetingRequest.getTeamId() != null){
            String teamName = teamRepository.findTeamNameByTeamId(recurringMeetingRequest.getTeamId());
            response.setEntityName(teamName);
        }  else if (recurringMeetingRequest.getProjectId() != null) {
            String projectName = projectRepository.findProjectNameByProjectId(recurringMeetingRequest.getProjectId());
            response.setEntityName(projectName);
        } else {
            String orgName = organizationRepository.findOrganizationNameByOrgId(recurringMeetingRequest.getOrgId());
            response.setEntityName(orgName);
        }

        return response;
    }

    /** This method returns whether a given account id and team id in input is having any higher role or not */
    public boolean isHigherRole(Long accountId, Long teamId){

        List<Integer> roleIdsList = accessDomainRepository.findRoleIdsByAccountIdEntityTypeIdAndEntityIdAndIsActive(accountId, com.tse.core_application.model.Constants.EntityTypes.TEAM, teamId);
        HashSet<Integer> roleIdsSet = new HashSet<>(roleIdsList);

        for(Integer id : com.tse.core_application.model.Constants.HIGHER_ROLE_IDS){
            if(roleIdsSet.contains(id)){
                return true;
            }
        }
        return false;
    }

    /** This method is used to get all the meetings based on the filters provided in input */
    public List<MeetingResponse> getAllMeetingsByFilters(MeetingFiltersRequest meetingFiltersRequest, String accountIds, String desiredTimeZone) {

        List<MeetingResponse> meetingResponseList = new ArrayList<>();
        List<Long> accountIdsList = Arrays.stream(accountIds.split(",")).map(Long::valueOf).collect(Collectors.toList());
        List<Long> validProjectIds = accessDomainRepository.getProjectInfoByAccountIdsAndIsActiveTrue(accountIdsList).stream().map(Project::getProjectId).collect(Collectors.toList());
        List<EntityDesc> orgList = userAccountRepository.findOrgDescByAccountIdsAndIsActive(accountIdsList, true);
        Map<Long, EntityDesc> orgIdMap = orgList.parallelStream().collect(Collectors.toMap(EntityDesc::getAccountId, entityDesc -> entityDesc));

        List<EntityDesc> entityDescList = accessDomainRepository.findEntityIdByAccountIdInAndEntityTypeIdAndIsActive(accountIdsList, com.tse.core_application.model.Constants.EntityTypes.TEAM, true);
        List<Long> teamList = entityDescList.parallelStream().map(EntityDesc::getEntityId).distinct().collect(Collectors.toList());
        Map<Long, List<EntityDesc>> teamListMap = entityDescList.parallelStream().collect(Collectors.groupingBy(EntityDesc::getAccountId));

        //meetingIds at team level
        List<MeetingAccountEntityMapDto> meetingIdsListFromTeamAttendee = attendeeRepository.findMeetingIdByAccountIdInAndTeamIdIn(accountIdsList, teamList, Constants.MeetingAttendeeInvitationStatus.ATTENDEE_INVITED_ID);
        Map<String, List<Long>> meetingMapWithAccountTeamId = meetingIdsListFromTeamAttendee.parallelStream().collect(
                Collectors.groupingByConcurrent(map -> map.getAccountId() + "_" + map.getEntityId(),
                        Collectors.mapping(MeetingAccountEntityMapDto::getMeetingId, Collectors.toList())));

        //meetingIds at project level
        List<MeetingAccountEntityMapDto> meetingIdsListFromProjectAttendee = attendeeRepository.findMeetingIdByAccountIdInAndProjectIdInAndTeamIdNull(accountIdsList, validProjectIds, Constants.MeetingAttendeeInvitationStatus.ATTENDEE_INVITED_ID);
        Map<String, List<Long>> meetingMapWithAccountProjectId = meetingIdsListFromProjectAttendee.parallelStream().collect(
                Collectors.groupingByConcurrent(map -> map.getAccountId() + "_" + map.getEntityId(),
                        Collectors.mapping(MeetingAccountEntityMapDto::getMeetingId, Collectors.toList())));

        //meetingIds when projectId and teamId is Null.
        List<MeetingAccountEntityMapDto> meetingIdsListFromAccountId = attendeeRepository.findMeetingIdByAccountIdInAndTeamIdNullAndProjectIdNull(accountIdsList, Constants.MeetingAttendeeInvitationStatus.ATTENDEE_INVITED_ID);
        Map<Long, List<Long>> meetingMapWithAccount = meetingIdsListFromAccountId.parallelStream().collect(
                Collectors.groupingByConcurrent(MeetingAccountEntityMapDto::getAccountId,
                        Collectors.mapping(MeetingAccountEntityMapDto::getMeetingId, Collectors.toList())));

        List<Long> eligibleTeamIds = new ArrayList<>();
        List<Long> finalMeetingIds = new ArrayList<>();
        List<Meeting> meetingsFromDb = new ArrayList<>();

        for (Long accountId : accountIdsList) {
            // get org id by account id
            Long orgId = orgIdMap.get(accountId).getEntityId();

            // find all teams related to the account id
            HashSet<Long> validTeamIdsSet = teamListMap.get(accountId) != null ?
                    teamListMap.get(accountId).stream().map(EntityDesc::getEntityId).collect(Collectors.toCollection(HashSet::new)) : new HashSet<>();

            HashSet<Long> finalTeamIdsSet = new HashSet<>();
            if (meetingFiltersRequest.getOrgId() != null) {
                if (meetingFiltersRequest.getOrgId().equals(orgId) && meetingFiltersRequest.getTeamIds() != null && !meetingFiltersRequest.getTeamIds().isEmpty()) {
                        finalTeamIdsSet.addAll(meetingFiltersRequest.getTeamIds());
                    }

            } else {
                if (!validTeamIdsSet.isEmpty()) {
                    if (meetingFiltersRequest.getTeamIds() != null && !meetingFiltersRequest.getTeamIds().isEmpty()) {
                        for (Long teamIdInRequest : meetingFiltersRequest.getTeamIds()) {
                            if (validTeamIdsSet.contains(teamIdInRequest)) {
                                finalTeamIdsSet.add(teamIdInRequest);
                            }
                        }
                    } else {
                        finalTeamIdsSet.addAll(validTeamIdsSet);
                    }
                }
            }

            List<Long> finalTeamIdsList = new ArrayList<>(finalTeamIdsSet);

            for (Long teamId : finalTeamIdsList) {

                if (isHigherRole(accountId, teamId)) {
                    eligibleTeamIds.add(teamId);
                } else {
                    List<Long> meetingIdsListFromAttendeesMap = meetingMapWithAccountTeamId.get(accountId + "_" + teamId);
                    if (meetingIdsListFromAttendeesMap != null && !meetingIdsListFromAttendeesMap.isEmpty()) {
                        finalMeetingIds.addAll(meetingIdsListFromAttendeesMap);
                    }


                }
            }
            if (meetingFiltersRequest.getTeamIds() == null || meetingFiltersRequest.getTeamIds().isEmpty()) {
                if (meetingFiltersRequest.getProjectIds() != null && !meetingFiltersRequest.getProjectIds().isEmpty()) {
                    for (Long projectId : meetingFiltersRequest.getProjectIds()) {
                        List<Long> meetingIdsListFromAttendeeProject = meetingMapWithAccountProjectId.get(accountId + "_" + projectId);
                        if (meetingIdsListFromAttendeeProject != null && !meetingIdsListFromAttendeeProject.isEmpty()) {
                            finalMeetingIds.addAll(meetingIdsListFromAttendeeProject);
                        }
                    }
                } else if (meetingFiltersRequest.getOrgId() == null && !meetingMapWithAccountProjectId.isEmpty()) {
                    for (Long projectId : validProjectIds) {
                        List<Long> meetingIdsListFromAttendeeProject = meetingMapWithAccountProjectId.get(accountId + "_" + projectId);
                        if (meetingIdsListFromAttendeeProject != null && !meetingIdsListFromAttendeeProject.isEmpty()) {
                            finalMeetingIds.addAll(meetingIdsListFromAttendeeProject);
                        }
                    }
                }
                List<Long> meetingIdsListFromAccountAttendee = meetingMapWithAccount.get(accountId);
                if (meetingIdsListFromAccountAttendee != null && !meetingIdsListFromAccountAttendee.isEmpty()) {
                    finalMeetingIds.addAll(meetingIdsListFromAccountAttendee);
                }

            }
        }
        if (!eligibleTeamIds.isEmpty()) {
            if (meetingFiltersRequest.getFromDate() != null && meetingFiltersRequest.getToDate() != null) {
                meetingsFromDb.addAll(meetingRepository.findByTeamIdInAndStartDateTimeGreaterThanEqualAndEndDateTimeLessThanEqualOrderByStartDateTimeAsc(eligibleTeamIds, meetingFiltersRequest.getFromDate(), meetingFiltersRequest.getToDate()));
            } else {
                meetingsFromDb.addAll(meetingRepository.findByTeamIdIn(eligibleTeamIds));
            }
        }

        if (!finalMeetingIds.isEmpty()) {
            if (meetingFiltersRequest.getFromDate() != null && meetingFiltersRequest.getToDate() != null) {
                meetingsFromDb.addAll(meetingRepository.findByFromDateAndToDateAndMeetingIdsIn(meetingFiltersRequest.getFromDate(), meetingFiltersRequest.getToDate(), finalMeetingIds));
            } else {
                meetingsFromDb.addAll(meetingRepository.findByMeetingIdsIn(finalMeetingIds));
            }
        }

        if (!meetingsFromDb.isEmpty()) {
            meetingsFromDb.sort(Comparator.comparing(Meeting::getStartDateTime).thenComparing(Meeting::getMeetingNumber));
            meetingResponseList.addAll(bulkCreateMeetingResponseFromMeeting(meetingsFromDb, desiredTimeZone));
        }
        return filterMeetingResponses(meetingResponseList, meetingFiltersRequest.getAttendeeAccountIds(), meetingFiltersRequest.getOrganizerAccountId(), meetingFiltersRequest.getCreatedAccountId());
    }
    /** This method is used to update the meeting status based on the organizer response provided in the request*/
    public MeetingOrganizerResponse updateOrganiserResponseAndMeetingStatus(MeetingOrganizerRequest organizerRequest, String accountIds, String desiredTimeZone, Meeting meetDb) {

        MeetingOrganizerResponse organizerResponse = new MeetingOrganizerResponse();
        HashSet<Long> accountIdsSet = Arrays.stream(accountIds.split(",")).map(Long::valueOf).collect(Collectors.toCollection(HashSet::new));

        // validation: the account updating the response is organizer of the meeting
        if (!accountIdsSet.contains(meetDb.getOrganizerAccountId())) {
            throw new ValidationFailedException("You are not authorized to update the meeting status");
        }

        // validation: if both the hasMeetingStarted & hasMeetingEnded are null or both are not null in the updateOrganizer request, throw an exception
        if((organizerRequest.getHasMeetingEnded() != null && organizerRequest.getHasMeetingStarted() !=null) || (organizerRequest.getHasMeetingStarted() == null && organizerRequest.getHasMeetingEnded() == null)) {
            throw new ValidationFailedException("invalid request: both isMeetingStarted/ isMeetingEnded responses are received");
        }

        // validation: if hasMeetingStarted is not null & not false, then the actualStartDateTime should also be not null
        if(organizerRequest.getHasMeetingStarted() != null && organizerRequest.getHasMeetingStarted() && organizerRequest.getActualStartDateTime() == null) {
            throw new ValidationFailedException("invalid request: actual start date time should be not null if the meeting is started");
        }

        // validation: if hasMeeting Ended is not null & not false, then the actualEndDateTime should also be not null
        if(organizerRequest.getHasMeetingEnded() !=null && organizerRequest.getHasMeetingEnded() && organizerRequest.getActualEndDateTime() == null) {
            throw new ValidationFailedException("invalid request: actual end date time should be not null if the meeting is ended");
        }

        // validation: block multiple response to the same notification for update organizer response
        if((meetDb.getActualStartDateTime() != null && organizerRequest.getHasMeetingStarted() != null && organizerRequest.getHasMeetingStarted()) ||
                (meetDb.getActualEndDateTime() != null && organizerRequest.getHasMeetingEnded() != null && organizerRequest.getHasMeetingEnded())) {
            throw new ValidationFailedException("Error: you've already provided a response for this");
        }

        LocalDateTime convertedActualStartDateTime = null, convertedActualEndDateTime = null;
        if(organizerRequest.getActualStartDateTime() != null) {
             convertedActualStartDateTime = convertUserDateToServerTimezone(organizerRequest.getActualStartDateTime(), desiredTimeZone);
        }
        if(organizerRequest.getActualEndDateTime() != null) {
             convertedActualEndDateTime = convertUserDateToServerTimezone(organizerRequest.getActualEndDateTime(), desiredTimeZone);
        }

        // when a meeting is created, the meeting progress is scheduled by default
        if(meetDb.getMeetingProgress() == MeetingStats.MEETING_SCHEDULED && organizerRequest.getHasMeetingStarted() != null) {
            if(organizerRequest.getHasMeetingStarted()) {
                meetDb.setMeetingProgress(MeetingStats.MEETING_STARTED);
                meetDb.setActualStartDateTime(organizerRequest.getActualStartDateTime());
            } else {
                meetDb.setMeetingProgress(MeetingStats.MEETING_DELAYED);
            }
        }

        // if isMeetingEnded response was given first as hasMeetingEnded = true and hasMeetingStarted response was given later -- we just update the actual start date time
        else if((meetDb.getMeetingProgress() == MeetingStats.MEETING_ENDED || meetDb.getMeetingProgress() == MeetingStats.MEETING_ENDED_WITH_OVER_RUN || meetDb.getMeetingProgress() == MeetingStats.MEETING_OVER_RUN) && organizerRequest.getHasMeetingStarted() != null && organizerRequest.getHasMeetingStarted()) {
            meetDb.setActualStartDateTime(organizerRequest.getActualStartDateTime());
        }

        // The user has marked meeting ended via meeting end time notification.
        else if (organizerRequest.getHasMeetingEnded() != null) {
            // If meeting status is scheduled or started or delayed and hasMeetingEnded is not null then status is changed to either meeting ended(if hasMeetingEnded is true) or meeting over run (if false).
            if (meetDb.getMeetingProgress() == MeetingStats.MEETING_SCHEDULED || meetDb.getMeetingProgress() == MeetingStats.MEETING_STARTED || meetDb.getMeetingProgress() == MeetingStats.MEETING_DELAYED) {
                if (meetDb.getMeetingProgress() == MeetingStats.MEETING_DELAYED) {
                    if (meetDb.getActualStartDateTime() == null) {
                        if (organizerRequest.getActualStartDateTime() != null) {
                            if (organizerRequest.getActualStartDateTime().isBefore(meetDb.getStartDateTime())) {
                                throw new ValidationFailedException("Actual start date time cannot be before expected time in case of meeting delayed");
                            }
                            meetDb.setActualStartDateTime(convertedActualStartDateTime);
                        } else {
                            throw new ValidationFailedException("Please provide actual start date time in case of delayed/ scheduled meeting");
                        }
                    }
                } else if (meetDb.getMeetingProgress() == MeetingStats.MEETING_SCHEDULED) {
                    if (meetDb.getActualStartDateTime() == null) {
                        if (organizerRequest.getActualStartDateTime() != null) {
                            meetDb.setActualStartDateTime(convertedActualStartDateTime);
                        } else {
                            meetDb.setActualStartDateTime(meetDb.getStartDateTime());
                        }
                    }
                }

                organizerRequest.setHasMeetingStarted(true);
                if (organizerRequest.getHasMeetingEnded()) {
                    meetDb.setMeetingProgress(MeetingStats.MEETING_ENDED);
                    meetDb.setActualEndDateTime(convertedActualEndDateTime);
                } else {
                    meetDb.setMeetingProgress(MeetingStats.MEETING_OVER_RUN);
                }
            }

            // This condition check that if meeting stats is over run (initial response to meeting ended notification was false),
            // then if request is having has meeting ended value as true and  actual end date,  then it is set in the meeting and meeting status is changed to meeting ended with over run.
            else if (meetDb.getMeetingProgress() == MeetingStats.MEETING_OVER_RUN) {
                if (organizerRequest.getHasMeetingEnded()) {
                    meetDb.setMeetingProgress(MeetingStats.MEETING_ENDED_WITH_OVER_RUN);
                    if (organizerRequest.getActualEndDateTime().isBefore(meetDb.getEndDateTime())) {
                        throw new ValidationFailedException("Actual end date time cannot be before expected time in case of meeting over run");
                    }
                    meetDb.setActualEndDateTime(convertedActualEndDateTime);
                }
            }
        }

        Meeting savedMeeting = meetingRepository.save(meetDb);

        //Meeting followup notification
        if (savedMeeting.getMeetingProgress() == MeetingStats.MEETING_ENDED || savedMeeting.getMeetingProgress() == MeetingStats.MEETING_ENDED_WITH_OVER_RUN) {
            try {
                List<HashMap<String, String>> payload = schedulingService.meetingFollowUpService(savedMeeting);
                taskServiceImpl.sendPushNotification(payload);
            } catch (Exception e) {
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error("Unable to create notifications for meeting followup: " + e, new Throwable(allStackTraces));
            }
        }

        organizerResponse.setMeetingProgress(savedMeeting.getMeetingProgress());
        organizerResponse.setHasMeetingEnded(organizerRequest.getHasMeetingEnded());
        organizerResponse.setHasMeetingStarted(organizerRequest.getHasMeetingStarted());
        if(savedMeeting.getActualStartDateTime() != null) {
            organizerResponse.setActualStartDateTime(convertServerDateToUserTimezone(savedMeeting.getActualStartDateTime(), desiredTimeZone));
            organizerResponse.setHasMeetingStarted(true);
        }
        if(savedMeeting.getActualEndDateTime() != null) {
            organizerResponse.setActualEndDateTime(convertServerDateToUserTimezone(savedMeeting.getActualEndDateTime(), desiredTimeZone));
            organizerResponse.setHasMeetingEnded(true);
        }

        return organizerResponse;
    }

    /**
     * @param entityTypeId
     * @param entityId
     * @param accountIdRemovedUser
     * @param accountIdAdmin
     * @param timeZone
     * cancel meetings where the user is an organizer and send cancel notification.
     */
    public void cancelOrganizedMeetingsByUserRemovedInEntityAndSendNotification(Integer entityTypeId, Long entityId, Long accountIdRemovedUser, Long accountIdAdmin, String timeZone) {
        // gets all the future meetings where the deleted user is an organizer
        List<Meeting> allFutureMeetings = new ArrayList<>();
        if (entityTypeId.equals(com.tse.core_application.model.Constants.EntityTypes.ORG)) {
            allFutureMeetings = meetingRepository.findByOrgIdAndOrganizerAccountIdAndIsCancelledAndStartDateTimeGreaterThan(entityId, accountIdRemovedUser, false, LocalDateTime.now());
        } else if (entityTypeId.equals(com.tse.core_application.model.Constants.EntityTypes.TEAM)) {
            allFutureMeetings = meetingRepository.findByTeamIdAndOrganizerAccountIdAndIsCancelledAndStartDateTimeGreaterThan(entityId, accountIdRemovedUser, false, LocalDateTime.now());
        }

        List<Long> recurringMeetingIds = new ArrayList<>();
        for (Meeting meeting : allFutureMeetings) {
            if (meeting.getRecurringMeeting() != null) {
                if (!recurringMeetingIds.contains(meeting.getRecurringMeeting().getRecurringMeetingId())) {
                    recurringMeetingIds.add(meeting.getRecurringMeeting().getRecurringMeetingId());
                    sendCancelMeetingNotification(meeting, accountIdRemovedUser, accountIdAdmin, timeZone, entityTypeId, entityId);
                }
                meeting.setIsCancelled(true);
                meeting.setUpdatedAccountId(accountIdAdmin);
                continue;
            }
            meeting.setIsCancelled(true);
            meeting.setUpdatedAccountId(accountIdAdmin);
            sendCancelMeetingNotification(meeting, accountIdRemovedUser, accountIdAdmin, timeZone, entityTypeId, entityId);
        }
        List<RecurringMeeting> allFutureRecurringMeetings = recurringMeetingRepository.findByRecurringMeetingIdIn(recurringMeetingIds);
        for (RecurringMeeting recurringMeeting : allFutureRecurringMeetings) {
            recurringMeeting.setIsCancelled(true);
            recurringMeeting.setUpdatedAccountId(accountIdAdmin);
        }
        meetingRepository.saveAll(allFutureMeetings);
        recurringMeetingRepository.saveAll(allFutureRecurringMeetings);
    }

    /**
     *
     * @param meeting
     * @param accountIdRemovedUser
     * @param accountIdAdmin
     * @param timeZone
     * method to send cancel meeting notification
     */
    private void sendCancelMeetingNotification(Meeting meeting, Long accountIdRemovedUser, Long accountIdAdmin, String timeZone, Integer entityTypeId, Long entityId) {
        try {
            List<HashMap<String, String>> meetingPayload = notificationService.cancelMeetingNotification(meeting, accountIdAdmin, timeZone);
            taskServiceImpl.sendPushNotification(meetingPayload);
        }catch (Exception e){
            logger.error("unable to create cancel meeting notification for removed user accountId: " + accountIdRemovedUser + " from " + (entityTypeId.equals(com.tse.core_application.model.Constants.EntityTypes.ORG) ? "org: " : "team: ") + entityId);
        }
    }

    public void removeUserAsAttendeeFromAllFutureMeetings(Integer entityTypeId, Long entityId, Long accountIdRemovedUser, Long accountIdAdmin, String timeZone) {
        List<Meeting> allFutureMeetings = new ArrayList<>();
        if (entityTypeId.equals(com.tse.core_application.model.Constants.EntityTypes.ORG)) {
            allFutureMeetings = meetingRepository.findByStartDateTimeGreaterThanAndIsCancelledFalseAndAttendeeList_AccountIdAndOrgId(LocalDateTime.now(), accountIdRemovedUser, entityId);
        } else if (entityTypeId.equals(com.tse.core_application.model.Constants.EntityTypes.TEAM)) {
            allFutureMeetings = meetingRepository.findByStartDateTimeGreaterThanAndIsCancelledFalseAndAttendeeList_AccountIdAndTeamId(LocalDateTime.now(), accountIdRemovedUser, entityId);
        }

        attendeeService.disInviteAttendeeOnAccountRemovalFromEntity(allFutureMeetings, accountIdRemovedUser, accountIdAdmin, timeZone);
    }

    public Boolean validateIsEditableForAttendee(Attendee attendee, String accountIds) {
        List<Long> accountIdsList = Arrays.stream(accountIds.split(",")).map(Long::valueOf).collect(Collectors.toList());

        if (accountIdsList.contains(attendee.getAccountId()) && attendee.getDidYouAttend() != null && attendee.getInitialEffortDateTime() != null && Objects.equals(attendee.getDidYouAttend(), com.tse.core_application.model.Constants.BooleanValues.BOOLEAN_TRUE)) {
            Integer effortEditTimeDuration = attendeeService.getEditTimeDurationForMeetings(attendee.getAccountId(), attendee.getTeamId());

            //Calculating time difference in added effort and edit effort duration
            Duration timeDifference = Duration.between(attendee.getInitialEffortDateTime(), LocalDateTime.now());
            if (timeDifference.toMinutes() <= effortEditTimeDuration.longValue()) {
                return true;
            }
        }
        List<Long> accountsWithEditAccess=taskServiceImpl.getAccountIdsOfRoleMembersWithEditEffortAccess(attendee.getTeamId());
        return CommonUtils.containsAny(accountsWithEditAccess,accountIdsList);
    }

    public void bulkValidateIsEditableForAttendee(List<MeetingResponse> meetingResponseList, List<Long> accountIdsList) {

        List<Long> teamIds = new ArrayList<>();
        List<Long> orgIds = new ArrayList<>();
        meetingResponseList.forEach(meetingResponse -> {
            if (meetingResponse.getTeamId() != null) {
                teamIds.add(meetingResponse.getTeamId());
            }
            if (meetingResponse.getOrgId() != null) {
                orgIds.add(meetingResponse.getOrgId());
            }
        });

        List<EntityPreference> teamPreferenceList = entityPreferenceRepository.findByEntityTypeIdAndEntityIdIn(com.tse.core_application.model.Constants.EntityTypes.TEAM, teamIds);
        List<EntityPreference> orgPreferenceList = entityPreferenceRepository.findByEntityTypeIdAndEntityIdIn(com.tse.core_application.model.Constants.EntityTypes.ORG, orgIds);
        Map<Long, EntityPreference> teamPreferenceMap = teamPreferenceList.stream().collect(Collectors.toMap(EntityPreference::getEntityId, entity -> entity));
        Map<Long, EntityPreference> orgPreferenceMap = orgPreferenceList.stream().collect(Collectors.toMap(EntityPreference::getEntityId, entity -> entity));

        List<Integer> roleIds = com.tse.core_application.model.Constants.ROLES_WITH_TEAM_EFFORT_EDIT_ACCESS;
        List<EntityPreferenceDto> higherRolesInTeamList = accessDomainRepository.findAccountIdsByEntityTypeIdAndEntityIdInAndRoleIdInAndIsActive(com.tse.core_application.model.Constants.EntityTypes.TEAM, teamIds, roleIds, true);
        Map<Long, List<Long>> teamHigherRoleAccountIdMap = higherRolesInTeamList.stream().collect(Collectors.groupingBy(EntityPreferenceDto::getEntityId, Collectors.mapping(EntityPreferenceDto::getAccountIdWithHigherRoles, Collectors.toList())));

        meetingResponseList.forEach(meetingResponse -> {
                    meetingResponse.setIsEditable(false);
                    meetingResponse.setCanEditMeeting(false);
                    List<Long> higherRoleAccountIdList = teamHigherRoleAccountIdMap.get(meetingResponse.getTeamId()) != null ? teamHigherRoleAccountIdMap.get(meetingResponse.getTeamId()) : new ArrayList<>();
                    Set<Long> higherRoleSet = new HashSet<>(higherRoleAccountIdList);

                    meetingResponse.getAttendeeRequestList().forEach(attendee -> {
                                if (accountIdsList.contains(attendee.getAccountId()) && attendee.getDidYouAttend() != null && attendee.getInitialEffortDateTime() != null && Objects.equals(attendee.getDidYouAttend(), com.tse.core_application.model.Constants.BooleanValues.BOOLEAN_TRUE)) {
                                    EntityPreference teamPreference = teamPreferenceMap.get(meetingResponse.getTeamId());
                                    EntityPreference orgPreference = orgPreferenceMap.get(meetingResponse.getOrgId());
                                    Integer effortEditTimeDuration = (teamPreference != null && teamPreference.getMeetingEffortEditDuration() != null) ?
                                            teamPreference.getMeetingEffortEditDuration() : ((orgPreference != null && orgPreference.getMeetingEffortEditDuration() != null) ?
                                            orgPreference.getMeetingEffortEditDuration() : defaultEffortEditTime);

                                    //Calculating time difference in added effort and edit effort duration
                                    Duration timeDifference = Duration.between(attendee.getInitialEffortDateTime(), LocalDateTime.now());
                                    if (timeDifference.toMinutes() <= effortEditTimeDuration.longValue()) {
                                        meetingResponse.setIsEditable(true);
                                    }
                                }
                            }
                    );
                    for (Long accountId : accountIdsList) {
                        if (higherRoleSet.contains(accountId)) {
                            meetingResponse.setIsEditable(true);
                            meetingResponse.setCanEditMeeting(true);
                            break;
                        }
                    }
                    if (accountIdsList.contains(meetingResponse.getOrganizerAccountId()) || accountIdsList.contains(meetingResponse.getCreatedAccountId())) {
                        meetingResponse.setCanEditMeeting(true);
                    }
                }
        );
    }

    /**
     * This method updates efforts of meeting in reference task and bill those efforts according to the meeting preference
     */
    public void updateTaskEffortForMeeting(TimeSheet timeSheet, Meeting meeting, AttendeeParticipationRequest attendeeParticipationRequest) {
        if (meeting.getReferenceEntityTypeId() != null && Objects.equals(meeting.getReferenceEntityTypeId(), com.tse.core_application.model.Constants.EntityTypes.TASK) && meeting.getReferenceEntityNumber() != null) {
            Long taskIdentifier = taskServiceImpl.getTaskIdentifierFromTaskNumber(meeting.getReferenceEntityNumber());
            Task referenceTaskDb = null;
            if (meeting.getTeamId() != null) {
                referenceTaskDb = taskRepository.findByTaskIdentifierAndFkTeamIdTeamId(taskIdentifier, meeting.getTeamId());
            } else if (meeting.getProjectId() != null) {
                referenceTaskDb = taskRepository.findByTaskIdentifierAndFkProjectIdProjectId(taskIdentifier, meeting.getProjectId());
            }
            Task referenceTask = new Task();
            BeanUtils.copyProperties(referenceTaskDb, referenceTask);
            Boolean changeInUserPerceived = Boolean.FALSE;
            if (referenceTask != null) {
                Task taskCopy = new Task();
                Task parentTaskCopy = new Task();
                Task parentTaskDb = null;
                Task parentTask = new Task();
                BeanUtils.copyProperties(referenceTask, taskCopy);
                Integer meetingPreferenceId = getMeetingPreferenceId(referenceTask);
                referenceTask.setMeetingEffortPreferenceId(meetingPreferenceId);
                Integer noOfAudit = 0;
                List<String> updatedFields = new ArrayList<>();
                List<String> updatedFieldsForParentTask = new ArrayList<>();
                Integer sum = attendeeParticipationRequest.getAttendeeDuration();
                int earnedTime = attendeeParticipationRequest.getAttendeeDuration() > meeting.getDuration() ? meeting.getDuration() : attendeeParticipationRequest.getAttendeeDuration();
                if (timeSheet.getNewEffort() != null) {
                    sum -= timeSheet.getNewEffort();
                    timeSheet.setNewEffort(sum + timeSheet.getNewEffort());
                } else {
                    timeSheet.setNewEffort(sum);
                    //Todo: We will need this when we implement the new meeting billed efforts (23-01-2025)
//                    changeInUserPerceived = Boolean.TRUE;
                }

                //recording efforts
                referenceTask.setTotalEffort((referenceTask.getTotalEffort() != null) ? (referenceTask.getTotalEffort() + sum) : sum);
                referenceTask.setTotalMeetingEffort((referenceTask.getTotalMeetingEffort() != null) ? (referenceTask.getTotalMeetingEffort() + sum) : sum);
                updatedFields.add(com.tse.core_application.model.Constants.TaskFields.TOTAL_MEETING_EFFORT);
                noOfAudit++;
                if (Objects.equals(referenceTask.getTaskTypeId(), com.tse.core_application.model.Constants.TaskTypes.CHILD_TASK) && referenceTask.getParentTaskId() != null) {
                    parentTaskDb = taskRepository.findByTaskId(referenceTask.getParentTaskId());
                    BeanUtils.copyProperties(parentTaskDb, parentTask);
                    BeanUtils.copyProperties(parentTask, parentTaskCopy);
                    parentTask.setTotalMeetingEffort(parentTask.getTotalMeetingEffort() != null ? parentTask.getTotalMeetingEffort() + sum : sum);
                    updatedFieldsForParentTask.add(com.tse.core_application.model.Constants.TaskFields.TOTAL_MEETING_EFFORT);
                    parentTask.setTotalEffort((parentTask.getTotalEffort() != null) ? (parentTask.getTotalEffort() + sum) : sum);
                }

                //making a list of workflow status for billing efforts in task
                ArrayList<String> workFlowStatusList = new ArrayList<>();
                workFlowStatusList.add(com.tse.core_application.model.Constants.WorkFlowTaskStatusConstants.STATUS_STARTED_TITLE_CASE);
                workFlowStatusList.add(com.tse.core_application.model.Constants.WorkFlowTaskStatusConstants.STATUS_BLOCKED_TITLE_CASE);
                workFlowStatusList.add(com.tse.core_application.model.Constants.WorkFlowTaskStatusConstants.STATUS_ON_HOLD_TITLE_CASE);

                //Billing meeting efforts in task
                if (!Objects.equals(meetingPreferenceId, com.tse.core_application.model.Constants.MeetingPreferenceEnum.NO_EFFORTS.getMeetingPreferenceId()) && workFlowStatusList.contains(referenceTask.getFkWorkflowTaskStatus().getWorkflowTaskStatus())) {
                    Boolean isBilled = Boolean.FALSE;
                    if (Objects.equals(meetingPreferenceId, com.tse.core_application.model.Constants.MeetingPreferenceEnum.ALL_MEETING_EFFORTS.getMeetingPreferenceId())) {
                        setReferenceEntitiesInTimesheet(meeting, timeSheet);
                        referenceTask.setBilledMeetingEffort((referenceTask.getBilledMeetingEffort() != null) ? (referenceTask.getBilledMeetingEffort() + earnedTime) : earnedTime);
                        noOfAudit++;
                        updatedFields.add(com.tse.core_application.model.Constants.TaskFields.BILLED_MEETING_EFFORT);
                        referenceTask.setRecordedEffort((referenceTask.getRecordedEffort() != null) ? (referenceTask.getRecordedEffort() + earnedTime) : earnedTime);
                        isBilled = Boolean.TRUE;
                    } else if (Objects.equals(meetingPreferenceId, com.tse.core_application.model.Constants.MeetingPreferenceEnum.ONLY_ASSIGNED_TO_EFFORTS.getMeetingPreferenceId())) {
                        if (Objects.equals(attendeeParticipationRequest.getAccountId(), referenceTask.getFkAccountIdAssigned().getAccountId())) {
                            setReferenceEntitiesInTimesheet(meeting, timeSheet);
                            referenceTask.setBilledMeetingEffort((referenceTask.getBilledMeetingEffort() != null) ? (referenceTask.getBilledMeetingEffort() + earnedTime) : earnedTime);
                            noOfAudit++;
                            updatedFields.add(com.tse.core_application.model.Constants.TaskFields.BILLED_MEETING_EFFORT);
                            referenceTask.setRecordedEffort((referenceTask.getRecordedEffort() != null) ? (referenceTask.getRecordedEffort() + earnedTime) : earnedTime);
                            isBilled = Boolean.TRUE;
                        }
                    } else if (Objects.equals(meetingPreferenceId, com.tse.core_application.model.Constants.MeetingPreferenceEnum.HYBRID_EFFORTS.getMeetingPreferenceId())) {
                        //List of stakeholders account ids
                        Set<Long> stakeHoldersAccountIdList = getTaskStakeHolders(referenceTask, true);
                        if (stakeHoldersAccountIdList.contains(attendeeParticipationRequest.getAccountId())) {
                            setReferenceEntitiesInTimesheet(meeting, timeSheet);
                            referenceTask.setBilledMeetingEffort((referenceTask.getBilledMeetingEffort() != null) ? (referenceTask.getBilledMeetingEffort() + earnedTime) : earnedTime);
                            noOfAudit++;
                            updatedFields.add(com.tse.core_application.model.Constants.TaskFields.BILLED_MEETING_EFFORT);
                            referenceTask.setRecordedEffort((referenceTask.getRecordedEffort() != null) ? (referenceTask.getRecordedEffort() + earnedTime) : earnedTime);
                            isBilled = Boolean.TRUE;
                        }
                    } else {
                        throw new IllegalStateException("Provided meeting preference does not exist " + meetingPreferenceId);
                    }

                    if (parentTaskDb != null && isBilled) {
                        parentTask.setBilledMeetingEffort((parentTask.getBilledMeetingEffort() != null) ? (parentTask.getBilledMeetingEffort() + earnedTime) : earnedTime);
                        parentTask.setRecordedEffort((parentTask.getRecordedEffort() != null) ? (parentTask.getRecordedEffort() + earnedTime) : earnedTime);
                        parentTask.setMeetingEffortPreferenceId(meetingPreferenceId);
                        updatedFieldsForParentTask.add(com.tse.core_application.model.Constants.TaskFields.BILLED_MEETING_EFFORT);
                    }

                    //Calculating earned efforts for user perceived percentage
                    if (Objects.equals(attendeeParticipationRequest.getAccountId(), referenceTask.getFkAccountIdAssigned().getAccountId())) {
                        if (changeInUserPerceived) {
                            Integer changeInUserPerceivedPercentage = 0;
                            if (attendeeParticipationRequest.getUserPerceivedPercentageTaskCompleted() != null) {
                                changeInUserPerceivedPercentage = (referenceTask.getUserPerceivedPercentageTaskCompleted() != null) ? attendeeParticipationRequest.getUserPerceivedPercentageTaskCompleted() - referenceTask.getUserPerceivedPercentageTaskCompleted() : attendeeParticipationRequest.getUserPerceivedPercentageTaskCompleted();
                            }
                            if (changeInUserPerceivedPercentage < 0) {
                                throw new IllegalStateException("User cannot decrease user perceived percentage of a task.");
                            } else if (changeInUserPerceivedPercentage > 0) {
                                referenceTask.setUserPerceivedPercentageTaskCompleted(attendeeParticipationRequest.getUserPerceivedPercentageTaskCompleted());
                                updatedFields.add(com.tse.core_application.model.Constants.TaskFields.USER_PERCEIVED_PERCENTAGE);
                                noOfAudit++;
                                referenceTask.setIncreaseInUserPerceivedPercentageTaskCompleted(changeInUserPerceivedPercentage);
                                taskServiceImpl.updateEarnedTimeAndUpdateTaskAndTimeSheet(referenceTask, Collections.singletonList(timeSheet), sum);
                            } else {
                                updatedFields.add(com.tse.core_application.model.Constants.TaskFields.USER_PERCEIVED_PERCENTAGE);
                                timeSheet.setIncreaseInUserPerceivedPercentageTaskCompleted(0);
                                timeSheet.setEarnedTime(changeInUserPerceivedPercentage);
                            }
                        }
                    }
                }
                if (referenceTask.getSprintId() != null) {
                    capacityService.updateEffortsInSprintCapacity(timeSheet, referenceTask.getSprintId());
                }
                taskRepository.save(referenceTask);
                if (parentTaskDb != null && parentTaskCopy != null) {
                    taskRepository.save(parentTask);
                    taskHistoryService.addTaskHistoryOnSystemUpdate(parentTaskCopy);
                    taskHistoryMetadataService.addTaskHistoryMetadata(updatedFieldsForParentTask, parentTask);
                }
                Audit auditCreated = auditService.createAudit(referenceTask, noOfAudit, referenceTask.getTaskId(), com.tse.core_application.model.Constants.TaskFields.RECORDED_EFFORT);
                auditRepository.save(auditCreated);
                taskHistoryService.addTaskHistoryOnUserUpdate(taskCopy);
                taskHistoryMetadataService.addTaskHistoryMetadata(updatedFields, referenceTask);

            } else {
                throw new TaskNotFoundException();
            }
        }
    }

    /**
     * This method returns meeting efforts preference id to bill meeting efforts
     */
    public Integer getMeetingPreferenceId(Task task) {
        Optional<EntityPreference> teamPreferenceDb = entityPreferenceRepository.findByEntityTypeIdAndEntityId(com.tse.core_application.model.Constants.EntityTypes.TEAM, task.getFkTeamId().getTeamId());
        if (teamPreferenceDb.isPresent() && teamPreferenceDb.get().getMeetingEffortPreferenceId() != null) {
            return teamPreferenceDb.get().getMeetingEffortPreferenceId();
        }
        Optional<EntityPreference> orgPreferenceDb = entityPreferenceRepository.findByEntityTypeIdAndEntityId(com.tse.core_application.model.Constants.EntityTypes.ORG, task.getFkOrgId().getOrgId());
        if (orgPreferenceDb.isPresent() && orgPreferenceDb.get().getMeetingEffortPreferenceId() != null) {
            return orgPreferenceDb.get().getMeetingEffortPreferenceId();
        }
        return com.tse.core_application.model.Constants.MeetingPreferenceEnum.NO_EFFORTS.getMeetingPreferenceId();
    }

    /**
     * This method returns list of task assigned to and mentors
     */
    public Set<Long> getTaskStakeHolders(Task task, Boolean isTaskReferenced) {
        Set<Long> stakeHoldersAccountIdList = new HashSet<>();
        if(task.getFkAccountIdAssigned() != null) {
            stakeHoldersAccountIdList.add(task.getFkAccountIdAssigned().getAccountId());
        }
        if (task.getFkAccountIdMentor1() != null) {
            stakeHoldersAccountIdList.add(task.getFkAccountIdMentor1().getAccountId());
        }
        if (task.getFkAccountIdMentor2() != null) {
            stakeHoldersAccountIdList.add(task.getFkAccountIdMentor2().getAccountId());
        }
        if (!isTaskReferenced && task.getFkAccountIdObserver1() != null) {
            stakeHoldersAccountIdList.add(task.getFkAccountIdObserver1().getAccountId());
        }
        if (!isTaskReferenced && task.getFkAccountIdObserver2() != null) {
            stakeHoldersAccountIdList.add(task.getFkAccountIdObserver2().getAccountId());
        }
        return stakeHoldersAccountIdList;
    }

    /**
     * This method returns a boolean by verifying the creator of meeting with reference task
     */
    private Boolean isMeetingCreatorValid(Task task, String accountIds) {
        List<Long> accountIdList = jwtRequestFilter.getAccountIdsFromHeader(accountIds);
        Set<Long> stakeHoldersList = getTaskStakeHolders(task, true);
        List<Integer> referenceTaskMeetingRoleId = getReferenceTaskMeetingRoleIdList(task);
        List<AccountId> roleAccountIdList = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(com.tse.core_application.model.Constants.EntityTypes.TEAM, task.getFkTeamId().getTeamId(), referenceTaskMeetingRoleId, true);

        for (AccountId accountId : roleAccountIdList) {
            stakeHoldersList.add(accountId.getAccountId());
        }

        if (CommonUtils.containsAny(accountIdList, Arrays.asList(stakeHoldersList.toArray()))) {
            return true;
        }

        return false;
    }

    /**
     * This method returns the list of role ids that are eligible to create meeting with reference task
     */
    private List<Integer> getReferenceTaskMeetingRoleIdList(Task task) {
        List<Integer> roleIdList= new ArrayList<>();
        if (task.getSprintId() == null) {
            roleIdList.add(RoleEnum.PROJECT_MANAGER_NON_SPRINT.getRoleId());
            roleIdList.add(RoleEnum.TEAM_MANAGER_NON_SPRINT.getRoleId());
        }
        roleIdList.add(RoleEnum.PROJECT_MANAGER_SPRINT.getRoleId());
        roleIdList.add(RoleEnum.TEAM_MANAGER_SPRINT.getRoleId());
        Optional<EntityPreference> teamPreference = entityPreferenceRepository.findByEntityTypeIdAndEntityId(com.tse.core_application.model.Constants.EntityTypes.TASK, task.getFkTeamId().getTeamId());
        Optional<EntityPreference> orgPreference = entityPreferenceRepository.findByEntityTypeIdAndEntityId(com.tse.core_application.model.Constants.EntityTypes.ORG, task.getFkOrgId().getOrgId());
        if (teamPreference.isPresent() && teamPreference.get().getReferenceTaskMeetingRoleIdList() != null) {
            roleIdList.addAll(teamPreference.get().getReferenceTaskMeetingRoleIdList());
        } else if (orgPreference.isPresent() && orgPreference.get().getReferenceTaskMeetingRoleIdList() != null) {
            roleIdList.addAll(orgPreference.get().getReferenceTaskMeetingRoleIdList());
        }

        return roleIdList;
    }

    /**
     * This method validates weather reference task is asssigned to attendee or not
     */
    public Boolean validateAttendeeIdAndAssignedToId(String taskNumber, Long teamId, String accountIds) {
        List<Long> accountIdList = jwtRequestFilter.getAccountIdsFromHeader(accountIds);
        Task task = taskServiceImpl.findTaskByTaskNumberAndTeamId(taskNumber, teamId);
        if (task != null && task.getFkAccountIdAssigned() != null && accountIdList.contains(task.getFkAccountIdAssigned().getAccountId())) {
            return true;
        }
        return false;
    }

    /** This method returns all the meeting ids from the attendee table using input accountIds and validTeamIds of the user */
    public List<Long> getAllMeetingIdsFromAttendeeTableByAccountIds(String accountIds){

        List<Long> accountIdsList = Arrays.stream(accountIds.split(",")).map(Long::valueOf).collect(Collectors.toList());

        List<Long> allMeetingIds = attendeeRepository.findAllMeetingIdsByAccountIds(accountIdsList, Constants.MeetingAttendeeInvitationStatus.ATTENDEE_INVITED_ID);

        return allMeetingIds;
    }

    public void validateMeetingWithRecordedEfforts (MeetingRequest meetingRequest, Meeting meetingDb, String accountIds) throws IllegalAccessException {
        List<TimeSheet> recordedEffortList = timeSheetRepository.findByEntityTypeIdAndEntityId(com.tse.core_application.model.Constants.EntityTypes.MEETING, meetingDb.getMeetingId());

        List<MeetingStats> meetingStats = Arrays.asList(MeetingStats.MEETING_COMPLETED, MeetingStats.MEETING_COMPLETED_WITH_OVER_RUN, MeetingStats.MEETING_ENDED, MeetingStats.MEETING_ENDED_WITH_OVER_RUN);
        if (recordedEffortList.isEmpty() && !meetingStats.contains(meetingDb.getMeetingProgress())) {
            return;
        }

        List<Long> accountIdList = CommonUtils.convertToLongList(accountIds);
        List<Integer> roleIdList = new ArrayList<>(List.of(RoleEnum.TEAM_MANAGER_SPRINT.getRoleId(), RoleEnum.PROJECT_MANAGER_SPRINT.getRoleId()));
        roleIdList.add(RoleEnum.PROJECT_MANAGER_NON_SPRINT.getRoleId());
        roleIdList.add(RoleEnum.TEAM_MANAGER_NON_SPRINT.getRoleId());
        List<AccountId> authorizedAccountIds = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(com.tse.core_application.model.Constants.EntityTypes.TEAM, meetingDb.getTeamId(), roleIdList, true);
        List<Long> authorizedAccountIdList = new ArrayList<>();
        if (authorizedAccountIds != null) {
            authorizedAccountIdList = authorizedAccountIds.stream().map(AccountId::getAccountId).collect(Collectors.toList());
        }

        // if the meeting is completed or the meeting has recorded effort then only project manager can edit the meeting duration
        if (!Objects.equals(meetingRequest.getDuration(), meetingDb.getDuration())) {
            if (!CommonUtils.containsAny(accountIdList, authorizedAccountIdList)) {
                throw new IllegalAccessException("Only the project manager or team manager has the authority to modify the meeting duration if the meeting is completed or meeting has any recorded efforts." +
                        " Please contact your manager for further assistance.");
            }
        }

        if (meetingRequest.getIsCancelled() != null && meetingRequest.getIsCancelled()) {
            if (!CommonUtils.containsAny(accountIdList, authorizedAccountIdList)) {
                throw new IllegalAccessException("Only the project manager has the authority to cancel meetings after efforts have been added. Please contact your manager for further assistance.");
            }
        }

        if (!meetingRequest.getStartDateTime().equals(meetingDb.getStartDateTime())) {
            throw new IllegalAccessException("Changes to meeting dates are not permitted after efforts have been recorded or the meeting is marked completed");
        }
    }

    private void splitMeetingEfforts(AttendeeParticipationRequest request, Meeting meeting, TimeSheet timeSheet, TimeSheet splitTimeSheet, String timeZone) {
        Duration duration = Duration.ofMinutes(request.getAttendeeDuration());
        Integer meetingDuration = (meeting.getDuration() != null ? meeting.getDuration() : 0);
        LocalDateTime meetingStartDate = DateTimeUtils.convertServerDateToUserTimezone(meeting.getStartDateTime(), timeZone);
        LocalDateTime attendeeMeetingEndDate = meetingStartDate.plus(duration);
        if (!(meetingStartDate.toLocalDate()).equals(attendeeMeetingEndDate.toLocalDate())) {
            if (splitTimeSheet.getNewEffort() == null) {
                CommonUtils.copyNonNullProperties(timeSheet, splitTimeSheet);
                splitTimeSheet.setTimeTrackingId(null);
            }
            Duration durationUntilMidnight = Duration.between(meetingStartDate, meetingStartDate.toLocalDate().atStartOfDay().plusDays(1));
            timeSheet.setNewEffortDate(meetingStartDate.toLocalDate());
            timeSheet.setNewEffort((int) durationUntilMidnight.toMinutes());
            timeSheet.setEarnedTime(timeSheet.getNewEffort());
            splitTimeSheet.setNewEffort((int) duration.minus(durationUntilMidnight).toMinutes());
            splitTimeSheet.setNewEffortDate(attendeeMeetingEndDate.toLocalDate());
            splitTimeSheet.setEarnedTime(splitTimeSheet.getNewEffort());
            Integer totalBurnEffort = (timeSheet.getNewEffort() != null ? timeSheet.getNewEffort() : 0) + (splitTimeSheet.getNewEffort() != null ? splitTimeSheet.getNewEffort() : 0);
            if (meetingDuration > 0 && totalBurnEffort > 0) {
                int earnedEffortForTimeSheet = (int) Math.floor(((timeSheet.getNewEffort() != null ? timeSheet.getNewEffort() : 0) * meetingDuration) / (double) totalBurnEffort);
                int earnedEffortForSplitTimeSheet  = meetingDuration - earnedEffortForTimeSheet;

                timeSheet.setEarnedTime(Math.min(earnedEffortForTimeSheet, (timeSheet.getNewEffort() != null ? timeSheet.getNewEffort() : 0)));
                splitTimeSheet.setEarnedTime(Math.max(earnedEffortForSplitTimeSheet, 0));
            } else {
                timeSheet.setEarnedTime(0);
                splitTimeSheet.setEarnedTime(0);
            }
        } else {
            timeSheet.setNewEffortDate(attendeeMeetingEndDate.toLocalDate());
            if (splitTimeSheet.getNewEffort() != null) {
                splitTimeSheet.setNewEffort(0);
            }
        }
    }

    public void cancelMeeting(Meeting meeting) {
        if (meeting.getReferenceEntityNumber() != null) {
            Task referenceTask = getReferenceTaskFromMeeting(meeting);
            List<TimeSheet> recordedEffortList = timeSheetRepository.findByEntityTypeIdAndEntityId(com.tse.core_application.model.Constants.EntityTypes.MEETING, meeting.getMeetingId());

            if (referenceTask != null && recordedEffortList.isEmpty() && meeting.getActualEndDateTime() == null) {
                EntityPreference orgPreference = entityPreferenceService.fetchEntityPreference(com.tse.core_application.model.Constants.EntityTypes.ORG, referenceTask.getFkTeamId().getFkOrgId().getOrgId());
                List<Long> accountIdsToUpdateForCapacity = capacityService.getAccountIdsPerMeetingPreference(meeting, referenceTask, referenceTask.getSprintId(), orgPreference.getMeetingEffortPreferenceId());
                Task referenceTaskCopy = new Task();
                BeanUtils.copyProperties(referenceTask, referenceTaskCopy);
                taskHistoryService.addTaskHistoryOnSystemUpdate(referenceTaskCopy);
                List<Long> meetingIds = referenceTask.getMeetingList() != null ? referenceTask.getMeetingList() : new ArrayList<>();
                meetingIds.remove(meeting.getMeetingId());
                referenceTask.setMeetingList(meetingIds);
                taskRepository.save(referenceTask);
                // ToDo: As of now, we have not implemented history for meetingList
                taskHistoryMetadataService.addTaskHistoryMetadata(List.of("meetingList"), referenceTask);

                // if the meeting is not completed or no effort is recorded and the meeting is cancelled, then we adjust the capacity
                if (referenceTask.getSprintId() != null) {
                    capacityService.updateReferenceMeetingCapacityOnAddTaskToSprint(referenceTask.getSprintId(), -meeting.getDuration(), accountIdsToUpdateForCapacity);
                }
            } else {
                throw new ValidationFailedException("This Reference Meeting contains an already logged effort/s so won't be able cancel/delete.");
            }
        }

        // set the isCancelled as true in the meeting
        meeting.setIsCancelled(true);
    }

    public List<MeetingResponse> filterMeetingResponses(List<MeetingResponse> meetingResponseList, List<Long> attendeeIds, Long organizerAccountId, Long createdAccountId) {
        return meetingResponseList.stream()
                .filter(meeting -> {
                    boolean matches = true;

                    if (organizerAccountId != null) {
                        matches &= meeting.getOrganizerAccountId() != null && meeting.getOrganizerAccountId().equals(organizerAccountId);
                    }

                    if (createdAccountId != null) {
                        matches &= meeting.getCreatedAccountId() != null && meeting.getCreatedAccountId().equals(createdAccountId);
                    }

                    if (attendeeIds != null && !attendeeIds.isEmpty()) {
                        // If AttendeeRequestList is not empty and contains any of the given attendeeIds
                        if (meeting.getAttendeeRequestList() != null && !meeting.getAttendeeRequestList().isEmpty()) {
                            matches &= meeting.getAttendeeRequestList().stream()
                                    .anyMatch(attendee -> attendeeIds.contains(attendee.getAccountId()));
                        } else {
                            matches = false; // If AttendeeRequestList is empty or null, exclude it from the results
                        }
                    }
                    return matches;
                })
                .collect(Collectors.toList());
    }

    public List<SearchMeetingResponse> searchMeeting(SearchMeetingRequest searchMeetingRequest, List<Long> accountIds, String timeZone) {
        List<SearchMeetingResponse> searchMeetingResponseList = new ArrayList<>();
        List<Long> allTeamIdsOfUser = accessDomainRepository.findDistinctEntityIdsByActiveAccountIds(com.tse.core_application.model.Constants.EntityTypes.TEAM, accountIds);

        List <Long> allProjectIdsOfUser  = accessDomainRepository.getProjectInfoByAccountIdsAndIsActiveTrue(accountIds).stream().map(Project::getProjectId).collect(Collectors.toList());

        List<Long> allOrgIdsOfUser = userAccountRepository.findAllOrgIdByAccountIdInAndIsActive(accountIds,true);

        String nativeQuery =
                "SELECT * FROM tse.meeting " +
                        "WHERE (" +
                        "   team_id IN (:allTeamIdsAsLong) " +
                        "   OR (team_id IS NULL AND project_id IN (:allProjectIdsAsLong)) " +
                        "   OR (team_id IS NULL AND project_id IS NULL AND org_id IN (:allOrgIdsAsLong))" +
                        ")";
        // Apply filters dynamically
        nativeQuery = applyFiltersToSearchQueryForMeeting(searchMeetingRequest, nativeQuery);

        // Apply ordering condition
        nativeQuery = applyOrderByConditionForMeeting(searchMeetingRequest, nativeQuery);

        // Create query
        Query query = entityManager.createNativeQuery(nativeQuery, Meeting.class);

        // Set parameters dynamically
        setParametersInSearchQueryForMeeting(searchMeetingRequest, query, allTeamIdsOfUser, allProjectIdsOfUser, allOrgIdsOfUser);

        @SuppressWarnings("unchecked")
        List<Meeting> meetingList = query.getResultList();

        HashMap<Long, Boolean> userHigherRoleInTeamMap = new HashMap<>();

        List<Long> teamMeetingIdsListFromAttendee = attendeeRepository.findAllMeetingIdsByAccountIdsAndTeamIds(accountIds, allTeamIdsOfUser, Constants.MeetingAttendeeInvitationStatus.ATTENDEE_INVITED_ID);
        List<Long> projectMeetingIdsListFromAttendee = attendeeRepository.findAllMeetingIdsByAccountIdsAndProjectIds(accountIds, allProjectIdsOfUser, Constants.MeetingAttendeeInvitationStatus.ATTENDEE_INVITED_ID);
        List<Long> orgMeetingIdsListFromAttendee = attendeeRepository.findAllMeetingIdsByAccountIdsAndTeamIdIsNullAndProjectIdIsNull(accountIds, Constants.MeetingAttendeeInvitationStatus.ATTENDEE_INVITED_ID);

        if (meetingList != null && !meetingList.isEmpty()) {
            for (Meeting meeting : meetingList) {
                if (meeting.getIsCancelled() != null && meeting.getIsCancelled()) continue;
                if (meeting.getTeamId() != null) {
                    SearchMeetingResponse searchMeetingResponse = new SearchMeetingResponse();
                    userHigherRoleInTeamMap.computeIfAbsent(
                            meeting.getTeamId(),
                            teamId -> isHigherRolePresent(accountIds, List.of(teamId))
                    );
                    if (userHigherRoleInTeamMap.get(meeting.getTeamId()) || teamMeetingIdsListFromAttendee.contains(meeting.getMeetingId())) {
                        createSearchMeetingResponse (meeting, searchMeetingResponse, timeZone);
                        searchMeetingResponseList.add(searchMeetingResponse);
                    }
                } else if (meeting.getProjectId() != null) {
                    SearchMeetingResponse searchMeetingResponse = new SearchMeetingResponse();
                    if (projectMeetingIdsListFromAttendee.contains(meeting.getMeetingId())) {
                        createSearchMeetingResponse(meeting, searchMeetingResponse, timeZone);
                        searchMeetingResponseList.add(searchMeetingResponse);
                    }
                } else if (meeting.getOrgId() != null) {
                    SearchMeetingResponse searchMeetingResponse = new SearchMeetingResponse();
                    if (orgMeetingIdsListFromAttendee.contains(meeting.getMeetingId())) {
                        createSearchMeetingResponse(meeting, searchMeetingResponse, timeZone);
                        searchMeetingResponseList.add(searchMeetingResponse);
                    }

                }
            }
        }

        return searchMeetingResponseList;
    }

    private String applyFiltersToSearchQueryForMeeting(SearchMeetingRequest request, String nativeQuery) {
        if (request.getSearchTerm() != null && !request.getSearchTerm().isEmpty()) {
            nativeQuery += " AND ( " +
                    "SIMILARITY(title, :searchTerm) > :similarityThreshold " +
                    "OR SIMILARITY(meeting_number, :searchTerm) > :similarityThreshold " +
                    ") ";
        }
        return nativeQuery;
    }

    private String applyOrderByConditionForMeeting(SearchMeetingRequest request, String nativeQuery) {
        if (request.getSearchTerm() != null && !request.getSearchTerm().isEmpty()) {
            nativeQuery += " ORDER BY " +
                    "CASE WHEN SIMILARITY(title, :searchTerm) > :similarityThreshold THEN " +
                    "SIMILARITY(title, :searchTerm) " +
                    "ELSE SIMILARITY(CAST(meeting_number AS TEXT), :searchTerm) END DESC, " +
                    "CAST(REGEXP_REPLACE(meeting_number, '\\D', '', 'g') AS BIGINT) DESC"; // Extract numeric part
        } else {
            nativeQuery += " ORDER BY CAST(REGEXP_REPLACE(meeting_number, '\\D', '', 'g') AS BIGINT) DESC"; // Extract numeric part
        }

        return nativeQuery;
    }


    private void setParametersInSearchQueryForMeeting(SearchMeetingRequest request, Query query, List<Long> allTeamIdsOfUser, List<Long> allProjectIdsOfUser, List<Long> allOrgIdsOfUser) {
        allTeamIdsOfUser = allTeamIdsOfUser.isEmpty() ? List.of(-1L) : allTeamIdsOfUser;
        allProjectIdsOfUser = allProjectIdsOfUser.isEmpty() ? List.of(-1L) : allProjectIdsOfUser;
        allOrgIdsOfUser = allOrgIdsOfUser.isEmpty() ? List.of(-1L) : allOrgIdsOfUser;
        query.setParameter("allTeamIdsAsLong", allTeamIdsOfUser);
        query.setParameter("allProjectIdsAsLong", allProjectIdsOfUser);
        query.setParameter("allOrgIdsAsLong", allOrgIdsOfUser);
        if (request.getSearchTerm() != null && !request.getSearchTerm().isEmpty()) {
            query.setParameter("searchTerm", request.getSearchTerm());
            query.setParameter("similarityThreshold", similarityThreshold);
        }

    }

    public void createSearchMeetingResponse (Meeting meetingDb, SearchMeetingResponse searchMeetingResponse, String timeZone) {
        BeanUtils.copyProperties(meetingDb, searchMeetingResponse);
        if (meetingDb.getAttendeeId() != null && meetingDb.getAttendeeList() != null) {
            searchMeetingResponse.setAttendeAccountIdList(attendeeService.removeDeletedAttendees(meetingDb.getAttendeeList()).stream().map(Attendee :: getAccountId).collect(Collectors.toList()));
        }else{
            searchMeetingResponse.setAttendeAccountIdList(Collections.emptyList());
        }
        searchMeetingResponse.setMeetingType(setMeetingType(meetingDb.getMeetingTypeIndicator()));
        searchMeetingResponse.setEntityName(teamRepository.findTeamNameByTeamId(meetingDb.getTeamId()));
        if(meetingDb.getRecurringMeeting() != null) {
            searchMeetingResponse.setRecurringMeetingId(meetingDb.getRecurringMeeting().getRecurringMeetingId());
            searchMeetingResponse.setRecurEvery(meetingDb.getRecurringMeeting().getRecurEvery());
            searchMeetingResponse.setRecurDays(meetingDb.getRecurringMeeting().getRecurDays());
        }
        if(meetingDb.getMeetingLabels() != null && !meetingDb.getMeetingLabels().isEmpty()) {
            searchMeetingResponse.setLabels(meetingDb.getMeetingLabels().stream().map(Label :: getLabelName).collect(Collectors.toList()));
        }
        searchMeetingResponse.setStartDateTime(DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(meetingDb.getStartDateTime(), timeZone));
        searchMeetingResponse.setEndDateTime(DateTimeUtils.convertServerDateToUserTimezoneWithSeconds(meetingDb.getEndDateTime(), timeZone));
    }

    public Boolean meetingEditAccess (Long organizerAccountId, Long creatorAccountId, Long teamId, String accountIds) {
        List<Long> accountIdsList = Arrays.stream(accountIds.split(",")).map(Long::valueOf).collect(Collectors.toList());
        List<Long> accountsWithEditAccess = taskServiceImpl.getAccountIdsOfRoleMembersWithEditEffortAccess(teamId);
        return accountIdsList.contains(organizerAccountId) || accountIdsList.contains(creatorAccountId) || CommonUtils.containsAny(accountsWithEditAccess, accountIdsList);
    }

    public LocalDateTime getNthWeekSaturday(LocalDate localStartDate, Integer recurEvery) {
        LocalDate targetDate = localStartDate.plusWeeks(recurEvery);

        while (targetDate.getDayOfWeek() != DayOfWeek.SATURDAY) {
            targetDate = targetDate.plusDays(1);
        }

        return LocalDateTime.of(targetDate, LocalTime.of(23, 59, 50));
    }

    public MeetingResponse getMeetingByEntityAndMeetingNumber(SearchMeetingV2Request request, String accountIds , String desiredTimeZone) throws IllegalAccessException{

        MeetingResponse meetingToGet;
        String entityTypeId = request.getEntityTypeId().toString();
        String fieldName;
        switch (entityTypeId) {
            case "2":
                fieldName = "orgId";
                break;
            case "3":
                fieldName = "buId";
                break;
            case "4":
                fieldName = "projId";
                break;
            case "5":
                fieldName = "teamId";
                break;
            default:
                throw new ValidationFailedException("Entity Type id is not valid!!! Please check again.");
        }

        Meeting foundMeetingDb = meetingRepository.findMeetingByEntityAndMeetingNumber(fieldName, request.getEntityId(), request.getMeetingNumber());
        if(foundMeetingDb != null) {
            List<Long> accounIdList = CommonUtils.convertToLongList(accountIds);
            if (foundMeetingDb.getTeamId() != null) {
                // team id validation to get meeting
                List<Long> validTeamIds = getAllValidTeamIdsByInputFilters(null,null,accountIds);
                HashSet<Long> validTeamIdsSet = new HashSet<>(validTeamIds);

                if(!validTeamIdsSet.contains(foundMeetingDb.getTeamId())){
                    throw new ValidationFailedException("You are not authorized to access the meeting");
                }
            } else if (foundMeetingDb.getProjectId() != null) {
                List<Long> validProjectIds = accessDomainRepository.getProjectInfoByAccountIdsAndIsActiveTrue(accounIdList).stream().map(Project::getProjectId).collect(Collectors.toList());
                if (!validProjectIds.contains(foundMeetingDb.getProjectId())) {
                    throw new ValidationFailedException("You are not authorized to access the meeting");
                }
            } else {
                if (!userAccountRepository.existsByAccountIdInAndOrgIdAndIsActive(accounIdList, foundMeetingDb.getOrgId(), true)) {
                    throw new ValidationFailedException("You are not authorized to access the meeting");
                }
            }
            meetingToGet = createMeetingResponseFromMeeting(foundMeetingDb, desiredTimeZone);
            for (Attendee attendee : meetingToGet.getAttendeeRequestList()) {
                if (validateIsEditableForAttendee(attendee, accountIds)) {
                    meetingToGet.setIsEditable(Boolean.TRUE);
                }
            }
            if (!meetingEditAccess (foundMeetingDb.getOrganizerAccountId(), foundMeetingDb.getCreatedAccountId(), foundMeetingDb.getTeamId(), accountIds)) {
                meetingToGet.setCanEditMeeting(false);
            }
            return meetingToGet;
        }
        else{
            return null;
        }
    }

    public Boolean fetchAttendeesAndSendNotification(TaskIdAssignedTo taskIdAssignedTo, String timeZone, String accountIds) {

        List<Long> requesterAccountIds = CommonUtils.convertToLongList(accountIds);
        Task task = taskRepository.findByTaskId(taskIdAssignedTo.getTaskId());

        if (task.getMeetingNotificationSentTime() != null &&
                !task.getMeetingNotificationSentTime().plusHours(com.tse.core_application.model.Constants.ReferenceMeetingDialogBox.RESEND_NOTIFICATION_COOLDOWN_TIME).isBefore(LocalDateTime.now())) {
            throw new ValidationFailedException("Notification has already been sent to all the attendees.");
        }
        List<AccessDomain> accessDomains = accessDomainRepository.findByEntityTypeIdAndEntityIdAndAccountIdInAndIsActive(com.tse.core_application.model.Constants.EntityTypes.TEAM, task.getFkTeamId().getTeamId(), requesterAccountIds, true);
        if(!requesterAccountIds.contains(task.getFkAccountIdAssigned().getAccountId()) &&
                accessDomains.stream().noneMatch(accessDomain -> com.tse.core_application.model.Constants.ROLES_WITH_TEAM_ATTENDANCE_ACCESS.contains(accessDomain.getRoleId()))) {
            throw new ValidationFailedException("User does not have permission to mark this Work Item as Completed or to send notification.");
        }
        List<Meeting> meetingList = meetingRepository.findActiveReferenceMeetingByReferenceEntityTypeIdAndReferenceEntityNumberAndTeamId(com.tse.core_application.model.Constants.EntityTypes.TASK, task.getTaskNumber(), task.getFkTeamId().getTeamId());

        HashSet<Long> userAccountIds = new HashSet<>();
        if (meetingList != null && !meetingList.isEmpty()) {
            for (Meeting meeting : meetingList) {
                List<Attendee> attendeeList = new ArrayList<>();
                if (meeting.getAttendeeId() != null && meeting.getAttendeeList() != null) {
                    attendeeList = attendeeService.removeDeletedAttendees(meeting.getAttendeeList());
                }
                if (attendeeList != null && !attendeeList.isEmpty()) {
                    attendeeList.forEach(attendee -> {
                        if(attendee.getDidYouAttend()==null || attendee.getDidYouAttend().equals(com.tse.core_application.model.Constants.BooleanValues.BOOLEAN_FALSE) || attendee.getAttendeeDuration()==null || attendee.getAttendeeDuration().equals(0)) {
                            userAccountIds.add(attendee.getAccountId());
                        }
                    });
                }
            }
        }
        if(!userAccountIds.isEmpty()) {
            task.setMeetingNotificationSentTime(LocalDateTime.now());
            notificationService.submitMeetingEffortNotification(task, userAccountIds, timeZone, accountIds, com.tse.core_application.model.Constants.ReferenceMeetingDialogBox.RESEND_NOTIFICATION_COOLDOWN_TIME);
            taskRepository.save(task);
            return true;
        }
        return false;
    }

    public List<ReferenceMeetingResponse> getReferenceMeetingByTask(Long taskId, String accountIds) {
        Task taskDb = taskRepository.findByTaskId(taskId);
        List<Long> requesterAccountIds = CommonUtils.convertToLongList(accountIds);
        List<AccessDomain> accessDomains = accessDomainRepository.findByEntityTypeIdAndEntityIdAndAccountIdInAndIsActive(com.tse.core_application.model.Constants.EntityTypes.TEAM, taskDb.getFkTeamId().getTeamId(), requesterAccountIds, true);
        if(!requesterAccountIds.contains(taskDb.getFkAccountIdAssigned().getAccountId()) &&
                accessDomains.stream().noneMatch(accessDomain -> com.tse.core_application.model.Constants.ROLES_WITH_TEAM_ATTENDANCE_ACCESS.contains(accessDomain.getRoleId()))) {
            throw new ValidationFailedException("User does not have permission to mark this Work Item as Completed.");
        }
        if(taskDb!=null){
            List<Long> meetingIdList = taskDb.getMeetingList();
            List<Meeting> meetingList = meetingRepository.findByMeetingIdsIn(meetingIdList);
            return meetingList.stream().filter(meeting -> !meeting.getIsCancelled())
                    .map(meeting -> ReferenceMeetingResponse.builder()
                            .meetingId(meeting.getMeetingId())
                            .meetingNumber(meeting.getMeetingNumber())
                            .title(meeting.getTitle())
                            .attendeeRequestList(meeting.getAttendeeList().stream().map( attendee -> AttendeeParticipationRequest.builder()
                                    .meetingId(attendee.getMeeting().getAttendeeId())
                                    .accountId(attendee.getAccountId())
                                    .attendeeDuration(attendee.getAttendeeDuration())
                                    .didYouAttend(attendee.getDidYouAttend())
                                    .isAttendeeExpected(attendee.getIsAttendeeExpected())
                                    .build()).collect(Collectors.toList()))
                            .build())
                    .collect(Collectors.toList());
        }
        else {
            throw new ValidationFailedException("TaskId provided is not valid. Please try again!!");
        }
    }

    public String updateFetchedButtonInMeeting(MeetingAnalysisFetchButtonRequest meetingAnalysisFetchButtonRequest, String environmentKey) {
        if (!Objects.equals(environmentKey, environmentApiKey)) {
            throw new ValidationFailedException("Environment key is not valid");
        }
        Meeting meeting = meetingRepository.findByMeetingId(meetingAnalysisFetchButtonRequest.getMeetingId());
        if (meeting == null) {
            throw new ValidationFailedException("Meeting doesn't exist");
        }
        MeetingAnalysisUploadedFile meetingAnalysisUploadedFile = meetingAnalysisUploadedFileRepository.findByMeetingIdAndModelId(meetingAnalysisFetchButtonRequest.getMeetingId(), meetingAnalysisFetchButtonRequest.getModelId());
        if (meetingAnalysisUploadedFile == null) {
            throw new ValidationFailedException("There is no such meeting transcription is in under process");
        }
        if (!meetingAnalysisUploadedFile.getUnderProcessing()) {
            throw new ValidationFailedException("This model is not in process");
        }
        List<ModelFetchedDto> modelFetchedDtoList = meeting.getModelFetchedList();
        if (modelFetchedDtoList == null) {
            modelFetchedDtoList = new ArrayList<>();
            meeting.setModelFetchedList(modelFetchedDtoList);
        }

        ModelFetchedDto existingDto = modelFetchedDtoList.stream()
                .filter(dto -> dto.getModelId().equals(meetingAnalysisFetchButtonRequest.getModelId()))
                .findFirst()
                .orElse(null);

        if (existingDto != null) {
            if (meetingAnalysisFetchButtonRequest.getIsProcessed()) {
                existingDto.setIsFetched(true);
                existingDto.setIsProblem(false);
            } else {
                existingDto.setIsFetched(false);
                existingDto.setIsProblem(true);
            }
        } else {
            ModelFetchedDto modelFetchedDto = new ModelFetchedDto();
            modelFetchedDto.setModelId(meetingAnalysisFetchButtonRequest.getModelId());
            if (meetingAnalysisFetchButtonRequest.getIsProcessed()) {
                modelFetchedDto.setIsFetched(true);
                modelFetchedDto.setIsProblem(false);
            } else {
                modelFetchedDto.setIsFetched(false);
                modelFetchedDto.setIsProblem(true);
            }
            modelFetchedDtoList.add(modelFetchedDto);
        }
        meeting.setViewTranscription(true);

        meetingRepository.save(meeting);
        meetingAnalysisUploadedFile.setUnderProcessing(false);
        meetingAnalysisUploadedFileRepository.save(meetingAnalysisUploadedFile);

        try {
            sendNotificationOfMeetingAnalysis (meeting, meetingAnalysisFetchButtonRequest);
        }
        catch (Exception ignored) {
        }

        return "Model fetch status updated successfully";

    }

    public void sendNotificationOfMeetingAnalysis (Meeting meeting, MeetingAnalysisFetchButtonRequest meetingAnalysisFetchButtonRequest) {
        HashSet<Long> accountIdToSendNotification = new HashSet<>();
        if (meeting.getOrganizerAccountId() != null) {
            accountIdToSendNotification.add(meeting.getOrganizerAccountId());
        }
        List<Long> accountIdListOfAttendee = meeting.getAttendeeList()
                .stream()
                .map(Attendee::getAccountId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<Long> higherRoleAccountIdList = null;
        if (meeting.getTeamId() != null && accountIdListOfAttendee != null && !accountIdListOfAttendee.isEmpty()) {
            higherRoleAccountIdList = accessDomainRepository.findDistinctAccountIdsByEntityTypeIdAndEntityIdInAndRoleIdInAndAccountIdInAndIsActive(com.tse.core_application.model.Constants.EntityTypes.TEAM, List.of(meeting.getTeamId()), com.tse.core_application.model.Constants.ROLE_IDS_FOR_MEETING_ANALYSIS, accountIdListOfAttendee, true);
        }

        if (higherRoleAccountIdList != null && !higherRoleAccountIdList.isEmpty()) {
            accountIdToSendNotification.addAll(new HashSet<>(higherRoleAccountIdList));
        }
        String timeZone = com.tse.core_application.model.Constants.DEFAULT_TIME_ZONE;
        if (meeting.getOrganizerAccountId() != null) {
            UserAccount userAccount = userAccountRepository.findByAccountId(meeting.getOrganizerAccountId());
            if (userAccount != null) {
                timeZone = userAccount.getFkUserId().getTimeZone();
            }
        }
        String notificationTitle = null;
        String notificationBody = null;
        if (meetingAnalysisFetchButtonRequest.getIsProcessed()) {
            notificationTitle = "Meeting analysis completed";
            notificationBody = "Meeting analysis for " + com.tse.core_application.model.Constants.Model.getTypeById(meetingAnalysisFetchButtonRequest.getModelId()) + " get completed";
        }
        else {
            notificationTitle = "Error occurred during meeting analysis";
            notificationBody = "Error occurred during meeting analysis for " + com.tse.core_application.model.Constants.Model.getTypeById(meetingAnalysisFetchButtonRequest.getModelId());
        }
        List<HashMap<String, String>> payload = notificationService.sendNotificationForMeetingAnalysis(meeting,notificationTitle, notificationBody, accountIdToSendNotification, meeting.getOrganizerAccountId(), timeZone);
        taskServiceImpl.sendPushNotification(payload);
    }

    @Transactional
    public UploadFileForModelResponse uploadFileMetadata(UploadFileForModelRequest uploadFileForModelRequest, String accountId, String timeZone) {
        Long accountIdOfUploader = null;
        try {
            accountIdOfUploader = Long.parseLong(accountId);
        } catch (NumberFormatException e) {
            throw new ValidationFailedException("Invalid accountId format!");
        }
        Meeting meeting = meetingRepository.findByMeetingId(uploadFileForModelRequest.getMeetingId());
        if (meeting == null) {
            throw new ValidationFailedException("Meeting doesn't exist");
        }
        if (!com.tse.core_application.model.Constants.MODEL_ID_LIST.contains(uploadFileForModelRequest.getModelId())) {
            throw new ValidationFailedException("Model is not valid");
        }
        LocalDateTime uploadServerDateTime = DateTimeUtils.convertUserDateToServerTimezone(uploadFileForModelRequest.getUploadedDateTime(), timeZone);
        if (uploadServerDateTime.isAfter(LocalDateTime.now())) {
            throw new ValidationFailedException("Uploaded date time can't be before current time");
        }
        validateUserAccessForMeetingAnalysis (accountIdOfUploader, uploadFileForModelRequest.getOrgId(), meeting);
        List<ModelFetchedDto> modelFetchedDtoList = meeting.getModelFetchedList();
        if (modelFetchedDtoList == null) {
            modelFetchedDtoList = new ArrayList<>();
            meeting.setModelFetchedList(modelFetchedDtoList);
        }

        ModelFetchedDto existingFileForModel1 = modelFetchedDtoList.stream()
                .filter(dto -> dto.getModelId().equals(com.tse.core_application.model.Constants.Model.MODEL_1.getTypeId()))
                .findFirst()
                .orElse(null);
        ModelFetchedDto existingFileForModel2 = modelFetchedDtoList.stream()
                .filter(dto -> dto.getModelId().equals(com.tse.core_application.model.Constants.Model.MODEL_2.getTypeId()))
                .findFirst()
                .orElse(null);
        ModelFetchedDto existingFileForModel3 = modelFetchedDtoList.stream()
                .filter(dto -> dto.getModelId().equals(com.tse.core_application.model.Constants.Model.MODEL_3.getTypeId()))
                .findFirst()
                .orElse(null);
        validationForProcessingAlreadyExist (uploadFileForModelRequest, existingFileForModel1, existingFileForModel2, existingFileForModel3);

        MeetingAnalysisUploadedFile existingMeetingAnalysisUploadedFile = meetingAnalysisUploadedFileRepository.findByMeetingIdAndModelId (uploadFileForModelRequest.getMeetingId(), uploadFileForModelRequest.getModelId());
        MeetingAnalysisUploadedFile savedMeetingAnalysisUploadedFile = new MeetingAnalysisUploadedFile();
        if (existingMeetingAnalysisUploadedFile == null) {
            MeetingAnalysisUploadedFile meetingAnalysisUploadedFile = new MeetingAnalysisUploadedFile();
            meetingAnalysisUploadedFile.setMeetingId(uploadFileForModelRequest.getMeetingId());
            meetingAnalysisUploadedFile.setModelId(uploadFileForModelRequest.getModelId());
            meetingAnalysisUploadedFile.setUploaderAccountId(accountIdOfUploader);
            meetingAnalysisUploadedFile.setUploadedDateTime(uploadServerDateTime);
            meetingAnalysisUploadedFile.setMeetingFileMetaDataList(uploadFileForModelRequest.getMeetingFileMetaDataList());
            savedMeetingAnalysisUploadedFile = meetingAnalysisUploadedFileRepository.save(meetingAnalysisUploadedFile);
        }
        else {
            existingMeetingAnalysisUploadedFile.setUploaderAccountId(accountIdOfUploader);
            existingMeetingAnalysisUploadedFile.setUploadedDateTime(uploadServerDateTime);
            existingMeetingAnalysisUploadedFile.setMeetingFileMetaDataList(uploadFileForModelRequest.getMeetingFileMetaDataList());
            existingMeetingAnalysisUploadedFile.setUnderProcessing(true);
            savedMeetingAnalysisUploadedFile = meetingAnalysisUploadedFileRepository.save(existingMeetingAnalysisUploadedFile);
        }
        meeting.setViewTranscription(false);
        Meeting savedMeeting = meetingRepository.save(meeting);
        UploadFileForModelResponse uploadFileForModelResponse = new UploadFileForModelResponse();
        uploadFileForModelResponse.setMeetingId(savedMeetingAnalysisUploadedFile.getMeetingId());
        uploadFileForModelResponse.setModelId(savedMeetingAnalysisUploadedFile.getModelId());
        uploadFileForModelResponse.setUploadedDateTime(uploadFileForModelRequest.getUploadedDateTime());
        EmailFirstLastAccountIdIsActive emailFirstLastAccountIdIsActive = userAccountRepository.getEmailFirstNameLastNameAccountIdIsActiveByAccountId(savedMeetingAnalysisUploadedFile.getUploaderAccountId());
        uploadFileForModelResponse.setUploaderUserAccountDetails(emailFirstLastAccountIdIsActive);
        uploadFileForModelResponse.setMeetingFileMetaDataList(savedMeetingAnalysisUploadedFile.getMeetingFileMetaDataList());
        uploadFileForModelResponse.setModelFetchedDtoList(savedMeeting.getModelFetchedList());
        uploadFileForModelResponse.setUnderProcessing(savedMeetingAnalysisUploadedFile.getUnderProcessing());
        return uploadFileForModelResponse;
    }

    private void validationForProcessingAlreadyExist(UploadFileForModelRequest uploadFileForModelRequest, ModelFetchedDto existingFileForModel1, ModelFetchedDto existingFileForModel2, ModelFetchedDto existingFileForModel3) {
        if (Objects.equals(com.tse.core_application.model.Constants.Model.MODEL_1.getTypeId(), uploadFileForModelRequest.getModelId()) || Objects.equals(com.tse.core_application.model.Constants.Model.MODEL_2.getTypeId(), uploadFileForModelRequest.getModelId())) {
            if (meetingAnalysisUploadedFileRepository.existsByMeetingIdAndModelIdInAndUnderProcessing(uploadFileForModelRequest.getMeetingId(), List.of(uploadFileForModelRequest.getModelId(), com.tse.core_application.model.Constants.Model.MODEL_3.getTypeId()), true)) {
                throw new ValidationFailedException("Meeting analysis is already is in under process for " + com.tse.core_application.model.Constants.Model.getTypeById(uploadFileForModelRequest.getModelId()) + " or " + com.tse.core_application.model.Constants.Model.MODEL_3.getType());
            }
            if (existingFileForModel3 != null) {
                existingFileForModel3.setIsFetched(false);
                existingFileForModel3.setIsProblem(false);
            }
            if (Objects.equals(com.tse.core_application.model.Constants.Model.MODEL_1.getTypeId(), uploadFileForModelRequest.getModelId())) {
                if (existingFileForModel1 != null) {
                    existingFileForModel1.setIsFetched(false);
                    existingFileForModel1.setIsProblem(false);
                }
            }
            else {
                if (existingFileForModel2 != null) {
                    existingFileForModel2.setIsFetched(false);
                    existingFileForModel2.setIsProblem(false);
                }
            }
        }
        else {
            if (meetingAnalysisUploadedFileRepository.existsByMeetingIdAndModelIdInAndUnderProcessing(uploadFileForModelRequest.getMeetingId(), List.of(com.tse.core_application.model.Constants.Model.MODEL_1.getTypeId(), com.tse.core_application.model.Constants.Model.MODEL_2.getTypeId(), com.tse.core_application.model.Constants.Model.MODEL_3.getTypeId()), true)) {
                throw new ValidationFailedException("Meeting analysis is already is in under process for " + com.tse.core_application.model.Constants.Model.MODEL_1.getType() + " or " + com.tse.core_application.model.Constants.Model.MODEL_2.getType() + " or " + com.tse.core_application.model.Constants.Model.MODEL_3.getType());
            }
            if (existingFileForModel1 != null) {
                existingFileForModel1.setIsFetched(false);
                existingFileForModel1.setIsProblem(false);
            }
            if (existingFileForModel2 != null) {
                existingFileForModel2.setIsFetched(false);
                existingFileForModel2.setIsProblem(false);
            }
        }
    }

    private void validateUserAccessForMeetingAnalysis(Long accountIdOfUploader, Long orgId, Meeting meeting) {
        if (!userAccountRepository.existsByAccountIdInAndOrgIdAndIsActive(List.of(accountIdOfUploader), orgId, true)) {
            throw new ValidationFailedException("User doesn't exist");
        }
        if (Objects.equals(accountIdOfUploader, meeting.getOrganizerAccountId())) {
            return;
        }
        List<Attendee> attendeeList = meeting.getAttendeeList();
        List<Integer> rolesForMeetingAnalysis = com.tse.core_application.model.Constants.ROLE_IDS_FOR_MEETING_ANALYSIS;
        if (meeting.getTeamId() != null) {
            if (attendeeList == null || attendeeList.stream().noneMatch(a -> a.getAccountId().equals(accountIdOfUploader)) || !accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndIsActiveAndRoleIdIn(com.tse.core_application.model.Constants.EntityTypes.TEAM, meeting.getTeamId(), List.of(accountIdOfUploader), true, rolesForMeetingAnalysis)) {
                throw new ValidationFailedException("You do not have access to upload Meeting analysis");
            }
        }

    }

    @Transactional
    public MeetingAnalysisDetailsResponse uploadMeetingAnalysis(MeetingAnalysisDetailsRequest meetingAnalysisDetailsRequest, String accountId, String timeZone) {
        MeetingAnalysisDetailsResponse meetingAnalysisDetailsResponse = new MeetingAnalysisDetailsResponse();

        Long accountIdOfUploader = null;
        try {
            accountIdOfUploader = Long.parseLong(accountId);
        } catch (NumberFormatException e) {
            throw new ValidationFailedException("Invalid accountId format!");
        }
        Meeting meetingDb = meetingRepository.findByMeetingId(meetingAnalysisDetailsRequest.getMeetingId());
        if (meetingDb == null) {
            throw new ValidationFailedException("Meeting doesn't exist");
        }
        Meeting meeting = new Meeting();
        BeanUtils.copyProperties(meetingDb, meeting);
        if (meetingAnalysisDetailsRequest.getModelId() != null && !com.tse.core_application.model.Constants.MODEL_ID_LIST.contains(meetingAnalysisDetailsRequest.getModelId())) {
            throw new ValidationFailedException("Model is not valid");
        }

        validateUserAccessForMeetingAnalysis (accountIdOfUploader, meeting.getOrgId(), meeting);

        meeting.setMinutesOfMeeting(meetingAnalysisDetailsRequest.getMinutesOfMeeting());
        actionItemService.updateActionItems (meetingAnalysisDetailsRequest.getActionItemList(), meeting);
        meetingNoteService.addOrUpdateMeetingNotes(meetingAnalysisDetailsRequest.getMeetingNoteList(), meeting, accountIdOfUploader);
        if (meetingAnalysisDetailsRequest.getModelId() != null && meeting.getModelFetchedList() != null) {
            ModelFetchedDto existingFileForModel = meeting.getModelFetchedList().stream()
                    .filter(dto -> dto.getModelId().equals(meetingAnalysisDetailsRequest.getModelId()))
                    .findFirst()
                    .orElse(null);
            if (existingFileForModel != null) {
                existingFileForModel.setIsFetched(false);
                existingFileForModel.setIsProblem(false);
            }
        }

        meetingRepository.save(meeting);
        List<ActionItem> actionItemList = actionItemRepository.findByFkMeetingIdMeetingIdAndIsDeleted(meeting.getMeetingId(), false);
        List<MeetingNote> meetingNoteList = meetingNoteRepository.findByFkMeetingIdMeetingIdAndIsDeleted(meeting.getMeetingId(), false);

        meetingAnalysisDetailsResponse.setMeetingId(meeting.getMeetingId());
        meetingAnalysisDetailsResponse.setModelId(meetingAnalysisDetailsRequest.getModelId());
        if (actionItemList != null && !actionItemList.isEmpty()) {
            List<ActionItemResponseDto> actionItemListResponse = getActionItemsForResponse (actionItemList, timeZone);
            meetingAnalysisDetailsResponse.setActionItemList(actionItemListResponse);
        }
        if (meetingNoteList != null && !meetingNoteList.isEmpty()) {
            List<MeetingNoteResponse> meetingNotesListResponse = getMeetingNotesForResponse (meetingNoteList, timeZone);
            meetingAnalysisDetailsResponse.setMeetingNoteList(meetingNotesListResponse);
        }
        meetingAnalysisDetailsResponse.setModelId(meetingAnalysisDetailsRequest.getModelId());
        meetingAnalysisDetailsResponse.setMinutesOfMeeting(meetingAnalysisDetailsRequest.getMinutesOfMeeting());
        return meetingAnalysisDetailsResponse;
    }

    private List<MeetingNoteResponse> getMeetingNotesForResponse(List<MeetingNote> meetingNoteList, String timeZone) {
        List<MeetingNoteResponse> meetingNoteResponseList = new ArrayList<>();
        for (MeetingNote meetingNote : meetingNoteList) {
            MeetingNoteResponse meetingNoteResponse = new MeetingNoteResponse();
            BeanUtils.copyProperties(meetingNote, meetingNoteResponse);
            if (meetingNoteResponse.getCreatedDateTime() != null) {
                meetingNoteResponse.setCreatedDateTime(DateTimeUtils.convertServerDateToUserTimezone(meetingNoteResponse.getCreatedDateTime(), timeZone));
            }
            if (meetingNoteResponse.getUpdatedDateTime() != null) {
                meetingNoteResponse.setUpdatedDateTime(DateTimeUtils.convertServerDateToUserTimezone(meetingNoteResponse.getUpdatedDateTime(), timeZone));
            }
            if (meetingNote.getPostedByAccountId() != null) {
                meetingNoteResponse.setPostedByAccountIdDetails(userAccountRepository.getEmailFirstNameLastNameAccountIdIsActiveByAccountId(meetingNote.getPostedByAccountId()));
            }
            if (meetingNote.getModifiedByAccountId() != null) {
                meetingNoteResponse.setModifiedByAccountIdDetails(userAccountRepository.getEmailFirstNameLastNameAccountIdIsActiveByAccountId(meetingNote.getModifiedByAccountId()));
            }
            meetingNoteResponseList.add(meetingNoteResponse);
        }
        return meetingNoteResponseList;
    }

    private List<ActionItemResponseDto> getActionItemsForResponse(List<ActionItem> actionItemList, String timeZone) {
        List<ActionItemResponseDto> actionItemListResponse = new ArrayList<>();
        Map<Long, ActionItemResponseDto> actionItemWithTaskId = new HashMap<>();
        for (ActionItem actionItemDb : actionItemList) {
            ActionItemResponseDto actionItem = new ActionItemResponseDto();
            BeanUtils.copyProperties(actionItemDb, actionItem);
            if (actionItem.getCreatedDateTime() != null) {
                actionItem.setCreatedDateTime(DateTimeUtils.convertServerDateToUserTimezone(actionItem.getCreatedDateTime(), timeZone));
            }
            if (actionItem.getLastUpdatedDateTime() != null) {
                actionItem.setLastUpdatedDateTime(DateTimeUtils.convertServerDateToUserTimezone(actionItem.getLastUpdatedDateTime(), timeZone));
            }
            if(actionItemDb.getTaskId() != null) {
                actionItemWithTaskId.put(actionItem.getTaskId(), actionItem);
            } else {
                actionItem.setTaskDetails(null);
            }
            actionItemListResponse.add(actionItem);
        }
        if(!actionItemWithTaskId.isEmpty()) {
            List<WorkItemProgressDetailsDto> workItems = taskRepository.findWorkItemProgressByTaskIdIn(new ArrayList<>(actionItemWithTaskId.keySet()));
            for (WorkItemProgressDetailsDto workItem : workItems) {
                actionItemWithTaskId.get(workItem.getTaskId()).setTaskDetails(workItem);
            }
        }
        return actionItemListResponse;
    }
}
