package com.tse.core_application.repository;

import com.tse.core_application.model.OfficeHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface OfficeHoursRepository extends JpaRepository<OfficeHours, Long> {

    OfficeHours findByKeyAndWorkflowTypeId(String key, Integer workflowTypeId);

    //TODO: select orgId where office hours is not over yet according to timezone
//    @Query(value = "select org_id from tse.office_hours o where workflow_type_id = 3 and key like 'EOD_TIME' and " +
//            "value between (CURRENT_TIME+ make_interval(mins \\:= 14))" +
//            "and (CURRENT_TIME + make_interval(mins \\:= 15))",nativeQuery = true)
//    List<Long> findOrgIdAccordingToOfficeHoursForPreTimeSheetReminder();

    //TODO: select orgId where office hours is over according to timezone
//    @Query(value = "select org_id from tse.office_hours o where workflow_type_id = 3 and key like 'EOD_TIME' and " +
//            "value < (CURRENT_TIME)",nativeQuery = true)
//    List<Long> findOrgIdAccordingToOfficeHoursForPostTimeSheetReminder();

    @Modifying
    @Transactional
    @Query("DELETE FROM OfficeHours oh WHERE oh.orgId = :orgId")
    void deleteByOrgId(Long orgId);
}
