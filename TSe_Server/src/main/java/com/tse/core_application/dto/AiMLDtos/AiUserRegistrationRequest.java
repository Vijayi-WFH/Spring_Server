package com.tse.core_application.dto.AiMLDtos;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class AiUserRegistrationRequest {

    private String accountId;
    private Integer maxTokens;
    private String timezone;
    // in Ai service we have found that their request and response is being used as SNAKE-CASE instead of pascalCase,
    // so we are using the @JsonNaming for complete class level snake-case modification
}
