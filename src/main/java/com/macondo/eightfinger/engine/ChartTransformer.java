package com.macondo.eightfinger.engine;

import com.macondo.eightfinger.model.ChartNote;
import com.macondo.eightfinger.model.DifficultyProfile;
import com.macondo.eightfinger.model.Song;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public class ChartTransformer {

    public static List<ChartNote> forDifficulty(Song song, DifficultyProfile profile) {
        List<ChartNote> baseChart = song.getChart();

        if (baseChart.isEmpty()) {
            return new ArrayList<>();
        }

        List<ChartNote> expanded = expandToDuration(song);

        List<ChartNote> adapted = new ArrayList<>();
        int sourceLaneCount = 4;
        int targetLaneCount = profile.getKeys().size();

        for ( int i = 0; i < expanded.size(); i++) {
            ChartNote note = expanded.get(i);

            if (!shouldKeep(note, i, profile.getLevel())) {
                continue;
            }

            int mappedLane = mapLane(note.getLaneSeed(), sourceLaneCount, targetLaneCount, i);
            result.add(new ChartNote(note.getBeat(), mappedLane, note.getHoldBeats()));
        }

        if (profile.getLevel() >= 4) {
            addChordAccents(adapted, profile.getLevel(), targetLaneCount);
        }

        if (profile.getLevel() >= 5) {
            addGapFills(adapted, targetLaneCount);
        }

        result.sort(Comparator.comparingDouble(ChartNote::getBeat));
        return result;
    }

    private static List<ChartNote> expandToDuration(Song song) {
        List<ChartNote> baseChart = song.getChart();
        double targetBeats = song.durationSeconds() / song.secondsPerBeat();
        double baseEndBeat = 0;

        for (ChartNote note : baseChart) {
            baseEndBeat = Math.max(baseEndBeat, note.getBeat() + note.getHoldBeats());
        }

        baseEndBeat = Math.max(baseEndBeat + 4, song.getPreviewBeats());

        if (baseEndBeat >= targetBeats) {
            return baseChart;
        }

        List<ChartNote> expanded = new ArrayList<>();
        int loopIndex = 0;
        double loopOffset = 0;

        while (loopOffset < targetBeats) {
            for (int i = 0; i < baseChart.size(); i++) {
                ChartNote note = baseChart.get(i);
                double beat = note.getBeat() + loopOffset;

                if (beat >= targetBeats - 2) {
                    break;
                }

                int shiftedLane = Math.floorMod(note.getLaneSeed() + loopIndex, 4);
                double holdBeats = (loopIndex % 2 == 0) ? note.getHoldBeats() : Math.max(0, note.getHoldBeats() - 0.25);

                expanded.add(new ChartNote(beat, shiftedLane, holdBeats));
                }

            loopIndex++;
            loopOffset += baseEndBeat;
            }

        return expanded;
        }

        private static boolean shouldKeep(ChartNote note, int index, int level) {
            if (level >= 3) {
                return true;
            }

            double beatFraction = note.getBeat() - Math.floor(note.getBeat());
            boolean strongBeat = Math.abs(beatFraction) < 0.0001 || Math.abs(beatFraction - 0.5) < 0.0001;

            if (note.isHold() || strongBeat) {
                return true;
            }

            if (level == 2) {
                return index % 2 == 0;
            }

            return index % 4 == 0;
        }

        private static void addChordAccents(List<ChartNote> chart, int level, int laneCount) {
            List<ChartNote> additions = new ArrayList<>();

            for (int i = 0; i < chart.size(); i++) {
                ChartNote note = chart.get(i);

                if (note.isHold()) {
                    continue;
                }

                double fraction = note.getBeat() - Math.floor(note.getBeat());
                boolean accentBeat = Math.abs(fraction) < 0.0001 || Math.abs(fraction - 0.5) < 0.0001;

                if (!accentBeat) {
                    continue;
                }

                if (level == 4 && i % 3 != 0) {
                    continue;
                }

                int offset = (i % 2 == 0) ? 1 : -1;
                int accentLane = wrapLane(note.getLaneSeed() + offset, laneCount);

                if (accentLane != note.getLaneSeed()) {
                    additions.add(new ChartNote(note.getBeat(), accentLane, 0));
                }
            }

            chart.addAll(additions);
        }

        private static void addGapFills(List<ChartNote> chart, int laneCount) {
        if (chart.size() < 2) {
            return;
        }

        List<ChartNote> additions = new ArrayList<>();

        for (int i = 0; i < chart.size() - 1; i++) {
            ChartNote current = chart.get(i);
            ChartNote next = chart.get(i + 1);

            double gap = next.getBeat() - current.getBeat();

            if (gap < 0.9 || current.isHold()) {
                continue;
            }

            double fillBeat = current.getBeat() + gap / 2.0;
            int fillLane = wrapLane(current.getLaneSeed() + (i % 2 == 0 ? 1 : -1), laneCount);

            additions.add(new ChartNote(fillBeat, fillLane, 0));
        }

        chart.addAll(additions);
    }

    private static int mapLane(int laneSeed, int sourceLaneCount, int targetLaneCount, int index) {
        if (targetLaneCount <= 1) {
            return 0;
        }

        if (sourceLaneCount <= 1) {
            return Math.floorMod(index, targetLaneCount);
        }

        int sourceLane = Math.floorMod(laneSeed, sourceLaneCount);

        int laneStart = (int) Math.floor((double) sourceLane * targetLaneCount / sourceLaneCount);
        int laneEnd = (int) Math.ceil((double) (sourceLane + 1) * targetLaneCount / sourceLaneCount) - 1;
        laneEnd = Math.max(laneStart, Math.min(targetLaneCount - 1, laneEnd));

        int span = laneEnd - laneStart + 1;
        return laneStart + Math.floorMod(index + sourceLane, span);
    }

    private static int wrapLane(int lane, int laneCount) {
        return Math.floorMod(lane, laneCount);
    }





}
