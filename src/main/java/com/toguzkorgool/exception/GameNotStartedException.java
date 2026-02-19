package com.toguzkorgool.exception;

public class GameNotStartedException extends RuntimeException {
    public GameNotStartedException(String roomId) {
        super("Game not started for room: " + roomId);
    }
}
