package com.toguzkorgool.dto.response;

public record GameEventMessage(String type, Object data, String reason) {

    public GameEventMessage(String type, Object data) {
        this(type, data, null);
    }
}
