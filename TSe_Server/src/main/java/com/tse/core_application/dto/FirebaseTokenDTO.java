package com.tse.core_application.dto;

import com.tse.core_application.constants.ErrorConstant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FirebaseTokenDTO {

    @NotBlank(message = ErrorConstant.FirebaseTokenDTO.TOKEN)
    private String token;

    @NotBlank(message = ErrorConstant.FirebaseTokenDTO.DEVICE_TYPE)
    private String deviceType;

    @NotBlank(message = ErrorConstant.FirebaseTokenDTO.DEVICE_ID)
    private String deviceId;

    @NotNull(message = ErrorConstant.FirebaseTokenDTO.TIMESTAMP)
    private LocalDateTime timestamp;
}
