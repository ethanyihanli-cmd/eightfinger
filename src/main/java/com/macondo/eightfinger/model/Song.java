package com.macondo.eightfinger.model;

import java.util.List;

public class Song {
    private String title;
    private String artist;
    private String caption;
    private GameTheme theme;
    private double bpm;
    private double previewBeats;
    private String mediaPath;
    private double targetDurationSeconds;
    private List<ChartNote> chart;
    private int[] bassPattern;
    private int[] leadPattern;

    public Song(String title, String artist, String caption, GameTheme theme,
                double bpm, double previewBeats, String mediaPath, double targetDurationSeconds,
                List<ChartNote> chart, int[] bassPattern, int[] leadPattern) {
        this.title = title;
        this.artist = artist;
        this.caption = caption;
        this.theme = theme;
        this.bpm = bpm;
        this.previewBeats = previewBeats;
        this.mediaPath = mediaPath;
        this.targetDurationSeconds = targetDurationSeconds;
        this.chart = chart;
        this.bassPattern = bassPattern;
        this.leadPattern = leadPattern;
    }

    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getCaption() { return caption; }
    public GameTheme getTheme() { return theme; }
    public double getBpm() { return bpm; }
    public double getPreviewBeats() { return previewBeats; }
    public String getMediaPath() { return mediaPath; }
    public double getTargetDurationSeconds() { return targetDurationSeconds; }
    public List<ChartNote> getChart() { return chart; }
    public int[] getBassPattern() { return bassPattern; }
    public int[] getLeadPattern() { return leadPattern; }

    public double secondsPerBeat() {
        return 60.0 / bpm;
    }

    public double durationSeconds() {
        if (targetDurationSeconds > 0) {
            return targetDurationSeconds;
        }
        double lastBeat = previewBeats;
        for (ChartNote note : chart) {
            lastBeat = Math.max(lastBeat, note.getBeat() + note.getHoldBeats() + 4);
        }
        return lastBeat * secondsPerBeat();
    }

    public int sourceLaneCount() {
        return 4;
    }

    public boolean hasMediaFile() {
        return mediaPath != null && !mediaPath.isBlank();
    }

    public boolean hasVideoBackground() {
        return hasMediaFile() && mediaPath.toLowerCase().endsWith(".mp4");
    }

    public int bassAt(int step) {
        return bassPattern[Math.floorMod(step, bassPattern.length)];
    }

    public int leadAt(int step) {
        return leadPattern[Math.floorMod(step, leadPattern.length)];
    }






}
