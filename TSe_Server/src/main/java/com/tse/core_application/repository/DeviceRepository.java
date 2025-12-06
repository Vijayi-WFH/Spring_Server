package com.tse.core_application.repository;

import com.tse.core_application.model.Device;
//import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {

    Device findTopByUserIdOrderByCreatedDateTimeDesc(Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Device d WHERE d.userId IN :accountIds")
    void deleteByAccountIdIn(List<Long> accountIds);

}
