package com.tse.core_application.dto;

import com.tse.core_application.model.Invite;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InvitesResponse {
    private List<Invite> pendingInvites = new ArrayList<>();
    private List<Invite> acceptedInvites = new ArrayList<>();
    private List<Invite> expiredInvites = new ArrayList<>();
    private List<Invite> revokedInvites = new ArrayList<>();
}
