package com.tse.core_application.repository;

import com.tse.core_application.model.NotificationCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationCategoryRepository extends JpaRepository<NotificationCategory, Integer> {

    @Query("select nc.categoryId from NotificationCategory nc where nc.categoryType = 'System'")
    List<Integer> findSystemLevelCategoryIds();

    @Query("SELECT nc FROM NotificationCategory nc WHERE nc.categoryType = 'System' OR nc.categoryType = 'User'")
    List<NotificationCategory> findUserAndSystemLevelCategoryIds();

}
