package com.example.chat_app.repository;

import com.example.chat_app.model.FavouriteChats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FavouriteChatsRepository extends JpaRepository<FavouriteChats, Long> {
    FavouriteChats findByAccountIdInAndChatTypeIdAndChatId(List<Long> accountIds, Long chatTypeId, Long chatId);

    List<FavouriteChats> findByAccountIdAndChatTypeIdAndChatId(Long requesterAccountId, Long chatTypeId, Long chatId);
}
