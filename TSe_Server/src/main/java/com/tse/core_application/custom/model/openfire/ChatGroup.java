package com.tse.core_application.custom.model.openfire;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ChatGroup {

    private String name;

    private String description;

    private List<String> admins;

    private List<String> members;

    public ChatGroup() {
    }

    public ChatGroup(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public ChatGroup(String name,String description,List<String> admins,List<String> members){
        this.name=name;
        this.description=description;
        this.admins=admins;
        this.members=members;
    }
}
