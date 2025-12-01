package com.tse.core_application.dto;

import com.tse.core_application.model.Stat;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class StatsResponse {

  private List<Stat> stats;
  private List<Stat> todayStats;
  private List<FlaggedTaskInfo> flaggedTaskInfoList;
}
