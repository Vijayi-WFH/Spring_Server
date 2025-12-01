package com.tse.core_application.dto.AiMLDtos;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class AiUserApisResponse {

    private Long accountId;
    private Integer maxTokens;
    private Integer initialTokens;
    private String timezone;
    private List<String> services;
    private Integer servicesUpdated;
    private String removalTime;
    private String maxTokenSetTo;
    private AiTokenInfoDataResponse tokenInfo;
}
