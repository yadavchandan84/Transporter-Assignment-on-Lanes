package com.freightfox.transporter.solver;

import java.util.ArrayList;
import java.util.List;

/** Reference solver: enumerates every subset (n <= 16). Test-only. */
final class BruteForceSolver implements AssignmentSolver {

    @Override
    public SolverResult solve(ProblemInstance instance, int maxTransporters, SolverOptions options) {
        int n = instance.transporterCount();
        int lanes = instance.laneCount();
        if (n > 16) {
            throw new IllegalArgumentException("Brute force supports at most 16 transporters");
        }
        long[][] cost = instance.cost();

        List<Integer> uncovered = new ArrayList<>();
        for (int l = 0; l < lanes; l++) {
            boolean quoted = false;
            for (int t = 0; t < n; t++) {
                quoted |= cost[t][l] != ProblemInstance.NO_QUOTE;
            }
            if (!quoted) {
                uncovered.add(instance.laneIds()[l]);
            }
        }
        if (!uncovered.isEmpty()) {
            return SolverResult.uncovered(uncovered);
        }

        int minCover = Integer.MAX_VALUE;
        HeuristicSolver.Evaluation best = null;
        for (int mask = 1; mask < (1 << n); mask++) {
            boolean[] inSet = new boolean[n];
            for (int t = 0; t < n; t++) {
                inSet[t] = (mask & (1 << t)) != 0;
            }
            HeuristicSolver.Evaluation e = evaluate(instance, inSet);
            if (e == null) {
                continue;
            }
            int size = Integer.bitCount(mask);
            minCover = Math.min(minCover, size);
            if (size <= maxTransporters && (best == null
                    || SolutionRanking.compare(e.cost(), e.usedIds(), best.cost(), best.usedIds()) < 0)) {
                best = e;
            }
        }
        if (best == null) {
            return SolverResult.tooFew(minCover);
        }
        return SolverResult.optimal(best.cost(), best.assignments(), best.usedIdList(), 0);
    }

    /** Independent canonical-assignment evaluation (does not reuse solver code beyond the record). */
    private static HeuristicSolver.Evaluation evaluate(ProblemInstance instance, boolean[] inSet) {
        long[][] cost = instance.cost();
        int n = instance.transporterCount();
        long total = 0;
        boolean[] used = new boolean[n];
        List<LaneAssignment> assignments = new ArrayList<>();
        for (int l = 0; l < instance.laneCount(); l++) {
            int arg = -1;
            for (int t = 0; t < n; t++) {
                if (inSet[t] && cost[t][l] != ProblemInstance.NO_QUOTE
                        && (arg < 0 || cost[t][l] < cost[arg][l]
                        || (cost[t][l] == cost[arg][l] && instance.transporterIds()[t] < instance.transporterIds()[arg]))) {
                    arg = t;
                }
            }
            if (arg < 0) {
                return null;
            }
            total += cost[arg][l];
            used[arg] = true;
            assignments.add(new LaneAssignment(instance.laneIds()[l], instance.transporterIds()[arg]));
        }
        List<Integer> ids = new ArrayList<>();
        for (int t = 0; t < n; t++) {
            if (used[t]) {
                ids.add(instance.transporterIds()[t]);
            }
        }
        return new HeuristicSolver.Evaluation(total, ids.stream().mapToInt(Integer::intValue).toArray(), assignments);
    }
}
