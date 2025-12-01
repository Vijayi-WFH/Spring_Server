package com.tse.core_application.service.Impl.geo_fencing.attendance;

import java.time.LocalTime;

/**
 * Phase 6b: Interface for retrieving office hours policy.
 * Stub implementation for now.
 */
public interface OfficePolicyProvider {

    /**
     * Get office start time for the organization.
     *
     * @param orgId Organization ID
     * @return Office start time (local time)
     */
    LocalTime getOfficeStartTime(long orgId);

    /**
     * Get office end time for the organization.
     *
     * @param orgId Organization ID
     * @return Office end time (local time)
     */
    LocalTime getOfficeEndTime(long orgId);

    /**
     * Get operational timezone for the organization.
     *
     * @param orgId Organization ID
     * @return Timezone ID (e.g., "America/New_York")
     */
    String getOperationalTimezone(long orgId);
}
