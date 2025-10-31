package com.chatflow.server.handler;

import com.chatflow.server.model.UserInfo;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RoomSessionManager {

    private final Map<WebSocketSession, UserInfo> sessionUsers = new ConcurrentHashMap<>();
    private final Map<String, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    public boolean hasSession(WebSocketSession session) {
        return sessionUsers.containsKey(session);
    }

    public void addUserSession(WebSocketSession session, UserInfo user) {
        sessionUsers.put(session, user);
    }

    public void addUserToRoom(WebSocketSession session, String roomId, UserInfo user) {
        roomSessions.computeIfAbsent(roomId, r -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void removeSession(WebSocketSession session) {
        UserInfo user = sessionUsers.remove(session);
        if (user != null) {
            for (Set<WebSocketSession> sessions : roomSessions.values()) {
                sessions.remove(session);
            }
        }
    }

    public Set<WebSocketSession> getSessionsInRoom(String roomId) {
        return roomSessions.getOrDefault(roomId, Collections.emptySet());
    }

    public UserInfo getUser(WebSocketSession session) {
        return sessionUsers.get(session);
    }
}