package com.tse.core_application.filters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tse.core_application.custom.model.RestResponseWithoutData;
import com.tse.core_application.exception.*;
import com.tse.core_application.model.HttpCustomStatus;
import com.tse.core_application.model.User;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.Constants;
import com.tse.core_application.model.UserAccount;
import com.tse.core_application.repository.UserAccountRepository;
import com.tse.core_application.repository.UserRepository;
import com.tse.core_application.service.Impl.UserAccountService;
import com.tse.core_application.service.Impl.UserService;
import com.tse.core_application.utils.JWTUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.annotation.PostConstruct;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.tse.core_application.constants.Constants.TOKEN_HASH;


@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final Logger logger = LogManager.getLogger(JwtRequestFilter.class.getName());

    @Autowired
    JWTUtil jwtUtil;

    @Autowired
    UserService userService;

    @Autowired
    private RequestHeaderHandler requestHeaderHandler;

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private ObjectMapper objectMapper;

    public static Map<String, Set<String>> blockedTokens = new HashMap<>();

    public static Set<String> deactivatedUsers = new HashSet<>();

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @PostConstruct
    public void initializeBlockedTokens(){
        Map<Object, Object> values = redisTemplate.opsForHash().entries(TOKEN_HASH);
        values.forEach((k,v) ->{
            try {
                blockedTokens.put((String) k,
                        objectMapper.convertValue(objectMapper.readTree(v.toString()), Set.class)
                        );
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        });
    }

    public List<Long> getAccountIdsFromHeader(String accountIdStr) {
        String[] accountIds = accountIdStr.split(",");
        List<Long> accountIdsResp = new ArrayList<>();

        if (accountIds != null && accountIds.length > 0) {
            for (String accountId : accountIds) {
                accountIdsResp.add(Long.parseLong(accountId));
            }
        }

        return accountIdsResp;
    }

    private boolean validateAccountIds(User user, List<Long> accountIds) {
        if (user != null && accountIds != null && accountIds.size() > 0) {
            boolean accountIdValidated = true;
            List<Long> accountIdsForUser = new ArrayList<>(userAccountService.getActiveAccountIdsForUserId(user.getUserId(), null));
            if (user.getIsUserManaging() != null && user.getIsUserManaging()) {
                List<Long> userIdList = userRepository.findAllUserIdByManagingUserId(user.getUserId());
                accountIdsForUser.addAll(userAccountRepository.findAllAccountIdsByUserIdInAndIsActive(userIdList, true));
            }
            for (Long accountId : accountIds) {
                if (!accountIdsForUser.contains(accountId)) {
                    accountIdValidated = false;
                    break;
                }
            }
            return accountIdValidated;
        }
        return false;
    }

    private boolean isRequestedApiAllowedByHeaderAccountIds(HttpServletRequest request, List<Long> headerAccountIds) {
        boolean isRequestAllowed = true;
        List<String> pathsToValidateForHeaderAccountIds = Arrays.asList(Constants.validatePrivatePathsForSingleHeaderAccountId);
        String pathUrl = request.getRequestURI();
        boolean isPathPresent = pathsToValidateForHeaderAccountIds.stream().anyMatch(path -> {
            Pattern pattern = Pattern.compile(path);
            Matcher matcher = pattern.matcher(pathUrl);
            return matcher.matches();
        });
        if (isPathPresent) {
            if (headerAccountIds.size() == 1) {
                isRequestAllowed = true;
            } else {
                isRequestAllowed = false;
            }
        }
        return isRequestAllowed;
    }

    private static String getCurrentUTCTimeStamp() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm:ss:SSS Z");
        String UtcDateTime = now.format(formatter);
        return UtcDateTime;
    }

    private void handleInvalidApiRequestException(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpStatus.NOT_FOUND.value());
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        restResponseWithoutData.setStatus(HttpStatus.NOT_FOUND.value());
        restResponseWithoutData.setMessage("Invalid API Endpoint");
        String timestampUtc = getCurrentUTCTimeStamp();
        restResponseWithoutData.setTimestamp(timestampUtc);
        byte[] bodyToWrite = new ObjectMapper().writeValueAsBytes(restResponseWithoutData);
        response.getOutputStream().write(bodyToWrite);
    }

    private boolean validateRequestForInvalidEndPoint(HttpServletRequest request) throws ServletException {
        List<String> pathsToValidateForInvalidEndPoint = Arrays.asList(Constants.privatePaths);
        String pathUrl = request.getRequestURI();
        return pathsToValidateForInvalidEndPoint.stream().anyMatch(path -> {
            Pattern pattern = Pattern.compile(path);
            Matcher matcher = pattern.matcher(pathUrl);
            return matcher.matches();
        });
    }

    private boolean validateAllHeaders(String screenName, String timeZone, String accountIds, HttpServletRequest request) {
        boolean isAllHeadersValidated = true;
        boolean isHeaderScreenNameValidated = true;
        boolean isHeaderAccountIdsAllowed = true;
        boolean isHeaderTimeZoneValidated = false;

        Pattern patternScreenName = Pattern.compile("-?\\d+(\\.\\d+)?");
        if (screenName == null || screenName.isEmpty() || screenName.contains(" ") || patternScreenName.matcher(screenName).matches() ||
                !Pattern.matches("[a-zA-Z]+", screenName)) {
            isHeaderScreenNameValidated = false;
        }
        if (accountIds != null && !accountIds.isEmpty()) {
            List<Long> accountIdsLong = getAccountIdsFromHeader(accountIds);
            isHeaderAccountIdsAllowed = this.isRequestedApiAllowedByHeaderAccountIds(request, accountIdsLong);
        } else {
            isHeaderAccountIdsAllowed = false;
        }
        String[] validIDs = TimeZone.getAvailableIDs();
        for (String validId : validIDs) {
            if (timeZone != null && !(timeZone.isEmpty()) && !(timeZone.contains(" ")) && validId.equals(timeZone)) {
                isHeaderTimeZoneValidated = true;
                break;
            }
        }
        if (isHeaderScreenNameValidated && isHeaderAccountIdsAllowed && isHeaderTimeZoneValidated) {
            isAllHeadersValidated = true;
        } else {
            isAllHeadersValidated = false;
        }
        return isAllHeadersValidated;
    }

    private void handleInvalidRequestHeaderException(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpStatus.NOT_ACCEPTABLE.value());
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        restResponseWithoutData.setStatus(HttpStatus.NOT_ACCEPTABLE.value());
        restResponseWithoutData.setMessage("Incorrect Request Header");
        String timestampUtc = getCurrentUTCTimeStamp();
        restResponseWithoutData.setTimestamp(timestampUtc);
        byte[] bodyToWrite = new ObjectMapper().writeValueAsBytes(restResponseWithoutData);
        response.getOutputStream().write(bodyToWrite);
    }

    private void handleExpiredTokenException(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String token = request.getHeader("Authorization").substring(7);
        Date tokenExpiredDate = jwtUtil.getExpirationDateFromToken(token);
        LocalDateTime tokenExpiredDateUTC = LocalDateTime.ofInstant(tokenExpiredDate.toInstant(), ZoneId.of("UTC"));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String timestampUtc = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpStatus.UNAUTHORIZED.value());
        restResponseWithoutData.setMessage("Session has expired on " + tokenExpiredDateUTC + "." + " The current time is " + timestampUtc);
        restResponseWithoutData.setTimestamp(timestampUtc);
        byte[] bodyToWrite = new ObjectMapper().writeValueAsBytes(restResponseWithoutData);
        response.getOutputStream().write(bodyToWrite);
        errorLogExpiredAccessToken(token, tokenExpiredDateUTC, timestampUtc, request);
    }

    private void handleInvalidTokenException(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String token = request.getHeader("Authorization").substring(7);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpCustomStatus.INVALID_TOKEN.value());
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        String timestampUtc = getCurrentUTCTimeStamp();
        restResponseWithoutData.setStatus(HttpCustomStatus.INVALID_TOKEN.value());
        restResponseWithoutData.setMessage("Token is INVALID !  The current time is " + timestampUtc);
        restResponseWithoutData.setTimestamp(timestampUtc);
        byte[] bodyToWrite = new ObjectMapper().writeValueAsBytes(restResponseWithoutData);
        response.getOutputStream().write(bodyToWrite);
        errorLogInvalidToken(token,getCurrentUTCTimeStamp(),request);
    }

    private void errorLogExpiredAccessToken(String token, LocalDateTime expiredTokenDateUTC, String currentTimeUTC, HttpServletRequest request) {
        String username = jwtUtil.getUsernameFromToken(token);
        User user = userService.getUserByUserName(username);
        if(user != null) {
            List<UserAccount> userAccounts = userAccountService.getAllUserAccountByUserIdAndIsActive(user.getUserId());
            if(!userAccounts.isEmpty()) {
                ThreadContext.put("userId", user.getUserId().toString());
                if(userAccounts.size() > 1) {
                    ThreadContext.put("accountId", String.valueOf(0));
                } else {
                    ThreadContext.put("accountId", userAccounts.get(0).getAccountId().toString());
                }
            }
        }
        TokenValidationFailedException tokenValidationFailedException = new TokenValidationFailedException(expiredTokenDateUTC, currentTimeUTC);
        String allStackTraces = StackTraceHandler.getAllStackTraces(tokenValidationFailedException);
        ThreadContext.clearMap();
        logger.error(request.getRequestURI() + " API: " + "Expired Access Token: The token is expired for username = " + username + ".", new Throwable(allStackTraces));
    }

    private void errorLogInvalidToken(String token, String currentTimeUTC, HttpServletRequest request) {
        String username = jwtUtil.getUsernameFromToken(token);
        User user = userService.getUserByUserName(username);
        if(user != null) {
            List<UserAccount> userAccounts = userAccountService.getAllUserAccountByUserIdAndIsActive(user.getUserId());
            if(!userAccounts.isEmpty()) {
                ThreadContext.put("userId", user.getUserId().toString());
                if(userAccounts.size() > 1) {
                    ThreadContext.put("accountId", String.valueOf(0));
                } else {
                    ThreadContext.put("accountId", userAccounts.get(0).getAccountId().toString());
                }
            }
        }
        InvalidTokenException invalidTokenException = new InvalidTokenException(currentTimeUTC);
        String allStackTraces = StackTraceHandler.getAllStackTraces(invalidTokenException);
        ThreadContext.clearMap();
        logger.error(request.getRequestURI() + " API: " + "Invalid token = " + username + ".", new Throwable(allStackTraces));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {


        String authorizarionHeader = request.getHeader("Authorization");

        String email = null;
        String jwt = null;

        if(authorizarionHeader != null && !authorizarionHeader.isEmpty()) {
            jwt = authorizarionHeader.substring(7);
            if (jwtUtil.validateToken(jwt)) {
                email = jwtUtil.getUsernameFromToken(jwt);

                List<Long> accountIdsFromToken = jwtUtil.getAllAccountIdsFromToken(jwt);
                // Check if any of the account IDs are in the Redis set of inactive account IDs
                for(Long accountId : accountIdsFromToken) {
                    Boolean isAccountInactive = redisTemplate.opsForSet().isMember("INACTIVE_ACCOUNTS", accountId.toString());
                    if (isAccountInactive) {
                        handleInvalidTokenException(request, response);
                        return;
                    }
                }

                if(blockedTokens.containsKey(email)){
                    if(blockedTokens.get(email).contains(jwt)){
                        handleExpiredTokenException(request, response);
                        return;
                    }
                }
                Boolean isUserDeactivated = redisTemplate.opsForSet().isMember("DEACTIVATED_USERS", email);
                if (Boolean.TRUE.equals(isUserDeactivated) || !userAccountRepository.existsByFkUserIdPrimaryEmailAndIsActive(email, true)) {
                    handleDeactivatedUserException(request, response);
                    return;
                }
            }
            else{
                handleInvalidTokenException(request,response);
                return;
            }
            ThreadContext.put("userId", userService.getUserByUserName(email).getUserId().toString());
        }
        if(authorizarionHeader != null && !authorizarionHeader.isEmpty()) {
            Boolean isTokenExpired = jwtUtil.isTokenExpired(authorizarionHeader.substring(7));
            if(isTokenExpired) {
                handleExpiredTokenException(request, response);
                return;
            }
        }

        String accountIdsHeader = request.getHeader("accountIds");

        String timeZoneHeader = request.getHeader("timeZone");

        String screenNameHeader = request.getHeader("screenName");

        if(accountIdsHeader != null && !accountIdsHeader.isEmpty()) {
            ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIdsHeader).toString());
        }


        if (!validateRequestForInvalidEndPoint(request)) {
            handleInvalidApiRequestException(request, response);
            String allStackTraces = StackTraceHandler.getAllStackTraces(new InvalidApiEndpointException());
            logger.error("Invalid API Endpoint: The API " + request.getRequestURI() + " not in use.", new Throwable(allStackTraces));
            ThreadContext.clearMap();
            return;
        }

        if (!validateAllHeaders(screenNameHeader, timeZoneHeader, accountIdsHeader, request)) {
            handleInvalidRequestHeaderException(request, response);
            String allStackTraces = StackTraceHandler.getAllStackTraces(new IncorrectRequestHeaderException());
            logger.error(request.getRequestURI() + " API: " + "Incorrect Request Header for username = " + email + "." + "    timeZone = " +
                    timeZoneHeader + " ,   screenName = " + screenNameHeader + " ,    " + "accountIds = " + accountIdsHeader, new Throwable(allStackTraces));
            ThreadContext.clearMap();
            return;
        }

        List<Long> accountIds = null;
        if (accountIdsHeader != null) {
            accountIds = getAccountIdsFromHeader(accountIdsHeader);
        }

/*
        String email = null;
        String jwt = null;

        if (authorizarionHeader != null && authorizarionHeader.startsWith("Bearer ")) {
            jwt = authorizarionHeader.substring(7);
            if (jwtUtil.validateToken(jwt)) {
                email = jwtUtil.getUsernameFromToken(jwt);
            }
        }
*/

        //Here we are checking if the security context doesn;t have an authenticated user saved.

        //  this line is commented to pass through the security check. Refer tracking email before deleting it
//		if(email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

        if (email != null) {
            if (jwtUtil.validateToken(jwt)) {
                User userByEmail = userService.getUserByUserName(email);
                if (accountIds != null && userByEmail != null && validateAccountIds(userByEmail, accountIds)) {
                    UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                            new UsernamePasswordAuthenticationToken(userByEmail, null, null);
                    usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                } else {
                    SecurityContextHolder.getContext().setAuthentication(null);
                    String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("AccountIds Not Validated"));
                    logger.error(request.getRequestURI() + " API: " + "Forbidden: Account Ids in header are not validated.", new Throwable(allStackTraces));
                    ThreadContext.clearMap();
                    response.setStatus(403);
                }

            } else {
                SecurityContextHolder.getContext().setAuthentication(null);
                String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("Token Not Validated"));
                logger.error(request.getRequestURI() + " API: " + "Forbidden: Something went wrong with the token. Token is not validated.", new Throwable(allStackTraces));
                ThreadContext.clearMap();
                response.setStatus(403);
            }

        } else {
            SecurityContextHolder.getContext().setAuthentication(null);
            String allStackTraces = StackTraceHandler.getAllStackTraces(new ValidationFailedException("No Username Found In Token"));
            logger.error(request.getRequestURI() + " API: " + "Forbidden: No username found in the token", new Throwable(allStackTraces));
            ThreadContext.clearMap();
            response.setStatus(403);
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
        }

    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        List<String> excludePaths = Arrays.asList(Constants.publicPaths);
        return excludePaths.stream().anyMatch(p -> {
            Pattern pattern = Pattern.compile(p);
            Matcher matcher = pattern.matcher(request.getServletPath());
            return matcher.matches();
        });
    }

    public void blockToken(String token, String username) {
        Set<String> userBlockedTokens = blockedTokens.getOrDefault(username, new HashSet<>());
        userBlockedTokens.add(token);
        blockedTokens.put(username, userBlockedTokens);

        try {
            redisTemplate.opsForHash().put(TOKEN_HASH, username, objectMapper.writeValueAsString(userBlockedTokens));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    private void handleDeactivatedUserException(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpCustomStatus.INVALID_TOKEN.value());
        RestResponseWithoutData restResponseWithoutData = new RestResponseWithoutData();
        restResponseWithoutData.setStatus(HttpCustomStatus.INVALID_TOKEN.value());
        restResponseWithoutData.setMessage("Account is deactivated. Please contact the System Administrator.");
        String timestampUtc = getCurrentUTCTimeStamp();
        restResponseWithoutData.setTimestamp(timestampUtc);
        byte[] bodyToWrite = new ObjectMapper().writeValueAsBytes(restResponseWithoutData);
        response.getOutputStream().write(bodyToWrite);
    }
}
