package com.toguzkorgool.storage;

import com.toguzkorgool.model.GameState;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameStore {

    private final ConcurrentHashMap<String, GameState> games = new ConcurrentHashMap<>();

    public GameState get(String roomId) {
        return games.get(roomId);
    }

    public GameState put(String roomId, GameState state) {
        return games.put(roomId, state);
    }

    public GameState remove(String roomId) {
        return games.remove(roomId);
    }

    public boolean containsKey(String roomId) {
        return games.containsKey(roomId);
    }
}
