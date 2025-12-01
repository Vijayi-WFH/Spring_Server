package com.tse.core_application.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "firebase_token", schema = Constants.SCHEMA_NAME)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FirebaseToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id", unique = true, nullable = false)
    private Long tokenId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "device_type", nullable = false)
    private String deviceType;

    @Column(name = "timestamp", updatable = false, nullable = false)
    private LocalDateTime timestamp;

    public FirebaseToken(Long userId, String token, String deviceType, String deviceId, LocalDateTime timestamp) {
        this.userId = userId;
        this.token = token;
        this.deviceType = deviceType;
        this.deviceId = deviceId;
        this.timestamp = timestamp;
    }
}
