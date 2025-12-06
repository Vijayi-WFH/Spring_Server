package com.tse.core_application.repository;

import com.tse.core_application.model.Audit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface AuditRepository extends JpaRepository<Audit, Long> {

    @Modifying
    @Transactional
    @Query("DELETE FROM Audit a WHERE a.createdBy IN :accountIds")
    void deleteByAccountIdIn(List<Long> accountIds);
}
