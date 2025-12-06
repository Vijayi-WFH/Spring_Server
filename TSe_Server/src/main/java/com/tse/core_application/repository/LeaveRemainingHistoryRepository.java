package com.tse.core_application.repository;

import com.tse.core_application.model.LeaveRemainingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface LeaveRemainingHistoryRepository extends JpaRepository<LeaveRemainingHistory, Long> {

    @Modifying
    @Transactional
    @Query("DELETE FROM LeaveRemainingHistory lrh WHERE lrh.accountId IN :accountIds")
    void deleteByAccountIdIn(List<Long> accountIds);
}
