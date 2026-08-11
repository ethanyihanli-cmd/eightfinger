package com.macondo.eightfinger.model;

public class ChartNote {
    private double beat;
    private int laneSeed;
    private double holdBeats;

    public ChartNote(double beat, int laneSeed, double holdBeats) {
        this.beat = beat;
        this.laneSeed = laneSeed;
        this.holdBeats = holdBeats;
    }

    public double getBeat() {
        return beat;
    }

    public int getLaneSeed() {
        return laneSeed;
    }

    public double getHoldBeats() {
        return holdBeats;
    }

    public boolean isHold() {
        return holdBeats > 0;
    }
}
