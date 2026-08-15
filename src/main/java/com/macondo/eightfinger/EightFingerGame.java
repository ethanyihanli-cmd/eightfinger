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

        System.out.println("Level " + selectedDifficulty + " - " + activeChart.size() + " notes");

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

        System.out.println("Miss ! Total misses: " + misses + " / " + activeMode.getMissLimit());

        if (misses >= activeMode.getMissLimit()) {
            soundEngine.stopBackingTrack();
            state = GameState.GAME_OVER;
            showFeedback("GAME OVER", currentTheme().getWarning());
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
          gc.setFill(color);
          gc.fillRect(0, i, CANVAS_WIDTH, 1);
      }

      if (soundEngine.hasActiveVideoBackground()) {
          double panelX = playfieldWidth() + 18;
          double panelWidth = CANVAS_WIDTH - panelX - 18;
          gc.setGlobalAlpha(0.26);
          gc.setFill(theme.getBackgroundMid());
          gc.fillRect(playfieldWidth(), 0, CANVAS_WIDTH - playfieldWidth(), CANVAS_HEIGHT);
          gc.setGlobalAlpha(1);

          gc.setFill(Color.color(0.02, 0.05, 0.08, 0.14));
          gc.fillRoundRect(panelX, 22, panelWidth, CANVAS_HEIGHT - 44, 28, 28);
          gc.setStroke(theme.getGlow().deriveColor(0, 1, 1, 0.55));
          gc.strokeRoundRect(panelX, 22, panelWidth, CANVAS_HEIGHT - 44, 28, 28);
      }

      gc.setGlobalAlpha(0.18);
      gc.setFill(theme.getAccent());
      gc.fillOval(-120, -90, 340, 220);
      gc.setFill(theme.getGlow());
      gc.fillOval(CANVAS_WIDTH - 240, 32, 320, 240);
      gc.setFill(theme.laneColor(3));
      gc.fillOval(110, CANVAS_HEIGHT - 160, 420, 210);
      gc.setGlobalAlpha(1);
    }

    private void drawMenu(GraphicsContext gc) {
        GameTheme theme = currentTheme();
        DifficultyProfile previewProfile = DifficultyProfile.forLevel(selectedDifficulty);
        GameMode previewMode = GameMode.values()[selectedModeIndex];

        gc.setFill(Color.color(0, 0, 0, 0.18));
        gc.fillRoundRect(72, 54, CANVAS_WIDTH - 144, CANVAS_HEIGHT - 108, 28, 28);

        gc.setFill(theme.getPanel());
        gc.fillRoundRect(92, 78, CANVAS_WIDTH - 184, CANVAS_HEIGHT - 144, 28, 28);
        gc.setStroke(Color.color(1, 1, 1, 0.12));
        gc.strokeRoundRect(92, 78, CANVAS_WIDTH - 184, CANVAS_HEIGHT - 144, 28, 28;

        gc.setFill(theme.getAccent());
        gc.setFont(TITLE_FONT);
        gc.fillText("THE 8 FINGER CHALLENGE", 128, 148);

        gc.setFill(Color.WHITE);
        gc.setFont(SUBTITLE_FONT);
        gc.fillText("Use UP/DOWN to move, LEFT/RIGHT to change, ENTER to start", 128, 184);

        double leftPanelX = 122;
        double leftPanelY = 214;
        double leftPanelWidth = 430;
        double rowHeight = 82;
        double rowGap = 10;

        drawMenuRow(gc, 0, "TRACK", selectedSong.getTitle(),
                selectedSong.getArtist().isBlank() ? selectedSong.getCaption() : selectedSong.getArtist(),
                leftPanelX, leftPanelY, leftPanelWidth, rowHeight, theme);

        drawMenuRow(gc, 1, "DIFFICULTY", String.valueOf(selectedDifficulty),
                "lanes " + previewProfile.getKeys().size() + "    bpm" + (int) selectedSong.getBpm(),
                leftPanelX, leftPanelY + (rowHeight + rowGap), leftPanelWidth, rowHeight, theme);

        drawMenuRow(gc, 2, "MODE", previewMode.getLabel(),
                previewMode.getDescription() + "    sound" + (soundEngine.isEnabled() ? "on" : "off"),
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

    private void drawMenuRow(GraphicsContext gc, int rowIndex, String label String value, String detail,
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

    private void drawLanes(GraphicsContext gc) {

    }

    private void drawHitLine(GraphicsContext gc) {
        GameTheme theme = currentTheme();
        double glow = 0.55 + 0.45 * Math.sin(pulseTime * 6);

        gc.setStroke(Color.color(1, 1, 1, 0.18));
        gc.strokeLine(84, LANE_Y + 10, playfieldWidth() - 84, LANE_Y + 10);

        gc.getStroke(theme.getGlow().deriveColor(0, 1, 1, 0.35 + 0.35 * glow));
        gc.setLineWidth(8);
        gc.strokeLine(84, LANE_Y, playfieldWidth() - 84, LANE_Y);

        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.strokeLine(84, LANE_Y, playfieldWidth() - 84, LANE_Y);
    }

    private void drawNotes(GraphicsContext gc) {
        for (Note note : notes) {
            int laneIndex = activeProfile.getKeys().indexOf(note.getKey());
            if (laneIndex < 0) {
                continue;
            }

            double x = laneX(laneIndex);
            Color laneColor = currentTheme().laneColor(laneIndex);

            if (note.isHold()) {
                double bodyHeight = Math.max(0, note.getTotalHeight() - 26);
                gc.setFill(laneColor.deriveColor(0, 1, 1, note.isActiveHold() ? 0.55 : 0.3));
                gc.fillRoundRect(x + 16, note.getY() + 26 - 4, NOTE_WIDTH - 32, bodyHeight + 8, 12, 12);
            }

            gc.setFill(laneColor.deriveColor(0, 1, 1.25, 0.18));
            gc.fillRoundRect(x - 4, note.getY() - 5, NOTE_WIDTH + 8, 26 + 10, 16, 16);

            gc.setFill(laneColor.brighter());
            gc.fillRoundRect(x, note.getY(), NOTE_WIDTH, 26, 14, 14);
            gc.setStroke(Color.color(1, 1, 1, 0.42));
            gc.strokeRoundRect(x, note.getY(), NOTE_WIDTH, 26, 14,14);
        }
    }

    private void drawHud(GraphicsContext gc) {
        GameTheme theme = currentTheme();

        gc.setFill(theme.getPanel());
        gc.fillRoundRect(24, 24, 260, 176, 24, 24);
        gc.setStroke(Color.color(1, 1, 1, 0.12));
        gc.strokeRoundRect(24, 24, 260, 176, 24, 24);

        gc.setFill(theme.getAccent());
        gc.setFont(PANEL_LABEL_FONT);
        gc.fillText(selectedSong.getTitle().toUpperCase(), 42, 48);

        gc.setFill(Color.WHITE);
        gc.setFont(PANEL_VALUE_FONT);
        gc.fillText(String.valueOf(score), 42, 84);

        gc.setFont(PANEL_TEXT_FONT);
        gc.fillText("combo  " + combo, 42, 114);
        gc.fillText("mode  " + activeMode.getLabel(), 42, 138);
        gc.fillText("miss  " + misses + "/" + (activeMode == GameMode.PRACTICE ? "INF" : String.valueOf(activeMode.getMissLimit())), 42, 162);
        gc.fillText(selectedSong.getArtist().isBlank() ? selectedSong.getCaption() : "artist " + selectedSong.getArtist(), 42, 186);
    }

    private void drawGameOver(GraphicsContext gc) {
        GameTheme theme = currentTheme();

        gc.setFill(Color.color(0, 0, 0, 0.58));
        gc.fillRoundRect(CANVAS_WIDTH / 2 - 186, CANVAS_HEIGHT / 2 - 104, 372, 212, 28, 28);
        gc.setStroke(Color.color(1, 1, 1, 0.15));
        gc.strokeRoundRect(CANVAS_WIDTH / 2 - 186, CANVAS_HEIGHT / 2 - 104, 372, 212, 28, 28);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(theme.getWarning());
        gc.setFont(TITLE_FONT);
        gc.fillText("RUN OVER", CANVAS_WIDTH / 2, CANVAS_HEIGHT / 2 - 30);

        gc.setFill(Color.WHITE);
        gc.setFont(SUBTITLE_FONT);
        gc.fillText("score  " + score + "    combo  " + combo, CANVAS_WIDTH / 2, CANVAS_HEIGHT / 2 + 12);
        gc.fillText("R to replay    ENTER for menu", CANVAS_WIDTH / 2, CANVAS_HEIGHT / 2 + 52);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawSongPreview(GraphicsContext gc, int rowIndex, String label, String value, String detail,
                                 double x, double y, double width, double height, GameTheme theme) {
        double previewX = 612;
        double previewY = 216;
        double previewWidth = 250;
        double previewHeight = 286;

        gc.setFill(Color.color(1, 1, 1, 0.05));
        gc.fillRoundRect(previewX, previewY, previewWidth, previewHeight, 24, 24);

        int laneCount = Math.min(4, previewProfile.getKeys().size());

        for (int i = 0; i < laneCount; i++) {
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

    private void drawLanes(GraphicsContext gc) {
        double pulse = 0.6 + 0.4 * Math.sin(pulseTime * 2.8) {
            Color laneColor = currentTheme().laneColor(i);

            gc.setFill(laneColor.deriveColor(0, 1, 1.1, 0.3));
            gc.fillRoundRect(x, 34, NOTE_WIDTH, CANVAS_HEIGHT - 68, 18, 18);

            gc.setStroke(Color.color(1, 1, 1, 0.12));
            gc.strokeRoundRect(x, 34, NOTE_WIDTH, CANVAS_HEIGHT - 68, 18, 18);

            gc.setFill(laneColor.deriveColor(0, 1, 1, 0.22 + 0.14 * pulse));
            gc.fillRoundRect(x + 6, 48, NOTE_WIDTH - 12, 18, 10, 10);

            gc.setTextAlign(TextAlignment.CENTER);
            gc.setFill(Color.WHITE);
            gc.setFont(KEY_FONT);
            gc.fillText(activeProfile.getKeys().get(i).getName(), x + NOTE_WIDTH / 2, LANE_Y - 18);
        }
        gc.setTextAlign(TextAlignment.LEFT);
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
