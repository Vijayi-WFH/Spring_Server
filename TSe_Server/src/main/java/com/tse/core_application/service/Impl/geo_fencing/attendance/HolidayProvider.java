package com.tse.core_application.service.Impl.geo_fencing.attendance;

import java.time.LocalDate;

/**
 * Phase 6b: Interface for checking if a date is a holiday.
 * Stub implementation for now.
 */
public interface HolidayProvider {

    /**
     * Check if a given date is a holiday for the organization.
     *
     * @param orgId Organization ID
     * @param date  Date to check
     * @return true if holiday, false otherwise
     */
    boolean isHoliday(long orgId, LocalDate date, Long accountId);
}
