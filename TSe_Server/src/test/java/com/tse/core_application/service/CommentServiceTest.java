package com.tse.core_application.service;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.tse.core_application.dto.PushNotificationRequest;
import com.tse.core_application.model.*;
import com.tse.core_application.service.Impl.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import javax.persistence.EntityManager;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CommentServiceTest {

    @Mock
    private FirebaseTokenService firebaseTokenService;

    @Mock
    private FCMService fcmService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private UserAccountService userAccountService;

    @Mock
    private TaskServiceImpl taskServiceImpl;

    @Spy
    @InjectMocks
    private CommentService commentService;

    @Test
    public void testSendFcmNotification_retrievesCorrectFirebaseTokens() throws FirebaseMessagingException {
        Long userId = 1L;

        FirebaseToken firebaseToken = new FirebaseToken();
        firebaseToken.setToken("test_token");
        List<FirebaseToken> firebaseTokenList = List.of(firebaseToken);
        HashMap<String, String> payload = new HashMap<>();
        payload.put("accountId", "1");

        when(firebaseTokenService.getFirebaseTokenByUserId(userId)).thenReturn(firebaseTokenList);
        when(fcmService.sendMessageToToken(any(PushNotificationRequest.class))).thenReturn("dummy string");
        commentService.sendFcmNotification(userId, payload);

        verify(firebaseTokenService).getFirebaseTokenByUserId(userId);
        verify(fcmService).sendMessageToToken(any(PushNotificationRequest.class));
    }


    @Test(expected = Exception.class)
    public void testSendFcmNotification_handlesFirebaseMessagingException() throws FirebaseMessagingException {
        Long userId = 1L;
        HashMap<String, String> payload = new HashMap<>();
        FirebaseToken firebaseToken = new FirebaseToken();
        firebaseToken.setToken("test_token");
        List<FirebaseToken> firebaseTokenList = List.of(firebaseToken);
        when(fcmService.sendMessageToToken(any(PushNotificationRequest.class))).thenThrow(new Exception());
        commentService.sendFcmNotification(userId, payload);
    }

    @Test
    public void testsendNewCommentNotification_retrievesCorrectTaskAndUserAccounts() {
        Comment comment = new Comment();
        comment.setComment("Test comment");
        comment.setPostedByAccountId(5L);
        Task task = new Task();
        task.setTaskId(1L);
        task.setTaskNumber("1L");
        Team team = new Team();
        team.setTeamId(1L);
        task.setFkTeamId(team);
        comment.setTask(task);
        List<Long> taggedAccountIds = List.of(1L);
        when(entityManager.getReference(Task.class, 1L)).thenReturn(task);

        UserAccount userAccount1 = new UserAccount();
        userAccount1.setAccountId(1L);
        User user1 = new User();
        user1.setUserId(1L);
        userAccount1.setFkUserId(user1);

        UserAccount userAccount2 = new UserAccount();
        userAccount2.setAccountId(2L);
        User user2 = new User();
        user2.setUserId(2L);
        userAccount2.setFkUserId(user2);

        task.setFkAccountIdAssigned(userAccount1);
        task.setFkAccountIdMentor2(userAccount2);

        UserAccount userAccount5 = new UserAccount();
        User user = new User();
        user.setUserId(5L);
        userAccount5.setFkUserId(user);

        HashMap<String, String> dummyPayload = new HashMap<>();
        dummyPayload.put("accountId", "1");
        dummyPayload.put("notificationId", "1");
        dummyPayload.put("notificationType", "ADD_COMMENT");
        dummyPayload.put("createdDateTime", "2023-03-20T00:00:00");
        dummyPayload.put("title", "New Comment added for task # " + task.getTaskNumber());
        dummyPayload.put("body", "Test comment");
        dummyPayload.put("taskNumber", task.getTaskNumber().toString());
        dummyPayload.put("scrollTo", "100");

        when(userAccountService.getActiveUserAccountByAccountId(5L)).thenReturn(userAccount5);
        when(userAccountService.getActiveUserAccountByAccountId(1L)).thenReturn(userAccount1);
        when(userAccountService.getActiveUserAccountByAccountId(2L)).thenReturn(userAccount2);
        when(taskServiceImpl.getUserIdsOfHigherRoleMembersInTeam(anyLong())).thenReturn(null);
        commentService.sendNewCommentNotification(comment,String.valueOf(ZoneId.systemDefault()), taggedAccountIds);

        verify(entityManager).getReference(Task.class, 1L);
        verify(commentService).sendFcmNotification(1L, dummyPayload);
        verify(commentService).sendFcmNotification(2L, dummyPayload);

    }

}