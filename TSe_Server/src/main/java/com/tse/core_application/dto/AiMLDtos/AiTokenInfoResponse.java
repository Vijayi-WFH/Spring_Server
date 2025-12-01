package com.tse.core_application.dto.AiMLDtos;


import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class AiTokenInfoResponse {

    private Integer curTokens;
    private Integer maxTokens;
    private Integer hourlyRate;
    private String lastUpdateTime;
    private String timezone;
}
