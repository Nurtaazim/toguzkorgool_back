package com.toguzkorgool.dto.request;

public record ChatMessage(String roomId, String playerId, String playerName, String message) {
}
