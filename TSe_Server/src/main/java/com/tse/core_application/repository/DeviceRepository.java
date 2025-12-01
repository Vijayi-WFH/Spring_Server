package com.tse.core_application.repository;

import com.tse.core_application.model.Device;
//import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {

    Device findTopByUserIdOrderByCreatedDateTimeDesc(Long userId);

}
