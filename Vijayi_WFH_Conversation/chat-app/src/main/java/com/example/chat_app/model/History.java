package com.example.chat_app.model;

import javax.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "history", schema = "chat")
public class History {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

//    @Column(nullable = false)
    private String content;

    //    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private Long messageId;

    @OneToMany(mappedBy = "history")
    private List<HistoryTag> historyTags;

    public List<HistoryTag> getHistoryTags() {
        return historyTags;
    }

    public void setHistoryTags(List<HistoryTag> historyTags) {
        this.historyTags = historyTags;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
