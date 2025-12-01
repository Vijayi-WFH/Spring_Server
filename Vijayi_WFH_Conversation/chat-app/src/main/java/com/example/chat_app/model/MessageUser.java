package com.example.chat_app.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.time.LocalDateTime;


@Entity
@Table(name = "message_user", schema = "chat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageUser implements Serializable {

    @EmbeddedId
    private MessageUserId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("messageId")
    @JoinColumn(name = "message_id")
    @JsonBackReference
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("accountId")
    @JoinColumn(name = "account_id")
    @JsonBackReference
    private User user;

    @Column(name = "is_delivered", nullable = false)
    private Boolean isDelivered = false;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(length = 10)  // Ensures enough space for emoji storage
    @Pattern(regexp = "^(\\p{So})$", message = "Reaction must be a single emoji")  // Validate single emoji
    private String reaction;
}
