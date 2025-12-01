package com.tse.core_application.repository;

import com.tse.core_application.model.ExceptionalRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExceptionalRegistrationRepository extends JpaRepository<ExceptionalRegistration, Long> {
    Boolean existsByEmailAndIsDeleted (String email, Boolean isDeleted);

    ExceptionalRegistration findByExceptionalRegistrationId(Long registrationId);

    List<ExceptionalRegistration> findByIsDeleted (Boolean isDeleted);

    ExceptionalRegistration findByEmailAndIsDeleted (String email, Boolean isDeleted);
}
