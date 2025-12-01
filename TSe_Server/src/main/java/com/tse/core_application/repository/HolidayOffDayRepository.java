package com.tse.core_application.repository;

import com.tse.core_application.dto.HolidayResponse;
import com.tse.core_application.model.HolidayOffDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HolidayOffDayRepository extends JpaRepository<HolidayOffDay, Long> {

    @Query("SELECT new com.tse.core_application.dto.HolidayResponse(h.holidayId, h.date, h.isRecurring, h.description) FROM HolidayOffDay h WHERE h.entityPreference.entityPreferenceId = :entityPreferenceId AND h.isActive = :isActive")
    List<HolidayResponse> findCustomHolidayResponseByEntityPreferenceIdAndIsActive(@Param("entityPreferenceId") Long entityPreferenceId, @Param("isActive") Boolean isActive);

    @Query("SELECT h FROM HolidayOffDay h WHERE h.entityPreference.entityPreferenceId = :entityPreferenceId AND h.isActive = :isActive")
    List<HolidayOffDay> findAllByEntityPreferenceIdAndIsActive(@Param("entityPreferenceId") Long entityPreferenceId, @Param("isActive") Boolean isActive);
}
