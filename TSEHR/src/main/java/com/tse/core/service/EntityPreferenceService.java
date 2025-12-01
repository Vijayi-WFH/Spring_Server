package com.tse.core.service;

import com.tse.core.model.Constants;
import com.tse.core.model.supplements.EntityPreference;
import com.tse.core.repository.leaves.LeaveApplicationRepository;
import com.tse.core.repository.supplements.EntityPreferenceRepository;
import com.tse.core.utils.DateTimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
public class EntityPreferenceService {

    @Autowired
    private EntityPreferenceRepository entityPreferenceRepository;

    public Boolean getIsMonthlyLeaveUpdateOnProRata (Long teamId, Long orgId) {
        if (teamId != null) {
            Optional<EntityPreference> teamPreferenceOptional = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.TEAM, teamId);
            if (teamPreferenceOptional.isPresent() && teamPreferenceOptional.get().getIsMonthlyLeaveUpdateOnProRata() != null) {
                return teamPreferenceOptional.get().getIsMonthlyLeaveUpdateOnProRata();
            }
        }
        if (orgId != null) {
            Optional<EntityPreference> orgPreferenceOptional = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, orgId);
            if (orgPreferenceOptional.isPresent() && orgPreferenceOptional.get().getIsMonthlyLeaveUpdateOnProRata() != null) {
                return orgPreferenceOptional.get().getIsMonthlyLeaveUpdateOnProRata();
            }
        }
        return true;
    }

    public Boolean getIsYearlyLeaveUpdateOnProRata (Long teamId, Long orgId) {
        if (teamId != null) {
            Optional<EntityPreference> teamPreferenceOptional = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.TEAM, teamId);
            if (teamPreferenceOptional.isPresent() && teamPreferenceOptional.get().getIsYearlyLeaveUpdateOnProRata() != null) {
                return teamPreferenceOptional.get().getIsYearlyLeaveUpdateOnProRata();
            }
        }
        if (orgId != null) {
            Optional<EntityPreference> orgPreferenceOptional = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, orgId);
            if (orgPreferenceOptional.isPresent() && orgPreferenceOptional.get().getIsYearlyLeaveUpdateOnProRata() != null) {
                return orgPreferenceOptional.get().getIsYearlyLeaveUpdateOnProRata();
            }
        }
        return true;
    }

    public LocalTime getLeaveRequesterCancelTime (Long orgId, String timeZone) {

        Optional<EntityPreference> orgPreferenceOptional = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, orgId);
        if (orgPreferenceOptional.isPresent() && orgPreferenceOptional.get().getLeaveRequesterCancelTime() != null) {
            return DateTimeUtils.convertLocalTimeToServerTimeZone(orgPreferenceOptional.get().getLeaveRequesterCancelTime(), timeZone);
        }

        return Constants.defaultLeaveRequestorCancelTime;
    }

    public Integer getLeaveRequesterCancelDate (Long orgId) {

        Optional<EntityPreference> orgPreferenceOptional = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, orgId);
        if (orgPreferenceOptional.isPresent() && orgPreferenceOptional.get().getLeaveRequesterCancelDate() != null) {
            return orgPreferenceOptional.get().getLeaveRequesterCancelDate();
        }

        return Constants.LeaveRequesterDate.FROMDATE.getTypeId();
    }

    public List<Integer> getOffDaysByOrgId(Long orgId) {
        Optional<EntityPreference> orgPreferenceOptional = entityPreferenceRepository.findByEntityTypeIdAndEntityId(Constants.EntityTypes.ORG, orgId);
        if (orgPreferenceOptional.isPresent() && orgPreferenceOptional.get().getOffDays() != null) {
            return orgPreferenceOptional.get().getOffDays();
        }

        return Constants.defaultOffDays;
    }

    public EntityPreference fetchEntityPreference(Integer entityTypeId, Long entityId) {
        Optional<EntityPreference> entityPreferenceOptional = entityPreferenceRepository.findByEntityTypeIdAndEntityId(entityTypeId, entityId);
        return entityPreferenceOptional.orElse(null);
    }

    public Boolean validateWeekendDate (Long orgId, LocalDate date) {
        EntityPreference orgPreference = fetchEntityPreference(Constants.EntityTypes.ORG, orgId);
        if (orgPreference != null && orgPreference.getOffDays() != null && !orgPreference.getOffDays().isEmpty()) {
            List<String> offDays = orgPreference.getOffDays().stream()
                    .map(dayInt -> DayOfWeek.of(dayInt).name()).collect(Collectors.toList());
            return offDays != null && !offDays.isEmpty() && offDays.contains(date.getDayOfWeek().toString());
        }
        return false;
    }
}
