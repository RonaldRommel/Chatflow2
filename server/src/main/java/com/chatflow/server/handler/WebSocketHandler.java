package com.chatflow.server.handler;

import com.chatflow.server.model.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public WebSocketHandler(ObjectMapper objectMapper, Validator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String roomId = extractRoomId(session);
//        System.out.println("New connection established - Session ID: " + session.getId() + ", Room: " + roomId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
//        System.out.println("Received message: " + payload);

        try {
            ChatMessage chatMessage = objectMapper.readValue(payload, ChatMessage.class);

            Set<ConstraintViolation<ChatMessage>> violations = validator.validate(chatMessage);

            if (!violations.isEmpty()) {
                StringBuilder errorMessages = new StringBuilder();
                for (ConstraintViolation<ChatMessage> violation : violations) {
                    errorMessages.append(violation.getMessage()).append("; ");
                }
                sendErrorResponse(session, errorMessages.toString());
                return;
            }

            try {
                int userId = Integer.parseInt(chatMessage.getUserID());
                if (userId < 1 || userId > 100000) {
                    sendErrorResponse(session, "UserId must be between 1 and 100000");
                    return;
                }
            } catch (NumberFormatException e) {
                sendErrorResponse(session, "UserId must be a valid number");
                return;
            }

            sendSuccessResponse(session, chatMessage);

        } catch (Exception e) {
            sendErrorResponse(session, "Invalid message format: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
//        System.out.println("Connection closed - Session ID: " + session.getId() + ", Status: " + status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
//        System.err.println("Transport error for session " + session.getId() + ": " + exception.getMessage());
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    private void sendSuccessResponse(WebSocketSession session, ChatMessage chatMessage) throws Exception {
        if (!session.isOpen()) {
//            System.err.println("Session closed, cannot send response");
            return;
        }
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("serverTimestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        response.put("message", chatMessage);

        String jsonResponse = objectMapper.writeValueAsString(response);
        session.sendMessage(new TextMessage(jsonResponse));
//        System.out.println("Sent success response: " + jsonResponse);
    }

    private void sendErrorResponse(WebSocketSession session, String errorMessage) throws Exception {
        if (!session.isOpen()) {
            System.err.println("Session closed, cannot send response");
            return;
        }
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ERROR");
        response.put("errorMessage", errorMessage);
        response.put("serverTimestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        String jsonResponse = objectMapper.writeValueAsString(response);
        session.sendMessage(new TextMessage(jsonResponse));
        System.err.println("Sent error response: " + jsonResponse);
    }

    private String extractRoomId(WebSocketSession session) {
        String path = session.getUri().getPath();
        String[] parts = path.split("/");
        return parts.length > 2 ? parts[2] : "unknown";
    }
}