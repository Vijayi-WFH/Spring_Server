package com.tse.core_application.controller;

import com.tse.core_application.dto.jitsi.*;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.service.Impl.JitsiService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(value = "*")
@RestController
@RequestMapping(path = "/jitsi")
public class JitsiController {

    private static final Logger logger = LogManager.getLogger(JitsiController.class.getName());

    @Autowired
    JitsiService jitsiService;

    @PostMapping("/token/{accountId}/{room-name}")
    public ResponseEntity<Object> getTokenForJitsiMeeting(@PathVariable("accountId") Long accountId,
                                                          @PathVariable("room-name") String roomName,
                                                          @RequestBody() JitsiTokenRequestDto tokenRequestDto,
                                                          @RequestHeader(value = "timezone") String timezone){
        String token = jitsiService.getTokenForJitsiMeeting(accountId, roomName, tokenRequestDto, timezone);
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, HttpStatus.OK.getReasonPhrase(), token);
    }

    @PostMapping("/token/guest")
    public ResponseEntity<Object> getTokenToInviteGuest(@RequestBody JitsiGuestTokenRequest request,
                                                        @RequestHeader("accountIds") List<Long> accountIds){
        String token = jitsiService.getTokenToInviteGuests(request, accountIds);
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, HttpStatus.OK.getReasonPhrase(), token);
    }

    //will be called by jitsi-prosody webhook in every time Meeting Ended on Jitsi.
    @PostMapping("/meeting-summary")
    public ResponseEntity<Object> getMeetingSummary(@RequestBody JitsiMeetingSummaryDTO request,
                                                    @RequestHeader("screenName") String screenName){
        logger.info("Jitsi MeetingSummary api called by JitsiServer with screenName: {}", screenName);
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, "Retrieve successfully!",
                jitsiService.processMeetingSummaryResponseFromJitsi(request));
    }

    /* This API is to fetch the active meetings running on the jitsi server.
    This is the wrapper api that is calling internal Jitsi's own exposed api of active-meetings */
    @GetMapping("/active-meetings")
    public ResponseEntity<Object> getActiveMeetings(@RequestParam("orgId") Long orgId,
                                                    @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                                    @RequestHeader(name = "accountIds") String accountIds){
        logger.info("Jitsi Active Meetings api called by: {} with screenName: {} and having timezone: {}", accountIds, screenName, timeZone);
        ActiveUpcomingMeetingsResponse meetingsResponse = jitsiService.findAllActiveJitsiMeetings(orgId, screenName, timeZone, accountIds);
        if(meetingsResponse != null && (!meetingsResponse.getOngoingMeetings().isEmpty() || !meetingsResponse.getUpcomingMeetings().isEmpty())){
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, "Retrieve successfully!", meetingsResponse);
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, "No Active Jitsi Meetings Found!", meetingsResponse);
    }
}
