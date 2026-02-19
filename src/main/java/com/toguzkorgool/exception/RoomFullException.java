package com.toguzkorgool.exception;

public class RoomFullException extends RuntimeException {
    public RoomFullException(String roomId) {
        super("Room is full: " + roomId);
    }
}
