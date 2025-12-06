package com.tse.core_application.repository;

import com.tse.core_application.model.FirebaseToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface FirebaseTokenRepository extends JpaRepository<FirebaseToken, Long> {

    FirebaseToken findByDeviceTypeAndDeviceId(String deviceType, String deviceId);

    public boolean existsFirebaseTokenByUserIdAndDeviceType(Long userId, String deviceType);

    public List<FirebaseToken> findByUserId(Long userId);

    public FirebaseToken findByUserIdAndDeviceType(Long userId, String deviceType);

    @Modifying
    @Transactional
    @Query("DELETE FROM FirebaseToken ft WHERE ft.userId IN :accountIds")
    void deleteByAccountIdIn(List<Long> accountIds);

}
