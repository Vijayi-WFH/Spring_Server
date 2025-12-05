package com.tse.core_application.repository;

import com.tse.core_application.model.CalendarDays;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CalendarDaysRepository extends JpaRepository<CalendarDays, Integer> {

    @Query("SELECT cd.previousBusinessDay FROM CalendarDays cd WHERE cd.calendarDate = :currentDate")
    LocalDate findPreviousBusinessDayByDate(@Param("currentDate") LocalDate currentDate);

    @Query("select c.isBusinessDay from CalendarDays c where c.calendarDate =:toDate")
    Boolean findIsBusinessDayByCalendarDate(LocalDate toDate);

    @Modifying
    @Transactional
    @Query("DELETE FROM CalendarDays cd WHERE cd.orgId = :orgId")
    void deleteByOrgId(Long orgId);

}
