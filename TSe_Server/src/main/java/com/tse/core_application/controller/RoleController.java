package com.tse.core_application.controller;

import com.tse.core_application.custom.model.RoleIdRoleNameRoleDesc;
import com.tse.core_application.model.User;
import com.tse.core_application.exception.InternalServerErrorException;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.service.Impl.RoleService;
import com.tse.core_application.service.Impl.UserService;
import com.tse.core_application.utils.JWTUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@CrossOrigin(value = "*")
@RestController
@RequestMapping(path = "/role")
public class RoleController {

    private static final Logger logger = LogManager.getLogger(RoleController.class.getName());

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserService userService;

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private RequestHeaderHandler requestHeaderHandler;

    @GetMapping(path = "/getAllRoles")
    public ResponseEntity<Object> getAllRoles(@RequestParam(name = "entityType", required = false) Integer entityType, @RequestHeader(name = "accountIds") String accountIds, @RequestHeader(name = "screenName") String screenName,
                                              @RequestHeader(name = "timeZone") String timeZone, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getAllRoles" + '"' + " method ...");
        List<RoleIdRoleNameRoleDesc> roles;
        try {
            roles = roleService.getRoles(entityType);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getAllRoles" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch(Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getAllRoles for the username = " + foundUser.getPrimaryEmail() +
                    " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return roleService.getAllRolesFormattedResponse(roles);

    }

    @GetMapping(path = "/getTeamRoles")
    public ResponseEntity<Object> getTeamRoles(@RequestHeader(name = "accountIds") String accountIds, @RequestHeader(name = "screenName") String screenName,
                                              @RequestHeader(name = "timeZone") String timeZone, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getTeamRoles" + '"' + " method ...");
        List<RoleIdRoleNameRoleDesc> roles;
        try {
            roles = roleService.getTeamRoles();
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getTeamRoles" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch(Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute getTeamRoles for the username = " + foundUser.getPrimaryEmail() +
                    " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return roleService.getAllRolesFormattedResponse(roles);

    }
}
