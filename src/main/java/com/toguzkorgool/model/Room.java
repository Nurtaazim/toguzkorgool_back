package com.toguzkorgool.model;

import com.toguzkorgool.model.enums.RoomStatus;

public class Room {

    private final String roomId;
    private Player player1;
    private Player player2;
    private RoomStatus status;
    private int timerSetting;
    private boolean undoEnabled;

    public Room(String roomId, Player player1, int timerSetting, boolean undoEnabled) {
        this.roomId = roomId;
        this.player1 = player1;
        this.status = RoomStatus.WAITING;
        this.timerSetting = timerSetting;
        this.undoEnabled = undoEnabled;
    }

    public String getRoomId() {
        return roomId;
    }

    public Player getPlayer1() {
        return player1;
    }

    public void setPlayer1(Player player1) {
        this.player1 = player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public void setPlayer2(Player player2) {
        this.player2 = player2;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }

    public int getTimerSetting() {
        return timerSetting;
    }

    public void setTimerSetting(int timerSetting) {
        this.timerSetting = timerSetting;
    }

    public boolean isUndoEnabled() {
        return undoEnabled;
    }

    public void setUndoEnabled(boolean undoEnabled) {
        this.undoEnabled = undoEnabled;
    }

    public boolean isFull() {
        return player1 != null && player2 != null;
    }
}
