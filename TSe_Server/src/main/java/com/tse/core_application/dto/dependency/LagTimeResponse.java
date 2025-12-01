package com.tse.core_application.dto.dependency;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LagTimeResponse {

    private List<String> offDays;
    private int breakTimeInDay;
    private LocalTime officeEndTime;
    private LocalTime officeStartTime;
    private int lagTime;

    public LagTimeResponse(int lagtime){
        this.lagTime = lagtime;
    }
}
