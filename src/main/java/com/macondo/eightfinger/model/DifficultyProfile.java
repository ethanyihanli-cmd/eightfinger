package com.macondo.eightfinger.model;

import javafx.scene.input.KeyCode;

import java.util.List;

public class DifficultyProfile {
    private int level;
    private double noteSpeed;
    private double spawnInterval;
    private double holdChance;
    private List<KeyCode> keys;

    public DifficultyProfile(int level, double noteSpeed, double spawnInterval, double holdChance, List<KeyCode> keys) {
        this.level = level;
        this.noteSpeed = noteSpeed;
        this.spawnInterval = spawnInterval;
        this.holdChance = holdChance;
        this.keys = keys;
    }

    public int getLevel() {
        return level;
    }

    public double getNoteSpeed() {
        return noteSpeed;
    }

    public double getSpawnInterval() {
        return spawnInterval;
    }

    public double getHoldChance() {
        return holdChance;
    }

    public List<KeyCode> getKeys() {
        return keys;
    }

    public static DifficultyProfile forLevel(int level) {
        switch (level) {
            case 1:
                return new DifficultyProfile(1, 165, 0.8, 0.1, List.of(KeyCode.J));
            case 2:
                return new DifficultyProfile(2, 210, 0.62, 0.14, List.of(KeyCode.F, KeyCode.J));
            case 3:
                return new DifficultyProfile(3, 255, 0.46, 0.18, List.of(KeyCode.D, KeyCode.F, KeyCode.J, KeyCode.K));
            case 4:
                return new DifficultyProfile(4, 305, 0.36, 0.22, List.of(KeyCode.S, KeyCode.D, KeyCode.F, KeyCode.J, KeyCode.K, KeyCode.L));
            case 5:
                return new DifficultyProfile(5, 355, 0.3, 0.28, List.of(KeyCode.A, KeyCode.S, KeyCode.D, KeyCode.F, KeyCode.H, KeyCode.J, KeyCode.K, KeyCode.L));
            default:
                return forLevel(3);

        }

    }



}
