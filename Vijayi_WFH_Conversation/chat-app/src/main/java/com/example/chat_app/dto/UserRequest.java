package com.example.chat_app.dto;

import com.example.chat_app.model.Group;

import java.io.Serializable;
import java.util.List;

public class UserRequest implements Serializable {

    private Long userId;

    private String name;

    private List<Group> groups;

    // Getters and Setters
    public Long getId() {
        return userId;
    }

    public void setId(Long id) {
        this.userId = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public void setGroups(List<Group> groups) {
        this.groups = groups;
    }
}
