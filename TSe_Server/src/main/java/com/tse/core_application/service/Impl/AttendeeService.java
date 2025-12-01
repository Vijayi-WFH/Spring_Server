package com.tse.core_application.service.Impl;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.custom.model.AttendeeId;
import com.tse.core_application.dto.*;
import com.tse.core_application.dto.jitsi.JitsiParticipantDTO;
import com.tse.core_application.exception.MeetingNotFoundException;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.model.*;
import com.tse.core_application.model.User;
import com.tse.core_application.repository.*;
import com.tse.core_application.utils.CommonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Optionals;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.naming.TimeLimitExceededException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AttendeeService {

    private static final Logger logger = LogManager.getLogger(AttendeeService.class.getName());

    @Autowired
    private AttendeeRepository attendeeRepository;
    @Autowired
    private MeetingRepository meetingRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private AccessDomainService accessDomainService;
    @Autowired
    private EntityPreferenceService entityPreferenceService;
    @Autowired
    private ProjectService projectService;

    @Autowired
    private MeetingService meetingService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private AccessDomainRepository accessDomainRepository;

    @Autowired
    TimeSheetRepository timeSheetRepository;

    @Value("${default.effort.edit.time.meeting}")
    private Integer defaultEffortEditTime;


    @Autowired
    private TaskServiceImpl taskServiceImpl;

    /** This method validates that no 2 attendees should have same account Ids when added in a meeting */
    public void validateAllAttendees(List<AttendeeRequest> attendees, Integer entityTypeId, Long entityId){

        HashSet<Long> accountIds = new HashSet<>();
        for(AttendeeRequest attendee : attendees){
            Boolean isActive;
            if (Objects.equals(com.tse.core_application.model.Constants.EntityTypes.TEAM, entityTypeId)) {
                isActive = accessDomainService.IsActiveAccessDomain(attendee.getAccountId(), entityId);
            } else if (Objects.equals(com.tse.core_application.model.Constants.EntityTypes.PROJECT, entityTypeId)) {
                isActive = projectService.existInProject(attendee.getAccountId(), entityId);
            } else {
                isActive = userAccountRepository.existsByAccountIdAndOrgIdAndIsActive(attendee.getAccountId(), entityId, true);
            }
            if (!isActive) {
                throw new ValidationFailedException("Attendee does not belong to the entity provided");

            }
            if(accountIds.contains(attendee.getAccountId())){
                throw new ValidationFailedException("The attendee with Account ID : "+attendee.getAccountId()+" cannot be added twice in same meeting");
            }
            else{
                accountIds.add(attendee.getAccountId());
            }
        }

    }

    /** This method adds all attendees in create meeting request to attendee table and also validates that no 2 attendees with same account id should get added */
    public List<Attendee> addAttendee(List<AttendeeRequest> attendeesList, Meeting savedMeeting) {
        List<Attendee> attendeeToSave = new ArrayList<>();
        boolean isOrganiserAdded = false;
        boolean isOrganiserAttendeeExist = true;
        if(savedMeeting.getOrganizerAccountId() != null) {
            isOrganiserAttendeeExist = attendeeRepository.existsByAccountIdAndMeetingMeetingId(savedMeeting.getOrganizerAccountId(), savedMeeting.getMeetingId());
        }

        Long foundAttendeeId = getNextAttendeeId();

        if (attendeesList != null && (!attendeesList.isEmpty())) {
            if (savedMeeting.getTeamId() != null) {
                validateAllAttendees(attendeesList, com.tse.core_application.model.Constants.EntityTypes.TEAM, savedMeeting.getTeamId());
            } else if (savedMeeting.getProjectId() != null) {
                validateAllAttendees(attendeesList, com.tse.core_application.model.Constants.EntityTypes.PROJECT, savedMeeting.getProjectId());
            } else {
                validateAllAttendees(attendeesList, com.tse.core_application.model.Constants.EntityTypes.ORG, savedMeeting.getOrgId());
            }

            for (AttendeeRequest x : attendeesList) {
                Attendee attendee = new Attendee();
                attendee.setAttendeeId(foundAttendeeId);
                attendee.setAccountId(x.getAccountId());
                attendee.setMeeting(savedMeeting);
                // set bu id , project id, and team id for an attendee for time sheet - task 2676
                attendee.setBuId(x.getBuId() != null ? x.getBuId() : savedMeeting.getBuId());
                attendee.setProjectId(x.getProjectId() != null ? x.getProjectId() : savedMeeting.getProjectId());
                attendee.setTeamId(x.getTeamId() != null ? x.getTeamId() : savedMeeting.getTeamId());
                attendee.setSystemGenEfforts(x.getSystemGenEfforts());
                // Add 3 statements below in Task 2539
                attendee.setAttendeeInvitationStatus(Constants.MeetingAttendeeInvitationStatus.ATTENDEE_INVITED);
                attendee.setAttendeeInvitationStatusId(getAttendeeInviteStatusId(Constants.MeetingAttendeeInvitationStatus.ATTENDEE_INVITED));
                attendeeToSave.add(attendee);

                if(Objects.equals(x.getAccountId(), savedMeeting.getOrganizerAccountId())) isOrganiserAdded = true;

            }

        }
        if (!isOrganiserAdded && !isOrganiserAttendeeExist) {
            Attendee attendee = new Attendee();
            attendee.setAttendeeId(foundAttendeeId);
            attendee.setAccountId(savedMeeting.getOrganizerAccountId());
            attendee.setMeeting(savedMeeting);
            attendee.setBuId(savedMeeting.getBuId());
            attendee.setProjectId(savedMeeting.getProjectId());
            attendee.setTeamId(savedMeeting.getTeamId());
            attendee.setAttendeeInvitationStatus(Constants.MeetingAttendeeInvitationStatus.ATTENDEE_INVITED);
            attendee.setAttendeeInvitationStatusId(Constants.MeetingAttendeeInvitationStatus.ATTENDEE_INVITED_ID);
            attendeeToSave.add(attendee);
        }
        savedMeeting.setAttendeeId(foundAttendeeId);

        return new ArrayList<>(attendeeRepository.saveAll(attendeeToSave));

    }

    /** This method updates attendees in attendee table : add new attendees and delete the attendees to be deleted and also ensures that no 2 attendees with same account id gets added */
    public List<Attendee> updateAllAttendees(List<AttendeeRequest> attendeesList, Meeting savedMeeting, List<Long> deletedAccountIds)
    {
        List<Attendee> attendeesUpdated = new ArrayList<>();
        List<AttendeeRequest> attendeesToDelete = new ArrayList<>();
        List<AttendeeRequest> attendeesToAdd = new ArrayList<>();
        boolean isOrganiserAdded = false;

        if(attendeesList != null && !attendeesList.isEmpty()){
            Long foundAttendeeId = savedMeeting.getAttendeeId() != null ? savedMeeting.getAttendeeId() : getNextAttendeeId();
            for(AttendeeRequest attendee : attendeesList){

                if(Objects.equals(attendee.getAccountId(), savedMeeting.getOrganizerAccountId())) isOrganiserAdded = true;
//                if(deletedAttendeesAccountIds.contains(attendee.getAccountId())){
//                    attendeesToDelete.add(attendee);
//                }
//                else
                    if(isNewAttendee(attendee)){
                    attendeesToAdd.add(attendee);
                   }
                    else{
                        Attendee unchanged = new Attendee();
                        unchanged.setAccountId(attendee.getAccountId());
                        attendeesUpdated.add(unchanged);
                    }

            }

            if(!isOrganiserAdded){
                AttendeeRequest attendee = new AttendeeRequest();
                attendee.setAccountId(savedMeeting.getOrganizerAccountId());
                attendeesToAdd.add(attendee);
            }

            attendeesUpdated.addAll(addNewAttendeesOnUpdateMeeting(attendeesToAdd,foundAttendeeId,savedMeeting));

//            if (!attendeesToDelete.isEmpty())
//                deleteAllAttendees(attendeesToDelete);


        }

        if(!deletedAccountIds.isEmpty()) {
            /* now if any higher role removed the attendees (who already put their efforts into the meeting), their efforts been removed from the timesheet to zero (timesheet table) and from attendee table
            making their response to null (attendeeDuration, didYouAttend, initialEffortTime) so if anyhow that member is added again it will override the existing entry instead of creating a nwe one. */
            attendeeRepository.updateAttendeeInvitationStatusIdAndAttendeeInvitationStatus(Constants.MeetingAttendeeInvitationStatus.ATTENDEE_DISINVITED_ID, Constants.MeetingAttendeeInvitationStatus.ATTENDEE_DISINVITED, savedMeeting.getMeetingId(), deletedAccountIds);
            timeSheetRepository.updateTimesheetEffortToZeroForMeetingOnRemoving(com.tse.core_application.model.Constants.EntityTypes.MEETING, savedMeeting.getMeetingId(), deletedAccountIds);
        }


        return attendeesUpdated;

    }

    /** This method is used to get the next attendee id according to the current max attendee id in attendee table*/
    public Long getNextAttendeeId() {
        AttendeeId attendeeId = attendeeRepository.getMaxAttendeeId();
        Long nextAttendeeId = attendeeId.getAttendeeId();
        if (nextAttendeeId == null) {
            Long longAttendeeId = 1L;
            return longAttendeeId;
        } else {
            return nextAttendeeId + 1;
        }
    }

    public Integer getAttendeeInviteStatusId(String attendeeInvitationStatus){
        if(Objects.equals(attendeeInvitationStatus, Constants.MeetingAttendeeInvitationStatus.ATTENDEE_INVITED)){
            return Constants.MeetingAttendeeInvitationStatus.ATTENDEE_INVITED_ID;
        }
        else if(Objects.equals(attendeeInvitationStatus, Constants.MeetingAttendeeInvitationStatus.ATTENDEE_DISINVITED)){
            return Constants.MeetingAttendeeInvitationStatus.ATTENDEE_DISINVITED_ID;
        }
        else{
            throw new ValidationFailedException("Invalid attendee status");
        }
    }

//    public boolean isAttendeeDeleted(AttendeeRequest attendee){
//        boolean isDeleted = false;
//        if(Objects.equals(getAttendeeInviteStatusId(attendee.getAttendeeInvitationStatus()), 0)){
//            isDeleted = true;
//        }
//        return isDeleted;
//    }

    public boolean isNewAttendee(AttendeeRequest attendee) {
        boolean isNew = false;
        if (attendee.getAttendeeLogId() == null){
            isNew = true;
        }
        return isNew;
    }

    /** This method adds new attendees in request when update API is called for a meeting and returns list of saved attendees */
    public List<Attendee> addNewAttendeesOnUpdateMeeting(List<AttendeeRequest> attendees, Long attendeeId, Meeting meetingAdded) {
        List<Attendee> attendeesToAdd = new ArrayList<>();
        List<Attendee> attendeesReInvited = new ArrayList<>();
        if (meetingAdded.getTeamId() != null) {
            validateAllAttendees(attendees, com.tse.core_application.model.Constants.EntityTypes.TEAM, meetingAdded.getTeamId());
        } else if (meetingAdded.getProjectId() != null) {
            validateAllAttendees(attendees, com.tse.core_application.model.Constants.EntityTypes.PROJECT, meetingAdded.getProjectId());
        } else {
            validateAllAttendees(attendees, com.tse.core_application.model.Constants.EntityTypes.ORG, meetingAdded.getOrgId());
        }

        // here two cases are checked - whether the attendee is a new attendee or old attendee earlier disinvited and now re invited.
        for (AttendeeRequest a : attendees) {
            if(!userAccountRepository.existsByAccountIdAndOrgIdAndIsActive(a.getAccountId(), meetingAdded.getOrgId(), true)){
                throw new ValidationFailedException("Account Id : "+a.getAccountId() +" does not exists in Org with org Id: "+meetingAdded.getOrgId());
            }

                Attendee attendeeDb = attendeeRepository.findByAccountIdAndMeetingMeetingId(a.getAccountId(), meetingAdded.getMeetingId());
            if (attendeeDb != null) {
//                if (Objects.equals(attendeeDb.getAttendeeInvitationStatusId(), Constants.MeetingAttendeeInvitationStatus.ATTENDEE_INVITED_ID)) {
//                    throw new ValidationFailedException("The attendee with Account ID : " + a.getAccountId() + " is already added in meeting with meetingId : " + meetingAdded.getMeetingId());
//                } else {
                    attendeeRepository.setAttendeeInvitationStatusIdAndAttendeeInvitationStatusByAttendeeLogId(Constants.MeetingAttendeeInvitationStatus.ATTENDEE_INVITED_ID, Constants.MeetingAttendeeInvitationStatus.ATTENDEE_INVITED, attendeeDb.getAttendeeLogId());
                    attendeeDb.setAttendeeInvitationStatus(Constants.MeetingAttendeeInvitationStatus.ATTENDEE_INVITED);
                    attendeeDb.setAttendeeInvitationStatusId(Constants.MeetingAttendeeInvitationStatus.ATTENDEE_INVITED_ID);
                    attendeesReInvited.add(attendeeDb);
//                }
            } else {
                Attendee addAttendee = new Attendee();
                addAttendee.setAttendeeId(attendeeId);
//            Integer attendeeInviteStatId = getAttendeeInviteStatusId(a.getAttendeeInvitationStatus());
//            if(Objects.equals(attendeeInviteStatId, Constants.MeetingAttendeeInvitationStatus.ATTENDEE_DISINVITED_ID) || Objects.equals(attendeeInviteStatId, Constants.MeetingAttendeeInvitationStatus.ATTENDEE_INVITED_ID)) {
//                addAttendee.setAttendeeInvitationStatus(a.getAttendeeInvitationStatus());
//                addAttendee.setAttendeeInvitationStatusId(attendeeInviteStatId);
//            }
//            else {
//                throw new ValidationFailedException("Invalid attendee status");
//            }
                addAttendee.setAttendeeInvitationStatusId(Constants.MeetingAttendeeInvitationStatus.ATTENDEE_INVITED_ID);
                addAttendee.setAttendeeInvitationStatus(Constants.MeetingAttendeeInvitationStatus.ATTENDEE_INVITED);
                addAttendee.setAccountId(a.getAccountId());

                addAttendee.setBuId(a.getBuId() != null ? a.getBuId() : meetingAdded.getBuId());
                addAttendee.setProjectId(a.getProjectId() != null ? a.getProjectId() : meetingAdded.getProjectId());
                addAttendee.setTeamId(a.getTeamId() != null ? a.getTeamId() : meetingAdded.getTeamId());

                addAttendee.setMeeting(meetingAdded);
                attendeesToAdd.add(addAttendee);
            }
        }
        List<Attendee> attendeesAdded =  attendeeRepository.saveAll(attendeesToAdd);
        attendeesAdded.addAll(attendeesReInvited);
        return attendeesAdded;
    }

//    public List<Long> getAttendeeIdByAccountId(Long accountId) {
//        List<Long> attendeeIdList = new ArrayList<>();
////        List<Attendee> attendeeId = attendeeRepository.findByAccountId(accountId);
//        if (attendeeId != null && !attendeeId.isEmpty()) {
//            for (Attendee a : attendeeId) {
//                attendeeIdList.add(a.getAttendeeId());
//            }
//        }
//        return attendeeIdList;
//    }

    /** This method soft delete all the attendees to be deleted as specified in the request , by changing the invitation status of the attendee */
    public Integer deleteAllAttendees(List<AttendeeRequest> attendeesToDelete) {
        List<Long> attendeeLogIds = new ArrayList<>();
        for (AttendeeRequest a  : attendeesToDelete) {
             attendeeLogIds.add(a.getAttendeeLogId());
        }
        return attendeeRepository.setAttendeeInvitationStatusIdByAttendeeLogIdIn(attendeeLogIds,0);
    }

   /** This method is used to remove the deleted attendees from attendee list while using get apis to get the meeting */
    public List<Attendee> removeDeletedAttendees(List<Attendee> attendees) {
        Iterator<Attendee> itr = attendees.iterator();
        while (itr.hasNext()) {
             Attendee attendee = itr.next();
            if (Objects.equals(attendee.getAttendeeInvitationStatusId(), 0)) {
                itr.remove();
            }
        }
        return attendees;
    }

    public Optional<Attendee> getAttendeeByAccountIdAndMeetingId(AttendeeParticipationRequest attendee){
        return attendeeRepository.findByAccountIdAndMeetingId(attendee.getAccountId(), attendee.getMeetingId());
    }

    /** This method updates the attendee table with request parameters when updateAttendeeResponse api is called */
    @Transactional
    public String updateAttendeeResponse(AttendeeParticipationRequest attendeeParticipationRequest, User foundUser, String timeZone) throws TimeLimitExceededException, IllegalAccessException {
        validateAttendeeDuration(attendeeParticipationRequest);
        Optional<Attendee> optionalAttendee = getAttendeeByAccountIdAndMeetingId(attendeeParticipationRequest);
        if (optionalAttendee.isPresent()) {
            if (attendeeParticipationRequest.getAttendeeDuration() != null) {
                Meeting meeting = meetingRepository.findByMeetingId(attendeeParticipationRequest.getMeetingId());
                Task referenceTask = meetingService.getReferenceTaskFromMeeting(meeting);
                if (referenceTask != null && referenceTask.getTaskActStDate() == null) {
                    throw new ValidationFailedException("Please start the reference work item " + meeting.getReferenceEntityNumber() + " to fill the time in meeting");
                }
            }
            Attendee attendeeDb = optionalAttendee.get();
            Integer attendeeDurationDb = attendeeDb.getAttendeeDuration();
            validateAttendeeResponse(optionalAttendee, foundUser.getUserId());

            Attendee attendee = optionalAttendee.get();
            attendee.setIsAttendeeExpected(attendeeParticipationRequest.getIsAttendeeExpected());
            attendee.setDidYouAttend(attendeeParticipationRequest.getDidYouAttend());
            attendee.setAttendeeDuration(attendeeParticipationRequest.getAttendeeDuration());
            if (attendee.getInitialEffortDateTime() == null) {
                attendee.setInitialEffortDateTime(LocalDateTime.now());
            }
            attendeeRepository.save(attendee);

            if (attendeeParticipationRequest.getAttendeeDuration() != null && !attendeeParticipationRequest.getAttendeeDuration().equals(attendeeDurationDb)) {
                meetingService.updateTimeSheetForAttendeeDurationInMeeting(attendeeParticipationRequest, attendeeDb, timeZone);
            }
            return "Response Updated Successfully";
        } else {
            throw new IllegalAccessException("Attendee Not Found");
        }
    }

    public Long updateAttendeeId(MeetingRequest meeting) {
        long updatedAttendeeId = 1L;
        AttendeeId attendeeId = attendeeRepository.getMaxAttendeeId();
        if (attendeeId != null) {
            updatedAttendeeId = attendeeId.getAttendeeId() + 1;
        }
        Attendee attendee = new Attendee();
        attendee.setAttendeeId(updatedAttendeeId);
        attendeeRepository.save(attendee);
        return updatedAttendeeId;
    }

    public void disInviteAttendeeOnAccountRemovalFromEntity(List<Meeting> meetings, Long accountIdRemovedUser, Long accountIdAdmin, String timeZone) {
        List<Long> meetingIds = meetings.stream().map(Meeting::getMeetingId).collect(Collectors.toList());
        attendeeRepository.setAttendeeInvitationStatusIdAndAttendeeInvitationStatusByMeetingIdAndAccountId(Constants.MeetingAttendeeInvitationStatus.ATTENDEE_DISINVITED_ID, Constants.MeetingAttendeeInvitationStatus.ATTENDEE_DISINVITED, accountIdRemovedUser, meetingIds);
        List<Long> recurringMeetingIds = new ArrayList<>();
        for (Meeting meeting : meetings) {
            if (meeting.getRecurringMeeting() != null) {
                if (!recurringMeetingIds.contains(meeting.getRecurringMeeting().getRecurringMeetingId())) {
                    recurringMeetingIds.add(meeting.getRecurringMeeting().getRecurringMeetingId());
                    notificationService.disInviteAttendeeFromMeetingNotification(meeting, accountIdRemovedUser, timeZone);
                    continue;
                }
            }
            meeting.setUpdatedAccountId(accountIdAdmin);
            notificationService.disInviteAttendeeFromMeetingNotification(meeting, accountIdRemovedUser, timeZone);
        }
    }

    /**
     * This methods validates if user is editing meeting efforts without exceeding time limit
     * @param optionalAttendee
     * @throws TimeLimitExceededException
     */
    public void validateAttendeeResponse(Optional<Attendee> optionalAttendee, Long userId) throws TimeLimitExceededException {
        if (optionalAttendee.isPresent()) {
            Attendee attendee = optionalAttendee.get();
            //checks if attendee attended the meeting
            if (attendee.getDidYouAttend() != null && Objects.equals(attendee.getDidYouAttend(), com.tse.core_application.model.Constants.BooleanValues.BOOLEAN_TRUE)) {
                Integer effortEditTimeDuration = getEditTimeDurationForMeetings(attendee.getAccountId(), attendee.getTeamId());

                //Calculating time difference in added effort and edit effort duration
                Duration timeDifference = Duration.between(attendee.getInitialEffortDateTime(), LocalDateTime.now());
                boolean hasEditAccess = taskServiceImpl.getUserIdsOfRoleMembersWithEditEffortAccess(attendee.getTeamId()).contains(userId);
                if (timeDifference.toMinutes() > effortEditTimeDuration.longValue()&&!hasEditAccess) {
                    throw new TimeLimitExceededException("Time limit exceeded : Cannot edit efforts after " + effortEditTimeDuration + " minutes");
                }
            }
        } else {
            throw new ValidationFailedException("Attendee not found");
        }
    }

    /**
     * Returns edit effort duration according to user preferences
     * @param accountId
     * @param teamId
     * @return
     */
    public Integer getEditTimeDurationForMeetings(Long accountId, Long teamId) {
        //getting edit duration according to team preference
        EntityPreference teamPreferenceDb = entityPreferenceService.getEntityPreference(com.tse.core_application.model.Constants.EntityTypes.TEAM, teamId);
        if (teamPreferenceDb != null && teamPreferenceDb.getMeetingEffortEditDuration() != null) {
            return teamPreferenceDb.getMeetingEffortEditDuration();
        }

        //getting edit duration according to org preference
        EntityPreference orgPreferenceDb = entityPreferenceService.getEntityPreference(com.tse.core_application.model.Constants.EntityTypes.ORG, userAccountRepository.findOrgIdByAccountIdAndIsActive(accountId, true).getOrgId());
        if (orgPreferenceDb != null && orgPreferenceDb.getMeetingEffortEditDuration() != null) {
            return orgPreferenceDb.getMeetingEffortEditDuration();
        }
        return defaultEffortEditTime;
    }

    public void validateAttendeeDuration(AttendeeParticipationRequest attendeeParticipationRequest) {
        if (attendeeParticipationRequest.getAttendeeDuration() != null && attendeeParticipationRequest.getAttendeeDuration() > 24*60) {
            throw new IllegalStateException("Attendee participation duration cannot exceed 24 hours. Please ensure your effort is within the provided time frame.");
        }
    }

    @Transactional
    public String bulkUpdateAttendeesResponse(List<AttendeeParticipationRequest> attendeeParticipationRequestList, Long taskId, User foundUser, String timeZone, Boolean isForReferenceTask) throws TimeLimitExceededException, IllegalAccessException {
        if (isForReferenceTask) {
            Task referenceTaskDb = taskRepository.findByTaskId(taskId);
            if (referenceTaskDb != null && referenceTaskDb.getTaskActStDate() == null) {
                throw new ValidationFailedException("Please start the reference work item " + referenceTaskDb.getTaskNumber() + " to fill the time in meeting");
            }
        }
        for (AttendeeParticipationRequest attendeeParticipationRequest : attendeeParticipationRequestList){
            if (attendeeParticipationRequest.getAttendeeDuration() != null && attendeeParticipationRequest.getAttendeeDuration() > 24*60) {
                throw new IllegalStateException("Attendee participation duration cannot exceed 24 hours. Please ensure your effort is within the provided time frame.");
            }
        }
        Optional<List<Attendee>> optionalAttendeeList = attendeeRepository.findByAccountIdInAndMeetingIdIn(attendeeParticipationRequestList.stream().map(AttendeeParticipationRequest::getAccountId).collect(Collectors.toList()),
                attendeeParticipationRequestList.stream().map(AttendeeParticipationRequest::getMeetingId).collect(Collectors.toList()));
        Map<String, AttendeeParticipationRequest> requestMap = attendeeParticipationRequestList.stream().
                collect(Collectors.toMap(attendeeParticipationRequest -> attendeeParticipationRequest.getAccountId() + ":" + attendeeParticipationRequest.getMeetingId(),
                        attendeeParticipationRequest -> attendeeParticipationRequest));

        Map<String, Integer> durationDbMap;
        if (optionalAttendeeList.isPresent()) {
            List<Attendee> attendeeList = new ArrayList<>();
            durationDbMap = optionalAttendeeList.get().stream().collect(Collectors.toMap(attendee -> attendee.getAccountId() + ":" + attendee.getMeeting().getMeetingId(),
                                                    attendee -> attendee.getAttendeeDuration() != null ? attendee.getAttendeeDuration() : 0));
            for(Attendee attendeeDb : optionalAttendeeList.get()) {
                AttendeeParticipationRequest attendeeParticipationRequest = requestMap.get(attendeeDb.getAccountId() + ":" + attendeeDb.getMeeting().getMeetingId());
                if(isForReferenceTask) {
                    validateAttendeeResponse(Optional.of(attendeeDb), foundUser.getUserId());
                }
                attendeeDb.setIsAttendeeExpected(attendeeParticipationRequest.getIsAttendeeExpected());
                attendeeDb.setDidYouAttend(attendeeParticipationRequest.getDidYouAttend());
                attendeeDb.setAttendeeDuration(attendeeParticipationRequest.getAttendeeDuration());
                if (attendeeDb.getInitialEffortDateTime() == null) {
                    attendeeDb.setInitialEffortDateTime(LocalDateTime.now());
                }
                attendeeList.add(attendeeDb);
            }
            attendeeRepository.saveAll(attendeeList);
            //ToDo: this needs to be compatiable with bulk timesheet update. (currently it takes only single request at once)
            for(Attendee attendeeDb : optionalAttendeeList.get()){
                AttendeeParticipationRequest attendeeParticipationRequest = requestMap.get(attendeeDb.getAccountId() + ":" + attendeeDb.getMeeting().getMeetingId());
                Integer attendeeDurationDb = durationDbMap.get(attendeeDb.getAccountId() + ":" + attendeeDb.getMeeting().getMeetingId());
                if (attendeeParticipationRequest.getAttendeeDuration() != null && !attendeeParticipationRequest.getAttendeeDuration().equals(attendeeDurationDb)) {
                    meetingService.updateTimeSheetForAttendeeDurationInMeeting(attendeeParticipationRequest, attendeeDb, timeZone);
                }
            }
            return "All attendees efforts are added for their respective meeting";
        } else {
            throw new IllegalAccessException("Attendee Not Found");
        }
    }

    public void updateSystemGenVijayiMeetEfforts(Long meetingId, Map<Long, Long> systemGenEffortsMap) {
        Meeting meetingDb = meetingRepository.findByMeetingId(meetingId);
        List<Long> existingAttendee = meetingDb.getAttendeeList().stream().map(Attendee::getAccountId).collect(Collectors.toList());
        //adding new participants into the meeting as well updating their efforts
        List<AttendeeRequest> attendeeRequestList = new ArrayList<>();
        for (Map.Entry<Long, Long> effort : systemGenEffortsMap.entrySet()) {
            if (existingAttendee.contains(effort.getKey())) {
                continue;
            }
            AttendeeRequest request = new AttendeeRequest();
            request.setSystemGenEfforts(effort.getValue());
            request.setAccountId(effort.getKey());
            request.setIsAttendeeExpected(1);
            attendeeRequestList.add(request);
        }
        addAttendee(attendeeRequestList, meetingDb);

        //updating the excluded participants efforts
        Optional<List<Attendee>> attendeeDbList = attendeeRepository.findByAccountIdInAndMeetingIdIn(existingAttendee, List.of(meetingId));
        if (attendeeDbList.isPresent()) {
            attendeeDbList.get().forEach(attendee -> attendee.setSystemGenEfforts(systemGenEffortsMap.get(attendee.getAccountId())));
            attendeeRepository.saveAll(attendeeDbList.get());
        }
    }

    @Transactional
    public void autoLoggedEffortsInTimesheetByVijayiMeet(Long meetingId, Map<Long, Long> systemGenEffortsMap, String timezone) {
        List<AttendeeParticipationRequest> attendeeParticipationRequests = new ArrayList<>();
        for (Map.Entry<Long, Long> effort : systemGenEffortsMap.entrySet()) {
            AttendeeParticipationRequest attendeeParticipationRequest = new AttendeeParticipationRequest();
            attendeeParticipationRequest.setMeetingId(meetingId);
            attendeeParticipationRequest.setAccountId(effort.getKey());
            attendeeParticipationRequest.setAttendeeDuration(effort.getValue().intValue());
            attendeeParticipationRequest.setDidYouAttend(1);
            attendeeParticipationRequests.add(attendeeParticipationRequest);
        }
        try {
            bulkUpdateAttendeesResponse(attendeeParticipationRequests, null, null, timezone, false);
        } catch (TimeLimitExceededException | IllegalAccessException e){
            logger.error("There is some exception occurred while automatic updating the efforts of the participants from the Vijayi-Meet: {}", e.getMessage());
        }
    }
}

