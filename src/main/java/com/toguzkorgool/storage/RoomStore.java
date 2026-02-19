package com.toguzkorgool.storage;

import com.toguzkorgool.model.Room;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class RoomStore {

    private final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();

    public Room get(String roomId) {
        return rooms.get(roomId);
    }

    public Room put(String roomId, Room room) {
        return rooms.put(roomId, room);
    }

    public Room putIfAbsent(String roomId, Room room) {
        return rooms.putIfAbsent(roomId, room);
    }

    public Room remove(String roomId) {
        return rooms.remove(roomId);
    }

    public boolean containsKey(String roomId) {
        return rooms.containsKey(roomId);
    }
}
