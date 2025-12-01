package com.tse.core_application.service.Impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.oauth2.sdk.util.CollectionUtils;
import com.tse.core_application.configuration.DataEncryptionConverter;
import com.tse.core_application.custom.model.*;
import com.tse.core_application.dto.*;
import com.tse.core_application.dto.SearchCriteria;
import com.tse.core_application.exception.*;
import com.tse.core_application.handlers.RequestHeaderHandler;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.*;
import com.tse.core_application.repository.*;
import com.tse.core_application.utils.CommonUtils;
import com.tse.core_application.utils.ComponentUtils;
import com.tse.core_application.utils.DateTimeUtils;
import com.tse.core_application.utils.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.Query;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private static final Logger logger = LogManager.getLogger(CommentService.class.getName());

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private TaskServiceImpl taskServiceImpl;

    @Autowired
    private AccessDomainRepository accessDomainRepository;

    @Autowired
    private TaskAttachmentRepository taskAttachmentRepository;

    ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ActionService actionService;

    Date d;

    @Autowired
    private EntityManager entityManager;

    //  ################## FCM Notification Section #######################################
    @Autowired
    private FCMService fcmService;

    @Autowired
    private FirebaseTokenService firebaseTokenService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AccessDomainService accessDomainService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private TaskService taskService;
    @Autowired
    private RequestHeaderHandler requestHeaderHandler;
    @Autowired
    private TeamRepository teamRepository;

    @Value("${comment.api.key}")
    private String commentApiKey;

    private final DataEncryptionConverter encryptionConverter = new DataEncryptionConverter();

    public void sendFcmNotification(Long userId, Map<String, String> payload) {
        List<FirebaseToken> firebaseTokenListDb = firebaseTokenService.getFirebaseTokenByUserId(userId);

        if (firebaseTokenListDb != null) {
            for (FirebaseToken firebaseToken : firebaseTokenListDb) {
//                String token = firebaseToken.getToken();
                PushNotificationRequest pushNotificationRequest = new PushNotificationRequest();
                pushNotificationRequest.setPayload(payload);
                pushNotificationRequest.setTargetToken(firebaseToken);
                try {
                    String messageSentResponse = fcmService.sendMessageToToken(pushNotificationRequest);
                } catch (Exception e) {
                    throw new IllegalStateException("fcm error");
                }
            }
        }
    }

    /**
     * method to send notification for a new comment added in a task
     */
    public void sendNewCommentNotification(Comment comment, String timeZone, List<Long> taggedAccountIds) {

        Task task = entityManager.getReference(Task.class, comment.getTask().getTaskId());
        String commentString = comment.getComment();
        try {
            List<HashMap<String, String>> payload = notificationService.createNotificationForNewCommentInTask(task, comment, timeZone, taggedAccountIds);
            taskServiceImpl.sendPushNotification(payload);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Not able to send notification for add comment " + e, new Throwable(allStackTraces));
        }
    }

    /**
     * method to send notification for a new mention in a comment added in a task
     */
    public void sendMentionInCommentNotification(Comment comment, String timeZone, List<Long> taggedAccountIds) {

        Task task = entityManager.getReference(Task.class, comment.getTask().getTaskId());
        String commentString = comment.getComment();
        try {
            List<HashMap<String, String>> payload = notificationService.createMentionInCommentNotification(task, comment, timeZone, taggedAccountIds,comment.getPostedByAccountId().toString());
            taskServiceImpl.sendPushNotification(payload);
        } catch (Exception e) {
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Not able to send notification for add comment " + e, new Throwable(allStackTraces));
        }
    }


//	######################## FCM Notification Section Ends #########################################################


    // find user task by its latest comments
    public List<CommentTaskIdTaskTitleCommentFrom> getUserAllTaskComments(Map<Long, Task> tasksMap, String localTimeZone) {
        if (tasksMap.isEmpty()) {
            return Collections.emptyList();
        }

        // Extract all comment IDs from tasks
        List<Long> commentIds = tasksMap.values().stream()
                .map(Task::getCommentId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (commentIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Fetch the latest comment per task using MAX(created_date_time)
        String sql = "SELECT c.comment_id, c.task_id, c.comment, c.created_date_time, " +
                "       u.email, usr.first_name, usr.last_name " +
                "FROM tse.comment c " +
                "JOIN tse.user_account u ON c.posted_by_account_id = u.account_id " +
                "JOIN tse.tse_users usr ON u.user_id = usr.user_id " +
                "WHERE (c.comment_id, c.created_date_time) IN " +
                "    (SELECT comment_id, MAX(created_date_time) " +
                "     FROM tse.comment " +
                "     WHERE comment_id IN :commentIds " +
                "     GROUP BY comment_id) " +
                "ORDER BY c.created_date_time DESC";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("commentIds", commentIds);
        List<Object[]> results = query.getResultList();

        List<CommentTaskIdTaskTitleCommentFrom> commentList = new ArrayList<>();

        for (Object[] row : results) {
            long commentId = ((Number) row[0]).longValue();
            long taskId = ((Number) row[1]).longValue();
            String encryptedComment = (String) row[2];
            Date createdDate = (Date) row[3];
            String encryptedEmail = (String) row[4];
            String encryptedFirstName = (String) row[5];
            String encryptedLastName = (String) row[6];

            Task task = tasksMap.get(taskId);
            if (task == null) continue; // Skip if task doesn't exist

            // Decrypt fields directly
            String comment = (String) encryptionConverter.convertToEntityAttribute(encryptedComment);
            String email = (String) encryptionConverter.convertToEntityAttribute(encryptedEmail);
            String firstName = (String) encryptionConverter.convertToEntityAttribute(encryptedFirstName);
            String lastName = (String) encryptionConverter.convertToEntityAttribute(encryptedLastName);

            if (comment == null || comment.isEmpty()) {
                comment = "[Attachment]";
            }

            CommentTaskIdTaskTitleCommentFrom commentDto = new CommentTaskIdTaskTitleCommentFrom();
            commentDto.setComment(comment);
            LocalDateTime ldt = LocalDateTime.ofInstant(createdDate.toInstant(), ZoneId.systemDefault());
            commentDto.setCreatedDateTime(DateTimeUtils.convertServerDateToUserTimezone(ldt, localTimeZone));
            commentDto.setLastCommentFromEmail(email);
            commentDto.setLastCommentFromFullName(firstName + " " + lastName);
            commentDto.setTaskTitle(task.getTaskTitle());
            commentDto.setTaskId(task.getTaskId());
            commentDto.setTeamCode(task.getFkTeamId().getTeamCode());
            commentDto.setTeamId(task.getFkTeamId().getTeamId());
            commentDto.setTaskNumber(task.getTaskNumber());
            commentDto.setOrgId(task.getFkOrgId().getOrgId());

            commentList.add(commentDto);
        }

        return commentList;
    }


    // find user all task with no comments
    public List<CommentTaskIdTaskTitleCommentFrom> getUserAllTaskNoComments(HashMap<Long, Task> tasksMap) {

        ArrayList<Long> listTaskId = new ArrayList<>(tasksMap.keySet());
        ArrayList<CommentTaskIdTaskTitleCommentFrom> commentTaskIdTaskTitleCommentFromArrayList = new ArrayList<>();

        Collections.sort(listTaskId, Collections.reverseOrder());

        for (Long id : listTaskId) {
            long taskId = id.longValue();
            Task task = tasksMap.get(id);
            CommentTaskIdTaskTitleCommentFrom commentTaskIdTaskTitleCommentFrom = new CommentTaskIdTaskTitleCommentFrom();
            commentTaskIdTaskTitleCommentFrom.setTaskTitle(task.getTaskTitle());
            commentTaskIdTaskTitleCommentFrom.setTaskId(task.getTaskId());
            commentTaskIdTaskTitleCommentFrom.setTaskNumber(task.getTaskNumber());
            commentTaskIdTaskTitleCommentFrom.setOrgId(task.getFkOrgId().getOrgId());
            commentTaskIdTaskTitleCommentFrom.setTeamId(task.getFkTeamId().getTeamId());
            commentTaskIdTaskTitleCommentFrom.setTeamCode(task.getFkTeamId().getTeamCode());
            commentTaskIdTaskTitleCommentFromArrayList.add(commentTaskIdTaskTitleCommentFrom);
        }

        return commentTaskIdTaskTitleCommentFromArrayList;

    }

    // to generate the new commentId
    public Long getMaxCommentId() {
        CommentId commentIdDb = commentRepository.getMaxCommentId();
        Long commentId = commentIdDb.getCommentId();
        if (commentId == null) {
            int intCommentId = 1;
            long maxCommentId = (long) intCommentId;
            return maxCommentId;
        } else {
            Long maxCommentId = commentId + 1;
            return maxCommentId;
        }
    }

    // to get commentId from Task table by its taskId
    public Long getCommentIdFromTaskTable(Long taskId) {
        CommentId commentIdDb = taskRepository.findCommentIdByTaskId(taskId);
//		Long commentId = commentIdDb.getCommentId();
        if (commentIdDb == null) {
            return null;
        } else {
            return commentIdDb.getCommentId();
        }
    }

    private List<Long> getAccountIdsFromHeader(String accountIdStr) {
        String[] accountIds = accountIdStr.split(",");
        List<Long> accountIdsResp = new ArrayList<>();

        if (accountIds != null && accountIds.length > 0) {
            for (String accountId : accountIds) {
                accountIdsResp.add(Long.parseLong(accountId));
            }
        }

        return accountIdsResp;
    }

    public List<SearchCriteria> getSearchCriteria(Long accountId) {
        List<SearchCriteria> searchCriteriaList = new ArrayList<>();

        searchCriteriaList.add(new SearchCriteria("fkAccountIdAssigned", ":", accountId, true, false));
        searchCriteriaList.add(new SearchCriteria("fkAccountIdAssignee", ":", accountId, true, false));
        searchCriteriaList.add(new SearchCriteria("fkAccountIdCreator", ":", accountId, true, false));
        searchCriteriaList.add(new SearchCriteria("fkAccountIdMentor1", ":", accountId, true, false));
        searchCriteriaList.add(new SearchCriteria("fkAccountIdMentor2", ":", accountId, true, false));
        searchCriteriaList.add(new SearchCriteria("fkAccountIdObserver1", ":", accountId, true, false));
        searchCriteriaList.add(new SearchCriteria("fkAccountIdObserver2", ":", accountId, true, false));

        return searchCriteriaList;
    }

    public List<Task> getAllFilteredTaskForComment(String accountIdStr, int activeDays) {
        List<Long> accountIds = this.getAccountIdsFromHeader(accountIdStr);
        if (accountIds.isEmpty()) {
            return List.of();
        }

        LocalDateTime activeDateTime = LocalDateTime.now().minus(activeDays, ChronoUnit.DAYS);

        String sql = "SELECT DISTINCT t.* FROM tse.task t " +
                "JOIN tse.team tm ON t.team_id = tm.team_id " +
                "WHERE (t.account_id_assigned IN :accountIds " +
                "    OR t.account_id_assignee IN :accountIds " +
                "    OR t.account_id_creator IN :accountIds " +
                "    OR t.account_id_mentor_1 IN :accountIds " +
                "    OR t.account_id_mentor_2 IN :accountIds " +
                "    OR t.account_id_observer_1 IN :accountIds " +
                "    OR t.account_id_observer_2 IN :accountIds) " +
                "AND (tm.is_deleted IS NULL OR tm.is_deleted = false)";

        if (activeDays > 0) {
            sql += " AND (t.last_updated_date_time >= :activeDateTime OR t.created_date_time >= :activeDateTime)";
        }

        Query query = entityManager.createNativeQuery(sql, Task.class);
        query.setParameter("accountIds", accountIds);

        if (activeDays > 0) {
            query.setParameter("activeDateTime", activeDateTime);
        }

        return query.getResultList();
    }

    public ArrayList<CommentResponse> getComments(Long taskId, String localTimeZone, String accountIds) {
        Task task = taskRepository.findByTaskId(taskId);
        Set<Long> allAccountIds = new HashSet<>();
        ArrayList<CommentResponse> commentResponseList = new ArrayList<>();

        if (task != null) {
            List<Long> accountIdsLong = requestHeaderHandler.convertToLongList(accountIds);
            boolean isSelfAssigned = false, updateResult = false;
            if(task.getFkAccountIdAssigned() != null && accountIdsLong.contains(task.getFkAccountIdAssigned().getAccountId())) {
                isSelfAssigned = true;
                updateResult = true;
            }

            if(!isSelfAssigned) {
                for(Long accountId : accountIdsLong) {
                    ArrayList<String> userActionList = actionService.getUserActionList(accountId, task.getFkTeamId().getTeamId());
                    if(userActionList.contains(Constants.UpdateTeam.All_Task_Basic_Update) || userActionList.contains(Constants.UpdateTeam.Team_Task_View)) {
                        updateResult = true;
                        break;
                    }
                }
            }

            if (updateResult) {
                CommentId commentIdDb = taskRepository.findCommentIdByTaskId(taskId);
                Long commentId = commentIdDb.getCommentId();
                if (commentId == null) {
                    return commentResponseList;
                }
                try {
//                    Pageable pageable = PageRequest.of(pageNumber - 1, Constants.CommentsWithPage.pageSize);
                    ArrayList<CommentPreview> allCommentsListFromDb = commentRepository
                            .findAllCommentsByCommentIdOrderByCreatedDateTimeDesc(commentId);

                    // Get all commentLogIds from the comments
                    List<Long> commentLogIds = allCommentsListFromDb.stream().map(CommentPreview::getCommentLogId).collect(Collectors.toList());

                    // Get all parent and child comment Log Ids
                    Set<Long> allParentAndChildCommentLogIds = new HashSet<>();
                    for (CommentPreview comment : allCommentsListFromDb) {
                        allParentAndChildCommentLogIds.add(comment.getParentCommentLogId());
                        allParentAndChildCommentLogIds.addAll(comment.getChildCommentLogIds());
                    }

                    // Exclude the commentLogIds that are already in allCommentsListFromDb and fetch the parent and child comments that are not in the current page
                    allParentAndChildCommentLogIds.removeAll(new HashSet<>(commentLogIds));
                    List<CommentPreview> parentAndChildCommentsNotInCurrentPage = commentRepository.findAllCommentsCustomResponseByCommentIds(new ArrayList<>(allParentAndChildCommentLogIds));

                    // map of allComments for quick lookup
                    Map<Long, CommentPreview> allCommentsMap = new HashMap<>();
                    for (CommentPreview comment : allCommentsListFromDb) {
                        allCommentsMap.put(comment.getCommentLogId(), comment);
                        allAccountIds.add(comment.getPostedByAccountId());
                    }
                    for (CommentPreview comment : parentAndChildCommentsNotInCurrentPage) {
                        allCommentsMap.put(comment.getCommentLogId(), comment);
                        allAccountIds.add(comment.getPostedByAccountId());
                    }

                    // create a map of accountId to AccountIdDetails
                    List<UserAccount> userAccountsDb = userAccountRepository.findByAccountIdIn(new ArrayList<>(allAccountIds));
                    HashMap<Long, HashMap<String, String>> accountIdToDetails = new HashMap<>();
                    for (UserAccount userAccount : userAccountsDb) {
                        HashMap<String, String> accountInfo = new HashMap<>();
                        accountInfo.put("email", userAccount.getEmail());
                        accountInfo.put("fullName", userAccount.getFkUserId().getFirstName() + " " + userAccount.getFkUserId().getLastName());
                        accountIdToDetails.put(userAccount.getAccountId(), accountInfo);
                    }

                    // Get all attachments for the comments retrieved from the DB
                    List<Long> combinedLogIds = new ArrayList<>(commentLogIds);
                    combinedLogIds.addAll(allParentAndChildCommentLogIds);
                    List<TaskAttachmentMetaInfo> attachments = taskAttachmentRepository.findAllAttachmentsByCommentLogIds(combinedLogIds);

                    // map commentLogId to a list of its attachments
                    Map<Long, List<TaskAttachmentMetaInfo>> attachmentsByCommentLogId = attachments.stream().collect(Collectors.groupingBy(TaskAttachmentMetaInfo::getCommentLogId));

                    // Set attachments info to allCommentsList
                    for (CommentPreview commentFromDb : allCommentsListFromDb) {
                        commentFromDb.setTaskAttachmentMetaInfoList(attachmentsByCommentLogId.get(commentFromDb.getCommentLogId()));
                    }

                    for (CommentPreview commentPreview : allCommentsListFromDb) {
                        if (commentPreview.getParentCommentLogId() == null) {
                            CommentResponse commentResponse = new CommentResponse();
                            commentResponse.setCommentLogId(commentPreview.getCommentLogId());
                            commentResponse.setComment(commentPreview.getComment());
                            HashMap<String, String> accountDetails = accountIdToDetails.get(commentPreview.getPostedByAccountId());
                            if (accountDetails != null) {
                                commentResponse.setCommentFromEmail(accountDetails.get("email"));
                                commentResponse.setCommentFromName(accountDetails.get("fullName"));
                            }
                            commentResponse.setTaskAttachmentMetaInfoList(commentPreview.getTaskAttachmentMetaInfoList());
                            commentResponse.setCommentsTags(commentPreview.getCommentsTags());
                            LocalDateTime ldt = LocalDateTime.ofInstant(commentPreview.getCreatedDateTime().toInstant(), ZoneId.systemDefault());
                            LocalDateTime convertedDateTime = DateTimeUtils.convertServerDateToUserTimezone(ldt, localTimeZone);
                            commentResponse.setCreatedDateTime(convertedDateTime);


//                        List<Long> childCommentLogIds = new ArrayList<>(commentPreview.getChildCommentLogIds());
//                        if (!childCommentLogIds.isEmpty()) {
//                            List<RelatedCommentInfo> replyCommentInfoList = new ArrayList<>();
//                            for (Long childCommentLogId : childCommentLogIds) {
//                                RelatedCommentInfo relatedCommentInfo = new RelatedCommentInfo();
//                                CommentPreview tempCommentPreview = allCommentsMap.get(childCommentLogId);
//                                if (tempCommentPreview != null) {
//                                    BeanUtils.copyProperties(tempCommentPreview, relatedCommentInfo);
//                                    List<TaskAttachmentMetaInfo> childAttachments = attachmentsByCommentLogId.get(childCommentLogId);
//                                    if (childAttachments != null && !childAttachments.isEmpty()) {
//                                        relatedCommentInfo.setAttachmentName(childAttachments.get(0).getFileName());
//                                    }
//                                    HashMap<String, String> accountIdDetails = accountIdToDetails.get(tempCommentPreview.getPostedByAccountId());
//                                    if (accountIdDetails != null) {
//                                        relatedCommentInfo.setLastCommentFromEmail(accountIdDetails.get("email"));
//                                        relatedCommentInfo.setLastCommentFromFullName(accountIdDetails.get("fullName"));
//                                    }
//                                    replyCommentInfoList.add(relatedCommentInfo);
//                                }
//                            }
//                            commentResponse.setReplyCommentInfoList(replyCommentInfoList);
//                        }

                            Set<Long> uniqueCommentLogIds = new HashSet<>(); // Set to track unique comment Log IDs
                            List<Long> childCommentLogIds = new ArrayList<>(commentPreview.getChildCommentLogIds());
                            if (!childCommentLogIds.isEmpty()) {
                                List<CommentResponse> replyCommentInfoList = new ArrayList<>();
                                for (Long childCommentLogId : childCommentLogIds) {
                                    createCommentResponse(childCommentLogId, allCommentsMap, attachmentsByCommentLogId, accountIdToDetails, 1, uniqueCommentLogIds, replyCommentInfoList, localTimeZone);
                                }
                                commentResponse.setReplyCommentInfoList(replyCommentInfoList);
                            }
                            commentResponseList.add(commentResponse);
                        }
                    }
                    Collections.sort(commentResponseList, Comparator.comparing(CommentResponse::getCreatedDateTime).reversed());
                    return commentResponseList;
                } catch (Exception e) {
                    e.printStackTrace();
                    String allStackTraces = StackTraceHandler.getAllStackTraces(e);
                    logger.error("Not able to execute the getCommentByPage() " + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
                    ThreadContext.clearMap();
                    throw new InternalServerErrorException(((IncorrectResultSizeDataAccessException) e).getMostSpecificCause().getMessage());
                }
            } else {
                throw new ForbiddenException("user does not have actions to get all comments of this Work Item for Work Item number = " + task.getTaskNumber());
            }
        } else {
            String allStackTraces = StackTraceHandler.getAllStackTraces(new TaskNotFoundException());
            logger.error("No task found. ", new Throwable(allStackTraces));
            ThreadContext.clearMap();
            throw new TaskNotFoundException();
        }
    }

    /**
     * Recursively creates a hierarchical structure of CommentResponse objects based on the provided CommentPreview and related data.
     * The resulting structure represents threaded comments with their attachments and user details.
     *
     * @param commentLogId                The ID of the current comment to process.
     * @param allCommentsMap              A map containing all CommentPreview objects, indexed by their comment log IDs.
     * @param attachmentsByCommentLogId   A map linking comment log IDs to lists of TaskAttachmentMetaInfo for associated attachments.
     * @param accountIdToDetails          A map linking account IDs to details (e.g., email, fullName) of the users who posted the comments.
     * @param currentLevel                The current depth level in the recursive processing of comments.
     * @param uniqueCommentLogIds         A set to track unique comment log IDs and avoid processing duplicate comments.
     * @param replyCommentInfoList        The list to which the resulting CommentResponse objects are added.
     * @param localTimeZone               The time zone used for converting date and time information.
     */
    private void createCommentResponse(Long commentLogId, Map<Long, CommentPreview> allCommentsMap,
                                       Map<Long, List<TaskAttachmentMetaInfo>> attachmentsByCommentLogId, Map<Long, HashMap<String, String>> accountIdToDetails,
                                       int currentLevel, Set<Long> uniqueCommentLogIds, List<CommentResponse> replyCommentInfoList, String localTimeZone) {
        CommentPreview tempCommentPreview = allCommentsMap.get(commentLogId);

        if (tempCommentPreview != null) {
            // Check if the comment Log ID is already in the set, and skip if duplicate
            if (uniqueCommentLogIds.contains(tempCommentPreview.getCommentLogId())) {
                return;
            }

            CommentResponse replyCommentResponse = new CommentResponse();
            BeanUtils.copyProperties(tempCommentPreview, replyCommentResponse);

            List<TaskAttachmentMetaInfo> childAttachments = attachmentsByCommentLogId.get(commentLogId);
            if (childAttachments != null && !childAttachments.isEmpty()) {
                replyCommentResponse.setTaskAttachmentMetaInfoList(childAttachments);
            }

            HashMap<String, String> accountIdDetails = accountIdToDetails.get(tempCommentPreview.getPostedByAccountId());
            if (accountIdDetails != null) {
                replyCommentResponse.setCommentFromEmail(accountIdDetails.get("email"));
                replyCommentResponse.setCommentFromName(accountIdDetails.get("fullName"));
            }

            Long parentCommentLogId = tempCommentPreview.getParentCommentLogId();
            if (parentCommentLogId != null) {
                RelatedCommentInfo relatedCommentInfo = new RelatedCommentInfo();
                CommentPreview tempParentCommentPreview = allCommentsMap.get(parentCommentLogId);
                if (tempParentCommentPreview != null) {
                    BeanUtils.copyProperties(tempParentCommentPreview, relatedCommentInfo);
                    List<TaskAttachmentMetaInfo> parentAttachments = attachmentsByCommentLogId.get(parentCommentLogId);
                    if (parentAttachments != null && !parentAttachments.isEmpty()) {
                        relatedCommentInfo.setAttachmentName(parentAttachments.get(0).getFileName());
                    }
                    HashMap<String, String> parentAccountIdDetails = accountIdToDetails.get(tempCommentPreview.getPostedByAccountId());
                    if (accountIdDetails != null) {
                        relatedCommentInfo.setLastCommentFromEmail(accountIdDetails.get("email"));
                        relatedCommentInfo.setLastCommentFromFullName(accountIdDetails.get("fullName"));
                    }
                    LocalDateTime ldt = LocalDateTime.ofInstant(tempParentCommentPreview.getCreatedDateTime().toInstant(), ZoneId.systemDefault());
                    LocalDateTime convertedDateTime = DateTimeUtils.convertServerDateToUserTimezone(ldt, localTimeZone);
                    relatedCommentInfo.setCreatedDateTime(Timestamp.valueOf(convertedDateTime));
                    replyCommentResponse.setParentCommentInfo(relatedCommentInfo);
                }
            }
            LocalDateTime ldt = LocalDateTime.ofInstant(tempCommentPreview.getCreatedDateTime().toInstant(), ZoneId.systemDefault());
            LocalDateTime convertedDateTime = DateTimeUtils.convertServerDateToUserTimezone(ldt, localTimeZone);
            replyCommentResponse.setCreatedDateTime(convertedDateTime);
            // Add the comment Log ID to the set to track uniqueness across all levels
            uniqueCommentLogIds.add(tempCommentPreview.getCommentLogId());


            List<Long> childCommentLogIds = new ArrayList<>(tempCommentPreview.getChildCommentLogIds());
            if (currentLevel <=1) {
                List<CommentResponse> childCommentInfoList = new ArrayList<>();
                for (Long childCommentLogId : childCommentLogIds) {
                    createCommentResponse(childCommentLogId, allCommentsMap, attachmentsByCommentLogId, accountIdToDetails, currentLevel + 1, uniqueCommentLogIds, childCommentInfoList, localTimeZone);
                }
                replyCommentResponse.setReplyCommentInfoList(childCommentInfoList);
            } else {
                for (Long childCommentLogId : childCommentLogIds) {
                    createCommentResponse(childCommentLogId, allCommentsMap, attachmentsByCommentLogId, accountIdToDetails, currentLevel + 1, uniqueCommentLogIds, replyCommentInfoList, localTimeZone);
                }
            }

            // Add the comment to replyCommentInfoList
            replyCommentInfoList.add(replyCommentResponse);
            Collections.sort(replyCommentInfoList, Comparator.comparing(CommentResponse::getCreatedDateTime).reversed());
        } else {
            return; // Return for non-existent comments
        }
    }


    /**
     * This method sanitizes the comment to prevent XSS attack and add files to the comment object and validates the input length of the comment
     */
    public void modifyCommentPropertiesAndValidate(Comment comment, MultipartFile[] files, String accountIds) {
        Long taskId = comment.getTask().getTaskId();
        Task task = taskRepository.findByTaskId(taskId);
        if (Objects.equals(task.getFkWorkflowTaskStatus().getWorkflowTaskStatus(), Constants.WorkFlowTaskStatusConstants.STATUS_DELETE_TITLE_CASE)) {
            throw new ValidationFailedException("User not allowed to add comments to a deleted task.");
        }
        boolean isSelfAssigned = false;
        if (task.getFkAccountIdAssigned() != null) {
            if (Objects.equals(Long.valueOf(accountIds), task.getFkAccountIdAssigned().getAccountId())) {
                isSelfAssigned = true;
            }
        }
        boolean updateResult = false;
        if (isSelfAssigned) {
            updateResult = taskService.isUpdateAllowed(com.tse.core_application.model.Constants.UpdateTeam.Task_Basic_Update, Long.valueOf(accountIds), task.getFkTeamId().getTeamId());
        } else {
            updateResult = taskService.isUpdateAllowed(com.tse.core_application.model.Constants.UpdateTeam.All_Task_Basic_Update, Long.valueOf(accountIds), task.getFkTeamId().getTeamId());
        }

        // validation: plain text validation
        if (!CommonUtils.isValidPlainTextLength(comment.getComment(), Constants.MAX_COMMENT_LENGTH)) {
            throw new ValidationFailedException("Comment length could be max " + Constants.MAX_COMMENT_LENGTH +  " characters");
        }

        if (updateResult) {
            addFilesToComment(comment, files);

            // Validation: If there are no attachments and the comment is empty or null, throw an exception.
            if ((files == null || files.length == 0) && (comment.getComment().trim().isEmpty())) {
                throw new ValidationFailedException("Comment cannot be empty when there are no attachments.");
            }

            // Validation: If childCommentIds is not null or empty then throw an exception
            if(CollectionUtils.isNotEmpty(comment.getChildCommentLogIds())) {
                throw new ValidationFailedException("Replies can not exist for a new message");
            }

            // Validation: If parentCommentLogId is not null then it should belong to the same task
            if(comment.getParentCommentLogId() != null) {
                Comment parentComment = commentRepository.findById(comment.getParentCommentLogId()).orElseThrow(() -> new IllegalArgumentException("invalid parent comment log id"));
                if(!Objects.equals(comment.getTask().getTaskId(), parentComment.getTask().getTaskId())) {
                    throw new ValidationFailedException("parent message doesn't belong to the same task");
                }
            }
            String safeCommentContent = sanitizeComment(comment.getComment());
            comment.setComment(safeCommentContent);
        } else {
            throw new ForbiddenException("user does not have actions to add comment for Work Item number = " + task.getTaskNumber());
        }
    }

    /** sanitize comment for safe HTML tags. It will remove any tags/ attributes that are not allowed/ safe */
    private String sanitizeComment (String comment) {
        // Start with the basic safe list and add custom tags and attributes
        Safelist customSafeList = Safelist.basic();
        customSafeList.addTags("s", "del")
                .addAttributes("span", "style", "data-email", "class");
        return Jsoup.clean(comment, customSafeList);
    }

    /**
     * This method adds the multipart files received in the request to the Comment Object
     * @param comment
     * @param files
     */
    public void addFilesToComment(Comment comment, MultipartFile[] files) {
        if (files != null && files.length > 0) {
            List<FileMetadata> allTaskAttachmentsFoundWithFileStatusA = taskAttachmentRepository.findFileMetadataByTaskIdAndFileStatus(comment.getTask().getTaskId(), com.tse.core_application.constants.Constants.FileAttachmentStatus.A);
            HashSet<String> uploadedFileNames = new HashSet<>();
            uploadedFileNames = (HashSet<String>) allTaskAttachmentsFoundWithFileStatusA.stream().map(FileMetadata::getFileName).collect(Collectors.toSet());
            // validation: no file name should be same for a given taskId
            for (MultipartFile file : files) {
                ScanResult scanResult = ComponentUtils.scanFile(file);
                if (!scanResult.getStatus().equals("PASSED")) {
                    throw new ValidationFailedException("File scan failed for: " + file.getOriginalFilename() + ".The file might be infected or corrupted.");
                }

                String sanitizedFileName = FileUtils.sanitizeFilename(file.getOriginalFilename());
                if(!uploadedFileNames.contains(sanitizedFileName)) {
                    uploadedFileNames.add(sanitizedFileName);
                } else {
                    throw new DuplicateFileNameException("\"A file with the name '\"" + sanitizedFileName + "\"' already exists for the given Work Item\"");
                }
            }

            List<TaskAttachment> taskAttachments = new ArrayList<>();
            for (MultipartFile file : files) {
                TaskAttachment taskAttachment = new TaskAttachment();
                String sanitizedFileName = FileUtils.sanitizeFilename(file.getOriginalFilename());
                taskAttachment.setFileName(sanitizedFileName);
                taskAttachment.setFileType(file.getContentType());
                taskAttachment.setFileSize((double) file.getSize());
                taskAttachment.setUploaderAccountId(comment.getPostedByAccountId());
                taskAttachment.setFileStatus(com.tse.core_application.constants.Constants.FileAttachmentStatus.A);
                taskAttachment.setTaskId(comment.getTask().getTaskId());
                try {
                    taskAttachment.setFileContent(file.getBytes());
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read file content", e);
                }
                taskAttachment.setComment(comment);
                taskAttachments.add(taskAttachment);
            }
            comment.setTaskAttachments(taskAttachments);
        }
    }

    public CommentResponse addComment(Comment comment, String accountIds, String timeZone) throws JsonProcessingException {
        //validation: posted by AccountId should be same as the account id in the header
        if (!Objects.equals(comment.getPostedByAccountId(), Long.valueOf(accountIds))) {
            throw new ValidationFailedException("invalid request parameter: posted by account Id");
        }

        Task task = entityManager.getReference(Task.class, comment.getTask().getTaskId());
        Long commentId = getCommentIdFromTaskTable(comment.getTask().getTaskId());
        Comment commentSave = null;
        if (commentId == null) {
            Long assignCommentId = getMaxCommentId();
            comment.setTask(task);
            comment.setCommentId(assignCommentId);
            commentSave = commentRepository.save(comment);
            taskRepository.setTaskCommentIdByTaskId(assignCommentId, comment.getTask().getTaskId());
        } else {
            comment.setCommentId(commentId);
            comment.setTask(task);
            commentSave = commentRepository.save(comment);
        }

        List<FileMetadata> allActiveTaskAttachmentsFound = taskAttachmentRepository.findFileMetadataByTaskIdAndFileStatus(task.getTaskId(), com.tse.core_application.constants.Constants.FileAttachmentStatus.A);
        taskServiceImpl.updateTaskAttachmentsByTaskId(objectMapper.writeValueAsString(allActiveTaskAttachmentsFound), task.getTaskId());

        if(commentSave.getParentCommentLogId() != null) {
            modifyParentCommentForReplies(commentSave.getParentCommentLogId(), commentSave.getCommentLogId());
        }

        List<Long> taggedAccountIds = new ArrayList<>();
        List<String> usersEmailInTag = processCommentForTags(commentSave);
        if (!usersEmailInTag.isEmpty()) {
            taggedAccountIds = getTaggedUsersFromCommentAndValidate(task, usersEmailInTag);
        }

        try {
            if (!taggedAccountIds.isEmpty()) {
                sendMentionInCommentNotification(commentSave, timeZone, taggedAccountIds);
            }
            sendNewCommentNotification(commentSave, timeZone, taggedAccountIds);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Something went wrong: Not able to send notification for new comment " + e, new Throwable(allStackTraces));
        }

        UserAccount userAccount = userAccountRepository.findByAccountId(commentSave.getPostedByAccountId());

        CommentResponse commentResponse = new CommentResponse();

        commentResponse.setCommentLogId(commentSave.getCommentLogId());
        commentResponse.setComment(commentSave.getComment());
        commentResponse.setCommentsTags(commentSave.getCommentsTags());
        commentResponse.setPostedByAccountId(commentSave.getPostedByAccountId());

        LocalDateTime commentCreatedDateTime = commentSave.getCreatedDateTime().toLocalDateTime();
        commentResponse.setCreatedDateTime(DateTimeUtils.convertServerDateToUserTimezone(commentCreatedDateTime, timeZone));

        commentResponse.setCommentFromEmail(userAccount.getEmail());
        commentResponse.setCommentFromName(userAccount.getFkUserId().getFirstName() + " " + userAccount.getFkUserId().getLastName());
//        Need to discuss
//        commentResponse.setTaskAttachmentMetaInfoList(null);

        return commentResponse;
    }

    /**
     * Parses a comment object to extract email IDs of tagged users using HTML tags and attributes.
     */

    private List<String> processCommentForTags(Comment comment) {
        List<String> emailIds = new ArrayList<>();
        Document doc = Jsoup.parse(comment.getComment()); // Parse the comment as HTML

        // Extract user email IDs if any user is tagged
        Elements taggedUsers = doc.select("span.user-tag[data-email]");
        if (!taggedUsers.isEmpty()) {
            for (Element tag : taggedUsers) {
                String userEmail = tag.attr("data-email");
                // verify that the user exists in the given entity
                emailIds.add(userEmail);
            }
        }
        return emailIds;
    }

    /**
     * Extracts and validates tagged user IDs from a comment, ensuring they exist in the same team as the task .
     */
    private List<Long> getTaggedUsersFromCommentAndValidate(Task task, List<String> usersEmailInTag) {
        List<Long> taggedUsersAccountIds = new ArrayList<>();

        for (String email : usersEmailInTag) {
            UserAccount userAccount = userAccountRepository.findByEmailAndOrgIdAndIsActive(email, task.getFkOrgId().getOrgId(), true);
            Boolean doesUserexistInEntity = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdAndIsActive(Constants.EntityTypes.TEAM, task.getFkTeamId().getTeamId(), userAccount.getAccountId(), true);
            if (!doesUserexistInEntity) {
                throw new ValidationFailedException("tagged user doesn't exists in this team");
            } else {
                taggedUsersAccountIds.add(userAccount.getAccountId());
            }
        }

        return taggedUsersAccountIds;
    }


    /**
     * adds a reply's commentLogId to the parent comment
     */
    public void modifyParentCommentForReplies(Long parentCommentLogId, Long childCommentLogId) {
        Comment parentComment = commentRepository.findById(parentCommentLogId).orElseThrow(() -> new IllegalArgumentException("parent comment doesn't exist"));
        List<Long> existingChildMessageIds = new ArrayList<>(parentComment.getChildCommentLogIds());
        existingChildMessageIds.add(childCommentLogId);
        parentComment.setChildCommentLogIds(existingChildMessageIds);
        commentRepository.save(parentComment);
    }

    public String updateCommentTag (CommentTagRequest commentTagRequest, String commentAiKey) {
        if (!commentAiKey.equalsIgnoreCase(commentApiKey)) {
            throw new ValidationFailedException("You do not have access to change the comment tag");
        }
        Comment comment = commentRepository.findByCommentLogId (commentTagRequest.getCommentLogId());
        if (comment == null) {
            throw new EntityNotFoundException("Comment doesn't exist");
        }
        comment.setCommentsTags(commentTagRequest.getCommentsTags());
        commentRepository.save(comment);

        return "Comment tag successfully updated";
    }

}
