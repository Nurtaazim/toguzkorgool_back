package com.toguzkorgool.service;

import com.toguzkorgool.dto.response.GameEventMessage;
import com.toguzkorgool.dto.response.GameStateResponse;
import com.toguzkorgool.model.GameState;
import com.toguzkorgool.model.enums.PlayerSide;
import com.toguzkorgool.storage.GameStore;
import jakarta.annotation.PreDestroy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
public class TimerService {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private final Map<String, ScheduledFuture<?>> timerTasks = new ConcurrentHashMap<>();
    private final GameStore gameStore;
    private final SimpMessagingTemplate messagingTemplate;

    public TimerService(GameStore gameStore, SimpMessagingTemplate messagingTemplate) {
        this.gameStore = gameStore;
        this.messagingTemplate = messagingTemplate;
    }

    public void startTimer(String roomId) {
        GameState state = gameStore.get(roomId);
        if (state == null || !state.isTimerEnabled()) return;

        state.setLastMoveTimestamp(System.currentTimeMillis());
        scheduleTimerTick(roomId);
    }

    public void switchTimer(String roomId) {
        GameState state = gameStore.get(roomId);
        if (state == null || !state.isTimerEnabled()) return;

        // Deduct precise time at move boundary
        long now = System.currentTimeMillis();
        long elapsed = now - state.getLastMoveTimestamp();
        double elapsedSeconds = elapsed / 1000.0;

        // The current player already switched, so deduct from the previous player (opponent of current)
        PlayerSide previousPlayer = state.getCurrentPlayer().opponent();
        deductTime(state, previousPlayer, elapsedSeconds);

        state.setLastMoveTimestamp(now);

        // Cancel existing tick and start new one
        cancelTimer(roomId);
        if (!state.isGameOver()) {
            scheduleTimerTick(roomId);
        }
    }

    public void cancelTimer(String roomId) {
        ScheduledFuture<?> future = timerTasks.remove(roomId);
        if (future != null) {
            future.cancel(false);
        }
    }

    private void scheduleTimerTick(String roomId) {
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> tick(roomId), 1, 1, TimeUnit.SECONDS);
        ScheduledFuture<?> old = timerTasks.put(roomId, future);
        if (old != null) {
            old.cancel(false);
        }
    }

    private void tick(String roomId) {
        GameState state = gameStore.get(roomId);
        if (state == null || state.isGameOver()) {
            cancelTimer(roomId);
            return;
        }

        synchronized (state) {
            if (state.isGameOver()) {
                cancelTimer(roomId);
                return;
            }

            long now = System.currentTimeMillis();
            long elapsed = now - state.getLastMoveTimestamp();
            double elapsedSeconds = elapsed / 1000.0;

            PlayerSide current = state.getCurrentPlayer();
            double remaining = getTimeRemaining(state, current) - elapsedSeconds;

            if (remaining <= 0) {
                // Timeout
                setTimeRemaining(state, current, 0);
                state.setGameOver(true);
                state.setGameOverReason("TIME");

                // Winner is the opponent
                PlayerSide winner = current.opponent();
                state.setWinner(String.valueOf(winner.getIndex()));

                cancelTimer(roomId);

                GameStateResponse stateResponse = GameService.toGameStateResponse(state);
                messagingTemplate.convertAndSend("/topic/game/" + roomId,
                        new GameEventMessage("GAME_OVER", stateResponse, "TIME"));
            } else {
                // Broadcast timer update
                Map<String, Double> timerData = Map.of(
                        "whiteTimeRemaining", current == PlayerSide.WHITE
                                ? getTimeRemaining(state, PlayerSide.WHITE) - elapsedSeconds
                                : getTimeRemaining(state, PlayerSide.WHITE),
                        "blackTimeRemaining", current == PlayerSide.BLACK
                                ? getTimeRemaining(state, PlayerSide.BLACK) - elapsedSeconds
                                : getTimeRemaining(state, PlayerSide.BLACK)
                );
                messagingTemplate.convertAndSend("/topic/game/" + roomId,
                        new GameEventMessage("TIMER_UPDATE", timerData));
            }
        }
    }

    private void deductTime(GameState state, PlayerSide side, double seconds) {
        double remaining = getTimeRemaining(state, side) - seconds;
        setTimeRemaining(state, side, Math.max(0, remaining));
    }

    private double getTimeRemaining(GameState state, PlayerSide side) {
        return side == PlayerSide.WHITE ? state.getWhiteTimeRemaining() : state.getBlackTimeRemaining();
    }

    private void setTimeRemaining(GameState state, PlayerSide side, double time) {
        if (side == PlayerSide.WHITE) {
            state.setWhiteTimeRemaining(time);
        } else {
            state.setBlackTimeRemaining(time);
        }
    }

    @PreDestroy
    public void shutdown() {
        timerTasks.values().forEach(f -> f.cancel(false));
        timerTasks.clear();
        scheduler.shutdownNow();
    }
}
