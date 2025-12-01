package com.tse.core_application.controller;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.service.Impl.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(path = "/batch")
public class BatchController {

    @Autowired
    private TeamService teamService;

    @Transactional
    @GetMapping(path = "/updateTeamInitials")
    public ResponseEntity<Object> updateTeamInitials(@RequestHeader(name = "screenName") String screenName,
                                                     @RequestHeader(name = "timeZone") String timeZone,
                                                     @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        try {
            teamService.updateAllTeamInitials();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, "Success");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}
