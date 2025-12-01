package com.tse.core_application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class AllEpicResponse {
    List<EpicDetailsResponse> epicDetailsResponseList;
}
