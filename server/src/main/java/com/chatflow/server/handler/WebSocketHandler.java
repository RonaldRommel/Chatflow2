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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Handles WebSocket connections for the chat server.
 * <p>
 * Validates incoming chat messages, sends success or error responses,
 * and manages connection lifecycle events and transport errors.
 */
@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    /**
     * Constructs the WebSocketHandler with a JSON mapper and validator.
     *
     * @param objectMapper the Jackson ObjectMapper for JSON serialization
     * @param validator    the Validator for ChatMessage constraints
     */

    public WebSocketHandler(ObjectMapper objectMapper, Validator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    /**
     * Called after a WebSocket connection is established.
     *
     * @param session the WebSocket session
     * @throws Exception if any error occurs during connection setup
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String roomId = extractRoomId(session);
    }


    /**
     * Handles incoming text messages, validates them, and sends appropriate responses.
     *
     * @param session the WebSocket session
     * @param message the received text message
     * @throws Exception if processing or sending response fails
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
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

    /**
     * Called when a WebSocket connection is closed.
     *
     * @param session the WebSocket session
     * @param status  the close status
     * @throws Exception if any error occurs during closure
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("Connection closed - Session ID: " + session.getId() + ", Status: " + status);
    }

    /**
     * Handles transport-level errors for the session.
     *
     * @param session   the WebSocket session
     * @param exception the error that occurred
     * @throws Exception if closing the session fails
     */

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("Transport error for session " + session.getId() + ": " + exception.getMessage());
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    /**
     * Sends a success response back to the client with the server timestamp.
     *
     * @param session     the WebSocket session
     * @param chatMessage the chat message to include in the response
     * @throws Exception if sending the response fails
     */
    private void sendSuccessResponse(WebSocketSession session, ChatMessage chatMessage) throws Exception {
        if (!session.isOpen()) {
            System.err.println("Session closed, cannot send response");
            return;
        }
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("serverTimestamp", Instant.now());
        response.put("message", chatMessage);

        String jsonResponse = objectMapper.writeValueAsString(response);
        session.sendMessage(new TextMessage(jsonResponse));
    }

    /**
     * Sends an error response back to the client with details.
     *
     * @param session      the WebSocket session
     * @param errorMessage the error message to include
     * @throws Exception if sending the response fails
     */

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

    /**
     * Extracts the room ID from the WebSocket session URI path.
     *
     * @param session the WebSocket session
     * @return the extracted room ID, or "unknown" if not found
     */
    private String extractRoomId(WebSocketSession session) {
        String path = session.getUri().getPath();
        String[] parts = path.split("/");
        return parts.length > 2 ? parts[2] : "unknown";
    }
}