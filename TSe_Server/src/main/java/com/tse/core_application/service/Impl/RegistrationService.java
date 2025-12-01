package com.tse.core_application.service.Impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tse.core_application.constants.*;
import com.tse.core_application.constants.Constants;
import com.tse.core_application.custom.model.CustomAccessDomain;
import com.tse.core_application.custom.model.CustomRoleAction;
import com.tse.core_application.custom.model.UserIdFirstLastName;
import com.tse.core_application.dto.AuthResponse;
import com.tse.core_application.dto.Otp;
import com.tse.core_application.dto.RegistrationRequest;
import com.tse.core_application.dto.SignUpCompletionDetail;
import com.tse.core_application.dto.conversations.ConversationGroup;
import com.tse.core_application.exception.*;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.*;
import com.tse.core_application.repository.*;
import com.tse.core_application.service.*;
import com.tse.core_application.utils.CommonUtils;
import com.tse.core_application.utils.JWTUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import javax.persistence.EntityNotFoundException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static com.tse.core_application.model.Constants.EntityTypes.ORG;

@Service
public class RegistrationService implements IRegistrationService {

    private static final Logger logger = LogManager.getLogger(RegistrationService.class.getName());

    @Autowired
    IOtpService otpService;

    @Autowired
    IEMailService emailService;

    @Autowired
    UserService userService;

    @Autowired
    IDeviceService deviceService;

    @Autowired
    IOrganizationService organizationService;

    @Autowired
    IUserAccountService userAccountService;

    @Autowired
    AccessDomainRepository accessDomainRepository;

    @Autowired
    JWTUtil jwtUtil;

    ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditRepository auditRepository;

    @Value("${email.subject}")
    private String emailSubject;

    @Autowired
    private AccessDomainService accessDomainService;

    @Autowired
    private RoleActionService roleActionService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private BUService buService;

    @Autowired
    private UserPreferenceRepository userPreferenceRepository;
    @Autowired
    private InviteService inviteService;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Value("${tseHr.application.root.path}")
    private String tseHrBaseUrl;
    @Autowired
    private CountryRepository countryRepository;
    @Autowired
    private EntityPreferenceService entityPreferenceService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ExceptionalRegistrationRepository exceptionalRegistrationRepository;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private EntityPreferenceRepository entityPreferenceRepository;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Value("${allow.organization.registration}")
    private Boolean allowOrganizationRegistration;

    @Value("${allow.personal.registration}")
    private Boolean allowPersonalRegistration;

    @Autowired
    private BlockedRegistrationRepository blockedRegistrationRepository;

    @Autowired
    private RestrictedDomainsRepository restrictedDomainsRepository;

    @Value("${restricted.domain.list}")
    private List<String> restrictedDomains;

    @Autowired
    private CustomEnvironmentService customEnvironmentService;

    @Autowired
    private CustomEnvironmentRepository customEnvironmentRepository;

    @Override
    public String generateAndSendOtp(RegistrationRequest request) throws JsonProcessingException {
        boolean isUserExists = isUserExistsInUserAccountByEmailAndOrg(request);
        if (isUserExists) {
            return "User already exists";
        }
        Otp otp = otpService.putOtp(CommonUtils.getRedisKeyForOtp(request.getPrimaryEmail(), request.getDeviceUniqueIdentifier()));
        String ownerEmail = null;
        if (request.getOrganizationName() != null) {
            Optional<Organization> organization = organizationRepository.findByOrganizationName(request.getOrganizationName());
            if (organization.isPresent()) {
                if (entityPreferenceRepository.existsByEntityTypeIdAndEntityIdInAndShouldOtpSendToOrgAdmin (com.tse.core_application.model.Constants.EntityTypes.ORG, List.of(organization.get().getOrgId()), true)) {
                    ownerEmail = organization.get().getOwnerEmail();
                }
            }
            else {
                organization = organizationRepository.findByOrganizationName(request.getOrganizationName().trim().replaceAll("\\s+", " "));
                if (organization.isPresent()) {
                    if (entityPreferenceRepository.existsByEntityTypeIdAndEntityIdInAndShouldOtpSendToOrgAdmin (com.tse.core_application.model.Constants.EntityTypes.ORG, List.of(organization.get().getOrgId()), true)) {
                        ownerEmail = organization.get().getOwnerEmail();
                    }
                }
            }
        }
        return emailService.sendOtp(request.getPrimaryEmail(), otp.getOtp(), emailSubject, ownerEmail, true);
    }

    public boolean isUserExistsInUserAccountByEmailAndOrg(RegistrationRequest request) {
        boolean isUserExists = false;
        Organization orgFoundDb = organizationService.getOrganizationByOrganizationName(request.getOrganizationName());
        if (orgFoundDb != null) {
            UserAccount userAccountFoundDb = userAccountService.getActiveUserAccountByPrimaryEmailAndOrgId(request.getPrimaryEmail(), orgFoundDb.getOrgId());
            if (userAccountFoundDb != null) {
                isUserExists = true;
            }
        }
        return isUserExists;
    }

    private void normalizeRequestFields(RegistrationRequest request, String timeZone) {
        if (request.getDeviceOs() == null || request.getDeviceOs().trim().isEmpty()) {
            request.setDeviceOs("NA");
        }
        if (request.getDeviceOsVersion() == null || request.getDeviceOsVersion().trim().isEmpty()) {
            request.setDeviceOsVersion("NA");
        }
        if (request.getDeviceMake() == null || request.getDeviceMake().trim().isEmpty()) {
            request.setDeviceMake("NA");
        }
        if (request.getDeviceModel() == null || request.getDeviceModel().trim().isEmpty()) {
            request.setDeviceModel("NA");
        }
        request.setPrimaryEmail(request.getPrimaryEmail().toLowerCase());
        if (request.getAlternateEmail() != null) {
            request.setAlternateEmail(request.getAlternateEmail().toLowerCase());
        }
        request.setFirstName(CommonUtils.convertToTitleCase(request.getFirstName()));
        request.setMiddleName(CommonUtils.convertToTitleCase(request.getMiddleName()));
        request.setLastName(CommonUtils.convertToTitleCase(request.getLastName()));
        request.setGivenName(CommonUtils.convertToTitleCase(request.getGivenName()));

        Country country = countryRepository.findById(request.getCountry().getCountryId()).orElseThrow(EntityNotFoundException::new);
        if (!TimeZoneCountryMapping.isValidTimeZoneForCountry(timeZone, country.getIsoCountryCode())) {
            throw new ValidationFailedException("The provided timezone and country information do not match. " +
                    "Please ensure that the provided country is within your current timezone.\"");
        }
    }

    @Override
    public ResponseEntity<AuthResponse> doOtpVerificationAndUserRegistration(RegistrationRequest request, String timeZone) throws IOException {
        normalizeRequestFields(request, timeZone);
        addImage(request);
        validateUserEmail(request.getPrimaryEmail(), request);
        if (request.getAlternateEmail() != null) validateUserEmail(request.getAlternateEmail(), request);

        if (request.getOrganizationName() != null && blockedRegistrationRepository.existsByEmailAndOrganizationNameAndIsDeleted(request.getPrimaryEmail(), request.getOrganizationName(), false)) {
            throw new IllegalStateException("Username have been blocked by the system admin. Please contact system administrator at support@vijayi-wfh.com.");
        }
        // if the invite is not null, that means the user is registering via invite link. We can only register in an existing organization via invite link
        if (request.getInviteId() != null) {
            inviteService.validateInviteId(request.getInviteId(), timeZone);
            inviteService.validateInviteDetailsWithRegistrationRequest(request);
            // if invite is valid, mark the invite status as accepted
            inviteService.markInviteAsAccepted(request.getInviteId());
        } else if (request.getInviteId() == null && !request.getIsPrimaryEmailPersonal()) {
            Organization organization = organizationService.getOrganizationByOrganizationName(request.getOrganizationName());
            if (organization != null)
                throw new ValidationFailedException("Organization already exists. Please contact organization admin to receive an invite to register");
        } else if (request.getIsPrimaryEmailPersonal() && !allowPersonalRegistration && !exceptionalRegistrationRepository.existsByEmailAndIsDeleted(request.getPrimaryEmail(), false)) {
            throw new IllegalStateException("Registration for personal users have been blocked. Please contact system administrator at support@vijayi-wfh.com.");
        }

        String resp = otpService.verifyOtp(request.getDeviceUniqueIdentifier(), request.getPrimaryEmail(), request.getOtp());

        ResponseEntity<AuthResponse> inserted;
        if (resp != null) {
            boolean isUserExists = isUserExistsInUserAccountByEmailAndOrg(request);
            if (isUserExists) {
                return ResponseEntity.ok(new AuthResponse(null, ErrorConstant.USER_ALREADY_EXISTS, null, null, null, null));
            } else {
                inserted = insertDataIntoDB(resp, request, timeZone);
                if (inserted.hasBody() && inserted.getBody().getToken() != null) {
                    Audit auditAdd = auditRepository.save(auditService.auditForSignUpAndLogin(request, null));
                }
            }
        } else {
            inserted = insertDataIntoDB(resp, request, timeZone);
            if (inserted.hasBody() && inserted.getBody().getToken() != null) {
                Audit auditAdd = auditRepository.save(auditService.auditForSignUpAndLogin(request, null));
            }
        }
        return inserted;
    }

    public UserIdFirstLastName getUserIdFirstLastNameByUser(User user) {
        UserIdFirstLastName userIdFirstLastName = null;
        if(user != null) {
            userIdFirstLastName = new UserIdFirstLastName();
            userIdFirstLastName.setUserId(user.getUserId());
            userIdFirstLastName.setFirstName(user.getFirstName());
            userIdFirstLastName.setLastName(user.getLastName());
        }
        return userIdFirstLastName;
    }

    @Transactional
    public ResponseEntity<AuthResponse> insertDataIntoDB(String resp, RegistrationRequest request, String timeZone) {
        if (resp.equals(Constants.SUCCESS)) {
            User user;
            boolean isUserExists = isUserExistsInUserAccountByEmailAndOrg(request);
            if (!isUserExists) {
                User userFoundDb = userService.getUserByUserName(request.getPrimaryEmail());
                boolean isNewUser = false;
                if (userFoundDb == null) {
                    isNewUser = true;
                    user = userService.addUser(new User().getUserFromRegistrationReq(request), timeZone);
                } else {
                    user = userService.updateUser(userFoundDb, request);
                }
                Device device = deviceService.addDevice(new Device().getDeviceFromReqgistrationReq(request, user.getUserId()));
                Organization org = organizationService.getOrganizationByOrganizationName(request.getOrganizationName().trim().replaceAll("\\s+", " "));
                boolean isNewOrg = false;
                Project addedProject = new Project();

                ConversationGroup conversationGroup = new ConversationGroup();

                if (org == null) {
                    ExceptionalRegistration exceptionalRegistration = exceptionalRegistrationRepository.findByEmailAndIsDeleted(request.getPrimaryEmail(), false);
                    if (!allowOrganizationRegistration && exceptionalRegistration == null) {
                        throw new IllegalStateException("Registration of new organization has been blocked. Please contact system administrator at support@vijayi-wfh.com.");
                    }
                    Integer orgCount = organizationService.getOrganizationCountByEmail(request.getPrimaryEmail());
                    if (exceptionalRegistration != null && (orgCount > exceptionalRegistration.getMaxOrgCount() || Objects.equals(orgCount, exceptionalRegistration.getMaxOrgCount()))) {
                        throw new IllegalStateException("User exceeded his/her quota to register organizations");
                    }
                    org = organizationService.addOrganization(new Organization().getOrgFromRegistrationReq(request), exceptionalRegistration, user.getPrimaryEmail(), user);
                    isNewOrg = true;
                    if (isNewOrg && (org != null)) {
                        CustomEnvironment customEnvironment = new CustomEnvironment();
                        customEnvironment.setEnvironmentDisplayName("Production");
                        customEnvironment.setEnvironmentDescription("The production environment.");
                        customEnvironment.setEntityTypeId(ORG);
                        customEnvironment.setEntityId(org.getOrgId());
                        customEnvironment.setIsActive(true);
                        CustomEnvironment customDb = customEnvironmentRepository.save(customEnvironment);
                    }

                    conversationGroup.setEntityTypeId((long) com.tse.core_application.model.Constants.EntityTypes.ORG);
                    conversationGroup.setEntityId(org.getOrgId());
                    if(org.getOrgId()!=0) { conversationService.createNewGroup(conversationGroup, org.getOrganizationName(), com.tse.core_application.model.Constants.ConversationsGroupTypes.ORG, org.getOrgId(), user); }
                }
                // add the default org in the user preference if the user is new
                if(isNewUser) {
                    userPreferenceRepository.save(new UserPreference(user.getUserId(), org.getOrgId(), null, null, com.tse.core_application.model.Constants.NOTIFICATION_CATEGORY_IDS));
                } else {
                    UserPreference userPreferenceOfUser = userPreferenceRepository.findByUserId(user.getUserId());
                    if (userPreferenceOfUser != null && userPreferenceOfUser.getOrgId() == null) {
                        userPreferenceOfUser.setOrgId(org.getOrgId());
                        userPreferenceOfUser.setProjectId(null);
                        userPreferenceOfUser.setTeamId(null);
                        userPreferenceRepository.save(userPreferenceOfUser);
                    }
                }

                UserAccount addedUserAccount = null;
                UserAccount existingUserAccount = userAccountRepository.findByEmailAndOrgIdAndIsActive(request.getPrimaryEmail(), org.getOrgId(), false);
                // If user already exist and inactive then mark that active
                if (existingUserAccount != null) {
                    existingUserAccount.setIsActive(true);
                    existingUserAccount.setIsDisabledBySams(false);
                    existingUserAccount.setDeactivatedByRole(null);
                    existingUserAccount.setDeactivatedByAccountId(null);
                    addedUserAccount = userAccountService.addUserAccount(existingUserAccount, conversationGroup, user, timeZone);
                    redisTemplate.opsForSet().remove("INACTIVE_ACCOUNTS", existingUserAccount.getAccountId().toString());
                }
                else {
                    addedUserAccount = userAccountService.addUserAccount(new UserAccount().getUserAccountFromRegistrationReq(request, org.getOrgId(), user), conversationGroup, user, timeZone);
                }

                if (isNewOrg) {
                    BU bu = buService.addBUByOrganization(org);
                    addedProject = projectService.addProject(org, bu, addedUserAccount.getAccountId());
                    // ZZZZZZ 14-04-2025
                    entityPreferenceService.setDefaultOrgPreference(org.getOrgId(), timeZone);
                    accessDomainService.addAccessDomainForNewOrg(addedUserAccount, org, addedProject);
                    Audit addedAudit = auditService.auditForNewOrg(addedUserAccount, org);
                }

                //creating default leave policy for the new organization
                try{
                    RestTemplate restTemplate = new RestTemplate();
                    String url = tseHrBaseUrl + ControllerConstants.TseHr.rootPathLeave+ ControllerConstants.TseHr.defaultLeavePolicyAssignmentUrl;
                    MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
                    headers.add("accountId", addedUserAccount.getAccountId().toString());
                    headers.add("orgId", org.getOrgId().toString());
                    headers.add("isNewOrg", String.valueOf(isNewOrg));
                    HttpEntity<Object> requestEntity = new HttpEntity<>(headers);
                    restTemplate.exchange(url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<String>() {
                    });
                }
                catch (Exception e){
                    String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                    logger.error("Default leave policy cannot be assigned "+e, new Throwable(allStackTraces));
                }
                if (org.getOrganizationName().equalsIgnoreCase(com.tse.core_application.model.Constants.PERSONAL_ORG)) {
                    addUserToDefaultTeam(org, user, timeZone);
                }
                com.tse.core_application.dto.User userObj = userService.findByUsername(request.getPrimaryEmail(), request.getOtp(), timeZone);
                List<Long> accountIdsForUser = userService.getAccountIdsForUser(userObj);
                List<CustomAccessDomain> accessDomains = accessDomainService.findAllActiveAccessDomainByAccountId(addedUserAccount.getAccountId());
                List<CustomRoleAction> roleActions = roleActionService.getAllRoleActionsByAccessDomains(accessDomains);
                UserIdFirstLastName userIdFirstLastName = this.getUserIdFirstLastNameByUser(user);

                EntityPreference entityPreference = entityPreferenceService.fetchEntityPreference(ORG, org.getOrgId());

                if ((user.getAgeRange() == null || user.getGender() == null || user.getHighestEducation() == null || user.getCity() == null)) {
                    if (entityPreference == null || !entityPreference.getRequireMinimumSignUpDetails()) {
                        return ResponseEntity.ok(new AuthResponse(jwtUtil.generateToken(userObj, accountIdsForUser), null, accessDomains, roleActions, userIdFirstLastName, false));
                    }
                }
                return ResponseEntity.ok(new AuthResponse(jwtUtil.generateToken(userObj, accountIdsForUser), null, accessDomains, roleActions, userIdFirstLastName, true));
            }
        } else {
            return ResponseEntity.ok(new AuthResponse(null, ErrorConstant.OTP_MISMATCH_ERROR, null, null, null, null));
        }
        return null;
    }

    public ResponseEntity<Object> getFormattedGenerateOtpResponse(ResponseEntity<String> otp, RegistrationRequest request) {
        ResponseEntity<Object> formattedResponse = null;
        if (otp.getBody().equalsIgnoreCase(Constants.SUCCESS)) {
            formattedResponse = CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, otp.getBody());
        } else {
            String message = "user already exists";
            if (otp.getBody() != null && otp.getBody().toLowerCase().equals(message)) {
                String allStackTraces = StackTraceHandler.getAllStackTraces(new UserAlreadyExistException());
                logger.error("User already exist with primary email and organization name: primary email = " + request.getPrimaryEmail() + ",   " + "organization name = " + request.getOrganizationName(), new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw new UserAlreadyExistException();
            } else {
                String allStackTraces = StackTraceHandler.getAllStackTraces(new ServerBusyException());
                logger.error("Server Busy for primary email = " + request.getPrimaryEmail(), new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw new ServerBusyException();
            }
        }
        return formattedResponse;
    }

    public ResponseEntity<Object> getFormattedSignUpResponse(ResponseEntity<AuthResponse> response, RegistrationRequest request) {
        ResponseEntity<Object> formattedResponse = null;
        if (Objects.requireNonNull(response.getBody()).getToken() != null) {
            HashMap<String, Object> map = objectMapper.convertValue(response.getBody(), HashMap.class);
            map.remove("error");
            formattedResponse = CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, map);
        } else {
            if (Objects.requireNonNull(response.getBody()).getError().equalsIgnoreCase(ErrorConstant.USER_ALREADY_EXISTS)) {
                String allStackTraces =StackTraceHandler.getAllStackTraces(new UserAlreadyExistException());
                logger.error("User already exist with primary email and organization name: primary email = " + request.getPrimaryEmail() + " ,    " + "organization name = " + request.getOrganizationName(), new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw new UserAlreadyExistException();
            } else {
                String allStackTraces = StackTraceHandler.getAllStackTraces(new InvalidOtpException());
                logger.error("Sign up API: OTP is invalid for primary email = " + request.getPrimaryEmail() + " ,   " + "OTP = " + request.getOtp(), new Throwable(allStackTraces));
                ThreadContext.clearMap();
                throw new InvalidOtpException();
            }
        }
        return formattedResponse;
    }

    /**
     * This method adds user to default team with role 91 as personal user
     */
    private void addUserToDefaultTeam (Organization personalOrg, User user, String timeZone) {
        List<Team> personalOrgTeamList = teamRepository.findByFkOrgIdOrgId(personalOrg.getOrgId());
        if (personalOrgTeamList.isEmpty()) {
            throw new TeamNotFoundException();
        }
        Team personalTeam = new Team();
        // If there are multiple teams, find the one with the personal team name
        if (personalOrgTeamList.size() > 1) {
            personalTeam = personalOrgTeamList.stream()
                    .filter(team -> Objects.equals(team.getTeamName(), com.tse.core_application.model.Constants.PERSONAL_ORG_DEFAULT_TEAM_NAME))
                    .findFirst()
                    .orElse(null);
        } else {
            // If there is only one team, assign it directly
            personalTeam = personalOrgTeamList.get(0);
        }
        if (personalTeam == null) {
            throw new TeamNotFoundException();
        }
        UserAccount userAccount = userAccountRepository.findByOrgIdAndFkUserIdUserIdAndIsActive(personalOrg.getOrgId(), user.getUserId(), true);
        AccessDomain accessDomain = new AccessDomain(userAccount.getAccountId(), com.tse.core_application.model.Constants.EntityTypes.TEAM, personalTeam.getTeamId(), RoleEnum.PERSONAL_USER.getRoleId());
        accessDomainRepository.save(accessDomain);
    }

    @Override
    public void addImage (RegistrationRequest request) throws IOException {
        if (request.getImageData() == null) {
            BufferedImage defaultImage = generateDefaultImage(request.getFirstName(), request.getLastName());
            String base64Image = convertToBase64(defaultImage, "jpeg");
            request.setImageData(base64Image);
        }
    }

    public static BufferedImage generateDefaultImage(String firstName, String lastName) {
        int width = 100;
        int height = 100;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // Generate unique color based on user name
        Color userColor = generateUniqueColor(firstName + lastName);

        // Draw a colored background
        g2d.setColor(userColor);
        g2d.fillRect(0, 0, width, height);

        // Draw initials in white color
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 40));

        String initials = extractInitials(firstName, lastName);
        FontMetrics fontMetrics = g2d.getFontMetrics();
        int x = (width - fontMetrics.stringWidth(initials)) / 2;
        int y = (height - fontMetrics.getHeight()) / 2 + fontMetrics.getAscent();
        g2d.drawString(initials, x, y);

        g2d.dispose();

        return image;
    }

    private static Color generateUniqueColor(String seed) {
        int hashCode = seed.hashCode();
        int red = (hashCode & 0xFF0000) >> 16;
        int green = (hashCode & 0x00FF00) >> 8;
        int blue = hashCode & 0x0000FF;

        return new Color(red, green, blue);
    }

    private static String extractInitials(String firstName, String lastName) {
        String firstInitial = firstName.substring(0, 1);
        String lastInitial = lastName.substring(0, 1);
        return firstInitial + lastInitial;
    }

    public static String convertToBase64(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, format, outputStream);

        byte[] imageBytes = outputStream.toByteArray();
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    /** completes the registration process by getting the remaining sign up details from the user and saving it*/
    public String completeRegistration(SignUpCompletionDetail request, User user) {
        if (user.getAgeRange() != null && user.getGender() != null && user.getHighestEducation() != null && user.getCity() != null) {
            return "Sign up is already complete";
        } else {
            CommonUtils.copyNonNullProperties(request, user);
            userRepository.save(user);
            return "Success";
        }
    }

    @Override
    public void validateUserEmail(String email, RegistrationRequest registrationRequest) {
        String lowerCaseEmail = email.toLowerCase();
        int atIndex = lowerCaseEmail.indexOf('@');
        String domain = lowerCaseEmail.substring(atIndex + 1);

        // Split the domain by "." and get the first part
        String domainName = domain.split("\\.")[0];

        List<String> restrictedDomainList = new ArrayList<>();
        if (registrationRequest.getIsPrimaryEmailPersonal() != null && registrationRequest.getIsPrimaryEmailPersonal()) {
            restrictedDomainList.addAll(restrictedDomainsRepository.findDomainByIsPersonalAllowed(false));
        } else {
            restrictedDomainList.addAll(restrictedDomainsRepository.findDomainByIsOrgRegistrationAllowed(false));
        }
        if ((restrictedDomainList.contains(domainName) || restrictedDomains.contains(domainName)) && !exceptionalRegistrationRepository.existsByEmailAndIsDeleted(lowerCaseEmail, false)) {
            throw new ValidationFailedException("Registration by the provided domain is not allowed by the system admin.");
        }
    }

}

