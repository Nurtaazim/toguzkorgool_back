package com.toguzkorgool.dto.response;

public record NewGameEventMessage(String type, String from, Boolean accept) {

    public static NewGameEventMessage request(String from) {
        return new NewGameEventMessage("NEW_GAME_REQUEST", from, null);
    }

    public static NewGameEventMessage response(String from, boolean accept) {
        return new NewGameEventMessage("NEW_GAME_RESPONSE", from, accept);
    }
}
