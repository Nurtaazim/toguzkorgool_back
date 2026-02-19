package com.toguzkorgool.service;

import com.toguzkorgool.dto.response.GameEventMessage;
import com.toguzkorgool.dto.response.GameStateResponse;
import com.toguzkorgool.dto.response.MoveHistoryResponse;
import com.toguzkorgool.dto.response.MoveRecord;
import com.toguzkorgool.engine.ToguzKorgoolEngine;
import com.toguzkorgool.exception.GameNotStartedException;
import com.toguzkorgool.exception.InvalidMoveException;
import com.toguzkorgool.exception.NotPlayerTurnException;
import com.toguzkorgool.exception.PlayerNotFoundException;
import com.toguzkorgool.model.GameState;
import com.toguzkorgool.model.Room;
import com.toguzkorgool.model.enums.PlayerSide;
import com.toguzkorgool.model.enums.RoomStatus;
import com.toguzkorgool.storage.GameStore;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class GameService {

    private static final int HISTORY_PAGE_SIZE = 20;

    private final GameStore gameStore;
    private final RoomService roomService;
    private final TimerService timerService;
    private final SimpMessagingTemplate messagingTemplate;

    public GameService(GameStore gameStore, RoomService roomService,
                       TimerService timerService, SimpMessagingTemplate messagingTemplate) {
        this.gameStore = gameStore;
        this.roomService = roomService;
        this.timerService = timerService;
        this.messagingTemplate = messagingTemplate;
    }

    public GameStateResponse startGame(String roomId) {
        Room room = roomService.getRoom(roomId);

        synchronized (room) {
            // If game already exists and is not over, return current state
            GameState existing = gameStore.get(roomId);
            if (existing != null && !existing.isGameOver()) {
                return toGameStateResponse(existing);
            }

            room.setStatus(RoomStatus.PLAYING);
            GameState state = new GameState(roomId, room.getTimerSetting(), room.isUndoEnabled());
            gameStore.put(roomId, state);

            GameStateResponse response = toGameStateResponse(state);

            messagingTemplate.convertAndSend("/topic/game/" + roomId,
                    new GameEventMessage("GAME_STARTED", response));

            if (state.isTimerEnabled()) {
                timerService.startTimer(roomId);
            }

            return response;
        }
    }

    public GameStateResponse makeMove(String roomId, String playerId, int holeIndex) {
        GameState state = getGameState(roomId);
        Room room = roomService.getRoom(roomId);

        synchronized (state) {
            if (state.isGameOver()) {
                throw new InvalidMoveException("Game is already over");
            }

            PlayerSide side = getPlayerSide(room, playerId);
            if (state.getCurrentPlayer() != side) {
                throw new NotPlayerTurnException();
            }

            int moveNum = state.getMoveNumber();
            String description = ToguzKorgoolEngine.makeMove(state, side, holeIndex);

            // Record move in history
            state.getMoveHistory().add(new GameState.MoveRecord(
                    moveNum, side.getIndex(), holeIndex, description));

            // Handle timer
            if (state.isTimerEnabled() && !state.isGameOver()) {
                timerService.switchTimer(roomId);
            }
            if (state.isGameOver()) {
                timerService.cancelTimer(roomId);
                room.setStatus(RoomStatus.FINISHED);
            }

            GameStateResponse response = toGameStateResponse(state);

            if (state.isGameOver()) {
                messagingTemplate.convertAndSend("/topic/game/" + roomId,
                        new GameEventMessage("GAME_OVER", response, state.getGameOverReason()));
            } else {
                messagingTemplate.convertAndSend("/topic/game/" + roomId,
                        new GameEventMessage("MOVE", response));
            }

            return response;
        }
    }

    public GameStateResponse resign(String roomId, String playerId) {
        GameState state = getGameState(roomId);
        Room room = roomService.getRoom(roomId);

        synchronized (state) {
            if (state.isGameOver()) {
                return toGameStateResponse(state);
            }

            PlayerSide side = getPlayerSide(room, playerId);
            PlayerSide winner = side.opponent();

            state.setGameOver(true);
            state.setWinner(String.valueOf(winner.getIndex()));
            state.setGameOverReason("RESIGN");

            timerService.cancelTimer(roomId);
            room.setStatus(RoomStatus.FINISHED);

            GameStateResponse response = toGameStateResponse(state);
            messagingTemplate.convertAndSend("/topic/game/" + roomId,
                    new GameEventMessage("GAME_OVER", response, "RESIGN"));

            return response;
        }
    }

    public void handleDrawAccepted(String roomId) {
        GameState state = getGameState(roomId);
        Room room = roomService.getRoom(roomId);

        synchronized (state) {
            if (state.isGameOver()) return;

            state.setGameOver(true);
            state.setWinner("DRAW");

            timerService.cancelTimer(roomId);
            room.setStatus(RoomStatus.FINISHED);

            GameStateResponse response = toGameStateResponse(state);
            messagingTemplate.convertAndSend("/topic/game/" + roomId,
                    new GameEventMessage("GAME_OVER", response, "DRAW"));
        }
    }

    public GameStateResponse startNewGame(String roomId) {
        Room room = roomService.getRoom(roomId);

        synchronized (room) {
            room.setStatus(RoomStatus.PLAYING);
            GameState state = new GameState(roomId, room.getTimerSetting(), room.isUndoEnabled());
            gameStore.put(roomId, state);

            GameStateResponse response = toGameStateResponse(state);

            messagingTemplate.convertAndSend("/topic/game/" + roomId,
                    new GameEventMessage("GAME_STARTED", response));

            if (state.isTimerEnabled()) {
                timerService.startTimer(roomId);
            }

            return response;
        }
    }

    public GameStateResponse getState(String roomId) {
        return toGameStateResponse(getGameState(roomId));
    }

    public MoveHistoryResponse getMoveHistory(String roomId, int page) {
        GameState state = getGameState(roomId);
        List<GameState.MoveRecord> history = state.getMoveHistory();

        int totalPages = Math.max(1, (int) Math.ceil((double) history.size() / HISTORY_PAGE_SIZE));
        int start = page * HISTORY_PAGE_SIZE;
        int end = Math.min(start + HISTORY_PAGE_SIZE, history.size());

        List<MoveRecord> pageRecords = (start < history.size())
                ? history.subList(start, end).stream()
                    .map(r -> new MoveRecord(r.moveNumber(), r.player(), r.holeIndex(), r.description()))
                    .toList()
                : List.of();

        return new MoveHistoryResponse(pageRecords, page, totalPages);
    }

    public GameState getGameState(String roomId) {
        GameState state = gameStore.get(roomId);
        if (state == null) {
            throw new GameNotStartedException(roomId);
        }
        return state;
    }

    public PlayerSide getPlayerSide(Room room, String playerId) {
        if (room.getPlayer1() != null && room.getPlayer1().id().equals(playerId)) {
            return PlayerSide.WHITE;
        } else if (room.getPlayer2() != null && room.getPlayer2().id().equals(playerId)) {
            return PlayerSide.BLACK;
        }
        throw new PlayerNotFoundException(playerId);
    }

    public static GameStateResponse toGameStateResponse(GameState state) {
        List<MoveRecord> history = state.getMoveHistory().stream()
                .map(r -> new MoveRecord(r.moveNumber(), r.player(), r.holeIndex(), r.description()))
                .toList();

        return new GameStateResponse(
                Arrays.copyOf(state.getHoles(), state.getHoles().length),
                Arrays.copyOf(state.getKazan(), state.getKazan().length),
                Arrays.copyOf(state.getTuz(), state.getTuz().length),
                state.getCurrentPlayer().getIndex(),
                state.isGameOver(),
                state.getWinner(),
                state.getMoveNumber(),
                history,
                state.getWhiteTimeRemaining(),
                state.getBlackTimeRemaining(),
                state.isTimerEnabled(),
                state.isUndoEnabled(),
                state.getLastMoveTimestamp() == 0 ? null : state.getLastMoveTimestamp()
        );
    }
}
