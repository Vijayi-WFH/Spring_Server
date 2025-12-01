package com.example.chat_app.controller;

import com.example.chat_app.constants.Constants;
import com.example.chat_app.dto.ActivateDeactivateUserRequest;
import com.example.chat_app.dto.UserDto;
import com.example.chat_app.dto.WebSocketUrlHeaders;
import com.example.chat_app.exception.UnauthorizedLoginException;
import com.example.chat_app.handlers.CustomResponseHandler;
import com.example.chat_app.model.User;
import com.example.chat_app.jwtUtils.JWTUtil;
import com.example.chat_app.repository.UserRepository;
import com.example.chat_app.service.UserService;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ValidationException;
import java.util.ArrayList;
import java.util.List;

@CrossOrigin(value = "*")
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JWTUtil jwtUtil;

    // POST method to create a new user
    @PostMapping("/create")
    public ResponseEntity<Object> createUser(@RequestBody User user, @RequestHeader List<Long> accountIds, HttpServletRequest request, @RequestHeader String screenName) throws Exception {

        String jwtToken = request.getHeader("Authorization");
        jwtUtil.validateTokenAndAccountIds(jwtToken, screenName, accountIds);

        if(!accountIds.contains(0L))
            throw new ValidationException("User does not have access to create a new user.");

        User createdUser = userService.createUser(user);
        return CustomResponseHandler.generateCustomResponse(HttpStatus.CREATED, Constants.FormattedResponse.SUCCESS, createdUser);
    }

    @PostMapping("/bulkCreate")
    public ResponseEntity<Object> bulkCreateUser(@RequestBody List<User> users, @RequestHeader List<Long> accountIds, HttpServletRequest request, @RequestHeader String screenName) throws Exception {

        String jwtToken = request.getHeader("Authorization");
        jwtUtil.validateTokenAndAccountIds(jwtToken, screenName, accountIds);

        if(!accountIds.contains(0L))
            throw new ValidationException("User does not have access to create a new user.");
        List<User> responseList = new ArrayList<>();
        for (User user : users) {
            User createdUser = userService.createUser(user);
            responseList.add(createdUser);
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.CREATED, Constants.FormattedResponse.SUCCESS, responseList);
    }

    @GetMapping("/all")
    public ResponseEntity<Object> getUsers(@RequestHeader List<Long> accountIds, HttpServletRequest request, @RequestHeader String screenName) throws Exception {
        try {

            String jwtToken = request.getHeader("Authorization");
            jwtUtil.validateTokenAndAccountIds(jwtToken, screenName, accountIds);
            List<UserDto> usersData = userService.findAllUsers(accountIds);
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, usersData);
        }
        catch (Exception e) {
            if(e.getMessage()==null) return new ResponseEntity<>(e, HttpStatus.INTERNAL_SERVER_ERROR);
            else throw e;
        }
    }

    @GetMapping("/{orgId}")
    public ResponseEntity<Object> getUsersByOrgId(@RequestHeader List<Long> accountIds, HttpServletRequest request, @RequestHeader String screenName, @PathVariable Long orgId) throws Exception {
        try {

            String jwtToken = request.getHeader("Authorization");
            jwtUtil.validateTokenAndAccountIds(jwtToken, screenName, accountIds);

            User user = userRepository.findFirstByOrgIdInAndAccountIdInAndIsActive(List.of(orgId), accountIds, true);

            if(!accountIds.contains(user.getAccountId()))
                throw new UnauthorizedLoginException("User is not part of this organisation.");

            List<User> users = userRepository.findDistinctByOrgIdIn(List.of(orgId));
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, users);
        }
        catch (Exception e) {
            if(e.getMessage()==null) return new ResponseEntity<>(e, HttpStatus.INTERNAL_SERVER_ERROR);
            else throw e;
        }
    }

    @GetMapping("/getUser")
    public ResponseEntity<Object> findByUser(@RequestHeader List<Long> accountIds, HttpServletRequest request, @RequestHeader String screenName) throws Exception {
        try {

            String jwtToken = request.getHeader("Authorization");
            jwtUtil.validateTokenAndAccountIds(jwtToken, screenName, accountIds);

            List<User> users = userRepository.findByAccountIdIn(accountIds);
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, users);
        }
        catch (Exception e) {
            if(e.getMessage()==null) return new ResponseEntity<>(e, HttpStatus.INTERNAL_SERVER_ERROR);
            else throw e;
        }
    }

    @PostMapping("/deleteUser/{accountId}")
    public ResponseEntity<String> deleteUserByUserIdAndAccountId(@PathVariable Long accountId,
                                                                 @RequestHeader List<Long> accountIds, HttpServletRequest request,
                                                                 @RequestHeader String screenName) throws Exception {
        String jwtToken = request.getHeader("Authorization");
        jwtUtil.validateTokenAndAccountIds(jwtToken, screenName, accountIds);

        if(!accountIds.contains(0L))
            throw new ValidationException("User does not have access to delete a new user.");

        return new ResponseEntity<>(userService.deleteUser(accountId), HttpStatus.CREATED);
    }

    @PostMapping("/changeUsername/{accountId}")
    public ResponseEntity<String> changeUserName(@PathVariable Long accountId,
                                                 @RequestParam String firstName,
                                                 @RequestParam String middleName,
                                                 @RequestParam String lastName,
                                                 @RequestHeader List<Long> accountIds, HttpServletRequest request,
                                                 @RequestHeader String screenName) throws Exception {
        String jwtToken = request.getHeader("Authorization");
        jwtUtil.validateTokenAndAccountIds(jwtToken, screenName, accountIds);

        if(!accountIds.contains(0L))
            throw new ValidationException("User does not have access to delete a new user.");

        return new ResponseEntity<>(userService.changeUserName(accountId, firstName, middleName, lastName), HttpStatus.ACCEPTED);
    }

    @PostMapping("/v2/deleteUser")
    public ResponseEntity<String> activateDeactivateUser(@RequestBody ActivateDeactivateUserRequest userRequest,
                                                         @RequestHeader List<Long> accountIds, HttpServletRequest request,
                                                         @RequestHeader String screenName){
        String jwtToken = request.getHeader("Authorization");
        jwtUtil.validateTokenAndAccountIds(jwtToken, screenName, accountIds);
        ThreadContext.put("accountIds", accountIds.toString());
        ThreadContext.put("requestURI", "/api/users/v2/deleteUser");
        //allowed only to be call from tse_server
        if(!accountIds.contains(0L))
            throw new ValidationException("User does not have access to activate/deactivate user.");

        WebSocketUrlHeaders urlHeaders = new WebSocketUrlHeaders(-1L, screenName, accountIds, request.getHeader("timeZone"), jwtToken);
        return new ResponseEntity<>(userService.activateDeactivateUserFromGroups(userRequest, urlHeaders), HttpStatus.CREATED);
    }
}
