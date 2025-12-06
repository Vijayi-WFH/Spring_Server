package com.tse.core_application.repository;

import com.tse.core_application.model.MemberDetails;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface MemberDetailsRepository extends JpaRepository<MemberDetails, Long> {

    MemberDetails findByEntityTypeIdAndEntityIdAndAccountId(Integer entityTypeId, Long entityId, Long accountId);

    List<MemberDetails> findByEntityTypeIdAndEntityIdAndAccountIdIn(Integer entityTypeId, Long entityId, List<Long> accountIds);

    List<MemberDetails> findByEntityTypeIdAndEntityId(Integer entityTypeId, Long entityId);

    @Modifying
    @Query("UPDATE MemberDetails m SET m.workMinutes = :workMinutes WHERE m.accountId IN :accountIds")
    void updateWorkMinutesForAccountIdIn(@Param("workMinutes") Integer workMinutes, @Param("accountIds") List<Long> accountIds);

    @Modifying
    @Transactional
    @Query("DELETE FROM MemberDetails md WHERE md.accountId IN :accountIds")
    void deleteByAccountIdIn(List<Long> accountIds);

}
