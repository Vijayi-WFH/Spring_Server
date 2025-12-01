package com.tse.core_application.service.Impl;

import com.tse.core_application.dto.FirebaseTokenDTO;
import com.tse.core_application.exception.UserDoesNotExistException;
import com.tse.core_application.model.FirebaseToken;
import com.tse.core_application.model.User;
import com.tse.core_application.repository.FirebaseTokenRepository;
import com.tse.core_application.service.IFirebaseTokenService;
import com.tse.core_application.utils.DateTimeUtils;
import com.tse.core_application.utils.JWTUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FirebaseTokenService implements IFirebaseTokenService {

    @Autowired
    private FirebaseTokenRepository firebaseTokenRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private JWTUtil jwtUtil;

    @Override
    public FirebaseToken addFirebaseToken(String token, FirebaseTokenDTO firebaseTokenDTO) {
        String username = jwtUtil.getUsernameFromToken(token);
        User foundUserDb = userService.getUserByUserName(username);
        if (foundUserDb != null) {

            FirebaseToken isFirebaseTokenPresent = firebaseTokenRepository.findByUserIdAndDeviceType(foundUserDb.getUserId(), firebaseTokenDTO.getDeviceType());

            if(isFirebaseTokenPresent != null){
                isFirebaseTokenPresent.setToken(firebaseTokenDTO.getToken());
                isFirebaseTokenPresent.setDeviceId(firebaseTokenDTO.getDeviceId());
                isFirebaseTokenPresent.setTimestamp(firebaseTokenDTO.getTimestamp());
                return firebaseTokenRepository.save(isFirebaseTokenPresent);
            } else{
                FirebaseToken firebaseToken = new FirebaseToken(foundUserDb.getUserId(), firebaseTokenDTO.getToken(), firebaseTokenDTO.getDeviceType(),
                        firebaseTokenDTO.getDeviceId(), firebaseTokenDTO.getTimestamp());
                return firebaseTokenRepository.save(firebaseToken);
            }
        } else {
            throw new UserDoesNotExistException();
        }
    }

    @Override
    public boolean validateFirebaseToken(Long userId, String deviceType) {
        return firebaseTokenRepository.existsFirebaseTokenByUserIdAndDeviceType(userId, deviceType);
    }

    @Override
    public String getFirebaseToken(Long userId, String deviceType) {
        FirebaseToken firebaseTokenFoundDb = firebaseTokenRepository.findByUserIdAndDeviceType(userId, deviceType);
        if(firebaseTokenFoundDb==null){
            return null;
        }
        return firebaseTokenFoundDb.getToken();
    }

    @Override
    public List<FirebaseToken> getFirebaseTokenByUserId(Long userId) {
        return firebaseTokenRepository.findByUserId(userId);
    }

    /**
     * This method is used to convert the local timestamp of the token into the server timezone.
     * i.e. the timestamp of the token which is in local timezone(as per user timezone) will be converted into
     * the timezone as per the server timezone.
     *
     * @param firebaseTokenDTO the firebaseTokenDTO object.
     * @param localTimeZone the user's timezone.
     */
    @Transactional(readOnly = true)
    public void convertTokenTimestampInToServerTimezone(FirebaseTokenDTO firebaseTokenDTO, String localTimeZone) {
        if (firebaseTokenDTO != null) {
            LocalDateTime convertedDateTime = DateTimeUtils.convertUserDateToServerTimezone(firebaseTokenDTO.getTimestamp(), localTimeZone);
            firebaseTokenDTO.setTimestamp(convertedDateTime);
        }
    }

}
