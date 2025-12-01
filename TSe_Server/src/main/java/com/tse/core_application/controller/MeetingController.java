package com.tse.core_application.controller;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.dto.*;
import com.tse.core_application.dto.meeting.ReferenceMeetingResponse;
import com.tse.core_application.dto.meeting.SearchMeetingRequest;
import com.tse.core_application.dto.meeting.SearchMeetingResponse;
import com.tse.core_application.dto.meeting.SearchMeetingV2Request;
import com.tse.core_application.exception.InternalServerErrorException;
import com.tse.core_application.exception.InvalidMeetingTypeException;
import com.tse.core_application.exception.MeetingNotFoundException;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.*;
import com.tse.core_application.model.User;
import com.tse.core_application.repository.AttendeeRepository;
import com.tse.core_application.repository.MeetingRepository;
import com.tse.core_application.service.Impl.*;
import com.tse.core_application.utils.DateTimeUtils;
import com.tse.core_application.utils.JWTUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.naming.TimeLimitExceededException;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@CrossOrigin(value = "*")
@RestController
@RequestMapping(path="/meeting")
public class MeetingController {
    private static final Logger logger = LogManager.getLogger(MeetingController.class.getName());
    @Autowired
    JWTUtil jwtUtil;
    @Autowired
    private UserService userService;
    @Autowired
    private RequestHeaderHandler requestHeaderHandler;
    @Autowired
    private MeetingService meetingService;
    @Autowired
    private AttendeeService attendeeService;
    @Autowired
    private MeetingRepository meetingRepository;
    @Autowired
    private TableColumnsTypeService tableColumnsTypeService;
    @Autowired
    private AccessDomainService accessDomainService;
    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private NotificationService notificationService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private EntityPreferenceService entityPreferenceService;
    @Autowired
    private AttendeeRepository attendeeRepository;

    @PostMapping(path="/createMeeting")
    @Transactional
    public ResponseEntity<Object> createMeeting(@Valid @RequestBody MeetingRequest meeting, @RequestHeader(name = "screenName") String screenName,
                                                @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                HttpServletRequest request) throws IllegalAccessException{
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " createMeeting" + '"' + " method ...");
        MeetingResponse meetingAdded=null;
        Boolean isValidAccount = false;
        try{
            if (!userAccountService.isUserAccountExistsByAccountIdAndOrgId(meeting.getOrganizerAccountId(), meeting.getOrgId())){
                throw new ValidationFailedException("Meeting organizer does not belongs to the organization");
            }
            if(meeting.getOrgId() != null && meeting.getTeamId() != null) {
                isValidAccount = accessDomainService.validateOrgAndTeamInRequest(meeting.getOrgId(), meeting.getTeamId(), accountIds);
                if (Objects.equals(meeting.getTeamId(), com.tse.core_application.model.Constants.PERSONAL_TEAM_ID) && Objects.equals(meeting.getOrgId(), com.tse.core_application.model.Constants.OrgIds.PERSONAL.longValue())) {
                    throw new IllegalStateException("User not allowed to create meeting for personal tasks");
                }
            } else if (meeting.getOrgId() != null && meeting.getProjectId() != null) {
                isValidAccount = projectService.validateOrgAndProjectWithAccountIds(meeting.getOrgId(), meeting.getProjectId(), accountIds);
            } else if (meeting.getOrgId() != null) {
                isValidAccount = organizationService.validateOrgUser(meeting.getOrgId(), accountIds);
            }

            if(!isValidAccount){
                throw new ValidationFailedException("Meeting cannot be created as the user account does not exists in this team");
            }

            // calculate endDateTime for meeting.
            if(meeting.getDuration() != null && meeting.getStartDateTime() != null){
                meeting.setEndDateTime(meetingService.calculateEndDateTimeForMeeting(meeting.getStartDateTime(), meeting.getDuration()));
            }
            meetingService.convertAllMeetingLocalDateAndTimeToServerTimeZone(meeting, timeZone);
            meetingService.validateMeetingAndUpdateReferenceTask(meeting, accountIds, null);

            RecurringMeeting savedRecurringMeeting = null;

            // In simple meeting we pass recurring meeting as null
            meetingAdded = meetingService.addMeeting(meeting, savedRecurringMeeting, timeZone, true);

        }
        catch(Exception e){
            if (e instanceof InvalidMeetingTypeException) {
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error("Meeting type validation failed: Invalid Meeting type provided " + " ,     " + meeting.getMeetingType() + " ,    "+ "meetingNumber = " + meetingAdded.getMeetingNumber() + " ,    " + "meetingId = " + meeting.getMeetingId() , new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw e;
            }
            else if(e instanceof  ValidationFailedException){
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error("Meeting validation failed: "+ e.getMessage() +" , "+ meeting.getMeetingType() + " ,    "+ "meetingKey = " + meeting.getMeetingKey()  + " ,    " + "meetingId = " + meeting.getMeetingId() , new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw e;
            }
           else if(e instanceof IllegalAccessException){
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error("One or more fields are not accessible in copying fields from one object to another or comparing 2 objects  ,  meetingType = " + meeting.getMeetingType() + " ,    "+ "meetingKey = " + meeting.getMeetingKey() + " ,    "+ "meetingNumber = " + meetingAdded.getMeetingNumber() + " ,    " + "meetingId = " + meeting.getMeetingId() , new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw  e;
            }
            else {
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute add Meeting for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
                ThreadContext.clearMap();
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
            }
        }

        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, meetingAdded);
    }

    // This API is not used , instead getExpandedView API is used
    @PostMapping(path="/getAllScheduledMeeting")
    public ResponseEntity<Object> getAllScheduledMeeting(@Valid @RequestBody GetScheduledMeetingRequest getScheduledMeetingRequest, @RequestHeader(name = "screenName") String screenName,
                                                         @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                         HttpServletRequest request) throws IllegalAccessException{
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getScheduledMeetingRequest" + '"' + " method ...");
        List<MeetingResponse> meetingList;

        try{
            meetingList= meetingService.getAllScheduledMeetings(getScheduledMeetingRequest, timeZone);
        }
        catch(Exception e){

            if(e instanceof IllegalAccessException){
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error("One or more fields are not accessible in copying fields from one object to another or comparing 2 objects " , new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw  e;
            }
            else {
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute get all scheduled meeting for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
                ThreadContext.clearMap();
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
            }

        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, meetingList);
    }

    @GetMapping(path = "/getMeeting/{meetingId}")
    public ResponseEntity<Object> getMeeting(@PathVariable(name = "meetingId") Long meetingId, @RequestHeader(name = "screenName") String screenName,
                                             @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                             HttpServletRequest request) throws IllegalAccessException{

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getMeeting" + '"' + " method ...");

        MeetingResponse foundMeetingDb = null;
        try {
            foundMeetingDb = meetingService.getMeetingByMeetingId(meetingId, accountIds, timeZone);


            if (foundMeetingDb != null) {
                    long estimatedTime = System.currentTimeMillis() - startTime;
                    ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                    logger.info("Exited" + '"' + " getMeeting" + '"' + " method because completed successfully ...");
                    ThreadContext.clearMap();

            }
             else {
                throw new MeetingNotFoundException();
            }
        } catch (Exception e) {
              if (e instanceof MeetingNotFoundException) {
                  e.printStackTrace();
                  String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                  logger.error("cannot find the meeting requested  , meetingId = "+  meetingId  , new Throwable(allStackTraces));
                  ThreadContext.clearMap();
                  throw e;
                }
              else if(e instanceof  ValidationFailedException){
                  e.printStackTrace();
                  String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                  logger.error("Meeting validation failed: "+ e.getMessage()   , new Throwable(allStackTraces));
                  ThreadContext.clearMap();
                  throw e;
              }
              else if(e instanceof IllegalAccessException){
                  e.printStackTrace();
                  String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                  logger.error("One or more fields are not accessible in copying fields from one object to another or comparing 2 objects  ,  meetingId = " +  meetingId  , new Throwable(allStackTraces));
                  ThreadContext.clearMap();
                  throw  e;
              }
              else {
                    e.printStackTrace();
                    String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                    logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get the meeting for username = " + foundUser.getPrimaryEmail() + " ,    " + "meetingId = " + meetingId + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
                    if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
                }
            }

        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, foundMeetingDb);

    }


   // This API is used to take Attendee response - is Attendee expected , did you attend and attendee duration
    @PostMapping(path="/updateAttendeeResponse")
    public ResponseEntity<Object> updateAttendeeResponse(@Valid @RequestBody AttendeeParticipationRequest attendeeParticipationRequest, @RequestHeader(name = "screenName") String screenName,
                                                         @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                         HttpServletRequest request) throws IllegalAccessException, TimeLimitExceededException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + "updateAttendeeResponse" + '"' + " method ...");
        String message = null;

        try{
            message = attendeeService.updateAttendeeResponse(attendeeParticipationRequest, foundUser, timeZone);
        }
        catch(Exception e){

            if(e instanceof ValidationFailedException){
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error("Validation failed: "+ e.getMessage() + "  meetingId = " + attendeeParticipationRequest.getMeetingId() , new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw e;
            }
            else if(e instanceof IllegalAccessException){
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error("One or more fields are not accessible in copying fields from one object to another or comparing 2 objects  ,  meetingId = " + attendeeParticipationRequest.getMeetingId()  , new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw  e;
            }
            else {
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to update attendee response for Meeting for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
                ThreadContext.clearMap();
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
            }

        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, message);
    }

    @PostMapping(path = "updateMeeting/{meetingId}")
    @Transactional
    public  ResponseEntity<Object> updateMeeting(@Valid @RequestBody MeetingRequest meeting, @PathVariable Long meetingId, @RequestHeader(name = "screenName") String screenName,
                                                 @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                 HttpServletRequest request) throws IllegalAccessException{

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " updateMeeting" + '"' + " method ...");
        Meeting meetDb = null;
        MeetingResponse response = null;
        boolean isUpdatable = true;
        try{
            meetDb = meetingRepository.findByMeetingId(meetingId);
            if (meetDb == null) {
                throw new MeetingNotFoundException();
            }
            if(meeting.getDuration() != null && meeting.getStartDateTime() != null){
                meeting.setEndDateTime(meetingService.calculateEndDateTimeForMeeting(meeting.getStartDateTime(), meeting.getDuration()));
            }
            if(meeting.getIsCancelled() && meetDb.getAttendeeList().stream().anyMatch(attendee -> attendee.getDidYouAttend()!=null && attendee.getDidYouAttend().equals(1))){
                throw new ValidationFailedException("Meeting with existing Efforts can not be mark as Deleted.");
            }
            meetingService.convertAllMeetingLocalDateAndTimeToServerTimeZone(meeting, timeZone);
            meetingService.validateMeetingAndUpdateReferenceTask(meeting, accountIds, meetDb);

            if (!meetingService.meetingEditAccess(meetDb.getOrganizerAccountId(), meetDb.getCreatedAccountId(), meetDb.getTeamId(), accountIds)) {
                throw new ValidationFailedException("You are not authorized to update this meeting");
            }
            if(meetDb.getIsCancelled() != null && meetDb.getIsCancelled()) {
                throw new ValidationFailedException("Deleted meeting can't be updated");
            }
            meetingService.validateMeetingWithRecordedEfforts(meeting, meetDb, accountIds);
            if(meeting.getIsCancelled() != null && meeting.getIsCancelled()){
//                meetDb.setIsCancelled(true);
                meetingService.cancelMeeting(meetDb);
                response=meetingService.getMeetingByMeetingId(meetingId, accountIds ,timeZone);
                response.setAttendeeRequestList(meetingService.getFilteredAttendees(meetDb));
                long estimatedTime = System.currentTimeMillis() - startTime;
                ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                logger.info("Exited" + '"' + " updateMeeting" + '"' + " method because completed successfully...");
                ThreadContext.clearMap();
                return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
            }

        }
        catch(Exception e){
            e.printStackTrace();
            if (e instanceof InvalidMeetingTypeException) {
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error("Meeting type validation failed: Invalid Meeting type provided " + " ,     " + meeting.getMeetingType() + " ,    "+ "meetingNumber = " + meeting.getMeetingNumber() + " ,    " + "meetingId = " + meeting.getMeetingId() , new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw e;
            }
            else if(e instanceof IllegalAccessException) {
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error("One or more fields are not accessible in copying fields from one object to another or comparing 2 objects  ,  meetingId = " + meeting.getMeetingId(), new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw e;
            }
            else if(e instanceof MeetingNotFoundException) {
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error("cannot find the meeting requested  , meetingId = "+  meetingId  , new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw e;
            }
            else if(e instanceof ValidationFailedException){
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error("Meeting validation failed: "+e.getMessage()+  " ,  meetingType = " + meeting.getMeetingType() + " ,    "+ "meetingKey = " + meeting.getMeetingKey() + " ,    "+ "meetingNumber = " + meeting.getMeetingNumber() + " ,    " + "meetingId = " + meeting.getMeetingId() , new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw e;
            }
                else {
                    e.printStackTrace();
                    String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                    logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute add Meeting for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
                    ThreadContext.clearMap();
                    if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
                }
            }

        List<Long> deletedAttendeesAccountIds = meetingService.getdeletedAttendeesAccountIdList(meeting, meetDb);
        ArrayList<String> updateMeetingFieldsByUser = meetingService.getMeetingFieldsToUpdate(meeting, meetingId, deletedAttendeesAccountIds);

        Meeting updatedMeeting = new Meeting();
         if (!updateMeetingFieldsByUser.isEmpty()) {
                try {
                    ArrayList<String> basicFields = new ArrayList<>(tableColumnsTypeService.getMeetingDbBasicFields());
                    for(String fieldToUpdate : updateMeetingFieldsByUser){
                        if(!basicFields.contains(fieldToUpdate)){
                            throw new ValidationFailedException("You are not authorized to update field : "+ fieldToUpdate);
                        }
                    }

                    RecurringMeeting recurringMeetingUpdated = meetDb.getRecurringMeeting();
                    updatedMeeting = meetingService.updateAllFieldsInMeetingTable(meeting, meetingId, updateMeetingFieldsByUser, deletedAttendeesAccountIds, recurringMeetingUpdated, timeZone, true);
                    response = meetingService.getMeetingByMeetingId(meetingId, accountIds, timeZone);
                    response.setAttendeeRequestList(meetingService.getFilteredAttendees(meetDb));
                    long estimatedTime = System.currentTimeMillis() - startTime;
                    ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                    logger.info("Exited" + '"' + " updateMeeting" + '"' + " method because completed successfully...");
                    ThreadContext.clearMap();
                    return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
                }
                catch (Exception e) {
                    if (e instanceof InvalidMeetingTypeException) {
                        e.printStackTrace();
                        String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                        logger.error("Meeting type validation failed: Invalid Meeting type provided  ,  meetingType = " + meeting.getMeetingType() + " ,  meetingId = " + meetingId , new Throwable(allStackTraces));
                        ThreadContext.clearMap();
                        throw e;
                    } else if (e instanceof ValidationFailedException) {
                        e.printStackTrace();
                        String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                        logger.error("Meeting validation failed: "+ e.getMessage() +  " ,    " +  "meetingId = " + meetingId +  " ,    "  , new Throwable(allStackTraces));
                        ThreadContext.clearMap();
                        throw e;

                    } else if(e instanceof IllegalAccessException) {
                        e.printStackTrace();
                        String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                        logger.error("One or more fields are not accessible in copying fields from one object to another or comparing 2 objects  ,  meetingId = " + meeting.getMeetingId(), new Throwable(allStackTraces));
                        ThreadContext.clearMap();
                        throw e;
                    }
                    else {
                        e.printStackTrace();
                        String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                        logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to update Work Item number = " + meeting.getMeetingKey() + " for the username = " + foundUser.getPrimaryEmail() + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
                        ThreadContext.clearMap();
                        if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
                    }
                }
            }
         response=meetingService.getMeetingByMeetingId(meetingId,accountIds,timeZone);
         response.setAttendeeRequestList(meetingService.getFilteredAttendees(meetDb));
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
    }

    @PostMapping(path="/createRecurringMeeting")
    @Transactional
    public ResponseEntity<Object> createRecurringMeeting(@Valid @RequestBody RecurringMeetingRequest recurringMeeting, @RequestHeader(name = "screenName") String screenName,
                                                @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " createRecurringMeeting" + '"' + " method ...");
        String message = null;
        RecurringMeetingResponse response = null;
        boolean isValidAccount = false;
        try {
            if(recurringMeeting.getOrgId() != null && recurringMeeting.getTeamId() != null) {
                isValidAccount = accessDomainService.validateOrgAndTeamInRequest(recurringMeeting.getOrgId(), recurringMeeting.getTeamId(), accountIds);
                if (Objects.equals(recurringMeeting.getTeamId(), com.tse.core_application.model.Constants.PERSONAL_TEAM_ID) && Objects.equals(recurringMeeting.getOrgId(), com.tse.core_application.model.Constants.OrgIds.PERSONAL.longValue())) {
                    throw new IllegalStateException("User not allowed to create recurring meeting for personal tasks");
                }
            } else if (recurringMeeting.getOrgId() != null && recurringMeeting.getProjectId() != null) {
                isValidAccount = projectService.validateOrgAndProjectWithAccountIds(recurringMeeting.getOrgId(), recurringMeeting.getProjectId(), accountIds);
            } else if (recurringMeeting.getOrgId() != null) {
                isValidAccount = organizationService.validateOrgUser(recurringMeeting.getOrgId(), accountIds);
            }

            if(!isValidAccount){
                throw new ValidationFailedException("Meeting cannot be created as the user account does not exists in this team");
            }

            EntityPreference entityPreference = entityPreferenceService.fetchEntityPreference(com.tse.core_application.model.Constants.EntityTypes.ORG, recurringMeeting.getOrgId());
            LocalTime officeHrsStartTime = DateTimeUtils.convertServerTimeToUserTimeZone(entityPreference.getOfficeHrsStartTime(), timeZone);
            if (recurringMeeting.getRecurringMeetingStartDateTime() == null) {
                LocalDateTime localDateTime = DateTimeUtils.convertServerDateToUserTimezone(LocalDateTime.now(), timeZone);
                LocalDateTime userStartDateTime = localDateTime.withHour(officeHrsStartTime.getHour())
                        .withMinute(officeHrsStartTime.getMinute())
                        .withSecond(officeHrsStartTime.getSecond());
                recurringMeeting.setRecurringMeetingStartDateTime(userStartDateTime);
            }
            if (recurringMeeting.getMeetingStartTime() == null) {
                recurringMeeting.setMeetingStartTime(officeHrsStartTime);
            }
            if (recurringMeeting.getRecurringMeetingEndDateTime() == null && recurringMeeting.getRecurWeek() != null) {
                if (recurringMeeting.getRecurWeek() > com.tse.core_application.model.Constants.MAX_NUMBER_OF_RECURRING_MEETING) {
                    throw new ValidationFailedException("Occurrence of recurring meeting can't be greater than " + com.tse.core_application.model.Constants.MAX_NUMBER_OF_RECURRING_MEETING);
                }
                LocalDateTime userEndDateTime = meetingService.getNthWeekSaturday (recurringMeeting.getRecurringMeetingStartDateTime().toLocalDate(), recurringMeeting.getRecurWeek());
                recurringMeeting.setRecurringMeetingEndDateTime(userEndDateTime);
            }

            LocalDate currDate = LocalDate.now();
            LocalDate startDate = recurringMeeting.getRecurringMeetingStartDateTime().toLocalDate();

            LocalDate maxValidDate = currDate.minusDays(Constants.Meeting_Preferrences.PAST_MEETING_DAYS_LIMIT);

            if(startDate.isBefore(maxValidDate)){
                throw new ValidationFailedException("You cannot create meeting for past date before 8 days from now");
            }

            response = meetingService.createRecurringMeeting(recurringMeeting, timeZone);

        }catch (Exception e){
            if (e instanceof InvalidMeetingTypeException) {
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error("Meeting type validation failed: Invalid Meeting type provided " + e.getMessage()+ " ,     " + recurringMeeting.getMeetingType() , new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw e;
            }
            else if(e instanceof  ValidationFailedException){
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error("Meeting validation failed: "+ e.getMessage()+  ",  meetingType = " + recurringMeeting.getMeetingType() + " ,    "+ "meetingKey = " + recurringMeeting.getMeetingKey()  , new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw e;
            }
            else if(e instanceof IllegalAccessException){
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error(e.getMessage() + " ,  meetingType = " + recurringMeeting.getMeetingType() + " ,    "+ "meetingKey = " + recurringMeeting.getMeetingKey() , new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw  e;
            }
            else {
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute create Recurring Meeting for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
                ThreadContext.clearMap();
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
            }
        }

        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
    }

    @GetMapping(path = "/getRecurringMeeting/{recurringMeetingId}")
    public ResponseEntity<Object> getRecurringMeeting(@PathVariable(name = "recurringMeetingId") Long recurringMeetingId, @RequestHeader(name = "screenName") String screenName,
                                             @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                             HttpServletRequest request) throws IllegalAccessException{

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getRecurringMeeting" + '"' + " method ...");

        RecurringMeetingResponse response = null;
        try {
            response = meetingService.getRecurringMeetingByRecurringMeetingId(recurringMeetingId,accountIds, timeZone);


            if (response != null) {
                long estimatedTime = System.currentTimeMillis() - startTime;
                ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                logger.info("Exited" + '"' + " getRecurringMeeting" + '"' + " method because completed successfully ...");
                ThreadContext.clearMap();

            }
            else {
                throw new MeetingNotFoundException();
            }
        } catch (Exception e) {
            if (e instanceof MeetingNotFoundException) {
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error("cannot find the meeting requested  , recurringMeetingId = "+  recurringMeetingId  , new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw e;
            }
            else if(e instanceof  ValidationFailedException){
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error("Meeting validation failed: "+ e.getMessage()   , new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw e;
            }
            else if(e instanceof IllegalAccessException){
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error("One or more fields are not accessible in copying fields from one object to another or comparing 2 objects  ,  recurringMeetingId = " +  recurringMeetingId  , new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw  e;
            }
            else {
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get the meeting for username = " + foundUser.getPrimaryEmail() + " ,    " + "recurringMeetingId = " + recurringMeetingId + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
            }
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);

    }


    @GetMapping(path = "/getRecurringMeeting/{recurringMeetingId}/{pageNumber}/{pageSize}")
    public ResponseEntity<Object> getRecurringMeeting(@PathVariable(name = "recurringMeetingId") Long recurringMeetingId, @PathVariable(name = "pageNumber") Integer pageNumber, @PathVariable(name = "pageSize") Integer pageSize,
                                                      @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                             HttpServletRequest request) throws IllegalAccessException {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getRecurringMeeting" + '"' + " method ...");

        RecurringMeetingListResponse response = null;

        try{
            response = meetingService.getRecurringMeetingByRecurringMeetingIdAndPageNumber(recurringMeetingId,accountIds, pageNumber,pageSize, timeZone);

            if (response != null) {
                long estimatedTime = System.currentTimeMillis() - startTime;
                ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                logger.info("Exited" + '"' + " getRecurringMeeting" + '"' + " method because completed successfully ...");
                ThreadContext.clearMap();

            }
            else {
                throw new MeetingNotFoundException();
            }

        }catch (Exception e){
            if(e instanceof MeetingNotFoundException){
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error("cannot find the recurring meeting requested  , recurringMeetingId = "+  recurringMeetingId  , new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw e;
            }
            else if(e instanceof  ValidationFailedException){
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error("Meeting validation failed: "+ e.getMessage()   , new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw e;
            }
            else if(e instanceof IllegalAccessException){
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error("One or more fields are not accessible in copying fields from one object to another or comparing 2 objects  , recurring meetingId = " +  recurringMeetingId  , new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw  e;
            }
            else {
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get the meeting for username = " + foundUser.getPrimaryEmail() + " ,    " + "recurringMeetingId = " + recurringMeetingId + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
            }
        }

        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
    }


    @PostMapping(path = "/getMeetingCondensedView")
    public ResponseEntity<Object> getMeetingCondensedView(@Valid @RequestBody MeetingCondensedViewRequest condensedViewRequest , @RequestHeader(name = "screenName") String screenName,
                                                          @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                          HttpServletRequest request) throws IllegalAccessException {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getMeetingCondensedView" + '"' + " method ...");

        List<AllMeetingsCondensedResponse> response = new ArrayList<>();
        try {

           response = meetingService.getAllMeetingsCondensedResponse(condensedViewRequest,accountIds, timeZone);

            if (!response.isEmpty()) {
                long estimatedTime = System.currentTimeMillis() - startTime;
                ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                logger.info("Exited" + '"' + " getMeetingCondensedView" + '"' + " method because completed successfully ...");
                ThreadContext.clearMap();

            }

        } catch (Exception e) {

               if (e instanceof IllegalAccessException) {
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error("One or more fields are not accessible in copying fields from one object to another or comparing 2 objects  meetingsFromDate : "+ condensedViewRequest.getFromDate() + " , meetingsEndDate"+ condensedViewRequest.getToDate() , new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw e;
            }
               else if(e instanceof  ValidationFailedException){
                   e.printStackTrace();
                   String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                   logger.error("Meeting validation failed: "+ e.getMessage()   , new Throwable(allStackTraces));
                   ThreadContext.clearMap();
                   throw e;
               }
               else {
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get the meeting for username = " + foundUser.getPrimaryEmail() + " Caught Exception: " + e, new Throwable(allStackTraces));
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
            }
        }

        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);

    }


    @PostMapping(path = "/getMeetingExpandedView")
    public ResponseEntity<Object> getMeetingExpandedView(@Valid @RequestBody MeetingExpandedViewRequest expandedViewRequest , @RequestHeader(name = "screenName") String screenName,
                                                          @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                          HttpServletRequest request) throws IllegalAccessException {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getMeetingExpandedView" + '"' + " method ...");

        AllMeetingsExpandedResponse response = new AllMeetingsExpandedResponse();
        try {
            response = meetingService.getAllMeetingsExpandedView(expandedViewRequest, accountIds, timeZone);

            if (response != null) {
                long estimatedTime = System.currentTimeMillis() - startTime;
                ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                logger.info("Exited" + '"' + " getMeetingExpandedView" + '"' + " method because completed successfully ...");
                ThreadContext.clearMap();

            }

        } catch (Exception e) {

            if (e instanceof IllegalAccessException) {
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error("One or more fields are not accessible in copying fields from one object to another or comparing 2 objects  meetingsFromDate : "+ expandedViewRequest.getFromDate() + " , meetingsEndDate"+ expandedViewRequest.getToDate() , new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw e;
            } else {
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get the meeting for username = " + foundUser.getPrimaryEmail() + " Caught Exception: " + e, new Throwable(allStackTraces));
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
            }
        }

        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);

    }

    @PostMapping(path = "/updateRecurringMeeting/{recurringMeetingId}")
    @Transactional
    public  ResponseEntity<Object> updateRecurringMeeting(@Valid @RequestBody RecurringMeetingRequest recurringMeeting, @PathVariable Long recurringMeetingId, @RequestHeader(name = "screenName") String screenName,
                                                 @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                 HttpServletRequest request) throws IllegalAccessException{

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " updateRecurringMeeting" + '"' + " method ...");

        RecurringMeeting recurringMeetDb = null;

        RecurringMeetingResponse response;

        try {
                meetingService.validateRecurringMeeting(recurringMeeting);

                response = meetingService.updateRecurringMeetingByRecurringId(recurringMeeting,recurringMeetingId, timeZone, accountIds);
                long estimatedTime = System.currentTimeMillis() - startTime;
                ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                logger.info("Exited" + '"' + " updateRecurringMeeting" + '"' + " method because completed successfully...");
                ThreadContext.clearMap();
                return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);

            } catch (Exception e) {
                if (e instanceof InvalidMeetingTypeException) {
                    e.printStackTrace();
                    String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                    logger.error("Meeting type validation failed: Invalid Meeting type provided  ,  meetingType = " + recurringMeeting.getMeetingType() + " ,  recurringMeetingId = " + recurringMeetingId , new Throwable(allStackTraces));
                    ThreadContext.clearMap();
                    throw e;
                } else if (e instanceof ValidationFailedException) {
                    e.printStackTrace();
                    String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                    logger.error("Meeting validation failed: "+ e.getMessage() +  " ,    " +  "recurringMeetingId = " + recurringMeetingId +  " ,    "  , new Throwable(allStackTraces));
                    ThreadContext.clearMap();
                    throw e;
                } else {
                    e.printStackTrace();
                    String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                    logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to update Work Item number = " + recurringMeeting.getMeetingKey() + " for the username = " + foundUser.getPrimaryEmail() + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
                    ThreadContext.clearMap();
                    if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
                }
            }

    }

    // Not in use currently
    @PostMapping(path = "/getMeetingByFilters")
    public ResponseEntity<Object> getMeetingByFilters(@Valid @RequestBody MeetingFiltersRequest meetingFiltersRequest , @RequestHeader(name = "screenName") String screenName,
                                                          @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                          HttpServletRequest request) throws IllegalAccessException {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getMeetingByFilters" + '"' + " method ...");

        List<MeetingResponse> response = new ArrayList<>();

        try {

                response = meetingService.getAllMeetingsByFilters(meetingFiltersRequest, accountIds,timeZone);

            if (!response.isEmpty()) {
                long estimatedTime = System.currentTimeMillis() - startTime;
                ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
                logger.info("Exited" + '"' + " getMeetingCondensedView" + '"' + " method because completed successfully ...");
                ThreadContext.clearMap();

            }

        } catch (Exception e) {

            if (e instanceof IllegalAccessException) {
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error("One or more fields are not accessible in copying fields from one object to another or comparing 2 objects  meetingsFromDate : " + meetingFiltersRequest.getFromDate() + " , meetingsEndDate" + meetingFiltersRequest.getToDate(), new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw e;
            } else {
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get the meeting for username = " + foundUser.getPrimaryEmail() + " Caught Exception: " + e, new Throwable(allStackTraces));
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
            }
        }

        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);


    }

    @PostMapping(path="/updateOrganizerResponse")
    @Transactional
    public ResponseEntity<Object> updateOrganizerResponse(@Valid @RequestBody MeetingOrganizerRequest meetingOrganizerRequest, @RequestHeader(name = "screenName") String screenName,
                                                         @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                         HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + "updateOrganizerResponse" + '"' + " method ...");
        String message = null;
        Meeting meetDb;
        MeetingOrganizerResponse response;
        try {
            meetDb = meetingRepository.findByMeetingId(meetingOrganizerRequest.getMeetingId());
            if (meetDb == null) {
                throw new MeetingNotFoundException();
            }

            response = meetingService.updateOrganiserResponseAndMeetingStatus(meetingOrganizerRequest,accountIds,timeZone,meetDb);

        } catch (Exception e) {

            if (e instanceof ValidationFailedException) {
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error("Validation failed: " + e.getMessage() + "  meetingId = " + meetingOrganizerRequest.getMeetingId(), new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw e;
            }
            else if(e instanceof MeetingNotFoundException) {
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error("cannot find the meeting requested  , meetingId = "+  meetingOrganizerRequest.getMeetingId()  , new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw e;
            }
            else if (e instanceof IllegalAccessException) {
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error("One or more fields are not accessible in copying fields from one object to another or comparing 2 objects  ,  meetingId = " + meetingOrganizerRequest.getMeetingId(), new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw e;
            } else {
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to update attendee response for Meeting for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
                ThreadContext.clearMap();
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
            }
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);

    }

    @PostMapping("/searchMeeting")
    public ResponseEntity<Object> searchMeeting(@Valid @RequestBody SearchMeetingRequest searchMeetingRequest, @RequestHeader(name = "screenName") String screenName,
                                          @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                          HttpServletRequest request) throws IllegalAccessException {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " searchMeeting" + '"' + " method ...");

        try {
            List<Long> accountIdsOfUser = requestHeaderHandler.convertToLongList(accountIds);
            List<SearchMeetingResponse> searchMeetingResponseList = meetingService.searchMeeting(searchMeetingRequest, accountIdsOfUser, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " searchMeeting" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, searchMeetingResponseList);
        }
        catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to search meeting for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @PostMapping(path = "/v2/getMeeting")
    public ResponseEntity<Object> getMeetingByEntityAndMeetingNumber(@RequestBody SearchMeetingV2Request meetingRequest, @RequestHeader(name = "screenName") String screenName,
                                                                     @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                                     HttpServletRequest request) throws IllegalAccessException {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getMeeting" + '"' + " method ...");

        MeetingResponse foundMeetingDb = null;

        foundMeetingDb = meetingService.getMeetingByEntityAndMeetingNumber(meetingRequest, accountIds, timeZone);

        if (foundMeetingDb != null) {
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getMeeting" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } else {
            throw new MeetingNotFoundException();
        }

        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, foundMeetingDb);

    }

    @PostMapping(path="/bulkUpdateAttendeeResponse")
    public ResponseEntity<Object> bulkUpdateAttendeeResponse(@Valid @RequestBody List<AttendeeParticipationRequest> attendeeParticipationRequestList,
                                                             @RequestParam("taskId") Long taskId, @RequestHeader(name = "screenName") String screenName,
                                                             @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                             HttpServletRequest request) throws IllegalAccessException, TimeLimitExceededException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + "bulkUpdateAttendeeResponse" + '"' + " method ...");
        String message = null;

        try{
            if (attendeeParticipationRequestList != null && !attendeeParticipationRequestList.isEmpty()) {
                message = attendeeService.bulkUpdateAttendeesResponse(attendeeParticipationRequestList, taskId, foundUser, timeZone, true);
            }
            logger.info("Time Taken for the bulk update the Attendee efforts for the reference meeting is: " + (System.currentTimeMillis() - startTime));
        }
        catch(Exception e){

            if(e instanceof ValidationFailedException){
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error("Validation failed: "+ e.getMessage() + " for one of the meetings while putting their effort.", new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw e;
            }
            else if(e instanceof IllegalAccessException){
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error("One or more fields are not accessible in copying fields from one object to another or comparing 2 objects  , for any of the meetings"  , new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw  e;
            }
            else {
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to update attendee response for Meeting for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
                ThreadContext.clearMap();
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
            }
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, message);
    }

    @PostMapping(path = "/notifyAndPutAttendeesEffort")
    public ResponseEntity<Object> notifyAndPutAttendeesEffort(@RequestBody TaskIdAssignedTo taskIdAssignedTo,
                                                              @RequestHeader(name = "screenName") String screenName,
                                                              @RequestHeader(name = "timeZone") String timeZone,
                                                              @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " notify meeting attendees " + '"' + " method ...");

        Boolean isNotificationSent;
        List<ReferenceMeetingResponse> meetingWithAttendees;

        if (com.tse.core_application.model.Constants.ReferenceMeetingDialogBox.NOTIFY.equals(taskIdAssignedTo.getActionId())) {
            // sending notification to all attendees who had not filled their meeting efforts.
            isNotificationSent = meetingService.fetchAttendeesAndSendNotification(taskIdAssignedTo, timeZone, accountIds);
            if (isNotificationSent) {
                return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "Notification Sent to all Attendees");
            } else {
                return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, "Notification Already Sent to all Attendees");
            }
        } else if (taskIdAssignedTo.getActionId().equals(com.tse.core_application.model.Constants.ReferenceMeetingDialogBox.PUT_EFFORT)) {
            // fetching all the list of meeting and their attendees who had not put their effort to assignedTo/Higher role.
            meetingWithAttendees = meetingService.getReferenceMeetingByTask(taskIdAssignedTo.getTaskId(), accountIds);
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, meetingWithAttendees);
        }
        // in case if none of the request matched.
        return CustomResponseHandler.generateCustomResponse(HttpStatus.BAD_REQUEST, com.tse.core_application.constants.Constants.FormattedResponse.BAD_REQUEST, "InValid Request!!");
    }

    @PostMapping("/updateFetchButtonInMeeting")
    public ResponseEntity<Object> updateFetchedButton(@Valid @RequestBody MeetingAnalysisFetchButtonRequest meetingAnalysisFetchButtonRequest, @RequestHeader(name = "environmentKey") String environmentKey) throws IllegalAccessException {

        long startTime = System.currentTimeMillis();
        logger.info("Entered" + '"' + " updateFetchButtonInMeeting" + '"' + " method ...");

        try {
            String response = meetingService.updateFetchedButtonInMeeting(meetingAnalysisFetchButtonRequest, environmentKey);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " updateFetchButtonInMeeting" + '"' + " method because completed successfully ...");
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
        }
        catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(" API: /meeting/updateFetchButtonInMeeting " + "Something went wrong: Not able to update meeting fetched button for meeting id = " + meetingAnalysisFetchButtonRequest.getMeetingId() + " and for model id " + meetingAnalysisFetchButtonRequest.getModelId() + " Caught Exception: " + e, new Throwable(allStackTraces));
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @PostMapping("/uploadFileMetadataForModel")
    public ResponseEntity<Object> uploadFileMetadataForModel(@Valid @RequestBody UploadFileForModelRequest uploadFileForModelRequest, @RequestHeader(name = "screenName") String screenName,
                                                     @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                     HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " uploadFileMetadataForModel" + '"' + " method ...");
        try{
            UploadFileForModelResponse response = meetingService.uploadFileMetadata(uploadFileForModelRequest, accountIds, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " uploadFileMetadataForModel" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to upload File Metadata for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @PostMapping("/uploadMeetingAnalysis")
    public ResponseEntity<Object> uploadMeetingAnalysis(@Valid @RequestBody MeetingAnalysisDetailsRequest meetingAnalysisDetailsRequest, @RequestHeader(name = "screenName") String screenName,
                                                             @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                             HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " uploadMeetingAnalysis" + '"' + " method ...");
        try{
            MeetingAnalysisDetailsResponse response = meetingService.uploadMeetingAnalysis(meetingAnalysisDetailsRequest, accountIds, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " uploadMeetingAnalysis" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, response);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to upload meeting analysis details for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

}

