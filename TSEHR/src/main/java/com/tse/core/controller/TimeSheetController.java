package com.tse.core.controller;

import com.tse.core.custom.model.TimeSheetRequest;
import com.tse.core.dto.TimeSheetResponse;
import com.tse.core.exception.InternalServerErrorException;
import com.tse.core.handlers.StackTraceHandler;
import com.tse.core.service.TimeSheetService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.ValidationException;
import java.util.List;

@RestController
public class TimeSheetController {

    @Autowired
    private TimeSheetService timeSheetService;

    private static final Logger logger = LogManager.getLogger(TimeSheetController.class.getName());


    @RequestMapping(path = "/getts")
    public ResponseEntity<Object> getTimeSheet(@RequestBody TimeSheetRequest tsRequest, HttpServletRequest request ) throws IllegalAccessException, ValidationException {

        String userId = request.getHeader("userId");
        try {
            List<TimeSheetResponse> timeSheetResponses = timeSheetService.getTimeSheet(tsRequest, userId, request.getHeader("timeZone"));
            return new ResponseEntity<>(timeSheetResponses,HttpStatus.OK);
        }
        catch (Exception e){
            if( e instanceof IllegalAccessException) {
                return new ResponseEntity<>(e.getMessage(),HttpStatus.NOT_ACCEPTABLE);
            }
            else if( e instanceof ValidationException){
                return new ResponseEntity<>(e.getMessage(),HttpStatus.NOT_ACCEPTABLE);
            }
            else{
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error("some error in TimeSheet", new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw new InternalServerErrorException();
            }
        }

    }


}
