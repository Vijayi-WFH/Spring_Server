package com.tse.core.repository.supplements;

import com.tse.core.custom.model.LeaveTypeAlias;
import com.tse.core.model.supplements.EntityPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EntityPreferenceRepository extends JpaRepository<EntityPreference, Long> {

    Optional<EntityPreference> findByEntityTypeIdAndEntityId(Integer entityTypeId, Long entityId);

    @Query("SELECT DISTINCT ep FROM EntityPreference ep JOIN HolidayOffDay hd On ep.entityPreferenceId = hd.entityPreference where ep.entityTypeId = :entityTypeId")
    List<EntityPreference> findByHolidayOffDayNotNullAndEntityTypeId(@Param("entityTypeId") Integer entityTypeId);

    @Query("SELECT NEW com.tse.core.custom.model.LeaveTypeAlias(ep.timeOffAlias, ep.sickLeaveAlias) FROM EntityPreference ep WHERE ep.entityTypeId = :entityTypeId AND ep.entityId = :entityId")
    LeaveTypeAlias findLeaveTypeAliasForEntity(Integer entityTypeId, Long entityId);

    @Query("SELECT ep.minApprovedSickDaysWithoutMedicalCert FROM EntityPreference ep WHERE ep.entityTypeId = :entityTypeId AND ep.entityId = :entityId")
    Integer findMinApprovedSickDaysWithoutMedicalCertByEntityTypeIdAndEntityId(Integer entityTypeId, Long entityId);
}
