package com.tse.core_application.custom.model.openfire;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoom {

    private String roomName;
    private String description;
    private String password;
    private String subject;
    private String naturalName;

    private int maxUsers;

    private Date creationDate;
    private Date modificationDate;

    private boolean persistent;
    private boolean publicRoom;
    private boolean registrationEnabled;
    private boolean canAnyoneDiscoverJID;
    private boolean canOccupantsChangeSubject;
    private boolean canOccupantsInvite;
    private boolean canChangeNickname;
    private boolean logEnabled;
    private boolean loginRestrictedToNickname;
    private boolean membersOnly;
    private boolean moderated;

    private List<String> broadcastPresenceRoles;

    private List<String> owners;
    private List<String> ownerGroups;

    private List<String> admins;
    private List<String> adminGroups;

    private List<String> members;
    private List<String> memberGroups;

    private List<String> outcasts;
    private List<String> outcastGroups;

    public ChatRoom(String naturalName, String roomName, String description) {
        this.naturalName = naturalName;
        this.roomName = roomName;
        this.description = description;
    }
}
