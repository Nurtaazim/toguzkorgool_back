package com.toguzkorgool.model;

import com.toguzkorgool.model.enums.PlayerSide;

import java.util.ArrayList;
import java.util.List;

public class GameState {

    private final String roomId;
    private final int[] holes = new int[18];
    private final int[] kazan = new int[2];
    private final int[] tuz = {-1, -1};
    private PlayerSide currentPlayer = PlayerSide.WHITE;
    private boolean gameOver;
    private String winner; // "0", "1", "DRAW", or null
    private String gameOverReason; // "RESIGN", "TIME", or null
    private int moveNumber = 1;
    private final List<MoveRecord> moveHistory = new ArrayList<>();
    private double whiteTimeRemaining;
    private double blackTimeRemaining;
    private boolean timerEnabled;
    private boolean undoEnabled;
    private long lastMoveTimestamp;

    public GameState(String roomId, int timerSetting, boolean undoEnabled) {
        this.roomId = roomId;
        this.timerEnabled = timerSetting > 0;
        this.whiteTimeRemaining = timerSetting;
        this.blackTimeRemaining = timerSetting;
        this.undoEnabled = undoEnabled;
        for (int i = 0; i < 18; i++) {
            holes[i] = 9;
        }
    }

    public record MoveRecord(int moveNumber, int player, int holeIndex, String description) {
    }

    public String getRoomId() {
        return roomId;
    }

    public int[] getHoles() {
        return holes;
    }

    public int[] getKazan() {
        return kazan;
    }

    public int[] getTuz() {
        return tuz;
    }

    public PlayerSide getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(PlayerSide currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public String getWinner() {
        return winner;
    }

    public void setWinner(String winner) {
        this.winner = winner;
    }

    public String getGameOverReason() {
        return gameOverReason;
    }

    public void setGameOverReason(String gameOverReason) {
        this.gameOverReason = gameOverReason;
    }

    public int getMoveNumber() {
        return moveNumber;
    }

    public void setMoveNumber(int moveNumber) {
        this.moveNumber = moveNumber;
    }

    public List<MoveRecord> getMoveHistory() {
        return moveHistory;
    }

    public double getWhiteTimeRemaining() {
        return whiteTimeRemaining;
    }

    public void setWhiteTimeRemaining(double whiteTimeRemaining) {
        this.whiteTimeRemaining = whiteTimeRemaining;
    }

    public double getBlackTimeRemaining() {
        return blackTimeRemaining;
    }

    public void setBlackTimeRemaining(double blackTimeRemaining) {
        this.blackTimeRemaining = blackTimeRemaining;
    }

    public boolean isTimerEnabled() {
        return timerEnabled;
    }

    public void setTimerEnabled(boolean timerEnabled) {
        this.timerEnabled = timerEnabled;
    }

    public boolean isUndoEnabled() {
        return undoEnabled;
    }

    public void setUndoEnabled(boolean undoEnabled) {
        this.undoEnabled = undoEnabled;
    }

    public long getLastMoveTimestamp() {
        return lastMoveTimestamp;
    }

    public void setLastMoveTimestamp(long lastMoveTimestamp) {
        this.lastMoveTimestamp = lastMoveTimestamp;
    }
}
