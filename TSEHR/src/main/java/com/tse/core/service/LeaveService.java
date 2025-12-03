package com.tse.core.service;

import com.sun.jdi.request.InvalidRequestStateException;
import com.tse.core.constants.RoleEnum;
import com.tse.core.custom.model.*;
import com.tse.core.dto.DoctorCertificate;
import com.tse.core.dto.leave.DoctorCertificateMetaData;
import com.tse.core.dto.leave.Request.*;
import com.tse.core.dto.leave.Response.*;
import com.tse.core.exception.LeaveApplicationValidationException;
import com.tse.core.exception.ValidationFailedException;
import com.tse.core.model.Constants;
import com.tse.core.model.TimeSheet;
import com.tse.core.model.leave.LeaveApplication;
import com.tse.core.model.leave.LeavePolicy;
import com.tse.core.model.leave.LeaveRemaining;
import com.tse.core.model.leave.LeaveType;
import com.tse.core.model.supplements.EntityPreference;
import com.tse.core.model.supplements.Team;
import com.tse.core.model.supplements.User;
import com.tse.core.model.supplements.UserAccount;
import com.tse.core.repository.TimeSheetRepository;
import com.tse.core.repository.supplements.BURepository;
import com.tse.core.repository.supplements.OrganizationRepository;
import com.tse.core.repository.supplements.ProjectRepository;
import com.tse.core.repository.supplements.TeamRepository;
import com.tse.core.repository.supplements.*;
import com.tse.core.repository.leaves.*;
import com.tse.core.utils.CommonUtils;
import com.tse.core.utils.DateTimeUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.web.server.NotAcceptableStatusException;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.Query;
import java.io.IOException;
import java.text.ParseException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tse.core.model.Constants.Leave.*;
import static java.lang.Math.min;

@Service
public class LeaveService {

    @Autowired
    OrganizationRepository organizationRepository;

    @Autowired
    AccessDomainRepository accessDomainRepository;
    @Autowired
    BURepository buRepository;
    @Autowired
    ProjectRepository projectRepository;
    @Autowired
    TeamRepository teamRepository;
    @Autowired
    LeavePolicyRepository leavePolicyRepository;
    @Autowired
    UserAccountRepository userAccountRepository;
    @Autowired
    LeaveRemainingRepository leaveRemainingRepository;
    @Autowired
    LeaveTypeRepository leaveTypeRepository;
    @Autowired
    LeaveApplicationStatusRepository leaveApplicationStatusRepository;
    @Autowired
    LeaveApplicationRepository leaveApplicationRepository;

    @Autowired
    TimeSheetService timeSheetService;
    @Autowired
    CalendarDaysRepository calendarDaysRepository;
    @Autowired
    TimeSheetRepository timeSheetRepository;
    @Autowired
    EntityPreferenceService entityPreferenceService;
    @Autowired
    EntityPreferenceRepository entityPreferenceRepository;

    @Autowired
    EntityManager entityManager;

    /**
     * set a default leave policy to a newly registered user
     */
    public void defaultLeavePolicyAssignment(Long accountId, Long orgId, Boolean isNewOrg) {
        if(isNewOrg){
            createLeavePolicyForLeaveType(orgId);
        }
        int numberOfMonthPassed = getMonthsBeforeCurrent();
        List<LeavePolicy> leavePolicyList = leavePolicyRepository.findByOrgIdBuIdProjectIdAndTeamId(orgId);
        for(LeavePolicy leavePolicy: leavePolicyList){
            if (leaveRemainingRepository.findByAccountIdAndLeaveType(accountId, leavePolicy.getLeaveTypeId()) != null) {
                continue;
            }
            LeaveRemaining leaveRemaining = new LeaveRemaining();
            leaveRemaining.setAccountId(accountId);
            leaveRemaining.setLeavePolicyId(leavePolicy.getLeavePolicyId());
            leaveRemaining.setLeaveTypeId(leavePolicy.getLeaveTypeId());
            if (!entityPreferenceService.getIsMonthlyLeaveUpdateOnProRata(leavePolicy.getTeamId(), leavePolicy.getOrgId())) {
                if (12 - numberOfMonthPassed > 0) {
                    leaveRemaining.setLeaveRemaining(leavePolicy.getInitialLeaves()*(12 - numberOfMonthPassed) / 12);
                }
            }
            else {
                leaveRemaining.setLeaveRemaining(0f);
            }
            leaveRemaining.setLeaveTaken(0f);
            leaveRemaining.setCalenderYear((short) LocalDate.now().getYear());
            leaveRemaining.setCurrentlyActive(true);
            leaveRemainingRepository.save(leaveRemaining);
        }
    }

    /**
     * create a new leave policy for an organization for all leave types
     */
    public void createLeavePolicyForLeaveType(Long orgId){
        List<LeaveType> leaveTypeList = leaveTypeRepository.findAll();
        for(LeaveType leaveType: leaveTypeList) {
            LeavePolicy leavePolicy = new LeavePolicy();
            leavePolicy.setLeaveTypeId(leaveType.getLeaveTypeId());
            leavePolicy.setOrgId(orgId);
            leavePolicy.setBuId(null);
            leavePolicy.setProjectId(null);
            leavePolicy.setTeamId(null);
            leavePolicy.setLeavePolicyTitle(DEFAULT_LEAVE_POLICY_TITLE);
            leavePolicy.setInitialLeaves(DEFAULT_INITIAL_LEAVES);
            leavePolicy.setIsLeaveCarryForward(DEFAULT_IS_LEAVE_CARRY_FORWARD);
            leavePolicy.setMaxLeaveCarryForward(DEFAULT_MAX_LEAVE_CARRY_FORWARD);
            leavePolicy.setCreatedByAccountId(null);
            leavePolicy.setIncludeNonBusinessDaysInLeave(false);
            leavePolicy.setIsNegativeLeaveAllowed(null);
            leavePolicy.setMaxNegativeLeaves(null);
            leavePolicyRepository.save(leavePolicy);
        }

    }

    /**
     * check for valid leave policy request
     */
    public boolean validateLeavePolicyRequest(LeavePolicyRequest leavePolicyRequest,Long leavePolicyId) {
        boolean validateLeavePolicyId,validateOrgId,validateBuId,validateProjectId,validateTeamId,validateLeaveTypeId;
        if(leavePolicyId!=null){
            validateLeavePolicyId=leavePolicyRepository.existsById(leavePolicyId);
            if(!validateLeavePolicyId){
                throw new NotAcceptableStatusException("Leave Policy does not exists.");
            }
        }
        if (leavePolicyRequest.getIsNegativeLeaveAllowed() != null && leavePolicyRequest.getIsNegativeLeaveAllowed() && leavePolicyRequest.getMaxNegativeLeaves() == null) {
            throw new IllegalStateException("Negative leave allowance is enabled, but the maximum allowed negative leaves count is not specified");
        }
        if(leavePolicyRequest.getIsLeaveCarryForward() != null && leavePolicyRequest.getIsLeaveCarryForward()){
            if(leavePolicyRequest.getMaxLeaveCarryForward() == null || leavePolicyRequest.getMaxLeaveCarryForward() == 0){
                throw new ValidationFailedException("Max leave carry forward can not be null or 0 if leave carry forward is true.");
            }
        }
        validateOrgId = organizationRepository.existsById(leavePolicyRequest.getOrgId());
        if(leavePolicyRequest.getBuId()!=null)
            validateBuId = buRepository.existsById(leavePolicyRequest.getBuId());
        else
            validateBuId=true;
        if(leavePolicyRequest.getProjectId()!=null)
            validateProjectId = projectRepository.existsById(leavePolicyRequest.getProjectId());
        else
            validateProjectId=true;
        if(leavePolicyRequest.getTeamId()!=null)
            validateTeamId = teamRepository.existsById(leavePolicyRequest.getTeamId());
        else
            validateTeamId=true;
        validateLeaveTypeId=leaveTypeRepository.existsById(leavePolicyRequest.getLeaveTypeId());
        return (validateOrgId && validateBuId && validateProjectId && validateTeamId && validateLeaveTypeId);
    }

    /**
     * add a new leave policy
     */
    public Long addLeavePolicy(LeavePolicyRequest leavePolicyRequest) {
        LeavePolicy leavePolicy= new LeavePolicy();
        leavePolicy.setLeaveTypeId(leavePolicyRequest.getLeaveTypeId());
        leavePolicy.setOrgId(leavePolicyRequest.getOrgId());
        leavePolicy.setBuId(leavePolicyRequest.getBuId());
        leavePolicy.setProjectId(leavePolicyRequest.getProjectId());
        leavePolicy.setTeamId(leavePolicyRequest.getTeamId());
        if (leavePolicyRequest.getLeavePolicyTitle() != null) {
            leavePolicy.setLeavePolicyTitle(leavePolicyRequest.getLeavePolicyTitle());
        } else {
            leavePolicy.setLeavePolicyTitle(leaveTypeRepository.findByLeaveTypeId(leavePolicy.getLeaveTypeId()).getLeaveType());
        }
        leavePolicy.setInitialLeaves(leavePolicyRequest.getInitialLeaves());
        leavePolicy.setIsLeaveCarryForward(leavePolicyRequest.getIsLeaveCarryForward());
        leavePolicy.setMaxLeaveCarryForward((leavePolicyRequest.getIsLeaveCarryForward())?leavePolicyRequest.getMaxLeaveCarryForward():DEFAULT_MAX_LEAVE_CARRY_FORWARD);
        leavePolicy.setCreatedByAccountId(leavePolicyRequest.getAccountId());
        leavePolicy.setIncludeNonBusinessDaysInLeave(leavePolicyRequest.getIncludeNonBusinessDaysInLeave());
        leavePolicy.setIsNegativeLeaveAllowed(leavePolicyRequest.getIsNegativeLeaveAllowed());
        leavePolicy.setMaxNegativeLeaves(leavePolicyRequest.getMaxNegativeLeaves());
        return leavePolicyRepository.save(leavePolicy).getLeavePolicyId();
    }

    /**
     * update the existing leave policy
     */
    public void updateLeavePolicy(LeavePolicyRequest leavePolicyRequest, Long leavePolicyId) {
        LeavePolicy leavePolicyDb = leavePolicyRepository.findByLeavePolicyId(leavePolicyId);
        List<LeaveRemaining> leaveRemainingList = new ArrayList<>();
        //Leave updates
        if (!Objects.equals(leavePolicyDb.getInitialLeaves(), leavePolicyRequest.getInitialLeaves())) {
            int numberOfMonthPassed = getMonthsBeforeCurrent();
            List<Long> usersAccountIdListWithPolicy = leaveRemainingRepository.findAllByLeavePolicyId(leavePolicyDb.getLeavePolicyId());
            for (Long accountId : usersAccountIdListWithPolicy) {
                LeaveRemaining leaveRemaining = leaveRemainingRepository.findByAccountIdAndLeaveType(accountId, leavePolicyDb.getLeaveTypeId());
                if (!entityPreferenceService.getIsMonthlyLeaveUpdateOnProRata(leavePolicyDb.getTeamId(), leavePolicyDb.getOrgId())) {
                    if (leaveRemaining.getLeaveRemaining() > leavePolicyDb.getInitialLeaves()) {
                        leaveRemaining.setLeaveRemaining(leaveRemaining.getLeaveRemaining() - leavePolicyDb.getInitialLeaves());
                    } else {
                        if (leaveRemaining.getLeaveRemaining() > 0f) {
                            leaveRemaining.setLeaveRemaining(0f);
                        }
                    }
                    if (numberOfMonthPassed > 0) {
                        leaveRemaining.setLeaveRemaining(leaveRemaining.getLeaveRemaining() + (leavePolicyDb.getInitialLeaves() * numberOfMonthPassed) / 12);
                    }
                    if (12 - numberOfMonthPassed > 0) {
                        leaveRemaining.setLeaveRemaining(leaveRemaining.getLeaveRemaining() + (leavePolicyRequest.getInitialLeaves() * (12 - numberOfMonthPassed)) / 12);
                    }
                }
                leaveRemaining.setCalenderYear((short) LocalDate.now().getYear());
                leaveRemainingList.add(leaveRemaining);
            }
            leavePolicyDb.setInitialLeaves(leavePolicyRequest.getInitialLeaves());
        }
        if (leavePolicyDb.getIsLeaveCarryForward() != leavePolicyRequest.getIsLeaveCarryForward()) {
            leavePolicyDb.setIsLeaveCarryForward(leavePolicyRequest.getIsLeaveCarryForward());
        }
        if (leavePolicyRequest.getIsLeaveCarryForward() != null && leavePolicyRequest.getIsLeaveCarryForward() && !Objects.equals(leavePolicyDb.getMaxLeaveCarryForward(), leavePolicyRequest.getMaxLeaveCarryForward())) {
            leavePolicyDb.setMaxLeaveCarryForward(leavePolicyRequest.getMaxLeaveCarryForward());
        }
        if (leavePolicyRequest.getIsLeaveCarryForward() == null || !leavePolicyRequest.getIsLeaveCarryForward()) {
            leavePolicyDb.setMaxLeaveCarryForward(0f);
        }
        if (Boolean.TRUE.equals(leavePolicyRequest.getIsNegativeLeaveAllowed())
                && leavePolicyRequest.getMaxNegativeLeaves() != null) {
            if (leavePolicyRequest.getMaxNegativeLeaves() > 0) {
                leavePolicyDb.setIsNegativeLeaveAllowed(true);
                leavePolicyDb.setMaxNegativeLeaves(leavePolicyRequest.getMaxNegativeLeaves());
            } else
                throw new ValidationFailedException("Maximum Negative Allowance cannot be 0");
        } else {
            leavePolicyDb.setIsNegativeLeaveAllowed(false);
            leavePolicyDb.setMaxNegativeLeaves(0f);
        }
        //Updated by
        if (!Objects.equals(leavePolicyDb.getCreatedByAccountId(), leavePolicyRequest.getAccountId())) {
            leavePolicyDb.setLastUpdatedByAccountId(leavePolicyRequest.getAccountId());
        }
        if (!Objects.equals(leavePolicyDb.getIncludeNonBusinessDaysInLeave(), leavePolicyRequest.getIncludeNonBusinessDaysInLeave())) {
            leavePolicyDb.setIncludeNonBusinessDaysInLeave(leavePolicyRequest.getIncludeNonBusinessDaysInLeave());
        }
        if (!leaveRemainingList.isEmpty()) {
            leaveRemainingRepository.saveAll(leaveRemainingList);
        }
        leavePolicyRepository.save(leavePolicyDb);
    }

    /**
     * This method get all leave policies for an organization
     */
    public List<LeavePolicyResponse> getOrgLeavePolicy(Long orgId, String timeZone) {
        List<LeavePolicy> leavePolicyList=leavePolicyRepository.findByOrgId(orgId);
        List<LeavePolicyResponse> leavePolicyResponseList = new ArrayList<>();
        LeaveTypeAlias leaveTypeAlias = entityPreferenceRepository.findLeaveTypeAliasForEntity(Constants.EntityTypes.ORG, orgId);
        for(LeavePolicy leavePolicy:leavePolicyList) {
            LeavePolicyResponse leavePolicyResponse = new LeavePolicyResponse();
            leavePolicyResponse.setLeavePolicyId(leavePolicy.getLeavePolicyId());
            leavePolicyResponse.setLeaveTypeId(leavePolicy.getLeaveTypeId());
            leavePolicyResponse.setLeaveType(Objects.equals(leavePolicy.getLeaveTypeId(), TIME_OFF_LEAVE_TYPE_ID) ? leaveTypeAlias.getTimeOffAlias() : leaveTypeAlias.getSickLeaveAlias());
            leavePolicyResponse.setOrgId(leavePolicy.getOrgId());
            leavePolicyResponse.setBuId(leavePolicy.getBuId());
            leavePolicyResponse.setProjectId(leavePolicy.getProjectId());
            leavePolicyResponse.setTeamId(leavePolicy.getTeamId());
            leavePolicyResponse.setLeavePolicyTitle(leavePolicy.getLeavePolicyTitle());
            leavePolicyResponse.setInitialLeaves(leavePolicy.getInitialLeaves());
            leavePolicyResponse.setIsLeaveCarryForward(leavePolicy.getIsLeaveCarryForward());
            leavePolicyResponse.setMaxLeaveCarryForward(leavePolicy.getMaxLeaveCarryForward());
            leavePolicyResponse.setCreatedByAccountId(leavePolicy.getCreatedByAccountId());
            leavePolicyResponse.setLastUpdatedByAccountId(leavePolicy.getLastUpdatedByAccountId());
            leavePolicyResponse.setCreatedDateTime(DateTimeUtils.convertServerDateToLocalTimezone(leavePolicy.getCreatedDateTime(), timeZone));
            leavePolicyResponse.setModifiedDateTime(DateTimeUtils.convertServerDateToLocalTimezone(leavePolicy.getModifiedDateTime(), timeZone));
            leavePolicyResponse.setIncludeNonBusinessDaysInLeave(leavePolicy.getIncludeNonBusinessDaysInLeave());
            leavePolicyResponse.setIsNegativeLeaveAllowed(leavePolicy.getIsNegativeLeaveAllowed());
            leavePolicyResponse.setMaxNegativeLeaves(leavePolicy.getMaxNegativeLeaves());
            leavePolicyResponseList.add(leavePolicyResponse);
        }
        leavePolicyResponseList.sort(Comparator.comparing(LeavePolicyResponse::getLeaveTypeId));
        return leavePolicyResponseList;
    }

    /**
     * get leave policy assigned an accountId
     */
    public List<LeavePolicyResponse> getUsersLeavePolicy(Long accountId, String timeZone) {
        List<LeavePolicy> leavePolicyList=leavePolicyRepository.findLeavePolicyByAccountId(accountId);
        List<LeavePolicyResponse> leavePolicyResponseList = new ArrayList<>();
        for(LeavePolicy leavePolicy:leavePolicyList) {
            LeavePolicyResponse leavePolicyResponse = new LeavePolicyResponse();
            leavePolicyResponse.setLeavePolicyId(leavePolicy.getLeavePolicyId());
            leavePolicyResponse.setLeaveTypeId(leavePolicy.getLeaveTypeId());
            leavePolicyResponse.setLeaveType(leaveTypeRepository.findByLeaveTypeId(leavePolicy.getLeaveTypeId()).getLeaveType());
            leavePolicyResponse.setOrgId(leavePolicy.getOrgId());
            leavePolicyResponse.setBuId(leavePolicy.getBuId());
            leavePolicyResponse.setProjectId(leavePolicy.getProjectId());
            leavePolicyResponse.setTeamId(leavePolicy.getTeamId());
            leavePolicyResponse.setLeavePolicyTitle(leavePolicy.getLeavePolicyTitle());
            leavePolicyResponse.setInitialLeaves(leavePolicy.getInitialLeaves());
            leavePolicyResponse.setIsLeaveCarryForward(leavePolicy.getIsLeaveCarryForward());
            leavePolicyResponse.setMaxLeaveCarryForward(leavePolicy.getMaxLeaveCarryForward());
            leavePolicyResponse.setCreatedByAccountId(leavePolicy.getCreatedByAccountId());
            leavePolicyResponse.setLastUpdatedByAccountId(leavePolicy.getLastUpdatedByAccountId());
            leavePolicyResponse.setCreatedDateTime(DateTimeUtils.convertServerDateToLocalTimezone(leavePolicy.getCreatedDateTime(), timeZone));
            leavePolicyResponse.setModifiedDateTime(DateTimeUtils.convertServerDateToLocalTimezone(leavePolicy.getModifiedDateTime(), timeZone));
            leavePolicyResponse.setIncludeNonBusinessDaysInLeave(leavePolicy.getIncludeNonBusinessDaysInLeave());
            leavePolicyResponse.setIsNegativeLeaveAllowed(leavePolicy.getIsNegativeLeaveAllowed());
            leavePolicyResponse.setMaxNegativeLeaves(leavePolicy.getMaxNegativeLeaves());
            leavePolicyResponseList.add(leavePolicyResponse);
        }
        leavePolicyResponseList.sort(Comparator.comparing(LeavePolicyResponse::getLeaveTypeId));
        return leavePolicyResponseList;
    }

    /**
     * This method validates the leave application by type and years
     */
    public boolean validateLeaveApplication(LeaveApplicationRequest leaveApplicationRequest) throws ParseException {
        Long orgId = userAccountRepository.findOrgIdByAccountId(leaveApplicationRequest.getAccountId());
        if (orgId == null) {
            throw new LeaveApplicationValidationException("User don't exists in the organization anymore.");
        }

        if(!leaveRemainingRepository.existsByAccountId(leaveApplicationRequest.getAccountId())){
            throw new LeaveApplicationValidationException("No leave policy assigned to user for current year. Please contact your admins to assign leave policy before applying for a leave");
        }

        validateDateOfLeave(leaveApplicationRequest, orgId);
        boolean validateLeaveTypeId;
        validateLeaveTypeId=Constants.leaveSelectionType.containsKey(leaveApplicationRequest.getLeaveSelectionTypeId());
        if(!validateLeaveTypeId){
            throw new LeaveApplicationValidationException("Invalid leave type");
        }
        boolean validateHalfDayLeaveTypeId;
        if(leaveApplicationRequest.getHalfDayLeaveType()!= null && HALF_DAY_LEAVES.contains(leaveApplicationRequest.getLeaveSelectionTypeId())) {
            validateHalfDayLeaveTypeId = Constants.halfDayLeaveTypeList.contains(leaveApplicationRequest.getHalfDayLeaveType());
            if(!validateHalfDayLeaveTypeId)
            {
                throw new ValidationFailedException("Invalid half day leave type");
            }
        }

        if (HALF_DAY_LEAVES.contains(leaveApplicationRequest.getLeaveSelectionTypeId()) && !Objects.equals(leaveApplicationRequest.getFromDate(), leaveApplicationRequest.getToDate())) {
            throw new LeaveApplicationValidationException("User not allowed to apply for multiple half day leaves at once");
        }

        if(Objects.equals(leaveApplicationRequest.getFromDate(),leaveApplicationRequest.getToDate())){
            if(leaveApplicationRequest.getIncludeLunchTime()==null){
                throw new LeaveApplicationValidationException("IncludeLunchTime can not be null if leave is within a day.");
            }
        }
        else{
            leaveApplicationRequest.setIncludeLunchTime(false);
        }
        LeaveTypeAlias leaveTypeAlias = entityPreferenceRepository.findLeaveTypeAliasForEntity(Constants.EntityTypes.ORG, orgId);
        float officeHours;
        //If leave type is a sick leave
        if(Constants.Leave.SICK_LEAVES.contains(leaveApplicationRequest.getLeaveSelectionTypeId())){
            LeaveRemaining leaveRemaining = leaveRemainingRepository.findByAccountIdAndLeaveTypeIdAndCalenderYear(leaveApplicationRequest.getAccountId(),Constants.Leave.SICK_LEAVE_TYPE_ID, (short) LocalDate.now().getYear());
            if (leaveRemaining == null) {
                throw new LeaveApplicationValidationException("No leave policy assigned to user for current year. Please contact your admins to assign leave policy before applying for a leave");
            }
            LeavePolicy leavePolicy = leavePolicyRepository.findByLeavePolicyId(leaveRemaining.getLeavePolicyId());
            officeHours = (float) timeSheetService.getMaxWorkingMinutes(leavePolicy.getTeamId(), orgId) / 60;
            //adding leave hours of applications in waiting for approval state
            float inlineLeaveConsumptionInHours =  previousLeaveApplicationConsumption(leaveApplicationRequest,officeHours, SICK_LEAVE_TYPE_ID)*officeHours;
            float sickLeaveRemaining = ((leavePolicy.getIsNegativeLeaveAllowed() != null && leavePolicy.getIsNegativeLeaveAllowed()) ? leaveRemaining.getLeaveRemaining() + leavePolicy.getMaxNegativeLeaves() : leaveRemaining.getLeaveRemaining())*officeHours;
            //sick leave for a day
            if ((Objects.equals(leaveApplicationRequest.getLeaveSelectionTypeId(), Constants.Leave.SICK_LEAVE_FOR_A_DAY)
                    || Objects.equals(leaveApplicationRequest.getLeaveSelectionTypeId(), HALF_DAY_SICK_LEAVE))
                    && (Objects.equals(leaveApplicationRequest.getFromDate(), leaveApplicationRequest.getToDate()))) {
                float leaveHours = Objects.equals(leaveApplicationRequest.getLeaveSelectionTypeId(), HALF_DAY_SICK_LEAVE) ? (float) (officeHours * 0.5) : officeHours;
                leaveApplicationRequest.setNumberOfLeaveDays(leaveHours/officeHours);
                //include or exclude lunch hours
                //TODO: take lunch hour from office hours table till use constant
                float breakDuration = getBreakDuration(leavePolicy.getTeamId(), orgId);
                if (leaveHours >= breakDuration) {
                    leaveHours -= breakDuration;
                }
                int currentYear = LocalDate.now().getYear();
                int toYear = leaveApplicationRequest.getToDate().getYear();
                float availableLeave;
                if (toYear == currentYear + 1) {
                    if (leavePolicy.getMaxLeaveCarryForward() == null
                            || leavePolicy.getMaxLeaveCarryForward() == 0
                            || Boolean.FALSE.equals(leavePolicy.getIsLeaveCarryForward())) {
                        throw new ValidationFailedException("Maximum leave carry forward is not available; cannot apply for leave in the next year.");
                    }
                    float nextYearLeaves=this.calculateNextYearLeaveDays(leaveApplicationRequest.getAccountId(),leaveApplicationRequest.getLeaveSelectionTypeId(),officeHours);
                    float remainCarryForward=leavePolicy.getMaxLeaveCarryForward()-nextYearLeaves;
                    if(remainCarryForward<=0)
                    {
                        throw new ValidationFailedException("Your leave request cannot processed as you dont have maximum leave carry forward to apply leave for next year");
                    }
                    availableLeave = Math.min(sickLeaveRemaining, remainCarryForward*officeHours);
                } else {
                    availableLeave = sickLeaveRemaining;
                }
                if (availableLeave < leaveHours) {
                    throw new LeaveApplicationValidationException("'" + leaveTypeAlias.getSickLeaveAlias() + "' leave remaining is lower than you applied for.");
                } else if (availableLeave < leaveHours + inlineLeaveConsumptionInHours) {
                    throw new LeaveApplicationValidationException("Your leave request cannot be processed as the requested days exceed your available '" + leaveTypeAlias.getSickLeaveAlias() + "' leave balance, including pending leave requests.");
                } else {
                    return true;
                }

            }
            //sick leave for multiple days
            else{
                float totalLeaveHours = totalLeaveHours(leaveApplicationRequest.getFromDate(), leaveApplicationRequest.getToDate(), leaveApplicationRequest.getAccountId(), SICK_LEAVE_TYPE_ID, officeHours);
                leaveApplicationRequest.setNumberOfLeaveDays(totalLeaveHours / officeHours);
                float minApprovedSickDaysWithoutMedicalCert  = (float) entityPreferenceRepository.findMinApprovedSickDaysWithoutMedicalCertByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, orgId);
                if (!Boolean.TRUE.equals(leaveApplicationRequest.getIsAttachmentPresent())) {
//                    if (leaveApplicationRequest.getDoctorCertificate() == null && minApprovedSickDaysWithoutMedicalCert < leaveApplicationRequest.getNumberOfLeaveDays()) {
//                    throw new LeaveApplicationValidationException("Providing Doctor Certificate is compulsory for more than one day sick leave.");
//                }
                }
                int currentYear = LocalDate.now().getYear();
                int toYear = leaveApplicationRequest.getToDate().getYear();
                float availableLeave;
                if (toYear == currentYear + 1) {
                    if (leavePolicy.getMaxLeaveCarryForward() == null
                            || leavePolicy.getMaxLeaveCarryForward() == 0
                            || Boolean.FALSE.equals(leavePolicy.getIsLeaveCarryForward())) {
                        throw new ValidationFailedException("Maximum leave carry forward is not available; cannot apply for leave in the next year.");
                    }
                    float nextYearLeaves=this.calculateNextYearLeaveDays(leaveApplicationRequest.getAccountId(),leaveApplicationRequest.getLeaveSelectionTypeId(),officeHours);
                    float remainCarryForward=leavePolicy.getMaxLeaveCarryForward()-nextYearLeaves;
                    if(remainCarryForward<=0)
                    {
                        throw new ValidationFailedException("Your leave request cannot processed as you don't have maximum leave carry forward to apply leave for next year");
                    }
                    availableLeave = Math.min(sickLeaveRemaining, remainCarryForward*officeHours);
                } else {
                    availableLeave = sickLeaveRemaining;
                }
                if (availableLeave < totalLeaveHours) {
                    throw new LeaveApplicationValidationException("'" + leaveTypeAlias.getSickLeaveAlias() + "' leave remaining is lower than you applied for.");
                } else if (availableLeave < totalLeaveHours + inlineLeaveConsumptionInHours) {
                    throw new LeaveApplicationValidationException("Your leave request cannot be processed as the requested days exceed your available '" + leaveTypeAlias.getSickLeaveAlias() + "' leave balance, including pending leave requests.");
                }  else {
                    return true;
                }
            }
        }
        //If leave type is a timeOff leave
        else {
            LeaveRemaining leaveRemaining=leaveRemainingRepository.findByAccountIdAndLeaveTypeIdAndCalenderYear(leaveApplicationRequest.getAccountId(),Constants.Leave.TIME_OFF_LEAVE_TYPE_ID, (short) LocalDate.now().getYear());
            if (leaveRemaining == null) {
                throw new LeaveApplicationValidationException("No leave policy assigned to user for current year. Please contact your admins to assign leave policy before applying for a leave");
            }
            if (Boolean.TRUE.equals(leaveApplicationRequest.getIsAttachmentPresent())) {
                throw new InvalidRequestStateException("Attachment can't be present in TimeOff leave");
            }
            LeavePolicy leavePolicy = leavePolicyRepository.findByLeavePolicyId(leaveRemaining.getLeavePolicyId());
            officeHours = (float) timeSheetService.getMaxWorkingMinutes(leavePolicy.getTeamId(), orgId) / 60;
            float timeOffRemaining = ((leavePolicy.getIsNegativeLeaveAllowed() != null && leavePolicy.getIsNegativeLeaveAllowed()) ? leaveRemaining.getLeaveRemaining() + leavePolicy.getMaxNegativeLeaves() : leaveRemaining.getLeaveRemaining())*officeHours;
            //adding leave hours of applications in waiting for approval state
            float inlineLeaveConsumptionInHours =  previousLeaveApplicationConsumption(leaveApplicationRequest,officeHours, TIME_OFF_LEAVE_TYPE_ID)*officeHours;
            //TimeOff for a day
            if((Objects.equals(leaveApplicationRequest.getLeaveSelectionTypeId(), Constants.Leave.LEAVE_FOR_A_DAY)
                    || Objects.equals(leaveApplicationRequest.getLeaveSelectionTypeId(), HALF_DAY_TIME_OFF_LEAVE))
                    && (Objects.equals(leaveApplicationRequest.getToDate(),leaveApplicationRequest.getFromDate()))){
                float leaveHours = Objects.equals(leaveApplicationRequest.getLeaveSelectionTypeId(), HALF_DAY_TIME_OFF_LEAVE) ? (float) (officeHours * 0.5) : officeHours;
                leaveApplicationRequest.setNumberOfLeaveDays(leaveHours/officeHours);
                //TODO: take lunch hour from office hours table till use constant
                float breakDuration = getBreakDuration(leavePolicy.getTeamId(), orgId);
                if (leaveHours >= breakDuration) {
                    leaveHours -= breakDuration;
                }
                int currentYear = LocalDate.now().getYear();
                int toYear = leaveApplicationRequest.getToDate().getYear();
                float availableLeave;
                if (toYear == currentYear + 1) {
                    if (leavePolicy.getMaxLeaveCarryForward() == null
                            || leavePolicy.getMaxLeaveCarryForward() == 0
                            || Boolean.FALSE.equals(leavePolicy.getIsLeaveCarryForward())) {
                        throw new ValidationFailedException("Maximum leave carry forward is not available; cannot apply for leave in the next year.");
                    }
                    float nextYearLeaves=this.calculateNextYearLeaveDays(leaveApplicationRequest.getAccountId(),leaveApplicationRequest.getLeaveSelectionTypeId(),officeHours);
                    float remainCarryForward=leavePolicy.getMaxLeaveCarryForward()-nextYearLeaves;
                    if(remainCarryForward<=0)
                    {
                        throw new ValidationFailedException("Your leave request cannot processed as you dont have maximum leave carry forward to apply leave for next year");
                    }
                    availableLeave = Math.min(timeOffRemaining, remainCarryForward*officeHours);
                } else {
                    availableLeave = timeOffRemaining;
                }
                if (availableLeave < leaveHours) {
                    throw new LeaveApplicationValidationException("'" + leaveTypeAlias.getTimeOffAlias() + "' leave remaining is lower than you applied for.");
                } else if (availableLeave < leaveHours + inlineLeaveConsumptionInHours) {
                    throw new LeaveApplicationValidationException("Your leave request cannot be processed as the requested days exceed your available '" + leaveTypeAlias.getTimeOffAlias() + "' leave balance, including pending leave requests.");
                } else {
                    return true;
                }
            }
            //TimeOff for multiple days
            else {
                float totalLeaveHours=totalLeaveHours(leaveApplicationRequest.getFromDate(), leaveApplicationRequest.getToDate(), leaveApplicationRequest.getAccountId(), TIME_OFF_LEAVE_TYPE_ID, officeHours);
                leaveApplicationRequest.setNumberOfLeaveDays(totalLeaveHours/officeHours);
                int currentYear = LocalDate.now().getYear();
                int toYear = leaveApplicationRequest.getToDate().getYear();
                float availableLeave;
                if (toYear == currentYear + 1) {
                    if (leavePolicy.getMaxLeaveCarryForward() == null
                            || leavePolicy.getMaxLeaveCarryForward() == 0
                            || Boolean.FALSE.equals(leavePolicy.getIsLeaveCarryForward())) {
                        throw new ValidationFailedException("Maximum leave carry forward is not available; cannot apply for leave in the next year.");
                    }
                    float nextYearLeaves=this.calculateNextYearLeaveDays(leaveApplicationRequest.getAccountId(),leaveApplicationRequest.getLeaveSelectionTypeId(),officeHours);
                    float remainCarryForward=leavePolicy.getMaxLeaveCarryForward()-nextYearLeaves;
                    if(remainCarryForward<=0)
                    {
                        throw new ValidationFailedException("Your leave request cannot processed as you dont have maximum leave carry forward to apply leave for next year");
                    }
                    availableLeave = Math.min(timeOffRemaining, remainCarryForward*officeHours);
                } else {
                    availableLeave = timeOffRemaining;
                }
                if (availableLeave < totalLeaveHours) {
                    throw new LeaveApplicationValidationException("'" + leaveTypeAlias.getTimeOffAlias() + "' leave remaining is lower than you applied for.");
                } else if (availableLeave < totalLeaveHours + inlineLeaveConsumptionInHours) {
                    throw new LeaveApplicationValidationException("Your leave request cannot be processed as the requested days exceed your available '" + leaveTypeAlias.getTimeOffAlias() + "' leave balance, including pending leave requests.");
                }  else {
                    return true;
                }
            }
        }
    }

    /**
     * This method checks if there is any leave applied for the provided days before
     */
    public boolean noPreviousLeaveApplication(LeaveApplicationRequest leaveApplicationRequest){
        List<LeaveApplication> previousLeaveApplications;
        if (Objects.equals(leaveApplicationRequest.getFromDate(),leaveApplicationRequest.getToDate())) {
            previousLeaveApplications = leaveApplicationRepository.findByAccountIdAndisLeaveForHalfDayAndDate(leaveApplicationRequest.getAccountId(), true, leaveApplicationRequest.getFromDate());
            if (previousLeaveApplications.size() == 1 && HALF_DAY_LEAVES.contains(leaveApplicationRequest.getLeaveSelectionTypeId())) {
                return true;
            }
            previousLeaveApplications = leaveApplicationRepository.findByAccountIdAndDate(leaveApplicationRequest.getAccountId(), leaveApplicationRequest.getFromDate());

            if (previousLeaveApplications.isEmpty()){
                return true;
            } else {
                for (LeaveApplication la : previousLeaveApplications){
                    if (!Objects.equals(leaveApplicationRequest.getLeaveApplicationId(),la.getLeaveApplicationId())){
                        if(Objects.equals(la.getLeaveApplicationStatusId(), CANCELLED_LEAVE_APPLICATION_STATUS_ID)
                                || Objects.equals(la.getLeaveApplicationStatusId(), REJECTED_LEAVE_APPLICATION_STATUS_ID)
                                || Objects.equals(la.getLeaveApplicationStatusId(), CANCELLED_AFTER_APPROVAL_LEAVE_APPLICATION_STATUS_ID)
                                || Objects.equals(la.getLeaveApplicationStatusId(), LEAVE_APPLICATION_EXPIRED_STATUS_ID)){
                            continue;
                        }
                        return false;
                    }
                }
            }
        } else {
            LocalDate date = leaveApplicationRequest.getFromDate();
            while (date.isBefore(leaveApplicationRequest.getToDate()) || date.isEqual(leaveApplicationRequest.getToDate())){
                previousLeaveApplications = leaveApplicationRepository.findByAccountIdAndDate(leaveApplicationRequest.getAccountId(), date);
                if (!previousLeaveApplications.isEmpty()){
                    for (LeaveApplication la : previousLeaveApplications){
                        if (!Objects.equals(leaveApplicationRequest.getLeaveApplicationId(),la.getLeaveApplicationId())){
                            if(Objects.equals(la.getLeaveApplicationStatusId(), CANCELLED_LEAVE_APPLICATION_STATUS_ID)
                                    ||Objects.equals(la.getLeaveApplicationStatusId(), REJECTED_LEAVE_APPLICATION_STATUS_ID)
                                    || Objects.equals(la.getLeaveApplicationStatusId(), CANCELLED_AFTER_APPROVAL_LEAVE_APPLICATION_STATUS_ID)
                                    || Objects.equals(la.getLeaveApplicationStatusId(), LEAVE_APPLICATION_EXPIRED_STATUS_ID)){
                                continue;
                            }
                            return false;
                        }
                    }
                }
                date = date.plusDays(1);
            }
        }

        return true;
    }

    /**
     * to get all approved leaves
     */
    public List<LeaveApplicationResponse> approvedLeaves(Long accountId){

        List<LeaveApplication> leaveApplicationList = leaveApplicationRepository.findByAccountIdAndApplicationStatusForYear(accountId , APPROVED_LEAVE_APPLICATION_STATUS_ID);
        List<LeaveApplicationResponse> leaveApplicationResponseList=new ArrayList<>();

        LeaveTypeAlias leaveTypeAlias = entityPreferenceRepository.findLeaveTypeAliasForEntity(Constants.EntityTypes.ORG, userAccountRepository.findOrgIdByAccountId(accountId));
        for(LeaveApplication leaveApplication: leaveApplicationList){
            leaveApplicationResponseList.add(getLeaveApplicationResponse(leaveApplication, leaveTypeAlias));
        }


        return leaveApplicationResponseList;
    }

    /**
     * check for leave hours consumed in pending leave applications
     */
    private float previousLeaveApplicationConsumption(LeaveApplicationRequest leaveApplicationRequest, float officeHours, Short leaveTypeId) throws ParseException {
        float totalLeave=0f;
        List<LeaveApplication> leaveApplicationList = leaveApplicationRepository.findByAccountIdAndLeaveTypeIdAndApplicationStatus(leaveApplicationRequest.getAccountId(),leaveTypeId, WAITING_APPROVAL_LEAVE_APPLICATION_STATUS_ID);
        for(LeaveApplication leaveApplication: leaveApplicationList) {
            if (leaveApplicationRequest.getLeaveApplicationId() != null && Objects.equals(leaveApplication.getLeaveApplicationId(), leaveApplicationRequest.getLeaveApplicationId())) {
                continue;
            }
            float leaveHours = officeHours;
            if (leaveApplication.getIsLeaveForHalfDay() != null && leaveApplication.getIsLeaveForHalfDay()) {
                leaveHours = officeHours * 0.5f;
            }
            totalLeave += totalLeaveHours(leaveApplication.getFromDate(), leaveApplication.getToDate(), leaveApplication.getAccountId(), leaveApplication.getLeaveTypeId(), leaveHours) / officeHours;
        }
        return totalLeave;
    }


    /**
     * create a new leave application
     */
    public Long applyLeaveApplication(LeaveApplicationRequest leaveApplicationRequest) throws IOException {

        LeaveApplication leaveApplication = new LeaveApplication();
        leaveApplication.setAccountId(leaveApplicationRequest.getAccountId());
        if(Constants.Leave.SICK_LEAVES.contains(leaveApplicationRequest.getLeaveSelectionTypeId())){
            leaveApplication.setLeaveTypeId(Constants.Leave.SICK_LEAVE_TYPE_ID);
        }
        else{
            leaveApplication.setLeaveTypeId(Constants.Leave.TIME_OFF_LEAVE_TYPE_ID);
        }
        leaveApplication.setExpiryLeaveDate(leaveApplicationRequest.getExpiryLeaveDate());
        leaveApplication.setLeaveApplicationStatusId(Constants.Leave.WAITING_APPROVAL_LEAVE_APPLICATION_STATUS_ID);
        leaveApplication.setFromDate(leaveApplicationRequest.getFromDate());
        leaveApplication.setToDate(leaveApplicationRequest.getToDate());
        leaveApplication.setIncludeLunchTime(leaveApplicationRequest.getIncludeLunchTime());
        leaveApplication.setLeaveReason(leaveApplicationRequest.getLeaveReason());
        leaveApplication.setApproverAccountId(leaveApplicationRequest.getApproverAccountId());
        leaveApplication.setPhone(leaveApplicationRequest.getPhone());
        leaveApplication.setAddress(leaveApplicationRequest.getAddress());
        if(leaveApplicationRequest.getNotifyTo()!=null){
            leaveApplication.setNotifyTo(leaveApplicationRequest.getNotifyTo().toString());
        }
        if(leaveApplicationRequest.getDoctorCertificate()!=null) {
            leaveApplicationRequest.setIsAttachmentPresent(true);
            leaveApplication.setDoctorCertificate(leaveApplicationRequest.getDoctorCertificate());
            leaveApplication.setDoctorCertificateFileName(leaveApplicationRequest.getDoctorCertificateFileName());
            leaveApplication.setDoctorCertificateFileType(leaveApplicationRequest.getDoctorCertificateFileType());
            leaveApplication.setDoctorCertificateFileSize(leaveApplicationRequest.getDoctorCertificateFileSize());
        }
        else {
            leaveApplication.setDoctorCertificate(null);
        }
        if (HALF_DAY_LEAVES.contains(leaveApplicationRequest.getLeaveSelectionTypeId())) {
            leaveApplication.setIsLeaveForHalfDay(true);
            leaveApplication.setHalfDayLeaveType(leaveApplicationRequest.getHalfDayLeaveType());
        }
        leaveApplication.setNumberOfLeaveDays(leaveApplicationRequest.getNumberOfLeaveDays());
        return leaveApplicationRepository.save(leaveApplication).getLeaveApplicationId();
    }

    /**
     * update an existing leave application
     */
    public Long updateLeaveApplication(LeaveApplicationRequest leaveApplicationRequest) throws IOException {
        LeaveApplication leaveApplication = leaveApplicationRepository.findByLeaveApplicationId(leaveApplicationRequest.getLeaveApplicationId());
        if(Constants.Leave.SICK_LEAVES.contains(leaveApplicationRequest.getLeaveSelectionTypeId())){
            leaveApplication.setLeaveTypeId(Constants.Leave.SICK_LEAVE_TYPE_ID);
        }
        else{
            leaveApplication.setLeaveTypeId(Constants.Leave.TIME_OFF_LEAVE_TYPE_ID);
        }

        if (HALF_DAY_LEAVES.contains(leaveApplicationRequest.getLeaveSelectionTypeId())) {
            leaveApplication.setIsLeaveForHalfDay(true);
            leaveApplication.setHalfDayLeaveType(leaveApplicationRequest.getHalfDayLeaveType());
        } else {
            leaveApplication.setIsLeaveForHalfDay(false);
        }

        leaveApplication.setNumberOfLeaveDays(leaveApplicationRequest.getNumberOfLeaveDays());
        leaveApplication.setFromDate(leaveApplicationRequest.getFromDate());
        leaveApplication.setToDate(leaveApplicationRequest.getToDate());
        leaveApplication.setIncludeLunchTime(leaveApplicationRequest.getIncludeLunchTime());
        leaveApplication.setLeaveReason(leaveApplicationRequest.getLeaveReason());
        leaveApplication.setApproverAccountId(leaveApplicationRequest.getApproverAccountId());
        leaveApplication.setPhone(leaveApplicationRequest.getPhone());
        leaveApplication.setExpiryLeaveDate(leaveApplicationRequest.getExpiryLeaveDate());
        leaveApplication.setAddress(leaveApplicationRequest.getAddress());
        if(leaveApplicationRequest.getNotifyTo()!=null){
            leaveApplication.setNotifyTo(leaveApplicationRequest.getNotifyTo().toString());
        }
        boolean isAlreadyPresentRequest = Boolean.TRUE.equals(leaveApplicationRequest.getIsAttachmentPresent());
        boolean hasNewFileInRequest = leaveApplicationRequest.getDoctorCertificate() != null;
        if (!isAlreadyPresentRequest && hasNewFileInRequest) {
            leaveApplication.setDoctorCertificate(leaveApplicationRequest.getDoctorCertificate());
            leaveApplication.setDoctorCertificateFileName(leaveApplicationRequest.getDoctorCertificateFileName());
            leaveApplication.setDoctorCertificateFileType(leaveApplicationRequest.getDoctorCertificateFileType());
            leaveApplication.setDoctorCertificateFileSize(leaveApplicationRequest.getDoctorCertificateFileSize());
            leaveApplication.setIsAttachmentPresent(true);
        }
        if (isAlreadyPresentRequest) {
            leaveApplication.setIsAttachmentPresent(true);
        }
        if (isAlreadyPresentRequest != true && leaveApplicationRequest.getDoctorCertificate()==null) {
            leaveApplication.setDoctorCertificate(null);
            leaveApplication.setDoctorCertificateFileName(null);
            leaveApplication.setDoctorCertificateFileType(null);
            leaveApplication.setDoctorCertificateFileSize(null);
            leaveApplication.setIsAttachmentPresent(false);
        }
        return leaveApplicationRepository.save(leaveApplication).getLeaveApplicationId();
    }

    /**
     * @return integer to check status of the cancelled leave application is valid or not
     * (return 0 for leave cancellation before approval)
     * (return 1 for leave cancellation after approval. So, the leave again go for approval from approver)
     * (return 2 when leave cancellation is not allowed)
     */
    public LeaveApplicationNotificationRequest cancelLeaveApplication(Long applicationId, Long accountId, String timeZone, String leaveCancellationReason) throws ParseException {
        Boolean sendNotification = Boolean.FALSE;
        Long notifyToAccountId = null;
        LeaveApplication leaveApplication = leaveApplicationRepository.findByLeaveApplicationId(applicationId);
        if (leaveApplication == null) {
            throw new LeaveApplicationValidationException("Leave not found");
        }
        if (Objects.equals(leaveApplication.getLeaveApplicationStatusId(), LEAVE_APPLICATION_EXPIRED_STATUS_ID)) {
            throw new LeaveApplicationValidationException("Leave application is already expired");
        }
        if (Objects.equals(leaveApplication.getLeaveApplicationStatusId(), REJECTED_LEAVE_APPLICATION_STATUS_ID)) {
            throw new LeaveApplicationValidationException("Leave application is already Rejected");
        }
        if (Objects.equals(leaveApplication.getLeaveApplicationStatusId(), CANCELLED_LEAVE_APPLICATION_STATUS_ID)
                || Objects.equals(leaveApplication.getLeaveApplicationStatusId(), CANCELLED_AFTER_APPROVAL_LEAVE_APPLICATION_STATUS_ID)) {
            throw new LeaveApplicationValidationException("Leave application is already cancelled");
        }
        Long orgId = userAccountRepository.findOrgIdByAccountId(leaveApplication.getAccountId());
        if (orgId == null) {
            throw new LeaveApplicationValidationException("User don't exists in the organization anymore.");
        }
        List<Long> teamIdList = accessDomainRepository.findTeamIdsByAccountIdsAndIsActiveTrue(List.of(leaveApplication.getAccountId()));
        List<Long> authorizedAccountIds = accessDomainRepository.getUserInfoWithRolesInEntities(Constants.EntityTypes.TEAM,
                teamIdList, List.of(RoleEnum.PROJECT_MANAGER_SPRINT.getRoleId()), true);
        if (!Objects.equals(leaveApplication.getAccountId(), accountId) && !Objects.equals(leaveApplication.getApproverAccountId(), accountId) && !authorizedAccountIds.contains(accountId)) {
            throw new LeaveApplicationValidationException("User not allowed to cancel the leave application for provided user");
        }
        if(Objects.equals(leaveApplication.getLeaveApplicationStatusId(), Constants.Leave.APPROVED_LEAVE_APPLICATION_STATUS_ID)){
            LocalTime cancelTime = entityPreferenceService.getLeaveRequesterCancelTime(orgId, timeZone);
            Integer cancelDateType = entityPreferenceService.getLeaveRequesterCancelDate(orgId);
            LocalDate cancelDate = Objects.equals(cancelDateType, Constants.LeaveRequesterDate.FROMDATE.getTypeId())
                    ? leaveApplication.getFromDate() : leaveApplication.getToDate();
            if (!cancelDate.isAfter(LocalDate.now()) || (cancelDate.equals(LocalDate.now()) && !LocalTime.now().withSecond(0).isBefore(cancelTime))) {
                throw new LeaveApplicationValidationException("Time has exceeded to cancel a leave application");
            }
            if (!authorizedAccountIds.contains(accountId)) {
                leaveApplicationRepository.changeLeaveApplicationStatus(WAITING_CANCEL_LEAVE_APPLICATION_STATUS_ID, leaveCancellationReason, applicationId);
                leaveApplication.setLeaveApplicationStatusId(WAITING_CANCEL_LEAVE_APPLICATION_STATUS_ID);
                sendNotification = Boolean.TRUE;
                notifyToAccountId = leaveApplication.getApproverAccountId();
            } else {
                leaveApplicationRepository.changeLeaveApplicationStatus(CANCELLED_AFTER_APPROVAL_LEAVE_APPLICATION_STATUS_ID, leaveCancellationReason, applicationId);
                leaveApplication.setLeaveApplicationStatusId(CANCELLED_AFTER_APPROVAL_LEAVE_APPLICATION_STATUS_ID);
                sendNotification = Boolean.TRUE;
                notifyToAccountId = leaveApplication.getAccountId();
            }
        }
        else if(Objects.equals(leaveApplicationRepository.findByLeaveApplicationId(applicationId).getLeaveApplicationStatusId(),Constants.Leave.WAITING_APPROVAL_LEAVE_APPLICATION_STATUS_ID)){
            leaveApplicationRepository.changeLeaveApplicationStatus(CANCELLED_LEAVE_APPLICATION_STATUS_ID, leaveCancellationReason, applicationId);
            leaveApplication.setLeaveApplicationStatusId(CANCELLED_LEAVE_APPLICATION_STATUS_ID);
        }
        leaveApplication.setLeaveCancellationReason(leaveCancellationReason);
        if (Objects.equals(leaveApplication.getLeaveApplicationStatusId(), CANCELLED_AFTER_APPROVAL_LEAVE_APPLICATION_STATUS_ID)) {
            LeaveRemaining leaveRemaining;
            if (Objects.equals(leaveApplication.getLeaveTypeId(), Constants.Leave.SICK_LEAVE_TYPE_ID)) {
                leaveRemaining = leaveRemainingRepository.findByAccountIdAndLeaveTypeIdAndCalenderYear(leaveApplication.getAccountId(), Constants.Leave.SICK_LEAVE_TYPE_ID, (short) LocalDate.now().getYear());
            } else {
                leaveRemaining = leaveRemainingRepository.findByAccountIdAndLeaveTypeIdAndCalenderYear(leaveApplication.getAccountId(), Constants.Leave.TIME_OFF_LEAVE_TYPE_ID, (short) LocalDate.now().getYear());
            }
            float officeHours = (float) timeSheetService.getMaxWorkingMinutes(null, orgId) / 60;
            float totalOfficeHours = officeHours;
            if (leaveApplication.getIsLeaveForHalfDay() != null && leaveApplication.getIsLeaveForHalfDay()) {
                officeHours = (float) (officeHours * 0.5);
            }
            float totalLeaveHours = totalLeaveHours(leaveApplication.getFromDate(), leaveApplication.getToDate(), leaveApplication.getAccountId(), leaveApplication.getLeaveTypeId(), officeHours) /totalOfficeHours;

            leaveRemaining.setLeaveTaken(leaveRemaining.getLeaveTaken() - totalLeaveHours);
            leaveRemaining.setLeaveRemaining(leaveRemaining.getLeaveRemaining() + totalLeaveHours);
            if(!Objects.equals(leaveApplication.getLeaveApplicationStatusId(), CANCELLED_LEAVE_APPLICATION_STATUS_ID)) {
                leaveRemainingRepository.save(leaveRemaining);
                timeSheetService.addLeaveToTimesheet(leaveApplication, false, totalLeaveHours, totalOfficeHours);
            }
        }
        LeaveApplicationNotificationRequest leaveApplicationNotificationRequest = new LeaveApplicationNotificationRequest();
        if (sendNotification) {
            sendNotificationForLeaveCancellation(notifyToAccountId, leaveApplication, leaveApplicationNotificationRequest, accountId);
        }

        return leaveApplicationNotificationRequest;
    }

    /**
     * assign a leave policy to User
     */
    public boolean assignLeavePolicyToUser(AssignLeavePolicyRequest assignLeavePolicyRequest){
        // check for other levels
        LeavePolicy leavePolicy = leavePolicyRepository.findLeavePolicyById(assignLeavePolicyRequest.getNewLeavePolicyId());
        if (leavePolicy != null) {
            if (leaveRemainingRepository.findByAccountIdAndLeaveType(assignLeavePolicyRequest.getAccountId(), leavePolicy.getLeaveTypeId()) != null) {
                throw new LeaveApplicationValidationException("Leave type provided is already assigned to this user");
            } else {
                if (leaveRemainingRepository.findByAccountIdAndLeavePolicyId(assignLeavePolicyRequest.getAccountId(), assignLeavePolicyRequest.getNewLeavePolicyId()) != null) {
                    return false;
                }
                LeaveRemaining leaveRemaining = new LeaveRemaining();
                leaveRemaining.setAccountId(assignLeavePolicyRequest.getAccountId());
                leaveRemaining.setLeavePolicyId(leavePolicy.getLeavePolicyId());
                leaveRemaining.setLeaveTypeId(leavePolicy.getLeaveTypeId());
                // checking for entity preference if leaves are updated on the basis of pro rata or not
                if (entityPreferenceService.getIsMonthlyLeaveUpdateOnProRata(leavePolicy.getTeamId(), leavePolicy.getOrgId())) {
                    leaveRemaining.setLeaveRemaining(0f);
                } else {
                    leaveRemaining.setLeaveRemaining(leavePolicy.getInitialLeaves());
                }
                leaveRemaining.setCreatedDateTime(LocalDateTime.now());
                leaveRemaining.setLeaveTaken(0f);
                leaveRemaining.setCalenderYear((short) LocalDate.now().getYear());
                leaveRemaining.setCurrentlyActive(true);
                leaveRemainingRepository.save(leaveRemaining);
                return true;
            }

        }
        return false;
    }

    /**
     * reassign another leave policy to user
     */
    public boolean reassignLeavePolicyToUser(AssignLeavePolicyRequest assignLeavePolicyRequest) {
        LeaveRemaining leaveRemainingDb = leaveRemainingRepository.findByAccountIdAndLeavePolicyId(assignLeavePolicyRequest.getAccountId(), assignLeavePolicyRequest.getOldLeavePolicyId());
        if (leaveRemainingDb != null) {
            LeavePolicy newLeavePolicy = leavePolicyRepository.findLeavePolicyById(assignLeavePolicyRequest.getNewLeavePolicyId());
            if (newLeavePolicy != null) {
                Float initialLeaves;
                if (entityPreferenceService.getIsMonthlyLeaveUpdateOnProRata(newLeavePolicy.getTeamId(), newLeavePolicy.getOrgId())) {
                    initialLeaves = 0f;
                } else {
                    initialLeaves = newLeavePolicy.getInitialLeaves();
                }
                if (Objects.equals(leaveRemainingDb.getLeaveTypeId(), newLeavePolicy.getLeaveTypeId())) {
                    //initial leaves on basis of start leave policy on
                    leaveRemainingDb.setLeaveRemaining(initialLeaves - leaveRemainingDb.getLeaveTaken());
                    leaveRemainingDb.setLeavePolicyId(assignLeavePolicyRequest.getNewLeavePolicyId());
                    if (!Objects.equals(leaveRemainingDb.getCalenderYear(), (short) LocalDate.now().getYear())) {
                        leaveRemainingDb.setCalenderYear((short) LocalDate.now().getYear());
                        if(newLeavePolicy.getIsLeaveCarryForward()) {
                            if (leaveRemainingDb.getLeaveRemaining() > newLeavePolicy.getMaxLeaveCarryForward()) {
                                leaveRemainingDb.setLeaveRemaining(initialLeaves + newLeavePolicy.getMaxLeaveCarryForward()); //It should be leaveRemainingDb.getLeaveRemaining() instead of initial leaves
                            } else {
                                leaveRemainingDb.setLeaveRemaining(initialLeaves + leaveRemainingDb.getLeaveRemaining());
                            }
                        }
                        else {
                            leaveRemainingDb.setLeaveRemaining(initialLeaves);
                        }
                        leaveRemainingDb.setLeaveTaken(0F);
                    }
                } else {
                    leaveRemainingDb.setLeaveRemaining(initialLeaves - leaveRemainingDb.getLeaveTaken());
                    leaveRemainingDb.setLeavePolicyId(assignLeavePolicyRequest.getNewLeavePolicyId());
                    leaveRemainingDb.setLeaveTypeId(newLeavePolicy.getLeaveTypeId());
                }
                leaveRemainingRepository.save(leaveRemainingDb);
                return true;
            }
        }
        return false;
    }

    /**
     * validate orgId provided and orgId assigned to an accountId
     */
    public boolean validateOrganization(Long orgId, String accountIds) {
        ArrayList<String> accountIdsList = new ArrayList<>(List.of(accountIds.split(",")));
        for(String accountId:accountIdsList){
            if(!Objects.equals(orgId, userAccountRepository.findOrgIdByAccountId(Long.valueOf(accountId)))){
                return false;
            }
        }

        return true;
    }

    /**
     * get leave history for an accountId according to from and to date
     */
    public List<LeaveApplicationResponse> getLeaveHistory(LeaveHistoryRequest leaveHistoryRequest){
        List<LeaveApplicationResponse> leaveApplicationResponseList = new ArrayList<>();
        List<LeaveApplication> leaveApplicationList;
        leaveApplicationList = leaveApplicationRepository.findByAccountIdAndFromToDate(leaveHistoryRequest.getAccountId(),leaveHistoryRequest.getFromDate(),leaveHistoryRequest.getToDate());

        LeaveTypeAlias leaveTypeAlias = entityPreferenceRepository.findLeaveTypeAliasForEntity(Constants.EntityTypes.ORG, userAccountRepository.findOrgIdByAccountId(leaveHistoryRequest.getAccountId()));
        for (LeaveApplication leaveApplication : leaveApplicationList){
            leaveApplicationResponseList.add(getLeaveApplicationResponse(leaveApplication,leaveTypeAlias));
        }
        return leaveApplicationResponseList;
    }

    /**
     * get get all leave applications which are in process for approval
     */
    public List<LeaveApplicationResponse> applicationStatus(Long accountId){
        List<LeaveApplicationResponse> leaveApplicationResponseList = new ArrayList<>();
        List<LeaveApplication> leaveApplicationList = new ArrayList<>();
        leaveApplicationList.addAll(leaveApplicationRepository.findByAccountIdAndApplicationStatus(accountId,Constants.Leave.WAITING_APPROVAL_LEAVE_APPLICATION_STATUS_ID));
        leaveApplicationList.addAll(leaveApplicationRepository.findByAccountIdAndApplicationStatus(accountId,Constants.Leave.WAITING_CANCEL_LEAVE_APPLICATION_STATUS_ID));
        LeaveTypeAlias leaveTypeAlias = entityPreferenceRepository.findLeaveTypeAliasForEntity(Constants.EntityTypes.ORG, userAccountRepository.findOrgIdByAccountId(accountId));
        for (LeaveApplication leaveApplication : leaveApplicationList){
            leaveApplicationResponseList.add(getLeaveApplicationResponse(leaveApplication, leaveTypeAlias));
        }
        return leaveApplicationResponseList;
    }

    /**
     * get all leave applications to be processed by the approve
     */
    public AllLeavesByFilterResponse getLeavesByFilter(LeaveWithFilterRequest leaveWithFilterRequest){
        List<LeaveApplicationResponse> leaveApplicationResponseList = new ArrayList<>();
        List<LeaveApplication> leaveApplicationList = new ArrayList<>();
        Integer page=null;
        Integer size= null;
        if(leaveWithFilterRequest.getPage()!=null)
            page=Math.toIntExact(leaveWithFilterRequest.getPage());
        if(leaveWithFilterRequest.getSize()!=null)
            size=Math.toIntExact(leaveWithFilterRequest.getSize());

        LocalDate fromDate = leaveWithFilterRequest.getFromDate();
        LocalDate toDate = leaveWithFilterRequest.getToDate();
        if (fromDate == null) {
            fromDate = LocalDate.of(LocalDateTime.now().getYear(), 1, 1);
            leaveWithFilterRequest.setFromDate(fromDate);
        }
        if (toDate == null) {
            toDate = LocalDate.of(LocalDateTime.now().getYear(), 12, 31);
            leaveWithFilterRequest.setToDate(toDate);
        }

        String nativeQuery = getNativeQuery(leaveWithFilterRequest);
        Query query = entityManager.createNativeQuery(nativeQuery, LeaveApplication.class);
        setQueryParameters(leaveWithFilterRequest, query);
        List<LeaveApplication> leaveApplications = query.getResultList();

        leaveApplications.sort(Comparator.comparing(LeaveApplication::getFromDate, Comparator.nullsLast(Comparator.reverseOrder())));
        if (page != null && size != null) {
            int startIndex = page * size;
            int endIndex = min(startIndex + size, leaveApplications.size());

            if (startIndex < leaveApplications.size()) {
                leaveApplicationList.addAll(leaveApplications.subList(startIndex, endIndex));
            }
        } else {
            leaveApplicationList.addAll(leaveApplications);
        }
        Long orgId = leaveWithFilterRequest.getOrgId();

        if (orgId == null) {
            Long accountId = null;
            if (leaveWithFilterRequest.getApproverAccountId() != null) accountId = leaveWithFilterRequest.getApproverAccountId();
            if (accountId == null && leaveWithFilterRequest.getAccountIdList() != null & !leaveWithFilterRequest.getAccountIdList().isEmpty()) {
                accountId = leaveWithFilterRequest.getAccountIdList().get(0);
            }
            orgId = userAccountRepository.findOrgIdByAccountId(accountId);
        }
        LeaveTypeAlias leaveTypeAlias = entityPreferenceRepository.findLeaveTypeAliasForEntity(Constants.EntityTypes.ORG, orgId);
        for (LeaveApplication leaveApplication : leaveApplicationList){
            leaveApplicationResponseList.add(getLeaveApplicationResponse(leaveApplication, leaveTypeAlias));
        }
        AllLeavesByFilterResponse allLeavesByFilterResponse = new AllLeavesByFilterResponse();
        allLeavesByFilterResponse.setUserLeaveReportResponseList(leaveApplicationResponseList);
        allLeavesByFilterResponse.setTotalLeavesByFilter(leaveApplications.size());
        allLeavesByFilterResponse.setFromDate(fromDate);
        allLeavesByFilterResponse.setToDate(toDate);

        return allLeavesByFilterResponse;
    }

    /**
     * update the status of the application by approver
     */
    public boolean changeLeaveStatus(ChangeLeaveStatusRequest changeLeaveStatusRequest, LeaveApplication leaveApplication) throws ParseException {
        if(!Constants.Leave.APPROVER_LEAVE_APPLICATION_STATUS.contains(changeLeaveStatusRequest.getLeaveApplicationStatusId())){
            throw new NotAcceptableStatusException("Not acceptable state change");
        }
        Long orgId = userAccountRepository.findOrgIdByAccountId(leaveApplication.getAccountId());
        float officeHours = (float) timeSheetService.getMaxWorkingMinutes(changeLeaveStatusRequest.getTeamId(), orgId) / 60;
        float totalOfficeHours = officeHours;
        if (leaveApplication.getIsLeaveForHalfDay() != null && leaveApplication.getIsLeaveForHalfDay()) {
            officeHours = (float) (officeHours * 0.5);
        }
        //check leave status to be updated to
        if(Objects.equals(leaveApplication.getLeaveApplicationStatusId(), Constants.Leave.WAITING_APPROVAL_LEAVE_APPLICATION_STATUS_ID)){
            if(Objects.equals(changeLeaveStatusRequest.getLeaveApplicationStatusId(), Constants.Leave.APPROVED_LEAVE_APPLICATION_STATUS_ID)){
                if (changeLeaveStatusRequest.getApproverReason() == null || changeLeaveStatusRequest.getApproverReason().isEmpty() || changeLeaveStatusRequest.getApproverReason().length() < 3) {
                    throw new LeaveApplicationValidationException("Please provide reason to approve leave application. Length of approver reason should be greater than 3.");
                }
                leaveApplication.setApproverReason(changeLeaveStatusRequest.getApproverReason());
                if (leaveApplication.getToDate().isAfter(LocalDate.now())) {
                    leaveApplication.setLeaveApplicationStatusId(APPROVED_LEAVE_APPLICATION_STATUS_ID);
                } else {
                    leaveApplication.setLeaveApplicationStatusId(CONSUMED_LEAVE_APPLICATION_STATUS_ID);
                }
                leaveApplicationRepository.save(leaveApplication);
                float totalLeaveHours = totalLeaveHours(leaveApplication.getFromDate(), leaveApplication.getToDate(), leaveApplication.getAccountId(), leaveApplication.getLeaveTypeId(), officeHours) /totalOfficeHours;
                //updating leaveRemaining according to leave type
                LeaveRemaining leaveRemaining;
                if (Objects.equals(leaveApplication.getLeaveTypeId(), Constants.Leave.SICK_LEAVE_TYPE_ID)) {
                    leaveRemaining = leaveRemainingRepository.findByAccountIdAndLeaveTypeIdAndCalenderYear(leaveApplication.getAccountId(), Constants.Leave.SICK_LEAVE_TYPE_ID, (short) LocalDate.now().getYear());
                }
                else {
                    leaveRemaining = leaveRemainingRepository.findByAccountIdAndLeaveTypeIdAndCalenderYear(leaveApplication.getAccountId(), Constants.Leave.TIME_OFF_LEAVE_TYPE_ID, (short) LocalDate.now().getYear());
                }

                if (leaveRemaining == null) {
                    throw new LeaveApplicationValidationException("No leave policy assigned to user for current year. Please contact your admins to assign leave policy before applying for a leave");
                }
                LeavePolicy leavePolicy = leavePolicyRepository.findByLeavePolicyId(leaveRemaining.getLeavePolicyId());
                float typeLeaveRemaining = ((leavePolicy.getIsNegativeLeaveAllowed() != null && leavePolicy.getIsNegativeLeaveAllowed()) ? leaveRemaining.getLeaveRemaining() + leavePolicy.getMaxNegativeLeaves() : leaveRemaining.getLeaveRemaining()) * officeHours;
                if (typeLeaveRemaining < totalLeaveHours) {
                    leaveApplication.setLeaveApplicationStatusId(Constants.Leave.WAITING_APPROVAL_LEAVE_APPLICATION_STATUS_ID);
                    throw new LeaveApplicationValidationException("User does not have sufficient leave.");
                }
                // TODO: Commenting this code as we have only one day leave as of now. The method getMaxWorkingMinutes returns the total office duration excluding the lunch hrs.
                // TODO: We need to modify the totalLeaveHours method accordingly
//                float breakDuration = getBreakDuration(changeLeaveStatusRequest.getTeamId(), changeLeaveStatusRequest.getOrgId());
//                if (totalLeaveHours > officeHours) {
//                    totalLeaveHours -= (float) breakDuration;
//                } else {
//                    totalLeaveHours -= (float) breakDuration * totalLeaveHours / officeHours;
//                }

                leaveRemaining.setLeaveTaken(leaveRemaining.getLeaveTaken() + totalLeaveHours);
                leaveRemaining.setLeaveRemaining(leaveRemaining.getLeaveRemaining() - totalLeaveHours);
                leaveRemainingRepository.save(leaveRemaining);
                timeSheetService.addLeaveToTimesheet(leaveApplication,true,totalLeaveHours, totalOfficeHours);
                return true;
            }
            else if(Objects.equals(changeLeaveStatusRequest.getLeaveApplicationStatusId(), Constants.Leave.REJECTED_LEAVE_APPLICATION_STATUS_ID)){
                leaveApplication.setApproverReason(changeLeaveStatusRequest.getApproverReason());
                leaveApplication.setLeaveApplicationStatusId(Constants.Leave.REJECTED_LEAVE_APPLICATION_STATUS_ID);
                leaveApplicationRepository.save(leaveApplication);
                return true;
            }
        }
        else if(Objects.equals(leaveApplication.getLeaveApplicationStatusId(), Constants.Leave.WAITING_CANCEL_LEAVE_APPLICATION_STATUS_ID)){

            if(Objects.equals(changeLeaveStatusRequest.getLeaveApplicationStatusId(), Constants.Leave.APPROVED_LEAVE_APPLICATION_STATUS_ID)){
                if (changeLeaveStatusRequest.getApproverReason() == null || changeLeaveStatusRequest.getApproverReason().isEmpty() || changeLeaveStatusRequest.getApproverReason().length() < 3) {
                    throw new LeaveApplicationValidationException("Please provide reason to cancel leave application. Length of cancellation reason should be greater than 3.");
                }
                leaveApplication.setApproverReason(changeLeaveStatusRequest.getApproverReason());
                leaveApplication.setLeaveApplicationStatusId(CANCELLED_AFTER_APPROVAL_LEAVE_APPLICATION_STATUS_ID);
                leaveApplicationRepository.save(leaveApplication);
                float totalLeaveHours = totalLeaveHours(leaveApplication.getFromDate(), leaveApplication.getToDate(), leaveApplication.getAccountId(), leaveApplication.getLeaveTypeId(), officeHours) / totalOfficeHours;
                //updating leaveRemaining according to leave type
                LeaveRemaining leaveRemaining;
                if (Objects.equals(leaveApplication.getLeaveTypeId(), Constants.Leave.SICK_LEAVE_TYPE_ID)) {
                    leaveRemaining = leaveRemainingRepository.findByAccountIdAndLeaveTypeIdAndCalenderYear(leaveApplication.getAccountId(), Constants.Leave.SICK_LEAVE_TYPE_ID, (short) LocalDate.now().getYear());
                } else {
                    leaveRemaining = leaveRemainingRepository.findByAccountIdAndLeaveTypeIdAndCalenderYear(leaveApplication.getAccountId(), Constants.Leave.TIME_OFF_LEAVE_TYPE_ID, (short) LocalDate.now().getYear());
                }

                if (leaveRemaining == null) {
                    throw new LeaveApplicationValidationException("No leave policy assigned to user for current year. Please contact your admins to assign leave policy before applying for a leave");
                }
                //TODO: Commenting this code as we have only one day leave as of now. The method getMaxWorkingMinutes returns the total office duration excluding the lunch hrs.
                //ToDo: We need to modify the  totalLeaveHours method accordingly
//                float breakDuration = getBreakDuration(changeLeaveStatusRequest.getTeamId(), changeLeaveStatusRequest.getOrgId());
//                if (totalLeaveHours > officeHours) {
//                    totalLeaveHours -= (float) breakDuration;
//                } else {
//                    totalLeaveHours -= (float) breakDuration * totalLeaveHours / officeHours;
//                }

                leaveRemaining.setLeaveTaken(leaveRemaining.getLeaveTaken() - totalLeaveHours);
                leaveRemaining.setLeaveRemaining(leaveRemaining.getLeaveRemaining() + totalLeaveHours);
                leaveRemainingRepository.save(leaveRemaining);
                timeSheetService.addLeaveToTimesheet(leaveApplication,false,totalLeaveHours, totalOfficeHours);
                return true;
            }
            else if(Objects.equals(changeLeaveStatusRequest.getLeaveApplicationStatusId(), Constants.Leave.REJECTED_LEAVE_APPLICATION_STATUS_ID)){
                leaveApplication.setApproverReason(changeLeaveStatusRequest.getApproverReason());
                leaveApplication.setLeaveApplicationStatusId(Constants.Leave.APPROVED_LEAVE_APPLICATION_STATUS_ID);
                leaveApplicationRepository.save(leaveApplication);
                return true;
            }
        }
        return false;
    }


    /**
     * find total leave hour for an application
     * Let us say full working day is of 9 hrs. If leave is for full day then officeHours should be 9 hrs and if leave is for half day then officeHours should be 4.5
     */
    private float totalLeaveHours(LocalDate fromDate,LocalDate toDate, Long accountId,Short leaveTypeId, float officeHours) throws ParseException {

        if (toDate.isBefore(fromDate)) {
            throw new LeaveApplicationValidationException("The leave start date must be earlier than the leave end date.");
        }
        boolean checkNonBusinessDays= leavePolicyRepository.findByLeavePolicyId(leaveRemainingRepository.findByAccountIdAndLeaveType(accountId, leaveTypeId).getLeavePolicyId()).getIncludeNonBusinessDaysInLeave();
        Long orgId = userAccountRepository.findOrgIdByAccountId(accountId);
        //within a day
        if(Objects.equals(toDate,fromDate)){
            if (checkNonBusinessDays) {
                return officeHours;
            }
            else {
                if (!timeSheetService.validateHolidayDate(accountId, toDate) && !entityPreferenceService.validateWeekendDate(orgId, toDate)) {
                    return officeHours;
                }
            }
        }
        //for multiple days
        else{
            float leaveTimeOnBetweenDate =0 ;
            LocalDate date = fromDate;
            if(checkNonBusinessDays) {
                while (!date.isAfter(toDate)) {
                    leaveTimeOnBetweenDate+=officeHours;
                    date = date.plusDays(1);
                }
            }
            else {
                while(!date.isAfter(toDate)) {
                    if (!timeSheetService.validateHolidayDate(accountId, date) && !entityPreferenceService.validateWeekendDate(orgId, date)) {
                        leaveTimeOnBetweenDate += officeHours;
                    }
                    date = date.plusDays(1);
                }
            }
            return leaveTimeOnBetweenDate;
        }
        return officeHours;
    }

    /**
     * get leave history for an accountId assigned else for the team
     */
    public List<LeaveApplicationResponse> getTeamLeaveHistory(TeamLeaveHistoryRequest teamLeaveHistoryRequest) {
        //for a single account
        if(teamLeaveHistoryRequest.getAccountId()!=null){
            List<LeaveApplicationResponse> leaveApplicationResponseList = new ArrayList<>();
            List<LeaveApplication> leaveApplicationList;
            leaveApplicationList = leaveApplicationRepository.findByAccountId(teamLeaveHistoryRequest.getAccountId());
            LeaveTypeAlias leaveTypeAlias = entityPreferenceRepository.findLeaveTypeAliasForEntity(Constants.EntityTypes.ORG, userAccountRepository.findOrgIdByAccountId(teamLeaveHistoryRequest.getAccountId()));
            for (LeaveApplication leaveApplication : leaveApplicationList){
                leaveApplicationResponseList.add(getLeaveApplicationResponse(leaveApplication, leaveTypeAlias));
            }
            return leaveApplicationResponseList;
        }
        //for a team
        else{
            List<AccountId> distinctAccountIdFoundDb = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityId(Constants.TEAM, teamLeaveHistoryRequest.getTeamId());

            List<LeaveApplicationResponse> leaveApplicationResponseList = new ArrayList<>();
            List<LeaveApplication> leaveApplicationList= new ArrayList<>();
            for(AccountId accountId:distinctAccountIdFoundDb) {
                leaveApplicationList.addAll(leaveApplicationRepository.findByAccountId(accountId.getAccountId()));
            }
            LeaveTypeAlias leaveTypeAlias = entityPreferenceRepository.findLeaveTypeAliasForEntity(Constants.EntityTypes.ORG, teamRepository.findOrgIdByTeamId(teamLeaveHistoryRequest.getTeamId()));
            for (LeaveApplication leaveApplication : leaveApplicationList){
                leaveApplicationResponseList.add(getLeaveApplicationResponse(leaveApplication, leaveTypeAlias));
            }
            return leaveApplicationResponseList;
        }
    }

    /**
     * get leaves remaining for an accountId according to the year provided
     */
    public List<LeaveRemainingResponse> getLeavesRemaining(LeaveRemainingHistoryRequest leaveRemainingHistoryRequest) {
        List<LeaveRemaining> leaveRemainingList = leaveRemainingRepository.findByAccountIdAndCalenderYear(leaveRemainingHistoryRequest.getAccountId(), leaveRemainingHistoryRequest.getYear());
        LeaveTypeAlias leaveTypeAlias = entityPreferenceRepository.findLeaveTypeAliasForEntity(Constants.EntityTypes.ORG, userAccountRepository.findOrgIdByAccountId(leaveRemainingHistoryRequest.getAccountId()));
        List<LeaveRemainingResponse> leaveRemainingResponseList = new ArrayList<>();
        for(LeaveRemaining leaveRemaining : leaveRemainingList){
            LeaveRemainingResponse leaveRemainingResponse = new LeaveRemainingResponse();
            if (leaveRemainingHistoryRequest.getFromDate() != null) {
                leaveRemainingResponse.setFromDate(leaveRemainingHistoryRequest.getFromDate());
            } else {
                leaveRemainingHistoryRequest.setFromDate(LocalDate.of(leaveRemaining.getCalenderYear().intValue(), 1, 1));
                leaveRemainingResponse.setFromDate(LocalDate.of(leaveRemaining.getCalenderYear().intValue(), 1, 1));
            }
            if (leaveRemainingHistoryRequest.getToDate() != null) {
                leaveRemainingResponse.setToDate(leaveRemainingHistoryRequest.getToDate());
            } else {
                leaveRemainingHistoryRequest.setToDate(LocalDate.of(leaveRemaining.getCalenderYear().intValue(), 12, 31));
                leaveRemainingResponse.setToDate(LocalDate.of(leaveRemaining.getCalenderYear().intValue(), 12, 31));
            }
            leaveRemainingResponse.setAccountId(leaveRemaining.getAccountId());
            leaveRemainingResponse.setLeavePolicyId(leaveRemaining.getLeavePolicyId());
            leaveRemainingResponse.setLeaveTypeId(leaveRemaining.getLeaveTypeId());
            leaveRemainingResponse.setLeaveRemaining(leaveRemaining.getLeaveRemaining());
            leaveRemainingResponse.setLeaveTaken(leaveRemaining.getLeaveTaken());
            leaveRemainingResponse.setCalenderYear(leaveRemaining.getCalenderYear());
            leaveRemainingResponse.setConsumedLeaves(leaveApplicationRepository.findCountByLeaveTypeAndApplicationStatusIdInAndYear(leaveRemaining.getAccountId(), List.of(CONSUMED_LEAVE_APPLICATION_STATUS_ID), leaveRemainingResponse.getFromDate(), leaveRemainingResponse.getToDate(),leaveRemaining.getLeaveTypeId()));
            leaveRemainingResponse.setPlannedLeaves(leaveApplicationRepository.findCountByLeaveTypeAndApplicationStatusIdInAndYear(leaveRemaining.getAccountId(), List.of(APPROVED_LEAVE_APPLICATION_STATUS_ID), leaveRemainingResponse.getFromDate(), leaveRemainingResponse.getToDate(),leaveRemaining.getLeaveTypeId()));
            leaveRemainingResponse.setPendingLeaves(leaveApplicationRepository.findCountByLeaveTypeAndApplicationStatusIdInAndYear(leaveRemaining.getAccountId(), List.of(WAITING_APPROVAL_LEAVE_APPLICATION_STATUS_ID,WAITING_CANCEL_LEAVE_APPLICATION_STATUS_ID), leaveRemainingResponse.getFromDate(), leaveRemainingResponse.getToDate(),leaveRemaining.getLeaveTypeId()));
            leaveRemainingResponse.setRejectedLeaves(leaveApplicationRepository.findCountByLeaveTypeAndApplicationStatusIdInAndYear(leaveRemaining.getAccountId(), List.of(REJECTED_LEAVE_APPLICATION_STATUS_ID), leaveRemainingResponse.getFromDate(), leaveRemainingResponse.getToDate(),leaveRemaining.getLeaveTypeId()));
            leaveRemainingResponse.setCancelledLeaves(leaveApplicationRepository.findCountByLeaveTypeAndApplicationStatusIdInAndYear(leaveRemaining.getAccountId(), List.of(CANCELLED_AFTER_APPROVAL_LEAVE_APPLICATION_STATUS_ID, CANCELLED_LEAVE_APPLICATION_STATUS_ID), leaveRemainingResponse.getFromDate(), leaveRemainingResponse.getToDate(),leaveRemaining.getLeaveTypeId()));
            leaveRemainingResponse.setExpiredLeaves(leaveApplicationRepository.findCountByLeaveTypeAndApplicationStatusIdInAndYear(leaveRemaining.getAccountId(), List.of(LEAVE_APPLICATION_EXPIRED_STATUS_ID), leaveRemainingResponse.getFromDate(), leaveRemainingResponse.getToDate(),leaveRemaining.getLeaveTypeId()));
            leaveRemainingResponse.setStartLeavePolicyOn(leaveRemaining.getStartLeavePolicyOn());
            leaveRemainingResponse.setLeaveTypeName(Objects.equals(leaveRemaining.getLeaveTypeId(), TIME_OFF_LEAVE_TYPE_ID) ? leaveTypeAlias.getTimeOffAlias() : leaveTypeAlias.getSickLeaveAlias());
            leaveRemainingResponseList.add(leaveRemainingResponse);
        }
        leaveRemainingResponseList.sort(Comparator.comparing(LeaveRemainingResponse::getLeaveTypeId));
        return leaveRemainingResponseList;
    }

    /**
     * get all team members who are on leave on that day
     */
    public List<LeaveApplicationResponse> getTeamMembersOnLeave(TeamMemberOnLeaveRequest teamMemberOnLeaveRequest) {
        List<AccountId> distinctAccountIdFoundDb = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityId(Constants.TEAM, teamMemberOnLeaveRequest.getTeamId());

        List<LeaveApplicationResponse> leaveApplicationResponseList = new ArrayList<>();
        List<LeaveApplication> leaveApplicationList= new ArrayList<>();

        if (distinctAccountIdFoundDb != null && !distinctAccountIdFoundDb.isEmpty()) {
            distinctAccountIdFoundDb.stream()
                    .map(accountId -> leaveApplicationRepository.findByAccountIdAndDate(accountId.getAccountId(), teamMemberOnLeaveRequest.getTodayDate(), List.of(CONSUMED_LEAVE_APPLICATION_STATUS_ID, APPROVED_LEAVE_APPLICATION_STATUS_ID)
                    ))
                    .filter(foundLeaves -> foundLeaves != null && !foundLeaves.isEmpty())
                    .map(foundLeaves -> foundLeaves.get(0))
                    .forEach(leaveApplicationList::add);
        }

        LeaveTypeAlias leaveTypeAlias = entityPreferenceRepository.findLeaveTypeAliasForEntity(Constants.EntityTypes.ORG, teamRepository.findOrgIdByTeamId(teamMemberOnLeaveRequest.getTeamId()));
        for (LeaveApplication leaveApplication : leaveApplicationList){
            leaveApplicationResponseList.add(getLeaveApplicationResponse(leaveApplication, leaveTypeAlias));
        }
        return leaveApplicationResponseList;
    }

    /**
     * LeaveApplication as per the provided leaveApplicationId
     */
    public LeaveApplicationResponse getLeaveApplication(Long leaveApplicationId) {
        LeaveApplication leaveApplication= leaveApplicationRepository.findByLeaveApplicationId(leaveApplicationId);
        LeaveTypeAlias leaveTypeAlias = entityPreferenceRepository.findLeaveTypeAliasForEntity(Constants.EntityTypes.ORG, userAccountRepository.findOrgIdByAccountId(leaveApplication.getAccountId()));
        return getLeaveApplicationResponse(leaveApplication, leaveTypeAlias);

    }

    public DoctorCertificateMetaData getDoctorCertificate(Long applicationId) {
        DoctorCertificateMetaData doctorCertificateMetaData=new DoctorCertificateMetaData();
        doctorCertificateMetaData.setDoctorCertificate(leaveApplicationRepository.findDoctorCertificateByLeaveApplicationId(applicationId));
        LeaveApplication leaveApplication = leaveApplicationRepository.findByLeaveApplicationId(applicationId);
        doctorCertificateMetaData.setFileName(leaveApplication.getDoctorCertificateFileName());
        doctorCertificateMetaData.setFileType(leaveApplication.getDoctorCertificateFileType());
        doctorCertificateMetaData.setFileSize(leaveApplication.getDoctorCertificateFileSize());
        return doctorCertificateMetaData;
    }

    public Boolean validateNoHolidayDate (LeaveApplicationRequest leaveApplicationRequest) {
        if (!leaveApplicationRequest.getFromDate().equals(leaveApplicationRequest.getToDate())) {
            LocalDate date = leaveApplicationRequest.getFromDate();
            while (date.isBefore(leaveApplicationRequest.getToDate()) || date.equals(leaveApplicationRequest.getToDate())) {
                TimeSheet timeSheet = timeSheetRepository.findByAccountIdAndNewEffortDateAndEntityTypeId(leaveApplicationRequest.getAccountId(),  date, Constants.HOLIDAY_TYPE_ID);
                if (timeSheet != null) {
                    return false;
                }
                date = date.plusDays(1);
            }
        } else {
            TimeSheet timeSheet = timeSheetRepository.findByAccountIdAndNewEffortDateAndEntityTypeId(leaveApplicationRequest.getAccountId(), leaveApplicationRequest.getFromDate(), Constants.HOLIDAY_TYPE_ID);
            return timeSheet == null;
        }
        return true;
    }

    /**
     * This api assigns leave to all the accounts provided
     */
    public void assignLeavePolicyToAllUser(AssignLeavePolicyInBulkRequest assignLeavePolicyRequest){
        LeavePolicy leavePolicy = leavePolicyRepository.findLeavePolicyById(assignLeavePolicyRequest.getLeavePolicyId());
        if (leavePolicy != null) {
            if (leavePolicy.getOrgId() != null && !Objects.equals(leavePolicy.getOrgId(), assignLeavePolicyRequest.getOrgId())) {
                throw new ValidationFailedException("The provided leave policy does not belong to the organization specified");
            }
            if (leavePolicy.getTeamId() != null && assignLeavePolicyRequest.getTeamId() != null && !Objects.equals(leavePolicy.getTeamId(), assignLeavePolicyRequest.getTeamId())) {
                throw new ValidationFailedException("The provided leave policy is not associated with the specified team");
            }
            List<Long> usersWithPolicy = leaveRemainingRepository.findAllByLeavePolicyId(leavePolicy.getLeavePolicyId());
            List<Long> usersWithPolicyType = leaveRemainingRepository.findByLeaveType(leavePolicy.getLeaveTypeId());
            List<LeaveRemaining> leaveRemainingList = new ArrayList<>();
            for (Long accountId : assignLeavePolicyRequest.getAccountIdList()) {
                if (usersWithPolicyType.contains(accountId)) {
                    LeaveRemaining leaveRemaining = leaveRemainingRepository.findByAccountIdAndLeaveType(accountId, leavePolicy.getLeaveTypeId());
                    if (leaveRemaining != null) {
                        changeLeavePolicyOfUser (leaveRemaining, leavePolicy);
                        leaveRemainingList.add(leaveRemaining);
                    }
                }
                else {
                    LeaveRemaining leaveRemaining = new LeaveRemaining();
                    leaveRemaining.setAccountId(accountId);
                    leaveRemaining.setLeavePolicyId(leavePolicy.getLeavePolicyId());
                    leaveRemaining.setLeaveTypeId(leavePolicy.getLeaveTypeId());
                    // checking for entity preference if leaves are updated on the basis of pro rata or not
                    if (entityPreferenceService.getIsMonthlyLeaveUpdateOnProRata(leavePolicy.getTeamId(), leavePolicy.getOrgId())) {
                        leaveRemaining.setLeaveRemaining(0f);
                    } else {
                        leaveRemaining.setLeaveRemaining(leavePolicy.getInitialLeaves());
                    }
                    leaveRemaining.setCreatedDateTime(LocalDateTime.now());
                    leaveRemaining.setLeaveTaken(0f);
                    leaveRemaining.setCalenderYear((short) LocalDate.now().getYear());
                    leaveRemaining.setCurrentlyActive(true);
                    leaveRemainingList.add(leaveRemaining);
                }

            }
            leaveRemainingRepository.saveAll(leaveRemainingList);
        }
    }

    public void changeLeavePolicyOfUser (LeaveRemaining leaveRemaining, LeavePolicy leavePolicy) {
        int numberOfMonthPassed = getMonthsBeforeCurrent();
        LeavePolicy oldLeavePolicy = leavePolicyRepository.findLeavePolicyById(leaveRemaining.getLeavePolicyId());
        if (!entityPreferenceService.getIsMonthlyLeaveUpdateOnProRata(oldLeavePolicy.getTeamId(), oldLeavePolicy.getOrgId())) {
            if (leaveRemaining.getLeaveRemaining() > oldLeavePolicy.getInitialLeaves()) {
                leaveRemaining.setLeaveRemaining(leaveRemaining.getLeaveRemaining() - oldLeavePolicy.getInitialLeaves());
            }
            else {
                if (leaveRemaining.getLeaveRemaining() > 0f) {
                    leaveRemaining.setLeaveRemaining(0f);
                }
            }
            if (numberOfMonthPassed > 0) {
                leaveRemaining.setLeaveRemaining(leaveRemaining.getLeaveRemaining() + (oldLeavePolicy.getInitialLeaves()*numberOfMonthPassed) / 12);
            }
        }
        if (!entityPreferenceService.getIsMonthlyLeaveUpdateOnProRata(leavePolicy.getTeamId(), leavePolicy.getOrgId())) {
            if (12 - numberOfMonthPassed > 0) {
                leaveRemaining.setLeaveRemaining(leaveRemaining.getLeaveRemaining() + (leavePolicy.getInitialLeaves()*(12 - numberOfMonthPassed)) / 12);
            }
        }
        leaveRemaining.setCalenderYear((short) LocalDate.now().getYear());
        leaveRemaining.setLeavePolicyId(leavePolicy.getLeavePolicyId());
    }

    public int getMonthsBeforeCurrent() {
        LocalDate now = LocalDate.now();
        int currentMonth = now.getMonthValue();

        return currentMonth - 1;
    }

    /**
     * get all user team members on leave for that day
     */
//    public List<PeopleOnLeaveResponse> getPeopleOnLeave(Integer entityTypeId, Long entityId, LocalDate todayDate, String accountIds) {
//        List<Long> accountIdsList = CommonUtils.convertToLongList(accountIds);
//        List<TeamDetails> distinctTeam = getTeamDetailsForEntity(entityTypeId, entityId, accountIdsList);
//        List<PeopleOnLeaveResponse> response = new ArrayList<>();
//        for (TeamDetails teamDetails : distinctTeam) {
//            List<AccountId> distinctAccountIdFoundDb = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityId(Constants.TEAM, teamDetails.getTeamId());
//            PeopleOnLeaveResponse peopleOnLeaveResponse = new PeopleOnLeaveResponse();
//            BeanUtils.copyProperties(teamDetails, peopleOnLeaveResponse);
//            List<LeaveApplication> leaveApplicationList= new ArrayList<>();
//            for(AccountId accountId:distinctAccountIdFoundDb) {
//                leaveApplicationList.addAll(leaveApplicationRepository.findByAccountIdAndDate(accountId.getAccountId(), todayDate, List.of(CONSUMED_LEAVE_APPLICATION_STATUS_ID, APPROVED_LEAVE_APPLICATION_STATUS_ID)));
//            }
//            LeaveTypeAlias leaveTypeAlias = entityPreferenceRepository.findLeaveTypeAliasForEntity(Constants.EntityTypes.ORG, teamDetails.getOrgId());
//            List<LeaveApplicationDashboardResponse> leaveApplicationResponseList = getPeopleOnLeaveResponse(leaveApplicationList, leaveTypeAlias);
//            peopleOnLeaveResponse.setLeaveApplicationDashboardResponse(leaveApplicationResponseList);
//            response.add(peopleOnLeaveResponse);
//        }
//        response.stream().sorted(Comparator.comparing(PeopleOnLeaveResponse::getOrgName)
//                .thenComparing(Comparator.comparing(PeopleOnLeaveResponse::getProjectName)
//                        .thenComparing(Comparator.comparing(PeopleOnLeaveResponse::getTeamName))));
//
//        return response;
//    }
    public List<PeopleOnLeaveResponse> getPeopleOnLeave(Integer entityTypeId, Long entityId, LocalDate todayDate, String accountIds) {
        List<Long> accountIdsList = CommonUtils.convertToLongList(accountIds);
        List<Long> accountIdListForEntity = getAccountIdsForEntity(entityTypeId, entityId, accountIdsList);
        List<PeopleOnLeaveResponse> response = new ArrayList<>();
        PeopleOnLeaveResponse peopleOnLeaveResponse = new PeopleOnLeaveResponse();
        List<LeaveApplication> leaveApplicationList= new ArrayList<>();
        Long orgId = null;
        if (Objects.equals(Constants.EntityTypes.ORG, entityTypeId)) {
            orgId = entityId;
        }
        else if (Objects.equals(Constants.EntityTypes.BU, entityTypeId)) {
            orgId = buRepository.findOrgIdByBuId(entityId);
        }
        else if (Objects.equals(Constants.EntityTypes.PROJECT, entityTypeId)) {
            orgId = projectRepository.findByProjectId(entityId).getOrgId();
        }
        else if(Objects.equals(Constants.EntityTypes.TEAM, entityTypeId)) {
            orgId = teamRepository.findFkOrgIdOrgIdByTeamId(entityId);
        }
        else {
            throw new ValidationFailedException("Incorrect Entity Type selected");
        }
        LeaveTypeAlias leaveTypeAlias = entityPreferenceRepository.findLeaveTypeAliasForEntity(Constants.EntityTypes.ORG, orgId);

        if (accountIdListForEntity != null && !accountIdListForEntity.isEmpty()) {
            accountIdListForEntity.stream()
                    .map(accountId -> leaveApplicationRepository.findByAccountIdAndDate(accountId, todayDate, List.of(CONSUMED_LEAVE_APPLICATION_STATUS_ID, APPROVED_LEAVE_APPLICATION_STATUS_ID)
                    ))
                    .filter(foundLeaves -> foundLeaves != null && !foundLeaves.isEmpty())
                    .map(foundLeaves -> foundLeaves.get(0))
                    .forEach(leaveApplicationList::add);
        }

        List<LeaveApplicationDashboardResponse> leaveApplicationResponseList = getPeopleOnLeaveResponse(leaveApplicationList, leaveTypeAlias);
        peopleOnLeaveResponse.setLeaveApplicationDashboardResponse(leaveApplicationResponseList);
        response.add(peopleOnLeaveResponse);

        return response;
    }

    private List<LeaveApplicationDashboardResponse> getPeopleOnLeaveResponse (List<LeaveApplication> leaveApplicationList, LeaveTypeAlias leaveTypeAlias) {
        List<LeaveApplicationDashboardResponse> leaveApplicationResponseList = new ArrayList<>();
        for (LeaveApplication leaveApplication : leaveApplicationList){
            LeaveApplicationDashboardResponse leaveApplicationResponse = new LeaveApplicationDashboardResponse();
            leaveApplicationResponse.setLeaveApplicationId(leaveApplication.getLeaveApplicationId());
            leaveApplicationResponse.setLeaveApplicationStatusId(leaveApplication.getLeaveApplicationStatusId());
            leaveApplicationResponse.setAccountId(leaveApplication.getAccountId());
            leaveApplicationResponse.setLeaveType(Objects.equals(leaveApplication.getLeaveTypeId(), TIME_OFF_LEAVE_TYPE_ID) ? leaveTypeAlias.getTimeOffAlias() : leaveTypeAlias.getSickLeaveAlias());
            leaveApplicationResponse.setFromDate(leaveApplication.getFromDate());
            leaveApplicationResponse.setToDate(leaveApplication.getToDate());
            leaveApplicationResponse.setLeaveReason(leaveApplication.getLeaveReason());
            leaveApplicationResponse.setApproverReason(leaveApplication.getApproverReason());
            leaveApplicationResponse.setApprover(userAccountRepository.findEmailByAccountId(leaveApplication.getApproverAccountId()));
            leaveApplicationResponse.setLeaveTypeId(leaveApplication.getLeaveTypeId());
            leaveApplicationResponseList.add(leaveApplicationResponse);
        }
        return leaveApplicationResponseList;
    }

    /**
     * This method iterates in accountId list and gets all the leave remaining for respective accountIds, prepare a report for orgAdmin and projectadmin
     * @param entityLeaveReportRequest takes accountIdList as not null field
     * @param accountIds
     * @return
     */
    public EntityLeaveReportResponse getEntityLeaveReport (EntityLeaveReportRequest entityLeaveReportRequest, String accountIds) {
        EntityLeaveReportResponse entityLeaveReportResponse = new EntityLeaveReportResponse();
        List<Long> accountIdsList = entityLeaveReportRequest.getAccountIdList();
        List<MemberLeaveReport> memberLeaveReportList = new ArrayList<>();
        for (Long accountId : accountIdsList) {
            LeaveRemainingHistoryRequest leaveRemainingHistoryRequest = new LeaveRemainingHistoryRequest();
            leaveRemainingHistoryRequest.setAccountId(accountId);
            leaveRemainingHistoryRequest.setYear((short) LocalDateTime.now().getYear());
            leaveRemainingHistoryRequest.setFromDate(entityLeaveReportRequest.getFromDate());
            leaveRemainingHistoryRequest.setToDate(entityLeaveReportRequest.getToDate());
            List<LeaveRemainingResponse> leaveRemainingResponseList = getLeavesRemaining(leaveRemainingHistoryRequest);
            if (entityLeaveReportRequest.getFromDate() == null) {
                entityLeaveReportRequest.setFromDate(leaveRemainingHistoryRequest.getFromDate());
            }
            if (entityLeaveReportRequest.getToDate() == null) {
                entityLeaveReportRequest.setToDate(leaveRemainingHistoryRequest.getToDate());
            }
            MemberLeaveReport memberLeaveReport = new MemberLeaveReport();
            memberLeaveReport.setLeaveRemainingResponseList(leaveRemainingResponseList);
            UserAccount userAccount = userAccountRepository.findByAccountIdAndIsActive(accountId, true);
            if (userAccount == null) {
                continue;
            }
            memberLeaveReport.setAccountId(accountId);
            memberLeaveReport.setEmail(userAccount.getEmail());
            memberLeaveReport.setFirstName(userAccount.getFkUserId().getFirstName());
            memberLeaveReport.setLastName(userAccount.getFkUserId().getLastName());
            if (entityLeaveReportRequest.getCalculateYearlyAllocation()) {
                updateYearlyAllocation(memberLeaveReport);
            }
            memberLeaveReportList.add(memberLeaveReport);
        }
        memberLeaveReportList.sort(
                Comparator
                        .comparing(MemberLeaveReport::getFirstName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(MemberLeaveReport::getLastName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(MemberLeaveReport::getEmail, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
        );

        entityLeaveReportResponse.setMemberLeaveReportList(memberLeaveReportList);
        entityLeaveReportResponse.setFromDate(entityLeaveReportRequest.getFromDate());
        entityLeaveReportResponse.setToDate(entityLeaveReportRequest.getToDate());
        return entityLeaveReportResponse;
    }

    private float getBreakDuration(Long teamId, Long orgId) {
        float lunchHours = Constants.LUNCH_HOUR;
        if (teamId != null) {
            Optional<EntityPreference> teamPreference = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.TEAM, teamId);
            if (teamPreference.isPresent() && teamPreference.get().getBreakDuration() != null) {
                lunchHours = (float) (teamPreference.get().getBreakDuration() / 60);
            }
        }
        if (orgId != null) {
            Optional<EntityPreference> orgPreference = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, orgId);
            if (orgPreference.isPresent() && orgPreference.get().getBreakDuration() != null) {
                lunchHours = (float) orgPreference.get().getBreakDuration() / 60;
            }
        }
        return lunchHours;
    }

    /**
     *This method will return user all policy report with total remaining sick leaves and time off leaves.
     */
    public AllUserPolicyReportResponse getUserPolicyReport (Integer entityTypeId, Long entityId, Integer pageNumber, Integer pageSize, String accountIds) {
        AllUserPolicyReportResponse allUserPolicyReportResponse = new AllUserPolicyReportResponse();
        List<UserLeaveReportResponse> fullUserLeaveReportList = new ArrayList<>();

        List<Long> accountIdsList = CommonUtils.convertToLongList(accountIds);
        Set<Long> userAccountIdsList = new HashSet<>(getAccountIdsForEntity(entityTypeId, entityId, accountIdsList));

        for (Long accountId : userAccountIdsList) {
            List<LeaveRemaining> leaveRemainingList = leaveRemainingRepository.findByAccountIdAndCalenderYear(accountId, (short) LocalDate.now().getYear());
            if (!leaveRemainingList.isEmpty()) {
                UserAccount userAccount = userAccountRepository.findByAccountIdAndIsActive(accountId, true);
                if (userAccount == null) continue;

                UserLeaveReportResponse userLeaveReportResponse = new UserLeaveReportResponse();
                Float sickLeaves = 0F;
                Float timeOffLeaves = 0F;

                userLeaveReportResponse.setAccountDetails(new AccountDetails(
                        userAccount.getAccountId(),
                        userAccount.getFkUserId().getPrimaryEmail(),
                        userAccount.getFkUserId().getFirstName(),
                        userAccount.getFkUserId().getLastName()
                ));

                for (LeaveRemaining leaveRemaining : leaveRemainingList) {
                    if (Objects.equals(leaveRemaining.getLeaveTypeId(), SICK_LEAVE_TYPE_ID)) {
                        sickLeaves = leaveRemaining.getLeaveRemaining();
                        if (userLeaveReportResponse.getStartLeavePolicyOn() == null) {
                            userLeaveReportResponse.setStartLeavePolicyOn(leaveRemaining.getStartLeavePolicyOn());
                            userLeaveReportResponse.setIsDateEditable(!leaveRemaining.getStartLeavePolicyUsedOnce());
                        }
                    } else if (Objects.equals(leaveRemaining.getLeaveTypeId(), TIME_OFF_LEAVE_TYPE_ID)) {
                        timeOffLeaves = leaveRemaining.getLeaveRemaining();
                        if (userLeaveReportResponse.getStartLeavePolicyOn() == null) {
                            userLeaveReportResponse.setStartLeavePolicyOn(leaveRemaining.getStartLeavePolicyOn());
                            userLeaveReportResponse.setIsDateEditable(!leaveRemaining.getStartLeavePolicyUsedOnce());
                        }
                    }
                }

                userLeaveReportResponse.setSickLeaves(sickLeaves);
                userLeaveReportResponse.setTimeOffLeaves(timeOffLeaves);
                fullUserLeaveReportList.add(userLeaveReportResponse);
            }
        }

        // Sort the full list
        fullUserLeaveReportList.sort(Comparator
                .comparing((UserLeaveReportResponse res) -> res.getAccountDetails() != null ? res.getAccountDetails().getFirstName() : null, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(res -> res.getAccountDetails() != null ? res.getAccountDetails().getLastName() : null, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(res -> res.getAccountDetails() != null ? res.getAccountDetails().getEmail() : null, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
        );

        // Apply pagination on the sorted result
        int totalLeaveReportSize = fullUserLeaveReportList.size();
        int startIndex = pageNumber * pageSize;
        int endIndex = min(startIndex + pageSize, totalLeaveReportSize);
        List<UserLeaveReportResponse> paginatedList = (startIndex < endIndex) ? fullUserLeaveReportList.subList(startIndex, endIndex) : Collections.emptyList();

        allUserPolicyReportResponse.setTotalUserPolicyReport(totalLeaveReportSize);
        allUserPolicyReportResponse.setUserLeaveReportResponseList(paginatedList);

        return allUserPolicyReportResponse;
    }


    /**
     * This method updates the user policy in leave remaining table for updated dates and leaves
     */
    public UserLeaveReportResponse updateUserPolicy (UpdateLeavePolicyForUsersRequest request) {
        UserLeaveReportResponse userLeaveReportResponse = new UserLeaveReportResponse();
        List<LeaveRemaining> leaveRemainingList = leaveRemainingRepository.findByAccountIdAndCalenderYear(request.getAccountId(), (short) LocalDate.now().getYear());
        if (leaveRemainingList.isEmpty()) {
            throw new LeaveApplicationValidationException("Please assign a leave policy to user before updating the provided fields");
        }
        UserAccount userAccount = userAccountRepository.findByAccountIdAndIsActive(request.getAccountId(), true);
        Float sickLeaves = request.getSickLeaves();
        Float timeOffLeaves = request.getTimeOffLeaves();
        YearMonth yearMonth;
        LocalDate lastDateOfMonth = null;
        if (request.getStartLeavePolicyOn() != null) {
            yearMonth = YearMonth.from(request.getStartLeavePolicyOn());
            lastDateOfMonth = yearMonth.atEndOfMonth();
        }
        userLeaveReportResponse.setAccountDetails(new AccountDetails(userAccount.getAccountId(), userAccount.getFkUserId().getPrimaryEmail()
                , userAccount.getFkUserId().getFirstName(), userAccount.getFkUserId().getLastName()));
        for (LeaveRemaining leaveRemaining : leaveRemainingList) {
            if (Objects.equals(leaveRemaining.getLeaveTypeId(), SICK_LEAVE_TYPE_ID)) {
                if (sickLeaves != null && !Objects.equals(sickLeaves, leaveRemaining.getLeaveRemaining())) {
                    leaveRemaining.setLeaveRemaining(sickLeaves);
                }
                userLeaveReportResponse.setSickLeaves(leaveRemaining.getLeaveRemaining());
            } else if (Objects.equals(leaveRemaining.getLeaveTypeId(), TIME_OFF_LEAVE_TYPE_ID)) {
                if (timeOffLeaves != null && !Objects.equals(timeOffLeaves, leaveRemaining.getLeaveRemaining())) {
                    leaveRemaining.setLeaveRemaining(timeOffLeaves);
                }
                userLeaveReportResponse.setTimeOffLeaves(leaveRemaining.getLeaveRemaining());
            } else {
                throw new ValidationFailedException("Please provide a valid leave type");
            }
            if (request.getStartLeavePolicyOn() != null && lastDateOfMonth != null
                    && request.getStartLeavePolicyOn().toLocalDate().isBefore(lastDateOfMonth) && !leaveRemaining.getStartLeavePolicyUsedOnce()) {
                leaveRemaining.setStartLeavePolicyOn(request.getStartLeavePolicyOn());
            }
            userLeaveReportResponse.setIsDateEditable(!leaveRemaining.getStartLeavePolicyUsedOnce());
            userLeaveReportResponse.setStartLeavePolicyOn(leaveRemaining.getStartLeavePolicyOn());
        }
        leaveRemainingRepository.saveAll(leaveRemainingList);

        return userLeaveReportResponse;
    }

    public void sendNotificationForLeaveCancellation (Long notifyToAccount, LeaveApplication leaveApplication, LeaveApplicationNotificationRequest leaveApplicationNotificationRequest, Long cancelledByAccountId) {
        leaveApplicationNotificationRequest.setSendNotification(true);
        leaveApplicationNotificationRequest.setLeaveApplicationId(leaveApplication.getLeaveApplicationId());
        leaveApplicationNotificationRequest.setNotificationFor(leaveApplicationStatusRepository.findByLeaveApplicationStatusId(leaveApplication.getLeaveApplicationStatusId()).getLeaveApplicationStatus());
        if (leaveApplication.getNotifyTo() != null && leaveApplication.getNotifyTo().length() > 2) {
            int l = leaveApplication.getNotifyTo().length();
            List<Long> notifyTo = Stream.of(leaveApplication.getNotifyTo().substring(1, l - 1).split(", "))
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
            notifyTo.add(notifyToAccount);
            leaveApplicationNotificationRequest.setNotifyTo(notifyTo);
        } else {
            leaveApplicationNotificationRequest.setNotifyTo(List.of(notifyToAccount));
        }
        leaveApplicationNotificationRequest.setFromDate(leaveApplication.getFromDate().toString());
        leaveApplicationNotificationRequest.setToDate(leaveApplication.getToDate().toString());
        leaveApplicationNotificationRequest.setApplicantAccountId(leaveApplication.getAccountId());
        if (Objects.equals(leaveApplication.getLeaveApplicationStatusId(), Constants.Leave.CANCELLED_AFTER_APPROVAL_LEAVE_APPLICATION_STATUS_ID))
            leaveApplicationNotificationRequest.setApproverAccountId(leaveApplication.getApproverAccountId());
        else leaveApplicationNotificationRequest.setApproverAccountId(cancelledByAccountId);
    }

    public String getNativeQuery (LeaveWithFilterRequest leaveWithFilterRequest) {

        String nativeQuery = "SELECT * FROM tse.leave_application WHERE leave_type_id in :leaveType ";

        if (leaveWithFilterRequest.getApproverAccountId() != null) {
            nativeQuery += "AND approver_account_id = :approverAccountId ";
        }
        if (leaveWithFilterRequest.getApplicationStatusIds() != null && !leaveWithFilterRequest.getApplicationStatusIds().isEmpty()) {
            nativeQuery += "AND leave_application_status_id in :applicationStatusIds ";
        }
        if (leaveWithFilterRequest.getAccountIdList() != null && !leaveWithFilterRequest.getAccountIdList().isEmpty()) {
            nativeQuery += "AND account_id in :accountIds ";
        }
        if (leaveWithFilterRequest.getFromDate() != null) {
            nativeQuery += "AND (from_date >= :fromDate OR to_date >= :fromDate) ";
        }
        if (leaveWithFilterRequest.getToDate() != null) {
            nativeQuery += "AND (from_date <= :toDate OR to_date <= :toDate) ";
        }

        return nativeQuery;
    }

    public void setQueryParameters (LeaveWithFilterRequest leaveWithFilterRequest, Query query) {
        query.setParameter("leaveType", leaveWithFilterRequest.getLeaveTypeList());
        if (leaveWithFilterRequest.getApproverAccountId() != null) {
            query.setParameter("approverAccountId", leaveWithFilterRequest.getApproverAccountId());
        }
        if (leaveWithFilterRequest.getApplicationStatusIds() != null && !leaveWithFilterRequest.getApplicationStatusIds().isEmpty()) {
            query.setParameter("applicationStatusIds", leaveWithFilterRequest.getApplicationStatusIds());
        }
        if (leaveWithFilterRequest.getAccountIdList() != null && !leaveWithFilterRequest.getAccountIdList().isEmpty()) {
            query.setParameter("accountIds", leaveWithFilterRequest.getAccountIdList());
        }
        if (leaveWithFilterRequest.getFromDate() != null) {
            query.setParameter("fromDate", leaveWithFilterRequest.getFromDate());
        }
        if (leaveWithFilterRequest.getToDate() != null) {
            query.setParameter("toDate", leaveWithFilterRequest.getToDate());
        }
    }

    private LeaveApplicationResponse getLeaveApplicationResponse (LeaveApplication leaveApplication, LeaveTypeAlias leaveTypeAlias) {
        LeaveApplicationResponse leaveApplicationResponse = new LeaveApplicationResponse();
        leaveApplicationResponse.setLeaveApplicationId(leaveApplication.getLeaveApplicationId());
        leaveApplicationResponse.setLeaveApplicationStatusId(leaveApplication.getLeaveApplicationStatusId());
        leaveApplicationResponse.setApplicantDetails(userAccountRepository.getEmailFirstNameLastNameAccountIdIsActiveByAccountId(leaveApplication.getAccountId()));
        leaveApplicationResponse.setLeaveType(Objects.equals(leaveApplication.getLeaveTypeId(), TIME_OFF_LEAVE_TYPE_ID) ? leaveTypeAlias.getTimeOffAlias() : leaveTypeAlias.getSickLeaveAlias());
        leaveApplicationResponse.setLeaveTypeId(leaveApplication.getLeaveTypeId());
        leaveApplicationResponse.setFromDate(leaveApplication.getFromDate());
        leaveApplicationResponse.setToDate(leaveApplication.getToDate());
        leaveApplicationResponse.setIncludeLunchTime(leaveApplication.getIncludeLunchTime());
        leaveApplicationResponse.setLeaveReason(leaveApplication.getLeaveReason());
        leaveApplicationResponse.setApproverReason(leaveApplication.getApproverReason());
        leaveApplicationResponse.setApprover(userAccountRepository.getEmailFirstNameLastNameAccountIdIsActiveByAccountId(leaveApplication.getApproverAccountId()));
        leaveApplicationResponse.setPhone(leaveApplication.getPhone());
        leaveApplicationResponse.setAddress(leaveApplication.getAddress());
        leaveApplicationResponse.setIsLeaveForHalfDay(leaveApplication.getIsLeaveForHalfDay());
        leaveApplicationResponse.setHalfDayLeaveType(leaveApplication.getHalfDayLeaveType());
        leaveApplicationResponse.setLeaveCancellationReason(leaveApplication.getLeaveCancellationReason());
        leaveApplicationResponse.setApplicationDate(leaveApplication.getCreatedDateTime().toLocalDate());
        if(leaveApplication.getNotifyTo()!=null){
            if(leaveApplication.getNotifyTo().length() >2){
                int l=leaveApplication.getNotifyTo().length();
                List<Long> notifyTo= Stream.of(leaveApplication.getNotifyTo().substring(1,l-1).split(", "))
                        .map(Long::parseLong)
                        .collect(Collectors.toList());
                leaveApplicationResponse.setNotifyTo(notifyTo);
            }else{
                leaveApplicationResponse.setNotifyTo(Collections.emptyList());
            }
        }
        if(leaveApplication.getDoctorCertificate()!=null)
        {
            DoctorCertificate doctorCertificate=new DoctorCertificate();
            doctorCertificate.setFileName(leaveApplication.getDoctorCertificateFileName());
            doctorCertificate.setFileType(leaveApplication.getDoctorCertificateFileType());
            doctorCertificate.setFileSize(leaveApplication.getDoctorCertificateFileSize());
            leaveApplicationResponse.setDoctorCertificate(doctorCertificate);
        }
        else {
            leaveApplicationResponse.setDoctorCertificate(null);
        }
        leaveApplicationResponse.setNumberOfLeaveDays(leaveApplication.getNumberOfLeaveDays());
        return leaveApplicationResponse;
    }

    public Float calculateYearlyAllocation(LeaveRemainingResponse leaveRemainingResponse) {
        LocalDate policyStartDate = leaveRemainingResponse.getStartLeavePolicyOn().toLocalDate();
        Float initialLeaves = leavePolicyRepository.findInitialLeavesByLeavePolicyId(leaveRemainingResponse.getLeavePolicyId());
        float monthlyLeave = initialLeaves / 12;

        // First month pro-rate leave
        YearMonth startMonth = YearMonth.of(policyStartDate.getYear(), policyStartDate.getMonth());
        int totalDaysInStartMonth = startMonth.lengthOfMonth();
        int activeDaysInStartMonth = totalDaysInStartMonth - policyStartDate.getDayOfMonth() + 1;
        float firstMonthProRate = (activeDaysInStartMonth / (float) totalDaysInStartMonth) * monthlyLeave;

        // Full months after start month till December
        int monthsRemaining = 12 - policyStartDate.getMonthValue();

        return firstMonthProRate + (monthsRemaining * monthlyLeave);
    }

    public void updateYearlyAllocation (MemberLeaveReport memberLeaveReport) {
        Float yearlyAllocation = 0F;
        Float allocationTillNow = 0F;
        Float rejectedLeaves = 0F;
        Float cancelledLeaves = 0F;
        Float expiredLeaves = 0F;
        for (LeaveRemainingResponse leaveRemainingResponse : memberLeaveReport.getLeaveRemainingResponseList()) {
            LocalDateTime policyStartDate = leaveRemainingResponse.getStartLeavePolicyOn();
            LocalDate startOfYear = LocalDate.of(policyStartDate.getYear(), 1, 1);
            yearlyAllocation += calculateYearlyAllocation(leaveRemainingResponse);
            allocationTillNow += leaveRemainingResponse.getLeaveTaken() + leaveRemainingResponse.getLeaveRemaining();
            rejectedLeaves += leaveApplicationRepository.findCountByLeaveTypeAndApplicationStatusIdInAndYear(leaveRemainingResponse.getAccountId(), List.of(REJECTED_LEAVE_APPLICATION_STATUS_ID), leaveRemainingResponse.getFromDate(), leaveRemainingResponse.getToDate(),leaveRemainingResponse.getLeaveTypeId());
            cancelledLeaves += leaveApplicationRepository.findCountByLeaveTypeAndApplicationStatusIdInAndYear(leaveRemainingResponse.getAccountId(), List.of(CANCELLED_AFTER_APPROVAL_LEAVE_APPLICATION_STATUS_ID, CANCELLED_LEAVE_APPLICATION_STATUS_ID), leaveRemainingResponse.getFromDate(), leaveRemainingResponse.getToDate(),leaveRemainingResponse.getLeaveTypeId());
            expiredLeaves += leaveApplicationRepository.findCountByLeaveTypeAndApplicationStatusIdInAndYear(leaveRemainingResponse.getAccountId(), List.of(LEAVE_APPLICATION_EXPIRED_STATUS_ID), leaveRemainingResponse.getFromDate(), leaveRemainingResponse.getToDate(),leaveRemainingResponse.getLeaveTypeId());
        }
        memberLeaveReport.setCancelledLeaves(cancelledLeaves);
        memberLeaveReport.setYearlyAllocation(yearlyAllocation);
        memberLeaveReport.setAllocationTillNow(allocationTillNow);
        memberLeaveReport.setRejectedLeaves(rejectedLeaves);
        memberLeaveReport.setExpiredLeaves(expiredLeaves);
    }

    public Float getUpcomingLeavesCount(LeaveWithFilterRequest leaveWithFilterRequest){
        String nativeQuery = getNativeQuery(leaveWithFilterRequest);
        Query query = entityManager.createNativeQuery(nativeQuery, LeaveApplication.class);
        setQueryParameters(leaveWithFilterRequest, query);
        List<LeaveApplication> leaveApplicationList = query.getResultList();
        Float upcomingLeaves = 0F;
        for (LeaveApplication leaveApplication : leaveApplicationList) {
            upcomingLeaves = upcomingLeaves + (leaveApplication.getNumberOfLeaveDays() != null ? leaveApplication.getNumberOfLeaveDays() : 0);
        }
        return upcomingLeaves;
    }

    public List<TeamDetails> getTeamDetailsForEntity (Integer entityTypeId, Long entityId, List<Long> accountIds) {
        List<TeamDetails> teamDetails = new ArrayList<>();
        List<Integer> roleIds = Constants.rolesToViewPeopleOnLeave;
        if (Objects.equals(entityTypeId, Constants.EntityTypes.ORG)) {
            teamDetails.addAll(accessDomainRepository.findByOrgIdAccountIdInAndRoleIdInAndIsActive(accountIds, roleIds, true, entityId));
        } else if (Objects.equals(entityTypeId, Constants.EntityTypes.BU)) {
            teamDetails.addAll(accessDomainRepository.findByBuIdAccountIdInAndRoleIdInAndIsActive(accountIds, roleIds, true, entityId));
        } else if (Objects.equals(entityTypeId, Constants.EntityTypes.PROJECT)) {
            teamDetails.addAll(accessDomainRepository.findByProjectIdAccountIdInAndRoleIdInAndIsActive(accountIds, roleIds, true, entityId));
        } else if (Objects.equals(entityTypeId, Constants.EntityTypes.TEAM)) {
            teamDetails.addAll(accessDomainRepository.findByTeamIdAccountIdInAndRoleIdInAndIsActive(accountIds, roleIds, true, entityId));
        } else {
            throw new LeaveApplicationValidationException("Please provide a valid entity type.");
        }
        return teamDetails;
    }

    public void validateDateOfLeave (LeaveApplicationRequest leaveApplicationRequest, Long orgId) {
        boolean checkNonBusinessDays;
        if(Constants.Leave.SICK_LEAVES.contains(leaveApplicationRequest.getLeaveSelectionTypeId())) {
            checkNonBusinessDays = leavePolicyRepository.findByLeavePolicyId(leaveRemainingRepository.findByAccountIdAndLeaveType(leaveApplicationRequest.getAccountId(), SICK_LEAVE_TYPE_ID).getLeavePolicyId()).getIncludeNonBusinessDaysInLeave();
        }
        else {
            checkNonBusinessDays = leavePolicyRepository.findByLeavePolicyId(leaveRemainingRepository.findByAccountIdAndLeaveType(leaveApplicationRequest.getAccountId(), TIME_OFF_LEAVE_TYPE_ID).getLeavePolicyId()).getIncludeNonBusinessDaysInLeave();
        }
        if (Objects.equals(leaveApplicationRequest.getToDate(), leaveApplicationRequest.getFromDate())) {
            if (!checkNonBusinessDays) {
                if (timeSheetService.validateHolidayDate(leaveApplicationRequest.getAccountId(), leaveApplicationRequest.getToDate()) || entityPreferenceService.validateWeekendDate(orgId, leaveApplicationRequest.getToDate())) {
                    throw new LeaveApplicationValidationException ("Leave can't be applied on business off-days or holidays.");
                }
            }
        }
        else {
            if (!checkNonBusinessDays) {
                if (timeSheetService.validateHolidayDate(leaveApplicationRequest.getAccountId(), leaveApplicationRequest.getToDate()) || entityPreferenceService.validateWeekendDate(orgId, leaveApplicationRequest.getToDate()) ||
                        timeSheetService.validateHolidayDate(leaveApplicationRequest.getAccountId(), leaveApplicationRequest.getFromDate()) || entityPreferenceService.validateWeekendDate(orgId, leaveApplicationRequest.getFromDate())) {
                    throw new LeaveApplicationValidationException ("Leave's From Date and To Date can't be business off-day or holiday");
                }
            }
        }
    }

    private List<Long> getAccountIdsForEntity (Integer entityTypeId, Long entityId, List<Long> accountIdsList) {
        List<Long> entityIdList = new ArrayList<>();
        Set<Long> userAccountIdList = new HashSet<>();
        List<Integer> roleList = Constants.rolesToViewPeopleOnLeave;
        if (Objects.equals(entityTypeId, Constants.EntityTypes.TEAM)) {
            if (accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.TEAM, entityId, accountIdsList, roleList, true)) {
                userAccountIdList.addAll(
                        new HashSet<>(accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdAndIsActive(Constants.EntityTypes.TEAM, entityId, true))
                );
            }
        } else if (Objects.equals(entityTypeId, Constants.EntityTypes.PROJECT)) {
            if (accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.PROJECT, entityId, accountIdsList, roleList, true)) {
                userAccountIdList.addAll(getprojectMembersAccountIdList(List.of(entityId)).stream().map(AccountId::getAccountId).collect(Collectors.toSet()));
            }
            else {
                entityIdList.addAll(accessDomainRepository.findByAccountIdInAndRoleIdInAndProjectIdAndIsActive(accountIdsList, roleList, entityId, true));
                userAccountIdList.addAll(accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdInAndIsActive(Constants.EntityTypes.TEAM, entityIdList, true).stream().map(AccountId::getAccountId).collect(Collectors.toSet()));
            }
        } else if (Objects.equals(entityTypeId, Constants.EntityTypes.ORG)) {
            if (accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.ORG, entityId, accountIdsList, roleList, true)) {
                userAccountIdList.addAll(userAccountRepository.findAccountIdByOrgIdAndIsActive(entityId, true).stream().map(AccountId::getAccountId).collect(Collectors.toSet()));
            }
            else {
                List<Long> allProjectIdOfOrg = projectRepository.findByOrgId(entityId).stream()
                        .map(ProjectIdProjectName::getProjectId)
                        .collect(Collectors.toList());
                List<Long> projectIdList = accessDomainRepository.findDistinctEntityIdByEntityTypeIdAndEntityIdInAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.PROJECT, allProjectIdOfOrg, accountIdsList, roleList, true);
                if (projectIdList != null && !projectIdList.isEmpty()) {
                    userAccountIdList.addAll(getprojectMembersAccountIdList(projectIdList).stream().map(AccountId::getAccountId).collect(Collectors.toSet()));
                }
                entityIdList.addAll(accessDomainRepository.findByAccountIdInAndRoleIdInAndOrgIdAndIsActive(accountIdsList, roleList, entityId, true));
                userAccountIdList.addAll(accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdInAndIsActive(Constants.EntityTypes.TEAM, entityIdList, true).stream().map(AccountId::getAccountId).collect(Collectors.toSet()));
            }
        } else {
            throw new EntityNotFoundException("Please provide a valid entity");
        }
        return new ArrayList<>(userAccountIdList);
    }

    private List<AccountId> getprojectMembersAccountIdList (List<Long> projectIdList) {
        List<AccountId> accountIdListProject = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdInAndIsActive(Constants.EntityTypes.PROJECT, projectIdList, true);
        List<Long> teamIdList = teamRepository.findTeamIdsByFkProjectIdProjectIdIn(projectIdList);
        List<AccountId> teamAccountIds = accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdInAndIsActive(Constants.EntityTypes.TEAM, teamIdList, true);
        Set<AccountId> combinedList = new HashSet<>(accountIdListProject);
        combinedList.addAll(teamAccountIds);

        return new ArrayList<>(combinedList);
    }

    public GetUsersLeaveDetailsResponse getLeaveDetails(
            LeaveApplicationDetailsRequest req, Integer pageNumber, Integer pageSize, String accountIds, String timeZone
    ) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        List<Long> accountIdList = req.getAccountIdList();
        List<Long> approverAccountIdList = req.getApproverAccountIdList();
        List<Short> leaveStatusIdList = req.getLeaveStatusIdList();

        // Use custom repo call
        Page<Object[]> leaveApplsPage = leaveApplicationRepository.findLeaveApplicationsExpanded(req.getOrgId(), req.getFromDate(), req.getToDate(), accountIdList, approverAccountIdList, leaveStatusIdList, Sort.unsorted(), Pageable.unpaged()
        );

        // Per-day expansion
        List<LeaveApplicationDetails> allRows = createLeaveApplicationDetailsList(req.getOrgId(), leaveApplsPage.getContent(), req.getFromDate(), req.getToDate(), timeZone);
        int totalCount = allRows.size();

        // Sorting
        Comparator<LeaveApplicationDetails> comparator;
        if (Boolean.TRUE.equals(req.getSortOnName())) {
            comparator = Comparator.comparing(LeaveApplicationDetails::getApplicantFirstName, Comparator.nullsFirst(String::compareTo))
                    .thenComparing(LeaveApplicationDetails::getApplicantLastName, Comparator.nullsFirst(String::compareTo))
                    .thenComparing(LeaveApplicationDetails::getApplicantEmail, Comparator.nullsFirst(String::compareTo))
                    .thenComparing(LeaveApplicationDetails::getFromDate)
                    .thenComparing(LeaveApplicationDetails::getLeaveApplicationStatusId);
        } else {
            comparator = Comparator.comparing(LeaveApplicationDetails::getFromDate)
                    .thenComparing(LeaveApplicationDetails::getApplicantFirstName, Comparator.nullsFirst(String::compareTo))
                    .thenComparing(LeaveApplicationDetails::getApplicantLastName, Comparator.nullsFirst(String::compareTo))
                    .thenComparing(LeaveApplicationDetails::getApplicantEmail, Comparator.nullsFirst(String::compareTo))
                    .thenComparing(LeaveApplicationDetails::getLeaveApplicationStatusId);
        }
        allRows.sort(comparator);

        // Pagination
        int fromIdx = pageNumber * pageSize;
        int toIdx = Math.min((pageNumber + 1) * pageSize, allRows.size());
        List<LeaveApplicationDetails> paged = (fromIdx >= toIdx) ? Collections.emptyList() : allRows.subList(fromIdx, toIdx);

        // Assemble response
        GetUsersLeaveDetailsResponse resp = new GetUsersLeaveDetailsResponse();
        resp.setLeaveApplicationDetailsList(paged);
        resp.setNumberOfLeaveCount(totalCount);
        return resp;
    }

//    private List<LeaveApplicationDetails> expandToPerDay(
//            Long orgId, List<Object[]> partialResult, LocalDate requestFromDate, LocalDate requestToDate, String timeZone) {
//
//        List<LeaveApplicationDetails> expandedDetails = new ArrayList<>();
//
//        List<Integer> offDays = fetchOrgOffDays(orgId);
//        Map<Short, String> leaveTypeAliasMap = fetchLeaveTypeAliasMap(orgId);
//
//        Map<Long, Boolean> leavePolicyCache = new HashMap<>();
//        Map<Pair<Long, Short>, LeaveRemaining> leaveRemainingMap = fetchLeaveRemainingMap(partialResult);
//
//        for (Object[] row : partialResult) {
//            LeaveApplication la = (LeaveApplication) row[0];
//            UserAccount uaApplicant = (UserAccount) row[1];
//            User uApplicant = (User) row[2];
//            UserAccount uaApprover = (UserAccount) row[3];
//            User uApprover = (User) row[4];
//
//            Pair<Long, Short> key = Pair.of(la.getAccountId(), la.getLeaveTypeId());
//            LeaveRemaining leaveRemaining = leaveRemainingMap.get(key);
//            if (leaveRemaining == null) continue;
//
//            Boolean includeNonBusiness = getIncludeNonBusinessForPolicy(leaveRemaining.getLeavePolicyId(), leavePolicyCache);
//
//            LocalDate start = la.getFromDate().isBefore(requestFromDate) ? requestFromDate : la.getFromDate();
//            LocalDate end = la.getToDate().isAfter(requestToDate) ? requestToDate : la.getToDate();
//
//            for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
//                if (Boolean.FALSE.equals(includeNonBusiness) && !offDays.isEmpty()
//                        && offDays.contains(d.getDayOfWeek().getValue())) {
//                    continue; // Exclude off days if leave policy excludes non-business days
//                }
//                expandedDetails.add(buildLeaveApplicationDetails(la, uaApplicant, uApplicant, uaApprover, uApprover, d, leaveTypeAliasMap, timeZone));
//            }
//        }
//        return expandedDetails;
//    }

    private List<LeaveApplicationDetails> createLeaveApplicationDetailsList(
            Long orgId, List<Object[]> partialResult, LocalDate requestFromDate, LocalDate requestToDate, String timeZone) {

        List<LeaveApplicationDetails> expandedDetails = new ArrayList<>();

        Map<Short, String> leaveTypeAliasMap = fetchLeaveTypeAliasMap(orgId);

        for (Object[] row : partialResult) {
            LeaveApplication la = (LeaveApplication) row[0];
            UserAccount uaApplicant = (UserAccount) row[1];
            User uApplicant = (User) row[2];
            UserAccount uaApprover = (UserAccount) row[3];
            User uApprover = (User) row[4];

            expandedDetails.add(buildLeaveApplicationDetails(la, uaApplicant, uApplicant, uaApprover, uApprover, leaveTypeAliasMap, timeZone));
        }
        return expandedDetails;
    }


    private List<Integer> fetchOrgOffDays(Long orgId) {
        return entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, orgId)
                .map(EntityPreference::getOffDays)
                .orElse(Collections.emptyList());
    }

    /**
     * Batch fetch LeaveRemaining for all distinct (accountId, leaveTypeId) pairs.
     */
//    private Map<Pair<Long, Short>, LeaveRemaining> fetchLeaveRemainingMap(List<Object[]> partialResult) {
//        Set<Pair<Long, Short>> keys = partialResult.stream()
//                .map(row -> {
//                    LeaveApplication la = (LeaveApplication) row[0];
//                    return Pair.of(la.getAccountId(), la.getLeaveTypeId());
//                })
//                .collect(Collectors.toSet());
//
//        List<Long> accountIds = keys.stream().map(Pair::getFirst).distinct().collect(Collectors.toList());
//        List<Short> leaveTypeIds = keys.stream().map(Pair::getSecond).distinct().collect(Collectors.toList());
//
//        short currentYear = (short) LocalDate.now().getYear();
//
//        List<LeaveRemaining> leaveRemainingList = leaveRemainingRepository.findByAccountIdInAndLeaveTypeIdInAndCalenderYear(
//                accountIds, leaveTypeIds, currentYear);
//
//        return leaveRemainingList.stream()
//                .collect(Collectors.toMap(
//                        lr -> Pair.of(lr.getAccountId(), lr.getLeaveTypeId()),
//                        Function.identity()
//                ));
//    }

//    private Boolean getIncludeNonBusinessForPolicy(Long leavePolicyId, Map<Long, Boolean> cache) {
//        if (leavePolicyId == null) return Boolean.TRUE;
//
//        if (cache.containsKey(leavePolicyId)) {
//            return cache.get(leavePolicyId);
//        } else {
//            LeavePolicy lp = leavePolicyRepository.findById(leavePolicyId).orElse(null);
//            Boolean include = (lp != null) ? lp.getIncludeNonBusinessDaysInLeave() : Boolean.TRUE;
//            cache.put(leavePolicyId, include);
//            return include;
//        }
//    }

    private LeaveApplicationDetails buildLeaveApplicationDetails(
            LeaveApplication leaveApplication, UserAccount uaApplicant, User uApplicant,
            UserAccount uaApprover, User uApprover,
            Map<Short, String> leaveTypeAliasMap, String timeZone) {

        LeaveApplicationDetails details = new LeaveApplicationDetails();

        LeaveApplication la = new LeaveApplication();
        BeanUtils.copyProperties(leaveApplication, la);

        details.setLeaveApplicationId(la.getLeaveApplicationId());
        details.setAccountId(la.getAccountId());
        details.setLeaveTypeId(la.getLeaveTypeId());
        details.setLeaveApplicationStatusId(la.getLeaveApplicationStatusId());
        details.setFromDate(la.getFromDate());
        details.setToDate(la.getToDate());
        details.setIncludeLunchTime(la.getIncludeLunchTime());
        details.setLeaveReason(la.getLeaveReason());
        details.setApproverReason(la.getApproverReason());
        details.setApproverAccountId(la.getApproverAccountId());
        details.setPhone(la.getPhone());
        details.setCreatedDateTime(DateTimeUtils.convertServerDateToLocalTimezone(la.getCreatedDateTime(), timeZone));
        details.setLastUpdatedDateTime(DateTimeUtils.convertServerDateToLocalTimezone(la.getLastUpdatedDateTime(), timeZone));
        details.setIsLeaveForHalfDay(la.getIsLeaveForHalfDay());
        details.setNumberOfLeaveDays(la.getNumberOfLeaveDays());
        details.setLeaveCancellationReason(la.getLeaveCancellationReason());
        if (la.getDoctorCertificateFileName() != null && !Boolean.FALSE.equals(la.getIsAttachmentPresent())) {
            details.setIsAttachmentPresent(true);
        }
        // Set leaveTypeAlias from map using leaveTypeId
        details.setLeaveTypeAlias(leaveTypeAliasMap.getOrDefault(la.getLeaveTypeId(), "Unknown Leave Type"));

        if (uApplicant != null) {
            details.setApplicantFirstName(uApplicant.getFirstName());
            details.setApplicantLastName(uApplicant.getLastName());
        }
        if (uaApplicant != null) {
            details.setApplicantEmail(uaApplicant.getEmail());
        }
        if (uApprover != null) {
            details.setApproverFirstName(uApprover.getFirstName());
            details.setApproverLastName(uApprover.getLastName());
        }
        if (uaApprover != null) {
            details.setApproverEmail(uaApprover.getEmail());
        }
        details.setDate(la.getFromDate());
        return details;
    }


    private Map<Short, String> fetchLeaveTypeAliasMap(Long orgId) {
        return entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, orgId)
                .map(pref -> {
                    Map<Short, String> map = new HashMap<>();
                    // leaveTypeId 1 = timeOffAlias
                    // leaveTypeId 2 = sickLeaveAlias
                    if (pref.getTimeOffAlias() != null) {
                        map.put((short) 1, pref.getTimeOffAlias());
                    }
                    if (pref.getSickLeaveAlias() != null) {
                        map.put((short) 2, pref.getSickLeaveAlias());
                    }
                    return map;
                }).orElse(Collections.emptyMap());
    }

    private float calculateNextYearLeaveDays(Long accountId, Short leaveTypeId, float officeHours) throws ParseException {
        LocalDate startOfNextYear = LocalDate.of(LocalDate.now().getYear() + 1, 1, 1);  // 2026-01-01
        LocalDate endOfNextYear = LocalDate.of(LocalDate.now().getYear() + 1, 12, 31); // 2026-12-31
        List<Short> statusIds = List.of(
                APPROVED_LEAVE_APPLICATION_STATUS_ID,
                WAITING_APPROVAL_LEAVE_APPLICATION_STATUS_ID,
                CONSUMED_LEAVE_APPLICATION_STATUS_ID
        );
        List<LeaveApplication> leaves = leaveApplicationRepository.findNextYearLeaves(
                accountId,
                leaveTypeId,
                statusIds,
                startOfNextYear,
                endOfNextYear
        );
        float totalHours = 0f;
        for (LeaveApplication leaveApplication : leaves) {
            LocalDate adjustedFromDate = leaveApplication.getFromDate();
            // If fromDate is before 1 Jan 2026, adjust it to 1 Jan 2026
            if (adjustedFromDate.isBefore(startOfNextYear)) {
                adjustedFromDate = startOfNextYear;
                totalHours += totalLeaveHours(adjustedFromDate, leaveApplication.getToDate(), accountId, leaveTypeId, officeHours);
            }
            else {
                totalHours += leaveApplication.getNumberOfLeaveDays()*officeHours;
            }
        }
        return totalHours/officeHours;
    }
}