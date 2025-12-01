package com.example.chat_app.config;

import com.example.chat_app.model.Group;
import com.example.chat_app.model.Message;
import com.example.chat_app.model.User;
import com.example.chat_app.repository.GroupRepository;
import com.example.chat_app.repository.MessageRepository;
import com.example.chat_app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

@Service
public class MigrationData {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final MessageRepository messageRepository;
    private final DataEncryptionConverter oldConverter = new DataEncryptionConverter();
    private final NewDataEncryptionConverter newConverter = new NewDataEncryptionConverter();

    @Autowired
    private EntityManager entityManager;

    public MigrationData(UserRepository userRepository, GroupRepository groupRepository, MessageRepository messageRepository) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional
    public void migrateData() {
        // Migrate User
//        List<User> users = userRepository.findAll();
//        for (User user : users) {
//            user.setFirstName(migrateField(user.getFirstName(), "User " + user.getAccountId() + " firstName"));
//            user.setLastName(migrateField(user.getLastName(), "User " + user.getAccountId() + " lastName"));
//            user.setMiddleName(migrateField(user.getMiddleName(), "User " + user.getAccountId() + " middleName"));
//            user.setEmail(migrateField(user.getEmail(), "User " + user.getAccountId() + " email"));
//            userRepository.save(user);
//        }

        // Migrate Group
        List<Group> groups = groupRepository.findAll();
//        for (Group group : groups) {
//            group.setName(migrateField(group.getName(), "Group " + group.getGroupId() + " name"));
//            group.setLastMessage(migrateField(group.getLastMessage(), "Group " + group.getGroupId() + " lastMessage"));
//            groupRepository.save(group);
//        }
//        List<Group> groups = groupRepo.findAll();
        /// ------------------need to uncomment--------//
//        for (Group group : groups) {
//            if (group.getCreatedByUser() != null) {
//                group.setCreatedByAccountId(group.getCreatedByUser().getAccountId());
//                groupRepository.save(group);
//            }
//        }
        // --------------------------//
//        entityManager.createNativeQuery("ALTER TABLE group DROP COLUMN created_by_user").executeUpdate();

//        //Migrate messages
//        List<Message> messages = messageRepository.findAll();
//        for(Message message : messages){
//            message.setContent(migrateField(message.getContent(), "messageContent "+ message.getMessageId()));
//            messageRepository.save(message);
//        }
    }

    private String migrateField(String encryptedData, String context) {
        if (encryptedData == null) {
            return null;
        }
        try {
            // Decrypt old format
            Object decrypted = oldConverter.convertToEntityAttribute(encryptedData);
            if (!(decrypted instanceof String)) {
                System.err.println("Unexpected type for " + context + ": " + (decrypted != null ? decrypted.getClass().getName() : "null"));
                return null;
            }
            // Encrypt with new converter
            String newEncrypted = newConverter.convertToDatabaseColumn(decrypted);
            System.out.println("Migrated " + context + ": " + encryptedData + " -> " + decrypted + " -> " + newEncrypted);
            return newEncrypted;
        } catch (Exception e) {
            System.err.println("Migration failed for " + context + ": " + encryptedData + ", error: " + e.getMessage());
            return null;
        }
    }
}