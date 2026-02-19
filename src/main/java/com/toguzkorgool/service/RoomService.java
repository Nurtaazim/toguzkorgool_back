package com.toguzkorgool.service;

import com.toguzkorgool.dto.response.PlayerResponse;
import com.toguzkorgool.dto.response.RoomEventMessage;
import com.toguzkorgool.dto.response.RoomResponse;
import com.toguzkorgool.exception.PlayerNotFoundException;
import com.toguzkorgool.exception.RoomAlreadyExistsException;
import com.toguzkorgool.exception.RoomFullException;
import com.toguzkorgool.exception.RoomNotFoundException;
import com.toguzkorgool.model.Player;
import com.toguzkorgool.model.Room;
import com.toguzkorgool.model.enums.RoomStatus;
import com.toguzkorgool.storage.RoomStore;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RoomService {

    private final RoomStore roomStore;
    private final SimpMessagingTemplate messagingTemplate;

    public RoomService(RoomStore roomStore, SimpMessagingTemplate messagingTemplate) {
        this.roomStore = roomStore;
        this.messagingTemplate = messagingTemplate;
    }

    public RoomResponse createRoom(String playerName, String roomId, int timerSetting, boolean undoEnabled) {
        String playerId = UUID.randomUUID().toString();
        Player player = new Player(playerId, playerName);
        Room room = new Room(roomId, player, timerSetting, undoEnabled);

        if (roomStore.putIfAbsent(roomId, room) != null) {
            throw new RoomAlreadyExistsException(roomId);
        }

        return toResponse(room);
    }

    public RoomResponse joinRoom(String roomId, String playerName) {
        Room room = getRoom(roomId);

        synchronized (room) {
            if (room.isFull()) {
                throw new RoomFullException(roomId);
            }

            String playerId = UUID.randomUUID().toString();
            Player player = new Player(playerId, playerName);
            room.setPlayer2(player);

            messagingTemplate.convertAndSend("/topic/room/" + roomId,
                    new RoomEventMessage("PLAYER_JOINED", playerName, playerId));

            return toResponse(room);
        }
    }

    public void leaveRoom(String roomId, String playerId) {
        Room room = getRoom(roomId);

        synchronized (room) {
            String leavingPlayerName;
            if (room.getPlayer1() != null && room.getPlayer1().id().equals(playerId)) {
                leavingPlayerName = room.getPlayer1().name();
                // If host leaves, promote player2 or remove room
                if (room.getPlayer2() != null) {
                    room.setPlayer1(room.getPlayer2());
                    room.setPlayer2(null);
                } else {
                    roomStore.remove(roomId);
                    return;
                }
            } else if (room.getPlayer2() != null && room.getPlayer2().id().equals(playerId)) {
                leavingPlayerName = room.getPlayer2().name();
                room.setPlayer2(null);
            } else {
                throw new PlayerNotFoundException(playerId);
            }

            room.setStatus(RoomStatus.WAITING);

            messagingTemplate.convertAndSend("/topic/room/" + roomId,
                    new RoomEventMessage("PLAYER_LEFT", leavingPlayerName, null));
        }
    }

    public RoomResponse getRoomResponse(String roomId) {
        return toResponse(getRoom(roomId));
    }

    public Room getRoom(String roomId) {
        Room room = roomStore.get(roomId);
        if (room == null) {
            throw new RoomNotFoundException(roomId);
        }
        return room;
    }

    private RoomResponse toResponse(Room room) {
        PlayerResponse p1 = room.getPlayer1() != null
                ? new PlayerResponse(room.getPlayer1().id(), room.getPlayer1().name(), true)
                : null;
        PlayerResponse p2 = room.getPlayer2() != null
                ? new PlayerResponse(room.getPlayer2().id(), room.getPlayer2().name(), false)
                : null;
        return new RoomResponse(room.getRoomId(), p1, p2);
    }
}
