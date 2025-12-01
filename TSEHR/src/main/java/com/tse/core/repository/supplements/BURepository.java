package com.tse.core.repository.supplements;

import com.tse.core.model.supplements.BU;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BURepository extends JpaRepository<BU, Long> {

    Long findOrgIdByBuId(Long buId);

    @Query("select bu.buId from BU bu where bu.orgId = :id")
    List<Long> findBuIdsByOrgId(Long id);
}
