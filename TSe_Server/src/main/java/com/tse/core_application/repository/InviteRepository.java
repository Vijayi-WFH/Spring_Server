package com.tse.core_application.repository;

import com.tse.core_application.model.Invite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface InviteRepository extends JpaRepository<Invite, Long> {

    Optional<Invite> findByInviteIdAndIsRevoked(String inviteId, Boolean isRevoked);

    List<Invite> findByEntityTypeIdAndEntityId(Integer entityTypeId, Long entityId);

    List<Invite> findByPrimaryEmailAndIsRevoked(String email, Boolean isRevoked);

    List<Invite> findByPrimaryEmail(@Param("primary_email") String primaryEmail);

    @Modifying
    @Transactional
    @Query("DELETE FROM Invite i WHERE i.entityTypeId = :entityTypeId AND i.entityId IN :entityIds")
    void deleteByEntityTypeIdAndEntityIdIn(Integer entityTypeId, List<Long> entityIds);
}
