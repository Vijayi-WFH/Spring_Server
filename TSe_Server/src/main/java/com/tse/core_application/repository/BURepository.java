package com.tse.core_application.repository;

import com.tse.core_application.custom.model.BuIdAndBuName;
import com.tse.core_application.custom.model.OrgId;
import com.tse.core_application.model.BU;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface BURepository extends JpaRepository<BU, Long> {

    @Query("SELECT b FROM BU b WHERE b.buId = :buId AND b.isDisabled = false")
    BU findByBuId(Long buId);

    @Query("SELECT b FROM BU b WHERE b.orgId = :orgId AND b.isDisabled = false")
    List<BU> findByOrgId(Long orgId);

    @Query("SELECT b FROM BU b WHERE b.buId IN :buIds AND b.isDisabled = false")
    List<BU> findByBuIdIn(List<Long> buIds);

    OrgId findOrgIdBybuId(Long buId);
    @Query("select b.buName from BU b where b.buId = :buId AND b.isDisabled = false")
    String findBuNameByBuId(Long buId);

    /**
     * This method will find all the BU for the given list of orgIds.
     *
     * @param orgIds the list of orgIds.
     * @return List<BU>
     */
    @Query("SELECT b FROM BU b WHERE b.orgId IN :orgIds AND b.isDisabled = false")
    List<BU> findByOrgIdIn(List<Long> orgIds);

    @Query("SELECT NEW com.tse.core_application.custom.model.BuIdAndBuName(b.buId, b.buName) FROM BU b WHERE b.orgId = :orgId AND b.isDisabled = false")
    List<BuIdAndBuName> findBuIdAndBuNameByOrgId(Long orgId);

    @Modifying
    @Transactional
    @Query("update BU b set b.isDisabled = :isDisabled where b.orgId = :orgId")
    void updateIsDisabledByOrgId (Long orgId, Boolean isDisabled);

    @Query("SELECT count(b) FROM BU b WHERE b.orgId = :orgId")
    Integer findBuCountByOrgId (Long orgId);
}
