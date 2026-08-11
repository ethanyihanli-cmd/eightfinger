package com.macondo.eightfinger.model;

public enum GameMode {
    PRACTICE("Practice", Integer.MAX_VALUE, "unlimited misses"),
    NORMAL("Normal", 10, "10 misses max");

    private String label;
    private int missLimit;
    private String description;

    GameMode(String label, int missLimit, String description) {
        this.label = label;
        this.missLimit = missLimit;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public int getMissLimit() {
        return missLimit;
    }

    public String getDescription() {
        return description;
    }
}
