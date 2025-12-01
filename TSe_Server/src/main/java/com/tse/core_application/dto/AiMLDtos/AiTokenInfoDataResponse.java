package com.tse.core_application.dto.AiMLDtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AiTokenInfoDataResponse {

    private AiTokenInfoResponse infobot;
    private AiTokenInfoResponse tsebot;
    private AiTokenInfoResponse transcription;
}
