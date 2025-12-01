package com.tse.core_application.controller;

import com.tse.core_application.constants.Constants;
import com.tse.core_application.custom.model.*;
import com.tse.core_application.dto.*;
import com.tse.core_application.exception.OpenfireException;
import com.tse.core_application.exception.InternalServerErrorException;
import com.tse.core_application.exception.NoDataFoundException;
import com.tse.core_application.exception.TeamNotFoundException;
import com.tse.core_application.handlers.CustomResponseHandler;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.*;
import com.tse.core_application.model.User;
import com.tse.core_application.repository.*;
import com.tse.core_application.service.Impl.*;
import com.tse.core_application.utils.JWTUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin(value = "*")
@RestController
@RequestMapping(path = "/team")
public class TeamController {

    private static final Logger logger = LogManager.getLogger(TeamController.class.getName());

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private AccessDomainService accessDomainService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private AccessDomainRepository accessDomainRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TeamService teamService;

    @Autowired
    private RequestHeaderHandler requestHeaderHandler;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditService auditService;
    @Autowired
    private UserService userService;

    @Autowired
    private OpenFireService openFireService;

    @Autowired
    private SecondaryDatabaseService secondaryDatabaseService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private InviteService inviteService;

    @Value("${enable.openfire}")
    private Boolean enableOpenfire;

    @PostMapping(path = "/createTeam")
    public ResponseEntity<Object> addTeam(@RequestBody @Valid CreateTeamRequest teamRequest, @RequestHeader(name = "screenName") String screenName,
                                          @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                          HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " addTeam" + '"' + " method ...");

        try {
            Team team = teamService.fillTeamDetailsAndCreateTeamObject(teamRequest);
            Team teamAdded = teamService.createAndAuditTeam(team, foundUser.getUserId(), teamRequest.getTeamAdmin(), Long.valueOf(accountIds));
            Team responseTeam = new Team();
            BeanUtils.copyProperties(teamAdded, responseTeam);
           // teamService.convertTeamServerDateTimeToLocalDateTime(responseTeam, timeZone);
//            secondaryDatabaseService.insertData(teamAdded.getTeamId(), "test","test","test","test","test","test");

            if (enableOpenfire) {
                try {
                    openFireService.createChatRoomForTeam(responseTeam);
                } catch (OpenfireException e) {
                    e.printStackTrace();
                    String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                    logger.error("Something went wrong in call to openfire create-team method" + e, new Throwable(allStackTraces));
                    ThreadContext.clearMap();
                }
            }
            TeamResponseDto response = teamService.createTeamResponse(responseTeam, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " addTeam" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to create Team for username = " + foundUser.getPrimaryEmail() +
                    " ,   " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
    }

    // endpoint to get the teams where the user is Team Admin or BackUp Team Admin
    @GetMapping(path = "/getTeamsForAdmins/{orgIds}")
    public ResponseEntity<Object> getTeam(@PathVariable(name = "orgIds") List<Long> orgIds,
                                          @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                          @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getTeam" + '"' + " method ...");
        try {
            List<Long> accountIdList = userService.getAllAccountIds(foundUser);
            List<Long> listEntityIdsDb = accessDomainService.getActiveEntityIdsFromAccessDomain(accountIdList);
            List<Team> teamsDb = teamRepository.findByTeamIdInAndFkOrgIdOrgIdIn(listEntityIdsDb, orgIds);
            List<TeamResponseDto> responseTeamList = teamsDb == null ? Collections.emptyList() :
                    teamsDb.stream()
                            .sorted(Comparator
                                    .comparing(Team::getTeamName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                                    .thenComparing(Team::getTeamCode, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                            .map(team -> teamService.createTeamResponse(team, timeZone))
                            .collect(Collectors.toList());
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getTeam" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, responseTeamList);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get the teams for username = " + foundUser.getPrimaryEmail() +
                    " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
    }

    //  endpoint to update the team
    @Transactional
    @PostMapping(path = "/updateTeam")
    public ResponseEntity<Object> updateTeam(@RequestBody @Valid Team team, @RequestHeader(name = "screenName") String screenName,
                                             @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                             HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " updateTeam" + '"' + " method ...");

        try {
            Team updatedTeam = teamService.updateTeamInTeamTable(team, Long.valueOf(accountIds), foundUser);
            if (enableOpenfire) {
                try {
                    openFireService.updateChatRoomForTeam(updatedTeam);
                } catch (OpenfireException e) {
                    e.printStackTrace();
                    String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                    logger.error("Something went wrong in call to openfire update-team method" + e, new Throwable(allStackTraces));
                    ThreadContext.clearMap();
                }
            }
            TeamResponseDto response = teamService.createTeamResponse(updatedTeam, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " updateTeam" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to update the team for username = " + foundUser.getPrimaryEmail() +
                    " ,   " + "teamId = " + team.getTeamId() + " ,   " + "orgId = " + team.getFkOrgId().getOrgId() + " ,    " + "projectId = " +
                    team.getFkProjectId().getProjectId() + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
    }

    //  endpoint to get team by teamId
    @GetMapping(path = "/getTeamById/{teamId}")
    public ResponseEntity<Object> getTeamByTeamId(@PathVariable(name = "teamId") Long teamId, @RequestHeader(name = "screenName") String screenName,
                                                  @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                  HttpServletRequest request) throws IllegalAccessException {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getTeamByTeamId" + '"' + " method ...");
        Team teamDb = null;
        Team team = new Team();
        try {
            teamDb = teamRepository.findByTeamId(teamId);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get the team for username = " + foundUser.getPrimaryEmail() +
                    " ,    " + "teamId = " + teamId + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        if (teamDb != null) {
            BeanUtils.copyProperties(teamDb, team);
           // teamService.convertTeamServerDateTimeToLocalDateTime(team, timeZone);
            TeamResponseDto response=teamService.createTeamResponse(team,timeZone);
            Organization teamOrg = organizationRepository.findByOrgId(team.getFkOrgId().getOrgId());
            if (team.getTeamName().equalsIgnoreCase(com.tse.core_application.model.Constants.PERSONAL_ORG_DEFAULT_TEAM_NAME) && teamOrg.getOrganizationName().equalsIgnoreCase(com.tse.core_application.model.Constants.PERSONAL_ORG)) {
                throw new IllegalAccessException("User not authorized to access the team");
            }
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getTeamByTeamId" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
        } else {
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getTeamByTeamId" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            String allStackTraces = StackTraceHandler.getAllStackTraces(new TeamNotFoundException());
            logger.error(request.getRequestURI() + " API: " + "Team not found for username = " + foundUser.getPrimaryEmail() + " ,    " + "teamId = " +
                    teamId, new Throwable(allStackTraces));
            throw new TeamNotFoundException();
        }
    }

    //  endpoint to get parentTeams
    @GetMapping(path = "/getParentTeams/{orgId}/{projectId}")
    public ResponseEntity<Object> getAllParentTeams(@PathVariable(name = "orgId") Long orgId, @PathVariable(name = "projectId") Long projectId,
                                                    @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                                    @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getAllParentTeams" + '"' + " method ...");
        List<TeamIdAndTeamName> teamIdAndTeamNames;
        try {
            teamIdAndTeamNames = teamRepository.findTeamIdTeamNameByFkOrgIdOrgIdAndFkProjectIdProjectId(orgId, projectId);
            teamService.removePersonalTeamFromTeamNameAndTeamIdResponse(teamIdAndTeamNames, orgId);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get the parent team for username = " + foundUser.getPrimaryEmail() +
                    " with orgId = " + orgId + " and projectId = " + projectId + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        if (teamIdAndTeamNames.isEmpty()) {
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getAllParentTeams" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            String allStackTraces = StackTraceHandler.getAllStackTraces(new NoDataFoundException());
            logger.error(request.getRequestURI() + " API: " + "No parent teams found for username = " + foundUser.getPrimaryEmail() + " with orgId = " + orgId +
                    " and projectId = " + projectId, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            throw new NoDataFoundException();
        } else {
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getAllParentTeams" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, teamIdAndTeamNames);
        }
    }

    @GetMapping(path = "/getTeamListForCreateTask/{email}/{orgId}")
    public ResponseEntity<Object> getTeamListForCreateTask(@PathVariable(name = "email") String email, @PathVariable(name = "orgId") Long orgId,
                                                           @RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                                           @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getTeamListForCreateTask" + '"' + " method ...");
        try {
            List<Team> teams = teamService.getAllTeamsForCreateTask(email, orgId);
            teamService.filterTeamsForPersonalOrg(teams);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getTeamListForCreateTask" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, teams);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get the list of team to create a task for username = " + email +
                    " in orgId = " + orgId + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
    }

    /* to get only all those teams of a user where user is part of the team.*/
    @GetMapping(path = "/getAllTeamsByUserId/{userId}")
    public ResponseEntity<Object> getAllTeamsByUserId(@PathVariable(name = "userId") Long userId, @RequestHeader(name = "screenName") String screenName,
                                                      @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                      HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getAllTeamsByUserId" + '"' + " method ...");
        try {
            List<TeamOrgBuAndProjectName> teamOrgAndProjectNameList = teamService.getAllMyTeamsForUserId(userId);
            teamService.filterTeamOrgAndProjectNameListResponse(teamOrgAndProjectNameList);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getAllTeamsByUserId" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, teamOrgAndProjectNameList);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get all the teams for username = " + foundUser.getPrimaryEmail() +
                    " ,    " + "userId = " + userId + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
    }


    // endpoint to get the all teams using orgId
    @GetMapping(path = "/getAllTeamsByOrgId/{orgId}")
    public ResponseEntity<Object> getAllTeamsByOrgId(@PathVariable(name = "orgId") Long orgId, @RequestHeader(name = "screenName") String screenName,
                                                      @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                      HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        Organization foundOrg = organizationRepository.findByOrgId(orgId);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getAllTeamsByOrgId" + '"' + " method ...");
        try {
            List<TeamIdAndTeamName> teamList = teamService.getAllTeamNameAndTeamIdByOrgId(orgId);
            teamService.filterTeamNameAndTeamIdResponse(foundUser, teamList, orgId);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getAllTeamsByOrgId" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, teamList);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get all the teams for Organization = " + foundOrg.getOrganizationName() +
                    " ,    " + "OrgId = " + orgId + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
    }

    // expects single accountId
    @PostMapping(path = "/inviteUserToTeam")
    public ResponseEntity<Object> createInvite(@Valid @RequestBody InviteRequest inviteRequest, @RequestHeader(name = "screenName") String screenName,
                                               @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                               HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String username = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUserDbByUsername = userService.getUserByUserName(username);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUserDbByUsername.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " inviteUserToTeam" + '"' + " method ...");
        Invite response;
        try {
            response = inviteService.createInviteForTeam(inviteRequest, Long.parseLong(accountIds), timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " inviteUserToTeam" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to send invite to user requested by user ," + username+  "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
    }

    // expects single accountId
    @PostMapping(path = "/addToTeamByInvite")
    public ResponseEntity<Object> addToTeamByInvite(@RequestParam(name = "inviteId") String inviteId, @RequestHeader(name = "screenName") String screenName,
                                               @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                               HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String username = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUserDbByUsername = userService.getUserByUserName(username);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUserDbByUsername.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " addToTeamByInvite" + '"' + " method ...");
        Integer response;
        try {
            response = teamService.addMemberToTeamByInvite(inviteId, foundUserDbByUsername, timeZone);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " addToTeamByInvite" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to send invite to user requested by user ," + username+  "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        switch (response) {
            case 1:
                return CustomResponseHandler.generateCustomResponse(HttpStatus.UNAUTHORIZED, Constants.FormattedResponse.FORBIDDEN, "User needs to register in personal organization to be part of the team");
            case 2:
                return CustomResponseHandler.generateCustomResponse(HttpStatus.NOT_FOUND, Constants.FormattedResponse.NOTFOUND, "Team not found");
            default:
                return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, "User successfully added to the team");
        }

    }

    @GetMapping(path = "/teamCodeExists/{teamCode}/{orgId}")
    public ResponseEntity<Object> initialsExists(@PathVariable(name = "teamCode") String teamCode,
                                                 @PathVariable(name = "orgId") Long orgId,
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
        logger.info("Entered" + '"' + " teamCodeExists" + '"' + " method ...");
        Boolean response;
        try {
            response = teamService.doTeamCodeExistsInOrg(teamCode, orgId);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " teamCodeExists" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to check existing teamCodeExists - requested by user ," + username+  "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
    }

    @PostMapping(path = "/generateTeamCode")
    public ResponseEntity<Object> generateTeamCode(@Valid @RequestBody GenerateTeamCodeRequest generateTeamCodeRequest,
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
        logger.info("Entered" + '"' + " generateTeamCode" + '"' + " method ...");
        String response;
        try {
            response = teamService.generateTeamCode(generateTeamCodeRequest);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " generateTeamCode" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to generate the team code - requested by user ," + username+  "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
    }

    @GetMapping(path = "/deleteTeam/{teamId}")
    @Transactional
    public ResponseEntity<Object> deleteTeam(@PathVariable Long teamId,
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
        logger.info("Entered" + '"' + " deleteTeam" + '"' + " method ...");
        DeleteTeamResponse response;
        try {
            response = teamService.deleteTeam(teamId, accountIds, timeZone, false, foundUserDbByUsername);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " deleteTeam" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to delete the team requested by user ," + username+  "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
    }

    @GetMapping(path = "/getAllTeamsByProjectId/{projectId}")
    public ResponseEntity<Object> getAllTeamsByProjectId(@PathVariable(name = "projectId") Long projectId, @RequestHeader(name = "screenName") String screenName,
                                                     @RequestHeader(name = "timeZone") String timeZone, @RequestHeader(name = "accountIds") String accountIds,
                                                     HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String jwtToken = request.getHeader("Authorization").substring(7);
        String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
        User foundUser = userService.getUserByUserName(tokenUsername);
        ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
        ThreadContext.put("userId", foundUser.getUserId().toString());
        ThreadContext.put("requestOriginatingPage", screenName);
        logger.info("Entered" + '"' + " getAllTeamsByProjectId" + '"' + " method ...");
        try {
            List<TeamIdAndTeamName> teamList = teamService.getAllTeamNameAndTeamIdByProjectId(projectId, accountIds);
            teamService.filterTeamNameAndTeamIdResponse(foundUser, teamList, projectId);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getAllTeamsByProjectId" + '"' + " method because completed successfully ...");
            ThreadContext.clearMap();
            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, teamList);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get all the teams for ProjectId = " + projectId + " ,     " + "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
    }

    @GetMapping(path = "/getAllDeletedTeamReport/{projectId}")
    @Transactional
    public ResponseEntity<Object> getAllDeletedTeamReport(@PathVariable Long projectId,
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
        logger.info("Entered" + '"' + " getAllDeletedTeamReport" + '"' + " method ...");
        List<DeletedTeamReport> response;
        try {
            response = teamService.getAllDeletedTeamReport(projectId, accountIds, timeZone, false);
            long estimatedTime = System.currentTimeMillis() - startTime;
            ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
            logger.info("Exited" + '"' + " getAllDeletedTeamReport" + '"' + " method because it completed successfully ...");
            ThreadContext.clearMap();
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to get the report for requested project by user ," + username+  "Caught Exception: " + e, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
        }
        return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, response);
    }
}
