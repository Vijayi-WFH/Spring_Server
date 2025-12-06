package com.tse.core_application.repository;

import com.tse.core_application.custom.model.ListOfDeliverablesDeliveredId;
import com.tse.core_application.model.DeliverablesDelivered;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface DeliverablesDeliveredRepository extends JpaRepository<DeliverablesDelivered, Long> {

    @Query(value = "select new com.tse.core_application.custom.model.ListOfDeliverablesDeliveredId (max(n.listOfDeliverablesDeliveredId)) from DeliverablesDelivered n")
    ListOfDeliverablesDeliveredId getMaxListOfDeliverablesDeliveredId();

    DeliverablesDelivered findByDeliverablesDeliveredLogId(Long deliverablesDeliveredLogId);

    @Modifying
    @Query("update DeliverablesDelivered n set n.isDeleted = :isDeleted where n.deliverablesDeliveredLogId IN (:deliverablesDeliveredLogId)")
    Integer setIsDeletedByDeliverablesDeliveredLogIdIn(List<Long> deliverablesDeliveredLogId, Integer isDeleted);

    @Modifying
    @Transactional
    @Query("DELETE FROM DeliverablesDelivered dd WHERE dd.task.taskId IN :taskIds")
    void deleteByTaskIdIn(List<Long> taskIds);

}
