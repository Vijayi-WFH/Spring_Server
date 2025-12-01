package com.tse.core_application.repository;

import com.tse.core_application.custom.model.SeverityIdDescDisplayAs;
import com.tse.core_application.model.Severity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeverityRepository extends JpaRepository<Severity, Integer> {

    @Query("select NEW com.tse.core_application.custom.model.SeverityIdDescDisplayAs(s.severityId, s.severityDescription, s.severityDisplayName) from Severity s")
    List<SeverityIdDescDisplayAs> getSeverityIdDescDisplayAs();

}