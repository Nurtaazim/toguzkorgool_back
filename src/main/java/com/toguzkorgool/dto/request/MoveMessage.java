package com.toguzkorgool.dto.request;

public record MoveMessage(String roomId, String playerId, int holeIndex, long timestamp) {
}
