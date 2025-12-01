package com.tse.core_application.utils;

import com.tse.core_application.dto.PushNotificationRequest;
import com.tse.core_application.dto.UserPreferenceDTO;
import com.tse.core_application.exception.FirebaseNotificationException;
import com.tse.core_application.model.FirebaseToken;
import com.tse.core_application.model.UserAccount;
import com.tse.core_application.repository.UserAccountRepository;
import com.tse.core_application.service.Impl.FCMService;
import com.tse.core_application.service.Impl.FirebaseTokenService;
import com.tse.core_application.service.Impl.UserPreferenceService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public class FCMNotificationUtil {

    @Autowired
    private FirebaseTokenService firebaseTokenService;

    @Autowired
    private FCMService fcmService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private UserPreferenceService userPreferenceService;

    private static final Logger logger = LogManager.getLogger(FCMNotificationUtil.class.getName());

    /**
     * method to send FCM Notification. This is helper method for sendPushNotification method.
     */
    public void sendFcmNotification(Long userId, HashMap<String, String> payload) {
        List<FirebaseToken> firebaseTokenListDb = firebaseTokenService.getFirebaseTokenByUserId(userId);

        if (firebaseTokenListDb != null) {
            for (FirebaseToken firebaseToken : firebaseTokenListDb) {
                PushNotificationRequest pushNotificationRequest = new PushNotificationRequest();
                pushNotificationRequest.setPayload(payload);
                pushNotificationRequest.setTargetToken(firebaseToken);
                pushNotificationRequest.setDeviceType(firebaseToken.getDeviceType());

                try {
                    String messageSentResponse = fcmService.sendMessageToToken(pushNotificationRequest);
                } catch (Exception e) {
                    throw new FirebaseNotificationException("Error sending fcm notification");
                }
            }
        }
    }

    /**
     * This method sends push notification for addTask/ updateTask to different accounts in the list of payload.
     * It uses sendFCMNotification method to send push notification.
     *
     * @param payloadList
     */
    public boolean sendPushNotification(List<HashMap<String, String>> payloadList) {
        if (payloadList != null && !payloadList.isEmpty()) {
            for (HashMap<String, String> payload : payloadList) {
                Long accountIdOfUser = Long.parseLong(payload.get("accountId"));
                UserAccount userAccount = userAccountRepository.findByAccountIdAndIsActive(accountIdOfUser, true);
                if (userAccount == null) {
                    continue;
                }
                Long userIdOfUser = userAccount.getFkUserId().getUserId();
                UserPreferenceDTO userPreferenceDTO = userPreferenceService.getUserPreference(userIdOfUser);
                List<Integer> notificationCategoryIds = new ArrayList<>();
                if (userPreferenceDTO != null && userPreferenceDTO.getNotificationCategoryIds() != null && !userPreferenceDTO.getNotificationCategoryIds().isEmpty()) {
                    notificationCategoryIds = userPreferenceDTO.getNotificationCategoryIds();
                }
                Integer categoryIdInPayload = payload.get("categoryId") != null ? Integer.parseInt(payload.get("categoryId")) : 0;
                if (notificationCategoryIds.contains(categoryIdInPayload)) {
                    sendFcmNotification(userIdOfUser, payload);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean sendConversationPushNotification(List<HashMap<String, String>> payloadList, String timezone) {
        if (payloadList != null && !payloadList.isEmpty()) {
            for (HashMap<String, String> payload : payloadList) {
                Long accountIdOfUser = Long.parseLong(payload.get("accountId"));
                UserAccount userAccount = userAccountRepository.findByAccountIdAndIsActive(accountIdOfUser, true);
                if (userAccount == null) {
                    continue;
                }
                Long userIdOfUser = userAccount.getFkUserId().getUserId();
                sendFcmNotification(userIdOfUser, payload);
            }
            return true;
        }
        return false;
    }
}
