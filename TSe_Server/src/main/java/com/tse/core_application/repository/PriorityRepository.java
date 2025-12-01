package com.tse.core_application.repository;

import com.tse.core_application.custom.model.PriorityIdDescDisplayAs;
import com.tse.core_application.model.Priority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PriorityRepository extends JpaRepository<Priority, Integer> {

    @Query("select NEW com.tse.core_application.custom.model.PriorityIdDescDisplayAs(p.priorityId, p.priorityDesc, p.priorityDisplayAs) from Priority p")
    List<PriorityIdDescDisplayAs> getPriorityIdDescDisplayAs();
}
