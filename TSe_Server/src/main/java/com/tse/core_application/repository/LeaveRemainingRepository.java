package com.tse.core_application.repository;

import com.tse.core_application.model.LeaveRemaining;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LeaveRemainingRepository extends JpaRepository<LeaveRemaining,Long> {

    @Query("select lr from LeaveRemaining lr where lr.currentlyActive=:bool")
    List<LeaveRemaining> findByCurrentlyActive(boolean bool);

}
