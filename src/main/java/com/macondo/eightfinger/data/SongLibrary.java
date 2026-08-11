package com.macondo.eightfinger.data;

import com.macondo.eightfinger.model.ChartNote;
import com.macondo.eightfinger.model.GameTheme;
import com.macondo.eightfinger.model.Song;

import java.util.ArrayList;
import java.util.List;

public class SongLibrary {
    private SongLibrary() {
        //work after
    }

    public static List<Song> builtInSongs() {
        return List.of(
                buildJustTheTwoOfUs(),
                buildShadowOfTheSun(),
                buildFlowers()
        );
    }

    private static Song buildJustTheTwoOfUs() {
        List<ChartNote> chart = newArrayList<>();

        addPhrase(chart, 0, new int[]{0, 1, 2, 1, 3, 2, 1, 0});
        addPhrase(chart, 8, new int[]{0, 2, 1, 3, 2, 1, 0, 1});
        addHold(chart, 14, 2, 2);

        addPhrase(chart, 16, new int[]{1, 2, 3, 2, 1, 0, 1, 2});
        addPhrase(chart, 24, new int[]{3, 2, 1, 0, 1, 2, 3, 1});
        addHold(chart, 30, 0, 1.5);

        addPhrase(chart, 32, new int[]{0, 1, 2, 3, 2, 1, 0, 2, 1, 3, 2, 1, 0, 1, 2, 3}, 0.5);
        addHold(chart, 40, 3, 2.5);

        addPhrase(chart, 44, new int[]{2, 1, 0, 1, 2, 3, 2, 1});

        return new Song(
                "Just the Two of Us",
                "",
                "smooth groove and lighter syncopation",
                GameTheme.neon(),
                96,
                16,
                "music/just-the-two-of-us.mp4",
                240,
                chart,
                new int[]{110, 165, 220, 165},
                new int[]{330, 392, 440, 494, 523, 494, 440, 392}
        );
    }


    private static Song buildShadowOfTheSun() {
        List<ChartNote> chart = new ArrayList<>();

        addPhrase(chart, 0, new int[]{0, 2, 4, 2, 1, 3, 5, 3});
        addHold(chart, 8, 2, 2);
        addPhrase(chart, 10, new int[]{1, 3, 4, 5, 4, 3, 1, 0}, 0.5);
        addPhrase(chart, 16, new int[]{0, 1, 2, 3, 4, 5, 4, 3});
        addHold(chart, 24, 5, 3);
        addPhrase(chart, 28, new int[]{4, 2, 0, 1, 3, 5, 4, 2}, 0.5);
        addPhrase(chart, 36, new int[]{0, 3, 1, 4, 2, 5 ,3, 1});
        addHold(chart, 44, 0, 2);

        return new Song(
                "Shadow of the Sun",
                "",
                "wide lanes and longer holds",
                GameTheme.glacier(),
                108,
                16,
                "music/in-the-shadow-of-the-sun.mp4"
                235,
                chart,
                new int[]{123, 185, 247, 185},
                new int[]{311, 370, 415, 466, 523, 622, 554, 466}
        );

    }


    private static Song buildFlowers() {
        List<ChartNote> chart = new ArrayList<>();

        addPhrase(chart, 0, new int[]{1, 2, 3, 2, 0, 1, 2, 1}, 0.5);
        addPhrase(chart, 8, new int[]{0, 1, 3, 2, 1, 0, 2, 3}, 0.5);
        addHold(chart, 15, 2, 1);

        addPhrase(chart, 16, new int[]{0, 2, 1, 3, 1, 2, 0, 1, 3, 2, 1, 0, 2, 1, 3, 2}, 0.5);
        addPhrase(chart, 24, new int[]{3, 2, 1, 0, 2, 3, 1, 2, 0, 1, 2, 3, 1, 0, 2, 1}, 0.5);
        addHold(chart, 32, 0, 2);

        addPhrase(chart, 36, new int[]{1, 2, 3, 2, 1, 0, 1, 2, 3, 1, 2, 0}, 0.5);

        return new Song(
            "Flowers",
            "",
            "brighter rhythem with denser chorus",
            GameTheme.sunset(),
            118,
            16,
                "music/flowers.mp4",
                215,
                chart,
                new int[]{98, 147, 196, 247},
                new int[]{294, 370, 392, 440, 494, 554, 587, 659}
        );
    }


    private static void addPhrase(List<ChartNote> chart, double startBeat, int[] lanes) {
        addPhrase(chart, startBeat, lanes, 1.0);
    }

    private static void addPhrase(List<ChartNote> chart, double startBeat, int[] lanes, double spacing) {
        for (int i = 0; i < lanes.length; i++) {
            chart.add(new ChartNote(startBeat + i * spacing, lanes[i], 0));
        }
    }

    private static void addHold(List<ChartNote> chart, double beat, int lane, double holdBeats) {
        chart.add(new ChartNote(beat, lane, holdBeats));
    }


}
