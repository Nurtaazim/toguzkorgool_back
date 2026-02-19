package com.toguzkorgool.exception;

public class NotPlayerTurnException extends RuntimeException {
    public NotPlayerTurnException() {
        super("It is not your turn");
    }
}
