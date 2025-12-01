package com.example.chat_app.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "message_stats", schema = "chat")
@AllArgsConstructor
@NoArgsConstructor
public class MessageStats implements Persistable<Long> {

    // This entity is to store the users Count of read/delivered messages, just to calculate the tick logic.
    @Id
    @Column(name = "message_id")
    private Long messageId;

    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "group_size")
    private Integer groupSize;

    @Column(name = "delivered_count")
    private Integer deliveredCount = 1;

    @Column(name = "read_count")
    private Integer readCount = 1;

    @Column(name = "sender_id")
    private Long senderId;

    @Transient
    private boolean isNew = true;

    @Override
    public Long getId() {
        return messageId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}
