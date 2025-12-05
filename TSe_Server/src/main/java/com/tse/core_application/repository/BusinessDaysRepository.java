package com.tse.core_application.repository;

import com.tse.core_application.model.BusinessDays;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BusinessDaysRepository extends JpaRepository<BusinessDays, Long> {

    public BusinessDays findByCurrDate(LocalDate date);

    @Modifying
    @Transactional
    @Query("DELETE FROM BusinessDays bd WHERE bd.orgId = :orgId")
    void deleteByOrgId(Long orgId);
}
