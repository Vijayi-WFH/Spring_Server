package com.tse.core_application.service.Impl;

import com.tse.core_application.model.NotificationCategory;
import com.tse.core_application.repository.NotificationCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationCategoryService {
    @Autowired
    private NotificationCategoryRepository repository;

    /** sends User & System Level Notification Categories */
    public List<NotificationCategory> getAllNotificationCategories() {
        return repository.findUserAndSystemLevelCategoryIds();
    }
}
