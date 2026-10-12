package com.freightfox.transporter.solver;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Problem instances from the assignment brief plus a seeded random generator. */
final class SolverTestData {

    private SolverTestData() {
    }

    /** Main 10-lane example from the brief. */
    static ProblemInstance mainExample() {
        Map<Integer, Map<Integer, Long>> q = new LinkedHashMap<>();
        q.put(1, Map.of(1, 5000L, 2, 6000L));
        q.put(2, Map.of(3, 7000L, 4, 4000L));
        q.put(3, Map.of(5, 3000L, 6, 2000L));
        q.put(4, Map.of(7, 8000L, 8, 5000L));
        q.put(5, Map.of(9, 4500L, 10, 3500L));
        q.put(6, Map.of(1, 5500L, 2, 6200L));
        q.put(7, Map.of(3, 7500L, 4, 4500L));
        return ProblemInstance.of(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), q);
    }

    /** Test Case 1 from the brief: 5 lanes, 7 transporters, every transporter quotes every lane. */
    static ProblemInstance testCase1() {
        long[][] m = {
                {20835, 10512, 22105, 42481, 19862},
                {48844, 31326, 18640, 45828, 18297},
                {39020, 20648, 31438, 36447, 12789},
                {14400, 44514, 14316, 10678, 13032},
                {11601, 19760, 40870, 20635, 26421},
                {35095, 12494, 17808, 36210, 39444},
                {26070, 41098, 20932, 16897, 27938},
        };
        Map<Integer, Map<Integer, Long>> q = new LinkedHashMap<>();
        for (int t = 0; t < m.length; t++) {
            Map<Integer, Long> row = new LinkedHashMap<>();
            for (int l = 0; l < m[t].length; l++) {
                row.put(l + 1, m[t][l]);
            }
            q.put(t + 1, row);
        }
        return ProblemInstance.of(List.of(1, 2, 3, 4, 5), q);
    }

    /** Test Case 2 from the brief: X=1, Y=2, Z=3, W=4. */
    static ProblemInstance testCase2() {
        Map<Integer, Map<Integer, Long>> q = new LinkedHashMap<>();
        q.put(1, Map.of(1, 1000L, 2, 1500L));
        q.put(2, Map.of(3, 2000L, 4, 1800L));
        q.put(3, Map.of(5, 1200L));
        q.put(4, Map.of(1, 1100L, 4, 1750L));
        return ProblemInstance.of(List.of(1, 2, 3, 4, 5), q);
    }

    /** Builds an instance from explicit per-transporter quote maps (transporter ids 1..n). */
    @SafeVarargs
    static ProblemInstance of(int laneCount, Map<Integer, Long>... quotes) {
        Map<Integer, Map<Integer, Long>> q = new LinkedHashMap<>();
        for (int t = 0; t < quotes.length; t++) {
            q.put(t + 1, quotes[t]);
        }
        List<Integer> lanes = new ArrayList<>();
        for (int l = 1; l <= laneCount; l++) {
            lanes.add(l);
        }
        return ProblemInstance.of(lanes, q);
    }

    /** Random instance; each (transporter, lane) pair is quoted with probability {@code density}. */
    static ProblemInstance random(long seed, int lanes, int transporters, double density, int maxQuote) {
        Random rnd = new Random(seed);
        Map<Integer, Map<Integer, Long>> q = new LinkedHashMap<>();
        for (int t = 1; t <= transporters; t++) {
            Map<Integer, Long> row = new LinkedHashMap<>();
            for (int l = 1; l <= lanes; l++) {
                if (rnd.nextDouble() < density) {
                    row.put(l, 1L + rnd.nextInt(maxQuote));
                }
            }
            q.put(t, row);
        }
        List<Integer> laneIds = new ArrayList<>();
        for (int l = 1; l <= lanes; l++) {
            laneIds.add(l);
        }
        return ProblemInstance.of(laneIds, q);
    }
}
