package com.macondo.eightfinger;

import com.macondo.eightfinger.data.SongLibrary;
import com.macondo.eightfinger.model.Song;
import com.macondo.eightfinger.engine.ChartTransformer;
import com.macondo.eightfinger.engine.SoundEngine;
import com.macondo.eightfinger.model.*;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.media.MediaView;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import javax.print.attribute.HashDocAttributeSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

public class EightFingerGame extends Application {
    private static final double CANVAS_WIDTH = 1120;
    private static final double CANVAS_HEIGHT = 720;
    private static final double VIDEO_PANEL_WIDTH = 320;
    private static final double LANE_Y = 610;

    private static final Font TITLE_FONT = Font.font("Verdana", FontWeight.BOLD, 44);
    private static final Font SUBTITLE_FONT = Font.font("Verdana", FontWeight.NORMAL, 18);
    private static final Font PANEL_LABEL_FONT = Font.font("Verdana", FontWeight.BOLD, 14);
    private static final Font PANEL_VALUE_FONT = Font.font("Verdana", FontWeight.BOLD, 28);
    private static final Font PANEL_TEXT_FONT = Font.font("Verdana", FontWeight.NORMAL, 16);
    private static final Font MENU_FONT = Font.font("Verdana", FontWeight.NORMAL, 17);
    private static final Font KEY_FONT = Font.font("Verdana", FontWeight.BOLD, 18);

    private GameState state = GameState.MENU;
    private int selectedSongIndex = 0;
    private int selectedDifficulty = 3;
    private int selectedModeIndex = 1;
    private int menuFieldIndex = 0;


    private List<Song> songs;
    private Song selectedSong;
    private DifficultyProfile activeProfile;
    private GameMode activeMode;
    private SoundEngine soundEngine;

    private Set<KeyCode> pressedKeys new HashSet<>();

    private double pulseTime;
    private double feedbackTimer;
    private String feedbackText = "";
    private Color feedbackColor = Color.WHITE;

    public EightFingerGame() {
        this.songs = SongLibrary.builtInSongs();
        this.selectedSong = songs.get(0);
        this.activeProfile = DifficultyProfile.forLevel(selectedDifficulty);
        this.activeMode = GameMode.values() [selectedModeIndex];
        this.soundEngine = new SoundEngine();
    }

    @Override
    public void start(Stage primaryStage) {
        MediaView mediaView = new MediaView();
        mediaView.setFitWidth(VIDEO_PANEL_WIDTH);
        mediaView.setFitHeight(CANVAS_HEIGHT);
        SoundEngine.attachMediaView(mediaView);

        Canvas canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        StackPane root = new StackPane(mediaView, canvas);
        root.setFocusTraversable(true);

        Scene scene = new Scene(root);
        scene.setOnKeyPressed(e -> {
            pressedKeys.add(e.getCode());
            if (state == GameState.MENU) {
                handleMenuInput(e.getCode());
            }
        });

        scene.setOnKeyReleased(e -> pressedKeys.remove(e.getCode()));

        AnimationTimer timer = new AnimationTimer() {
            private long lastFrame;

            @Override
            public void handle(long now) {
                if (lastFrame == 0) {
                    lastFrame = now;
                }
                double delta = (now - lastFrame) / 1_000_000_000.0;
                lastFrame = now;

                pulseTime += delta;
                if (feedbackTimer > 0) {
                    feedbackTimer = Math.max(0, feedbackTimer - delta);
                }

                draw(gc);
            }

        };

        timer.start();

        primaryStage.setScene(scene);
        primaryStage.setTitle("The 8 finger challenge");
        primaryStage.show();
        root.requestFocus();
    }

    private void handleMenuInput(KeyCode key) {
        switch (key) {
            case UP -> {
                menuFieldIndex = Math.floorMod(menuFieldIndex - 1, 4);
                soundEngine.playMenuMove();
            }
            case DOWN -> {
                menuFieldIndex = Math.floorMod(menuFieldIndex + 1, 4);
                soundEngine.playMenuMove();
            }
            case LEFT -> changeMenuValue(-1);
            case RIGHT -> changeMenuValue(1);
            case S -> {
                soundEngine.toggle();
                showFeedback(soundEngine.isEnabled() ? "SOUND ON" : "SOUND OFF", currentTheme().getAccent());
            }
            case ENTER -> {
                if (menuFieldIndex == 3) {
                    startRound();
                } else {
                    changeMenuValue(1);
                }
            }
            default -> {}
        }
    }

    private void changeMenuValue(int direction) {
        switch (menuFieldIndex) {
            case 0 -> {
                selectedSongIndex = Math.floorMod(selectedSongIndex + direction, songs.size());
                selectedSong = songs.get(selectedSongIndex);
                activeProfile = DifficultyProfile.forLevel(selectedDifficulty);
            }
            case 1 -> {
                selectedDifficulty = Math.max(1, Math.min(5, selectedDifficulty + direction));
                activeProfile = DifficultyProfile.forLevel(selectedDifficulty);
            }
            case 2 -> {
                selectedModeIndex = Math.floorMod(selectedModeIndex + direction, GameMode.values().length);
                activeMode = GameMode.values()[selectedModeIndex];
            }
            case 3 -> {
                if (direction > 0) {
                    startRound();
                    return;
                }
            }
            default -> {}
        }
        soundEngine.playMenuMove();
        activeProfile = DifficultyProfile.forLevel(selectedDifficulty);
    }

    private void startRound() {
        state = GameState.PLAYING;
        soundEngine.playStart();
    }

    private void showFeedback(String text, Color color) {
        feedbackText = text;
        feedbackColor = color;
        feedbackTimer = 0.7;
    }

    private GameTheme currentTheme() {
        return selectedSong.getTheme();
    }

    private void draw(GraphicsContext gc) {
        drawBackground(gc);

        if (state == GameState.MENU) {
            drawMenu(gc);
        }

        drawFeedback(gc);
    }

    private void drawBackground(GraphicsContext gc) {
        GameTheme theme = currentTheme();

        Color top = theme.getBackgroundTop();
        Color mid = theme.getBackgroundMid();
        Color bottom = theme.getBackgroundBottom();

        for (int i = 0; i < CANVAS_HEIGHT; i++) {
            double progress = (double) i / CANVAS_HEIGHT;
            Color color;
            if (progress < 0.5) {
                double t = progress / 0.5;
                color = top.interpolate(mid, t);
                } else {
                double t = (progress - 0.5) / 0.5;
                color = mid.interpolate(bottom, t);
            }
            gc.getFill(color);
            gc.fillRect(0, i, CANVAS_WIDTH, 1);
        }

        gc.setGlobalAlpha(0.1);
        gc.setFill(theme.getAccent());
        gc.fillOval(-120, -90, 340, 220);
        gc.setFill(theme.getGlow());
        gc.fillOval(CANVAS_WIDTH - 240, 32, 320, 240);
        gc.setGlobalAlpha(1);
    }

    private void drawFeedback(GraphicsContext gc) {
        if (feedbackTimer <= 0) {
            return;
        }

        gc.setGlobalAlpha(Math.min(1, feedbackTimer * 1.6));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(feedbackColor);
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 30));
        gc.fillText(feedbackText, CANVAS_WIDTH / 2, 100 - (0.7 - feedbackTimer) * 34);
        gc.setGlobalAlpha(1);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawMenu(GraphicsContext gc) {
        GameTheme theme = currentTheme();
        DifficultyProfile previewProfile = DifficultyProfile.forLevel(selectedDifficulty);
        GameMode previewProfile = GameMode.values()[selectedModeIndex];

        gc.setFill(Color.color(0, 0, 0, 0.18));
        gc.fillRoundRect(72, 54, CANVAS_WIDTH - 144, CANVAS_HEIGHT - 108, 28, 28);

        gc.setFill(theme.getPanel());
        gc.fillRoundRect(92, 78, CANVAS_WIDTH - 184, CANVAS_HEIGHT - 144, 28, 28);
        gc.setStroke(Color.color(1, 1, 1, 0.12));
        gc.strokeRoundRect(92, 78, CANVAS_WIDTH - 184, CANVAS_HEIGHT - 144, 28, 28);

        gc.setFill(theme.getAccent());
        gc.setFont(TITLE_FONT);
        gc.fillText("THE 8 FINGER CHALLANGE", 128, 148);

        gc.setFill(Color.WHITE);
        gc.setFont(SUBTITLE_FONT);
        gc.fillText("Use UP/DOWN to move, LEFT/RIGHT to change, ENTER to start", 128, 184);

        double leftPanelX = 122;
        double leftPanelY = 214;
        double leftPaneWidth = 430;
        double rowHeight = 82;
        double rowGap = 10;

        drawMenuRow(gc, 0, "TRACK", selectedSong.getTitle(),
                selectedSong.getArtist().isBlank() ? selectedSong.getCaption() : selectedSong.getArtist(),
                leftPanelX, leftPanelY, leftPanelWidth, rowHeight, theme);

        drawMenuRow(gc, 1, "DIFFICULTY", String.valueOf(selectedDifficulty),
                "lanes" + previewProfile.getKeys().size() + "    bpm " + (int) selectedSong.getBpm(),
                leftPanelX, leftPanelY + (rowHeight + rowGap), leftPanelWidth, rowHeight, theme);

        drawMenuRow(gc, 2, "MODE", previewMode.getLabel(),
                previewMode.getDescription() + "    sound " + (soundEngine.isEnabled() ? "on" : "off"),
                leftPanelX, leftPanelY + (rowHeight + rowGap) * 2, leftPanelWidth, rowHeight, theme);

        drawMenuRow(gc, 3, "START", "Play",
                "press ENTER to launch this chart",
                leftPanelX, leftPanelY + (rowHeight + rowGap) * 3, leftPanelWidth, rowHeight, theme);

        gc.setFont(MENU_FONT);
        gc.setFill(Color.color(1, 1, 1, 0.75));
        gc.fillText("S toggle sound", 790, 140);
        gc.fillText("MP4 lyric video on the right in game", 720, 166);

        drawSongPreview(gc, selectedSong, previewProfile);
    }

    private void drawMenuRow(GraphicsContext gc, int rowIndex, String label, String value, String detail,
                             double x, double y, double width, double height, GameTheme theme) {
        boolean selected = menuFieldIndex == rowIndex;

        gc.setFill(selected ? theme.getAccent().deriveColor(0, 1, 1, 0.17) : Color.color(1, 1, 1, 0.05));
        gc.fillRoundRect(x, y, width, height, 18, 18);
        gc.setStroke(selected ? theme.getGlow().deriveColor(0, 1, 1, 0.8) : Color.color(1, 1, 1, 0.08));
        gc.strokeRoundRect(x, y, width, height, 18, 18);

        gc.setFont(PANEL_LABEL_FONT);
        gc.setFill(selected ? theme.getGlow() : Color.color(1, 1, 1, 0.72));
        gc.fillText(label, x + 18, y + 24);

        gc.setFont(PANEL_VALUE_FONT);
        gc.setFill(Color.WHITE);
        gc.fillText(value, x + 18, y + 54);

        gc.setFont(PANEL_TEXT_FONT);
        gc.setFill(Color.color(1, 1, 1, 0.78));
        gc.fillText(detail, x + 18, y + 74);

    }

    private void drawSongPreview(GraphicsContext gc, Song song, DifficultyProfile previewProfile) {
        double previewX = 612;
        double previewY = 216;
        double previewWidth = 250;
        double previewHeight = 286;

        gc.setFill(Color.color(1, 1, 1, 0.05));
        gc.fillRoundRect(previewX, previewY, previewWidth, previewHeight, 24, 24);

        int laneCourt = Math.min(4, previewProfile.getKeys().size());

        for (int i = 0; i < laneCourt; i++) {
            double laneX = previewX + 22 + i * 52;
            gc.setFill(song.getTheme().laneColor(i).deriveColor(0, 1, 1, 0.26));
            gc.fillRoundRect(laneX, previewY + 18, 40, previewHeight - 36, 16, 16);
        }

        int previewNotes = Math.min(12, song.getChart().size());
        for (int i = 0; i < previewNotes; i++) {
            ChartNote note = song.getChart().get(i);
            int lane = Math.floorMod(note.getLaneSeed(), laneCount);
            double x = previewX + 28 + lane * 52;
            double y = previewY + 36 + i * 16;
            gc.setFill(song.getTheme().laneColor(lane));
            gc.fillRoundRect(x, y, 28, note.isHold() ? 42 : 16, 10, 10);
        }

        gc.setStroke(song.getTheme().getGlow());
        gc.setLineWidth(4);
        gc.strokeLine(previewX + 16, previewY + previewHeight - 34,
                previewX + previewWidth - 16, previewY + previewHeight - 34);
    }









    @Override
    public void stop() {
        if (soundEngine != null) {
            soundEngine.stopBackingTrack();
        }
    }


    public static void main(String[] args) {
        launch(args);
    }


}
