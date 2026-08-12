package com.macondo.eightfinger;

import com.macondo.eightfinger.data.SongLibrary;
import com.macondo.eightfinger.model.Song;
import com.macondo.eightfinger.engine.ChartTransformer;
import com.macondo.eightfinger.engine.SoundEngine;
import com.macondo.eightfinger.model.ChartNote;
import com.macondo.eightfinger.model.DifficultyProfile;
import com.macondo.eightfinger.model.GameState;
import com.macondo.eightfinger.model.GameMode;
import com.macondo.eightfinger.model.Judgement;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.List;

public class EightFingerGame extends Application {
    private List<Song> songs;

    @Override
    public void start(Stage primaryStage) {
        songs = SongLibrary.builtInSongs();
        soundEngine = new SoundEngine();

        soundEngine.playMenuMove();
        System.out.println("Test");

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #121922;");

        Label label = new Label("The 8 Finger Challange");
        label.setStyle("-fx-text-fill: #d8e1ea; -fx-font-size: 36px; -fx-font-weight: bold;");
        label.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        root.getChildren().add(label);

        Scene scene = new Scene(root, 1120, 720, Color.web("#121922"));
        primaryStage.setTitle("The 8 Finger Challange");
        primaryStage.setScene(scene);
        primaryStage.show();
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
