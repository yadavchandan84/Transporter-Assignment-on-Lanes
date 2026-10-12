package com.freightfox.transporter.solver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;

/**
 * Exact minimum set cover size: the fewest transporters (rows of the cost matrix) whose quotes
 * together cover every lane. Used to detect "maxTransporters too small" before optimizing.
 */
public final class MinimumCoverCalculator {

    private final int laneCount;
    private final int rowCount;
    private final int[][] lanesOfRow;
    private final int[][] rowsOfLane;
    private final long deadlineNanos;
    private final int[] coveredCount;
    private final boolean[] chosen;
    private int uncovered;
    private int best;
    private long nodes;
    private boolean timedOut;

    private MinimumCoverCalculator(long[][] cost, int laneCount, long deadlineNanos) {
        this.laneCount = laneCount;
        this.rowCount = cost.length;
        this.deadlineNanos = deadlineNanos;
        this.lanesOfRow = new int[rowCount][];
        List<List<Integer>> byLane = new ArrayList<>();
        for (int l = 0; l < laneCount; l++) {
            byLane.add(new ArrayList<>());
        }
        for (int r = 0; r < rowCount; r++) {
            int[] tmp = new int[laneCount];
            int n = 0;
            for (int l = 0; l < laneCount; l++) {
                if (cost[r][l] != ProblemInstance.NO_QUOTE) {
                    tmp[n++] = l;
                    byLane.get(l).add(r);
                }
            }
            lanesOfRow[r] = Arrays.copyOf(tmp, n);
        }
        this.rowsOfLane = new int[laneCount][];
        for (int l = 0; l < laneCount; l++) {
            rowsOfLane[l] = byLane.get(l).stream().mapToInt(Integer::intValue).toArray();
        }
        this.coveredCount = new int[laneCount];
        this.chosen = new boolean[rowCount];
        this.uncovered = laneCount;
    }

    /**
     * @param cost          cost[row][lane], {@link ProblemInstance#NO_QUOTE} for no quote
     * @param laneCount     number of lanes
     * @param deadlineNanos {@link System#nanoTime()} deadline
     * @return the exact minimum cover size; empty if some lane cannot be covered at all
     *         or the deadline was hit before optimality was proven
     */
    public static OptionalInt minimumCover(long[][] cost, int laneCount, long deadlineNanos) {
        if (laneCount == 0) {
            return OptionalInt.of(0);
        }
        MinimumCoverCalculator calc = new MinimumCoverCalculator(cost, laneCount, deadlineNanos);
        for (int[] rows : calc.rowsOfLane) {
            if (rows.length == 0) {
                return OptionalInt.empty();
            }
        }
        calc.best = calc.greedyCoverSize();
        calc.dfs(0);
        return calc.timedOut ? OptionalInt.empty() : OptionalInt.of(calc.best);
    }

    private int greedyCoverSize() {
        boolean[] covered = new boolean[laneCount];
        boolean[] used = new boolean[rowCount];
        int remaining = laneCount;
        int size = 0;
        while (remaining > 0) {
            int bestRow = -1;
            int bestGain = 0;
            for (int r = 0; r < rowCount; r++) {
                if (used[r]) {
                    continue;
                }
                int gain = 0;
                for (int l : lanesOfRow[r]) {
                    if (!covered[l]) {
                        gain++;
                    }
                }
                if (gain > bestGain) {
                    bestGain = gain;
                    bestRow = r;
                }
            }
            used[bestRow] = true;
            size++;
            for (int l : lanesOfRow[bestRow]) {
                if (!covered[l]) {
                    covered[l] = true;
                    remaining--;
                }
            }
        }
        return size;
    }

    private int newCoverage(int r) {
        int gain = 0;
        for (int l : lanesOfRow[r]) {
            if (coveredCount[l] == 0) {
                gain++;
            }
        }
        return gain;
    }

    private void dfs(int depth) {
        if (timedOut) {
            return;
        }
        if ((nodes++ & 255) == 0 && System.nanoTime() - deadlineNanos >= 0) {
            timedOut = true;
            return;
        }
        if (uncovered == 0) {
            best = Math.min(best, depth);
            return;
        }
        if (depth + 1 >= best) {
            return;
        }
        int maxGain = 0;
        for (int r = 0; r < rowCount; r++) {
            if (!chosen[r]) {
                maxGain = Math.max(maxGain, newCoverage(r));
            }
        }
        if (maxGain == 0 || depth + (uncovered + maxGain - 1) / maxGain >= best) {
            return;
        }
        // Branch on the uncovered lane with the fewest candidate rows: one of them must be chosen.
        int pivot = -1;
        int pivotOptions = Integer.MAX_VALUE;
        for (int l = 0; l < laneCount; l++) {
            if (coveredCount[l] == 0 && rowsOfLane[l].length < pivotOptions) {
                pivotOptions = rowsOfLane[l].length;
                pivot = l;
            }
        }
        int[] options = rowsOfLane[pivot].clone();
        int[] gains = new int[options.length];
        Integer[] order = new Integer[options.length];
        for (int i = 0; i < options.length; i++) {
            gains[i] = newCoverage(options[i]);
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> gains[b] != gains[a] ? Integer.compare(gains[b], gains[a])
                : Integer.compare(options[a], options[b]));
        for (int idx : order) {
            int r = options[idx];
            if (chosen[r]) {
                continue;
            }
            chosen[r] = true;
            for (int l : lanesOfRow[r]) {
                if (coveredCount[l]++ == 0) {
                    uncovered--;
                }
            }
            dfs(depth + 1);
            for (int l : lanesOfRow[r]) {
                if (--coveredCount[l] == 0) {
                    uncovered++;
                }
            }
            chosen[r] = false;
            if (timedOut || depth + 1 >= best) {
                return;
            }
        }
    }
}
