package com.tse.core_application.repository;

import com.tse.core_application.model.personal_task.PersonalTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface PersonalTaskRepository extends JpaRepository<PersonalTask, Long>, JpaSpecificationExecutor<PersonalTask> {

//    @Query(value = "SELECT nextval('tse.personal_task_number_seq')", nativeQuery = true)
//    Long getMaxTaskNumber();

    PersonalTask findByPersonalTaskNumber(Long taskNumber);

    PersonalTask findByPersonalTaskIdentifierAndFkAccountIdAccountId(Long personalTaskIdentifier, Long accountId);

    @Modifying
    @Query("update PersonalTask pt set pt.attachments = :attachments where pt.personalTaskId = :taskId")
    Integer setTaskAttachmentsByTaskId(String attachments, Long taskId);

    List<PersonalTask> findByFkAccountIdAccountIdAndCurrentlyScheduledTaskIndicator(Long accountId, Boolean currentlyScheduledTaskIndicator);

    @Query("SELECT DISTINCT t FROM PersonalTask t WHERE t.fkAccountId.accountId IN :accountIds AND (t.taskProgressSystem = 'DELAYED' OR (t.taskExpEndDate = :expectedEndDate AND t.taskProgressSystem NOT IN ('COMPLETED')) OR t.taskProgressSystem = 'WATCHLIST')")
    List<PersonalTask> findTaskListForTodayFocus (List<Long> accountIds, LocalDateTime expectedEndDate);

    @Modifying
    @Transactional
    @Query("DELETE FROM PersonalTask pt WHERE pt.fkAccountId.accountId IN :accountIds")
    void deleteByAccountIdIn(List<Long> accountIds);

    @Query("SELECT pt.personalTaskId FROM PersonalTask pt WHERE pt.fkAccountId.accountId IN :accountIds")
    List<Long> findAllPersonalTaskIdsByAccountIds(List<Long> accountIds);
}
