package com.tse.core_application.repository;

import com.tse.core_application.model.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationTypeRepository extends JpaRepository<NotificationType,Long> {

    NotificationType findByNotificationType(String notificationType);

    @Query("SELECT n FROM NotificationType n WHERE n.notificationType = :notificationType")
    List<NotificationType> findNotificationTypeIdByNotificationType(@Param("notificationType") String notificationType);
}
