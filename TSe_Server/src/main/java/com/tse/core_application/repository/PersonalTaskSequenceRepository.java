package com.tse.core_application.repository;

import com.tse.core_application.model.PersonalTaskSequence;
import com.tse.core_application.model.TaskSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.LockModeType;
import java.util.List;

@Repository
public interface PersonalTaskSequenceRepository extends JpaRepository<PersonalTaskSequence, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pts FROM PersonalTaskSequence pts WHERE pts.accountId = :accountId")
    PersonalTaskSequence findByAccountIdForUpdate(@Param("accountId") Long accountId);

    @Modifying
    @Transactional
    @Query("DELETE FROM PersonalTaskSequence pts WHERE pts.accountId IN :accountIds")
    void deleteByAccountIdIn(List<Long> accountIds);
}
