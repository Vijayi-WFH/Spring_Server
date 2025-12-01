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
public class FavouriteChats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long favouriteId;
    private Long accountId;
    private Long chatTypeId;
    private Long chatId;
}
