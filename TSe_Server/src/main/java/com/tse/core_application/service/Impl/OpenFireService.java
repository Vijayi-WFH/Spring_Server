package com.tse.core_application.service.Impl;

import com.tse.core_application.custom.model.openfire.*;
import com.tse.core_application.exception.OpenfireException;
import com.tse.core_application.model.Task;
import com.tse.core_application.model.Team;
import com.tse.core_application.model.User;
import com.tse.core_application.model.UserAccount;
import com.tse.core_application.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class OpenFireService {

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private TeamService teamService;

    @Autowired
    private OrganizationService organizationService;

    @Value("${openfire.add-member.url}")
    private String addMemberInRoomEndPointURL;

    @Value("${openfire.create-room.url}")
    private String createChatRoomEndPointURL;

    @Value("${openfire.update-room.url}")
    private String updateChatRoomEndPointURL;

    @Value("${openfire.create-user.url}")
    private String createUserEndPointUrl;

    @Value("${openfire.add-roster-entry.url}")
    private String addRosterEntryEndPointUrl;

    @Value("${openfire.create-group.url}")
    private String createChatGroupEndPointUrl;

    @Value("${openfire.add-member-to-chat-group.url}")
    private String addUserToChatGroupEndPointUrl;

    @Value("${openfire.add-group-to-chat-room.url}")
    private String addGroupToChatRoomUrl;

    @Value("${openfire.update-group.url}")
    private String updateChatGroupEndPointUrl;

    @Autowired
    UserAccountRepository userAccountRepository;

    @Value(("${xmpp.domain.name}"))
    private String xmppDomainName;

    public void setUpChatRoomWithGroup(Task task){
        String chatRoomName = createChatRoomForTask(task);
        String chatGroupName = createChatGroupForTask(task);
        addGroupToChatRoom(chatRoomName, chatGroupName);
    }

    /** This method is used to add team member/ access domain to chat room of that team */
    public boolean addMemberInChatRoom(Long entityId, Long accountId){

        Long teamId = entityId;
        User user = userAccountService.getActiveUserAccountByAccountId(accountId).getFkUserId();
        Team team = teamService.getTeamByTeamId(teamId);
//        String roomName = team.getTeamName().replaceAll("\\s", "") + "_" + team.getTeamId();
          String roomName = teamService.createChatRoomNameForTeam(teamId);
        String userName = user.getPrimaryEmail();

        String url = addMemberInRoomEndPointURL + "?roomName=" + roomName + "&userName=" + userName;

        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<OpenFireResponse> response = restTemplate.exchange(url, HttpMethod.PUT, null, OpenFireResponse.class);
            return true;
        } catch (Exception e){
            throw new OpenfireException(e.getMessage());
        }
    }

    /** This method is used to create a new chat room when a new team is created */
    public Boolean createChatRoomForTeam(Team team){
        ChatRoom chatRoom = new ChatRoom();
//        String JID = team.getTeamName().replaceAll("\\s", "") + "_" + team.getTeamId();
        String JID = teamService.createChatRoomNameForTeam(team.getTeamId());
        chatRoom.setNaturalName(team.getTeamName());
        chatRoom.setRoomName(JID);
        chatRoom.setDescription(team.getTeamDesc());
        chatRoom.setCreationDate(team.getCreatedDateTime());
        chatRoom.setPersistent(true);

        HttpEntity<ChatRoom> requestEntity = new HttpEntity<>(chatRoom);
        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<OpenFireResponse> response = restTemplate.exchange(createChatRoomEndPointURL, HttpMethod.POST, requestEntity, OpenFireResponse.class);
        } catch (Exception e){
            throw new OpenfireException(e.getMessage());
        }
        return true;
    }

    /**This method is used to create a chat room for the Task*/
    public String createChatRoomForTask(Task task){
        ChatRoom chatRoom = new ChatRoom();
        String JID = createChatRoomNameForTask(task.getTaskNumber());
        chatRoom.setNaturalName("Task " + task.getTaskNumber());
        chatRoom.setRoomName(JID);
        chatRoom.setDescription(task.getTaskTitle());
        Instant instant = task.getCreatedDateTime().atZone(ZoneId.systemDefault()).toInstant();
        chatRoom.setCreationDate(Date.from(instant));
        chatRoom.setPersistent(true);

        HttpEntity<ChatRoom> requestEntity = new HttpEntity<>(chatRoom);
        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<OpenFireResponse> response = restTemplate.exchange(createChatRoomEndPointURL, HttpMethod.POST, requestEntity, OpenFireResponse.class);
            return JID;
        } catch (Exception e){
            throw new OpenfireException(e.getMessage());
        }
    }

    /** This method is used to create a new group when a new task is created */
    public String createChatGroupForTask(Task task){
        ChatGroup chatGroup = new ChatGroup();
        String groupName = createChatRoomNameForTask(task.getTaskNumber());
        chatGroup.setName(groupName);
        chatGroup.setDescription(task.getTaskTitle());

        Set<String> usernames = Stream.of(
                        task.getFkAccountIdAssigned(),
                        task.getFkAccountIdCreator()
                )
                .filter(Objects::nonNull)
                .map(userAccount -> userAccountRepository.findByAccountId(userAccount.getAccountId()))
                .map(UserAccount::getEmail)
                .map(email -> email.replace("@", "_"))
                .collect(Collectors.toSet());

        chatGroup.setMembers(new ArrayList<>(usernames));
        chatGroup.setAdmins(Collections.emptyList());


        HttpEntity<ChatGroup> requestEntity = new HttpEntity<>(chatGroup);
        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<OpenFireResponse> response = restTemplate.exchange(createChatGroupEndPointUrl, HttpMethod.POST, requestEntity, OpenFireResponse.class);
            return groupName;
        } catch (Exception e){
            throw new OpenfireException(e.getMessage());
        }
    }

    /** This method is used to update the group for task when task_title or stakeholders are modified*/
    private String updateChatGroupForTask(Task task){
        ChatGroup chatGroup = new ChatGroup();
        String groupName = createChatRoomNameForTask(task.getTaskNumber());
        chatGroup.setName(groupName);
        chatGroup.setDescription(task.getTaskTitle());

        Set<String> usernames = Stream.of(
                        task.getFkAccountIdMentor1(),
                        task.getFkAccountIdMentor2(),
                        task.getFkAccountIdObserver1(),
                        task.getFkAccountIdObserver2(),
                        task.getFkAccountIdAssigned(),
                        task.getFkAccountIdAssignee()
                )
                .filter(Objects::nonNull)
                .map(userAccount -> userAccountRepository.findByAccountId(userAccount.getAccountId()))
                .map(UserAccount::getEmail)
                .map(email -> email.replace("@", "_"))
                .collect(Collectors.toSet());

        chatGroup.setMembers(new ArrayList<>(usernames));
        chatGroup.setAdmins(Collections.emptyList());

        HttpEntity<ChatGroup> requestEntity = new HttpEntity<>(chatGroup);
        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<OpenFireResponse> response = restTemplate.exchange(updateChatGroupEndPointUrl, HttpMethod.POST, requestEntity, OpenFireResponse.class);
            return groupName;
        } catch (Exception e){
            throw new OpenfireException(e.getMessage());
        }
    }
    /** This method adds a user to the existing chat group */
    public void addUserToChatGroup(String userName, String groupName){

        RestTemplate restTemplate = new RestTemplate();
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("userName", userName);
        map.add("groupName", groupName);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
        try {
            ResponseEntity<OpenFireResponse> responseEntity = restTemplate.exchange(addUserToChatGroupEndPointUrl, HttpMethod.PUT, request, OpenFireResponse.class);
        } catch (Exception e){
            throw new OpenfireException(e.getMessage());
        }
    }
    /** This method is used to add group to the chat room as a member */
    public void addGroupToChatRoom(String roomName, String groupName){

        RestTemplate restTemplate = new RestTemplate();
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("roomName", roomName);
        map.add("groupName", groupName);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
        try {
            ResponseEntity<OpenFireResponse> responseEntity = restTemplate.exchange(addGroupToChatRoomUrl, HttpMethod.PUT, request, OpenFireResponse.class);
        } catch (Exception e){
            throw new OpenfireException(e.getMessage());
        }
    }

    /** This method is used to update the chat room name when the team name is updated */
    public void updateChatRoomForTeam(Team team){
        ChatRoom chatRoom = new ChatRoom();
        String JID = teamService.createChatRoomNameForTeam(team.getTeamId());
        chatRoom.setNaturalName(team.getTeamName());
        chatRoom.setRoomName(JID);
        chatRoom.setDescription(team.getTeamDesc());

        HttpEntity<ChatRoom> requestEntity = new HttpEntity<>(chatRoom);
        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<OpenFireResponse> response = restTemplate.exchange(updateChatRoomEndPointURL, HttpMethod.PUT, requestEntity, OpenFireResponse.class);
        } catch (Exception e){
            throw new OpenfireException(e.getMessage());
        }
    }
    /** This method is used to update the chat room name when a task is updated */

    private void updateChatRoomForTask(Task task){
        ChatRoom chatRoom = new ChatRoom();
        String JID = createChatRoomNameForTask(task.getTaskNumber());
        chatRoom.setNaturalName("Task " + task.getTaskNumber());
        chatRoom.setRoomName(JID);
        chatRoom.setDescription(task.getTaskTitle());
        Instant instant = task.getLastUpdatedDateTime().atZone(ZoneId.systemDefault()).toInstant();
        chatRoom.setModificationDate(Date.from(instant));
        chatRoom.setPersistent(true);

        HttpEntity<ChatRoom> requestEntity = new HttpEntity<>(chatRoom);
        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<OpenFireResponse> response = restTemplate.exchange(createChatRoomEndPointURL, HttpMethod.POST, requestEntity, OpenFireResponse.class);
        } catch (Exception e){
            throw new OpenfireException(e.getMessage());
        }
    }

    /** this method is used to create a new user on openfire server when a new user sign up on the application**/
    public void createChatUser(User user){

        ChatUser chatUser = new ChatUser();
        chatUser.setEmail(user.getPrimaryEmail());
        chatUser.setUsername(user.getChatUserName());
        chatUser.setName(user.getFirstName() + " " + user.getLastName());
        chatUser.setPassword(user.getChatPassword());
        chatUser.setProperties(Collections.emptyList());

        HttpEntity<ChatUser> requestEntity = new HttpEntity<>(chatUser);
        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<OpenFireResponse> response = restTemplate.exchange(createUserEndPointUrl, HttpMethod.POST, requestEntity, OpenFireResponse.class);
        } catch (Exception e){
            throw new OpenfireException(e.getMessage());
        }

    }

    /** this method adds the user to all roster of all other users in the org and all users in the org to new user's roster **/
    public void addRosterEntry(String orgName, String userName){

        Long org_id = organizationService.getOrganizationByOrganizationName(orgName.trim().replaceAll("\\s+", " ")).getOrgId();
        List<UserAccount> userAccounts = userAccountService.getAllUserAccountsByOrgId(org_id);

        // url for the new user
        String newUserUrl = addRosterEntryEndPointUrl + "?userName=" + userName.replace("@", "_");

        // rosterItemEntity for the new user
        RosterItemEntity rosterItemEntityNewUser = new RosterItemEntity();
        rosterItemEntityNewUser.setJid(createJID(userName));
        rosterItemEntityNewUser.setSubscriptionType(3);
        rosterItemEntityNewUser.setGroups(Collections.emptyList());
        rosterItemEntityNewUser.setNickname(null);


        for(UserAccount userAccount: userAccounts){
            User user = userAccount.getFkUserId();

            if(!userName.equals(user.getPrimaryEmail())){

                String jid = createJID(user.getPrimaryEmail());

                RosterItemEntity rosterItemEntity = new RosterItemEntity();
                rosterItemEntity.setJid(jid);
                rosterItemEntity.setSubscriptionType(3);
                rosterItemEntity.setGroups(Collections.emptyList());
                rosterItemEntity.setNickname(null);

                HttpEntity<RosterItemEntity> requestEntity = new HttpEntity<>(rosterItemEntity);
                    RestTemplate restTemplate = new RestTemplate();
                    try {

                        // adding the org members in new user's roster
                        ResponseEntity<OpenFireResponse> response = restTemplate.exchange(newUserUrl, HttpMethod.POST, requestEntity, OpenFireResponse.class);
//                        System.out.println("member added to new user roster: " + response.getBody().getData()); // for testing


                        // adding new user in org members rosters
                        ResponseEntity<OpenFireResponse> response2 = restTemplate.exchange(addRosterEntryEndPointUrl + "?userName=" + user.getPrimaryEmail().replace("@", "_"), HttpMethod.POST, new HttpEntity<>(rosterItemEntityNewUser), OpenFireResponse.class);
//                        System.out.println(response2.getBody().getData()); // for testing

                    } catch (Exception e){
                        throw new OpenfireException(e.getMessage());
                    }
            }
        }

    }

    private String createJID(String userName){

        String Jid =  userName.replace("@", "_") + xmppDomainName;
        return Jid;
    }

    /** creates chat room name for a given task number*/
    private String createChatRoomNameForTask(String taskNumber){
        String chatRoomName = "Task" + "_" + taskNumber;
        return chatRoomName;
    }

    /** This method is called from update task API to update chat group/ room when a task is updated*/
    public void updateChatGroupAndChatRoom(List<String> updateFieldsByUser, Task task){

        List<String> fieldsToCheckForOpenfireUpdate = List.of("taskTitle", "fkAccountIdMentor1", "fkAccountIdMentor2", "fkAccountIdObserver1", "fkAccountIdObserver2", "fkAccountIdAssigned", "fkAccountIdAssignee");
        for (String str : fieldsToCheckForOpenfireUpdate) {
            if (updateFieldsByUser.contains(str)) {
                updateChatGroupForTask(task);
                break;
            }
        }

        if(updateFieldsByUser.contains("taskTitle")){
            updateChatRoomForTask(task);
        }
    }

}
