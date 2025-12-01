package com.tse.core_application.repository;

import com.tse.core_application.model.DashboardButtons;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DashboardButtonRepository extends JpaRepository<DashboardButtons, Integer> {
}
