package com.tse.core_application.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class LeaveTabsAccessResponse {
    Long orgId;
    List<Integer> tabsIdList = new ArrayList<>();


    public void addToTabIdList (Integer id) {
        this.tabsIdList.add(id);
    }
    public List<Integer> getTabsIdList()
    {
        return this.tabsIdList;
    }
}
