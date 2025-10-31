package com.chatflow.server.model;

public class UserInfo {
    private String userId;
    private String username;
    private String roomId;

    public UserInfo() {}

    public UserInfo(String userId, String username, String roomId) {
        this.userId = userId;
        this.username = username;
        this.roomId = roomId;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserInfo)) return false;
        UserInfo userInfo = (UserInfo) o;
        return userId != null && userId.equals(userInfo.userId);
    }

    @Override
    public int hashCode() {
        return userId != null ? userId.hashCode() : 0;
    }
}