package com.toguzkorgool.model.enums;

public enum PlayerSide {
    WHITE(0),
    BLACK(1);

    private final int index;

    PlayerSide(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public PlayerSide opponent() {
        return this == WHITE ? BLACK : WHITE;
    }

    public static PlayerSide fromIndex(int index) {
        return index == 0 ? WHITE : BLACK;
    }
}
