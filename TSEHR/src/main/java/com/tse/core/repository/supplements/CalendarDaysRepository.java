package com.tse.core.repository.supplements;

import com.tse.core.model.supplements.CalendarDays;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface CalendarDaysRepository extends JpaRepository<CalendarDays, Integer> {


    @Query("select c.isBusinessDay from CalendarDays c where c.calendarDate =:toDate")
    Boolean findIsBusinessDayByCalendarDate(LocalDate toDate);
}
