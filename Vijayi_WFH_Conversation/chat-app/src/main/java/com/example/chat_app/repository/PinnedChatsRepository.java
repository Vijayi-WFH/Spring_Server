package com.example.chat_app.repository;

import com.example.chat_app.model.PinnedChats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PinnedChatsRepository extends JpaRepository<PinnedChats, Long> {
    List<PinnedChats> findByAccountIdAndChatTypeIdAndChatId(Long accountId, Long chatTypeId, Long chatId);

    PinnedChats findByAccountIdInAndChatTypeIdAndChatId(List<Long> accountIds, Long chatTypeId, Long entityId);
}
