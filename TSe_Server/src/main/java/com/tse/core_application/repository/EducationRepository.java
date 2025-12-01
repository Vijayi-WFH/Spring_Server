package com.tse.core_application.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tse.core_application.model.Education;

@Repository
public interface EducationRepository extends JpaRepository<Education, Integer> {

}
