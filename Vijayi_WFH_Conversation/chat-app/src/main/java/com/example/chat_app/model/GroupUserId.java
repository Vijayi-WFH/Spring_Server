package com.example.chat_app.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

import javax.persistence.Embeddable;

@Embeddable
@Getter
@Setter
public class GroupUserId implements Serializable {

    private Long groupId;
    private Long accountId;

    // Default constructor, getters, setters, equals, and hashcode methods
    public GroupUserId() {
    }

    public GroupUserId(Long groupId, Long accountId) {
        this.groupId = groupId;
        this.accountId = accountId;
    }

    // Getters and Setters

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GroupUserId that = (GroupUserId) o;
        return groupId.equals(that.groupId) && accountId.equals(that.accountId);
    }

    @Override
    public int hashCode() {
        return 31 * groupId.hashCode() + accountId.hashCode();
    }
}