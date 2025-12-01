package com.example.chat_app.controller;

import com.example.chat_app.constants.Constants;
import com.example.chat_app.dto.GroupResponseDto;
import com.example.chat_app.dto.MessageResponse;
import com.example.chat_app.dto.MessageUserInfoRequest;
import com.example.chat_app.dto.MessageUserInfoResponse;
import com.example.chat_app.exception.UnauthorizedLoginException;
import com.example.chat_app.exception.ValidationFailedException;
import com.example.chat_app.handlers.CustomResponseHandler;
import com.example.chat_app.jwtUtils.JWTUtil;
import com.example.chat_app.model.Group;
import com.example.chat_app.model.Message;
import com.example.chat_app.model.User;
import com.example.chat_app.repository.GroupRepository;
import com.example.chat_app.repository.UserRepository;
import com.example.chat_app.service.GroupService;
import com.example.chat_app.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@CrossOrigin(value = "*")
@RestController
@RequestMapping("/api/message")
public class MessageController {

    @Autowired
    private GroupService groupService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WebSocketController webSocketController;

    @Autowired
    private GroupRepository groupRepository;

    @GetMapping("/{senderId}/{receiverId}")
    public ResponseEntity<Object> home(@PathVariable(name = "senderId") Long senderId, @PathVariable(name = "receiverId") Long receiverId, @RequestHeader List<Long> accountIds, HttpServletRequest request, @RequestHeader String screenName, @RequestHeader String timeZone) throws Exception {
        try {
            String jwtToken = request.getHeader("Authorization");
            jwtUtil.validateTokenAndAccountIds(jwtToken, screenName, accountIds);

            if (!accountIds.contains(senderId) && !accountIds.contains(receiverId))
                throw new UnauthorizedLoginException("Invalid senderId/receiverId.");

            List<Message> messages = messageService.getMessagesByReceiverId(senderId, receiverId, timeZone);

            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, messages);
        } catch (Exception e) {
            if (e.getMessage() == null) return new ResponseEntity<>(e, HttpStatus.INTERNAL_SERVER_ERROR);
            else throw e;
        }
    }

    @GetMapping("/group/{userId}/{groupId}")
    public ResponseEntity<Object> getMessagesByGroup(@PathVariable Long userId, @PathVariable Long groupId, @RequestHeader List<Long> accountIds, HttpServletRequest request, @RequestHeader String screenName, @RequestHeader String timeZone) throws Exception {
        try {

            String jwtToken = request.getHeader("Authorization");
            jwtUtil.validateTokenAndAccountIds(jwtToken, screenName, accountIds);

            if (!Objects.equals(userId, userRepository.findFirstByAccountIdInAndIsActive(accountIds, true).getUserId()))
                throw new UnauthorizedLoginException("Wrong userId provided.");

            List<GroupResponseDto> groups = groupService.getGroupsByUser(userId);
            Optional<GroupResponseDto> matchingGroup = groups.stream()
                    .filter(group -> Objects.equals(group.getGroupId(), groupId))
                    .findFirst();
            if (matchingGroup.isEmpty())
                throw new IllegalStateException("User is not part of this group.");
            List<Message> messages = messageService.getMessagesByGroupId(groupId, timeZone);

            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, messages);
        } catch (Exception e) {
            if (e.getMessage() == null) return new ResponseEntity<>(e, HttpStatus.INTERNAL_SERVER_ERROR);
            else throw e;
        }
    }

    @GetMapping("/v2/{senderId}/{receiverId}")
    public ResponseEntity<Object> home(@PathVariable(name = "senderId") Long senderId, @PathVariable(name = "receiverId") Long receiverId,
                                       @RequestParam(name = "messageId", defaultValue = "0") Long messageId, @RequestParam(name = "size", defaultValue = "25") int size,
                                       @RequestHeader List<Long> accountIds, HttpServletRequest request, @RequestHeader String screenName, @RequestHeader String timeZone) throws Exception {
        try {
            String jwtToken = request.getHeader("Authorization");
            jwtUtil.validateTokenAndAccountIds(jwtToken, screenName, accountIds);

            if (!accountIds.contains(senderId))
                throw new UnauthorizedLoginException("Invalid senderId.");

            Page<MessageResponse> messages = messageService.getMessagesByReceiverId(senderId, receiverId, timeZone, messageId, size);

            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, messages);
        } catch (Exception e) {
            if (e.getMessage() == null) return new ResponseEntity<>(e, HttpStatus.INTERNAL_SERVER_ERROR);
            else throw e;
        }
    }

    @GetMapping("/v2/group/{userId}/{groupId}")
    public ResponseEntity<Object> getMessagesByGroup(@PathVariable Long userId, @PathVariable Long groupId, @RequestParam(name = "messageId", defaultValue = "0") Long messageId,
                                                     @RequestParam(name = "size", defaultValue = "25") int size,
                                                     @RequestParam(name = "page", defaultValue = "0") int pageNo,
                                                     @RequestHeader List<Long> accountIds, HttpServletRequest request, @RequestHeader String screenName, @RequestHeader String timeZone) throws Exception {
        try {

            String jwtToken = request.getHeader("Authorization");
            jwtUtil.validateTokenAndAccountIds(jwtToken, screenName, accountIds);

            if (!Objects.equals(userId, userRepository.findFirstByAccountIdInAndIsActive(accountIds, true).getUserId()))
                throw new UnauthorizedLoginException("Wrong userId provided.");

            List<GroupResponseDto> groups = groupService.getGroupsByUser(userId);
            Optional<GroupResponseDto> matchingGroup = groups.stream()
                    .filter(group -> Objects.equals(group.getGroupId(), groupId))
                    .findFirst();
            if (matchingGroup.isEmpty())
                throw new IllegalStateException("User is not part of this group.");

            User reqUser = userRepository.findByUserIdAndOrgId(userId, matchingGroup.get().getOrgId()).orElseThrow(() -> new ValidationFailedException("User Not Found!"));
            Page<MessageResponse> messages = messageService.getMessagesByGroupId(messageId, reqUser.getAccountId(), groupId, timeZone, size, pageNo);
            
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, messages);
        } catch (Exception e) {
            if (e.getMessage() == null) return new ResponseEntity<>(e, HttpStatus.INTERNAL_SERVER_ERROR);
            else throw e;
        }
    }

    @GetMapping("/audit/{senderId}/{receiverId}")
    public ResponseEntity<Object> auditDirect(@PathVariable(name = "senderId") Long senderId, @PathVariable(name = "receiverId") Long receiverId, @RequestParam(name = "messageId", defaultValue = "0") Long messageId, @RequestParam(name = "size", defaultValue = "25") int size, @RequestHeader List<Long> accountIds, HttpServletRequest request, @RequestHeader String screenName, @RequestHeader String timeZone) throws Exception {
        try {
            String jwtToken = request.getHeader("Authorization");
            jwtUtil.validateTokenAndAccountIds(jwtToken, screenName, accountIds);

            if (accountIds.size() > 1)
                throw new UnauthorizedLoginException("Please provide exactly one accountId.");

            User user = userRepository.findFirstByAccountIdInAndIsActive(accountIds, true);

            User sender = userRepository.findByAccountIdAndIsActive(senderId, true);

            if (!accountIds.contains(senderId) && !(user.getIsOrgAdmin() && user.getOrgId().equals(sender.getOrgId())))
                throw new UnauthorizedLoginException("Invalid senderId.");

            Page<MessageResponse> messages = messageService.getMessagesByReceiverId(senderId, receiverId, timeZone, messageId, size);

            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, messages);
        } catch (Exception e) {
            if (e.getMessage() == null) return new ResponseEntity<>(e, HttpStatus.INTERNAL_SERVER_ERROR);
            else throw e;
        }
    }

    @GetMapping("/audit/group/{userId}/{groupId}")
    public ResponseEntity<Object> auditGroup(@PathVariable Long userId, @PathVariable Long groupId, @RequestParam(name = "messageId", defaultValue = "0") Long messageId,
                                             @RequestParam(name = "size", defaultValue = "25") int size, @RequestParam(name = "page", defaultValue = "0") int pageNo,
                                             @RequestHeader List<Long> accountIds, HttpServletRequest request, @RequestHeader String screenName, @RequestHeader String timeZone) throws Exception {
        try {

            String jwtToken = request.getHeader("Authorization");
            jwtUtil.validateTokenAndAccountIds(jwtToken, screenName, accountIds);

            User user = userRepository.findFirstByAccountIdInAndIsActive(accountIds, true);

            if (!Objects.equals(userId, user.getUserId()))
                throw new UnauthorizedLoginException("Wrong userId provided.");

            List<Group> groups = groupRepository.findForAdminOrderByLastMessageTimestampDescGroupIdDesc(user.getOrgId());
            Optional<Group> matchingGroup = groups.stream()
                    .filter(group -> Objects.equals(group.getGroupId(), groupId) || (Objects.equals(group.getOrgId(), user.getOrgId()) && user.getIsOrgAdmin()))
                    .findFirst();
            if (matchingGroup.isEmpty())
                throw new IllegalStateException("User is not part of this group.");
            User reqUser = userRepository.findByUserIdAndOrgId(userId, matchingGroup.get().getOrgId()).orElseThrow(() -> new ValidationFailedException("User Not Found!"));
            Page<MessageResponse> messages = messageService.getMessagesByGroupId(messageId, reqUser.getAccountId(), groupId, timeZone, size, pageNo);

            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, messages);
        } catch (Exception e) {
            if (e.getMessage() == null) return new ResponseEntity<>(e, HttpStatus.INTERNAL_SERVER_ERROR);
            else throw e;
        }
    }

    @PostMapping("/getMessageInfo")
    public ResponseEntity<Object> getMessageInfo(@RequestBody MessageUserInfoRequest userInfoRequest,
                                                 @RequestHeader List<Long> accountIds, HttpServletRequest request,
                                                 @RequestHeader String screenName, @RequestHeader String timeZone) {

        String jwtToken = request.getHeader("Authorization");
        jwtUtil.validateTokenAndAccountIds(jwtToken, screenName, accountIds);
        MessageUserInfoResponse response = messageService.getMessageUserInfo(userInfoRequest, timeZone);
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
    }

}
