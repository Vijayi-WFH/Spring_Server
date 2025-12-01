package com.example.chat_app.model;


import com.example.chat_app.config.NewDataEncryptionConverter;
import com.example.chat_app.constants.Constants.GroupIconEnum;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.BeanUtils;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "group", schema = "chat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties("groupUsers")
public class Group implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long groupId;

    @Column(nullable = false)
    @Convert(converter = NewDataEncryptionConverter.class)
    private String name;

    private String description;

    @Column(nullable = false)
    private String type;


    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<GroupUser> groupUsers;

    @Column(name = "org_id")
    private Long orgId;

    @Convert(converter = NewDataEncryptionConverter.class)
    private String lastMessage;
    private Long lastMessageSenderAccountId;
    private Long lastMessageId;
    private LocalDateTime lastMessageTimestamp;

    private Long entityTypeId;
    private Long entityId;

    @Transient
    private List<User> users;

    // Many-to-many relationship between Group and User
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_mention", // join table name
            schema = "chat",
            joinColumns = @JoinColumn(name = "group_id"), // foreign key to Group
            inverseJoinColumns = @JoinColumn(name = "account_id") // foreign key to User
    )
    private List<User> mentionedUsers;

    @Column(name = "created_by_user")
    private User createdByUser;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "group_icon_code")
    private String groupIconCode;

    @Column(name = "group_icon_color")
    private String groupIconColor;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_by")
    private Long createdByAccountId;

    @PostLoad
    public void setUsersFromGroupUsers() {
        ObjectMapper mapper = new ObjectMapper();

        this.users = groupUsers.stream()
                .map(groupUser -> {
                    User user = groupUser.getUser();
                    User userCopy = new User();
                    if(user!=null){
                        BeanUtils.copyProperties(user, userCopy);
                        userCopy.setIsAdmin(groupUser.getIsAdmin());
                        userCopy.setIsDeleted(groupUser.getIsDeleted());
                    }
                    return userCopy;
                })
                .collect(Collectors.toList());
    }
}