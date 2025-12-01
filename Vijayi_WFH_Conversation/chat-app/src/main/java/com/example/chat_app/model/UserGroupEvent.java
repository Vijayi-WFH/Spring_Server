package com.example.chat_app.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "user_group_event", schema = "chat")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserGroupEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_group_event_id")
    private Long userGroupEventId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt = Instant.now();

    public UserGroupEvent(Long accountId, Long groupId, String eventType, Instant occurredAt) {
        this.accountId = accountId;
        this.groupId = groupId;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
    }
}
