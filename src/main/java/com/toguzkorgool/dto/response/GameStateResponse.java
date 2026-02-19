package com.toguzkorgool.dto.response;

import java.util.List;

public record GameStateResponse(
        int[] holes,
        int[] kazan,
        int[] tuz,
        int currentPlayer,
        boolean gameOver,
        String winner,
        int moveNumber,
        List<MoveRecord> moveHistory,
        double whiteTimeRemaining,
        double blackTimeRemaining,
        boolean timerEnabled,
        boolean undoEnabled,
        Long lastMoveTime
) {
}
