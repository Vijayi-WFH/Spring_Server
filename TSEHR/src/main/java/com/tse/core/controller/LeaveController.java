package com.tse.core.controller;

import com.tse.core.dto.DateRequest;
import com.tse.core.dto.leave.DoctorCertificateMetaData;
import com.tse.core.dto.leave.Request.*;
import com.tse.core.dto.leave.Response.*;
import com.tse.core.exception.LeaveApplicationValidationException;
import com.tse.core.handlers.StackTraceHandler;
import com.tse.core.model.Constants;
import com.tse.core.model.leave.LeaveApplication;
import com.tse.core.repository.leaves.LeaveApplicationRepository;
import com.tse.core.repository.leaves.LeaveApplicationStatusRepository;
import com.tse.core.repository.supplements.OrganizationRepository;
import com.tse.core.repository.supplements.UserAccountRepository;
import com.tse.core.service.LeaveService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@CrossOrigin(value = "*")
@RestController
@RequestMapping(path = "/leave")
public class LeaveController {

    private static final Logger logger = LogManager.getLogger(LeaveController.class.getName());
    @Autowired
    private LeaveService leaveService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private LeaveApplicationRepository leaveApplicationRepository;

    @Autowired
    private LeaveApplicationStatusRepository leaveApplicationStatusRepository;

    @PostMapping("/defaultLeavePolicyAssignment")
    @Transactional
    public ResponseEntity<Object> defaultLeavePolicyAssignment(HttpServletRequest request){
        try{
            Long accountId = Long.valueOf(request.getHeader("accountId"));
            Long orgId = Long.valueOf(request.getHeader("orgId"));
            Boolean isNewOrg = Boolean.valueOf(request.getHeader("isNewOrg"));
            leaveService.defaultLeavePolicyAssignment(accountId,orgId,isNewOrg);
            return new ResponseEntity<>("Leave policies are assigned successfully.",HttpStatus.OK);
        }catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to assign default leave policy. Caught Exception: " + e, new Throwable(allStackTraces));
            return new ResponseEntity<>(e.getMessage(),HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @PostMapping("/addLeavePolicy")
    @Transactional
    public ResponseEntity<Object> addLeavePolicy(@RequestBody LeavePolicyRequest leavePolicyRequest,
                                                 HttpServletRequest request){
        try{
            long leavePolicyId;
            String accountIds =request.getHeader("accountIds");
            if(accountIds.contains(leavePolicyRequest.getAccountId().toString())) {
                if (leaveService.validateLeavePolicyRequest(leavePolicyRequest, null)) {
                    leavePolicyId = leaveService.addLeavePolicy(leavePolicyRequest);
                } else {
                    return new ResponseEntity<>("Leave Policy Validation failed. Please check again!", HttpStatus.NOT_ACCEPTABLE);
                }
            }else{
                return new ResponseEntity<>("This accountId does not belongs to this user. Please check again!",HttpStatus.UNAUTHORIZED);
            }
            return new ResponseEntity<>("New policy with Id "+ leavePolicyId +" added successfully.",HttpStatus.OK);
        }catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to add leave policy. Caught Exception: " + e, new Throwable(allStackTraces));
            return new ResponseEntity<>(e.getMessage(),HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @PostMapping("/getUserLeavePolicy")
    public ResponseEntity<Object> getUserLeavePolicy(
                                                    HttpServletRequest request) {
        try{
            String accountIds =request.getHeader("accountIds");
            Long accountId =Long.valueOf(request.getHeader("accountId"));
            String timeZone = request.getHeader("timeZone");
            List<LeavePolicyResponse> response= null;
            response=leaveService.getUsersLeavePolicy(accountId, timeZone);
            return new ResponseEntity<>(response,HttpStatus.OK);
        }
        catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to get leave policy. Caught Exception: " + e, new Throwable(allStackTraces));
            return new ResponseEntity<>(e.getMessage(),HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @PostMapping("/getOrgLeavePolicy")
    public ResponseEntity<Object> getOrgLeavePolicy(
            HttpServletRequest request) {
        try{
            String accountIds =request.getHeader("accountIds");
            Long orgId =Long.valueOf(request.getHeader("orgId"));
            String tmieZone = request.getHeader("timeZone");
            List<LeavePolicyResponse> response= null;
            if(organizationRepository.existsById(orgId) && leaveService.validateOrganization(orgId,accountIds)){
                response=leaveService.getOrgLeavePolicy(orgId, tmieZone);
            }
            else{
                return new ResponseEntity<>("This orgId either does not exists or does not belongs to this user. Please check again!",HttpStatus.NOT_ACCEPTABLE);
            }
            return new ResponseEntity<>(response,HttpStatus.OK);
        }
        catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to get org leave policy. Caught Exception: " + e, new Throwable(allStackTraces));
            return new ResponseEntity<>(e.getMessage(),HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @PostMapping("/updateLeavePolicy")
    @Transactional
    public ResponseEntity<Object> updateLeavePolicy(@RequestBody @Valid LeavePolicyRequest leavePolicyRequest,
                                                 HttpServletRequest request){
        try{
            Long leavePolicyId = Long.valueOf(request.getHeader("leavePolicyId"));
            String accountIds =request.getHeader("accountIds");
            if(accountIds.contains(leavePolicyRequest.getAccountId().toString())) {
                if (leaveService.validateLeavePolicyRequest(leavePolicyRequest, leavePolicyId)) {
                    leaveService.updateLeavePolicy(leavePolicyRequest, leavePolicyId);
                } else {
                    return new ResponseEntity<>("Leave Policy Validation failed. Please check again!", HttpStatus.NOT_ACCEPTABLE);
                }
            }
            else{
                return new ResponseEntity<>("This accountId does not belongs to this user. Please check again!",HttpStatus.UNAUTHORIZED);
            }
            return new ResponseEntity<>("Your policy with Id "+ leavePolicyId +" updated successfully. Updated leave policy will be effective from the 1st of the current month",HttpStatus.OK);
        }catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to update leave policy. Caught Exception: " + e, new Throwable(allStackTraces));
            return new ResponseEntity<>(e.getMessage(),HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @PostMapping("/assignLeavePolicyToUser")
    @Transactional
    public ResponseEntity<Object> assignLeavePolicyToUser(@RequestBody AssignLeavePolicyRequest assignLeavePolicyRequest,
                                                 HttpServletRequest request){
        try{
            if(leaveService.assignLeavePolicyToUser(assignLeavePolicyRequest)){
                return new ResponseEntity<>("Account Id "+ assignLeavePolicyRequest.getAccountId()+" is assigned with leave policy Id "+ assignLeavePolicyRequest.getNewLeavePolicyId()+". ",HttpStatus.OK);
            }
            else{
                return new ResponseEntity<>("Leave Policy Validation failed. Please check again!",HttpStatus.NOT_ACCEPTABLE);
            }
        }catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if (e instanceof LeaveApplicationValidationException) {
                logger.error("Unable to assign leave policy Caught Exception: " + e, new Throwable(allStackTraces));
                throw new LeaveApplicationValidationException(e.getMessage());
            }else{
                logger.error("Unable to assign leave policy. Caught Exception: " + e, new Throwable(allStackTraces));
                return new ResponseEntity<>(e.getMessage(),HttpStatus.NOT_ACCEPTABLE);
            }

        }
    }

    @PostMapping("/reassignLeavePolicyToUser")
    @Transactional
    public ResponseEntity<Object> reassignLeavePolicyToUser(@RequestBody AssignLeavePolicyRequest assignLeavePolicyRequest,
                                                           HttpServletRequest request){
        try{
            if(leaveService.reassignLeavePolicyToUser(assignLeavePolicyRequest)){
                return new ResponseEntity<>("Account Id "+ assignLeavePolicyRequest.getAccountId()+" is assigned with new leave policy Id "+ assignLeavePolicyRequest.getNewLeavePolicyId()+". ",HttpStatus.OK);
            }
            else{
                return new ResponseEntity<>("Leave Policy Validation failed. Please check again!",HttpStatus.NOT_ACCEPTABLE);
            }
        }catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if (e instanceof LeaveApplicationValidationException) {
                logger.error("Unable to reassign leave policy. Caught Exception: " + e, new Throwable(allStackTraces));
                throw new LeaveApplicationValidationException(e.getMessage());
            }else{
                logger.error("Unable to reassign leave policy. Caught Exception: " + e, new Throwable(allStackTraces));
                return new ResponseEntity<>(e.getMessage(),HttpStatus.NOT_ACCEPTABLE);
            }

        }
    }

    @PostMapping("/createLeave")
    @Transactional
    public ResponseEntity<Object> createLeave(@RequestBody LeaveApplicationRequest leaveApplicationRequest,
                                              HttpServletRequest request){
        try{
            String accountIds = request.getHeader("accountIds");
            Long leaveApplicationId=null;
            if(accountIds.contains(leaveApplicationRequest.getAccountId().toString())){
                if (leaveService.noPreviousLeaveApplication(leaveApplicationRequest)) {
                    if (leaveService.validateLeaveApplication(leaveApplicationRequest)) {
                        leaveApplicationId=leaveService.applyLeaveApplication(leaveApplicationRequest);
                    }
                    else {
                        return new ResponseEntity<>("Leave Application Validation failed. Please check again!",HttpStatus.NOT_ACCEPTABLE);
                    }
                } else {
                    return new ResponseEntity<>("Leave Application already applied for given date",HttpStatus.NOT_ACCEPTABLE);
                }

            }
            else{
                return new ResponseEntity<>("This accountId does not belongs to this user. Please check again!",HttpStatus.UNAUTHORIZED);
            }
            return new ResponseEntity<>(leaveService.getLeaveApplication(leaveApplicationId),HttpStatus.OK);
        }catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if (e instanceof LeaveApplicationValidationException) {
                logger.error("Unable to create leave application. Caught Exception: " + e, new Throwable(allStackTraces));
                throw new LeaveApplicationValidationException(e.getMessage());
            } else {
                logger.error("Unable to create leave application. Caught Exception: " + e, new Throwable(allStackTraces));
                return new ResponseEntity<>(e.getMessage(),HttpStatus.NOT_ACCEPTABLE);
            }
        }
    }

    @PostMapping("/updateLeave")
    @Transactional
    public ResponseEntity<Object> updateLeave(@RequestBody LeaveApplicationRequest leaveApplicationRequest,
                                              HttpServletRequest request){
        try{
            String accountIds = request.getHeader("accountIds");
            Long leaveApplicationId=null;
            if(accountIds.contains(leaveApplicationRequest.getAccountId().toString())){
                if (leaveService.noPreviousLeaveApplication(leaveApplicationRequest)) {
                    if(leaveApplicationRepository.existsById(leaveApplicationRequest.getLeaveApplicationId())
                            && leaveService.validateLeaveApplication(leaveApplicationRequest)
                            && (Objects.equals(leaveApplicationRepository.findByLeaveApplicationId(leaveApplicationRequest.getLeaveApplicationId()).getLeaveApplicationStatusId(), Constants.Leave.WAITING_APPROVAL_LEAVE_APPLICATION_STATUS_ID)||Objects.equals(leaveApplicationRepository.findByLeaveApplicationId(leaveApplicationRequest.getLeaveApplicationId()).getLeaveApplicationStatusId(), Constants.Leave.WAITING_CANCEL_LEAVE_APPLICATION_STATUS_ID))){
                        leaveApplicationId=leaveService.updateLeaveApplication(leaveApplicationRequest);
                    }
                    else {
                        return new ResponseEntity<>("Leave Application Validation failed. Please check again!",HttpStatus.NOT_ACCEPTABLE);
                    }
                } else {
                    return new ResponseEntity<>("Leave Application already applied for given date",HttpStatus.NOT_ACCEPTABLE);
                }
            }
            else{
                return new ResponseEntity<>("This accountId does not belongs to this user. Please check again!",HttpStatus.UNAUTHORIZED);
            }
            return new ResponseEntity<>(leaveService.getLeaveApplication(leaveApplicationId),HttpStatus.OK);
        }catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if(e instanceof LeaveApplicationValidationException){
                logger.error(e.getMessage()+" Caught Error: " + e, new Throwable(allStackTraces));
                throw new LeaveApplicationValidationException(e.getMessage());
            }
            logger.error("Unable to update leave application. Caught Exception: " + e, new Throwable(allStackTraces));
            return new ResponseEntity<>(e.getMessage(),HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @PostMapping("/getLeaveApplication")
    public ResponseEntity<Object> getLeaveApplication(HttpServletRequest request){
        try{
            String accountIds =request.getHeader("accountIds");
            Long leaveApplicationId = Long.valueOf(request.getHeader("leaveApplicationId"));

            LeaveApplicationResponse response= null;
            if(leaveApplicationRepository.existsById(leaveApplicationId)){

                response=leaveService.getLeaveApplication(leaveApplicationId);
            }
            else{
                return new ResponseEntity<>("This leaveApplicationId does not exists. Please check again!",HttpStatus.NOT_FOUND);
            }

            return new ResponseEntity<>(response,HttpStatus.OK);
        }
        catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to get leave application with leaveApplicationId= "+request.getHeader("leaveApplicationId")+". Caught Exception: " + e, new Throwable(allStackTraces));
            return new ResponseEntity<>(e.getMessage(),HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @PostMapping("/getLeaveHistory")
    public ResponseEntity<Object> getLeaveHistory(
            @RequestBody LeaveHistoryRequest leaveHistoryRequest,
            HttpServletRequest request) {
        try{
            String accountIds =request.getHeader("accountIds");
            Long accountId = leaveHistoryRequest.getAccountId();

            List<LeaveApplicationResponse> response= null;
            if(accountIds.contains(accountId.toString())){

                response=leaveService.getLeaveHistory(leaveHistoryRequest);
            }
            else{
                return new ResponseEntity<>("This accountId does not belongs to this user. Please check again!",HttpStatus.UNAUTHORIZED);
            }
            return new ResponseEntity<>(response,HttpStatus.OK);
        }
        catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to get leave history for user. Caught Exception: " + e, new Throwable(allStackTraces));
            return new ResponseEntity<>(e.getMessage(),HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @PostMapping("/getDoctorCertificate")
    public ResponseEntity<Object> getDoctorCertificate(
            HttpServletRequest request) {
        try{
            Long applicationId =Long.valueOf(request.getHeader("applicationId"));

            DoctorCertificateMetaData response= null;
            if(leaveApplicationRepository.existsById(applicationId)){
                response = leaveService.getDoctorCertificate(applicationId);
            }
            else{
                return new ResponseEntity<>("This applicationId does not exists. Please check again!",HttpStatus.NOT_ACCEPTABLE);
            }
            return new ResponseEntity<>(response,HttpStatus.OK);
        }
        catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to get Doctor Certificate. Caught Exception: " + e, new Throwable(allStackTraces));
            return new ResponseEntity<>(e.getMessage(),HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @PostMapping("/applicationStatus")
    public ResponseEntity<Object> applicationStatus(
            HttpServletRequest request) {
        try{
            String accountIds =request.getHeader("accountIds");
            Long accountId = Long.valueOf(request.getHeader("accountId"));

            List<LeaveApplicationResponse> response= null;
            if(accountIds.contains(accountId.toString())){
                response=leaveService.applicationStatus(accountId);
            }
            else{
                return new ResponseEntity<>("This accountId does not belongs to this user. Please check again!",HttpStatus.UNAUTHORIZED);
            }
            return new ResponseEntity<>(response,HttpStatus.OK);
        }
        catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to get application status. Caught Exception: " + e, new Throwable(allStackTraces));
            return new ResponseEntity<>(e.getMessage(),HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @PostMapping("/getLeavesByFilter")
    public ResponseEntity<Object> getLeavesByFilter(
            @RequestBody LeaveWithFilterRequest leaveWithFilterRequest,
            HttpServletRequest request) {
        try{
            AllLeavesByFilterResponse response = leaveService.getLeavesByFilter(leaveWithFilterRequest);
            return new ResponseEntity<>(response,HttpStatus.OK);
        }
        catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to get leaves. Caught Exception: " + e, new Throwable(allStackTraces));
            return new ResponseEntity<>(e.getMessage(),HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @PostMapping("/changeLeaveStatus")
    @Transactional
    public ResponseEntity<Object> changeLeaveStatus(
            @RequestBody ChangeLeaveStatusRequest changeLeaveStatusRequest,
            HttpServletRequest request) {
        try{
            String accountIds =request.getHeader("accountIds");
            Long accountId = changeLeaveStatusRequest.getAccountId();

            LeaveApplication leaveApplication = leaveApplicationRepository.findByLeaveApplicationId(changeLeaveStatusRequest.getApplicationId());
            if(Objects.equals(changeLeaveStatusRequest.getIsSprintCapacityAdjustment(),true)) {
                leaveApplication.setIsSprintCapacityAdjustment(changeLeaveStatusRequest.getIsSprintCapacityAdjustment());
            }
            if(accountIds.contains(accountId.toString()) && Objects.equals(leaveApplication.getApproverAccountId(), accountId)){
                if(!leaveService.changeLeaveStatus(changeLeaveStatusRequest, leaveApplication)){
                    return new ResponseEntity<>("Operation not allowed!",HttpStatus.FORBIDDEN);
                }
            }
            else{
                return new ResponseEntity<>("AccountId "+changeLeaveStatusRequest.getAccountId()+" is not authorized to change application status of application with applicationId "+changeLeaveStatusRequest.getApplicationId()+" .",HttpStatus.UNAUTHORIZED);
            }

            LeaveApplicationNotificationRequest leaveApplicationNotificationRequest = new LeaveApplicationNotificationRequest();
            if(Objects.equals(leaveApplication.getLeaveApplicationStatusId(), Constants.Leave.APPROVED_LEAVE_APPLICATION_STATUS_ID)
                    || Objects.equals(leaveApplication.getLeaveApplicationStatusId(), Constants.Leave.CONSUMED_LEAVE_APPLICATION_STATUS_ID)
                    || Objects.equals(leaveApplication.getLeaveApplicationStatusId(), Constants.Leave.REJECTED_LEAVE_APPLICATION_STATUS_ID)
                    || Objects.equals(leaveApplication.getLeaveApplicationStatusId(), Constants.Leave.CANCELLED_AFTER_APPROVAL_LEAVE_APPLICATION_STATUS_ID)) {
                leaveApplicationNotificationRequest.setSendNotification(true);
                leaveApplicationNotificationRequest.setLeaveApplicationId(leaveApplication.getLeaveApplicationId());
                leaveApplicationNotificationRequest.setIsSprintCapacityAdjustment(leaveApplication.getIsSprintCapacityAdjustment());
                if(Objects.equals(leaveApplication.getLeaveApplicationStatusId(), Constants.Leave.CONSUMED_LEAVE_APPLICATION_STATUS_ID))
                    leaveApplicationNotificationRequest.setNotificationFor(leaveApplicationStatusRepository.findByLeaveApplicationStatusId(Constants.Leave.APPROVED_LEAVE_APPLICATION_STATUS_ID).getLeaveApplicationStatus());
                else
                    leaveApplicationNotificationRequest.setNotificationFor(leaveApplicationStatusRepository.findByLeaveApplicationStatusId(leaveApplication.getLeaveApplicationStatusId()).getLeaveApplicationStatus());
                if(leaveApplication.getNotifyTo()!=null){
                    if(leaveApplication.getNotifyTo().length() >2){
                        int l = leaveApplication.getNotifyTo().length();
                        List<Long> notifyTo = Stream.of(leaveApplication.getNotifyTo().substring(1, l - 1).split(", "))
                                .map(Long::parseLong)
                                .collect(Collectors.toList());
                        notifyTo.add(leaveApplication.getAccountId());
                        leaveApplicationNotificationRequest.setNotifyTo(notifyTo);
                    }else{
                        leaveApplicationNotificationRequest.setNotifyTo(Collections.emptyList());
                    }

                }
            }
            else {
                leaveApplicationNotificationRequest.setSendNotification(false);
            }
            leaveApplicationNotificationRequest.setFromDate(leaveApplication.getFromDate().toString());
            leaveApplicationNotificationRequest.setToDate(leaveApplication.getToDate().toString());
            leaveApplicationNotificationRequest.setApplicantAccountId(leaveApplication.getAccountId());
            leaveApplicationNotificationRequest.setApproverAccountId(leaveApplication.getApproverAccountId());
            return new ResponseEntity<>(leaveApplicationNotificationRequest,HttpStatus.OK);
        }
        catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to change leave status. Caught Exception: " + e, new Throwable(allStackTraces));
            return new ResponseEntity<>(e.getMessage(),HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @PostMapping("/cancelLeaveApplication")
    public ResponseEntity<Object> cancelLeaveApplication(
            HttpServletRequest request) {
        try{
            Long applicationId =Long.valueOf(request.getHeader("applicationId"));
            Long accountId = Long.valueOf(request.getHeader("accountIds"));
            String leaveCancellationReason = request.getHeader("cancellationReason");
            String timeZone = request.getHeader("timeZone");
            if(leaveApplicationRepository.existsById(applicationId)){
                LeaveApplicationNotificationRequest leaveApplicationNotificationRequest = leaveService.cancelLeaveApplication(applicationId, accountId, timeZone, leaveCancellationReason);
                return new ResponseEntity<>(leaveApplicationNotificationRequest,HttpStatus.OK);
            }
            else{
                return new ResponseEntity<>("This applicationId does not exists. Please check again!",HttpStatus.NOT_ACCEPTABLE);
            }
        }
        catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if (e instanceof HttpClientErrorException.Forbidden) {
                logger.error(e.getMessage() + e, new Throwable(allStackTraces));
                return new ResponseEntity<>(e.getMessage(),HttpStatus.FORBIDDEN);
            } else {
                logger.error("Unable to cancel leave application. Caught Exception: " + e, new Throwable(allStackTraces));
                return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_ACCEPTABLE);
            }
        }
    }

    @PostMapping("/getTeamLeaveHistory")
    public ResponseEntity<Object> getTeamLeaveHistory(
            @RequestBody TeamLeaveHistoryRequest teamLeaveHistoryRequest,
            HttpServletRequest request){
        try{
            String accountIds =request.getHeader("accountIds");
            Long accountId = teamLeaveHistoryRequest.getAccountId();
            List<LeaveApplicationResponse> response= null;
            response=leaveService.getTeamLeaveHistory(teamLeaveHistoryRequest);
            return new ResponseEntity<>(response,HttpStatus.OK);
        }
        catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to get team leave history. Caught Exception: " + e, new Throwable(allStackTraces));
            return new ResponseEntity<>(e.getMessage(),HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @PostMapping("/getLeavesRemaining")
    public ResponseEntity<Object> getLeavesRemaining(
            @RequestBody LeaveRemainingHistoryRequest leaveRemainingHistoryRequest,
            HttpServletRequest request) {
        try{
            String accountIds =request.getHeader("accountIds");
            Long accountId = leaveRemainingHistoryRequest.getAccountId();


            List<LeaveRemainingResponse> response= null;
            if(userAccountRepository.existsById(accountId)){
                response=leaveService.getLeavesRemaining(leaveRemainingHistoryRequest);
            }
            else{
                return new ResponseEntity<>("This accountId does not belongs to this user. Please check again!",HttpStatus.UNAUTHORIZED);
            }
            return new ResponseEntity<>(response,HttpStatus.OK);
        }
        catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to get leaves remaining. Caught Exception: " + e, new Throwable(allStackTraces));
            return new ResponseEntity<>(e.getMessage(),HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @PostMapping("/getApprovedLeaves")
    public ResponseEntity<Object> getApprovedLeaves(
            HttpServletRequest request) {
        try{
            String accountIds =request.getHeader("accountIds");
            String accountId=request.getHeader("accountId");


            List<LeaveApplicationResponse> response= null;
            if(userAccountRepository.existsById(Long.valueOf(accountId))){
                response=leaveService.approvedLeaves(Long.valueOf(accountId));
            }
            else{
                return new ResponseEntity<>("This accountId does not belongs to this user. Please check again!",HttpStatus.UNAUTHORIZED);
            }
            return new ResponseEntity<>(response,HttpStatus.OK);
        }
        catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to get approved leaves. Caught Exception: " + e, new Throwable(allStackTraces));
            return new ResponseEntity<>(e.getMessage(),HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @PostMapping("/getTeamMembersOnLeave")
    public ResponseEntity<Object> getTeamMembersOnLeave(
            @RequestBody TeamMemberOnLeaveRequest teamMemberOnLeaveRequest,
            HttpServletRequest request){
        try{
            String accountIds =request.getHeader("accountIds");
            List<LeaveApplicationResponse> response= null;
            response=leaveService.getTeamMembersOnLeave(teamMemberOnLeaveRequest);
            return new ResponseEntity<>(response,HttpStatus.OK);
        }
        catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to get team members on leave. Caught Exception: " + e, new Throwable(allStackTraces));
            return new ResponseEntity<>(e.getMessage(),HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @PostMapping("/assignLeavePolicyToAllUser")
    @Transactional
    public ResponseEntity<Object> assignLeavePolicyToAllUser(@RequestBody AssignLeavePolicyInBulkRequest assignLeavePolicyRequest,
                                                          HttpServletRequest request){
        try{
            leaveService.assignLeavePolicyToAllUser(assignLeavePolicyRequest);
            return new ResponseEntity<>("Leave policy assigned to the provided users. This leave policy will be effective from the 1st of the current month", HttpStatus.OK);
        }catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            if (e instanceof LeaveApplicationValidationException) {
                logger.error("Unable to assign leave policy Caught Exception: " + e, new Throwable(allStackTraces));
                throw new LeaveApplicationValidationException(e.getMessage());
            }else{
                logger.error("Unable to assign leave policy. Caught Exception: " + e, new Throwable(allStackTraces));
                return new ResponseEntity<>(e.getMessage(),HttpStatus.NOT_ACCEPTABLE);
            }

        }
    }

    @PostMapping("/getPeopleOnLeave")
    public ResponseEntity<Object> getPeopleOnLeave(
            @RequestBody DateRequest todayDate,
            HttpServletRequest request){
        try{
            String accountIds =request.getHeader("accountIds");
            Integer entityTypeId = Integer.valueOf(request.getHeader("entityTypeId"));
            Long entityId = Long.valueOf(request.getHeader("entityId"));
            List<PeopleOnLeaveResponse> response= null;
            response=leaveService.getPeopleOnLeave(entityTypeId, entityId, todayDate.getTodaysDate().toLocalDate(), accountIds);
            return new ResponseEntity<>(response,HttpStatus.OK);
        }
        catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to get people on leave. Caught Exception: " + e, new Throwable(allStackTraces));
            return new ResponseEntity<>(e.getMessage(),HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @PostMapping("/getEntityLeaveReport")
    public ResponseEntity<Object> getEntityLeaveReport(
            @RequestBody EntityLeaveReportRequest entityLeaveReportRequest,
            HttpServletRequest request) {
        try {
            String accountIds = request.getHeader("accountIds");
            EntityLeaveReportResponse response = leaveService.getEntityLeaveReport(entityLeaveReportRequest, accountIds);
            response.setTotalLeaveReport(response.getMemberLeaveReportList().size());
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to get people on leave. Caught Exception: " + e, new Throwable(allStackTraces));
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @PostMapping("/getAllUsersPolicyReport")
    public ResponseEntity<Object> getAllUsersPolicyReport(
            HttpServletRequest request){
        try{
            String accountIds =request.getHeader("accountIds");
            Integer entityTypeId = Integer.valueOf(request.getHeader("entityTypeId"));
            Long entityId = Long.valueOf(request.getHeader("entityId"));
            Integer pageNumber = Integer.valueOf(request.getHeader("pageNumber"));
            Integer pageSize = Integer.valueOf(request.getHeader("pageSize"));
            AllUserPolicyReportResponse response = null;
            response = leaveService.getUserPolicyReport(entityTypeId, entityId, pageNumber, pageSize, accountIds);
            return new ResponseEntity<>(response,HttpStatus.OK);
        }
        catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to get users policy reports. Caught Exception: " + e, new Throwable(allStackTraces));
            return new ResponseEntity<>(e.getMessage(),HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @PostMapping("/updateUserPolicy")
    public ResponseEntity<Object> updateUserPolicy(
            @RequestBody @Valid UpdateLeavePolicyForUsersRequest updateLeavePolicyForUsersRequest,
            HttpServletRequest request){
        try{
            String accountIds = request.getHeader("accountIds");
            UserLeaveReportResponse response = null;
            response = leaveService.updateUserPolicy(updateLeavePolicyForUsersRequest);
            return new ResponseEntity<>(response,HttpStatus.OK);
        }
        catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to update user policy. Caught Exception: " + e, new Throwable(allStackTraces));
            return new ResponseEntity<>(e.getMessage(),HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @PostMapping("/getUpcomingLeavesCount")
    public ResponseEntity<Object> getUpcomingLeavesCount(
            @RequestBody LeaveWithFilterRequest leaveWithFilterRequest,
            HttpServletRequest request){
        try{
            Float response = 0F;
            response += leaveService.getUpcomingLeavesCount(leaveWithFilterRequest);
            return new ResponseEntity<>(response,HttpStatus.OK);
        }
        catch (Exception e){
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to get upcoming leaves count. Caught Exception: " + e, new Throwable(allStackTraces));
            return new ResponseEntity<>(e.getMessage(),HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @PostMapping("/getUserLeaveDetails")
    public ResponseEntity<Object> getUserLeaveDetails(
            @RequestBody LeaveApplicationDetailsRequest leaveApplicationDetailsRequest,
            HttpServletRequest request) {
        try {
            String accountIds = request.getHeader("accountIds");
            Integer pageNumber = Integer.valueOf(request.getHeader("pageNumber"));
            Integer pageSize = Integer.valueOf(request.getHeader("pageSize"));
            String timeZone = request.getHeader("timeZone");

            GetUsersLeaveDetailsResponse response = leaveService.getLeaveDetails(leaveApplicationDetailsRequest, pageNumber, pageSize, accountIds, timeZone);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Unable to get people on leave. Caught Exception: " + e, new Throwable(allStackTraces));
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_ACCEPTABLE);
        }
    }
}



