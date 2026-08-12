package com.macondo.eightfinger.view;

import javafx.scene.input.KeyCode;

public class Note {
    private double y;
    private KeyCode key;
    private boolean hold;
    private double totalHeight;
    private boolean activeHold;

    public Note(double y, KeyCode key, boolean hold, double totalHeight) {
        this.y = y;
        this.key = key;
        this.hold = hold;
        this.totalHeight = totalHeight;
        this.activeHold = false;
    }

    public double getY() {
        return y;
    }

    public void move(double amount) {
        y += amount;
    }

    public KeyCode getKey() {
        return key;
    }

    public boolean isHold() {
        return hold;
    }

    public double getTotalHeight() {
        return totalHeight;
    }

    public boolean isActiveHold() {
        return activeHold;
    }

    public void setActiveHold(boolean activeHold) {
        this.activeHold = activeHold;
    }

    public double getHeadCenterY() {
        return y + 13;
    }

    public double getTailY() {
        return y + totalHeight;
    }



}
