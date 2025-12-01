 package com.tse.core_application.controller;

 import com.tse.core_application.dto.EncryptDecryptValueRequest;
 import com.tse.core_application.exception.InternalServerErrorException;
 import com.tse.core_application.handlers.CustomResponseHandler;
 import com.tse.core_application.handlers.RequestHeaderHandler;
 import com.tse.core_application.handlers.StackTraceHandler;
 import com.tse.core_application.model.*;
 import com.tse.core_application.repository.*;
 import com.tse.core_application.service.Impl.UserService;
 import com.tse.core_application.utils.CommonUtils;
 import com.tse.core_application.utils.EncryptionUtils;
 import com.tse.core_application.utils.JWTUtil;
 import org.apache.logging.log4j.LogManager;
 import org.apache.logging.log4j.Logger;
 import org.apache.logging.log4j.ThreadContext;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.beans.factory.annotation.Value;
 import org.springframework.http.HttpStatus;
 import org.springframework.http.ResponseEntity;
 import org.springframework.transaction.annotation.Transactional;
 import org.springframework.web.bind.annotation.*;

 import javax.servlet.http.HttpServletRequest;
 import java.util.ArrayList;
 import java.util.List;

 @CrossOrigin(value = "*")
 @RestController
 @RequestMapping(path = "/converter")
 public class EncryptionController {

     private static final Logger logger = LogManager.getLogger(EncryptionController.class.getName());

     @Autowired
     TaskRepository taskRepository;

     @Autowired
     AuditRepository auditRepository;

     @Autowired
     BURepository buRepository;

     @Autowired
     CommentRepository commentRepository;

     @Autowired
     DeliverablesDeliveredRepository deliverablesDeliveredRepository;

     @Autowired
     DeliverablesDeliveredHistoryRepository deliverablesDeliveredHistoryRepository;

     @Autowired
     MeetingRepository meetingRepository;

     @Autowired
     NoteRepository noteRepository;

     @Autowired
     NoteHistoryRepository noteHistoryRepository;

     @Autowired
     NotificationRepository notificationRepository;

     @Autowired
     OrganizationRepository organizationRepository;

     @Autowired
     ProjectRepository projectRepository;

     @Autowired
     StickyNoteRepository stickyNoteRepository;

     @Autowired
     UserAccountRepository userAccountRepository;

     @Autowired
     TaskHistoryRepository taskHistoryRepository;

     @Autowired
     TaskMediaRepository taskMediaRepository;

     @Autowired
     TeamRepository teamRepository;

     @Autowired
     TimeSheetRepository timeSheetRepository;

     @Autowired
     UserRepository userRepository;

     @Autowired
     TaskAttachmentRepository taskAttachmentRepository;

     @Value("${app.developer.accountIds}")
     private String[] developerAccountIds;

     @Autowired
     private JWTUtil jwtUtil;

     @Autowired
     private RequestHeaderHandler requestHeaderHandler;

     @Autowired
     private UserService userService;

     @Autowired
     private EncryptionUtils encryptionUtils;


     @GetMapping(path = "/encryptData")
     @Transactional
     public void encryptData(@RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                                 @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
//
//         List<Task> tasks = taskRepository.findAll();
//         for (Task task : tasks) {
//             if (task.getImmediateAttentionFrom() != null) {
//                 task.setImmediateAttentionFrom(converter.convertToDatabaseColumn(task.getImmediateAttentionFrom()));
//             }
//         }
//         taskRepository.saveAll(tasks);
//
//         List<TaskHistory> taskHistories = taskHistoryRepository.findAll();
//         for (TaskHistory taskHistory : taskHistories) {
//             if (taskHistory.getImmediateAttentionFrom() != null) {
//                 taskHistory.setImmediateAttentionFrom(converter.convertToDatabaseColumn(taskHistory.getImmediateAttentionFrom()));
//             }
//         }
//         taskHistoryRepository.saveAll(taskHistories);
//
//
//         // --------------------------------------------------------- //
//
//         List<Audit> audits = auditRepository.findAll();
//         for (Audit audit : audits) {
//             audit.setMessageForUser(converter.convertToDatabaseColumn(audit.getMessageForUser()));
//         }
//         auditRepository.saveAll(audits);
//
//         // ----------------------------------------------------------- //
//
//         List<BU> BUs = buRepository.findAll();
//         for (BU bu : BUs) {
//             bu.setBuName(converter.convertToDatabaseColumn(bu.getBuName()));
//         }
//         buRepository.saveAll(BUs);
//
//         // ----------------------------------------------------------- //
//
//         List<DeliverablesDelivered> deliverablesDelivereds = deliverablesDeliveredRepository.findAll();
//         for (DeliverablesDelivered deliverablesDelivered : deliverablesDelivereds) {
//             deliverablesDelivered.setDeliverablesDelivered(converter.convertToDatabaseColumn(deliverablesDelivered.getDeliverablesDelivered()));
//         }
//         deliverablesDeliveredRepository.saveAll(deliverablesDelivereds);
//
//         // ----------------------------------------------------------- //
//
//         List<DeliverablesDeliveredHistory> deliverablesDeliveredHistories = deliverablesDeliveredHistoryRepository.findAll();
//         for (DeliverablesDeliveredHistory deliverablesDeliveredHistory : deliverablesDeliveredHistories) {
//             deliverablesDeliveredHistory.setDeliverablesDelivered(converter.convertToDatabaseColumn(deliverablesDeliveredHistory.getDeliverablesDelivered()));
//         }
//         deliverablesDeliveredHistoryRepository.saveAll(deliverablesDeliveredHistories);
//
//         // ----------------------------------------------------------- //
//
//         List<Meeting> meetings = meetingRepository.findAll();
//         for (Meeting meeting : meetings) {
//             meeting.setAgenda(converter.convertToDatabaseColumn(meeting.getAgenda()));
//             meeting.setVenue(converter.convertToDatabaseColumn(meeting.getVenue()));
//             meeting.setMeetingKey(converter.convertToDatabaseColumn(meeting.getMeetingKey()));
//         }
//         meetingRepository.saveAll(meetings);
//
//         // ----------------------------------------------------------- //
//
//         List<Note> notes = noteRepository.findAll();
//         for (Note note : notes) {
//             note.setNote(converter.convertToDatabaseColumn(note.getNote()));
//         }
//         noteRepository.saveAll(notes);
//
//         // ----------------------------------------------------------- //
//
//         List<NoteHistory> noteHistories = noteHistoryRepository.findAll();
//         for (NoteHistory noteHistory : noteHistories) {
//             noteHistory.setNote(converter.convertToDatabaseColumn(noteHistory.getNote()));
//         }
//         noteHistoryRepository.saveAll(noteHistories);
//
//         // ----------------------------------------------------------- //
//
//         List<Notification> notifications = notificationRepository.findAll();
//         for (Notification notification : notifications) {
//             notification.setNotificationTitle(converter.convertToDatabaseColumn(notification.getNotificationTitle()));
//             notification.setNotificationBody(converter.convertToDatabaseColumn(notification.getNotificationBody()));
//             notification.setPayload(converter.convertToDatabaseColumn(notification.getPayload()));
//         }
//         notificationRepository.saveAll(notifications);
//
//         // ----------------------------------------------------------- //
//
//         List<Organization> organizations = organizationRepository.findAll();
//         for (Organization organization : organizations) {
//             organization.setOrganizationName(converter.convertToDatabaseColumn(organization.getOrganizationName()));
//             organization.setOrganizationDisplayName(converter.convertToDatabaseColumn(organization.getOrganizationDisplayName()));
//         }
//         organizationRepository.saveAll(organizations);
//
//         // ----------------------------------------------------------- //
//
//         List<Project> projects = projectRepository.findAll();
//         for (Project project : projects) {
//             project.setProjectName(converter.convertToDatabaseColumn(project.getProjectName()));
//         }
//         projectRepository.saveAll(projects);
//
//         // ----------------------------------------------------------- //
//
//         List<StickyNote> stickyNotes = stickyNoteRepository.findAll();
//         for (StickyNote stickyNote : stickyNotes) {
//             stickyNote.setNote(converter.convertToDatabaseColumn(stickyNote.getNote()));
//         }
//         stickyNoteRepository.saveAll(stickyNotes);
//
//         // ----------------------------------------------------------- //
//
//         List<UserAccount> userAccounts = userAccountRepository.findAll();
//         for (UserAccount userAccount : userAccounts) {
//             userAccount.setEmail(converter.convertToDatabaseColumn(userAccount.getEmail()));
//         }
//         userAccountRepository.saveAll(userAccounts);
//
//         // ----------------------------------------------------------- //
//
//         List<TaskHistory> taskHistories = taskHistoryRepository.findAll();
//         for (TaskHistory taskHistory : taskHistories) {
//             taskHistory.setTaskTitle(converter.convertToDatabaseColumn(taskHistory.getTaskTitle()));
//             taskHistory.setTaskDesc(converter.convertToDatabaseColumn(taskHistory.getTaskDesc()));
//             taskHistory.setAcceptanceCriteria(converter.convertToDatabaseColumn(taskHistory.getAcceptanceCriteria()));
//             taskHistory.setKeyDecisions(converter.convertToDatabaseColumn(taskHistory.getKeyDecisions()));
//             taskHistory.setParkingLot(converter.convertToDatabaseColumn(taskHistory.getParkingLot()));
//         }
//         taskHistoryRepository.saveAll(taskHistories);
//
//         // ----------------------------------------------------------- //
//
//         List<TaskMedia> taskMedias = taskMediaRepository.findAll();
//         for (TaskMedia taskMedia : taskMedias) {
//             taskMedia.setFileName(converter.convertToDatabaseColumn(taskMedia.getFileName()));
//         }
//         taskMediaRepository.saveAll(taskMedias);
//
//         // ----------------------------------------------------------- //
//
//         List<Team> teams = teamRepository.findAll();
//         for (Team team : teams) {
//             team.setTeamName(converter.convertToDatabaseColumn(team.getTeamName()));
//             team.setTeamDesc(converter.convertToDatabaseColumn(team.getTeamDesc()));
//             team.setChatRoomName(converter.convertToDatabaseColumn(team.getChatRoomName()));
//         }
//         teamRepository.saveAll(teams);
//
//         // ----------------------------------------------------------- //
//
//         List<TimeSheet> timeSheets = timeSheetRepository.findAll();
//         for (TimeSheet timeSheet : timeSheets) {
//             timeSheet.setEntityTitle(converter.convertToDatabaseColumn(timeSheet.getEntityTitle()));
//             timeSheet.setReferenceEntityTitle(converter.convertToDatabaseColumn(timeSheet.getReferenceEntityTitle()));
//         }
//         timeSheetRepository.saveAll(timeSheets);
//
//         // ----------------------------------------------------------- //
//
//         List<TaskAttachment> taskAttachments = taskAttachmentRepository.findAll();
//         for (TaskAttachment taskAttachment : taskAttachments) {
//             taskAttachment.setFileName(converter.convertToDatabaseColumn(taskAttachment.getFileName()));
//         }
//         taskAttachmentRepository.saveAll(taskAttachments);
//
//         // ----------------------------------------------------------- //
//
//         List<User> users = userRepository.findAll();
//         for (User user : users) {
//             user.setPrimaryEmail(converter.convertToDatabaseColumn(user.getPrimaryEmail()));
//             user.setAlternateEmail(converter.convertToDatabaseColumn(user.getAlternateEmail()));
//             user.setPersonalEmail(converter.convertToDatabaseColumn(user.getPersonalEmail()));
//             user.setCurrentOrgEmail(converter.convertToDatabaseColumn(user.getCurrentOrgEmail()));
//             user.setGivenName(converter.convertToDatabaseColumn(user.getGivenName()));
//             user.setFirstName(converter.convertToDatabaseColumn(user.getFirstName()));
//             user.setLastName(converter.convertToDatabaseColumn(user.getLastName()));
//             user.setMiddleName(converter.convertToDatabaseColumn(user.getMiddleName()));
//             user.setLocale(converter.convertToDatabaseColumn(user.getLocale()));
//             user.setCity(converter.convertToDatabaseColumn(user.getCity()));
//             user.setChatUserName(converter.convertToDatabaseColumn(user.getChatUserName()));
//             user.setChatPassword(converter.convertToDatabaseColumn(user.getChatPassword()));
//         }
//         userRepository.saveAll(users);
//
//         // ----------------------------------------------------------- //
//
//         List<Comment> comments = commentRepository.findAll();
//         for (Comment comment : comments) {
//             comment.setComment(converter.convertToDatabaseColumn(comment.getComment()));
//         }
//         commentRepository.saveAll(comments);
//     }
//
//     @GetMapping(path = "/decryptData")
//     @Transactional
//     public void decryptData(@RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
//                             @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {
//
//         DataEncryptionConverter converter = new DataEncryptionConverter();
//
//         List<Task> tasks = taskRepository.findAll();
//         for (Task task : tasks) {
//             task.setTaskTitle((String) converter.convertToEntityAttribute(task.getTaskTitle()));
//         }
//         taskRepository.saveAll(tasks);
//
//
//         List<TaskHistory> taskHistories = taskHistoryRepository.findAll();
//         for (TaskHistory taskHistory : taskHistories) {
//             taskHistory.setTaskTitle((String) converter.convertToEntityAttribute(taskHistory.getTaskTitle()));
//         }
//         taskHistoryRepository.saveAll(taskHistories);
     }

     @GetMapping(path = "/decryptData")
     @Transactional
     public void decryptData(@RequestHeader(name = "screenName") String screenName, @RequestHeader(name = "timeZone") String timeZone,
                             @RequestHeader(name = "accountIds") String accountIds, HttpServletRequest request) {

//         DataEncryptionConverter converter = new DataEncryptionConverter();
//
//         List<Task> tasks = taskRepository.findAll();
//         for (Task task : tasks) {
//             if (task.getImmediateAttentionFrom() != null) {
//                 task.setImmediateAttentionFrom((String) converter.convertToEntityAttribute(task.getImmediateAttentionFrom()));
//             }
//         }
//         taskRepository.saveAll(tasks);

     }

     @PostMapping(path = "/encryptValues")
     public ResponseEntity<Object> encryptValues(@RequestBody EncryptDecryptValueRequest encryptDecryptValueRequest,
                                                   @RequestHeader(name = "screenName") String screenName,
                                                   @RequestHeader(name = "timeZone") String timeZone,
                                                   @RequestHeader(name = "accountIds") String accountIds,
                                                   HttpServletRequest request) {

         long startTime = System.currentTimeMillis();
         String jwtToken = request.getHeader("Authorization").substring(7);
         String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
         User foundUser = userService.getUserByUserName(tokenUsername);
         ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
         ThreadContext.put("userId", foundUser.getUserId().toString());
         ThreadContext.put("requestOriginatingPage", screenName);
         logger.info("Entering" + '"' + " encryptValues" + '"' + " method ...");
         List<String> convertedValues = new ArrayList<>();
         try {
             CommonUtils.validateDeveloperAccount(developerAccountIds, jwtUtil.getAllAccountIdsFromToken(jwtToken));
             convertedValues = encryptionUtils.getEncryptValues(encryptDecryptValueRequest.getValues());
             long estimatedTime = System.currentTimeMillis() - startTime;
             ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
             logger.info("Exited" + '"' + " encryptValues" + '"' + " method because completed successfully ...");
             ThreadContext.clearMap();
         } catch (Exception e) {
             String allStackTraces = StackTraceHandler.getAllStackTraces(e);
             logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute encryptValues" + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
             ThreadContext.clearMap();
             if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
         }
         return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, convertedValues);
     }

     @PostMapping(path = "/decryptValues")
     public ResponseEntity<Object> decryptValues(@RequestBody EncryptDecryptValueRequest encryptDecryptValueRequest,
                                                 @RequestHeader(name = "screenName") String screenName,
                                                 @RequestHeader(name = "timeZone") String timeZone,
                                                 @RequestHeader(name = "accountIds") String accountIds,
                                                 HttpServletRequest request) {

         long startTime = System.currentTimeMillis();
         String jwtToken = request.getHeader("Authorization").substring(7);
         String tokenUsername = jwtUtil.getUsernameFromToken(jwtToken);
         User foundUser = userService.getUserByUserName(tokenUsername);
         ThreadContext.put("accountId", requestHeaderHandler.getAccountIdFromRequestHeader(accountIds).toString());
         ThreadContext.put("userId", foundUser.getUserId().toString());
         ThreadContext.put("requestOriginatingPage", screenName);
         logger.info("Entering" + '"' + " decryptValues" + '"' + " method ...");
         List<String> convertedValues = new ArrayList<>();
         try {
             CommonUtils.validateDeveloperAccount(developerAccountIds, jwtUtil.getAllAccountIdsFromToken(jwtToken));
             convertedValues = encryptionUtils.getDecryptedValues(encryptDecryptValueRequest.getValues());
             long estimatedTime = System.currentTimeMillis() - startTime;
             ThreadContext.put("systemResponseTime", String.valueOf(estimatedTime));
             logger.info("Exited" + '"' + " decryptValues" + '"' + " method because completed successfully ...");
             ThreadContext.clearMap();
         } catch (Exception e) {
             String allStackTraces = StackTraceHandler.getAllStackTraces(e);
             logger.error(request.getRequestURI() + " API: " + "Something went wrong: Not able to execute decryptValues" + " ,    " + "Caught Exception: " + e, new Throwable(allStackTraces));
             ThreadContext.clearMap();
             if (e.getMessage()==null) throw new InternalServerErrorException("Internal Server Error!"); else throw e;
         }
         return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, com.tse.core_application.constants.Constants.FormattedResponse.SUCCESS, convertedValues);
     }
 }
