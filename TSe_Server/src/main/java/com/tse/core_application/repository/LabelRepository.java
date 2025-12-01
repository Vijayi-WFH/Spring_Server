package com.tse.core_application.repository;

import com.tse.core_application.dto.label.EntityTypeLabelResponse;
import com.tse.core_application.dto.label.LabelResponse;
import com.tse.core_application.model.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LabelRepository extends JpaRepository<Label, Long> {

    Label findByLabelNameIgnoreCaseAndEntityTypeIdAndEntityId(String labelName, Integer entityTypeId, Long entityId);

    @Query("SELECT new com.tse.core_application.dto.label.LabelResponse(l.labelId, l.labelName, t.teamCode) FROM Label l JOIN Team t ON l.entityId = t.teamId WHERE l.entityId IN :entityIdList")
    List<LabelResponse> findLabelInfoInTeam(List<Long> entityIdList);

    @Query("SELECT new com.tse.core_application.dto.label.EntityTypeLabelResponse(l.labelId, l.labelName) " +
            "FROM Label l WHERE l.entityTypeId = :entityTypeId AND l.entityId IN :entityIdList")
    List<EntityTypeLabelResponse> findLabelInfoByEntityTypeAndEntityList(
            @Param("entityTypeId") Integer entityTypeId,
            @Param("entityIdList") List<Long> entityIdList
    );

}

