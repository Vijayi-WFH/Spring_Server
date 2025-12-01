package com.tse.core_application.service.Impl;

import com.tse.core_application.dto.*;
import com.tse.core_application.exception.InvalidInviteException;
import com.tse.core_application.exception.TeamNotFoundException;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.model.*;
import com.tse.core_application.model.User;
import com.tse.core_application.repository.*;
import com.tse.core_application.service.IInviteService;
import com.tse.core_application.utils.CommonUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDate;
import java.util.*;

@Service
public class InviteService implements IInviteService {
    @Autowired
    private InviteRepository inviteRepository;
    @Autowired
    private EmailService emailService;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private AccessDomainRepository accessDomainRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private TemplateEngine templateEngine;
    @Autowired
    private UserService userService;
    @Autowired
    private DeviceService deviceService;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private EntityPreferenceRepository entityPreferenceRepository;

    @Value("${application.domain}")
    private String inviteApplicationDomain;

    @Autowired
    private AuditService auditService;

    /**
     * validates an invitation request, generates a unique invite ID, saves the new invite to the repository, and sends out an invitation email with the organization's name
     */
    @Override
    public Invite createInvite(InviteRequest inviteRequest, Long accountIdRequester, String timeZone) {
        normalizeInviteRequest(inviteRequest);
        Invite invite = validateInviteRequestAndConvertToInviteEntity(inviteRequest, accountIdRequester, timeZone);
        Organization organization = organizationRepository.findByOrgId(invite.getEntityId());
        // Generate a unique invite ID
        String uniqueInviteId = UUID.randomUUID().toString();
        invite.setInviteId(uniqueInviteId);

        Invite savedInvite = inviteRepository.save(invite);
        Optional<EntityPreference> optionalEntityPreference = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, organization.getOrgId());
        if (optionalEntityPreference.isPresent() && optionalEntityPreference.get().getShouldInviteLinkSendToOrgAdmin()) {
            sendInvitationEmail(invite, organization.getOrganizationName(), organization.getOwnerEmail());
        }
        else {
            sendInvitationEmail(invite, organization.getOrganizationName(), null);
        }
        if (Objects.equals(invite.getEntityTypeId(), Constants.EntityTypes.ORG)) {
            auditService.auditForSendingOrgInvite(userAccountRepository.findByAccountIdAndIsActive(accountIdRequester, true), savedInvite);
        } else {
            auditService.auditForSendingTeamInvite(userAccountRepository.findByAccountIdAndIsActive(accountIdRequester, true), savedInvite);
        }
        return savedInvite;
    }

    /**
     * get the invite request in standard format
     */
    private void normalizeInviteRequest(InviteRequest request) {
        request.setPrimaryEmail(request.getPrimaryEmail().toLowerCase());
        request.setFirstName(CommonUtils.convertToTitleCase(request.getFirstName()));
        request.setMiddleName(CommonUtils.convertToTitleCase(request.getMiddleName()));
        request.setLastName(CommonUtils.convertToTitleCase(request.getLastName()));
    }

    /**
     * validate invite request and convert it to Invite entity
     */
    private Invite validateInviteRequestAndConvertToInviteEntity(InviteRequest request, Long accountIdRequester, String timeZone) {
        validateOrganizationExists(request.getEntityId());
        validateUserIsOrgAdmin(request.getEntityId(), accountIdRequester);
        validateUserNotRegistered(request.getPrimaryEmail(), request.getEntityId());
        validateInviteAlreadyExists(request, timeZone);

        return convertToInviteEntity(request);
    }

    /**
     * Ensures the existence of the organization by the given organization ID.
     */
    private void validateOrganizationExists(Long orgId) {
        organizationRepository.findById(orgId).orElseThrow(() -> new IllegalArgumentException("No such Organization exists"));
    }

    /**
     * Checks if the user, is an org admin of the specified organization
     */
    public void validateUserIsOrgAdmin(Long orgId, Long accountIdRequester) {
        boolean isUserOrgAdmin = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdAndIsActive(Constants.EntityTypes.ORG, orgId, accountIdRequester, true);

        if (!isUserOrgAdmin) {
            throw new ValidationFailedException("You're not authorized to send/ modify user invite");
        }
    }

    /**
     * Verifies that the user, identified by their email, is not already registered in the given organization
     */
    private void validateUserNotRegistered(String email, Long orgId) {
        UserAccount userAccount = userAccountRepository.findByEmailAndOrgIdAndIsActive(email, orgId, true);
        if (userAccount != null) {
            throw new ValidationFailedException("User is already registered in the organization");
        }
        UserAccount deactivatedUserAccount = userAccountRepository.findByEmailAndOrgIdAndIsActiveAndIsDisabledBySams(email, orgId, false, true);
        if (deactivatedUserAccount != null) {
            throw new ValidationFailedException("User is deactivated by the super admin, cannot register to the organization.");
        }
    }

    private void validateUserRegistered(String email, Long orgId, Long accountId) {
        UserAccount userAccount = userAccountRepository.findByEmailAndOrgIdAndIsActive(email, orgId, true);
        if (userAccount == null) {
            throw new ValidationFailedException("User is not registered in the organization");
        }

        if (Objects.equals(userAccount.getAccountId(), accountId)) {
            throw new ValidationFailedException("Users cannot send invitations to themselves");
        }
    }

    /**
     * Converts an InviteRequest object to an Invite entity object
     */
    private Invite convertToInviteEntity(InviteRequest request) {
        Invite invite = new Invite();
        BeanUtils.copyProperties(request, invite);
        return invite;
    }

    /**
     * Composes and sends an invitation email using the details from the Invite object
     */
    private void sendInvitationEmail(Invite invite, String orgName, String ownerEmail) {
        String trimmedOrgName = orgName.length() > 20 ? orgName.substring(0, 20) + "..." : orgName;
        String emailSubject = "Join " + trimmedOrgName + "'s Workspace on WFHTSE app by Vijayi WFH";

        String inviteLink = inviteApplicationDomain + Constants.InviteBaseDomain.INVITE_DOMAIN + "?inviteId=" + invite.getInviteId();

        String emailContent = buildEmailContent(invite, inviteLink, orgName);
        emailService.sendInviteEmail(invite.getPrimaryEmail(), emailSubject, emailContent, ownerEmail);
    }

    /**
     * Constructs the content of the invitation email using Thymeleaf template engine for organization.
     */
    private String buildEmailContent(Invite invite, String inviteLink, String orgName) {
        Context context = new Context();
        context.setVariable("firstName", invite.getFirstName());
        context.setVariable("inviteLink", inviteLink);
        context.setVariable("orgName", orgName);

        return templateEngine.process("inviteEmailTemplateInOrg", context);
    }

    /**
     * Constructs the content of the invitation email using Thymeleaf template engine for team.
     */
    private String buildEmailContentForTeam(Invite invite, String inviteLink, String teamName, String requesterFullName) {
        Context context = new Context();
        context.setVariable("firstName", invite.getFirstName());
        context.setVariable("inviteLink", inviteLink);
        context.setVariable("teamName", teamName);
        context.setVariable("validity", invite.getValidityDuration());
        context.setVariable("requester", requesterFullName);

        return templateEngine.process("inviteEmailTemplateInTeam", context);
    }

    /**
     * Retrieves detailed information about an invite, including user and device details if the user exists
     */
    @Override
    public OrganizationInviteResponse getInviteDetails(String inviteId, String timezone) {
        Invite invite = validateInviteId(inviteId, timezone);
        Organization organization = organizationRepository.findByOrgId(invite.getEntityId());

        boolean doesUserExist = userAccountRepository.existsByEmailAndIsActive(invite.getPrimaryEmail(), true);

        OrganizationInviteResponse organizationInviteResponse = new OrganizationInviteResponse();
        BeanUtils.copyProperties(invite, organizationInviteResponse);

        organizationInviteResponse.setOrganizationName(organization.getOrganizationName());

        if (doesUserExist) {
            User user = userService.getUserByUserName(invite.getPrimaryEmail());
            Device device = deviceService.getLatestDeviceInfoByUserId(user.getUserId());
            BeanUtils.copyProperties(user, organizationInviteResponse);
            organizationInviteResponse.setFirstName(invite.getFirstName());
            organizationInviteResponse.setLastName(invite.getLastName());
            organizationInviteResponse.setDeviceMake(device.getDeviceMake());
            organizationInviteResponse.setDeviceModel(device.getDeviceModel());
            organizationInviteResponse.setDeviceOs(device.getDeviceOs());
            organizationInviteResponse.setDeviceOsVersion(device.getDeviceOsVersion());
        }
        return organizationInviteResponse;
    }

    /**
     * Validates the given invite ID and checks for its expiry and active status based on the specified timezone
     */
    @Override
    public Invite validateInviteId(String inviteId, String timeZone) {
        // validates if the invite exists
        Invite invite = inviteRepository.findByInviteIdAndIsRevoked(inviteId, false).orElseThrow(() -> new InvalidInviteException("This invite is not valid. Please contact organization admin."));
        LocalDate userDate = CommonUtils.getLocalDateInGivenTimeZone(timeZone);

        if (invite.getIsAccepted()) {
            throw new InvalidInviteException("You have already accepted the invitation. You cannot accept it again.");
        }

        if (invite.getIsExpired() || userDate.isAfter(invite.getUserLocalSentDate().plusDays(invite.getValidityDuration()))) {
            throw new InvalidInviteException("Invite has expired");
        }

        return invite;
    }

    /**
     * Marks an invitation as revoked
     */
    @Override
    public void revokeInvite(String inviteId, String accountIds) {
        Long accountId = Long.parseLong(accountIds);
        Invite invite = inviteRepository.findByInviteIdAndIsRevoked(inviteId, false).orElseThrow(() -> new InvalidInviteException("Invalid invite id"));
        if (Objects.equals(invite.getEntityTypeId(), Constants.EntityTypes.ORG)) {
            validateUserIsOrgAdmin(invite.getEntityId(), accountId);
        } else {
            validateIfTeamAdmin(invite.getEntityId(), accountId);
        }
        invite.setIsRevoked(true);
        inviteRepository.save(invite);
    }

    @Override
    public void resendInvite(Invite invite, String accountIds) {
        Long accountId = Long.parseLong(accountIds);
        if (Objects.equals(invite.getEntityTypeId(), Constants.EntityTypes.ORG)) {
            validateUserIsOrgAdmin(invite.getEntityId(), accountId);
            Organization organization = organizationRepository.findByOrgId(invite.getEntityId());
            Optional<EntityPreference> optionalEntityPreference = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, organization.getOrgId());
            if (optionalEntityPreference.isPresent() && optionalEntityPreference.get().getShouldInviteLinkSendToOrgAdmin() != null && optionalEntityPreference.get().getShouldInviteLinkSendToOrgAdmin()) {
                sendInvitationEmail(invite, organization.getOrganizationName(), organization.getOwnerEmail());
            }
            else {
                sendInvitationEmail(invite, organization.getOrganizationName(), null);
            }

        } else if (Objects.equals(invite.getEntityTypeId(), Constants.EntityTypes.TEAM)) {
            validateIfTeamAdmin(invite.getEntityId(), accountId);
            UserAccount userAccount = userAccountRepository.findByAccountId(accountId);
            Team team = teamRepository.findByTeamId(invite.getEntityId());
            sendInvitationEmailForTeam(invite, team.getTeamName(), userAccount.getFkUserId().getFirstName() + " " + userAccount.getFkUserId().getLastName());
        }
    }

    /**
     * method to edit the validity duration of an existing invite
    */
    @Override
    public Invite editInviteValidity(String inviteId, int newDuration, String timeZone, String accountIds) {
        if (newDuration < 0) throw new IllegalArgumentException("newDuration value should be minimum 0 day");
        if (newDuration > 14) throw new IllegalArgumentException("newDuration value should be maximum 15 days");
        Long accountId = Long.parseLong(accountIds);
        Invite invite = inviteRepository.findByInviteIdAndIsRevoked(inviteId, false).orElseThrow(() -> new InvalidInviteException("Invite is invalid"));
        if (Objects.equals(invite.getEntityTypeId(), Constants.EntityTypes.ORG)) {
            validateUserIsOrgAdmin(invite.getEntityId(), accountId);
        } else {
            validateIfTeamAdmin(invite.getEntityId(), accountId);
        }        validateInviteNotExpiredAndDurationValid(invite, newDuration, timeZone);
        invite.setValidityDuration(newDuration);
        return inviteRepository.save(invite);
    }

    /**
     * validate that the invite is not expired and that the new duration doesn't result in an already expired invite
     */
    private void validateInviteNotExpiredAndDurationValid(Invite invite, int newDuration, String timeZone) {
        LocalDate userDate = CommonUtils.getLocalDateInGivenTimeZone(timeZone);
        LocalDate updatedExpiryDate = invite.getUserLocalSentDate().plusDays(newDuration);

        if (invite.getUserLocalSentDate().plusDays(invite.getValidityDuration()).isBefore(userDate)) {
            throw new InvalidInviteException("Invite has already expired");
        }

        if (updatedExpiryDate.isBefore(userDate)) {
            throw new ValidationFailedException("New duration results in an expired invite");
        }
    }

    /**
     * retrieves all invites associated with a specified organization ID, categorizes them into 'pending', 'accepted', 'expired', and 'revoked' based on their status, and dynamically updates the isExpired status of invites where necessary
     * (since the max timezone difference is 1 day or date, we are considering 2 day as buffer for determining isExpired)
     */
    @Override
    public InvitesResponse getOrganizationInvites(Long orgId, String accountIds, String timeZone) {
        Long accountIdOfUser = Long.parseLong(accountIds);
        validateUserIsOrgAdmin(orgId, accountIdOfUser);

        InvitesResponse response = new InvitesResponse();
        List<Invite> invites = inviteRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, orgId);
        LocalDate currentUserDate = CommonUtils.getLocalDateInGivenTimeZone(timeZone);

        for (Invite invite : invites) {
            LocalDate expiryDate = invite.getUserLocalSentDate().plusDays(invite.getValidityDuration());

            // Check for expired invites and update if necessary
            if (!invite.getIsAccepted() && !invite.getIsExpired() && expiryDate.plusDays(2).isBefore(currentUserDate)) {
                invite.setIsExpired(true);
                inviteRepository.save(invite);
            }

            // Categorize the invites
            if (invite.getIsAccepted()) {
                response.getAcceptedInvites().add(invite);
            } else if (invite.getIsExpired() || currentUserDate.isAfter(expiryDate)) {
                response.getExpiredInvites().add(invite);
            } else if (invite.getIsRevoked()) {
                response.getRevokedInvites().add(invite);
            } else {
                response.getPendingInvites().add(invite);
            }
        }

        response.getPendingInvites().sort(Comparator
                .comparing(Invite::getCreatedDateTime, Comparator.nullsLast(Comparator.reverseOrder()))
        );
        response.getAcceptedInvites().sort(Comparator
                .comparing(Invite::getFirstName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(Invite::getLastName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
        );
        response.getExpiredInvites().sort(Comparator
                .comparing(Invite::getFirstName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(Invite::getLastName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
        );
        response.getRevokedInvites().sort(Comparator
                .comparing(Invite::getFirstName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(Invite::getLastName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
        );
        return response;
    }

    @Override
    public InvitesResponse getTeamInvites(Long teamId, String accountIds, String timeZone) {
        Long accountIdOfUser = Long.parseLong(accountIds);
        validateIfTeamAdmin(teamId, accountIdOfUser);

        InvitesResponse response = new InvitesResponse();
        List<Invite> invites = inviteRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.TEAM, teamId);
        LocalDate currentUserDate = CommonUtils.getLocalDateInGivenTimeZone(timeZone);

        for (Invite invite : invites) {
            LocalDate expiryDate = invite.getUserLocalSentDate().plusDays(invite.getValidityDuration());

            // Check for expired invites and update if necessary
            if (!invite.getIsAccepted() && !invite.getIsExpired() && expiryDate.plusDays(2).isBefore(currentUserDate)) {
                invite.setIsExpired(true);
                inviteRepository.save(invite);
            }

            // Categorize the invites
            if (invite.getIsAccepted()) {
                response.getAcceptedInvites().add(invite);
            } else if (invite.getIsExpired() || currentUserDate.isAfter(expiryDate)) {
                response.getExpiredInvites().add(invite);
            } else if (invite.getIsRevoked()) {
                response.getRevokedInvites().add(invite);
            } else {
                response.getPendingInvites().add(invite);
            }
        }

        return response;
    }

    /**
     * validates details like name, email, etc. provided when sending the invite request is same as in the registration request
     */
    @Override
    public void validateInviteDetailsWithRegistrationRequest(RegistrationRequest request) {
        Invite invite = inviteRepository.findByInviteIdAndIsRevoked(request.getInviteId(), false).orElseThrow(() -> new InvalidInviteException("Incorrect invite id"));
        Organization organization = organizationRepository.findByOrganizationName(request.getOrganizationName()).orElseThrow(() -> new InvalidInviteException("Invalid org Id"));
        if (!Objects.equals(request.getFirstName(), invite.getFirstName()) || !Objects.equals(request.getLastName(), invite.getLastName())
                || !Objects.equals(request.getPrimaryEmail(), invite.getPrimaryEmail()) || !Objects.equals(organization.getOrgId(), invite.getEntityId())) {
            throw new IllegalArgumentException("Information doesn't match with the original invite request. Contact org admin");
        }
    }

    /**
     * mark the invite as accepted when the registration is done via the invite link
     */
    @Override
    public void markInviteAsAccepted(String inviteId) {
        Invite invite = inviteRepository.findByInviteIdAndIsRevoked(inviteId, false).orElseThrow(() -> new InvalidInviteException("Incorrect invite id"));
        invite.setIsAccepted(true);
        inviteRepository.save(invite);
    }

    /** validates whether the invite already exists and is Valid for the given email*/
    @Override
    public void validateInviteAlreadyExists(InviteRequest request, String timeZone) {
        List<Invite> invites = inviteRepository.findByPrimaryEmailAndIsRevoked(request.getPrimaryEmail(), false);
        LocalDate userDate = CommonUtils.getLocalDateInGivenTimeZone(timeZone);

        for(Invite invite: invites) {
            LocalDate expiryDate = invite.getUserLocalSentDate().plusDays(invite.getValidityDuration());
            if (Objects.equals(invite.getEntityTypeId(), Constants.EntityTypes.TEAM) && Objects.equals(request.getEntityTypeId(), Constants.EntityTypes.TEAM) && !Objects.equals(request.getEntityId(), invite.getEntityId())) {
                continue;
            }

            if (Objects.equals(request.getEntityTypeId(), Constants.EntityTypes.ORG) && Objects.equals(invite.getEntityTypeId(), Constants.EntityTypes.ORG)&& Objects.equals(request.getEntityId(), invite.getEntityId())) {
                if (invite.getIsAccepted() != null) {
                    if (invite.getIsAccepted()) {
                        if (userAccountRepository.existsByEmailAndOrgIdAndIsActive(invite.getPrimaryEmail(), invite.getEntityId(), true)) {
                            throw new ValidationFailedException("User already exists in the organization");
                        }
                    }
                    else if (!userDate.isAfter(expiryDate)) {
                        throw new ValidationFailedException("An already active invite exits for this user");
                    }
                }
            }
        }

    }

    /** generates invite response from invite object*/
//    private InviteResponse createInviteResponse(Invite invite) {
//        InviteResponse inviteResponse = new InviteResponse();
//        if (invite != null) {
//            BeanUtils.copyProperties(invite, inviteResponse);
//            Organization organization = organizationRepository.findByOrgId(invite.getOrgId());
//            inviteResponse.setOrganizationName(organization.getOrganizationName());
//        }
//        return inviteResponse;
//    }

    /**
     * validates an invitation request, generates a unique invite ID, saves the new invite to the repository, and sends out an invitation email with the team's name
     */
    @Override
    public Invite createInviteForTeam (InviteRequest request, Long accountIdRequester, String timeZone) {
        normalizeRequest(request);
        validateIfTeamAdmin(request.getEntityId(), accountIdRequester);
        validateUserRegistered(request.getPrimaryEmail(), Constants.OrgIds.PERSONAL.longValue(), accountIdRequester);
        validateInviteAlreadyExists(request, timeZone);
        Invite invite = convertToInviteEntity(request);
        Team team = teamRepository.findByTeamId(request.getEntityId());
        if (team == null) {
            throw new TeamNotFoundException();
        }
        // Generate a unique invite ID
        String uniqueInviteId = UUID.randomUUID().toString();
        invite.setInviteId(uniqueInviteId);

        Invite savedInvite = inviteRepository.save(invite);
        UserAccount userAccount = userAccountRepository.findByAccountId(accountIdRequester);
        sendInvitationEmailForTeam(invite, team.getTeamName(), userAccount.getFkUserId().getFirstName() + " " + userAccount.getFkUserId().getLastName());
        return savedInvite;
    }

    /**
     * Verifying if user is team admin
     */
    private void validateIfTeamAdmin (Long teamId, Long accountId) {
        boolean isUserTeamAdmin = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdAndIsActive(Constants.EntityTypes.TEAM, teamId, accountId, true);

        if (!isUserTeamAdmin) {
            throw new ValidationFailedException("You're not authorized to send/ modify user invite");
        }
    }

    /**
     * Composes and sends an invitation email using the details from the team Invite object
     */
    private void sendInvitationEmailForTeam(Invite invite, String teamName, String requesterFullName) {
        String emailSubject = "Your Invitation to Join " + teamName;
        String inviteLink = inviteApplicationDomain + Constants.InviteBaseDomain.INVITE_TO_TEAM_DOMAIN + "?inviteId=" + invite.getInviteId();

        String emailContent = buildEmailContentForTeam(invite, inviteLink, teamName, requesterFullName);
        emailService.sendInviteEmail(invite.getPrimaryEmail(), emailSubject, emailContent, null);
    }

    /**
     * Retrieves detailed information about a team invite
     */
    @Override
    public TeamInviteResponse getTeamInviteDetails(String inviteId, String timezone) {
        Invite invite = validateInviteId(inviteId, timezone);
        boolean doesUserExist = userAccountRepository.existsByEmailAndIsActive(invite.getPrimaryEmail(), true);

        TeamInviteResponse inviteResponse = new TeamInviteResponse();
        BeanUtils.copyProperties(invite, inviteResponse);
        Team team = teamRepository.findByTeamId(invite.getEntityId());
        inviteResponse.setTeamName(team.getTeamName());
        if (doesUserExist) {
            User user = userService.getUserByUserName(invite.getPrimaryEmail());
            BeanUtils.copyProperties(user, inviteResponse);
            inviteResponse.setFirstName(invite.getFirstName());
            inviteResponse.setLastName(invite.getLastName());
        }
        return inviteResponse;
    }

    private void normalizeRequest (InviteRequest request) {
        request.setPrimaryEmail(request.getPrimaryEmail().toLowerCase());
        request.setFirstName(CommonUtils.convertToTitleCase(request.getFirstName()));
        request.setMiddleName(CommonUtils.convertToTitleCase(request.getMiddleName()));
        request.setLastName(CommonUtils.convertToTitleCase(request.getLastName()));
    }
}
