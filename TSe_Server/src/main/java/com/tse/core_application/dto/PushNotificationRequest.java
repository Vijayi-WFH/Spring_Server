package com.tse.core_application.dto;

import com.tse.core_application.model.FirebaseToken;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PushNotificationRequest {

    private Map<String, String> payload;
    private FirebaseToken targetToken;
    private String deviceType;
}
