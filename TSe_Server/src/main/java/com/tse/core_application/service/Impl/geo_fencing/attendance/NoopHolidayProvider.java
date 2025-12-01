package com.tse.core_application.service.Impl.geo_fencing.attendance;

import com.tse.core_application.model.Constants;
import com.tse.core_application.model.EntityPreference;
import com.tse.core_application.repository.EntityPreferenceRepository;
import com.tse.core_application.repository.LeaveApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Phase 6b: Stub implementation that always returns false (no holidays).
 */
@Component
public class NoopHolidayProvider implements HolidayProvider {

    @Autowired
    private EntityPreferenceRepository entityPreferenceRepository;

    @Autowired
    private LeaveApplicationRepository leaveApplicationRepository;

    @Override
    public boolean isHoliday(long orgId, LocalDate date, Long accountId) {
        // ORG-level weekends & public holidays
        EntityPreference ep = entityPreferenceRepository
                .findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, orgId)
                .orElse(null);

        if (ep != null) {
            // Weekend/off-days: store is typically 1..7 (Mon..Sun). Use ints directly.
            List<Integer> offDays = ep.getOffDays();
            if (offDays != null && offDays.contains(date.getDayOfWeek().getValue())) {
                return true;
            }

            // Public holidays on exact date and active
            if (ep.getHolidayOffDays() != null) {
                boolean publicHoliday = ep.getHolidayOffDays().stream()
                        .anyMatch(h -> h.isActive() && date.equals(h.getDate()));
                if (publicHoliday) {
                    return true;
                }
            }
        }

        // Personal leave
        if (accountId != null) {
            boolean onLeave = leaveApplicationRepository.existsByAccountIdsAndDate(
                    Collections.singletonList(accountId),
                    date,
                    List.of(
                            Constants.LeaveApplicationStatusIds.APPROVED_LEAVE_APPLICATION_STATUS_ID,
                            Constants.LeaveApplicationStatusIds.CONSUMED_LEAVE_APPLICATION_STATUS_ID
                    )
            );
            if (onLeave) {
                return true;
            }
        }

        return false;
    }

}
