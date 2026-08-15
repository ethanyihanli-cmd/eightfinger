package com.macondo.eightfinger.engine;

import com.macondo.eightfinger.model.Judgement;
import com.macondo.eightfinger.model.Song;
import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;


public class SoundEngine {
    private static final float SAMPLE_RATE = 44100;

    private ExecutorService effectsWorker;
    private AtomicBoolean backingTrackRunning;
    private boolean enabled;
    private Thread backingTrackThread;
    private MediaPlayer backingPlayer;
    private MediaView mediaView;
    private boolean videoBackgroundActive;

    public SoundEngine() {
        this.effectsWorker = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "sound-engine");
            thread.setDaemon(true);
            return thread;
        });
        this.backingTrackRunning = new AtomicBoolean(false);
        this.enabled = true;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        enabled = !enabled;
        if (!enabled) {
            stopBackingTrack();
        }
    }

    public void playMenuMove() {
        playTone(520, 55, 0.16);
    }

    public void playStart() {
        playTone(620, 90, 0.18);
    }

    public void playJudgement(Judgement judgement) {
        int millis = ( judgement == Judgement.HOLD) ? 110 : 75;
        playTone(judgement.getToneHz(), millis, 0.18);
    }

    public void playMiss() {
        playTone(220, 120, 0.18);
    }

    private void playTone(int hz, int millis, double volume) {
        if (!enabled) {
            return;
        }
        effectsWorker.submit(() -> emitTone(hz, millis, volume));
    }

    public void startBackingTrack(Song song) {
        stopBackingTrack();

        if (!enabled) {
            return;
        }

        backingTrackRunning.set(true);
        Thread thread = new Thread(() -> runBackingTrack(song), "backing-track");
        thread.setDaemon(true);
        backingTrackThread = thread;
        thread.start();
    }

    public void attachMediaView(MediaView mediaView) {
        this.mediaView = mediaView;
        if (mediaView != null) {
            mediaView.setPreserveRatio(false);
        }
    }

    public boolean hasActiveVideoBackground() {
        return videoBackgroundActive;
    }

    public void stopBackingTrack() {
        backingTrackRunning.set(false);
        videoBackgroundActive = false;

        Thread thread = backingTrackThread;
        backingTrackThread = null;
        if (thread != null) {
            thread.interrupt();
        }

        MediaPlayer player = backingPlayer;
        backingPlayer = null;
        if (player != null) {
            try {
                player.stop();
            } finally {
                player.dispose();
            }
        }

        MediaView currentMediaView = mediaView;
        if (currentMediaView != null) {
            currentMediaView.setMediaPlayer(null);
        }
    }

    private void runBackingTrack(Song song) {
        if (tryStartMedia(song)) {
            while (enabled && backingTrackRunning.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            return;
        }

        long stepMillis = Math.max(110, Math.round(song.secondsPerBeat() * 1000));
        int step = 0;

        while (enabled && backingTrackRunning.get() && !Thread.currentThread().isInterrupted()) {
            int bass = song.bassAt(step / 2);
            int lead = song.leadAt(step);

            emitTone(bass, (int) Math.min(220, stepMillis), 0.09);
            if (step % 2 == 0) {
                int duration = (int) Math.min(160, stepMillis * 0.75);
                double volume = (step % 4 == 0) ? 0.12 : 0.08;
                emitTone(lead, duration, volume);
            }

            if (step % 4 == 3) {
                emitTone(lead + 12, (int) Math.min(120, stepMillis * 0.55), 0.06);
            }

            long sleepMillis = Math.max(25, stepMillis - 140);
            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            step++;
        }
    }

    private boolean tryStartMedia(Song song) {
        if (!song.hasMediaFile()) {
            return false;
        }

        Path path = Paths.get(song.getMediaPath());
        if (!path.isAbsolute()) {
            path = Paths.get("").resolve(path).normalize();
        }

        String mediaUri;
        if (Files.exists(path)) {
            mediaUri = path.toUri().toString();
        } else {
            URL resource = SoundEngine.class.getResource("/com/macondo/eightfinger/" + song.getMediaPath());
            if (resource == null) {
                resource = SoundEngine.class.getResource("/" + song.getMediaPath());
            }
            if (resource == null) {
                return false;
            }
            mediaUri = resource.toString();
        }

        Runnable startMedia = () -> {
            try {
                MediaPlayer player = new MediaPlayer(new Media(mediaUri));
                MediaView currentMediaView = mediaView;
                videoBackgroundActive = song.hasVideoBackground() && currentMediaView != null;

                player.setOnEndOfMedia(() -> {
                    backingTrackRunning.set(false);
                    videoBackgroundActive = false;
                });

                player.setOnError(() -> videoBackgroundActive = false);

                backingPlayer = player;
                backingTrackRunning.set(true);

                if (currentMediaView != null) {
                    currentMediaView.setMediaPlayer(player);
                }

                player.play();
            } catch (Exception ignored) {
                MediaPlayer player = backingPlayer;
                backingPlayer = null;
                videoBackgroundActive = false;
                if (player != null) {
                    player.dispose();
                }
            }
        };

        if (Platform.isFxApplicationThread()) {
            startMedia.run();
            return backingPlayer != null;
        }

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean started = new AtomicBoolean(false);

        Platform.runLater(() -> {
            try {
                startMedia.run();
                started.set(backingPlayer != null);
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        return started.get();


    }

    private void emitTone(int hz, int millis, double volume) {
        if (hz <= 0) {
            return;
        }

        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            byte[] buffer = new byte[(int) (SAMPLE_RATE * millis / 1000) * 2];
            for (int i = 0; i < buffer.length / 2; i++) {
                double angle = (i / (SAMPLE_RATE / hz)) * 2.0 * Math.PI;
                short sample = (short) (Math.sin(angle) * Short.MAX_VALUE * volume);
                buffer[i * 2] = (byte) (sample & 0xff);
                buffer[i * 2 + 1] = (byte) ((sample >> 8) & 0xff);
            }

            try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
                line.open(format);
                line.start();
                line.write(buffer, 0, buffer.length);
                line.drain();
            }
        } catch (Exception ignored) {

        }
    }

}
