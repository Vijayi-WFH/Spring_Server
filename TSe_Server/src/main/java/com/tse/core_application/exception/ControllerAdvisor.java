package com.tse.core_application.exception;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.custom.model.RestResponseWithData;
import com.tse.core_application.custom.model.RestResponseWithoutData;
import com.tse.core_application.exception.geo_fencing.FenceNotFoundException;
import com.tse.core_application.exception.geo_fencing.GeoFencingAccessDeniedException;
import com.tse.core_application.exception.geo_fencing.PolicyNotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.websocket.AuthenticationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.management.relation.InvalidRelationTypeException;
import javax.naming.TimeLimitExceededException;
import javax.persistence.EntityNotFoundException;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@ControllerAdvice
public class ControllerAdvisor extends ResponseEntityExceptionHandler {

    private static final Logger log = LogManager.getLogger(ControllerAdvisor.class);

    @Override
    public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpHeaders headers,
                                                               HttpStatus status, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        restResponseWithoutData.setStatus(status.value());
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setTimestamp(UtcDateTime);
//        String errorMessage = "Invalid Request";
        String errorMessage = "Error: " + e.getBindingResult().getFieldError().getDefaultMessage();
        restResponseWithoutData.setMessage(errorMessage);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolationException(ConstraintViolationException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.BAD_REQUEST.value());
        // Get the first constraint violation message
        ConstraintViolation<?> violation = e.getConstraintViolations().iterator().next();
        String message = violation.getMessage();
        if (message.contains("unique_task_number")) {
            restResponseWithoutData.setMessage("Something went wrong at our end. Please try again later.");
        } else {
            restResponseWithoutData.setMessage(message);
        }
        restResponseWithoutData.setMessage(violation.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Object> handleEntityNotFoundException(EntityNotFoundException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.NOT_FOUND.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MissingDetailsException.class)
    public ResponseEntity<Object> handleMissingDetailsException(MissingDetailsException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.PRECONDITION_FAILED.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.PRECONDITION_FAILED);
    }

    @ExceptionHandler(BoardViewErrorException.class)
    public ResponseEntity<Object> handleBoardViewErrorException(BoardViewErrorException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.BAD_REQUEST.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidRequestParamater.class)
    public ResponseEntity<Object> handleInvalidRequestParamater(InvalidRequestParamater e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.BAD_REQUEST.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InternalServerErrorException.class)
    public ResponseEntity<Object> handleInternalServerErrorException(InternalServerErrorException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(SubTaskDetailsMissingException.class)
    public ResponseEntity<Object> handleSubTaskDetailsMissingException(SubTaskDetailsMissingException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(InvalidInviteException.class)
    public ResponseEntity<Object> handleInvalidInviteException(InvalidInviteException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.BAD_REQUEST.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidRequestHeaderException.class)
    public ResponseEntity<Object> handleInvalidRequestHeaderException(InvalidRequestHeaderException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.NOT_ACCEPTABLE.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler(UserDoesNotExistException.class)
    public ResponseEntity<Object> handleUserDoesNotExistException(UserDoesNotExistException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.NOT_FOUND.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UserAlreadyExistException.class)
    public ResponseEntity<Object> handleUserAlreadyExistException(UserAlreadyExistException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<Object> handleInvalidOtpException(InvalidOtpException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.NOT_FOUND.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(NoDataFoundException.class)
    public ResponseEntity<Object> handleNoDataFoundException(NoDataFoundException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.NOT_FOUND.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ServerBusyException.class)
    public ResponseEntity<Object> handleServerBusyException(ServerBusyException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.ACCEPTED.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.ACCEPTED);
    }

    @ExceptionHandler(UnauthorizedLoginException.class)
    public ResponseEntity<Object> handleUnauthorizedLoginException(UnauthorizedLoginException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.UNAUTHORIZED.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(OrganizationDoesNotExistException.class)
    public ResponseEntity<Object> handleOrganizationDoesNotExistException(OrganizationDoesNotExistException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.NOT_FOUND.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<Object> handleProjectNotFoundException(ProjectNotFoundException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.NOT_FOUND.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<Object> handleTaskNotFoundException(TaskNotFoundException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.NOT_FOUND.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MeetingNotFoundException.class)
    public ResponseEntity<Object> handleMeetingNotFoundException(MeetingNotFoundException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.NOT_FOUND.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<Object> handleCommentNotFoundException(CommentNotFoundException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.NOT_FOUND.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Object> handleForbiddenException(ForbiddenException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.FORBIDDEN.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(TeamNotFoundException.class)
    public ResponseEntity<Object> handleTeamNotFoundException(TeamNotFoundException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.NOT_FOUND.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(FileNameException.class)
    public ResponseEntity<Object> handleFileNameException(FileNameException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.BAD_REQUEST.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<Object> handleFileStorageException(FileStorageException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(DuplicateFileException.class)
    public ResponseEntity<Object> handleDuplicateFileException(DuplicateFileException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<Object> handleFileNotFoundException(FileNotFoundException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.NOT_FOUND.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(WorkflowTypeDoesNotExistException.class)
    public ResponseEntity<Object> handleWorkflowTypeDoesNotExistException(WorkflowTypeDoesNotExistException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.BAD_REQUEST.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<Object> handleAuthenticationFailedException(AuthenticationFailedException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.UNAUTHORIZED.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(InvalidUserNameException.class)
    public ResponseEntity<Object> handleInvalidUserNameException(InvalidUserNameException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.UNAUTHORIZED.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(TaskViewException.class)
    public ResponseEntity<Object> handleTaskViewException(TaskViewException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.FORBIDDEN.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ValidationFailedException.class)
    public ResponseEntity<Object> handleValidationFailedException(ValidationFailedException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.FORBIDDEN.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(IllegalAccessException.class)   // new exception added in Task 2539
    public ResponseEntity<Object> handleIllegalAccessException(IllegalAccessException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.FORBIDDEN.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.BAD_REQUEST.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoTaskMediaFoundException.class)
    public ResponseEntity<Object> handleNoTaskMediaFoundException(NoTaskMediaFoundException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.NOT_FOUND.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidApiEndpointException.class)
    public ResponseEntity<Object> handleInvalidApiEndpointException(InvalidApiEndpointException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.NOT_FOUND.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IncorrectRequestHeaderException.class)
    public ResponseEntity<Object> handleIncorrectRequestHeaderException(IncorrectRequestHeaderException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.NOT_ACCEPTABLE.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler(WorkflowTaskStatusFailedException.class)
    public ResponseEntity<Object> handleWorkflowTaskStatusFailedException(WorkflowTaskStatusFailedException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.NOT_ACCEPTABLE.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler(DateAndTimePairFailedException.class)
    public ResponseEntity<Object> handleDateAndTimePairFailedException(DateAndTimePairFailedException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.NOT_ACCEPTABLE.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler(TaskEstimateException.class)
    public ResponseEntity<Object> handleTaskEstimateException(TaskEstimateException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.NOT_ACCEPTABLE.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler(InvalidStatsRequestFilterException.class)
    public ResponseEntity<Object> handleInvalidStatsRequestFilterException(InvalidStatsRequestFilterException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.NOT_ACCEPTABLE.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler(InvalidMeetingTypeException.class)
    public ResponseEntity<Object> handleInvalidMeetingTypeException(InvalidMeetingTypeException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.NOT_ACCEPTABLE.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler(StickyNoteFailedException.class)
    public ResponseEntity<Object> handleStickyNoteFailedException(StickyNoteFailedException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.NOT_ACCEPTABLE.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.NOT_ACCEPTABLE);
    }


    public static String getCurrentUTCTimeStamp() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm:ss:SSS Z");
        String UtcDateTime = now.format(formatter);
        return UtcDateTime;
    }

    @ExceptionHandler(InvalidAuthentication.class)
    public ResponseEntity<Object> handleInvalidAuthenticationException(InvalidAuthentication e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.UNAUTHORIZED.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UserNotRegisteredException.class)
    public ResponseEntity<Object> handleUserNotRegisteredException(UserNotRegisteredException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.UNAUTHORIZED.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Object> handleUnauthorizedException(UnauthorizedException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.UNAUTHORIZED.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(LeaveApplicationValidationException.class)
    public ResponseEntity<Object> handleLeaveApplicationValidationException(LeaveApplicationValidationException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.NOT_ACCEPTABLE.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler(DeleteTaskException.class)
    public ResponseEntity<Object> handleDeleteTaskException(DeleteTaskException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.NOT_ACCEPTABLE.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler(IllegalStateException.class)   // new exception added in Task 2539
    public ResponseEntity<Object> handleIllegalStateException(IllegalStateException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.FORBIDDEN.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(TimeLimitExceededException.class)   // new exception added in Task 2539
    public ResponseEntity<Object> handleTimeLimitExceededException(TimeLimitExceededException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.FORBIDDEN.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(IOException.class)   // new exception added in Task 2539
    public ResponseEntity<Object> handleIOException(IOException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.NOT_ACCEPTABLE.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> handleAuthenticationException(AuthenticationException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.NOT_ACCEPTABLE.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler(InvalidRelationTypeException.class)
    public ResponseEntity<Object> handleConstraintViolationException(InvalidRelationTypeException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String UtcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.FORBIDDEN.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(UtcDateTime);
        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(DuplicateFileNameException.class)
    public ResponseEntity<Object> handleDuplicateFileNameException(DuplicateFileNameException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String utcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.CONFLICT.value());
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(utcDateTime);

        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(DependencyValidationException.class)
    public ResponseEntity<Object> handleDependencyValidationException(DependencyValidationException e, WebRequest request) {
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String utcDateTime = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(Constants.FormattedResponse.DEPENDENCY_VALIDATION_ERROR_CODE);
        restResponseWithoutData.setMessage(e.getMessage());
        restResponseWithoutData.setTimestamp(utcDateTime);

        return new ResponseEntity<>(restResponseWithoutData, HttpStatus.BAD_REQUEST);
    }
    /**
     * Handle all sort of exceptions that are Uncaught/Unhandled.
     * */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllExceptions(Exception e, WebRequest request) {
        RestResponseWithData response = new RestResponseWithData();
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.setMessage("Unexpected error: " + e.getLocalizedMessage());
        response.setTimestamp(getCurrentUTCTimeStamp());
        response.setData(e);
        log.error("e: ", e);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handle exceptions that are related with Invalid Number Format.
     * */
    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<Object> handleNumberFormatException(NumberFormatException e, WebRequest request) {
        RestResponseWithoutData response = new RestResponseWithoutData();
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setMessage(e.getMessage());
        response.setTimestamp(getCurrentUTCTimeStamp());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(FirebaseNotificationException.class)
    public ResponseEntity<Object> handleFireBaseNotificationException(FirebaseNotificationException e, WebRequest request) {
        RestResponseWithoutData response = new RestResponseWithoutData();
        response.setStatus(HttpStatus.CONFLICT.value());
        response.setMessage(e.getMessage());
        response.setTimestamp(getCurrentUTCTimeStamp());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(NoSuchAlgorithmException.class)
    public ResponseEntity<Object> handleNoSuchAlgorithmException(NoSuchAlgorithmException e, WebRequest request) {
        RestResponseWithoutData response = new RestResponseWithoutData();
        response.setStatus(HttpStatus.CONFLICT.value());
        response.setMessage(e.getMessage());
        response.setTimestamp(getCurrentUTCTimeStamp());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleHttpMessageNotReadableException(HttpMessageNotReadableException e, WebRequest request) {
        RestResponseWithoutData response = new RestResponseWithoutData();
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setMessage(e.getMessage());
        response.setTimestamp(getCurrentUTCTimeStamp());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ReferenceMeetingException.class)
    public ResponseEntity<Object> handleReferenceMeetingException(ReferenceMeetingException e, WebRequest request) {
        RestResponseWithData restResponseWithData = new RestResponseWithData();
        String utcDateTime = getCurrentUTCTimeStamp();
        restResponseWithData.setStatus(Constants.FormattedResponse.REFERENCE_MEETING_ERROR_CODE);
        restResponseWithData.setMessage(e.getMessage());
        restResponseWithData.setTimestamp(utcDateTime);
        restResponseWithData.setData(e.getIsNotificationOnCooldown());

        return new ResponseEntity<>(restResponseWithData, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(GeoFencingAccessDeniedException.class)
    public ResponseEntity<Object> handleGeoFencingAccessDeniedException(GeoFencingAccessDeniedException e, WebRequest request) {
        RestResponseWithData restResponseWithData = new RestResponseWithData();
        String utcDateTime = getCurrentUTCTimeStamp();
        restResponseWithData.setStatus(HttpStatus.FORBIDDEN.value());
        restResponseWithData.setMessage(e.getMessage());
        restResponseWithData.setTimestamp(utcDateTime);

        return new ResponseEntity<>(restResponseWithData, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(FenceNotFoundException.class)
    public ResponseEntity<Object> handleGeoFencingAccessDeniedException(FenceNotFoundException e, WebRequest request) {
        RestResponseWithData restResponseWithData = new RestResponseWithData();
        String utcDateTime = getCurrentUTCTimeStamp();
        restResponseWithData.setStatus(HttpStatus.NOT_FOUND.value());
        restResponseWithData.setMessage(e.getMessage());
        restResponseWithData.setTimestamp(utcDateTime);

        return new ResponseEntity<>(restResponseWithData, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(PolicyNotFoundException.class)
    public ResponseEntity<Object> handleGeoFencingAccessDeniedException(PolicyNotFoundException e, WebRequest request) {
        RestResponseWithData restResponseWithData = new RestResponseWithData();
        String utcDateTime = getCurrentUTCTimeStamp();
        restResponseWithData.setStatus(HttpStatus.NOT_FOUND.value());
        restResponseWithData.setMessage(e.getMessage());
        restResponseWithData.setTimestamp(utcDateTime);

        return new ResponseEntity<>(restResponseWithData, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<Object> handleHttpClientErrorException(HttpClientErrorException e, WebRequest request) {
        RestResponseWithoutData restResponseWithData = new RestResponseWithoutData();
        String utcDateTime = getCurrentUTCTimeStamp();
        restResponseWithData.setStatus(e.getStatusCode().value());
        restResponseWithData.setMessage(e.getResponseBodyAsString());
        restResponseWithData.setTimestamp(utcDateTime);

        return new ResponseEntity<>(restResponseWithData, HttpStatus.CONFLICT);
    }

}
