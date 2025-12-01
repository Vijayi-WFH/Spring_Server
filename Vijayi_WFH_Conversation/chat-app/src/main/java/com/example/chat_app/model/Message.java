package com.example.chat_app.model;

import com.example.chat_app.config.NewDataEncryptionConverter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "message", schema = "chat")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long messageId;

    private Long senderId;
    private Long receiverId;

    @Column(name = "content", length = 8000)
    @Convert(converter = NewDataEncryptionConverter.class)
    private String content;

    // replyId is used to store the ID of the parent message (if any)
    private Long replyId;

    private Boolean isDelivered = false;
    private Boolean isRead = false;
    private Boolean isEdited = false;
    private Boolean isDeleted = false;
    private LocalDateTime timestamp;

    private Long groupId;

    private Long taskAttachmentId;

    @Column(name = "message_attachment_ids")
    private String messageAttachmentIds;

    // Many-to-One relationship to refer to the immediate parent message
    @ManyToOne(fetch = FetchType.EAGER) // Lazy load to avoid fetching the entire chain of replies
    @JoinColumn(name = "replyId", insertable = false, updatable = false)
    private Message reply;

    @OneToMany(mappedBy = "message", fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<MessageUser> messageUsers;
}
