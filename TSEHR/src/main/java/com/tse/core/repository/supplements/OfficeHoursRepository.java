package com.tse.core.repository.supplements;

import com.tse.core.model.supplements.OfficeHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OfficeHoursRepository extends JpaRepository<OfficeHours, Long> {

    OfficeHours findByKeyAndWorkflowTypeId(String key, Integer workflowTypeId);
}
