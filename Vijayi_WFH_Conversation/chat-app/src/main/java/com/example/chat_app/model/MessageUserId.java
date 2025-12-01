package com.example.chat_app.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
@Getter
@Setter
public class MessageUserId implements Serializable {

    private Long messageId;
    private Long accountId;

    // Default constructor, getters, setters, equals, and hashcode methods
    public MessageUserId() {
    }

    public MessageUserId(Long groupId, Long accountId) {
        this.messageId = groupId;
        this.accountId = accountId;
    }

    // Getters and Setters

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageUserId that = (MessageUserId) o;
        return messageId.equals(that.messageId) && accountId.equals(that.accountId);
    }

    @Override
    public int hashCode() {
        return 31 * messageId.hashCode() + accountId.hashCode();
    }
}
