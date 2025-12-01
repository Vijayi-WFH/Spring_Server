package com.tse.core_application.service.Impl;

import com.google.gson.Gson;
import com.tse.core_application.constants.ControllerConstants;
import com.tse.core_application.constants.RoleEnum;
import com.tse.core_application.dto.jitsi.*;
import com.tse.core_application.dto.meeting.ScheduledMeetingsResponse;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.model.*;
import com.tse.core_application.repository.*;
import com.tse.core_application.utils.SHAJWTUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
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

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.tse.core_application.constants.Constants.Meeting_Preferrences.BUFFER_TIME_FOR_SCHEDULED_MEETING;
import static com.tse.core_application.utils.DateTimeUtils.convertUserDateToServerTimezone;

@Service
public class JitsiService {

    private static final Logger logger = LogManager.getLogger(JitsiService.class.getName());

    @Autowired
    UserAccountRepository userAccountRepository;

    @Autowired
    AccessDomainRepository accessDomainRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    SHAJWTUtil shaJwtUtil;

    @Autowired
    EntityPreferenceRepository entityPreferenceRepository;

    @Autowired
    AttendeeService attendeeService;

    @Autowired
    MeetingRepository meetingRepository;

    @Value("${jitsi.meet.url}")
    private String jitsiBaseUrl;

    public String getTokenForJitsiMeeting(Long accountId, String roomName, JitsiTokenRequestDto requestDto, String timezone){

        Map<String, String> claims = new HashMap<>();
        UserAccount userAccount = userAccountRepository.findFkUserIdByAccountIdAndIsActiveTrue(accountId);
        String userName = userAccount.getFkUserId().getFirstName() + " " + userAccount.getFkUserId().getLastName();

        if(userAccount!=null){
            claims.put("accountId", accountId.toString());
            claims.put("userId", userAccount.getFkUserId().getUserId().toString());
            claims.put("email", userAccount.getEmail());
            claims.put("username", userName);
            claims.put("orgId", userAccount.getOrgId().toString());
            claims.put("roomName", roomName);
            claims.put("meetingId", requestDto.getMeetingId().toString());
            claims.put("isOrganiser", requestDto.getIsOrganiser().toString());
            claims.put("teamId", requestDto.getTeamId() != null ? requestDto.getTeamId().toString() : null);
            claims.put("projectId", requestDto.getProjectId() != null ? requestDto.getProjectId().toString() : null);
            claims.put("timezone", timezone);
            claims.put("userType", "PLATFORM");
        } else
            throw new ValidationFailedException("Either Account is inactive or AccountId is not valid!!");

        return shaJwtUtil.doGenerateToken(claims);
    }

    public String getTokenToInviteGuests(JitsiGuestTokenRequest request, List<Long> accountIds) {
        Map<String, String> claims = new HashMap<>();
        if(accountIds != null && !accountIds.isEmpty()) {
            List<AccessDomain> accessDomainList = accessDomainRepository.findByAccountIdInAndRoleIdInAndIsActive(accountIds,
                    List.of(RoleEnum.ORG_ADMIN.getRoleId(), RoleEnum.BACKUP_ORG_ADMIN.getRoleId(), RoleEnum.PROJECT_ADMIN.getRoleId(), RoleEnum.TEAM_ADMIN.getRoleId()), true);

            if (accessDomainList != null && !accessDomainList.isEmpty()) {
                String userName = request.getFirstName() + " " + request.getLastName();
                if (request.getRoomName() != null) {
                    claims.put("senderUserId", accessDomainList.get(0).getUserAccount().getFkUserId().getUserId().toString());
                    claims.put("email", request.getEmail());
                    claims.put("username", userName);
                    claims.put("roomName", request.getRoomName());
                    claims.put("expirationTime", request.getExpirationTime().toString());
                    claims.put("userType", "GUESTS");
                    claims.put("orgId", accessDomainList.get(0).getUserAccount().getOrgId().toString());
                    return shaJwtUtil.doGenerateTokenForGuests(claims);
                } else
                    throw new ValidationFailedException("RoomName is not valid!!");
            }
            throw new ValidationFailedException("User does not have access to create a GUEST invite token!!");
        }
        throw new ValidationFailedException("Some unknown error occurred please try later!!");
    }

    public JitsiMeetingSummaryDTO processMeetingSummaryResponseFromJitsi(JitsiMeetingSummaryDTO request) {
        if(!request.getParticipants().isEmpty() && request.getParticipants().size() > 1) {
            Long orgId = Long.valueOf(request.getParticipants().get(0).getOrgId());
            Long meetingId = request.getParticipants().get(0).getMeetingId();
            String timezone = request.getParticipants().get(0).getTimezone();
            Map<Long, List<JitsiParticipantDTO>> participantMap = request.getParticipants().stream()
                    .collect(Collectors.groupingBy(JitsiParticipantDTO::getAccountId));

            Map<Long, Long> systemGenEffortsMap = new HashMap<>();

            for (Map.Entry<Long, List<JitsiParticipantDTO>> entry : participantMap.entrySet()) {
                systemGenEffortsMap.put(entry.getKey(), calculateTotalTime(entry.getValue()));
            }
            // Add attendees and Put the jitsi efforts time into the System Generated Column of table Attendee.
            attendeeService.updateSystemGenVijayiMeetEfforts(meetingId, systemGenEffortsMap);

            Optional<EntityPreference> orgPreferenceOpt = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, orgId);
            if(orgPreferenceOpt.get() != null && orgPreferenceOpt.get().getMeetAutoLogEffortEnabled()){
                //Update the attendee duration automatically. Call the existing attendee method to put the effort so all the validation will check and insert the effort into the table.
                //Putting the try catch block here so if an automatic process failed due to validation issue remaining process should not break.
                try{
                    attendeeService.autoLoggedEffortsInTimesheetByVijayiMeet(meetingId, systemGenEffortsMap, timezone);
                } catch (Exception e) {
                    logger.error("There is some exception occurred while automatic updating the efforts of the participants from the Vijayi-Meet: {}", e);
                }
            }
        }
        Gson gson = new Gson();
        System.out.println(gson.toJson(request));
        return request;
    }

    private Long calculateTotalTime(List<JitsiParticipantDTO> sessions) {
        List<Long[]> merged = mergeIntervals(sessions);
        return merged.stream()
                .mapToLong(interval -> interval[1] - interval[0])
                .sum() / 60;
    }

    private List<Long[]> mergeIntervals(List<JitsiParticipantDTO> sessions) {
        List<Long[]> intervals = sessions.stream()
                .map(s -> new Long[]{s.getJoinTime(), s.getLeaveTime()})
                .sorted(Comparator.comparingLong(a -> a[0]))
                .collect(Collectors.toList());

        List<Long[]> merged = new ArrayList<>();
        for (Long[] interval : intervals) {
            if (merged.isEmpty() || merged.get(merged.size() - 1)[1] < interval[0]) {
                merged.add(interval);
            } else {
                merged.get(merged.size() - 1)[1] = Math.max(merged.get(merged.size() - 1)[1], interval[1]);
            }
        }
        return merged;
    }

    public ActiveUpcomingMeetingsResponse findAllActiveJitsiMeetings(Long orgId, String screenName, String timezone, String accountIds) {

        List<JitsiActiveMeetingResponse> ongoingMeetings = new ArrayList<>();
        List<ScheduledMeetingsResponse> upcomingMeetings = new ArrayList<>();

        String url = jitsiBaseUrl + ControllerConstants.JitsiApi.activeMeetings;
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("timeZone", timezone);
        headers.add("screenName", screenName);
        headers.add("accountIds", accountIds);
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<Object> requestEntity = new HttpEntity<>(headers);
        try {
            ResponseEntity<List<JitsiActiveMeetingResponse>> responseEntity = restTemplate.exchange(url, HttpMethod.GET, requestEntity, new ParameterizedTypeReference<>() {
            });
            if (responseEntity.getBody() != null) {
                ongoingMeetings = responseEntity.getBody();
                ongoingMeetings = ongoingMeetings.stream().filter(meeting -> meeting.getOrgId().equals(orgId))
                        .peek(meeting -> meeting.setElapsedTime(((System.currentTimeMillis() / 1000) - meeting.getStartedAt()) / 60))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            logger.error("Problems while fetching the details of Vijayi_Active_Meetings: {}", e.getMessage());
        }

        LocalDateTime fromDate = LocalDateTime.now();
        LocalDateTime toDate = fromDate.plusMinutes(BUFFER_TIME_FOR_SCHEDULED_MEETING);

        List<Meeting> scheduledMeetings = meetingRepository.findByStartDateTimeGreaterThanEqualAndStartDateTimeLessThanEqualAndOrgId(fromDate, toDate, orgId);
        if (scheduledMeetings != null && !scheduledMeetings.isEmpty()) {
            for(Meeting meeting : scheduledMeetings) {
                ScheduledMeetingsResponse meetingsResponse = new ScheduledMeetingsResponse();
                BeanUtils.copyProperties(meeting, meetingsResponse);
                upcomingMeetings.add(meetingsResponse);
            }
        }

        return new ActiveUpcomingMeetingsResponse(ongoingMeetings, upcomingMeetings);
    }
}
