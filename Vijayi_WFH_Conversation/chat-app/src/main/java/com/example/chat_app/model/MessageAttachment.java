package com.example.chat_app.model;

import com.example.chat_app.config.NewDataEncryptionConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "message_attachment", schema = "chat")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MessageAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_attachment_id", nullable = false, unique = true)
    private Long messageAttachmentId;

    @Column(name = "message_id", nullable = false)
    private Long messageId = -1L;

    @Column(name = "file_name", nullable = false, length = 500)
    @Convert(converter = NewDataEncryptionConverter.class)
    @Size(max= 100)
    private String fileName;

    @Column(name = "file_type", nullable = false, length = 100)
    private String fileType;

    @Column(name = "file_size", nullable = false)
    private Double fileSize;

    //    @Basic(fetch = FetchType.LAZY)
    @Column(name = "file_content", nullable = false)
    @Lob
    @JsonIgnore
    private byte[] fileContent;

    @Column(name = "file_status", nullable = false, length = 1)
    private Character fileStatus;

    @CreationTimestamp
    @Column(name = "created_date_time", nullable = false, updatable = false)
    private LocalDateTime createdDateTime;

    //    @UpdateTimestamp
    @Column(name = "deleted_date_time")
    private LocalDateTime deletedDateTime;
}
