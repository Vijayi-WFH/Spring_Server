package com.tse.core_application.service.Impl;


import com.tse.core_application.constants.ControllerConstants;
import com.tse.core_application.constants.RoleEnum;
import com.tse.core_application.custom.model.AccountId;
import com.tse.core_application.custom.model.EmailFirstLastAccountId;
import com.tse.core_application.custom.model.LeaveTypeAlias;
import com.tse.core_application.custom.model.ProjectIdProjectName;
import com.tse.core_application.dto.*;
import com.tse.core_application.dto.leave.DoctorCertificate;
import com.tse.core_application.dto.leave.DoctorCertificateMetaData;
import com.tse.core_application.dto.leave.Request.*;
import com.tse.core_application.dto.leave.Response.*;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.model.*;
import com.tse.core_application.repository.*;
import com.tse.core_application.utils.CommonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;


import javax.persistence.EntityNotFoundException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.DataFormatException;

import static com.tse.core_application.model.Constants.ExpiryDays.BACKWARD_DAYS;
import static com.tse.core_application.model.Constants.ExpiryDays.FORWARD_DAYS;
import static com.tse.core_application.model.Constants.Payroll.DEFAULT_PAYROLL_DAY;

@Service
public class LeaveService {

    private static final Logger logger = LogManager.getLogger(LeaveService.class.getName());

    @Value("${tseHr.application.root.path}")
    private String tseHrBaseUrl;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AccessDomainRepository accessDomainRepository;

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserService userService;
    @Autowired
    private RequestHeaderHandler requestHeaderHandler;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TaskServiceImpl taskServiceImpl;

    @Autowired
    private LeavePolicyRepository leavePolicyRepository;

    @Autowired
    private LeaveRemainingRepository leaveRemainingRepository;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private TeamService teamService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private AccessDomainService accessDomainService;

    @Autowired
    private TimeSheetService timeSheetService;

    @Autowired
    private CapacityService capacityService;

    @Autowired
    private SprintRepository sprintRepository;

    @Autowired
    private LeaveApplicationRepository leaveApplicationRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private EntityPreferenceService entityPreferenceService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private EntityPreferenceRepository entityPreferenceRepository;

    @Autowired
    private UserFeatureAccessRepository userFeatureAccessRepository;
    /**
     * This method is used to add leave policy in the organization
     * @param leavePolicyRequest
     * @param userId
     * @param timeZone
     * @param accountIds
     * @return
     */
    public ResponseEntity<String> addLeavePolicy(LeavePolicyRequest leavePolicyRequest, String userId, String timeZone, String accountIds){
        if(!isOrgAdminOrBackUpAdmin(leavePolicyRequest.getOrgId(), leavePolicyRequest.getTeamId(), leavePolicyRequest.getAccountId())) {
            throw new ValidationFailedException("You're not authorized to add leave policy");
        }
        if (leavePolicyRequest.getLeavePolicyTitle() != null) {
            leavePolicyRequest.setLeavePolicyTitle(leavePolicyRequest.getLeavePolicyTitle().trim());
        }
        RestTemplate restTemplate = new RestTemplate();
        String url = tseHrBaseUrl + ControllerConstants.TseHr.rootPathLeave+ ControllerConstants.TseHr.addLeavePolicyUrl;
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("userId", userId);
        headers.add("timeZone", timeZone);
        headers.add("accountIds", accountIds);
        HttpEntity<Object> requestEntity = new HttpEntity<>(leavePolicyRequest, headers);
        ResponseEntity<String> response = null;
        response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<String>() {
        });
        return response;
    }


    /**
     * This method is used to get list of leave policies assigned to user
     * @param accountId
     * @param accountIds
     * @param timeZone
     * @param userId
     * @return
     */
    public ResponseEntity<List<LeavePolicyResponse>> getUserLeavePolicy(Long accountId, String accountIds, String timeZone, String userId){
        //Single accountIds required from frontend
        if(!isOrgAdminOrBackUpAdmin(null, null, Long.valueOf(accountIds))) {
            throw new ValidationFailedException("You're not authorized to access other users leave policies");
        }

        RestTemplate restTemplate = new RestTemplate();
        String url = tseHrBaseUrl +ControllerConstants.TseHr.rootPathLeave+ ControllerConstants.TseHr.getUserLeavePolicy;
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("userId", userId);
        headers.add("timeZone", timeZone);
        headers.add("accountIds", accountIds);
        headers.add("accountId", accountId.toString());
        HttpEntity<Object> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<List<LeavePolicyResponse>> response = null;
        response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<List<LeavePolicyResponse>>() {
        });
        return response;
    }


    /**
     * This method returns list of leave policies in an organization
     * @param orgId
     * @param timeZone
     * @param accountIds
     * @param userId
     * @return
     */
    public ResponseEntity<List<LeavePolicyResponse>> getOrgLeavePolicy(Long orgId, String timeZone, String accountIds, String userId){

        //Single accountIds required from frontend
        if(!isOrgAdminOrBackUpAdmin(null, null, Long.valueOf(accountIds))) {
            throw new ValidationFailedException("You're not authorized to access organization leave policies");
        }

        RestTemplate restTemplate = new RestTemplate();
        String url = tseHrBaseUrl +ControllerConstants.TseHr.rootPathLeave+ ControllerConstants.TseHr.getOrgLeavePolicy;
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("userId", userId);
        headers.add("timeZone", timeZone);
        headers.add("accountIds", accountIds);
        headers.add("orgId", orgId.toString());
        HttpEntity<Object> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<List<LeavePolicyResponse>> response = null;
        response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<List<LeavePolicyResponse>>() {
        });
        return response;
    }


    /**
     * This method updates the leave policy for provided leave policy id
     * @param leavePolicyId
     * @param leavePolicyRequest
     * @param userId
     * @param timeZone
     * @param accountIds
     * @return
     */
    public ResponseEntity<String> updateLeavePolicy(Long leavePolicyId, LeavePolicyRequest leavePolicyRequest, String userId, String timeZone, String accountIds){

        if(!isOrgAdminOrBackUpAdmin(leavePolicyRequest.getOrgId(), leavePolicyRequest.getTeamId(), leavePolicyRequest.getAccountId())) {
            throw new ValidationFailedException("You're not authorized to update leave policy");
        }
        if (isPolicyUpdatedDuringRestrictedPeriod()) {
            throw new ValidationFailedException("Currently server is busy please update the leave policy after sometime");
        }
        if (leavePolicyRequest.getLeavePolicyTitle() != null) {
            leavePolicyRequest.setLeavePolicyTitle(leavePolicyRequest.getLeavePolicyTitle().trim());
        }
        RestTemplate restTemplate = new RestTemplate();
        String url = tseHrBaseUrl +ControllerConstants.TseHr.rootPathLeave+ ControllerConstants.TseHr.updateLeavePolicyUrl;
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("userId", userId);
        headers.add("timeZone", timeZone);
        headers.add("accountIds", accountIds);
        headers.add("leavePolicyId", leavePolicyId.toString());
        HttpEntity<Object> requestEntity = new HttpEntity<>(leavePolicyRequest,headers);
        ResponseEntity<String> response = null;
        response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<String>() {
        });
        return response;
    }


    /**
     * This method assigns a leave policy to provided user
     * @param assignLeavePolicyRequest
     * @param timeZone
     * @param accountIds
     * @param userId
     * @return
     */
    public ResponseEntity<String>  assignLeavePolicyToUser(AssignLeavePolicyRequest assignLeavePolicyRequest, String timeZone, String accountIds, String userId){

        if(!isOrgAdminOrBackUpAdmin(null,null,Long.valueOf(accountIds))) {
            throw new ValidationFailedException("You're not authorized to assign leave policies");
        }

        RestTemplate restTemplate = new RestTemplate();
        String url = tseHrBaseUrl +ControllerConstants.TseHr.rootPathLeave+ ControllerConstants.TseHr.assignLeavePolicyToUserUrl;
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("userId", userId);
        headers.add("timeZone", timeZone);
        headers.add("accountIds", accountIds);
        HttpEntity<Object> requestEntity = new HttpEntity<>(assignLeavePolicyRequest,headers);
        ResponseEntity<String> response = null;
        response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<String>() {
        });
        return response;
    }


    /**
     * This method is used to reassign a leave policy to provided user
     * @param assignLeavePolicyRequest
     * @param timeZone
     * @param accountIds
     * @param userId
     * @return
     */
    public ResponseEntity<String>  reassignLeavePolicyToUser(AssignLeavePolicyRequest assignLeavePolicyRequest, String timeZone, String accountIds, String userId){

        if(!isOrgAdminOrBackUpAdmin(null,null,Long.valueOf(accountIds))) {
            throw new ValidationFailedException("You're not authorized to reassign leave policies");
        }

        RestTemplate restTemplate = new RestTemplate();
        String url = tseHrBaseUrl +ControllerConstants.TseHr.rootPathLeave+ ControllerConstants.TseHr.reassignLeavePolicyToUserUrl;
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("userId", userId);
        headers.add("timeZone", timeZone);
        headers.add("accountIds", accountIds);
        HttpEntity<Object> requestEntity = new HttpEntity<>(assignLeavePolicyRequest,headers);
        ResponseEntity<String> response = null;
        response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<String>() {
        });
        return response;
    }


    /**
     * This method creates a new leave for user
     * @param leaveRequest
     * @param doctorCertificate
     * @param userId
     * @param timeZone
     * @param accountIds
     * @return
     * @throws IOException
     * @throws HttpMediaTypeNotAcceptableException
     */
    public ResponseEntity<LeaveApplicationResponse> createLeave(LeaveApplicationRequest leaveRequest, MultipartFile doctorCertificate, String userId, String timeZone, String accountIds) throws IOException, HttpMediaTypeNotAcceptableException, DataFormatException {

        validatePersonalUser(leaveRequest.getAccountId());
        List<Long> teamIdList = accessDomainRepository.findTeamIdsByAccountIdsAndIsActiveTrue(List.of(leaveRequest.getAccountId()));
        validateLeaveApprover(teamIdList, leaveRequest.getApproverAccountId());

        if (leaveRequest.getLeaveReason() != null) {
            leaveRequest.setLeaveReason(leaveRequest.getLeaveReason().trim());
        }
        LeaveApplicationRequest leaveApplicationRequest = new LeaveApplicationRequest();
        leaveApplicationRequest.setAccountId(leaveRequest.getAccountId());
        leaveApplicationRequest.setLeaveSelectionTypeId(leaveRequest.getLeaveSelectionTypeId());
        leaveApplicationRequest.setExpiryLeaveDate(getExpiryDateOfLeaveApplication(leaveRequest,LocalDate.now()));
        leaveApplicationRequest.setHalfDayLeaveType(leaveRequest.getHalfDayLeaveType());
        try {
            //from Date
            leaveApplicationRequest.setFromDate(leaveRequest.getFromDate());
            //To Date
            leaveApplicationRequest.setToDate(leaveRequest.getToDate());
        }
        catch (Exception e){
            throw new DataFormatException("Date provided is not in correct format.");
        }
        int fromYear = leaveRequest.getFromDate().getYear();
        int currentYear = LocalDate.now().getYear();
        if (fromYear < currentYear || fromYear > currentYear + 1) {
            throw new IllegalStateException("Leave can be applied only for the current or upcoming year");
        }
        leaveApplicationRequest.setIncludeLunchTime(leaveRequest.getIncludeLunchTime());
        leaveApplicationRequest.setLeaveReason(leaveRequest.getLeaveReason());
        leaveApplicationRequest.setApproverAccountId(leaveRequest.getApproverAccountId());
        leaveApplicationRequest.setPhone(leaveRequest.getPhone());
        leaveApplicationRequest.setAddress(leaveRequest.getAddress());
        leaveApplicationRequest.setNotifyTo(leaveRequest.getNotifyTo());
        RestTemplate restTemplate = new RestTemplate();
        String url = tseHrBaseUrl +ControllerConstants.TseHr.rootPathLeave+ ControllerConstants.TseHr.createLeaveUrl;
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("userId", userId);
        headers.add("timeZone", timeZone);
        headers.add("accountIds", accountIds);
        if(doctorCertificate!=null) {
            if (Objects.equals(doctorCertificate.getContentType(), MediaType.IMAGE_JPEG_VALUE)
                    || Objects.equals(doctorCertificate.getContentType(), MediaType.IMAGE_PNG_VALUE)
                    || Objects.equals(doctorCertificate.getContentType(), MediaType.APPLICATION_PDF_VALUE)) {
                leaveApplicationRequest.setDoctorCertificate(doctorCertificate.getBytes());
                leaveApplicationRequest.setDoctorCertificateFileName(doctorCertificate.getOriginalFilename());
                leaveApplicationRequest.setDoctorCertificateFileType(doctorCertificate.getContentType());
                leaveApplicationRequest.setDoctorCertificateFileSize(doctorCertificate.getSize());
            } else {
                throw new HttpMediaTypeNotAcceptableException("File type not supported.");
            }
        }
        HttpEntity<Object> requestEntity = new HttpEntity<>(leaveApplicationRequest,headers);
        ResponseEntity<LeaveApplicationResponse> response = null;
        response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<LeaveApplicationResponse>() {
        });
        auditService.auditForApplyLeave(Objects.requireNonNull(response.getBody()), false);
        return response;
    }

    public void validateLeaveApprover(List<Long> teamIdList, Long approverAccountId) {
        List<Long> orgIdList = teamRepository.findFkOrgIdOrgIdByTeamIds(teamIdList);
        List<Long> projectIdListFromTeam = teamRepository.findFkProjectIdProjectIdByTeamIds(teamIdList);
        List<Long> projectIdListFromAccessDomain = accessDomainRepository.findDistinctEntityIdsByActiveAccountIds(Constants.EntityTypes.PROJECT, List.of(approverAccountId));

        Set<Long> projectIds = new HashSet<>(projectIdListFromTeam);
        if (projectIdListFromAccessDomain != null && !projectIdListFromAccessDomain.isEmpty()) {
            projectIds.addAll(projectIdListFromAccessDomain);
        }

        List<Long> projectIdList = new ArrayList<>(projectIds);

        List<Long> hrAccountIdList = new ArrayList<>();
        List<Long> teamHrAccountIdList = userFeatureAccessRepository.findDistinctUserAccountIdByEntityTypeIdAndEntityIdInAndActionIdsAndIsDeleted(Constants.EntityTypes.TEAM, teamIdList, Constants.ActionId.MANAGE_LEAVE, false);
        List<Long> projectHrAccountIdList = userFeatureAccessRepository.findDistinctUserAccountIdByEntityTypeIdAndEntityIdInAndActionIdsAndIsDeleted(Constants.EntityTypes.PROJECT, projectIdList, Constants.ActionId.MANAGE_LEAVE, false);
        List<Long> orgHrListHrAccountIdList = userFeatureAccessRepository.findDistinctUserAccountIdByEntityTypeIdAndEntityIdInAndActionIdsAndIsDeleted(Constants.EntityTypes.ORG, orgIdList, Constants.ActionId.MANAGE_LEAVE, false);

        if (teamHrAccountIdList != null && !teamHrAccountIdList.isEmpty()) {
            hrAccountIdList.addAll(teamHrAccountIdList);
        }
        if (projectHrAccountIdList != null && !projectHrAccountIdList.isEmpty()) {
            hrAccountIdList.addAll(projectHrAccountIdList);
        }
        if (orgHrListHrAccountIdList != null && !orgHrListHrAccountIdList.isEmpty()) {
            hrAccountIdList.addAll(orgHrListHrAccountIdList);
        }

        List<Long> approverList = new ArrayList<>();
        approverList.addAll(accessDomainRepository.getUserInfoWithRolesInEntities(Constants.EntityTypes.PROJECT,
                projectIdList, List.of(RoleEnum.PROJECT_ADMIN.getRoleId()),
                true).stream().map(EmailFirstLastAccountId::getAccountId).collect(Collectors.toList()));
        approverList.addAll(accessDomainRepository.getUserInfoWithRolesInEntities(Constants.EntityTypes.ORG,
                orgIdList, List.of(RoleEnum.ORG_ADMIN.getRoleId()),
                true).stream().map(EmailFirstLastAccountId::getAccountId).collect(Collectors.toList()));

        if (!hrAccountIdList.isEmpty()) {
            approverList.addAll(hrAccountIdList);
        }
        if (!approverList.contains(approverAccountId)) {
            throw new IllegalStateException("Only org/project admin are authorized to approve leave requests for this user.");
        }
    }


    /**
     * This method updates leave
     * @param leaveRequest
     * @param doctorCertificate
     * @param userId
     * @param timeZone
     * @param accountIds
     * @return
     * @throws IOException
     * @throws HttpMediaTypeNotAcceptableException
     */
    public ResponseEntity<LeaveApplicationResponse> updateLeave(LeaveApplicationRequest leaveRequest, MultipartFile doctorCertificate, String userId, String timeZone, String accountIds) throws IOException, HttpMediaTypeNotAcceptableException, DataFormatException {
        validatePersonalUser(leaveRequest.getAccountId());
        LeaveApplication leaveDb = leaveApplicationRepository.findByLeaveApplicationId(leaveRequest.getAccountId());
        if (Boolean.FALSE.equals(leaveDb.getIsAttachmentPresent()) &&
                Boolean.FALSE.equals(leaveRequest.getIsAttachmentPresent()) && doctorCertificate == null) {
            throw new ValidationFailedException(
                    "No attachment found for this leave request. Cannot remove an attachment that does not exist."
            );
        }
        if (leaveRequest.getLeaveReason() != null) {
            leaveRequest.setLeaveReason(leaveRequest.getLeaveReason().trim());
        }
        LeaveApplicationRequest leaveApplicationRequest = new LeaveApplicationRequest();
        leaveApplicationRequest.setLeaveApplicationId(leaveRequest.getLeaveApplicationId());
        leaveApplicationRequest.setAccountId(leaveRequest.getAccountId());
        leaveApplicationRequest.setLeaveSelectionTypeId(leaveRequest.getLeaveSelectionTypeId());
        leaveApplicationRequest.setHalfDayLeaveType(leaveRequest.getHalfDayLeaveType());
        leaveApplicationRequest.setIsAttachmentPresent(leaveRequest.getIsAttachmentPresent());
        try {
            //from Date
            leaveApplicationRequest.setFromDate(leaveRequest.getFromDate());
            //To Date
            leaveApplicationRequest.setToDate(leaveRequest.getToDate());
        }
        catch (Exception e){
            throw new DataFormatException("Date provided is not in correct format.");
        }
        leaveApplicationRequest.setIncludeLunchTime(leaveRequest.getIncludeLunchTime());
        leaveApplicationRequest.setLeaveReason(leaveRequest.getLeaveReason());
        leaveApplicationRequest.setApproverAccountId(leaveRequest.getApproverAccountId());
        leaveApplicationRequest.setPhone(leaveRequest.getPhone());
        leaveApplicationRequest.setAddress(leaveRequest.getAddress());
        leaveApplicationRequest.setNotifyTo(leaveRequest.getNotifyTo());
        leaveApplicationRequest.setExpiryLeaveDate(getExpiryDateOfLeaveApplication(leaveRequest,LocalDate.now()));


        RestTemplate restTemplate = new RestTemplate();
        String url = tseHrBaseUrl +ControllerConstants.TseHr.rootPathLeave+ ControllerConstants.TseHr.updateLeaveUrl;
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("userId", userId);
        headers.add("timeZone", timeZone);
        headers.add("accountIds", accountIds);
        if(doctorCertificate!=null) {
            if (Objects.equals(doctorCertificate.getContentType(), MediaType.IMAGE_JPEG_VALUE)
                    || Objects.equals(doctorCertificate.getContentType(), MediaType.IMAGE_PNG_VALUE)
                    || Objects.equals(doctorCertificate.getContentType(), MediaType.APPLICATION_PDF_VALUE)) {
                leaveApplicationRequest.setDoctorCertificate(doctorCertificate.getBytes());
                leaveApplicationRequest.setDoctorCertificateFileName(doctorCertificate.getOriginalFilename());
                leaveApplicationRequest.setDoctorCertificateFileType(doctorCertificate.getContentType());
                leaveApplicationRequest.setDoctorCertificateFileSize(doctorCertificate.getSize());
            } else {
                throw new HttpMediaTypeNotAcceptableException("File type not supported.");
            }
        }
        HttpEntity<Object> requestEntity = new HttpEntity<>(leaveApplicationRequest,headers);
        ResponseEntity<LeaveApplicationResponse> response = null;
        response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<LeaveApplicationResponse>() {
        });
        auditService.auditForApplyLeave(response.getBody(), true);
        return new ResponseEntity<>(getLeaveApplication(response.getBody().getLeaveApplicationId()),HttpStatus.OK);
    }

    public LeaveApplicationResponse getLeaveApplication(Long leaveApplicationId) {
        LeaveApplication leaveApplication= leaveApplicationRepository.findByLeaveApplicationId(leaveApplicationId);
        LeaveTypeAlias leaveTypeAlias = entityPreferenceRepository.findLeaveTypeAliasForEntity(Constants.EntityTypes.ORG, userAccountRepository.findOrgIdByAccountId(leaveApplication.getAccountId()));
        return getLeaveApplicationResponse(leaveApplication, leaveTypeAlias);
    }
    private LeaveApplicationResponse getLeaveApplicationResponse (LeaveApplication leaveApplication, LeaveTypeAlias leaveTypeAlias) {
        LeaveApplicationResponse leaveApplicationResponse = new LeaveApplicationResponse();
        leaveApplicationResponse.setLeaveApplicationId(leaveApplication.getLeaveApplicationId());
        leaveApplicationResponse.setLeaveApplicationStatusId(leaveApplication.getLeaveApplicationStatusId());
        leaveApplicationResponse.setApplicantDetails(userAccountRepository.getEmailFirstNameLastNameAccountIdIsActiveByAccountId(leaveApplication.getAccountId()));
        leaveApplicationResponse.setLeaveType(Objects.equals(leaveApplication.getLeaveTypeId(), 1) ? leaveTypeAlias.getTimeOffAlias() : leaveTypeAlias.getSickLeaveAlias());
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
    public ResponseEntity<LeaveApplicationResponse> getLeaveApplication(Long leaveApplicationId, String userId, String timeZone, String accountIds){

        RestTemplate restTemplate = new RestTemplate();
        String url = tseHrBaseUrl +ControllerConstants.TseHr.rootPathLeave+ ControllerConstants.TseHr.getLeaveApplicationUrl;
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("userId", userId);
        headers.add("timeZone", timeZone);
        headers.add("accountIds", accountIds);
        headers.add("leaveApplicationId",leaveApplicationId.toString());
        HttpEntity<Object> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<LeaveApplicationResponse> response = null;
        response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<LeaveApplicationResponse>() {
        });
        return response;
    }


    /**
     * This method returns leave history
     * @param leaveHistoryRequest
     * @param userId
     * @param timeZone
     * @param accountIds
     * @return
     */
    public ResponseEntity<List<LeaveApplicationResponse>> getLeaveHistory(LeaveHistoryRequest leaveHistoryRequest, String userId, String timeZone, String accountIds){

        RestTemplate restTemplate = new RestTemplate();
        String url = tseHrBaseUrl +ControllerConstants.TseHr.rootPathLeave+ ControllerConstants.TseHr.getLeaveHistoryUrl;
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("userId", userId);
        headers.add("timeZone", timeZone);
        headers.add("accountIds", accountIds);
        HttpEntity<Object> requestEntity = new HttpEntity<>(leaveHistoryRequest,headers);
        ResponseEntity<List<LeaveApplicationResponse>> response = null;
        response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<List<LeaveApplicationResponse>>() {
        });
        return response;
    }


    /**
     * This method returns doctor certificate for provided application Id
     * @param applicationId
     * @param userId
     * @param timeZone
     * @param accountIds
     * @return
     */
    public ResponseEntity<DoctorCertificateMetaData> getDoctorCertificate(Long applicationId, String userId, String timeZone, String accountIds){

        RestTemplate restTemplate = new RestTemplate();
        String url = tseHrBaseUrl +ControllerConstants.TseHr.rootPathLeave+ ControllerConstants.TseHr.getDoctorCertificateUrl;
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("userId", userId);
        headers.add("timeZone", timeZone);
        headers.add("accountIds", accountIds);
        headers.add("applicationId", applicationId.toString());
        HttpEntity<Object> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<DoctorCertificateMetaData> response = null;
        response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<DoctorCertificateMetaData>() {
        });
        return response;
    }


    /**
     *
     * @param accountId
     * @param userId
     * @param timeZone
     * @param accountIds
     * @return
     */
    public ResponseEntity<List<LeaveApplicationResponse>> applicationStatus(Long accountId, String userId, String timeZone, String accountIds){

        RestTemplate restTemplate = new RestTemplate();
        String url = tseHrBaseUrl +ControllerConstants.TseHr.rootPathLeave+ ControllerConstants.TseHr.applicationStatusUrl;
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("userId", userId);
        headers.add("timeZone", timeZone);
        headers.add("accountIds", accountIds);
        headers.add("accountId",accountId.toString());
        HttpEntity<Object> requestEntity = new HttpEntity<>(accountId,headers);
        ResponseEntity<List<LeaveApplicationResponse>> response = null;
        response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<List<LeaveApplicationResponse>>() {
        });
        return response;
    }


    /**
     *
     * @param userId
     * @param timeZone
     * @param accountIds
     * @return
     */
    public ResponseEntity<AllLeavesByFilterResponse> getLeavesToProcess(LeaveWithFilterRequest leaveWithFilterRequest, String userId, String timeZone, String accountIds){

        if (leaveWithFilterRequest.getApproverAccountId() == null) {
            throw new IllegalStateException("Please provide approver accountId.");
        }
        if(!accountIds.contains(leaveWithFilterRequest.getApproverAccountId().toString())){
            throw new IllegalStateException("This accountId does not belongs to this user. Please check again!");
        }
        normalizeLeaveWithFilterRequest(leaveWithFilterRequest, false, true, false, null);
        RestTemplate restTemplate = new RestTemplate();
        String url = tseHrBaseUrl +ControllerConstants.TseHr.rootPathLeave+ ControllerConstants.TseHr.getLeavesByFilter;
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("userId", userId);
        headers.add("timeZone", timeZone);
        headers.add("accountIds", accountIds);
        HttpEntity<Object> requestEntity = new HttpEntity<>(leaveWithFilterRequest,headers);
        ResponseEntity<AllLeavesByFilterResponse> response = null;
        response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<AllLeavesByFilterResponse>() {
        });
        Objects.requireNonNull(response.getBody())
                .getUserLeaveReportResponseList()
                .sort(Comparator
                        .comparing(LeaveApplicationResponse::getFromDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(
                                leave -> {
                                    EmailFirstLastAccountIdIsActive info = leave.getApplicantDetails();
                                    return info != null ? info.getFirstName() : null;
                                },
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                        )
                        .thenComparing(
                                leave -> {
                                    EmailFirstLastAccountIdIsActive info = leave.getApplicantDetails();
                                    return info != null ? info.getLastName() : null;
                                },
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                        )
                );
        return response;
    }


    /**
     * This method cancels the provided leave application
     * @param userId
     * @param timeZone
     * @param accountIds
     * @return
     */
    public ResponseEntity<Object> cancelLeaveApplication(CancelLeaveRequest cancelLeaveRequest, String userId, String timeZone, String accountIds){
        if (cancelLeaveRequest.getLeaveCancellationReason() != null) {
            cancelLeaveRequest.setLeaveCancellationReason(cancelLeaveRequest.getLeaveCancellationReason().trim());
        }
        RestTemplate restTemplate = new RestTemplate();
        String url = tseHrBaseUrl +ControllerConstants.TseHr.rootPathLeave+ ControllerConstants.TseHr.cancelLeaveApplicationUrl;
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("userId", userId);
        headers.add("timeZone", timeZone);
        headers.add("accountIds", accountIds);
        headers.add("applicationId", cancelLeaveRequest.getLeaveApplicationId().toString());
        headers.add("cancellationReason", cancelLeaveRequest.getLeaveCancellationReason());
        HttpEntity<Object> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<LeaveApplicationNotificationRequest> response = null;
        response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<LeaveApplicationNotificationRequest>() {
        });
        //for notification
        LeaveApplicationNotificationRequest leaveApplicationNotificationRequest = response.getBody();
        if(leaveApplicationNotificationRequest.getSendNotification() != null && leaveApplicationNotificationRequest.getSendNotification()) {
            ChangeLeaveStatusRequest changeLeaveStatusRequest = new ChangeLeaveStatusRequest();
            changeLeaveStatusRequest.setAccountId(leaveApplicationNotificationRequest.getApplicantAccountId());
            changeLeaveStatusRequest.setApplicationId(leaveApplicationNotificationRequest.getLeaveApplicationId());
            changeLeaveStatusRequest.setApproverReason(leaveApplicationNotificationRequest.getApproverAccountId().toString());
            //create meeting invite payload
            List<HashMap<String, String>> leaveNotificationPayload = notificationService.notifyForLeaveApplication(changeLeaveStatusRequest,leaveApplicationNotificationRequest,timeZone);
            //  pass this payload to fcm for notification
            taskServiceImpl.sendPushNotification(leaveNotificationPayload);
        }
        if (Objects.equals(leaveApplicationNotificationRequest.getNotificationFor(), Constants.NOTIFY_FOR_LEAVE_CANCELLED)) {
            updateSprintCapacitiesOnLeaveStatusChange(leaveApplicationNotificationRequest, cancelLeaveRequest.getUpdateCapacity(), timeZone);
        }
        String message = (leaveApplicationNotificationRequest.getNotificationFor() == null || Objects.equals(leaveApplicationNotificationRequest.getNotificationFor(), Constants.NOTIFY_FOR_LEAVE_CANCELLED)) ? "Leave successfully cancelled." : "Cancellation request raised successfully.";
        return CustomResponseHandler.generateCustomResponse(response.getStatusCode(), response.getStatusCode().getReasonPhrase(), message);
    }


    /**
     * This method changes leave status
     * @param changeLeaveStatusRequest
     * @param userId
     * @param timeZone
     * @param accountIds
     * @return
     */
    public ResponseEntity<LeaveApplicationNotificationRequest> changeLeaveStatus(ChangeLeaveStatusRequest changeLeaveStatusRequest, String userId, String timeZone, String accountIds){

        if (changeLeaveStatusRequest.getApproverReason() != null) {
            changeLeaveStatusRequest.setApproverReason(changeLeaveStatusRequest.getApproverReason().trim());
        }

        RestTemplate restTemplate = new RestTemplate();
        String url = tseHrBaseUrl +ControllerConstants.TseHr.rootPathLeave+ ControllerConstants.TseHr.changeLeaveStatusUrl;
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("userId", userId);
        headers.add("timeZone", timeZone);
        headers.add("accountIds", accountIds);
        HttpEntity<Object> requestEntity = new HttpEntity<>(changeLeaveStatusRequest,headers);
        ResponseEntity<LeaveApplicationNotificationRequest> response = null;
        response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<LeaveApplicationNotificationRequest>() {
        });
        auditService.auditForChangeLeaveStatus(userAccountRepository.findByAccountIdAndIsActive(response.getBody().getApproverAccountId(), true), changeLeaveStatusRequest.getApplicationId());
        return response;
    }


    /**
     * This method returns team history
     * @param teamLeaveHistoryRequest
     * @param userId
     * @param timeZone
     * @param accountIds
     * @return
     */
    public ResponseEntity<List<LeaveApplicationResponse>> getTeamLeaveHistory(TeamLeaveHistoryRequest teamLeaveHistoryRequest, String userId, String timeZone, String accountIds){

        RestTemplate restTemplate = new RestTemplate();
        String url = tseHrBaseUrl +ControllerConstants.TseHr.rootPathLeave+ ControllerConstants.TseHr.getTeamLeaveHistoryUrl;
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("userId", userId);
        headers.add("timeZone", timeZone);
        headers.add("accountIds", accountIds);
        HttpEntity<Object> requestEntity = new HttpEntity<>(teamLeaveHistoryRequest,headers);
        ResponseEntity<List<LeaveApplicationResponse>> response = null;
        response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<List<LeaveApplicationResponse>>() {
        });
        return response;
    }


    /**
     * this method returns remaining leaves
     * @param leaveRemainingHistoryRequest
     * @param userId
     * @param timeZone
     * @param accountIds
     * @return
     */
    public ResponseEntity<List<LeaveRemainingResponse>> getLeavesRemaining(LeaveRemainingRequest leaveRemainingHistoryRequest, String userId, String timeZone, String accountIds){

        RestTemplate restTemplate = new RestTemplate();
        String url = tseHrBaseUrl +ControllerConstants.TseHr.rootPathLeave+ ControllerConstants.TseHr.getLeavesRemainingUrl;

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("userId", userId);
        headers.add("timeZone", timeZone);
        headers.add("accountIds", accountIds);
        HttpEntity<Object> requestEntity = new HttpEntity<>(leaveRemainingHistoryRequest,headers);
        ResponseEntity<List<LeaveRemainingResponse>> response = null;
        response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<List<LeaveRemainingResponse>>() {
        });
        return response;
    }


    /**
     * This method returns all the approved leaves for a user
     * @param accountId
     * @param accountIds
     * @return
     */
    public ResponseEntity<List<LeaveApplicationResponse>> getApprovedLeaves(Long accountId, String accountIds){

        RestTemplate restTemplate = new RestTemplate();
        String url = tseHrBaseUrl +ControllerConstants.TseHr.rootPathLeave+ ControllerConstants.TseHr.getApprovedLeavesUrl;

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("accountIds", accountIds);
        headers.add("accountId",accountId.toString());
        HttpEntity<Object> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<List<LeaveApplicationResponse>> response = null;
        response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<List<LeaveApplicationResponse>>() {
        });
        return response;
    }

    /** gets all approved leaves for a user (Assumes that the leave can only be a full day leave */
    public Set<LocalDate> getUserApprovedLeaves(Long accountId, String accountIds) {
        Set<LocalDate> leaveDates = new HashSet<>();
        try {
            ResponseEntity<List<LeaveApplicationResponse>> response = getApprovedLeaves(accountId, accountIds);
            if (response.getStatusCode() == HttpStatus.OK) {
                List<LeaveApplicationResponse> leaves = response.getBody();
                for (LeaveApplicationResponse leave : leaves) {
                    LocalDate fromDate = leave.getFromDate();
                    LocalDate toDate = leave.getToDate();

                    while (!fromDate.isAfter(toDate)) {
                        leaveDates.add(fromDate);
                        fromDate = fromDate.plusDays(1);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching user approved leaves for accountId: " + accountId);
        }

        return leaveDates;
    }




    /**
     * This methods returns all the team members on leave on that day
     * @param teamMemberOnLeaveRequest
     * @param userId
     * @param timeZone
     * @param accountIds
     * @return
     */
    public ResponseEntity<List<LeaveApplicationResponse>> getTeamMembersOnLeave(TeamMemberOnLeaveRequest teamMemberOnLeaveRequest, String userId, String timeZone, String accountIds){

        RestTemplate restTemplate = new RestTemplate();
        String url = tseHrBaseUrl +ControllerConstants.TseHr.rootPathLeave+ ControllerConstants.TseHr.getTeamMembersOnLeaveUrl;
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("userId", userId);
        headers.add("timeZone", timeZone);
        headers.add("accountIds", accountIds);
        HttpEntity<Object> requestEntity = new HttpEntity<>(teamMemberOnLeaveRequest,headers);
        ResponseEntity<List<LeaveApplicationResponse>> response = null;
        response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<List<LeaveApplicationResponse>>() {
        });
        return response;
    }


    /**
     * This methode checks whether user exists in organization as organization admin or back up admin
     * Also check whether team exists in organization
     * @param orgId
     * @param teamId
     * @param accountId
     * @return
     */
    private boolean isOrgAdminOrBackUpAdmin(Long orgId, Long teamId, Long accountId){

        UserAccount userOrgId= new UserAccount();
        if(orgId ==null){
            userOrgId=userAccountRepository.findByAccountIdAndIsActive(accountId,true);
            orgId =  userOrgId.getOrgId();
        }
        if(teamId !=null){
            List<Long> orgTeamIds = teamRepository.findTeamIdsByOrgId(orgId);
            if(!CommonUtils.containsAny(orgTeamIds, Collections.singletonList(teamId))){
                return false;
            }
        }
        if (Objects.equals(orgId, Constants.OrgIds.PERSONAL.longValue())) {
            throw new IllegalStateException("Leave services are not available for personal organization.");
        }
        Integer orgAdminRoleId = RoleEnum.ORG_ADMIN.getRoleId();
        Integer backUpOrgAdminRoleId = RoleEnum.BACKUP_ORG_ADMIN.getRoleId();
        List<AccountId> orgAdminOrBackUpAdminList= accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdAndRoleIdInAndIsActive(Constants.EntityTypes.ORG, orgId,List.of(backUpOrgAdminRoleId, orgAdminRoleId),true);
        for(AccountId ac: orgAdminOrBackUpAdminList) {
            if(Objects.equals(ac.getAccountId(), accountId)) {
                return true;
            }
        }
        return false;
    }

    public ResponseEntity<String>  assignLeavePolicyToAllUser(AssignLeavePolicyInBulkRequest assignLeavePolicyRequest, String timeZone, String accountIds, String userId){

        if(!isOrgAdminOrBackUpAdmin(null,null,Long.valueOf(accountIds))) {
            throw new ValidationFailedException("You're not authorized to assign leave policies");
        }
        if (isPolicyUpdatedDuringRestrictedPeriod()) {
            throw new ValidationFailedException("Currently server is busy please assign leave policy after sometime");
        }

        RestTemplate restTemplate = new RestTemplate();
        String url = tseHrBaseUrl +ControllerConstants.TseHr.rootPathLeave+ ControllerConstants.TseHr.assignLeavePolicyToAllUser;
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("userId", userId);
        headers.add("timeZone", timeZone);
        headers.add("accountIds", accountIds);
        HttpEntity<Object> requestEntity = new HttpEntity<>(assignLeavePolicyRequest,headers);
        ResponseEntity<String> response = null;
        response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<String>() {
        });
        return response;
    }

    public ResponseEntity<List<PeopleOnLeaveResponse>> getPeopleOnLeave(Integer entityTypeId, Long entityId, DateRequest todayDate, String userId, String timeZone, String accountIds){

        RestTemplate restTemplate = new RestTemplate();
        String url = tseHrBaseUrl +ControllerConstants.TseHr.rootPathLeave+ ControllerConstants.TseHr.getPeopleOnLeave;
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("userId", userId);
        headers.add("timeZone", timeZone);
        headers.add("accountIds", accountIds);
        headers.add("entityTypeId", entityTypeId.toString());
        headers.add("entityId", entityId.toString());

        HttpEntity<Object> requestEntity = new HttpEntity<>(todayDate, headers);
        ResponseEntity<List<PeopleOnLeaveResponse>> response = null;
        response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<List<PeopleOnLeaveResponse>>() {
        });
        return response;
    }

    /**
     * This method gets the leave report for users provided
     */
    public ResponseEntity<Object> getEntityLeaveReport(EntityLeaveReportRequest entityLeaveReportRequest, Integer pageNumber, Integer pageSize, String timeZone, String accountIds) {
        List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
        List<Long> accountIdList = new ArrayList<>();

        List<Long> accountIdsForEntity = new ArrayList<>();
        if (entityLeaveReportRequest.getEntityTypeId() != null && entityLeaveReportRequest.getEntityId() != null) {
            accountIdsForEntity.addAll(getAccountIdsForEntity(
                    entityLeaveReportRequest.getEntityTypeId(),
                    entityLeaveReportRequest.getEntityId(),
                    headerAccountIds)
            );
        } else {
            accountIdsForEntity.addAll(getAccountIdsForEntity(
                    Constants.EntityTypes.ORG,
                    entityLeaveReportRequest.getOrgId(),
                    headerAccountIds)
            );
        }

        if (entityLeaveReportRequest.getAccountIdList() == null || entityLeaveReportRequest.getAccountIdList().isEmpty()) {
            accountIdList.addAll(accountIdsForEntity);
        } else {
            accountIdsForEntity.retainAll(entityLeaveReportRequest.getAccountIdList());
            accountIdList.addAll(accountIdsForEntity);
        }

        entityLeaveReportRequest.setAccountIdList(accountIdList);

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("timeZone", timeZone);
        headers.add("accountIds", accountIds);

        HttpEntity<Object> requestEntity = new HttpEntity<>(entityLeaveReportRequest, headers);

        RestTemplate restTemplate = new RestTemplate();
        String url = tseHrBaseUrl + ControllerConstants.TseHr.rootPathLeave + ControllerConstants.TseHr.getEntityLeaveReport;

        ResponseEntity<EntityLeaveReportResponse> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<>() {
        });

        // Apply pagination here
        EntityLeaveReportResponse fullResponse = response.getBody();
        if (fullResponse == null) {
            return new ResponseEntity<>("No response from HR system", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        List<MemberLeaveReport> memberLeaveReportList = fullResponse.getMemberLeaveReportList();
        int totalLeaveReport = memberLeaveReportList.size();
        int startIndex = pageNumber * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalLeaveReport);

        List<MemberLeaveReport> paginatedList = (startIndex < totalLeaveReport) ? memberLeaveReportList.subList(startIndex, endIndex) : Collections.emptyList();

        EntityLeaveReportResponse paginatedResponse = new EntityLeaveReportResponse();
        paginatedResponse.setFromDate(fullResponse.getFromDate());
        paginatedResponse.setToDate(fullResponse.getToDate());
        paginatedResponse.setTotalLeaveReport(totalLeaveReport);
        paginatedResponse.setMemberLeaveReportList(paginatedList);

        return new ResponseEntity<>(paginatedResponse, HttpStatus.OK);
    }

    private void validatePersonalUser (Long accountId) {
        UserAccount userAccount = userAccountRepository.findByAccountIdAndIsActive(accountId, true);
        if (Objects.equals(userAccount.getOrgId(), Constants.OrgIds.PERSONAL.longValue())) {
            throw new IllegalStateException("Leave services are not available for personal organization.");
        }
    }

    public UserListForLeave getLeaveApproversAndNotifier(Long orgId, String accountIds){
        Set<EmailFirstLastAccountId> approverList = new HashSet<>();
        Set<EmailFirstLastAccountId> notifyToList = new HashSet<>();
        UserListForLeave userListForLeave = new UserListForLeave();
        List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
        UserAccount userAccount = userAccountRepository.findByAccountIdInAndOrgIdAndIsActive(headerAccountIds, orgId, true);
        if (userAccount == null) {
            throw new ValidationFailedException("The specified user does not belong to the provided organization.");
        }
        List<Long> teamIdList = accessDomainRepository.findTeamIdsByAccountIdsAndIsActiveTrue(List.of(userAccount.getAccountId()));
        List<Long> orgIdList = List.of(orgId);
        List<Long> projectIdListFromTeam = teamRepository.findFkProjectIdProjectIdByTeamIds(teamIdList);
        List<Long> projectIdListFromAccessDomain = accessDomainRepository.findDistinctEntityIdsByActiveAccountIds(Constants.EntityTypes.PROJECT, List.of(userAccount.getAccountId()));

        Set<Long> projectIds = new HashSet<>(projectIdListFromTeam);
        if (projectIdListFromAccessDomain != null && !projectIdListFromAccessDomain.isEmpty()) {
            projectIds.addAll(projectIdListFromAccessDomain);
        }

        List<Long> projectIdList = new ArrayList<>(projectIds);

        List<Long> hrAccountIdList = new ArrayList<>();
        List<Long> teamHrAccountIdList = userFeatureAccessRepository.findDistinctUserAccountIdByEntityTypeIdAndEntityIdInAndActionIdsAndIsDeleted(Constants.EntityTypes.TEAM, teamIdList, Constants.ActionId.MANAGE_LEAVE, false);
        List<Long> projectHrAccountIdList = userFeatureAccessRepository.findDistinctUserAccountIdByEntityTypeIdAndEntityIdInAndActionIdsAndIsDeleted(Constants.EntityTypes.PROJECT, projectIdList, Constants.ActionId.MANAGE_LEAVE, false);
        List<Long> orgHrListHrAccountIdList = userFeatureAccessRepository.findDistinctUserAccountIdByEntityTypeIdAndEntityIdInAndActionIdsAndIsDeleted(Constants.EntityTypes.ORG, orgIdList, Constants.ActionId.MANAGE_LEAVE, false);

        if (teamHrAccountIdList != null && !teamHrAccountIdList.isEmpty()) {
            hrAccountIdList.addAll(teamHrAccountIdList);
        }
        if (projectHrAccountIdList != null && !projectHrAccountIdList.isEmpty()) {
            hrAccountIdList.addAll(projectHrAccountIdList);
        }
        if (orgHrListHrAccountIdList != null && !orgHrListHrAccountIdList.isEmpty()) {
            hrAccountIdList.addAll(orgHrListHrAccountIdList);
        }
        List<EmailFirstLastAccountId> hrUserDetailsList = userAccountRepository.getEmailFirstNameLastNameAccountIdByAccountIdIn(hrAccountIdList);

        // there was a bug 8951 in which Akshit and sir decided that project manager won't be shown in list of approvers
//        approverList.addAll(accessDomainRepository.getUserInfoWithRolesInEntities(Constants.EntityTypes.TEAM,
//                teamIdList, List.of(RoleEnum.PROJECT_MANAGER_SPRINT.getRoleId(), RoleEnum.PROJECT_MANAGER_NON_SPRINT.getRoleId()),
//                true));

        approverList.addAll(accessDomainRepository.getUserInfoWithRolesInEntities(Constants.EntityTypes.PROJECT,
                projectIdList, List.of(RoleEnum.PROJECT_ADMIN.getRoleId()),
                true));
        approverList.addAll(accessDomainRepository.getUserInfoWithRolesInEntities(Constants.EntityTypes.ORG,
                orgIdList, List.of(RoleEnum.ORG_ADMIN.getRoleId()),
                true));
        if (hrUserDetailsList != null && !hrUserDetailsList.isEmpty()) {
            approverList.addAll(hrUserDetailsList);
        }

        notifyToList = new HashSet<>(accessDomainRepository.getUserInfoInEntities(Constants.EntityTypes.TEAM, teamIdList, true));
        if (hrUserDetailsList != null && !hrUserDetailsList.isEmpty()) {
            notifyToList.addAll(hrUserDetailsList);
        }
        userListForLeave.setNotifiyToList(new ArrayList<>(notifyToList));
        userListForLeave.setApproverList(new ArrayList<>(approverList));
        if (userListForLeave.getApproverList() != null && !userListForLeave.getApproverList().isEmpty()) {
            userListForLeave.getApproverList().sort(Comparator
                    .comparing(EmailFirstLastAccountId::getFirstName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(EmailFirstLastAccountId::getLastName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(EmailFirstLastAccountId::getAccountId, Comparator.nullsLast(Long::compareTo))
            );
        }
        if (userListForLeave.getNotifiyToList() != null && !userListForLeave.getNotifiyToList().isEmpty()) {
            userListForLeave.getNotifiyToList().sort(Comparator
                    .comparing(EmailFirstLastAccountId::getFirstName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(EmailFirstLastAccountId::getLastName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(EmailFirstLastAccountId::getAccountId, Comparator.nullsLast(Long::compareTo))
            );
        }
        return userListForLeave;
    }


    public ResponseEntity<Object> getUserPolicyReport(Integer entityTypeId, Long entityId, Integer pageNumber, Integer pageSize, String timeZone, String accountIds) throws IllegalAccessException {

        ResponseEntity<Object> response = null;

        if (Objects.equals(entityId, Constants.PERSONAL_TEAM_ID)) {
            return response;
        }
        List<Long> accountIdsList = CommonUtils.convertToLongList(accountIds);

        Boolean hasViewAccess = accessDomainService.isOrgAminOrProjectAdmin(entityTypeId, entityId, accountIdsList);
        if (!hasViewAccess && Objects.equals(entityTypeId, Constants.EntityTypes.TEAM)) hasViewAccess = accessDomainService.isManangerInTeam(entityId, accountIdsList);
        if (!hasViewAccess) throw new IllegalAccessException("User not authorized to view policy reports for provided entity");
        RestTemplate restTemplate = new RestTemplate();
        String url = tseHrBaseUrl +ControllerConstants.TseHr.rootPathLeave+ ControllerConstants.TseHr.getAllUsersPolicyReport;
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("timeZone", timeZone);
        headers.add("accountIds", accountIds);
        headers.add("entityTypeId", entityTypeId.toString());
        headers.add("entityId", entityId.toString());
        headers.add("pageNumber", pageNumber.toString());
        headers.add("pageSize", pageSize.toString());

        HttpEntity<Object> requestEntity = new HttpEntity<>(headers);
        response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<Object>() {
        });
        return response;
    }

    public ResponseEntity<Object> updateUserPolicy(UpdateLeavePolicyForUsersRequest usersRequest, String timeZone, String accountIds) throws IllegalAccessException {

        ResponseEntity<Object> response = null;

        if (Objects.equals(usersRequest.getEntityId(), Constants.PERSONAL_TEAM_ID)) {
            return response;
        }
        List<Long> accountIdsList = CommonUtils.convertToLongList(accountIds);

        Boolean hasUpdateAccess = accessDomainService.isOrgAminOrProjectAdmin(usersRequest.getEntityTypeId(), usersRequest.getEntityId(), accountIdsList);
        if (!hasUpdateAccess) throw new IllegalAccessException("User not authorized to update policies for provided entity");
        RestTemplate restTemplate = new RestTemplate();
        String url = tseHrBaseUrl +ControllerConstants.TseHr.rootPathLeave+ ControllerConstants.TseHr.updateUserPolicy;
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("timeZone", timeZone);
        headers.add("accountIds", accountIds);


        HttpEntity<Object> requestEntity = new HttpEntity<>(usersRequest, headers);
        response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<Object>() {
        });
        return response;
    }

    // ZZZZZZ 14-04-2025
    public void updateSprintCapacitiesOnLeaveStatusChange(LeaveApplicationNotificationRequest leaveApplicationNotificationRequest, Boolean updateCapacityForActiveSprint, String timeZone) {
        List<Long> userTeamIdsList = accessDomainRepository.findTeamIdsByAccountIdsAndIsActiveTrue(List.of(leaveApplicationNotificationRequest.getApplicantAccountId()));
        if (!userTeamIdsList.isEmpty()) {
            Set<Sprint> allSprints = new HashSet<>();
            LocalDate fromDate = LocalDate.parse(leaveApplicationNotificationRequest.getFromDate());
            LocalDate toDate = LocalDate.parse(leaveApplicationNotificationRequest.getToDate());
            while (toDate.equals(fromDate) || fromDate.isBefore(toDate)) {
                if (updateCapacityForActiveSprint) {
                    allSprints.addAll(sprintRepository.getCustomAllActiveSprintsForEntitiesAndContainsLeaveDate(userTeamIdsList, Constants.EntityTypes.TEAM, fromDate));
                }
                allSprints.addAll(sprintRepository.getCustomSprintsForEntitiesAndContainsDate(userTeamIdsList, Constants.EntityTypes.TEAM, fromDate, List.of(Constants.SprintStatusEnum.NOT_STARTED.getSprintStatusId())));
                fromDate = fromDate.plusDays(1);
            }

            for (Sprint sprint : allSprints) {
                capacityService.updateSprintAndUserCapacityOnLeaveCancellation(sprint, leaveApplicationNotificationRequest.getApplicantAccountId(), timeZone);
            }
        }
    }

    public PartOfActiveSprintResponse isUserPartOfActiveSprint (Long applicationId) {
        Optional<LeaveApplication> leaveApplicationOptional = leaveApplicationRepository.findById(applicationId);
        if (leaveApplicationOptional.isEmpty()) {
            throw new EntityNotFoundException("Leave not found");
        }
        LeaveApplication leaveApplication = leaveApplicationOptional.get();
        PartOfActiveSprintResponse partOfActiveSprintResponse = new PartOfActiveSprintResponse();
        String message = "User not part of any active sprint.";
        List<Long> userTeamIdsList = accessDomainRepository.findTeamIdsByAccountIdsAndIsActiveTrue(List.of(leaveApplication.getAccountId()));
        if (!userTeamIdsList.isEmpty()) {
            Set<Sprint> allActiveSprints = new HashSet<>();
            LocalDate fromDate = leaveApplication.getFromDate();
            LocalDate toDate = leaveApplication.getToDate();
            while (toDate.equals(fromDate) || fromDate.isBefore(toDate)) {
                allActiveSprints.addAll(sprintRepository.getCustomAllActiveSprintsForEntitiesAndContainsLeaveDate(userTeamIdsList, Constants.EntityTypes.TEAM, fromDate));
                fromDate = fromDate.plusDays(1);
            }
            if (!allActiveSprints.isEmpty()) {
                partOfActiveSprintResponse.setIsPartOfActiveSprint(true);
                message = "User is part of an active sprint so, approving or cancelling leave application will affect the sprint capacity. Do you want to modify capacity of that sprint?";
            }
        }
        partOfActiveSprintResponse.setMessage(message);
        return partOfActiveSprintResponse;
    }

    public String sendAlertForLeaveApproval (Long applicationId, Long accountId, String timeZone,String headerAccountIds) {
        Alert alert = new Alert();
        Optional<LeaveApplication> leaveApplicationOptional = leaveApplicationRepository.findById(applicationId);
        if (leaveApplicationOptional.isEmpty()) {
            throw new EntityNotFoundException("Leave application not found");
        }
        LeaveApplication leaveApplication = leaveApplicationOptional.get();
        if (!Objects.equals(leaveApplication.getLeaveApplicationStatusId(), Constants.LeaveApplicationStatusIds.WAITING_APPROVAL_LEAVE_APPLICATION_STATUS_ID)) {
            throw new IllegalStateException("User only allowed to send alert for unapproved leaves");
        }
        if (!Objects.equals(leaveApplication.getAccountId(), accountId)) {
            throw new IllegalStateException("User not authorized to send alert for provided leave application");
        }

        UserAccount userAccountReceiver = userAccountRepository.findByAccountIdAndIsActive(leaveApplication.getApproverAccountId(), true);
        UserAccount userAccountSender = userAccountRepository.findByAccountIdAndIsActive(leaveApplication.getAccountId(), true);
        if (userAccountSender == null) {
            throw new EntityNotFoundException("Sending user not found");
        }
        if (userAccountReceiver == null) {
            throw new EntityNotFoundException("Receiving user not found");
        }
        List<Long> teamIds = accessDomainRepository.findTeamIdsByAccountIdsAndIsActiveTrue(List.of(userAccountReceiver.getAccountId(), accountId));
        Team team = teamRepository.findByTeamId(teamIds.get(0));
        Project project = projectRepository.findByProjectId(team.getFkProjectId().getProjectId());
        Organization organization = organizationRepository.findByOrgId(team.getFkOrgId().getOrgId());
        alert.setFkOrgId(organization);
        alert.setFkProjectId(project);
        alert.setFkTeamId(team);
        alert.setAlertTitle("Leave approval alert");
        alert.setAlertReason(userAccountSender.getFkUserId().getFirstName() + " " + userAccountSender.getFkUserId().getLastName()
                + " has requested you to approve leave with leave reason as " + leaveApplication.getLeaveReason());
        alert.setFkAccountIdSender(userAccountSender);
        alert.setFkAccountIdReceiver(userAccountReceiver);
        alert.setAlertStatus(Constants.AlertStatusEnum.UNVIEWED.getStatus());
        alertRepository.save(alert);
        notificationService.immediateAttentionNotification(alert, timeZone,headerAccountIds);
        return "Alert sent successfully.";
    }

    public ResponseEntity<AllLeavesByFilterResponse> getAllUserLeaveApplicationByFilter(LeaveWithFilterRequest leaveWithFilterRequest, String userId, String timeZone, String accountIds){
        List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
        if (leaveWithFilterRequest.getOrgId() == null && (leaveWithFilterRequest.getAccountIdList() == null || leaveWithFilterRequest.getAccountIdList().isEmpty())) {
            throw new EntityNotFoundException("Please provide organization information or user details to view user applications");
        }

        if (leaveWithFilterRequest.getApplicationStatusIds() == null || leaveWithFilterRequest.getApplicationStatusIds().isEmpty()) {
            throw new IllegalStateException("Please provide application status to view leaves.");
        }
        normalizeLeaveWithFilterRequest(leaveWithFilterRequest, true, false, false, headerAccountIds);

        RestTemplate restTemplate = new RestTemplate();
        String url = tseHrBaseUrl +ControllerConstants.TseHr.rootPathLeave+ ControllerConstants.TseHr.getLeavesByFilter;
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("userId", userId);
        headers.add("timeZone", timeZone);
        headers.add("accountIds", accountIds);
        HttpEntity<Object> requestEntity = new HttpEntity<>(leaveWithFilterRequest,headers);
        ResponseEntity<AllLeavesByFilterResponse> response = null;
        response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<AllLeavesByFilterResponse>() {
        });
        return response;
    }

    public ResponseEntity<AllLeavesByFilterResponse> getMyLeaves(LeaveWithFilterRequest leaveWithFilterRequest, String userId, String timeZone, String accountIds){
        List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
        leaveWithFilterRequest.setAccountIdList(headerAccountIds);
        normalizeLeaveWithFilterRequest(leaveWithFilterRequest, false, false, false, headerAccountIds);

        RestTemplate restTemplate = new RestTemplate();
        String url = tseHrBaseUrl +ControllerConstants.TseHr.rootPathLeave+ ControllerConstants.TseHr.getLeavesByFilter;
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("userId", userId);
        headers.add("timeZone", timeZone);
        headers.add("accountIds", accountIds);
        HttpEntity<Object> requestEntity = new HttpEntity<>(leaveWithFilterRequest,headers);
        ResponseEntity<AllLeavesByFilterResponse> response = null;
        response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<AllLeavesByFilterResponse>() {
        });
        Objects.requireNonNull(response.getBody())
                .getUserLeaveReportResponseList()
                .sort(Comparator.comparing(
                            LeaveApplicationResponse::getFromDate,
                            Comparator.nullsLast(Comparator.reverseOrder())
                    )
                );

        return response;
    }

    private void normalizeLeaveWithFilterRequest (LeaveWithFilterRequest leaveWithFilterRequest, Boolean setAccountIds, Boolean setWaitingStatusIds, Boolean setDates, List<Long> accountIdList) {
        if (leaveWithFilterRequest.getLeaveTypeList() == null || leaveWithFilterRequest.getLeaveTypeList().isEmpty()) {
            leaveWithFilterRequest.setLeaveTypeList(Constants.LEAVE_TYPE);
        }

        if (setAccountIds && (leaveWithFilterRequest.getAccountIdList() == null || leaveWithFilterRequest.getAccountIdList().isEmpty())) {
            List<Long> orgAccountIds = getAccountIdsForEntity(Constants.EntityTypes.ORG, leaveWithFilterRequest.getOrgId(), accountIdList);
            leaveWithFilterRequest.setAccountIdList(orgAccountIds);
        }

        if (setWaitingStatusIds) {
            List<Short> applicationStatusIds = new ArrayList<>(List.of(Constants.LeaveApplicationStatusIds.WAITING_APPROVAL_LEAVE_APPLICATION_STATUS_ID, Constants.LeaveApplicationStatusIds.WAITING_CANCEL_LEAVE_APPLICATION_STATUS_ID));
            leaveWithFilterRequest.setApplicationStatusIds(applicationStatusIds);
        }

        if (setDates) {
            if (leaveWithFilterRequest.getFromDate() == null) {
                leaveWithFilterRequest.setFromDate(LocalDate.now().plusDays(1));
            }
            if (leaveWithFilterRequest.getToDate() == null) {
                leaveWithFilterRequest.setToDate(leaveWithFilterRequest.getFromDate().plusDays(7));
            }
        }
    }

    private List<Long> getAccountIdsForEntity (Integer entityTypeId, Long entityId, List<Long> accountIdsList) {
        List<Long> entityIdList = new ArrayList<>();
        Set<Long> userAccountIdList = new HashSet<>();
        List<Integer> roleList = Constants.ROLE_IDS_FOR_LEAVE;
        if (Objects.equals(entityTypeId, Constants.EntityTypes.TEAM)) {
            if (accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.TEAM, entityId, accountIdsList, roleList, true)) {
                userAccountIdList.addAll(
                        accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdAndIsActive(Constants.EntityTypes.TEAM, entityId, true)
                                .stream()
                                .map(AccountId::getAccountId)
                                .collect(Collectors.toSet())
                );
            }
        } else if (Objects.equals(entityTypeId, Constants.EntityTypes.PROJECT)) {
            if (accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndRoleIdInAndIsActive(Constants.EntityTypes.PROJECT, entityId, accountIdsList, roleList, true)) {
                userAccountIdList.addAll(projectService.getprojectMembersAccountIdList(List.of(entityId)).stream().map(AccountId::getAccountId).collect(Collectors.toSet()));
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
                    userAccountIdList.addAll(projectService.getprojectMembersAccountIdList(projectIdList).stream().map(AccountId::getAccountId).collect(Collectors.toSet()));
                }
                entityIdList.addAll(accessDomainRepository.findByAccountIdInAndRoleIdInAndOrgIdAndIsActive(accountIdsList, roleList, entityId, true));
                userAccountIdList.addAll(accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdInAndIsActive(Constants.EntityTypes.TEAM, entityIdList, true).stream().map(AccountId::getAccountId).collect(Collectors.toSet()));
            }
            userAccountIdList.addAll(getMembersWithFeatureAccess(entityTypeId,entityId, accountIdsList, userAccountIdList));
        } else {
            throw new EntityNotFoundException("Please provide a valid entity");
        }
        return new ArrayList<>(userAccountIdList);
    }

    public EntityMembersAvailabilityResponse getEntityMembersAvailability(Integer entityTypeId, Long entityId, LocalDate localDate, List<Long> accountIds) {
        EntityMembersAvailabilityResponse teamAttendanceResponse = new EntityMembersAvailabilityResponse();
        List<Long> teamIds = getTeamIdListByEntity(entityTypeId, entityId, accountIds);

        List<Long> accountIdsList = accessDomainRepository.findDistinctAccountIdsByEntityTypeIdAndEntityIdInAndIsActive(Constants.EntityTypes.TEAM, teamIds, true);
        Integer numberOfMemberOnLeave = leaveApplicationRepository.findMemberOnLeaveByAccountIdsAndDate(accountIdsList, localDate, List.of(Constants.LeaveApplicationStatusIds.APPROVED_LEAVE_APPLICATION_STATUS_ID, Constants.LeaveApplicationStatusIds.CONSUMED_LEAVE_APPLICATION_STATUS_ID));
        Integer numberOfMemberPresent = accountIdsList.size() - numberOfMemberOnLeave;
        teamAttendanceResponse.setNumberOfMembersAvailable(numberOfMemberPresent);
        teamAttendanceResponse.setNumberOfMembersOnLeave(numberOfMemberOnLeave);
        return teamAttendanceResponse;
    }

    List<Long> getTeamIdListByEntity(Integer entityTypeId, Long entityId, List<Long> accountIds) {
        List<Long> teamIds = new ArrayList<>();
        List<Integer> roleIds = Constants.rolesToViewPeopleOnLeave;
        if (Objects.equals(entityTypeId, Constants.EntityTypes.ORG)) {
            teamIds.addAll(accessDomainRepository.findByOrgIdAccountIdInAndRoleIdInAndIsActive(accountIds, roleIds, true, entityId));
        } else if (Objects.equals(entityTypeId, Constants.EntityTypes.BU)) {
            teamIds.addAll(accessDomainRepository.findByBuIdAccountIdInAndRoleIdInAndIsActive(accountIds, roleIds, true, entityId));
        } else if (Objects.equals(entityTypeId, Constants.EntityTypes.PROJECT)) {
            teamIds.addAll(accessDomainRepository.findByProjectIdAccountIdInAndRoleIdInAndIsActive(accountIds, roleIds, true, entityId));
        } else if (Objects.equals(entityTypeId, Constants.EntityTypes.TEAM)) {
            teamIds.addAll(accessDomainRepository.findByTeamIdAccountIdInAndRoleIdInAndIsActive(accountIds, roleIds, true, entityId));
        } else {
            throw new EntityNotFoundException("Please provide a valid entity type");
        }
        return teamIds;
    }

    public UpcomingLeaveResponse getUpcomingLeaves(UpcomingLeaveRequest upcomingLeaveRequest, String userId, String timeZone, String accountIds){
        List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
        LeaveWithFilterRequest leaveWithFilterRequest = new LeaveWithFilterRequest();
        leaveWithFilterRequest.setAccountIdList(getAccountIdsForEntity(upcomingLeaveRequest.getEntityTypeId(), upcomingLeaveRequest.getEntityId(), headerAccountIds));
        leaveWithFilterRequest.setFromDate(upcomingLeaveRequest.getFromDate());
        leaveWithFilterRequest.setToDate(upcomingLeaveRequest.getToDate());
        normalizeLeaveWithFilterRequest(leaveWithFilterRequest, false, false, true, headerAccountIds);
        leaveWithFilterRequest.setApplicationStatusIds(List.of(Constants.LeaveApplicationStatusIds.APPROVED_LEAVE_APPLICATION_STATUS_ID));
        UpcomingLeaveResponse upcomingLeaveResponse = new UpcomingLeaveResponse();
        List<LeaveApplicationResponse> leaveApplicationResponseList = new ArrayList<>();
        if (upcomingLeaveRequest.getReturnLeaves()) {
            RestTemplate restTemplate = new RestTemplate();
            String url = tseHrBaseUrl + ControllerConstants.TseHr.rootPathLeave + ControllerConstants.TseHr.getLeavesByFilter;
            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            headers.add("userId", userId);
            headers.add("timeZone", timeZone);
            headers.add("accountIds", accountIds);
            HttpEntity<Object> requestEntity = new HttpEntity<>(leaveWithFilterRequest, headers);
            leaveApplicationResponseList = restTemplate.exchange(url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<List<LeaveApplicationResponse>>() {
            }).getBody();
        }
        upcomingLeaveResponse.setUpcomingLeaves(leaveApplicationResponseList);
        if (!leaveApplicationResponseList.isEmpty()) {
            Float upcomingLeaves = 0F;
            for (LeaveApplicationResponse leaveApplication : leaveApplicationResponseList) {
                upcomingLeaves = upcomingLeaves + (leaveApplication.getNumberOfLeaveDays() != null ? leaveApplication.getNumberOfLeaveDays() : 0);
            }
            upcomingLeaveResponse.setUpcomingLeavesCount(upcomingLeaves);
        } else {
            RestTemplate restTemplate = new RestTemplate();
            String url = tseHrBaseUrl + ControllerConstants.TseHr.rootPathLeave + ControllerConstants.TseHr.getUpcomingLeavesCount;
            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            headers.add("userId", userId);
            headers.add("timeZone", timeZone);
            headers.add("accountIds", accountIds);
            HttpEntity<Object> requestEntity = new HttpEntity<>(leaveWithFilterRequest, headers);
            upcomingLeaveResponse.setUpcomingLeavesCount(restTemplate.exchange(url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<Float>() {
            }).getBody());
        }

        return upcomingLeaveResponse;
    }

    public List<LeaveTypesResponse> getLeaveTypeReponse(Long orgId) {
        List<LeaveTypesResponse> leaveTypesResponseList = new ArrayList<>();
        List<LeaveTypesResponse> leaveTypeAliasResponse = entityPreferenceService.getLeaveTypeAlias(orgId);
        HashMap<Integer, String> leaveTypeIdToNameMap = Constants.LeaveTypeIdToNameMap;

        leaveTypeAliasResponse.forEach((leaveType) -> {
            Integer intializer = (Integer.parseInt(String.valueOf(leaveType.getLeaveTypeId())) - 1) * 3;
            for (Map.Entry<Integer, String> map : leaveTypeIdToNameMap.entrySet()) {
                short typeId = (short) (intializer + map.getKey());
                String typeName = map.getValue() + leaveType.getLeaveTypeName();
                LeaveTypesResponse leaveTypesResponse = new LeaveTypesResponse(typeId, typeName);
                leaveTypesResponseList.add(leaveTypesResponse);
            }
        });

        return leaveTypesResponseList;
    }

    public List<LeaveTabsAccessResponse> getLeaveTabsAccess(String headerAccountIds) {
        List<LeaveTabsAccessResponse> leaveTabsAccessResponseList = new ArrayList<>();
        List<Long> accountIds = CommonUtils.convertToLongList(headerAccountIds);
        List<Long> orgIdList = userAccountRepository.getAllOrgIdByAccountIdInAndIsActive(accountIds, true);
        String actionId = String.valueOf(Constants.ActionId.MANAGE_LEAVE);
        List<UserFeaturesAccess> featureAccessList = userFeatureAccessRepository.findByUserAccountIds(accountIds);
        List<Long> orgIdsWithAction = featureAccessList.stream()
                .filter(entry -> entry.getActionIds() != null && entry.getActionIds().contains(1001))
                .map(UserFeaturesAccess::getOrgId)
                .distinct()
                .collect(Collectors.toList());

        HashMap<Integer, String> leaveTabIdToNameMap = Constants.LeaveTabIdToNameMap;
        for (Long orgId : orgIdList) {
            LeaveTabsAccessResponse leaveTabsAccessResponse = new LeaveTabsAccessResponse();
            leaveTabsAccessResponse.setOrgId(orgId);

            if (!Objects.equals(orgId,Constants.OrgIds.PERSONAL.longValue())) {
                leaveTabIdToNameMap.forEach((tabId, tabName) -> {
                    com.tse.core_application.model.Constants.ScreenRoleEnum roleEnum = com.tse.core_application.model.Constants.ScreenRoleEnum.getByType(tabName);
                    if (roleEnum == null) {
                        throw new IllegalStateException("Unable to determine user role: Screen state unknown.");
                    }
                    List<Integer> roleList = roleEnum.getList();
                    if (roleList == null) {
                        leaveTabsAccessResponse.addToTabIdList(tabId);
                    } else {
                        if (accessDomainRepository.existsByRolesInOrg(roleList, orgId, accountIds)) {
                            leaveTabsAccessResponse.addToTabIdList(tabId);
                        }
                    }
                    if ((tabId == Constants.LeaveTabs.LEAVE_CONTROLS || tabId == Constants.LeaveTabs.LEAVE_REPORT)
                            && orgIdsWithAction.contains(orgId)
                            && !leaveTabsAccessResponse.getTabsIdList().contains(tabId)) {
                        leaveTabsAccessResponse.addToTabIdList(tabId);
                    }
                });
            }

            leaveTabsAccessResponseList.add(leaveTabsAccessResponse);
        }

        return leaveTabsAccessResponseList;
    }

    public Boolean isPolicyUpdatedDuringRestrictedPeriod() {
        LocalDateTime currentDateTime = LocalDateTime.now();

        YearMonth yearMonth = YearMonth.from(currentDateTime);
        int lastDayOfMonth = yearMonth.lengthOfMonth();

        LocalDateTime startTime = currentDateTime.withDayOfMonth(lastDayOfMonth).with(LocalTime.of(23, 55, 0));
        LocalDateTime endTime = currentDateTime.withDayOfMonth(lastDayOfMonth).with(LocalTime.of(23, 59, 59));

        return currentDateTime.isAfter(startTime) && currentDateTime.isBefore(endTime);
    }

    public LeaveAttachmentResponse getLeaveAttachment(Long leaveApplicationId) {
        if (leaveApplicationId == null) {
            throw new ValidationFailedException("Leave application ID must not be null");
        }
        LeaveApplication leaveDb = leaveApplicationRepository.findByLeaveApplicationId(leaveApplicationId);
        if (leaveDb == null) {
            throw new ValidationFailedException("Leave application not found with ID: " + leaveApplicationId);
        }
        LeaveAttachmentResponse response = new LeaveAttachmentResponse();
        if (leaveDb.getDoctorCertificate() != null && leaveDb.getDoctorCertificateFileName() != null) {
            response.setDoctorCertificateFileName(leaveDb.getDoctorCertificateFileName());
            response.setDoctorCertificate(leaveDb.getDoctorCertificate());
        }
        return response;
    }

    /**
     * Returns a list of userAccountIds that includes all users from the input list
     * and adds active members of each entity from the provided feature access list.
     * Ensures no duplicates and filters only those in the provided accountIdsList.
     *
     * @param userFeaturesAccessList list of user feature access entries
     * @param userAccountIdList      base user account IDs
     * @param accountIdsList         reference list of allowed account IDs
     * @return combined list of user account IDs without duplicates
     */

    private boolean hasFeatureAccess(Integer entityTypeId, Long entityId, List<Long> accountIdsList) {
        return userFeatureAccessRepository.existsByEntityTypeIdAndEntityIdAndUserAccountIdAndActionIdAndIsDeletedFalse(
                entityTypeId, entityId, accountIdsList, Constants.ActionId.MANAGE_LEAVE
        );
    }

    private List<Long> getMembersWithFeatureAccess(Integer entityTypeId, Long entityId,
                                                       List<Long> accountIdsList,
                                                       Set<Long> userAccountIdList) {
        if (Objects.equals(entityTypeId, Constants.EntityTypes.TEAM)) {
            Long projectId = teamRepository.findFkProjectIdProjectIdByTeamId(entityId);
            Long orgId = teamRepository.findFkOrgIdOrgIdByTeamId(entityId);
            if (hasFeatureAccess(Constants.EntityTypes.TEAM, entityId, accountIdsList) || hasFeatureAccess(Constants.EntityTypes.PROJECT, projectId, accountIdsList) || hasFeatureAccess(Constants.EntityTypes.ORG, orgId, accountIdsList)) {
                List<Long> entityIdList = List.of(entityId);
                userAccountIdList.addAll(accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdInAndIsActive(Constants.EntityTypes.TEAM, entityIdList, true).stream().map(AccountId::getAccountId).collect(Collectors.toSet()));
            }
        } else if (Objects.equals(entityTypeId, Constants.EntityTypes.PROJECT)) {
            Long projectId = entityId;
            Long orgId = projectRepository.findOrgIdByProjectId(projectId).getOrgId();
            if (hasFeatureAccess(Constants.EntityTypes.PROJECT, projectId, accountIdsList) || hasFeatureAccess(Constants.EntityTypes.ORG, orgId, accountIdsList)) {
                userAccountIdList.addAll(projectService.getprojectMembersAccountIdList(List.of(entityId)).stream().map(AccountId::getAccountId).collect(Collectors.toSet()));
            } else {
                //find all teams that have hr role of that project and get all members of that team
                List<Long> teamIds = teamRepository.findTeamIdsByProjectId(projectId);
                List<Long> accessEntityIds = userFeatureAccessRepository.findAllMatchingEntityIds(Constants.EntityTypes.TEAM, accountIdsList, teamIds, Constants.ActionId.MANAGE_LEAVE);
                userAccountIdList.addAll(accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdInAndIsActive(Constants.EntityTypes.TEAM, accessEntityIds, true).stream().map(AccountId::getAccountId).collect(Collectors.toSet()));
            }
        } else if (Objects.equals(entityTypeId, Constants.EntityTypes.ORG)) {
            if (hasFeatureAccess(Constants.EntityTypes.ORG, entityId, accountIdsList)) {
                userAccountIdList.addAll(userAccountRepository.findAccountIdByOrgIdAndIsActive(entityId, true).stream().map(AccountId::getAccountId).collect(Collectors.toSet()));

            } else {
                List<Long> teamIds = teamRepository.findTeamIdsByOrgId(entityId);
                //add hr access Team in org
                List<Long> accessTeamEntityIds = userFeatureAccessRepository.findAllMatchingEntityIds(Constants.EntityTypes.TEAM, accountIdsList, teamIds, Constants.ActionId.MANAGE_LEAVE);
                userAccountIdList.addAll(accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdInAndIsActive(Constants.EntityTypes.TEAM, accessTeamEntityIds, true).stream().map(AccountId::getAccountId).collect(Collectors.toSet()));
                //add hr access project in team
                List<Long> projectIds = projectRepository.findProjectIdsByOrgIds(List.of(entityId));
                List<Long> accessProjectEntityIds = userFeatureAccessRepository.findAllMatchingEntityIds(Constants.EntityTypes.PROJECT, accountIdsList, projectIds, Constants.ActionId.MANAGE_LEAVE);
                List<Long>accessProjectTeamList=teamRepository.findTeamIdsByProjectIds(accessProjectEntityIds);
                userAccountIdList.addAll(accessDomainRepository.findDistinctAccountIdByEntityTypeIdAndEntityIdInAndIsActive(Constants.EntityTypes.TEAM, accessProjectTeamList, true).stream().map(AccountId::getAccountId).collect(Collectors.toSet()));
            }
        }
        if (userAccountIdList != null) {
            return new ArrayList(userAccountIdList);
        }
        return new ArrayList<>();
    }

    public GetUsersLeaveDetailsResponse getLeaveDetails(LeaveApplicationDetailsRequest leaveApplicationDetailsRequest, Integer pageNumber, Integer pageSize, String timeZone, String accountIds) {
        if (leaveApplicationDetailsRequest.getFromDate().isAfter(leaveApplicationDetailsRequest.getToDate())) {
            throw new ValidationFailedException("From date can't be after To date");
        }
        if (!organizationRepository.existsByOrgId(leaveApplicationDetailsRequest.getOrgId())) {
            throw new ValidationFailedException("Organization doesn't exist");
        }
        List<Long> headerAccountIds = CommonUtils.convertToLongList(accountIds);
        GetUsersLeaveDetailsResponse getUsersLeaveDetailsResponse = new GetUsersLeaveDetailsResponse();
        List<Long> accountIdList = getAccountIdsForEntity(Constants.EntityTypes.ORG, leaveApplicationDetailsRequest.getOrgId(), headerAccountIds);
        if (accountIdList.isEmpty()) {
            return getUsersLeaveDetailsResponse;
        }
        leaveApplicationDetailsRequest.setAccountIdList(accountIdList);

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("timeZone", timeZone);
        headers.add("accountIds", accountIds);
        headers.add("pageNumber", pageNumber.toString());
        headers.add("pageSize", pageSize.toString());

        HttpEntity<Object> requestEntity = new HttpEntity<>(leaveApplicationDetailsRequest, headers);

        RestTemplate restTemplate = new RestTemplate();
        String url = tseHrBaseUrl + ControllerConstants.TseHr.rootPathLeave + ControllerConstants.TseHr.getUserLeaveDetails;

        ResponseEntity<GetUsersLeaveDetailsResponse> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<>() {
        });

        return response.getBody();
    }

    public void changeApproverOfLeaveApplications(List<Long> accountIdList,
                                                  List<Long> removedApproverAccountIdList,
                                                  Long newApproverAccountId) {
        if (accountIdList == null || accountIdList.isEmpty()
                || removedApproverAccountIdList == null || removedApproverAccountIdList.isEmpty()
                || newApproverAccountId == null) {
            return;
        }

        int updated = leaveApplicationRepository.bulkUpdateApprover(
                accountIdList,
                removedApproverAccountIdList,
                List.of(Constants.LeaveApplicationStatusIds.WAITING_APPROVAL_LEAVE_APPLICATION_STATUS_ID,
                        Constants.LeaveApplicationStatusIds.WAITING_CANCEL_LEAVE_APPLICATION_STATUS_ID),
                newApproverAccountId
        );
    }

    //This method will work for both ful da and half day
    public Map<LocalDate, Float> getAllUserLeaveDays(Long accountId, String accountIds) {
        Map<LocalDate, Float> leaveDayMap = new HashMap<>();
        try {
            ResponseEntity<List<LeaveApplicationResponse>> response =
                    getApprovedLeaves(accountId, accountIds);
            if (response.getStatusCode() == HttpStatus.OK) {
                List<LeaveApplicationResponse> leaves = response.getBody();
                for (LeaveApplicationResponse leave : leaves) {
                    Float duration = leave.getNumberOfLeaveDays(); // 1.0 or 0.5
                    LocalDate from = leave.getFromDate();
                    LocalDate to = leave.getToDate();
                    if (Objects.equals(duration, Constants.HalfDayLeaveDuration)) {
                        leaveDayMap.put(from, 0.5f);
                    }
                    else {
                        while (!from.isAfter(to)) {
                            leaveDayMap.put(from, 1.0f);
                            from = from.plusDays(1);
                        }
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Error fetching leave days for accountId: " + accountId, e);
        }
        return leaveDayMap;
    }
    public LocalDate getExpiryDateOfLeaveApplication(
            LeaveApplicationRequest request, LocalDate appliedDate
    ) {
        LocalDate leaveFrom = request.getFromDate();
        LocalDate leaveTo = request.getToDate();
        boolean isForwardLeave = !appliedDate.isAfter(leaveFrom);
        Long orgId = userAccountRepository.findOrgIdByAccountId(request.getAccountId());
        YearMonth leaveMonth;
        LocalDate referenceDate;
        if (isForwardLeave) {
            //claude changes: use leaveTo (end date) instead of leaveFrom (start date) for forward leave expiry calculation
            referenceDate = leaveTo;
        } else {
            referenceDate = appliedDate;
        }
        Optional<EntityPreference> entityPreferenceOpt = entityPreferenceRepository
                .findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, orgId);
        int forwardDays= FORWARD_DAYS;
        int backwardDays= BACKWARD_DAYS;
        LocalDate payRollDate;
        if (entityPreferenceOpt.isPresent()) {
            EntityPreference entityPreference = entityPreferenceOpt.get();
            if (entityPreference.getForwardDated() != null) {
                forwardDays = entityPreference.getForwardDated() ;
            }
            if (entityPreference.getBackwardDated() != null) {
                backwardDays = entityPreference.getBackwardDated();
            }
            payRollDate=generatePayRollDate(referenceDate,entityPreference);
        }
        else {
            payRollDate=generateDefaultPayroll(referenceDate);
        }
        LocalDate expiryDate;
        if (isForwardLeave) {
            if(forwardDays!= FORWARD_DAYS) {
                //claude changes: use leaveTo (end date) instead of leaveFrom (start date) as per requirement: Expiry Date = Leave End Date + X days
                LocalDate forwardExpiry = leaveTo.plusDays(forwardDays);
                expiryDate = forwardExpiry.isBefore(payRollDate) ? forwardExpiry : payRollDate;
            } else{
                expiryDate=payRollDate;
            }
        } else {
            if(backwardDays!= BACKWARD_DAYS) {
                LocalDate backwardExpiry = appliedDate.plusDays(backwardDays);
                expiryDate = backwardExpiry.isBefore(payRollDate) ? backwardExpiry : payRollDate;
            } else{
                expiryDate=payRollDate;
            }
        }
        return expiryDate;
    }

    private LocalDate generatePayRollDate(
            LocalDate referenceDate,
            EntityPreference entityPreference
    ) {
        if (entityPreference == null) {
            return generateDefaultPayroll(referenceDate);
        }
        YearMonth yearMonth = YearMonth.from(referenceDate);
        int lastDay = yearMonth.lengthOfMonth();
        Integer customDay = entityPreference.getPayRollGenerationDay();
        Boolean lastDayFlag = entityPreference.getIsPayRollAtLastDayOfMonth();
        Boolean secondLastDayFlag = entityPreference.getIsPayRollAtLastSecondDayOfMonth();
        LocalDate payrollDate;
        if (Boolean.TRUE.equals(lastDayFlag)) {
            payrollDate = yearMonth.atEndOfMonth();
        }
        else if (Boolean.TRUE.equals(secondLastDayFlag)) {
            payrollDate = LocalDate.of(yearMonth.getYear(), yearMonth.getMonthValue(), lastDay - 1);
        }
        else if (customDay != null && customDay >= 1 && customDay <= 29) {
            int validDay = Math.min(customDay, lastDay);
            payrollDate = LocalDate.of(yearMonth.getYear(), yearMonth.getMonthValue(), validDay);
        }
        else {
            int validDay = Math.min(DEFAULT_PAYROLL_DAY, lastDay);
            payrollDate = LocalDate.of(yearMonth.getYear(), yearMonth.getMonthValue(), validDay);
        }
        if (!payrollDate.isAfter(referenceDate)) {
            YearMonth nextMonth = yearMonth.plusMonths(1);
            int nextLastDay = nextMonth.lengthOfMonth();
            if (Boolean.TRUE.equals(lastDayFlag)) {
                payrollDate = nextMonth.atEndOfMonth();
            } else if (Boolean.TRUE.equals(secondLastDayFlag)) {
                payrollDate = LocalDate.of(nextMonth.getYear(), nextMonth.getMonthValue(), nextLastDay - 1);
            } else {
                int selectedDay = payrollDate.getDayOfMonth();
                int finalDay = Math.min(selectedDay, nextLastDay);
                payrollDate = LocalDate.of(nextMonth.getYear(), nextMonth.getMonthValue(), finalDay);
            }
        }
        return payrollDate;
    }

    private LocalDate generateDefaultPayroll(LocalDate referenceDate) {
        int day = DEFAULT_PAYROLL_DAY;
        YearMonth ym = YearMonth.from(referenceDate);
        int last = ym.lengthOfMonth();
        LocalDate payrollDate = LocalDate.of(ym.getYear(), ym.getMonthValue(), Math.min(day, last));
        if (!payrollDate.isAfter(referenceDate)) {
            YearMonth next = ym.plusMonths(1);
            int nextLast = next.lengthOfMonth();
            payrollDate = LocalDate.of(next.getYear(), next.getMonthValue(), Math.min(day, nextLast));
        }
        return payrollDate;
    }
}


