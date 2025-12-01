package com.tse.core_application.service;

import com.tse.core_application.dto.*;
import com.tse.core_application.model.Invite;

public interface IInviteService {

    Invite createInvite(InviteRequest inviteRequest, Long accountIdRequester, String timeZone);

    Invite createInviteForTeam(InviteRequest inviteRequest, Long accountIdRequester, String timeZone);

    OrganizationInviteResponse getInviteDetails(String inviteId, String timezone);

    Invite validateInviteId(String inviteId, String timeZone);

    void revokeInvite(String inviteId, String accountIds);

    Invite editInviteValidity(String inviteId, int newDuration, String timeZone, String accountIds);

    InvitesResponse getOrganizationInvites(Long orgId, String accountIds, String timeZone);

    void validateInviteDetailsWithRegistrationRequest(RegistrationRequest request);

    void markInviteAsAccepted(String inviteId);

    InvitesResponse getTeamInvites(Long teamId, String accountIds, String timeZone);

    void validateInviteAlreadyExists(InviteRequest request, String timeZone);

    TeamInviteResponse getTeamInviteDetails(String inviteId, String timezone);

    void resendInvite(Invite invite, String accountIds);

}
