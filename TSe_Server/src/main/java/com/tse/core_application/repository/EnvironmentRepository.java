package com.tse.core_application.repository;

import com.tse.core_application.custom.model.EnvironmentIdDescDisplayAs;
import com.tse.core_application.custom.model.PriorityIdDescDisplayAs;
import com.tse.core_application.model.Environment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnvironmentRepository extends JpaRepository<Environment, Integer> {

   // @Query("select NEW com.tse.core_application.custom.model.EnvironmentIdDescDisplayAs(e.environmentId, e.environmentDescription, e.environmentDisplayName) from Environment e")
  //  List<EnvironmentIdDescDisplayAs> getEnvironmentIdDescDisplayAs();

}