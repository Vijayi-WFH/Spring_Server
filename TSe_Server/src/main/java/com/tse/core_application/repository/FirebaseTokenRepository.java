package com.tse.core_application.repository;

import com.tse.core_application.model.FirebaseToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FirebaseTokenRepository extends JpaRepository<FirebaseToken, Long> {

    FirebaseToken findByDeviceTypeAndDeviceId(String deviceType, String deviceId);

    public boolean existsFirebaseTokenByUserIdAndDeviceType(Long userId, String deviceType);

    public List<FirebaseToken> findByUserId(Long userId);

    public FirebaseToken findByUserIdAndDeviceType(Long userId, String deviceType);

}
