package com.tse.core_application.repository;

import com.tse.core_application.custom.model.PriorityIdDescDisplayAs;
import com.tse.core_application.custom.model.ResolutionIdDescDisplayAs;
import com.tse.core_application.model.Resolution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResolutionRepository extends JpaRepository<Resolution, Integer> {

    @Query("select NEW com.tse.core_application.custom.model.ResolutionIdDescDisplayAs(r.resolutionId, r.resolutionDescription, r.resolutionDisplayName) from Resolution r")
    List<ResolutionIdDescDisplayAs> getResolutionIdDescDisplayAs();


}
