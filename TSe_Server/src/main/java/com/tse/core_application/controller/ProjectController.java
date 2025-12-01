package com.tse.core_application.controller;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.custom.model.*;
import com.tse.core_application.dto.*;
import com.tse.core_application.dto.project.ProjectRequest;
import com.tse.core_application.dto.project.ProjectResponse;
import com.tse.core_application.dto.project.UpdateProjectRequest;
import com.tse.core_application.dto.project.UserAllProjectResponseWithOrg;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.model.User;
import com.tse.core_application.exception.InternalServerErrorException;
import com.tse.core_application.exception.NoDataFoundException;
import com.tse.core_application.exception.ProjectNotFoundException;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.Project;
import com.tse.core_application.repository.*;
import com.tse.core_application.service.Impl.ProjectService;
import com.tse.core_application.service.Impl.UserService;
import com.tse.core_application.utils.JWTUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.security.InvalidKeyException;
import java.util.*;

@CrossOrigin(value = "*")
@RestController
@RequestMapping(path = "/project")
public class ProjectController {

    private static final Logger logger = LogManager.getLogger(ProjectController.class.getName());
    @Autowired
    UserRepository userRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private RequestHeaderHandler requestHeaderHandler;
    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private ProjectService projectService;
    @Autowired
    private UserService userService;

    //  endPoint to get projectIds, projectNames by orgId
    @GetMapping(path = "/projectListByOrgId/{orgId}")
    public ResponseEntity<Object> getProjectIdByOrgId(@PathVariable(name = "orgId") Long orgId, @RequestHeader(name = "screenName") String screenName,
                                                      @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                      HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getProjectIdByOrgId" + '"' + " method ...");
        List<ProjectIdProjectName> projectIdProjectNameList = null;
        try {
            projectIdProjectNameList = projectRepository.findByOrgId(orgId);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getProjectIdByOrgId" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get project list for the username = " + foundUser.getPrimaryEmail() +
                    " for the organization = " + orgId + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        try {
            return projectService.getProjectIdByOrgIdFormattedResponse(projectIdProjectNameList);
        } catch(Exception e) {
            if(e instanceof NoDataFoundException) {
                throw e;
            } else {
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get project list for the username = " + foundUser.getPrimaryEmail() +
                        " for the organization = " + orgId + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
                ThreadContext.clearMap();
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
            }
        }
    }

    @GetMapping(path = "/getProject/{projectId}")
    public ResponseEntity<Object> getProjectByProjectId(@PathVariable(name = "projectId") Long projectId, @RequestHeader(name = "screenName") String screenName,
                                                        @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                        HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getProjectByProjectId" + '"' + " method ...");
        Project project = null;
        try {
            project = projectRepository.findByProjectId(projectId);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getProjectByProjectId" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get the project for the username = " + foundUser.getPrimaryEmail() +
                    " ,     " + "ProjectId = " + projectId + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        try {
            return projectService.getProjectByProjectIdFormattedResponse(project);
        } catch(Exception e) {
            if(e instanceof ProjectNotFoundException) {
                throw e;
            } else {
                e.printStackTrace();
                String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get the project for the username = " + foundUser.getPrimaryEmail() +
                        " ,     " + "ProjectId = " + projectId + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
                ThreadContext.clearMap();
                if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
            }
        }
    }

    // expects single accountId of the user creating the project
    @Transactional
    @PostMapping("/createProject")
    public ResponseEntity<Object> createProject(@Valid @RequestBody ProjectRequest projectRequest, @RequestHeader(name = "screenName") String screenName,
                                             @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                             HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " addProject" + '"' + " method ...");
        ProjectResponse projectResponse = new ProjectResponse();
        try {
            projectResponse = projectService.validateAndCreateProject(projectRequest, accountIds, timeZone);
            projectResponse.setLastUpdatedDateTime(null);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " addProject" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to add the project for the username = " + foundUser.getPrimaryEmail() +
                    "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }

        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, projectResponse);
    }

    // expects single accountId of the user requesting the projects list
    @GetMapping(path = "/getProjectsListByBu/{buId}")
    public ResponseEntity<Object> getProjectsListByBu(@PathVariable(name = "buId") Long buId, @RequestHeader(name = "screenName") String screenName,
                                                   @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                   HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getProjectsListByBu" + '"' + " method ...");
        List<ProjectResponse> response = null;
        try {
            response = projectService.getProjectsListByBu(buId, accountIds, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getProjectsListByBu" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get project list for the username = " + foundUser.getPrimaryEmail() +
                    " for the bu = " + buId + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }

        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
    }

    // expects single accountId of the user requesting the update
    @Transactional
    @PostMapping("/updateProject")
    public ResponseEntity<Object> updateProject(@Valid @RequestBody UpdateProjectRequest updateProjectRequest, @RequestHeader(name = "screenName") String screenName,
                                                @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " updateProject" + '"' + " method ...");
        ProjectResponse projectResponse = new ProjectResponse();
        try {
            projectResponse = projectService.validateAndUpdateProject(updateProjectRequest, accountIds, timeZone, foundUser);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " updateProject" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to update the project for the username = " + foundUser.getPrimaryEmail() +
                    "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }

        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, projectResponse);
    }

    @PostMapping("/getProjectAllMember/{projectId}")
    public ResponseEntity<Object> getProjectMember(@PathVariable(name = "projectId") Long projectId, @RequestHeader(name = "screenName") String screenName,
                                                @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getProjectMember" + '"' + " method ...");
        List<EmailFirstLastAccountId> response;
        try {
            response = projectService.getAllMembersInProject(projectId, accountIds);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getProjectMember" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get project members for the username = " + foundUser.getPrimaryEmail() +
                    "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }

        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
    }

    @GetMapping(path = "/deleteProject/{projectId}")
    @Transactional
    public ResponseEntity<Object> deleteProject(@PathVariable Long projectId,
                                                @RequestHeader(name = "screenName") String screenName,
                                                @RequestHeader(name = "timeZone") String timeZone,
                                                @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String username = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUserDbByUsername = userService.getUserByUserName(username);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUserDbByUsername.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " deleteProject" + '"' + " method ...");
        DeleteProjectResponse response;
        try {
            response = projectService.deleteProject(projectId, accountIds, timeZone, false, foundUserDbByUsername);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " deleteProject" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to delete the project requested by user ," + username+  "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
    }

    @GetMapping(path = "/getAllDeletedProjectReport/{buId}")
    @Transactional
    public ResponseEntity<Object> getAllDeletedTeamReport(@PathVariable Long buId,
                                                          @RequestHeader(name = "screenName") String screenName,
                                                          @RequestHeader(name = "timeZone") String timeZone,
                                                          @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String username = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUserDbByUsername = userService.getUserByUserName(username);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUserDbByUsername.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getAllDeletedProjectReport" + '"' + " method ...");
        List<DeletedProjectReport> response;
        try {
            response = projectService.getAllDeletedProjectReport(buId, accountIds, timeZone, false);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getAllDeletedProjectReport" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get the report for requested bu by user ," + username+  "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
    }

    @GetMapping(path = "/getAllProjectOfUserWithAdminRole")
    public ResponseEntity<Object> getAllProjectOfUser (@RequestParam(name = "email") String email, @RequestHeader(name = "screenName") String screenName,
                                              @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                              HttpServletRequest request) throws IllegalAccessException, InvalidKeyException {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getAllProjectOfUserWithAdminRole" + '"' + " method ...");

        try {
            List<UserAllProjectResponseWithOrg> userAllProjectResponseWithOrgList = projectService.getAllProjectsOfUser(email, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getAllProjectOfUserWithAdminRole" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, userAllProjectResponseWithOrgList);
        }
        catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get all project for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @GetMapping(path = "/getAdminRoleAccessDomain")
    public ResponseEntity<Object> getAllAdminAccessDomain (@RequestParam(name = "entityTypeId") Integer entityTypeId, @RequestParam(name = "entityId") Long entityId , @RequestHeader(name = "screenName") String screenName,
                                                       @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                       HttpServletRequest request) throws IllegalAccessException, InvalidKeyException {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getAdminRoleAccessDomain" + '"' + " method ...");

        try {
            List<CustomAccessDomain> adminRoleList = projectService.getAllAdminRoleForEntity (entityTypeId, entityId);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getAdminRoleAccessDomain" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, adminRoleList);
        }
        catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get all admin role for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }

    @GetMapping(path = "/getPMRoleOfProject")
    public ResponseEntity<Object> getAllPMRoleOfProject (@RequestParam(name = "projectId") Long projectId , @RequestHeader(name = "screenName") String screenName,
                                                           @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                           HttpServletRequest request) throws IllegalAccessException, InvalidKeyException {

        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getAllPMRoleOfProject" + '"' + " method ...");

        try {
            List<CustomAccessDomain> adminRoleList = projectService.getAllPMRoleOfProject (projectId);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getAllPMRoleOfProject" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, adminRoleList);
        }
        catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get all PM role of project for username = " + foundUser.getPrimaryEmail() + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage() == null) throw new InternalServerErrorException("Internal Server Error!");
            else throw e;
        }
    }
}
