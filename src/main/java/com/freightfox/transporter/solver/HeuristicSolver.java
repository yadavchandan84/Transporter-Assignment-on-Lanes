package com.freightfox.transporter.solver;

import java.util.ArrayList;
import java.util.List;

/**
 * Fast heuristic: greedy cover, greedy cost improvement, then first-improvement local search
 * (drop / swap / add) under {@link SolutionRanking}. Never claims optimality.
 */
public class HeuristicSolver implements AssignmentSolver {

    @Override
    public SolverResult solve(ProblemInstance instance, int maxTransporters, SolverOptions options) {
        return solve(instance, maxTransporters, System.nanoTime() + options.timeBudget().toNanos());
    }

    /**
     * Same as {@link #solve(ProblemInstance, int, SolverOptions)} with an absolute deadline.
     * The greedy phases always run; the local search stops once the deadline passes.
     */
    SolverResult solve(ProblemInstance instance, int maxTransporters, long deadlineNanos) {
        if (maxTransporters < 1) {
            throw new IllegalArgumentException("maxTransporters must be >= 1");
        }
        int n = instance.transporterCount();
        int lanes = instance.laneCount();
        long[][] cost = instance.cost();

        List<Integer> uncoveredIds = new ArrayList<>();
        for (int l = 0; l < lanes; l++) {
            boolean quoted = false;
            for (int t = 0; t < n && !quoted; t++) {
                quoted = cost[t][l] != ProblemInstance.NO_QUOTE;
            }
            if (!quoted) {
                uncoveredIds.add(instance.laneIds()[l]);
            }
        }
        if (!uncoveredIds.isEmpty()) {
            return SolverResult.uncovered(uncoveredIds);
        }

        boolean[] inSet = new boolean[n];
        int size = 0;

        // 1. Greedy cover.
        boolean[] covered = new boolean[lanes];
        int remaining = lanes;
        while (remaining > 0 && size < maxTransporters) {
            int bestT = -1;
            int bestGain = 0;
            long bestSum = Long.MAX_VALUE;
            for (int t = 0; t < n; t++) {
                if (inSet[t]) {
                    continue;
                }
                int gain = 0;
                long sum = 0;
                for (int l = 0; l < lanes; l++) {
                    if (!covered[l] && cost[t][l] != ProblemInstance.NO_QUOTE) {
                        gain++;
                        sum += cost[t][l];
                    }
                }
                if (gain > bestGain || (gain == bestGain && gain > 0 && sum < bestSum)) {
                    bestT = t;
                    bestGain = gain;
                    bestSum = sum;
                }
            }
            if (bestT < 0) {
                break;
            }
            inSet[bestT] = true;
            size++;
            for (int l = 0; l < lanes; l++) {
                if (!covered[l] && cost[bestT][l] != ProblemInstance.NO_QUOTE) {
                    covered[l] = true;
                    remaining--;
                }
            }
        }
        if (remaining > 0) {
            return SolverResult.noSolution();
        }

        // 2. Greedy cost improvement while capacity remains.
        while (size < maxTransporters) {
            long[] curMin = laneMinima(cost, inSet, lanes);
            int bestT = -1;
            long bestReduction = 0;
            for (int t = 0; t < n; t++) {
                if (inSet[t]) {
                    continue;
                }
                long reduction = 0;
                for (int l = 0; l < lanes; l++) {
                    if (cost[t][l] < curMin[l]) {
                        reduction += curMin[l] - cost[t][l];
                    }
                }
                if (reduction > bestReduction) {
                    bestReduction = reduction;
                    bestT = t;
                }
            }
            if (bestT < 0) {
                break;
            }
            inSet[bestT] = true;
            size++;
        }

        // 3. First-improvement local search.
        Evaluation current = evaluate(instance, inSet);
        boolean improved = true;
        while (improved && System.nanoTime() - deadlineNanos < 0) {
            improved = false;
            // Drops.
            for (int a = 0; a < n && !improved; a++) {
                if (!inSet[a]) {
                    continue;
                }
                inSet[a] = false;
                Evaluation e = evaluate(instance, inSet);
                if (e != null && e.isBetterThan(current)) {
                    current = e;
                    size--;
                    improved = true;
                } else {
                    inSet[a] = true;
                }
            }
            // Swaps.
            for (int a = 0; a < n && !improved; a++) {
                if (!inSet[a]) {
                    continue;
                }
                inSet[a] = false;
                for (int b = 0; b < n && !improved; b++) {
                    if (b == a || inSet[b]) {
                        continue;
                    }
                    inSet[b] = true;
                    Evaluation e = evaluate(instance, inSet);
                    if (e != null && e.isBetterThan(current)) {
                        current = e;
                        improved = true;
                    } else {
                        inSet[b] = false;
                    }
                }
                if (!improved) {
                    inSet[a] = true;
                }
            }
            // Adds.
            for (int b = 0; b < n && !improved && size < maxTransporters; b++) {
                if (inSet[b]) {
                    continue;
                }
                inSet[b] = true;
                Evaluation e = evaluate(instance, inSet);
                if (e != null && e.isBetterThan(current)) {
                    current = e;
                    size++;
                    improved = true;
                } else {
                    inSet[b] = false;
                }
            }
        }
        return SolverResult.feasible(current.cost, current.assignments, current.usedIdList(), 0);
    }

    private static long[] laneMinima(long[][] cost, boolean[] inSet, int lanes) {
        long[] min = new long[lanes];
        java.util.Arrays.fill(min, ProblemInstance.NO_QUOTE);
        for (int t = 0; t < cost.length; t++) {
            if (inSet[t]) {
                for (int l = 0; l < lanes; l++) {
                    min[l] = Math.min(min[l], cost[t][l]);
                }
            }
        }
        return min;
    }

    /** Canonical assignment of a set; null when the set does not cover every lane. */
    static Evaluation evaluate(ProblemInstance instance, boolean[] inSet) {
        long[][] cost = instance.cost();
        int lanes = instance.laneCount();
        int n = instance.transporterCount();
        long total = 0;
        boolean[] used = new boolean[n];
        List<LaneAssignment> assignments = new ArrayList<>(lanes);
        for (int l = 0; l < lanes; l++) {
            int arg = -1;
            long min = ProblemInstance.NO_QUOTE;
            for (int t = 0; t < n; t++) {
                // strict '<' with ascending index keeps the smallest id on ties
                if (inSet[t] && cost[t][l] < min) {
                    min = cost[t][l];
                    arg = t;
                }
            }
            if (arg < 0) {
                return null;
            }
            total += min;
            used[arg] = true;
            assignments.add(new LaneAssignment(instance.laneIds()[l], instance.transporterIds()[arg]));
        }
        int count = 0;
        for (boolean u : used) {
            if (u) {
                count++;
            }
        }
        int[] usedIds = new int[count];
        for (int t = 0, i = 0; t < n; t++) {
            if (used[t]) {
                usedIds[i++] = instance.transporterIds()[t];
            }
        }
        return new Evaluation(total, usedIds, assignments);
    }

    record Evaluation(long cost, int[] usedIds, List<LaneAssignment> assignments) {
        boolean isBetterThan(Evaluation other) {
            return SolutionRanking.compare(cost, usedIds, other.cost, other.usedIds) < 0;
        }

        List<Integer> usedIdList() {
            List<Integer> list = new ArrayList<>(usedIds.length);
            for (int id : usedIds) {
                list.add(id);
            }
            return list;
        }
    }
}
