package com.tse.core_application.dto;

import com.google.firebase.database.utilities.Pair;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OffDaysResponse {
    private Long accountId;
    private String firstName;
    private String lastName;
    private Boolean isActive;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<UserOffDaysResponse> userOffDaysResponses;
}
