package com.example.chat_app.controller;

import com.example.chat_app.constants.Constants;
import com.example.chat_app.custom.model.RestResponseWithGenericData;
import com.example.chat_app.dto.ChatResponse;
import com.example.chat_app.handlers.CustomResponseHandler;
import com.example.chat_app.jwtUtils.JWTUtil;
import com.example.chat_app.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@CrossOrigin(value = "*")
@RestController
@RequestMapping("/api/chats")
public class ChatController {

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private ChatService chatService;

    @GetMapping("/all")
    ResponseEntity<RestResponseWithGenericData<List<ChatResponse>>> getChatsWithoutFilter(@RequestHeader List<Long> accountIds, HttpServletRequest request, @RequestHeader String screenName, @RequestHeader String timeZone) {

        String jwtToken = request.getHeader("Authorization");
        jwtUtil.validateTokenAndAccountIds(jwtToken, screenName, accountIds);
        List<ChatResponse> combinedList = chatService.getChatResponses("", accountIds, timeZone);

        return CustomResponseHandler.generateCustomResponseWrapper(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, combinedList);
    }

    @PostMapping("/all")
    ResponseEntity<Object> search (@RequestBody String query, @RequestHeader List<Long> accountIds, HttpServletRequest request, @RequestHeader String screenName, @RequestHeader String timeZone) {
        try {
            String jwtToken = request.getHeader("Authorization");
            jwtUtil.validateTokenAndAccountIds(jwtToken, screenName, accountIds);

            List<ChatResponse> combinedList = chatService.getChatResponses(query, accountIds, timeZone);

            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, combinedList);

        } catch (Exception e) {
            return new ResponseEntity<>(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/pin/{chatTypeId}/{chatId}")
    ResponseEntity<Object> pinChat (@RequestHeader List<Long> accountIds, HttpServletRequest request, @RequestHeader String screenName, @PathVariable Long chatTypeId, @PathVariable Long chatId) {
        try {
            String jwtToken = request.getHeader("Authorization");
            jwtUtil.validateTokenAndAccountIds(jwtToken, screenName, accountIds);

            String message = chatService.pinChat(accountIds.get(0), chatTypeId, chatId);

            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, message);

        } catch (Exception e) {
            return new ResponseEntity<>(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/unpin/{chatTypeId}/{chatId}")
    ResponseEntity<Object> unpinChat (@RequestHeader List<Long> accountIds, HttpServletRequest request, @RequestHeader String screenName, @PathVariable Long chatTypeId, @PathVariable Long chatId) {
        try {
            String jwtToken = request.getHeader("Authorization");
            jwtUtil.validateTokenAndAccountIds(jwtToken, screenName, accountIds);

            String message = chatService.unpinChat(accountIds.get(0), chatTypeId, chatId);

            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, message);

        } catch (Exception e) {
            return new ResponseEntity<>(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/favourite/{chatTypeId}/{chatId}")
    ResponseEntity<Object> favouriteChat (@RequestHeader List<Long> accountIds, HttpServletRequest request, @RequestHeader String screenName, @PathVariable Long chatTypeId, @PathVariable Long chatId) {
        try {
            String jwtToken = request.getHeader("Authorization");
            jwtUtil.validateTokenAndAccountIds(jwtToken, screenName, accountIds);

            String message = chatService.favouriteChat(accountIds.get(0), chatTypeId, chatId);

            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, message);

        } catch (Exception e) {
            return new ResponseEntity<>(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/unfavourite/{chatTypeId}/{chatId}")
    ResponseEntity<Object> unfavouriteChat (@RequestHeader List<Long> accountIds, HttpServletRequest request, @RequestHeader String screenName, @PathVariable Long chatTypeId, @PathVariable Long chatId) {
        try {
            String jwtToken = request.getHeader("Authorization");
            jwtUtil.validateTokenAndAccountIds(jwtToken, screenName, accountIds);

            String message = chatService.unfavouriteChat(accountIds.get(0), chatTypeId, chatId);

            return CustomResponseHandler.generateCustomResponse(HttpStatus.OK, Constants.FormattedResponse.SUCCESS, message);

        } catch (Exception e) {
            return new ResponseEntity<>(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
