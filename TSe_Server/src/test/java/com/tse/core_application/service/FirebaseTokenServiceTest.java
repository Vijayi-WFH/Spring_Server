package com.tse.core_application.service;

import com.tse.core_application.dto.FirebaseTokenDTO;
import com.tse.core_application.exception.UserDoesNotExistException;
import com.tse.core_application.model.FirebaseToken;
import com.tse.core_application.model.User;
import com.tse.core_application.repository.FirebaseTokenRepository;
import com.tse.core_application.service.Impl.FirebaseTokenService;
import com.tse.core_application.service.Impl.UserService;
import com.tse.core_application.utils.JWTUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;

import java.time.LocalDateTime;

@RunWith(MockitoJUnitRunner.class)
public class FirebaseTokenServiceTest {

    @InjectMocks
    private FirebaseTokenService firebaseTokenService;

    @Mock
    private JWTUtil jwtUtil;

    @Mock
    private UserService userService;

    @Mock
    private FirebaseTokenRepository firebaseTokenRepository;

    /**
     * Case - when the valid user exists and token is not already present
     * The method which is under test. @link #{addFirebaseToken(String token, FirebaseTokenDTO firebaseTokenDTO)}.
     */
    @Test
    public void testAddFirebaseToken_saveNewToken() {
        User foundUserDb = new User();
        FirebaseTokenDTO firebaseTokenDTO = new FirebaseTokenDTO("abc123@#", "laptop", "999", LocalDateTime.now());
        foundUserDb.setUserId(1L);

        lenient().when(jwtUtil.getUsernameFromToken("xyz648pqr")).thenReturn("user@gmail.com");
        lenient().when(userService.getUserByUserName("user@gmail.com")).thenReturn(foundUserDb);
        lenient().when(firebaseTokenRepository.findByUserIdAndDeviceType(anyLong(), anyString())).thenReturn(null);

        FirebaseToken firebaseTokenSaved = new FirebaseToken(1L, 1L, "abc123@#", "999", "laptop", LocalDateTime.now());
        FirebaseToken firebaseTokenToSave = new FirebaseToken(1l, "abc123@#", "laptop", "999", LocalDateTime.now());
        lenient().when(firebaseTokenRepository.save(firebaseTokenToSave)).thenReturn(firebaseTokenSaved);

        FirebaseToken actual = firebaseTokenService.addFirebaseToken("xyz648pqr", firebaseTokenDTO);
        Mockito.verify(firebaseTokenRepository, times(1)).save(Mockito.any(FirebaseToken.class));
    }

    /**
     *  Case - when the valid user Exists and token is already present
     */
    @Test
    public void testAddFirebaseToken_updateExistingToken() {
        User foundUserDb = new User();
        foundUserDb.setUserId(1L);
        FirebaseTokenDTO firebaseTokenDTO = new FirebaseTokenDTO("abc123@#", "laptop", "999", LocalDateTime.now());
        FirebaseToken isFirebaseTokenPresent = new FirebaseToken(1L, 1L, "123456", "laptop", "999", LocalDateTime.now());

        lenient().when(jwtUtil.getUsernameFromToken("xyz648pqr")).thenReturn("user@gmail.com");
        lenient().when(userService.getUserByUserName("user@gmail.com")).thenReturn(foundUserDb);
        lenient().when(firebaseTokenRepository.findByUserIdAndDeviceType(anyLong(), anyString())).thenReturn(isFirebaseTokenPresent);

        FirebaseToken firebaseTokenSaved = new FirebaseToken(1L, 1L, "abc123@#", "999", "laptop", LocalDateTime.now());
        lenient().when(firebaseTokenRepository.save(isFirebaseTokenPresent)).thenReturn(firebaseTokenSaved);

        FirebaseToken actual = firebaseTokenService.addFirebaseToken("xyz648pqr", firebaseTokenDTO);
        Mockito.verify(firebaseTokenRepository, times(1)).save(Mockito.any(FirebaseToken.class));
    }

    /**
     * Case - when the user is not found from the database.
     * The method which is under test. @link #{addFirebaseToken(String token, FirebaseTokenDTO firebaseTokenDTO)}.
     */
    @Test(expected = UserDoesNotExistException.class)
    public void testAddFirebaseToken_ElseCase1() {
        FirebaseTokenDTO firebaseTokenDTO = new FirebaseTokenDTO("abc123@#", "laptop", "999", LocalDateTime.now());
        lenient().when(jwtUtil.getUsernameFromToken("xyz648pqr")).thenReturn("user@gmail.com");
        lenient().when(userService.getUserByUserName("user@gmail.com")).thenReturn(null);
        FirebaseToken firebaseToken = new FirebaseToken(1L, 1L, "abc123@#", "999", "laptop", LocalDateTime.now());
        FirebaseToken actual = firebaseTokenService.addFirebaseToken("xyz648pqr", firebaseTokenDTO);
    }

    /**
     * Case - when all conditions are true. i.e. when token exists in the database.
     * The method which is under test. @link #{validateFirebaseToken(Long userId, String deviceType, String deviceId)}.
     */
    @Test
    public void testValidateFirebaseToken_Success() {
        Long userId = 1L;
        String deviceType = "laptop";
        String deviceId = "123abc@#";

        lenient().when(firebaseTokenRepository.existsFirebaseTokenByUserIdAndDeviceType(userId, deviceType)).thenReturn(true);
        boolean actual = firebaseTokenService.validateFirebaseToken(userId, deviceType);
        assertThat(actual).isEqualTo(true);
    }

    /**
     * Case - when token does not exist in the database.
     * The method which is under test. @link #{validateFirebaseToken(Long userId, String deviceType, String deviceId)}.
     */
    @Test
    public void testValidateFirebaseToken_FalseCase() {
        Long userId = 1L;
        String deviceType = "laptop";
        String deviceId = "123abc@#";

        lenient().when(firebaseTokenRepository.existsFirebaseTokenByUserIdAndDeviceType(userId, deviceType)).thenReturn(false);
        boolean actual = firebaseTokenService.validateFirebaseToken(userId, deviceType);
        assertThat(actual).isEqualTo(false);
    }

    /**
     * Case - when all conditions are rue. i.e. when token is successfully fetched from the database.
     * The method which is under test. @link #{getFirebaseToken(Long userId, String deviceType, String deviceId))}.
     */
    @Test
    public void testGetFirebaseToken_Success() {
        Long userId = 1L;
        String deviceType = "laptop";
        String deviceId = "999";

        FirebaseToken firebaseToken = new FirebaseToken(1L, 1L, "abc123@#", "999", "laptop", LocalDateTime.now());

        lenient().when(firebaseTokenRepository.findByUserIdAndDeviceType(userId, deviceType)).thenReturn(firebaseToken);
        String actual = firebaseTokenService.getFirebaseToken(userId, deviceType);
        assertThat(actual).containsIgnoringCase(firebaseToken.getToken());
    }

    /**
     * Case - when no token is found in the database
     * The method which is under test. @link #{getFirebaseToken(Long userId, String deviceType, String deviceId)}.
     */
    @Test
    public void testGetFirebaseToken_noTokenFound() {
        Long userId = 1L;
        String deviceType = "laptop";
        String deviceId = "999";
        lenient().when(firebaseTokenRepository.findByUserIdAndDeviceType(userId, deviceType)).thenReturn(null);

        String actual = firebaseTokenService.getFirebaseToken(userId, deviceType);
        assertThat(actual).isEqualTo(null);
    }

}
