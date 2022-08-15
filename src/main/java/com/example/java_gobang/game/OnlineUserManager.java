package com.example.java_gobang.game;

import org.springframework.stereotype.Component;
import org.springframework.web.server.WebSession;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OnlineUserManager {
    private ConcurrentHashMap<Integer, WebSocketSession> gameHall = new ConcurrentHashMap<>();

    public void enterGameHall(int userId,WebSocketSession webSocketSession) {
        gameHall.put(userId,webSocketSession);
    }

    public void exitGameHall(int userId) {
        gameHall.remove(userId);
    }

    public WebSocketSession getFromGameHall(int userId) {
        return gameHall.get(userId);
    }
}
