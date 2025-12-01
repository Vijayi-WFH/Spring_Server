package com.example.chat_app.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(schema = "chat")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PinnedChats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pinOrFavouriteId;
    private Long accountId;
    private Long chatTypeId;
    private Long chatId;
}
