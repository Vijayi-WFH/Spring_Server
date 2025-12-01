package com.tse.core_application.repository;

import com.tse.core_application.model.ScrollTo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ScrollToRepository extends JpaRepository<ScrollTo,Long> {

    @Query("select s.scrollToId from ScrollTo s where s.scrollToTitle=:scrollto")
    Long findScrollToIdByScrollToTitle(String scrollto);
}
