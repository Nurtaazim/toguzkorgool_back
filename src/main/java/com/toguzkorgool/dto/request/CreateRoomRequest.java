package com.toguzkorgool.dto.request;

public record CreateRoomRequest(String playerName, String roomId, int timerSetting, boolean undoEnabled) {
}
