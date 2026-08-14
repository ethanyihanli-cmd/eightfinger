package com.macondo.eightfinger;

import com.macondo.eightfinger.data.SongLibrary;
import com.macondo.eightfinger.model.Song;
import com.macondo.eightfinger.engine.ChartTransformer;
import com.macondo.eightfinger.engine.SoundEngine;
import com.macondo.eightfinger.model.*;
import com.macondo.eightfinger.view.Note;
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
import java.util.Iterator;

public class EightFingerGame extends Application {
    private static final double CANVAS_WIDTH = 1120;
    private static final double CANVAS_HEIGHT = 720;
    private static final double VIDEO_PANEL_WIDTH = 320;
    private static final double LANE_Y = 610;
    private static final double NOTE_WIDTH = 60;
    private static final double LANE_GAP = 12;
    private static final double HIT_WINDOW = 48;
    private static final double RELEASE_WINDOW = 14;
    private static final double MISS_WINDOW = 42;

    private static final Font TITLE_FONT = Font.font("Verdana", FontWeight.BOLD, 44);
    private static final Font SUBTITLE_FONT = Font.font("Verdana", FontWeight.NORMAL, 18);
    private static final Font PANEL_LABEL_FONT = Font.font("Verdana", FontWeight.BOLD, 14);
    private static final Font PANEL_VALUE_FONT = Font.font("Verdana", FontWeight.BOLD, 28);
    private static final Font PANEL_TEXT_FONT = Font.font("Verdana", FontWeight.NORMAL, 16);
    private static final Font MENU_FONT = Font.font("Verdana", FontWeight.NORMAL, 17);
    private static final Font KEY_FONT = Font.font("Verdana", FontWeight.BOLD, 18);
    private static final Font MENU_FONT = Font.font("Verdana", FontWeight.NORMAL, 17);

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
    private List<ChartNote> activeChart;
    private List<Note> notes;
    private Set<KeyCode> pressedKeys;

    private int score;
    private int combo;
    private int misses;
    private int nextChartIndex;
    private double songTime;

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
        this.notes = new ArrayList<>();
        this.pressedKeys = new HashSet<>();
        this.activeChart = new ArrayList<>();
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
            } else if (state == GameState.PLAYING) {
                handlePlayPress(e.getCode());
            } else if (state == GameState.GAME_OVER) {
                handleGameOverInput(e.getCode());
            }
        });

        scene.setOnKeyReleased(e -> {
            pressedKeys.remove(e.getCode());
            if (state == GameState.PLAYING) {
                handlePlayRelease(e.getCode());
            }
        });

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

                if (state == GameState.PlAYING) {
                    updateGame(delta);
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

    private void handlePlayPress(KeyCode key) {
        if (key == KeyCode.R) {
            startRound();
        } else if (key == KeyCode.ENTER || key == KeyCode.ESCAPE) {
            state = GameState.MENU;
            notes.clear();
            pressedKeys.clear();
            soundEngine.stopBackingTrack();
            showFeedback("", Color.WHITE);
            soundEngine.playMenuMove();
        }
    }

    private void handlePlayPress(KeyCode key) {
        if (!activeProfile.getKeys().contains(key)) {
            return;
        }

        Note note = findHittableNote(key);
        if (note == null) {
            registerMiss(Judgement.MISS);
            return;
        }

        Judgement judgement = judge(note);
        if (note == null) {
            registerMiss(Judgement.MISS);
            return;
        }

        score += judgement.getPoints() + combo / 4;
        combo++;
        showFeedback(judgement.getLabel(), judgement.getColor());
        soundEngine.playJudgement(judgement);

        if (note.isHold()) {
            note.setActiveHold(true);
        } else {
            notes.remove(note);
        }
    }

    private void handlePlayRelease(KeyCode key) {
        Note held = findHeldNote(key);
        if (held == null) {
            return;
        }
        if (held.getTailY() >= LANE_Y - RELEASE_WINDOW) {
            finishHold(held);
            return;
        }
        notes.remove(held);
        registerMiss(Judgement.EARLY);
    }

    private void startRound() {
        activeProfile = DifficultyProfile.forLevel(selectedDifficulty);
        activeMode = GameMode.values()[selectedModeIndex];
        activeChart = ChartTransformer.forDifficulty(selectedSong, activeProfile);

        state = GameState.PLAYING;
        notes.clear();
        pressedKeys.clear();
        score = 0;
        combo = 0;
        misses = 0;
        nextChartIndex = 0;
        songTime = 0;

        soundEngine.startBackingTrack(selectedSong);
        showFeedback("START", currentTheme().getAccent());
        soundEngine.playStart();
    }

    private void updateGame(double delta) {
        songTime += delta;
        spawnChartNotes();

        Iterator<Note> iterator = notes.iterators();
        while (iterator.hasNext()) {
            Note note = iterator.next();
            note.move(avtiveProfile.getNoteSpeed() * delta);

            if (note.isActiveHold()) {
                if (!pressedKeys.contains(note.getKey())) {
                    iterator.remove();
                    registerMiss(Judgement.EARLY);
                    continue;
                }
                if (note.getTailY() >= LANE_Y) {
                    iterator.remove();
                    rewardHold();
                }
                continue;
            }

            if (note.getHeadCenterY() > LANE_Y + MISS_WINDOW) {
                iterator.remove();
                registerMiss(Judgement.DROP);
            }
        }

        if (nextChartIndex >= activeChart.size() && notes.isEmpty() && songTime > selectedSong.durationSeconds()) {
            soundEngine.stopBackingTrack();
            state = GameState.GAME_OVER;
            showFeedback("CLEAR", currentTheme().getAccent());
        }
    }

    private void spawnChartNotes() {
        double travelTime = (LANE_Y + 13) / activeProfile.getNoteSpeed();

        while (nextChartIndex < activeChart.size()) {
            ChartNote chartNote = activeChart.get(nextChartIndex);
            double noteTime = chartNote.getBeat() * selectedSong.secondsPerBeat();

            if (noteTime - travelTime > songTime) {
                break;
            }

            int laneIndex = Math.floorMod(chartNote.getLaneSeed(), activeProfile.getKeys().size());
            KeyCode key = activeProfile.getKeys().get(laneIndex);

            double holdHeight = chartNote.isHold()
                    ? 26 + chartNote.getHoldBeats() * selectedSong.secondsPerBeat() * activeProfile.getNoteSpeed()
                    : 26;

            notes.add(new Note(-26, key, chartNote.isHold(), holdHeight));
            nextChartIndex++;

        }
    }

    private Note findHittableNote(KeyCode key) {
        for (Note note : notes) {
            if (note.getKey() == key && !note.isActiveHold() && judge(note) != null) {
                return note;
            }
        }
        return null;
    }

    private Note findHeldNote(KeyCode key) {
        for (Note note : notes) {
            if (note.getKey() == key && note.isActiveHold()) {
                return note;
            }
        }
        return null;
    }

    private Judgement judge (Note note) {
        double distance = Math.abs(note.getHeadCenterY() - LANE_Y);
        if (distance <= 9) {
            return Judgement.PERFECT;
        }
        if (distance <= 18) {
            return Judgement.EXCELLENT;
        }
        if (distance <= 30) {
            return Judgement.GREAT;
        }
        if (distance <= HIT_WINDOW) {
            return Judgement.GOOD;
        }
        return null;
    }

    private void rewardHold() {
        score += Judgement.HOLD.getPoints() + combo / 5;
        showFeedback(Judgement.HOLD.getLabel(), Judgement.HOLD.getColor());
        soundEngine.playJudgement(Judgement.HOLD);
    }

    private void finishHold(Note note) {
        notes.remove(note);
        rewardHold();
    }

    private void registerMiss(Judgement judgement) {
        if (state != GameState.PLAYING) {
            return;
        }

        combo = 0;
        misses++;
        showFeedback(judgement.getLabel(), judgement.getColor());
        soundEngine.playMiss();

        if (misses >= activeMode.getMissLimit()) {
            soundEngine.stopBackingTrack();
            state = GameState.GAME_OVER;
        }
    }

    private void showFeedback(String text, Color color) {
        feedbackText = text;
        feedbackColor = color;
        feedbackTimer = 0.7;
    }

    private GameTheme currentTheme() {
        return selectedSong.getTheme();
    }

    private double playfieldWidth() {
        return CANVAS_WIDTH - (soundEngine.hasActiveVideoBackground() ? VIDEO_PANEL_WIDTH : 0);
    }

    private double laneX(int laneIndex) {
        double totalWidth = activeProfile.getKeys().size() * NOTE_WIDTH + (activeProfile.getKeys().size() - 1) * LANE_GAP;
        return (playfieldWidth() - totalWidth) / 2 + laneIndex * (NOTE_WIDTH + LANE_GAP);
    }

    private void draw(GraphicsContext gc) {
        drawBackground(gc);

        if (state == GameState.MENU) {
            drawMenu(gc);
        } else {
            drawGameplay(gc);
        }

        drawFeedback(gc);
    }

    private void drawFeedback(GraphicsContext gc) {
        if (feedbackTimer <= 0) {
            return;
        }

        gc.setGlobalAlpha(Math.min(1, feedbackTimer * 1.6));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(feedbackColor);
        gc.setFont(FEEDBACK_FONT);
        gc.fillText(feedbackText, CANVAS_WIDTH / 2, 100 - (0.7 - feedbackTimer) * 34);
        gc.setGlobalAlpha(1);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawGameplay(GraphicsContext gc) {
        drawLanes(gc);
        drawHitLine(gc);
        drawNotes(gc);
        drawHud(gc);

        if (state == GameState.GAME_OVER) {
            drawGameOver(gc);
        }
    }

    //work later

    private void drawBackground(GraphicsContext gc) {

    }

    private void drawMenu(GraphicsContext gc) {

    }

    private void drawLanes(GraphicsContext gc) {

    }

    private void drawHitLine(GraphicsContext gc) {

    }

    private void drawNotes(GraphicsContext gc) {

    }

    private void drawHud(GraphicsContext gc) {

    }

    private void drawGameOver(GraphicsContext gc) {

    }

    private void drawSongPreview(GraphicsContext gc, int rowIndex, String label, String value, String detail,
                                 double x, double y, double width, double height, GameTheme theme) {

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
