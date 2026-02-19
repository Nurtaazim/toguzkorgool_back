package com.toguzkorgool.dto.response;

public record DrawEventMessage(String type, String from, Boolean accept) {

    public static DrawEventMessage offer(String from) {
        return new DrawEventMessage("DRAW_OFFER", from, null);
    }

    public static DrawEventMessage response(String from, boolean accept) {
        return new DrawEventMessage("DRAW_RESPONSE", from, accept);
    }
}
