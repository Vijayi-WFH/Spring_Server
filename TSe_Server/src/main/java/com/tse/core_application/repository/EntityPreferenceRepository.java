package com.tse.core_application.repository;

import com.tse.core_application.custom.model.LeaveTypeAlias;
import com.tse.core_application.model.EntityPreference;
import com.tse.core_application.model.HolidayOffDay;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EntityPreferenceRepository extends JpaRepository<EntityPreference, Long> {

    Optional<EntityPreference> findByEntityTypeIdAndEntityId(Integer entityTypeId, Long entityId);

    @Query("SELECT DISTINCT ep FROM EntityPreference ep JOIN HolidayOffDay hd On ep.entityPreferenceId = hd.entityPreference where ep.entityTypeId = :entityTypeId")
    List<EntityPreference> findByHolidayOffDayNotNullAndEntityTypeId(@Param("entityTypeId") Integer entityTypeId);

    @Query("SELECT DISTINCT h.date FROM EntityPreference ep JOIN HolidayOffDay h ON ep.entityPreferenceId = h.entityPreference WHERE ep.entityTypeId = :entityTypeId AND ep.entityId = :entityId")
    List<LocalDate> getListOfHolidayDatesByEntityTypeIdAndEntityId(Integer entityTypeId, Long entityId);

    List<EntityPreference> findByEntityTypeIdAndEntityIdIn(Integer entityTypeId, List<Long> entityIds);

    @Query("SELECT NEW com.tse.core_application.custom.model.LeaveTypeAlias(ep.timeOffAlias, ep.sickLeaveAlias) FROM EntityPreference ep WHERE ep.entityTypeId = :entityTypeId AND ep.entityId = :entityId")
    LeaveTypeAlias findLeaveTypeAliasForEntity(Integer entityTypeId, Long entityId);

    @Query("SELECT ep.entityId FROM EntityPreference ep WHERE ep.entityTypeId = :entityTypeId AND ep.entityId IN :entityIdList AND ep.shouldOtpSendToOrgAdmin = :shouldOtpSendToOrgAdmin")
    List<Long> findEntityIdsByEntityTypeIdAndEntityIdInAndShouldOtpSendToOrgAdmin(Integer entityTypeId, List<Long> entityIdList, Boolean shouldOtpSendToOrgAdmin);

    @Query("SELECT ep.entityId FROM EntityPreference ep WHERE ep.entityTypeId = :entityTypeId AND ep.entityId IN :entityIdList AND ep.shouldInviteLinkSendToOrgAdmin = :shouldInviteLinkSendToOrgAdmin")
    List<Long> findEntityIdsByEntityTypeIdAndEntityIdInAndShouldInviteLinkSendToOrgAdmin(Integer entityTypeId, List<Long> entityIdList, Boolean shouldInviteLinkSendToOrgAdmin);

    @Query("SELECT CASE WHEN COUNT(ep) > 0 THEN true ELSE false END FROM EntityPreference ep WHERE ep.entityTypeId = :entityTypeId AND ep.entityId IN :entityIdList AND ep.shouldOtpSendToOrgAdmin = :shouldOtpSendToOrgAdmin")
    Boolean existsByEntityTypeIdAndEntityIdInAndShouldOtpSendToOrgAdmin(Integer entityTypeId, List<Long> entityIdList, Boolean shouldOtpSendToOrgAdmin);

    @Query("SELECT DISTINCT h FROM EntityPreference ep JOIN HolidayOffDay h ON ep.entityPreferenceId = h.entityPreference WHERE ep.entityTypeId = :entityTypeId AND ep.entityId = :entityId")
    Optional<List<HolidayOffDay>> fetchAllHolidayDatesByEntityTypeIdAndEntityId(Integer entityTypeId, Long entityId);

    @Query("SELECT CASE WHEN COUNT(ep) > 0 THEN true ELSE false END FROM EntityPreference ep " +
            "WHERE ep.entityTypeId = :entityTypeId AND ep.entityId = :entityId " +
            "AND ep.isGeoFencingAllowed = true AND ep.isGeoFencingActive = true")
    boolean isGeoFencingEnabledForEntity(@org.springframework.data.repository.query.Param("entityTypeId") Integer entityTypeId,
                                         @org.springframework.data.repository.query.Param("entityId") Long entityId);

    @Query("SELECT ep.entityId FROM EntityPreference ep WHERE ep.entityTypeId = :entityTypeId AND ep.entityId IN :entityIdList AND ep.isGeoFencingAllowed = :isGeoFencingAllowed AND ep.isGeoFencingActive = :isGeoFencingActive")
    List<Long> findEntityIdsByEntityTypeIdAndEntityIdInAndIsGeoFencingAllowedAndIsGeoFencingActive(Integer entityTypeId, List<Long> entityIdList, Boolean isGeoFencingAllowed, Boolean isGeoFencingActive);
}
