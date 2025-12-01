package com.tse.core_application.repository;

import com.tse.core_application.model.BusinessDays;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface BusinessDaysRepository extends JpaRepository<BusinessDays, Long> {

    public BusinessDays findByCurrDate(LocalDate date);
}
