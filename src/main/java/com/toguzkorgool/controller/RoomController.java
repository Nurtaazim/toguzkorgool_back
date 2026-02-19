package com.toguzkorgool.controller;

import com.toguzkorgool.dto.request.CreateRoomRequest;
import com.toguzkorgool.dto.request.JoinRoomRequest;
import com.toguzkorgool.dto.response.RoomResponse;
import com.toguzkorgool.service.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping("/create")
    public ResponseEntity<RoomResponse> createRoom(@RequestBody CreateRoomRequest request) {
        RoomResponse response = roomService.createRoom(
                request.playerName(), request.roomId(), request.timerSetting(), request.undoEnabled());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{roomId}/join")
    public ResponseEntity<RoomResponse> joinRoom(@PathVariable String roomId,
                                                  @RequestBody JoinRoomRequest request) {
        RoomResponse response = roomService.joinRoom(roomId, request.playerName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{roomId}/leave/{playerId}")
    public ResponseEntity<Void> leaveRoom(@PathVariable String roomId, @PathVariable String playerId) {
        roomService.leaveRoom(roomId, playerId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<RoomResponse> getRoom(@PathVariable String roomId) {
        return ResponseEntity.ok(roomService.getRoomResponse(roomId));
    }
}
