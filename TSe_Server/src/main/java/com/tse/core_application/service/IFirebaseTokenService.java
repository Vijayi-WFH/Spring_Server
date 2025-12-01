package com.tse.core_application.service;

import com.tse.core_application.dto.FirebaseTokenDTO;
import com.tse.core_application.model.FirebaseToken;

import java.util.List;

public interface IFirebaseTokenService {

    FirebaseToken addFirebaseToken(String username, FirebaseTokenDTO firebaseTokenDTO);

//    boolean validateFirebaseToken(Long userId, String deviceType, String deviceId);

//    String getFirebaseToken(Long userId, String deviceType, String deviceId);

    boolean validateFirebaseToken(Long userId, String deviceType);

    String getFirebaseToken(Long userId, String deviceType);

    List<FirebaseToken> getFirebaseTokenByUserId(Long userId);
}
