package com.example.chat_app.controller;

import com.example.chat_app.constants.Constants;
import com.example.chat_app.dto.*;
import com.example.chat_app.exception.InternalServerErrorException;
import com.example.chat_app.exception.NullPointerException;
import com.example.chat_app.exception.UnauthorizedLoginException;
import com.example.chat_app.handlers.CustomResponseHandler;
import com.example.chat_app.jwtUtils.JWTUtil;
import com.example.chat_app.model.Group;
import com.example.chat_app.repository.GroupRepository;
import com.example.chat_app.repository.UserRepository;
import com.example.chat_app.service.GroupService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ValidationException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@CrossOrigin(value = "*")
@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private static final Logger logger = LogManager.getLogger(GroupController.class);

    @Autowired
    private GroupService groupService;

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    // Endpoint to create a new group and add users
    @PostMapping("/create")
    public ResponseEntity<Object> createGroup(@RequestBody GroupDTO groupDTO, @RequestHeader List<Long> accountIds, HttpServletRequest request, @RequestHeader String screenName) {
        String jwtToken = request.getHeader("Authorization");
        jwtUtil.validateTokenAndAccountIds(jwtToken, screenName, accountIds);
        ThreadContext.put("accountIds", accountIds.toString());
        ThreadContext.put("requestURI", "api/groups/create");

        WebSocketUrlHeaders urlHeaders = new WebSocketUrlHeaders(-1L, screenName, accountIds, request.getHeader("timeZone"), jwtToken);
        GroupResponseDto savedGroup = groupService.createGroup(groupDTO, accountIds, urlHeaders);

        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, savedGroup);
    }

    // Endpoint to add a user to a group
    @PostMapping("/addUser")
    public ResponseEntity<Object> addUserToGroup(@RequestBody GroupAndUserDTO groupAndUserDTO, @RequestHeader List<Long> accountIds, HttpServletRequest request, @RequestHeader String screenName) throws Exception {
        try {

            String jwtToken = request.getHeader("Authorization");
            jwtUtil.validateTokenAndAccountIds(jwtToken, screenName, accountIds);
            ThreadContext.put("accountIds", accountIds.toString());
            ThreadContext.put("requestURI", "api/groups/addUser");

            Long groupId = groupAndUserDTO.getGroupId();
            Long userId = groupAndUserDTO.getUserId();
            WebSocketUrlHeaders urlHeaders = new WebSocketUrlHeaders(-1L, screenName, accountIds, request.getHeader("timeZone"), jwtToken);
            GroupResponseDto updatedGroup = groupService.addUserToGroup(groupId, userId, accountIds, urlHeaders);
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, updatedGroup);
        } catch (Exception e) {
            if (e.getMessage() == null) throw new InternalServerErrorException(e.getMessage());
            else
                throw e;
        }
    }

    @PostMapping("/bulkAddUsers")
    public ResponseEntity<Object> bulkAddUsersToGroup(@RequestBody GroupAndUsersDTO groupAndUsersDTO, @RequestHeader List<Long> accountIds, HttpServletRequest request, @RequestHeader String screenName) {
        String jwtToken = request.getHeader("Authorization");
        jwtUtil.validateTokenAndAccountIds(jwtToken, screenName, accountIds);
        ThreadContext.put("accountIds", accountIds.toString());
        ThreadContext.put("requestURI", "api/groups/bulkAddUsers");

        Long groupId = groupAndUsersDTO.getGroupId();
        List<Long> userIds = groupAndUsersDTO.getUserIds();

        WebSocketUrlHeaders urlHeaders = new WebSocketUrlHeaders(-1L, request.getHeader("screenName"), accountIds, request.getHeader("timeZone"), jwtToken);
        GroupResponseDto updatedGroup = groupService.bulkAddUsersToGroup(groupId, userIds, accountIds, urlHeaders);

        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, updatedGroup);
    }

    @PostMapping("/removeUser")
    public ResponseEntity<Object> removeUserFromGroup(@RequestBody GroupAndUserDTO groupAndUserDTO, @RequestHeader List<Long> accountIds, HttpServletRequest request, @RequestHeader String screenName) throws Exception {

            String jwtToken = request.getHeader("Authorization");
            jwtUtil.validateTokenAndAccountIds(jwtToken, screenName, accountIds);
            ThreadContext.put("accountIds", accountIds.toString());
            ThreadContext.put("requestURI", "api/groups/removeUser");

            Long groupId = groupAndUserDTO.getGroupId();
            Long userId = groupAndUserDTO.getUserId();

            WebSocketUrlHeaders urlHeaders = new WebSocketUrlHeaders(-1L, screenName, accountIds, request.getHeader("timeZone"), jwtToken);
            GroupResponseDto message = groupService.removeUserFromGroup(groupId, userId, accountIds, urlHeaders);
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, message);
    }

    @PostMapping("/bulkRemoveUsers")
    public ResponseEntity<Object> bulkRemoveUsersFromGroup(@RequestBody GroupAndUsersDTO groupAndUsersDTO, @RequestHeader List<Long> accountIds, HttpServletRequest request, @RequestHeader String screenName) {
        String jwtToken = request.getHeader("Authorization");
        jwtUtil.validateTokenAndAccountIds(jwtToken, screenName, accountIds);
        ThreadContext.put("accountIds", accountIds.toString());
        ThreadContext.put("requestURI", "api/groups/bulkRemoveUser");

        Long groupId = groupAndUsersDTO.getGroupId();
        List<Long> userIds = groupAndUsersDTO.getUserIds();

        WebSocketUrlHeaders urlHeaders = new WebSocketUrlHeaders(-1L, screenName, accountIds, request.getHeader("timeZone"), jwtToken);
        GroupResponseDto message = groupService.bulkRemoveUsersFromGroup(groupId, userIds, accountIds, urlHeaders);
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, message);
    }

    //Fetch all groups of a user
    @GetMapping("/user/{userId}")
    public ResponseEntity<Object> getGroupsByUser(@PathVariable Long userId, @RequestHeader List<Long> accountIds, HttpServletRequest request, @RequestHeader String screenName) {
        String jwtToken = request.getHeader("Authorization");
        jwtUtil.validateTokenAndAccountIds(jwtToken, screenName, accountIds);
        ThreadContext.put("accountIds", accountIds.toString());
        ThreadContext.put("requestURI", "api/groups/user");

        if (!Objects.equals(userId, userRepository.findFirstByAccountIdInAndIsActive(accountIds, true).getUserId()))
            throw new UnauthorizedLoginException("Wrong userId provided.");

        List<GroupResponseDto> groups = groupService.getGroupsByUser(userId);
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, groups);

    }

    @PostMapping("/setAdmin")
    public ResponseEntity<Object> setAdminForGroup(@RequestBody GroupAndUserDTO groupAndUserDTO, @RequestHeader List<Long> accountIds, HttpServletRequest request, @RequestHeader String screenName) {
        String jwtToken = request.getHeader("Authorization");
        jwtUtil.validateTokenAndAccountIds(jwtToken, screenName, accountIds);
        ThreadContext.put("accountIds", accountIds.toString());
        ThreadContext.put("requestURI", "api/groups/setAdmin");

        Long groupId = groupAndUserDTO.getGroupId();
        Long userId = groupAndUserDTO.getUserId();

        WebSocketUrlHeaders urlHeaders = new WebSocketUrlHeaders(-1L, screenName, accountIds, request.getHeader("timeZone"), jwtToken);
        GroupResponseDto updatedGroup = groupService.setAdminForGroup(groupId, userId, accountIds, urlHeaders);
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, updatedGroup);
    }

    @PostMapping("/removeAdmin")
    public ResponseEntity<Object> removeAdminForGroup(@RequestBody GroupAndUserDTO groupAndUserDTO, @RequestHeader List<Long> accountIds, HttpServletRequest request, @RequestHeader String screenName) {

        String jwtToken = request.getHeader("Authorization");
        jwtUtil.validateTokenAndAccountIds(jwtToken, screenName, accountIds);
        ThreadContext.put("accountIds", accountIds.toString());
        ThreadContext.put("requestURI", "api/groups/removeAdmin");

        Long groupId = groupAndUserDTO.getGroupId();
        Long userId = groupAndUserDTO.getUserId();

        WebSocketUrlHeaders urlHeaders = new WebSocketUrlHeaders(-1L, screenName, accountIds, request.getHeader("timeZone"), jwtToken);
        GroupResponseDto updatedGroup = groupService.removeAdminForGroup(groupId, userId, accountIds, urlHeaders);
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, updatedGroup);
    }
    /**
     * Edit/Update Custom Group
     * */
    @PostMapping("/edit")
    public ResponseEntity<Object> editGroup(@RequestBody GroupDTO group, @RequestHeader List<Long> accountIds, HttpServletRequest request, @RequestHeader String screenName) {

        String jwtToken = request.getHeader("Authorization");
        jwtUtil.validateTokenAndAccountIds(jwtToken, screenName, accountIds);
        ThreadContext.put("accountIds", accountIds.toString());
        ThreadContext.put("requestURI", "api/groups/removeUser");

        GroupResponseDto result = groupService.editGroup(group, accountIds);
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, result);

    }

    @GetMapping("/getGroup/{entityId}/{entityTypeId}")
    public ResponseEntity<Object> getGroupByEntityIdAndEntityType(@PathVariable ("entityId") Long entityId,
                                                                  @PathVariable ("entityTypeId") Long entityTypeId,
                                                                  @RequestHeader List<Long> accountIds, HttpServletRequest request, @RequestHeader String screenName) {
        try {
            String jwtToken = request.getHeader("Authorization");
            jwtUtil.validateTokenAndAccountIds(jwtToken, screenName, accountIds);
            ThreadContext.put("accountIds", accountIds.toString());
            ThreadContext.put("requestURI", "api/groups/getGroup 'ByEntity'");

            if(!accountIds.contains(0L))
                throw new ValidationException("User does not have access to get a Group.");

            Group group = groupService.findGroupByEntityTypeAndEntityId(entityId, entityTypeId);

            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, group);
        } catch (Exception e) {
            throw new InternalServerErrorException(e.getMessage());
        }
    }

    /** This API is used to update the SystemGroups which is generally called by Tse_Server.
     * */
    @PostMapping("/updateGroup/{entityId}/{entityTypeId}")
    public ResponseEntity<Object> updateGroupDetails(@PathVariable ("entityId") Long entityId,
                                                     @PathVariable ("entityTypeId") Long entityTypeId,
                                                     @RequestBody GroupUpdateRequest groupRequest,
                                                     @RequestHeader List<Long> accountIds, HttpServletRequest request, @RequestHeader String screenName) {
        try {
            String jwtToken = request.getHeader("Authorization");
            jwtUtil.validateTokenAndAccountIds(jwtToken, screenName, accountIds);
            ThreadContext.put("accountIds", accountIds.toString());
            ThreadContext.put("requestURI", "api/groups/updateGroup");

            if(!accountIds.contains(0L))
                throw new ValidationException("User does not have access to get a Group.");

            Group group = groupService.updateGroupDetails(entityId, entityTypeId, groupRequest);
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, group!=null ? "Not able to Update" : "Group Details Updated");
        } catch (Exception e) {
            throw new InternalServerErrorException(e.getMessage());
        }
    }

    @GetMapping("/getGroup/{groupId}")
    public ResponseEntity<Object> getGroupByGroupId(@PathVariable("groupId") Long groupId,
                                                    @RequestHeader List<Long> accountIds,
                                                    HttpServletRequest request, @RequestHeader String screenName) {

        String jwtToken = request.getHeader("Authorization");
        jwtUtil.validateTokenAndAccountIds(jwtToken, screenName, accountIds);
        ThreadContext.put("accountIds", accountIds.toString());
        ThreadContext.put("requestURI", "api/groups/getGroup");

        GroupResponseDto group = groupService.getGroupByGroupId(groupId, accountIds);
        if (group == null || group.getGroupId() == null) {
            throw new NullPointerException("No Group found for this GroupId");
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, group);

    }

    @PostMapping("/v2/bulkRemoveUsers")
    public ResponseEntity<Object> bulkRemoveUsersFromGroupV2(@RequestBody GroupByEntityUsersRequest groupByEntityUsersRequest,
                                                             @RequestHeader List<Long> accountIds, HttpServletRequest request, @RequestHeader String screenName) {
        String jwtToken = request.getHeader("Authorization");
        jwtUtil.validateTokenAndAccountIds(jwtToken, screenName, accountIds);
        ThreadContext.put("accountIds", accountIds.toString());
        ThreadContext.put("requestURI", "api/groups/v2/bulkRemoveUser");

        Group group = groupRepository.findTopByEntityTypeAndEntityTypeId(groupByEntityUsersRequest.getEntityId(), groupByEntityUsersRequest.getEntityTypeId().longValue());
        if(group != null ) {
            List<Long> userIds = groupByEntityUsersRequest.getUsersIds();
            WebSocketUrlHeaders urlHeaders = new WebSocketUrlHeaders(-1L, screenName, accountIds, request.getHeader("timeZone"), jwtToken);
            GroupResponseDto message = groupService.bulkRemoveUsersFromGroup(group.getGroupId(), userIds, accountIds, urlHeaders);
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, message);
        }
        else {
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, "System Group is not available for this Entity Type.");
        }
    }

    //Fetch all groups {majorly for migration purposes}
    @GetMapping("/allGroups")
    public ResponseEntity<Object> getAllGroups(@RequestHeader List<Long> accountIds, HttpServletRequest request, @RequestHeader String screenName) {
        String jwtToken = request.getHeader("Authorization");
        jwtUtil.validateTokenAndAccountIds(jwtToken, screenName, accountIds);
        ThreadContext.put("accountIds", accountIds.toString());
        ThreadContext.put("requestURI", "api/groups/user");

//        if (!Objects.equals(userId, userRepository.findFirstByAccountIdInAndIsActive(accountIds, true).getUserId()))
//            throw new UnauthorizedLoginException("Wrong userId provided.");

        List<GroupResponseDto> groups = groupService.getAllGroupsData();
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, groups);
    }

    @PostMapping("/activeInactiveGroup")
    public ResponseEntity<Object> activeInactiveCustomGroup(@RequestBody ActiveInactiveGroupDto activeInactiveGroupDto, @RequestHeader String accountIds,
                                                            HttpServletRequest request, @RequestHeader String screenName) {
        String jwtToken = request.getHeader("Authorization");
        List<Long> accountIdsLong = Arrays.stream(accountIds.split(",")).map(Long::valueOf).collect(Collectors.toList());
        jwtUtil.validateTokenAndAccountIds(jwtToken, screenName, accountIdsLong);
        ThreadContext.put("accountIds", accountIds);
        ThreadContext.put("requestURI", "api/groups/activeInactiveGroup");

        WebSocketUrlHeaders urlHeaders = new WebSocketUrlHeaders(-1L, screenName, accountIds, request.getHeader("timeZone"), jwtToken);
        GroupResponseDto responseDto = groupService.activeInactiveCustomGroup(activeInactiveGroupDto, accountIdsLong, urlHeaders);
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, responseDto.getLastMessage(), responseDto);
    }
}
