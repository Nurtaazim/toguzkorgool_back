package com.toguzkorgool.controller;

import com.toguzkorgool.dto.response.GameStateResponse;
import com.toguzkorgool.dto.response.MoveHistoryResponse;
import com.toguzkorgool.service.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping("/{roomId}/start")
    public ResponseEntity<GameStateResponse> startGame(@PathVariable String roomId) {
        return ResponseEntity.ok(gameService.startGame(roomId));
    }

    @GetMapping("/{roomId}/state")
    public ResponseEntity<GameStateResponse> getGameState(@PathVariable String roomId) {
        return ResponseEntity.ok(gameService.getState(roomId));
    }

    @GetMapping("/{roomId}/history")
    public ResponseEntity<MoveHistoryResponse> getMoveHistory(@PathVariable String roomId,
                                                               @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(gameService.getMoveHistory(roomId, page));
    }

    @PostMapping("/{roomId}/resign")
    public ResponseEntity<GameStateResponse> resign(@PathVariable String roomId,
                                                     @RequestParam String playerId) {
        return ResponseEntity.ok(gameService.resign(roomId, playerId));
    }
}
