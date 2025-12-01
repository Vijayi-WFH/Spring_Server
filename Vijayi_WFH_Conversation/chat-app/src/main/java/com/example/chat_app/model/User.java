package com.example.chat_app.model;

import com.example.chat_app.config.NewDataEncryptionConverter;
import com.fasterxml.jackson.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

@Entity
@Table(name = "user", schema = "chat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties({"groups","groupMessageReadReceipts"})
public class User implements Serializable {

    //Todo: Temporary change for pre-prod testing. Need a long term solution
    private static final long serialVersionUID = 1L;

    @Id
    private Long accountId;

    @Column(nullable = false)
    private Long userId;

    @Column(name = "name", nullable = false)
    @Convert(converter = NewDataEncryptionConverter.class)
    private String firstName;

    @Convert(converter = NewDataEncryptionConverter.class)
    private String lastName;

    @Column(name = "middle_name")
    @Convert(converter = NewDataEncryptionConverter.class)
    private String middleName;
//    // Many-to-many relationship between User and Group
//    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "users") // This is the back reference of the relationship
//    @JsonIgnore
////    @JsonBackReference // Prevents serialization of 'groups' on the 'User' side
//    private List<Group> groups;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<GroupUser> groups;

    @Column(nullable = false)
    private Long orgId;

    @Column(nullable = false)
    private Boolean isActive;

    @Column(nullable = false)
    @Convert(converter = NewDataEncryptionConverter.class)
    private String email;

    @Transient
    private Boolean isAdmin;
    @Transient
    private Boolean isDeleted;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<MessageUser> groupMessageReadReceipts;

    @Column(name = "is_org_admin")
    private Boolean isOrgAdmin;
}
