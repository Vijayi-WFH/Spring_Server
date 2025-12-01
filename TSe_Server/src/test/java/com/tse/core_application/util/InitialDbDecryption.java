// package com.tse.core_application.util;

// import com.tse.core_application.configuration.DataEncryptionConverter;
// import com.tse.core_application.model.User;
// import com.tse.core_application.model.*;
// import com.tse.core_application.repository.*;
// import org.junit.Test;
// import org.junit.runner.RunWith;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.test.context.junit4.SpringRunner;

// import java.util.List;

// // comment this file after all test cases are successful

// @RunWith(SpringRunner.class)
// @SpringBootTest
// public class InitialDbDecryption {

//     @Autowired
//     TaskRepository taskRepository;

//     @Autowired
//     AuditRepository auditRepository;

//     @Autowired
//     BURepository buRepository;

//     @Autowired
//     CommentRepository commentRepository;

//     @Autowired
//     DeliverablesDeliveredRepository deliverablesDeliveredRepository;

//     @Autowired
//     DeliverablesDeliveredHistoryRepository deliverablesDeliveredHistoryRepository;

//     @Autowired
//     MeetingRepository meetingRepository;

//     @Autowired
//     NoteRepository noteRepository;

//     @Autowired
//     NoteHistoryRepository noteHistoryRepository;

//     @Autowired
//     NotificationRepository notificationRepository;

//     @Autowired
//     OrganizationRepository organizationRepository;

//     @Autowired
//     ProjectRepository projectRepository;

//     @Autowired
//     StickyNoteRepository stickyNoteRepository;

//     @Autowired
//     UserAccountRepository userAccountRepository;

//     @Autowired
//     TaskHistoryRepository taskHistoryRepository;

//     @Autowired
//     TaskMediaRepository taskMediaRepository;

//     @Autowired
//     TeamRepository teamRepository;

//     @Autowired
//     TimeSheetRepository timeSheetRepository;

//     @Autowired
//     UserRepository userRepository;

//     @Autowired
//     TaskAttachmentRepository taskAttachmentRepository;


//     // comment the size condition on task_title, task_desc and uncomment after test is run
//     @Test
//     public void decryptTaskData() {
//         DataEncryptionConverter converter = new DataEncryptionConverter();

//         List<Task> tasks = taskRepository.findAll();
//         for (Task task : tasks) {
//             task.setTaskTitle((String) converter.convertToEntityAttribute(task.getTaskTitle()));
//             task.setTaskDesc((String)converter.convertToEntityAttribute((String) task.getTaskDesc()));
//             task.setAcceptanceCriteria((String)converter.convertToEntityAttribute(task.getAcceptanceCriteria()));
//             task.setKeyDecisions((String)converter.convertToEntityAttribute(task.getKeyDecisions()));
//             task.setParkingLot((String)converter.convertToEntityAttribute(task.getParkingLot()));
//         }
//         taskRepository.saveAll(tasks);
//     }

//     @Test
//     public void decryptAudit(){
//         DataEncryptionConverter converter = new DataEncryptionConverter();
//         List<Audit> audits = auditRepository.findAll();
//         for (Audit audit : audits) {
//             audit.setMessageForUser((String) converter.convertToEntityAttribute(audit.getMessageForUser()));
//         }
//         auditRepository.saveAll(audits);
//     }

//     @Test
//     public void decryptBU(){
//         DataEncryptionConverter converter = new DataEncryptionConverter();
//         List<BU> BUs = buRepository.findAll();
//         for (BU bu : BUs) {
//             bu.setBuName((String) converter.convertToEntityAttribute(bu.getBuName()));
//         }
//         buRepository.saveAll(BUs);
//     }

//     @Test
//     public void decryptDeliverablesDelivered(){
//         DataEncryptionConverter converter = new DataEncryptionConverter();
//         List<DeliverablesDelivered> deliverablesDelivereds = deliverablesDeliveredRepository.findAll();
//         for (DeliverablesDelivered deliverablesDelivered : deliverablesDelivereds) {
//             deliverablesDelivered.setDeliverablesDelivered((String) converter.convertToEntityAttribute(deliverablesDelivered.getDeliverablesDelivered()));
//         }
//         deliverablesDeliveredRepository.saveAll(deliverablesDelivereds);
//     }

//     @Test
//     public void decryptDeliverablesDeliveredHistory(){
//         DataEncryptionConverter converter = new DataEncryptionConverter();
//         List<DeliverablesDeliveredHistory> deliverablesDeliveredHistories = deliverablesDeliveredHistoryRepository.findAll();
//         for (DeliverablesDeliveredHistory deliverablesDeliveredHistory : deliverablesDeliveredHistories) {
//             deliverablesDeliveredHistory.setDeliverablesDelivered((String) converter.convertToEntityAttribute(deliverablesDeliveredHistory.getDeliverablesDelivered()));
//         }
//         deliverablesDeliveredHistoryRepository.saveAll(deliverablesDeliveredHistories);
//     }

//     @Test
//     public void decryptMeeting(){
//         DataEncryptionConverter converter = new DataEncryptionConverter();
//         List<Meeting> meetings = meetingRepository.findAll();
//         for (Meeting meeting : meetings) {
//             meeting.setAgenda((String) converter.convertToEntityAttribute(meeting.getAgenda()));
//             meeting.setVenue((String) converter.convertToEntityAttribute(meeting.getVenue()));
//             meeting.setMeetingKey((String) converter.convertToEntityAttribute(meeting.getMeetingKey()));
//         }
//         meetingRepository.saveAll(meetings);
//     }

//     @Test
//     public void decryptNote(){
//         DataEncryptionConverter converter = new DataEncryptionConverter();
//         List<Note> notes = noteRepository.findAll();
//         for (Note note : notes) {
//             note.setNote((String) converter.convertToEntityAttribute(note.getNote()));
//         }
//         noteRepository.saveAll(notes);
//     }

//     @Test
//     public void decryptNoteHistory(){
//         DataEncryptionConverter converter = new DataEncryptionConverter();
//         List<NoteHistory> noteHistories = noteHistoryRepository.findAll();
//         for (NoteHistory noteHistory : noteHistories) {
//             noteHistory.setNote((String) converter.convertToEntityAttribute(noteHistory.getNote()));
//         }
//         noteHistoryRepository.saveAll(noteHistories);
//     }

//     @Test
//     public void decryptNotification(){
//         DataEncryptionConverter converter = new DataEncryptionConverter();
//         List<Notification> notifications = notificationRepository.findAll();
//         for (Notification notification : notifications) {
//             notification.setNotificationTitle((String) converter.convertToEntityAttribute(notification.getNotificationTitle()));
//             notification.setNotificationBody((String) converter.convertToEntityAttribute(notification.getNotificationBody()));
//             notification.setPayload((String) converter.convertToEntityAttribute(notification.getPayload()));
//         }
//         notificationRepository.saveAll(notifications);
//     }

//     @Test
//     public void decryptOrganization(){
//         DataEncryptionConverter converter = new DataEncryptionConverter();
//         List<Organization> organizations = organizationRepository.findAll();
//         for (Organization organization : organizations) {
//             organization.setOrganizationName((String) converter.convertToEntityAttribute(organization.getOrganizationName()));
//             organization.setOrganizationDisplayName((String) converter.convertToEntityAttribute(organization.getOrganizationDisplayName()));
//         }
//         organizationRepository.saveAll(organizations);
//     }

//     @Test
//     public void decryptProject(){
//         DataEncryptionConverter converter = new DataEncryptionConverter();
//         List<Project> projects = projectRepository.findAll();
//         for (Project project : projects) {
//             project.setProjectName((String) converter.convertToEntityAttribute(project.getProjectName()));
//         }
//         projectRepository.saveAll(projects);
//     }

//     @Test
//     public void decryptStickyNote(){
//         DataEncryptionConverter converter = new DataEncryptionConverter();
//         List<StickyNote> stickyNotes = stickyNoteRepository.findAll();
//         for (StickyNote stickyNote : stickyNotes) {
//             stickyNote.setNote((String) converter.convertToEntityAttribute(stickyNote.getNote()));
//         }
//         stickyNoteRepository.saveAll(stickyNotes);
//     }

//     @Test
//     public void decryptUserAccount(){
//         DataEncryptionConverter converter = new DataEncryptionConverter();
//         List<UserAccount> userAccounts = userAccountRepository.findAll();
//         for (UserAccount userAccount : userAccounts) {
//             userAccount.setEmail((String) converter.convertToEntityAttribute(userAccount.getEmail()));
//         }
//         userAccountRepository.saveAll(userAccounts);
//     }

//     @Test
//     public void decryptTaskHistory() {
//         DataEncryptionConverter converter = new DataEncryptionConverter();

//         List<TaskHistory> taskHistories = taskHistoryRepository.findAll();
//         for (TaskHistory taskHistory : taskHistories) {
//             taskHistory.setTaskTitle((String) converter.convertToEntityAttribute(taskHistory.getTaskTitle()));
//             taskHistory.setTaskDesc((String) converter.convertToEntityAttribute(taskHistory.getTaskDesc()));
//             taskHistory.setAcceptanceCriteria((String) converter.convertToEntityAttribute(taskHistory.getAcceptanceCriteria()));
//             taskHistory.setKeyDecisions((String) converter.convertToEntityAttribute(taskHistory.getKeyDecisions()));
//             taskHistory.setParkingLot((String) converter.convertToEntityAttribute(taskHistory.getParkingLot()));
//         }
//         taskHistoryRepository.saveAll(taskHistories);
//     }

//     @Test
//     public void decryptTaskMedia() {
//         DataEncryptionConverter converter = new DataEncryptionConverter();

//         List<TaskMedia> taskMedias = taskMediaRepository.findAll();
//         for (TaskMedia taskMedia : taskMedias) {
//             taskMedia.setFileName((String) converter.convertToEntityAttribute(taskMedia.getFileName()));
//         }
//         taskMediaRepository.saveAll(taskMedias);
//     }

//     @Test
//     public void decryptTeam() {
//         DataEncryptionConverter converter = new DataEncryptionConverter();

//         List<Team> teams = teamRepository.findAll();
//         for (Team team : teams) {
//             team.setTeamName((String) converter.convertToEntityAttribute(team.getTeamName()));
//             team.setTeamDesc((String) converter.convertToEntityAttribute(team.getTeamDesc()));
//             team.setChatRoomName((String) converter.convertToEntityAttribute(team.getChatRoomName()));
//         }
//         teamRepository.saveAll(teams);
//     }

//     @Test
//     public void decryptTimeSheet() {
//         DataEncryptionConverter converter = new DataEncryptionConverter();

//         List<TimeSheet> timeSheets = timeSheetRepository.findAll();
//         for (TimeSheet timeSheet : timeSheets) {
//             timeSheet.setEntityTitle((String) converter.convertToEntityAttribute(timeSheet.getEntityTitle()));
//             timeSheet.setReferenceEntityTitle((String) converter.convertToEntityAttribute(timeSheet.getReferenceEntityTitle()));
//         }
//         timeSheetRepository.saveAll(timeSheets);
//     }

//     @Test
//     public void decryptTaskAttachment() {
//         DataEncryptionConverter converter = new DataEncryptionConverter();

//         List<TaskAttachment> taskAttachments = taskAttachmentRepository.findAll();
//         for (TaskAttachment taskAttachment : taskAttachments) {
//             taskAttachment.setFileName((String) converter.convertToEntityAttribute(taskAttachment.getFileName()));
//         }
//         taskAttachmentRepository.saveAll(taskAttachments);
//     }

//     @Test
//     public void decryptUser() {
//         DataEncryptionConverter converter = new DataEncryptionConverter();

//         List<User> users = userRepository.findAll();
//         for (User user : users) {
//             user.setPrimaryEmail((String) converter.convertToEntityAttribute(user.getPrimaryEmail()));
//             user.setAlternateEmail((String) converter.convertToEntityAttribute(user.getAlternateEmail()));
//             user.setPersonalEmail((String) converter.convertToEntityAttribute(user.getPersonalEmail()));
//             user.setCurrentOrgEmail((String) converter.convertToEntityAttribute(user.getCurrentOrgEmail()));
//             user.setGivenName((String) converter.convertToEntityAttribute(user.getGivenName()));
//             user.setFirstName((String) converter.convertToEntityAttribute(user.getFirstName()));
//             user.setLastName((String) converter.convertToEntityAttribute(user.getLastName()));
//             user.setMiddleName((String) converter.convertToEntityAttribute(user.getMiddleName()));
//             user.setLocale((String) converter.convertToEntityAttribute(user.getLocale()));
//             user.setCity((String) converter.convertToEntityAttribute(user.getCity()));
//             user.setChatUserName((String) converter.convertToEntityAttribute(user.getChatUserName()));
//             user.setChatPassword((String) converter.convertToEntityAttribute(user.getChatPassword()));
//         }
//         userRepository.saveAll(users);
//     }


// }
