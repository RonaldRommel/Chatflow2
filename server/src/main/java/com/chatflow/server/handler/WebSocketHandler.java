package com.chatflow.server.handler;

import com.chatflow.server.model.ChatMessage;
import com.chatflow.server.model.UserInfo;
import com.chatflow.server.rabbit.RabbitMQSender;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.*;

@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final RabbitMQSender rabbitMQSender;
    private final RoomSessionManager roomSessionManager;

    public WebSocketHandler(ObjectMapper objectMapper, Validator validator,
                            RabbitMQSender rabbitMQSender, RoomSessionManager roomSessionManager) {
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.rabbitMQSender = rabbitMQSender;
        this.roomSessionManager = roomSessionManager;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {}

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if (!session.isOpen()) return;

        String payload = message.getPayload();

        try {
            if (!roomSessionManager.hasSession(session)) {
                UserInfo user = objectMapper.readValue(payload, UserInfo.class);
                roomSessionManager.addUserSession(session, user);

                if (user.getRoomId() != null) {
                    roomSessionManager.addUserToRoom(session, user.getRoomId(), user);
                }

                Map<String, String> response = new HashMap<>();
                response.put("status", "REGISTERED");
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
                return;
            }

            ChatMessage chatMessage = objectMapper.readValue(payload, ChatMessage.class);

            Set<ConstraintViolation<ChatMessage>> violations = validator.validate(chatMessage);
            if (!violations.isEmpty()) {
                return;
            }

            String json = objectMapper.writeValueAsString(chatMessage);
            rabbitMQSender.sendMessage(chatMessage.getRoomId(), json);

        } catch (Exception e) {}
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        roomSessionManager.removeSession(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        roomSessionManager.removeSession(session);
        try {
            if (session.isOpen()) {
                session.close(CloseStatus.SERVER_ERROR);
            }
        } catch (Exception e) {}
    }
}