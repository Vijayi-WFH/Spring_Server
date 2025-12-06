package com.tse.core_application.repository;

import com.tse.core_application.model.Notification;
import com.tse.core_application.model.NotificationView;
import com.tse.core_application.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

public interface NotificationViewRepository extends JpaRepository<NotificationView,Long> {


    @Modifying
//    @Transactional
    @Query("update NotificationView n set n.isRead =true where n.notificationId = :notificationId AND n.accountId= :account")
    int setIsReadTrueByNotificationIdAndAccountId(Notification notificationId, UserAccount account);


    @Query("select n.isRead from NotificationView n where n.notificationId = :notificationId")
    String findIsReadByNotificationId(Notification notificationId);

    @Query("select n from NotificationView n where n.accountId = :accountId")
    List<NotificationView> findNotificationByAccountId(UserAccount accountId);

    @Query("select n from NotificationView n where n.accountId = :accountId and n.notificationId.notificationId > :notificationId")
    List<NotificationView> findNotificationsAfterGivenId(UserAccount accountId, Long notificationId);

    NotificationView findByNotificationIdAndAccountId(Notification notification, UserAccount account);

    @Modifying
//    @Transactional
    @Query("update NotificationView n set n.isRead =true where n.accountId= :account")
    void setIsReadTrueByAccountId(UserAccount account);

    @Modifying
//    @Transactional
    @Query("delete from NotificationView n where n.notificationId = :notification AND n.accountId in :accountIdList")
    void removeByNotificationIdAndAccountId(Notification notification, List<UserAccount> accountIdList);

    @Modifying
//    @Transactional
    @Query("delete from NotificationView n where n.accountId in :account")
    int removeByAccountId(List<UserAccount> account);

    @Modifying
//    @Transactional
    @Query("delete from NotificationView n where n.notificationId = :notification")
    void removeByNotificationId(Notification notification);

    @Modifying
    @Transactional
    @Query("DELETE FROM NotificationView nv WHERE nv.accountId IN (SELECT ua FROM UserAccount ua WHERE ua.accountId IN :accountIds)")
    void deleteByAccountIdIn(List<Long> accountIds);
}
