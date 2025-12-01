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
// public class InitialDbEncryption {

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
//     public void encryptTask() {
//         DataEncryptionConverter converter = new DataEncryptionConverter();

//         List<Task> tasks = taskRepository.findAll();
//         for (Task task : tasks) {
//             task.setTaskTitle(converter.convertToDatabaseColumn(task.getTaskTitle()));
//             task.setTaskDesc(converter.convertToDatabaseColumn(task.getTaskDesc()));
//             task.setAcceptanceCriteria(converter.convertToDatabaseColumn(task.getAcceptanceCriteria()));
//             task.setKeyDecisions(converter.convertToDatabaseColumn(task.getKeyDecisions()));
//             task.setParkingLot(converter.convertToDatabaseColumn(task.getParkingLot()));
//         }
//         taskRepository.saveAll(tasks);
//     }

//     @Test
//     public void encryptAudit(){
//         DataEncryptionConverter converter = new DataEncryptionConverter();
//         List<Audit> audits = auditRepository.findAll();
//         for (Audit audit : audits) {
//             audit.setMessageForUser(converter.convertToDatabaseColumn(audit.getMessageForUser()));
//         }
//         auditRepository.saveAll(audits);
//     }

//     @Test
//     public void encryptBU(){
//         DataEncryptionConverter converter = new DataEncryptionConverter();
//         List<BU> BUs = buRepository.findAll();
//         for (BU bu : BUs) {
//             bu.setBuName(converter.convertToDatabaseColumn(bu.getBuName()));
//         }
//         buRepository.saveAll(BUs);
//     }

//     @Test
//     public void encryptDeliverablesDelivered(){
//         DataEncryptionConverter converter = new DataEncryptionConverter();
//         List<DeliverablesDelivered> deliverablesDelivereds = deliverablesDeliveredRepository.findAll();
//         for (DeliverablesDelivered deliverablesDelivered : deliverablesDelivereds) {
//             deliverablesDelivered.setDeliverablesDelivered(converter.convertToDatabaseColumn(deliverablesDelivered.getDeliverablesDelivered()));
//         }
//         deliverablesDeliveredRepository.saveAll(deliverablesDelivereds);
//     }

//     @Test
//     public void encryptDeliverablesDeliveredHistory(){
//         DataEncryptionConverter converter = new DataEncryptionConverter();
//         List<DeliverablesDeliveredHistory> deliverablesDeliveredHistories = deliverablesDeliveredHistoryRepository.findAll();
//         for (DeliverablesDeliveredHistory deliverablesDeliveredHistory : deliverablesDeliveredHistories) {
//             deliverablesDeliveredHistory.setDeliverablesDelivered(converter.convertToDatabaseColumn(deliverablesDeliveredHistory.getDeliverablesDelivered()));
//         }
//         deliverablesDeliveredHistoryRepository.saveAll(deliverablesDeliveredHistories);
//     }

//     @Test
//     public void encryptMeeting(){
//         DataEncryptionConverter converter = new DataEncryptionConverter();
//         List<Meeting> meetings = meetingRepository.findAll();
//         for (Meeting meeting : meetings) {
//             meeting.setAgenda(converter.convertToDatabaseColumn(meeting.getAgenda()));
//             meeting.setVenue(converter.convertToDatabaseColumn(meeting.getVenue()));
//             meeting.setMeetingKey(converter.convertToDatabaseColumn(meeting.getMeetingKey()));
//         }
//         meetingRepository.saveAll(meetings);
//     }

//     @Test
//     public void encryptNote(){
//         DataEncryptionConverter converter = new DataEncryptionConverter();
//         List<Note> notes = noteRepository.findAll();
//         for (Note note : notes) {
//             note.setNote(converter.convertToDatabaseColumn(note.getNote()));
//         }
//         noteRepository.saveAll(notes);
//     }

//     @Test
//     public void encryptNoteHistory(){
//         DataEncryptionConverter converter = new DataEncryptionConverter();
//         List<NoteHistory> noteHistories = noteHistoryRepository.findAll();
//         for (NoteHistory noteHistory : noteHistories) {
//             noteHistory.setNote(converter.convertToDatabaseColumn(noteHistory.getNote()));
//         }
//         noteHistoryRepository.saveAll(noteHistories);
//     }

//     @Test
//     public void encryptNotification(){
//         DataEncryptionConverter converter = new DataEncryptionConverter();
//         List<Notification> notifications = notificationRepository.findAll();
//         for (Notification notification : notifications) {
//             notification.setNotificationTitle(converter.convertToDatabaseColumn(notification.getNotificationTitle()));
//             notification.setNotificationBody(converter.convertToDatabaseColumn(notification.getNotificationBody()));
//             notification.setPayload(converter.convertToDatabaseColumn(notification.getPayload()));
//         }
//         notificationRepository.saveAll(notifications);
//     }

//     @Test
//     public void encryptOrganization(){
//         DataEncryptionConverter converter = new DataEncryptionConverter();
//         List<Organization> organizations = organizationRepository.findAll();
//         for (Organization organization : organizations) {
//             organization.setOrganizationName(converter.convertToDatabaseColumn(organization.getOrganizationName()));
//             organization.setOrganizationDisplayName(converter.convertToDatabaseColumn(organization.getOrganizationDisplayName()));
//         }
//         organizationRepository.saveAll(organizations);
//     }

//     @Test
//     public void encryptProject(){
//         DataEncryptionConverter converter = new DataEncryptionConverter();
//         List<Project> projects = projectRepository.findAll();
//         for (Project project : projects) {
//             project.setProjectName(converter.convertToDatabaseColumn(project.getProjectName()));
//         }
//         projectRepository.saveAll(projects);
//     }

//     @Test
//     public void encryptStickyNote(){
//         DataEncryptionConverter converter = new DataEncryptionConverter();
//         List<StickyNote> stickyNotes = stickyNoteRepository.findAll();
//         for (StickyNote stickyNote : stickyNotes) {
//             stickyNote.setNote(converter.convertToDatabaseColumn(stickyNote.getNote()));
//         }
//         stickyNoteRepository.saveAll(stickyNotes);
//     }

//     @Test
//     public void encryptUserAccount(){
//         DataEncryptionConverter converter = new DataEncryptionConverter();
//         List<UserAccount> userAccounts = userAccountRepository.findAll();
//         for (UserAccount userAccount : userAccounts) {
//             userAccount.setEmail(converter.convertToDatabaseColumn(userAccount.getEmail()));
//         }
//         userAccountRepository.saveAll(userAccounts);
//     }

//     @Test
//     public void encryptTaskHistory() {
//         DataEncryptionConverter converter = new DataEncryptionConverter();

//         List<TaskHistory> taskHistories = taskHistoryRepository.findAll();
//         for (TaskHistory taskHistory : taskHistories) {
//             taskHistory.setTaskTitle(converter.convertToDatabaseColumn(taskHistory.getTaskTitle()));
//             taskHistory.setTaskDesc(converter.convertToDatabaseColumn(taskHistory.getTaskDesc()));
//             taskHistory.setAcceptanceCriteria(converter.convertToDatabaseColumn(taskHistory.getAcceptanceCriteria()));
//             taskHistory.setKeyDecisions(converter.convertToDatabaseColumn(taskHistory.getKeyDecisions()));
//             taskHistory.setParkingLot(converter.convertToDatabaseColumn(taskHistory.getParkingLot()));
//         }
//         taskHistoryRepository.saveAll(taskHistories);
//     }

//     @Test
//     public void encryptTaskMedia() {
//         DataEncryptionConverter converter = new DataEncryptionConverter();

//         List<TaskMedia> taskMedias = taskMediaRepository.findAll();
//         for (TaskMedia taskMedia : taskMedias) {
//             taskMedia.setFileName(converter.convertToDatabaseColumn(taskMedia.getFileName()));
//         }
//         taskMediaRepository.saveAll(taskMedias);
//     }

//     @Test
//     public void encryptTeam() {
//         DataEncryptionConverter converter = new DataEncryptionConverter();

//         List<Team> teams = teamRepository.findAll();
//         for (Team team : teams) {
//             team.setTeamName(converter.convertToDatabaseColumn(team.getTeamName()));
//             team.setTeamDesc(converter.convertToDatabaseColumn(team.getTeamDesc()));
//             team.setChatRoomName(converter.convertToDatabaseColumn(team.getChatRoomName()));
//         }
//         teamRepository.saveAll(teams);
//     }

//     @Test
//     public void encryptTimeSheet() {
//         DataEncryptionConverter converter = new DataEncryptionConverter();

//         List<TimeSheet> timeSheets = timeSheetRepository.findAll();
//         for (TimeSheet timeSheet : timeSheets) {
//             timeSheet.setEntityTitle(converter.convertToDatabaseColumn(timeSheet.getEntityTitle()));
//             timeSheet.setReferenceEntityTitle(converter.convertToDatabaseColumn(timeSheet.getReferenceEntityTitle()));
//         }
//         timeSheetRepository.saveAll(timeSheets);
//     }

//     @Test
//     public void encryptTaskAttachment() {
//         DataEncryptionConverter converter = new DataEncryptionConverter();

//         List<TaskAttachment> taskAttachments = taskAttachmentRepository.findAll();
//         for (TaskAttachment taskAttachment : taskAttachments) {
//             taskAttachment.setFileName(converter.convertToDatabaseColumn(taskAttachment.getFileName()));
//         }
//         taskAttachmentRepository.saveAll(taskAttachments);
//     }

//     @Test
//     public void encryptUser() {
//         DataEncryptionConverter converter = new DataEncryptionConverter();

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
//     }

//     @Test
//     public void encryptComment() {
//         DataEncryptionConverter converter = new DataEncryptionConverter();

//         List<Comment> comments = commentRepository.findAll();
//         for (Comment comment : comments) {
//             comment.setComment(converter.convertToDatabaseColumn(comment.getComment()));
//         }
//         commentRepository.saveAll(comments);
//     }


// }
