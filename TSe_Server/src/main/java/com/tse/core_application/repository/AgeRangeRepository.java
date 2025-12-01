package com.tse.core_application.repository;

import com.tse.core_application.model.AgeRange;
import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgeRangeRepository extends JpaRepository<AgeRange, Long> {

}
