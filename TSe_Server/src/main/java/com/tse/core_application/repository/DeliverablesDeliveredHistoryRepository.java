package com.tse.core_application.repository;

import com.tse.core_application.model.DeliverablesDeliveredHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface DeliverablesDeliveredHistoryRepository extends JpaRepository<DeliverablesDeliveredHistory, Long> {

    @Modifying
    @Transactional
    @Query("DELETE FROM DeliverablesDeliveredHistory ddh WHERE ddh.taskId IN :taskIds")
    void deleteByTaskIdIn(List<Long> taskIds);
}
