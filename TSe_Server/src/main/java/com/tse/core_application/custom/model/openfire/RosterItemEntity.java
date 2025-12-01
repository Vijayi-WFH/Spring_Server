package com.tse.core_application.custom.model.openfire;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RosterItemEntity {

    private String jid;

    private String nickname;

    private int subscriptionType;

    private List<String> groups;

    public RosterItemEntity(String jid, String nickname, int subscriptionType) {
        this.jid = jid;
        this.nickname = nickname;
        this.subscriptionType = subscriptionType;
    }
}
