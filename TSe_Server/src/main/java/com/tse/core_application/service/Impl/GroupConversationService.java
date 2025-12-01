package com.tse.core_application.service.Impl;

import com.nimbusds.oauth2.sdk.util.CollectionUtils;
import com.tse.core_application.custom.model.*;
import com.tse.core_application.dto.*;
import com.tse.core_application.dto.conversations.GroupConversationResponse;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.handlers.StackTraceHandler;
import com.tse.core_application.model.*;
import com.tse.core_application.model.User;
import com.tse.core_application.repository.*;
import com.tse.core_application.utils.CommonUtils;
import com.tse.core_application.utils.ComponentUtils;
import com.tse.core_application.utils.DateTimeUtils;
import com.tse.core_application.utils.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GroupConversationService {

    private static final Logger logger = LogManager.getLogger(CommentService.class.getName());

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private AccessDomainService accessDomainService;

    @Autowired
    private GroupConversationRepository gcRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TaskServiceImpl taskServiceImpl;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private AccessDomainRepository accessDomainRepository;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private TeamRepository teamRepository;

    /**
     * This method sanitizes the message to prevent XSS attack and add files to the groupConversation object
     */
    public void modifyMessagePropertiesAndValidate(GroupConversation gcMessage, MultipartFile[] files) {
        // convert multipart files to Attachment object and set it in the group conversation message
        addFilesToGcMessage(gcMessage, files);

        // validation: plain text validation
        if (!CommonUtils.isValidPlainTextLength(gcMessage.getMessage(), Constants.MAX_GC_MESSAGE_LENGTH)) {
            throw new ValidationFailedException("Message length could be max " + Constants.MAX_GC_MESSAGE_LENGTH +  " characters");
        }

        // Validation: If there are no attachments and the message is empty or null, throw an exception.
        if ((files == null || files.length == 0) && (gcMessage.getMessage().trim().isEmpty())) {
            throw new ValidationFailedException("Message cannot be empty when there are no attachments.");
        }

        // Validation: If childTaskIds is not null or empty then throw an exception
        if(CollectionUtils.isNotEmpty(gcMessage.getChildGcIds())) {
            throw new ValidationFailedException("Replies can not exist for a new message");
        }

        // Validation: If parentGroupConversationId is not null then it should belong to the same entity
        if(gcMessage.getParentGroupConversationId() != null) {
            GroupConversation parentMessage = gcRepository.findById(gcMessage.getParentGroupConversationId()).orElseThrow(() -> new IllegalArgumentException("invalid parent group conversation id"));
            if(!Objects.equals(gcMessage.getEntityTypeId(), parentMessage.getEntityTypeId()) || !Objects.equals(gcMessage.getEntityId(), parentMessage.getEntityId())) {
                throw new ValidationFailedException("parent message doesn't belong to the same entity");
            }
        }

        String safeCommentContent = sanitizeMessage(gcMessage.getMessage());
        gcMessage.setMessage(safeCommentContent);
    }

    /** sanitize message for safe HTML tags */
    private String sanitizeMessage (String message) {
        // Start with the basic safe list and add custom tags and attributes
        Safelist customSafeList = Safelist.basic();
        customSafeList.addTags("s", "del")
                .addAttributes("span", "style", "data-email", "class");
        return Jsoup.clean(message, customSafeList);
    }

    /**
     * This method adds the multipart files received in the request to the GroupConversation Object
     */
    public void addFilesToGcMessage(GroupConversation gcMessage, MultipartFile[] files) {
        if (files != null && files.length > 0) {
            List<Attachment> attachments = new ArrayList<>();
            for (MultipartFile file : files) {
                ScanResult scanResult = ComponentUtils.scanFile(file);
                if (!scanResult.getStatus().equals("PASSED")) {
                    throw new ValidationFailedException("File scan failed for: " + file.getOriginalFilename() + ".The file might be infected or corrupted.");
                }
                Attachment attachment = new Attachment();
                String sanitizedFileName = FileUtils.sanitizeFilename(file.getOriginalFilename());
                attachment.setFileName(sanitizedFileName);
                attachment.setFileType(file.getContentType());
                attachment.setFileSize(file.getSize());
                attachment.setUploaderAccountId(gcMessage.getPostedByAccountId());
                try {
                    attachment.setFileContent(file.getBytes());
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read file content", e);
                }
                attachment.setGroupConversation(gcMessage);
                attachments.add(attachment);
            }
            gcMessage.setAttachments(attachments);
        }
    }

    /**
     * saves a new message for the given entity Id
     */
    public GroupConversationResponse addGcMessageForEntity(GroupConversation gcMessage, String accountIds, String timeZone) {
        GroupConversationResponse response = new GroupConversationResponse();
        Integer entityTypeId = gcMessage.getEntityTypeId();
        Long accountIdInHeader = Long.valueOf(accountIds);

        //validation: posted by AccountId should be same as the account id in the header
        if (!Objects.equals(gcMessage.getPostedByAccountId(), accountIdInHeader)) {
            throw new ValidationFailedException("invalid request parameter: posted by account Id");
        }

        // validation: account that is adding the comment is a part of the given entity
        if (entityTypeId.equals(Constants.EntityTypes.ORG)) {
            UserAccount userAccount = userAccountService.getActiveUserAccountByAccountId(gcMessage.getPostedByAccountId());
            if (!Objects.equals(userAccount.getOrgId(), gcMessage.getEntityId()))
                throw new ValidationFailedException("accountId not part of the given entity type");
        } else if (entityTypeId.equals(Constants.EntityTypes.TEAM)) {
            List<CustomAccessDomain> customAccessDomains = accessDomainService.getAccessDomainByAccountIdAndEntityId(gcMessage.getPostedByAccountId(), gcMessage.getEntityId());
            if (customAccessDomains == null || customAccessDomains.isEmpty())
                throw new ValidationFailedException("accountId not part of the given entity type");
        }

        GroupConversation savedGcMessage = gcRepository.save(gcMessage);
        BeanUtils.copyProperties(savedGcMessage, response);

        // if the message is a reply, then include the parent gc message information in the API response
        if (savedGcMessage.getParentGroupConversationId() != null) {
            GroupConversation savedParentMsg = modifyParentMessageForReplies(savedGcMessage.getParentGroupConversationId(), savedGcMessage.getGroupConversationId());
            ParentGcInfo parentGcInfo = createParentGcResponse(savedParentMsg);
            response.setParentGcInfo(parentGcInfo);
        }

        List<Long> taggedAccountIds = new ArrayList<>();
        List<String> usersEmailInTag = processMessageForTags(gcMessage);
        if (!usersEmailInTag.isEmpty()) {
            taggedAccountIds = getTaggedUsersFromMessageAndValidate(gcMessage, usersEmailInTag);
        }

        try {
            if (!taggedAccountIds.isEmpty()) {
                sendEntityTagNotification(gcMessage, taggedAccountIds, timeZone,accountIds);
            }
            sendNewMessageInEntityNotification(gcMessage, timeZone, taggedAccountIds);
        } catch (Exception e) {
            e.printStackTrace();
            String allStackTraces = StackTraceHandler.getAllStackTraces(e);
            logger.error("Something went wrong: Not able to send notification for new message added in entity " + e, new Throwable(allStackTraces));
        }

        return response;
    }

    /**
     * Extracts and validates tagged user IDs from a group conversation message, ensuring they exist in the specified entity (ORG or TEAM).
     */
    private List<Long> getTaggedUsersFromMessageAndValidate(GroupConversation gcMessage, List<String> usersEmailInTag) {
        List<Long> taggedUsersAccountIds = new ArrayList<>();
        if(usersEmailInTag != null && !usersEmailInTag.isEmpty()) {
            if(gcMessage.getEntityTypeId().equals(Constants.EntityTypes.ORG)) {
                for (String email : usersEmailInTag) {
                    UserAccount userAccount = userAccountRepository.findByEmailAndOrgIdAndIsActive(email, gcMessage.getEntityId(), true);
                    if (userAccount == null) {
                        throw new ValidationFailedException("tagged user doesn't exists in the given entity");
                    } else {
                        taggedUsersAccountIds.add(userAccount.getAccountId());
                    }
                }
            } else if (gcMessage.getEntityTypeId().equals(Constants.EntityTypes.TEAM)) {
                for (String email : usersEmailInTag) {
                    Team team = teamRepository.findByTeamId(gcMessage.getEntityId());
                    UserAccount userAccount = userAccountRepository.findByEmailAndOrgIdAndIsActive(email, team.getFkOrgId().getOrgId(), true);
                    Boolean isUserexistsInEntity = accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdAndIsActive(Constants.EntityTypes.TEAM, gcMessage.getEntityId(), userAccount.getAccountId(), true);
                    if(!isUserexistsInEntity) {
                        throw new ValidationFailedException("tagged user doesn't exists in the given entity");
                    } else {
                        taggedUsersAccountIds.add(userAccount.getAccountId());
                    }
                }
            }
        }
        return taggedUsersAccountIds;
    }

    /**
     * Parses a group conversation message to extract email IDs of tagged users using HTML tags and attributes.
     */
    public List<String> processMessageForTags(GroupConversation gcMessage) {
        List<String> userEmails = new ArrayList<>();
        Document doc = Jsoup.parse(gcMessage.getMessage());

        // Extract all span tags with the class 'user-tag'
        Elements taggedUsers = doc.select("span.user-tag");
        for (Element tag : taggedUsers) {
            String userEmail = tag.attr("data-email");
            String nameWithAt = tag.text().trim(); // Get the name including '@'

            // Remove the '@' symbol from the beginning of the name
            if (nameWithAt.startsWith("@")) {
                nameWithAt = nameWithAt.substring(1);
            }

            String[] nameWords = nameWithAt.split("\\s+");

            // Assuming the first word is the first name and the last word is the last name
            String firstName = nameWords[0];
            String lastName = nameWords[nameWords.length - 1];
            String middleName = null;

            // If there are more than two words, assume the second word is the middle name
            if (nameWords.length > 2) {
                middleName = nameWords[1];
            }

            List<UserAccount> userAccounts = userAccountRepository.findByEmailAndIsActive(userEmail, true);

            // Verify the extracted name against the user account's first name, last name, and middle name
            if (userAccounts != null && !userAccounts.isEmpty()) {
                User user = userAccounts.get(0).getFkUserId();
                if (!firstName.equalsIgnoreCase(user.getFirstName()) || !lastName.equalsIgnoreCase(user.getLastName()) ||
                        (middleName != null && !Objects.equals(middleName, user.getMiddleName()))) {
                    throw new ValidationFailedException("Tagged user's name doesn't match");
                }
            } else {
                throw new IllegalArgumentException("No user account found for the tagged user");
            }
            userEmails.add(userEmail);
        }

        return userEmails;
    }

    /**
     * method creates a ParentGcInfo response from the parent group conversation message
     */
    public ParentGcInfo createParentGcResponse(GroupConversation parentGc) {
        ParentGcInfo parentGcInfo = new ParentGcInfo();
        BeanUtils.copyProperties(parentGc, parentGcInfo);
        UserAccount userAccount = userAccountRepository.findByAccountIdAndIsActive(parentGc.getPostedByAccountId(), true);
        parentGcInfo.setLastMessageFromEmail(userAccount.getEmail());
        String fullName = userAccount.getFkUserId().getFirstName() + " " + userAccount.getFkUserId().getLastName();
        parentGcInfo.setLastMessageFromFullName(fullName);
        List<Attachment> parentGcAttachments = parentGc.getAttachments();
        List<AttachmentMetadata> attachmentsMetadata = new ArrayList<>();
        if(!parentGcAttachments.isEmpty()) {
            for (Attachment attachment: parentGcAttachments) {
                AttachmentMetadata attachmentMetadata = new AttachmentMetadata();
                attachmentMetadata.setAttachmentId(attachment.getAttachmentId());
                attachmentMetadata.setFileName(attachment.getFileName());
                attachmentMetadata.setFileSize(attachment.getFileSize());
                attachmentMetadata.setFileType(attachment.getFileType());
                attachmentsMetadata.add(attachmentMetadata);
            }
        }
        parentGcInfo.setAttachmentsMetadata(attachmentsMetadata);
        return parentGcInfo;
    }

    /**
     * method to send notification for a new message added in a group (team/ org)
     */
    public void sendNewMessageInEntityNotification(GroupConversation gcMessage, String timeZone, List<Long> taggedAccountIds) {
        List<HashMap<String, String>> payload = notificationService.createNotificationForMessageInEntity(gcMessage, timeZone, taggedAccountIds);
        taskServiceImpl.sendPushNotification(payload);
    }

    /**
     * method for sending notifications related to tagging (Tag) (@UserMention) within a specific entity
     */
    public void sendEntityTagNotification(GroupConversation gcMessage, List<Long> taggedAccountIds, String timeZone,String headerAccountIds) {
        List<HashMap<String, String>> payload = notificationService.createNotificationForTagInGroupConversation(gcMessage, taggedAccountIds, timeZone,headerAccountIds);
        taskServiceImpl.sendPushNotification(payload);
    }

    /**
     * gets All messages for the given entity from the group conversation table ordered by created date time and returns custom response
     */
    public List<EntityMessageResponse> getEntityAllMessages(Integer entityTypeId, Long entityId, Integer pageNumber, Integer pageSize, String localTimeZone) {
        List<EntityMessageResponse> entityMessageResponseList = new ArrayList<>();
        Set<Long> allUniqueAccountIds = new HashSet<>();
        Set<Long> allParentAndChildGcIds = new HashSet<>();
        Set<Long> gcIds = new HashSet<>();
        HashMap<Long, HashMap<String, String>> accountIdToDetails = new HashMap<>();

        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("createdDateTime").descending());
        List<GroupConversation> allMessages = gcRepository.findByEntityTypeIdAndEntityId(entityTypeId, entityId, pageable);

        for (GroupConversation gcMessage : allMessages) {
            allUniqueAccountIds.add(gcMessage.getPostedByAccountId());
            gcIds.add(gcMessage.getGroupConversationId());
            allParentAndChildGcIds.add(gcMessage.getParentGroupConversationId());
            allParentAndChildGcIds.addAll(new ArrayList<>(gcMessage.getChildGcIds()));
        }

        // unique parent or child gc Ids that are not in all messages
        allParentAndChildGcIds.removeAll(gcIds);
        List<GroupConversation> parentAndChildGcNotInAllMessages = gcRepository.findByGroupConversationIdIn(new ArrayList<>(allParentAndChildGcIds));
        Set<Long> combinedGcIds = new HashSet<>(gcIds);
        combinedGcIds.addAll(allParentAndChildGcIds);
        List<GroupConversation> combinedMessages = new ArrayList<>(allMessages);
        combinedMessages.addAll(parentAndChildGcNotInAllMessages);
        HashMap<Long, GroupConversation> combinedMessagesMap = (HashMap<Long, GroupConversation>) combinedMessages.stream().collect(Collectors.toMap(GroupConversation::getGroupConversationId, gc -> gc));

        for (GroupConversation relatedMessage : parentAndChildGcNotInAllMessages) {
            allUniqueAccountIds.add(relatedMessage.getPostedByAccountId());
        }

        List<UserAccount> userAccountsDb = userAccountRepository.findByAccountIdInAndIsActive(new ArrayList<>(allUniqueAccountIds), true);
        List<AttachmentProjection> allAttachments = attachmentRepository.findByGroupConversationIds(new ArrayList<>(combinedGcIds));

        Map<Long, List<AttachmentMetadata>> attachmentsByGcId = new HashMap<>();
        for (AttachmentProjection attachment : allAttachments) {
            AttachmentMetadata metadata = new AttachmentMetadata();
            metadata.setAttachmentId(attachment.getAttachmentId());
            metadata.setFileName(attachment.getFileName());
            metadata.setFileType(attachment.getFileType());
            metadata.setFileSize(attachment.getFileSize());

            Long gcId = attachment.getGroupConversationId();
            attachmentsByGcId.computeIfAbsent(gcId, k -> new ArrayList<>())
                    .add(metadata);
        }

        for (UserAccount userAccount : userAccountsDb) {
            HashMap<String, String> accountInfo = new HashMap<>();
            accountInfo.put("email", userAccount.getEmail());
            accountInfo.put("fullName", userAccount.getFkUserId().getFirstName() + " " + userAccount.getFkUserId().getLastName());
            accountIdToDetails.put(userAccount.getAccountId(), accountInfo);
        }

        for (GroupConversation gcMessage : allMessages) {
            EntityMessageResponse entityMessageResponse = new EntityMessageResponse();
            entityMessageResponse.setGroupConversationId(gcMessage.getGroupConversationId());
            entityMessageResponse.setEntityId(gcMessage.getEntityId());
            entityMessageResponse.setEntityTypeId(gcMessage.getEntityTypeId());
            entityMessageResponse.setMessage(gcMessage.getMessage());
            entityMessageResponse.setMessageTags(gcMessage.getMessageTags());
            entityMessageResponse.setCreatedDateTime(DateTimeUtils.convertServerDateToUserTimezone(gcMessage.getCreatedDateTime().toLocalDateTime(), localTimeZone));
            HashMap<String, String> accountDetails = accountIdToDetails.get(gcMessage.getPostedByAccountId());
            if (accountDetails != null) {
                entityMessageResponse.setLastMessageFromEmail(accountDetails.get("email"));
                entityMessageResponse.setLastMessageFromFullName(accountDetails.get("fullName"));
            }
            entityMessageResponse.setAttachments(attachmentsByGcId.getOrDefault(gcMessage.getGroupConversationId(), Collections.emptyList()));

            Long parentGcId = gcMessage.getParentGroupConversationId();
            if (parentGcId != null) {
                ParentMsgInfo parentMsgInfo = new ParentMsgInfo();
                GroupConversation tempGc = combinedMessagesMap.get(parentGcId);
                if (tempGc != null) {
                    BeanUtils.copyProperties(tempGc, parentMsgInfo);
                    List<AttachmentMetadata> parentAttachments = attachmentsByGcId.get(parentGcId);
                    if (parentAttachments != null && !parentAttachments.isEmpty()) {
                        // need to check if we are selecting the first file in order
                        parentMsgInfo.setAttachmentName(parentAttachments.get(0).getFileName());
                    }
                    HashMap<String, String> accountIdDetails = accountIdToDetails.get(tempGc.getPostedByAccountId());
                    if (accountIdDetails != null) {
                        parentMsgInfo.setLastMessageFromEmail(accountIdDetails.get("email"));
                        parentMsgInfo.setLastMessageFromFullName(accountIdDetails.get("fullName"));
                    }
                    entityMessageResponse.setParentMsgInfo(parentMsgInfo);
                }
            }

            List<Long> childGcIds = new ArrayList<>(gcMessage.getChildGcIds());
            if (!childGcIds.isEmpty()) {
                List<ReplyMsgInfo> replyMsgInfoList = new ArrayList<>();
                for (Long childGcId : childGcIds) {
                    ReplyMsgInfo replyMsgInfo = new ReplyMsgInfo();
                    GroupConversation tempGc = combinedMessagesMap.get(childGcId);
                    if (tempGc != null) {
                        BeanUtils.copyProperties(tempGc, replyMsgInfo);
                        List<AttachmentMetadata> childAttachments = attachmentsByGcId.get(childGcId);
                        if (childAttachments != null && !childAttachments.isEmpty()) {
                            replyMsgInfo.setAttachmentName(childAttachments.get(0).getFileName());
                        }
                        HashMap<String, String> accountIdDetails = accountIdToDetails.get(tempGc.getPostedByAccountId());
                        if (accountIdDetails != null) {
                            replyMsgInfo.setLastMessageFromEmail(accountIdDetails.get("email"));
                            replyMsgInfo.setLastMessageFromFullName(accountIdDetails.get("fullName"));
                        }
                        replyMsgInfoList.add(replyMsgInfo);
                    }
                }
                entityMessageResponse.setReplyMsgInfoList(replyMsgInfoList);
            }

            entityMessageResponseList.add(entityMessageResponse);
        }
        return entityMessageResponseList;
    }

    /**
     * Retrieves entities that the user (identified by the given account IDs) is part of,
     * and returns them in the order of the latest messages. It retrieves the last message and createdDateTime of that message
     * in each entity with and then order all the entities based on createdDateTime
     */
    public List<EntityOrderResponse> getEntitiesOrderByMessage(List<Long> accountIds, String localTimeZone) {
        List<EntityInfo> entityInfos = new ArrayList<>();
        List<EntityOrderResponse> entityOrderResponses = new ArrayList<>();

        entityInfos.addAll(userAccountRepository.findOrgInfoByAccountIdsAndIsActive(accountIds, true));
        entityInfos.addAll(accessDomainRepository.getTeamInfoByAccountIdsAndIsActive(accountIds));
        List<Project> projectList = accessDomainRepository.getProjectInfoByAccountIdsAndIsActiveTrue(accountIds);
        for (Project project : projectList) {
            if (!Objects.equals(project.getProjectType(), com.tse.core_application.constants.Constants.ProjectType.DEFAULT_PROJECT) && !project.getProjectName().contains(Constants.DEFAULT_INDICATOR)) {
                EntityInfo entityInfo = new EntityInfo(project.getProjectId(), Constants.EntityTypes.PROJECT, project.getProjectName());
                entityInfos.add(entityInfo);
            }
        }
        for(EntityInfo entityInfo: entityInfos) {
            EntityOrderResponse response = new EntityOrderResponse();
            if (entityInfo.getEntityName().equalsIgnoreCase(Constants.PERSONAL_ORG_DEFAULT_TEAM_NAME) || entityInfo.getEntityName().equalsIgnoreCase(Constants.PERSONAL_ORG)) {
                continue;
            }
            GroupConversation latestMsg = gcRepository.findFirstByEntityIdAndEntityTypeIdOrderByCreatedDateTimeDesc(entityInfo.getEntityId(), entityInfo.getEntityTypeId());
            response.setEntityId(entityInfo.getEntityId());
            response.setEntityTypeId(entityInfo.getEntityTypeId());
            response.setEntityName(entityInfo.getEntityName());
            if(latestMsg != null) {
                response.setMessage(latestMsg.getMessage());
                response.setCreatedDateTime(DateTimeUtils.convertServerDateToUserTimezone(latestMsg.getCreatedDateTime().toLocalDateTime(), localTimeZone));
                UserAccount userAccount = userAccountRepository.findByAccountId(latestMsg.getPostedByAccountId());
                response.setLastMessageFromEmail(userAccount.getEmail());
                response.setLastMessageFromFullName(userAccount.getFkUserId().getFirstName() + " " + userAccount.getFkUserId().getLastName());
            }
            entityOrderResponses.add(response);
        }
        entityOrderResponses.sort(Comparator.comparing(EntityOrderResponse::getCreatedDateTime, Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
        return entityOrderResponses;
    }

    /**
     * adds a reply's gcLogId to the parent gcMessage
     */
    public GroupConversation modifyParentMessageForReplies(Long parentGcId, Long childGcId) {
        GroupConversation parentGcMessage = gcRepository.findById(parentGcId).orElseThrow(() -> new IllegalArgumentException("parent message doesn't exist"));
        List<Long> existingChildMessageIds = new ArrayList<>(parentGcMessage.getChildGcIds());
        existingChildMessageIds.add(childGcId);
        parentGcMessage.setChildGcIds(existingChildMessageIds);
        return gcRepository.save(parentGcMessage);
    }

    /** gets messages between two group conversation Ids. The comments are sent between gcId1 - 10 to gcId2 - 1 */
    public List<EntityMessageResponse> getEntityMessagesBetweenGroupConversations(Integer entityTypeId, Long entityId, Long userId, List<Long> accountIds, Long firstGcId, Long secondGcId, String timeZone) {
        List<EntityMessageResponse> entityMessageResponseList = new ArrayList<>();
        Set<Long> allUniqueAccountIds = new HashSet<>();
        Set<Long> allParentAndChildGcIds = new HashSet<>();
        Set<Long> gcIds = new HashSet<>();
        HashMap<Long, HashMap<String, String>> accountIdToDetails = new HashMap<>();

        // Validation: user belong to the given entity
        boolean isUserInGivenEntity = false;
        if (Objects.equals(entityTypeId, Constants.EntityTypes.ORG) && userAccountService.isActiveUserAccountExistsByUserIdAndOrgId(userId, entityId)) {
            isUserInGivenEntity = true;
        } else if (Objects.equals(entityTypeId, Constants.EntityTypes.TEAM)) {
            List<AccessDomain> accessDomains = accessDomainRepository.findByEntityTypeIdAndEntityIdAndAccountIdInAndIsActive(entityTypeId, entityId, accountIds, true);
            if (accessDomains != null && !accessDomains.isEmpty()) isUserInGivenEntity = true;
        }
        if (!isUserInGivenEntity) {
            throw new ValidationFailedException("user doesn't belong to this entity group");
        }

        // validation: both the group conversation ids belong to the given entityId
        List<GroupConversation> gcs = gcRepository.findByGroupConversationIdIn(List.of(firstGcId, secondGcId));
        boolean isGivenGcIdsValidated = gcs.stream().allMatch(gc -> Objects.equals(gc.getEntityId(), entityId) && Objects.equals(gc.getEntityTypeId(), entityTypeId));
        if (!isGivenGcIdsValidated) throw new ValidationFailedException("invalid group conversation Ids");

        Long startGcId = Math.max(1, firstGcId - 10);
        Long endGcId = secondGcId - 1;
        List<GroupConversation> allMessages = gcRepository.findMessagesBetweenGroupConversationIds(entityTypeId, entityId, startGcId, endGcId);

        for (GroupConversation gcMessage : allMessages) {
            allUniqueAccountIds.add(gcMessage.getPostedByAccountId());
            gcIds.add(gcMessage.getGroupConversationId());
            allParentAndChildGcIds.add(gcMessage.getParentGroupConversationId());
            allParentAndChildGcIds.addAll(new ArrayList<>(gcMessage.getChildGcIds()));
        }

        // unique parent or child gc Ids that are not in all messages
        allParentAndChildGcIds.removeAll(gcIds);
        List<GroupConversation> parentAndChildGcNotInAllMessages = gcRepository.findByGroupConversationIdIn(new ArrayList<>(allParentAndChildGcIds));
        Set<Long> combinedGcIds = new HashSet<>(gcIds);
        combinedGcIds.addAll(allParentAndChildGcIds);
        List<GroupConversation> combinedMessages = new ArrayList<>(allMessages);
        combinedMessages.addAll(parentAndChildGcNotInAllMessages);
        HashMap<Long, GroupConversation> combinedMessagesMap = (HashMap<Long, GroupConversation>) combinedMessages.stream().collect(Collectors.toMap(GroupConversation::getGroupConversationId, gc -> gc));

        for (GroupConversation relatedMessage : parentAndChildGcNotInAllMessages) {
            allUniqueAccountIds.add(relatedMessage.getPostedByAccountId());
        }

        List<UserAccount> userAccountsDb = userAccountRepository.findByAccountIdInAndIsActive(new ArrayList<>(allUniqueAccountIds), true);
        List<AttachmentProjection> allAttachments = attachmentRepository.findByGroupConversationIds(new ArrayList<>(combinedGcIds));

        Map<Long, List<AttachmentMetadata>> attachmentsByGcId = new HashMap<>();
        for (AttachmentProjection attachment : allAttachments) {
            AttachmentMetadata metadata = new AttachmentMetadata();
            metadata.setAttachmentId(attachment.getAttachmentId());
            metadata.setFileName(attachment.getFileName());
            metadata.setFileType(attachment.getFileType());
            metadata.setFileSize(attachment.getFileSize());

            Long gcId = attachment.getGroupConversationId();
            attachmentsByGcId.computeIfAbsent(gcId, k -> new ArrayList<>())
                    .add(metadata);
        }

        for (UserAccount userAccount : userAccountsDb) {
            HashMap<String, String> accountInfo = new HashMap<>();
            accountInfo.put("email", userAccount.getEmail());
            accountInfo.put("fullName", userAccount.getFkUserId().getFirstName() + " " + userAccount.getFkUserId().getLastName());
            accountIdToDetails.put(userAccount.getAccountId(), accountInfo);
        }

        for (GroupConversation gcMessage : allMessages) {
            EntityMessageResponse entityMessageResponse = new EntityMessageResponse();
            entityMessageResponse.setGroupConversationId(gcMessage.getGroupConversationId());
            entityMessageResponse.setEntityId(gcMessage.getEntityId());
            entityMessageResponse.setEntityTypeId(gcMessage.getEntityTypeId());
            entityMessageResponse.setMessage(gcMessage.getMessage());
            entityMessageResponse.setMessageTags(gcMessage.getMessageTags());
            entityMessageResponse.setCreatedDateTime(DateTimeUtils.convertServerDateToUserTimezone(gcMessage.getCreatedDateTime().toLocalDateTime(), timeZone));
            HashMap<String, String> accountDetails = accountIdToDetails.get(gcMessage.getPostedByAccountId());
            if (accountDetails != null) {
                entityMessageResponse.setLastMessageFromEmail(accountDetails.get("email"));
                entityMessageResponse.setLastMessageFromFullName(accountDetails.get("fullName"));
            }
            entityMessageResponse.setAttachments(attachmentsByGcId.getOrDefault(gcMessage.getGroupConversationId(), Collections.emptyList()));

            Long parentGcId = gcMessage.getParentGroupConversationId();
            if (parentGcId != null) {
                ParentMsgInfo parentMsgInfo = new ParentMsgInfo();
                GroupConversation tempGc = combinedMessagesMap.get(parentGcId);
                BeanUtils.copyProperties(tempGc, parentMsgInfo);
                if (tempGc != null) {
                    BeanUtils.copyProperties(tempGc, parentMsgInfo);
                    List<AttachmentMetadata> parentAttachments = attachmentsByGcId.get(parentGcId);
                    if (parentAttachments != null && !parentAttachments.isEmpty()) {
                        // need to check if we are selecting the first file in order
                        parentMsgInfo.setAttachmentName(parentAttachments.get(0).getFileName());
                    }
                    if (accountDetails != null) {
                        parentMsgInfo.setLastMessageFromEmail(accountDetails.get("email"));
                        parentMsgInfo.setLastMessageFromFullName(accountDetails.get("fullName"));
                    }
                    entityMessageResponse.setParentMsgInfo(parentMsgInfo);
                }
            }

            List<Long> childGcIds = new ArrayList<>(gcMessage.getChildGcIds());
            if (childGcIds != null && !childGcIds.isEmpty()) {
                List<ReplyMsgInfo> replyMsgInfoList = new ArrayList<>();
                for (Long childGcId : childGcIds) {
                    ReplyMsgInfo replyMsgInfo = new ReplyMsgInfo();
                    GroupConversation tempGc = combinedMessagesMap.get(childGcId);
                    if (tempGc != null) {
                        BeanUtils.copyProperties(tempGc, replyMsgInfo);
                        List<AttachmentMetadata> childAttachments = attachmentsByGcId.get(childGcId);
                        if (childAttachments != null && !childAttachments.isEmpty()) {
                            replyMsgInfo.setAttachmentName(childAttachments.get(0).getFileName());
                        }
                        if (accountDetails != null) {
                            replyMsgInfo.setLastMessageFromEmail(accountDetails.get("email"));
                            replyMsgInfo.setLastMessageFromFullName(accountDetails.get("fullName"));
                        }
                        replyMsgInfoList.add(replyMsgInfo);
                    }
                }
                entityMessageResponse.setReplyMsgInfoList(replyMsgInfoList);
            }

            entityMessageResponseList.add(entityMessageResponse);
        }

        return entityMessageResponseList;
    }
}
