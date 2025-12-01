package com.tse.core_application.repository;

import com.tse.core_application.custom.model.EntityTypeId;
import com.tse.core_application.custom.model.EntityTypeInEntityType;
import com.tse.core_application.model.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EntityTypeRepository extends JpaRepository<EntityType, Integer> {

    // find entityType from EntityType by entityTypeId
    public EntityTypeInEntityType findEntityTypeByEntityTypeId(Integer entityTypeId);

    EntityTypeId findEntityTypeIdByEntityType(String entityType);

    EntityType findByEntityType(String entityType);



}
