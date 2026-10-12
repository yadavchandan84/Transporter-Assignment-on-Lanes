package com.freightfox.transporter.solver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;

/**
 * Exact branch-and-bound solver with a time budget.
 *
 * <p>Pipeline: uncovered-lane check, exact-safe candidate filtering (strict dominance),
 * exact minimum cover check, heuristic incumbent, then a depth-first include/exclude search over
 * candidates with a cost lower bound and a coverage-feasibility bound. If the time budget runs
 * out, the best solution found so far is returned with status {@link SolverStatus#FEASIBLE}.
 */
public class BranchAndBoundSolver implements AssignmentSolver {

    private static final long NO_QUOTE = ProblemInstance.NO_QUOTE;

    private final HeuristicSolver heuristic;

    public BranchAndBoundSolver() {
        this(new HeuristicSolver());
    }

    public BranchAndBoundSolver(HeuristicSolver heuristic) {
        this.heuristic = heuristic;
    }

    @Override
    public SolverResult solve(ProblemInstance instance, int maxTransporters, SolverOptions options) {
        if (maxTransporters < 1) {
            throw new IllegalArgumentException("maxTransporters must be >= 1");
        }
        long deadline = System.nanoTime() + options.timeBudget().toNanos();
        int lanes = instance.laneCount();
        if (lanes == 0) {
            return SolverResult.optimal(0, List.of(), List.of(), 0);
        }

        // 1. Uncovered lanes.
        long[][] cost = instance.cost();
        List<Integer> uncovered = new ArrayList<>();
        for (int l = 0; l < lanes; l++) {
            boolean quoted = false;
            for (long[] row : cost) {
                if (row[l] != NO_QUOTE) {
                    quoted = true;
                    break;
                }
            }
            if (!quoted) {
                uncovered.add(instance.laneIds()[l]);
            }
        }
        if (!uncovered.isEmpty()) {
            return SolverResult.uncovered(uncovered);
        }

        // 2. Candidate filtering + branching order.
        int[] candidates = orderCandidates(cost, filterCandidates(cost, lanes), lanes);
        long[][] candCost = new long[candidates.length][];
        for (int i = 0; i < candidates.length; i++) {
            candCost[i] = cost[candidates[i]];
        }

        // 3. Exact minimum cover check.
        OptionalInt minCover = MinimumCoverCalculator.minimumCover(candCost, lanes, deadline);
        if (minCover.isPresent() && minCover.getAsInt() > maxTransporters) {
            return SolverResult.tooFew(minCover.getAsInt());
        }

        // 4. Heuristic incumbent + 5/6. Branch and bound.
        Search search = new Search(instance, candidates, maxTransporters, deadline);
        SolverResult incumbent = heuristic.solve(instance, maxTransporters, deadline);
        if (incumbent.isFeasible()) {
            search.offerIncumbent(incumbent);
        }
        search.run();

        // 7. Result.
        if (search.bestUsed == null) {
            if (search.timedOut) {
                return SolverResult.noSolution();
            }
            return SolverResult.tooFew(minCover.orElse(maxTransporters + 1));
        }
        List<LaneAssignment> assignments = new ArrayList<>(lanes);
        for (int l = 0; l < lanes; l++) {
            assignments.add(new LaneAssignment(instance.laneIds()[l], instance.transporterIds()[search.bestArg[l]]));
        }
        List<Integer> selected = new ArrayList<>();
        for (int id : search.bestUsed) {
            selected.add(id);
        }
        return search.timedOut
                ? SolverResult.feasible(search.bestCost, assignments, selected, search.nodes)
                : SolverResult.optimal(search.bestCost, assignments, selected, search.nodes);
    }

    /**
     * Drops transporters with no quotes and transporters strictly dominated by another one
     * (the other quotes every lane they quote, each strictly cheaper). Such a transporter can never
     * appear in the used set of an optimal solution, so dropping it is exact.
     */
    private static int[] filterCandidates(long[][] cost, int lanes) {
        int n = cost.length;
        List<Integer> kept = new ArrayList<>();
        for (int j = 0; j < n; j++) {
            boolean hasQuote = false;
            for (int l = 0; l < lanes && !hasQuote; l++) {
                hasQuote = cost[j][l] != NO_QUOTE;
            }
            if (!hasQuote) {
                continue;
            }
            boolean dominated = false;
            for (int i = 0; i < n && !dominated; i++) {
                if (i == j) {
                    continue;
                }
                boolean all = true;
                for (int l = 0; l < lanes && all; l++) {
                    if (cost[j][l] != NO_QUOTE && !(cost[i][l] < cost[j][l])) {
                        all = false;
                    }
                }
                dominated = all;
            }
            if (!dominated) {
                kept.add(j);
            }
        }
        return kept.stream().mapToInt(Integer::intValue).toArray();
    }

    /** Order: most lanes where the candidate holds the global minimum, then lowest quote sum, then id. */
    private static int[] orderCandidates(long[][] cost, int[] candidates, int lanes) {
        long[] globalMin = new long[lanes];
        Arrays.fill(globalMin, NO_QUOTE);
        for (int t : candidates) {
            for (int l = 0; l < lanes; l++) {
                globalMin[l] = Math.min(globalMin[l], cost[t][l]);
            }
        }
        int[] minCount = new int[cost.length];
        long[] sum = new long[cost.length];
        for (int t : candidates) {
            for (int l = 0; l < lanes; l++) {
                if (cost[t][l] != NO_QUOTE) {
                    sum[t] += cost[t][l];
                    if (cost[t][l] == globalMin[l]) {
                        minCount[t]++;
                    }
                }
            }
        }
        return Arrays.stream(candidates).boxed()
                .sorted(Comparator.<Integer>comparingInt(t -> -minCount[t])
                        .thenComparingLong(t -> sum[t])
                        .thenComparingInt(t -> t))
                .mapToInt(Integer::intValue)
                .toArray();
    }

    /** Depth-first include/exclude search state. Transporter indices refer to the original instance. */
    private static final class Search {
        private final ProblemInstance instance;
        private final long[][] cost;
        private final int[] cand;
        private final int n;
        private final int lanes;
        private final int k;
        private final long deadline;
        private final long[][] suffixMin;
        private final long[][] curMin;
        private final int[][] curArg;

        long nodes;
        boolean timedOut;
        long bestCost = Long.MAX_VALUE;
        int[] bestUsed;
        int[] bestArg;

        Search(ProblemInstance instance, int[] cand, int k, long deadline) {
            this.instance = instance;
            this.cost = instance.cost();
            this.cand = cand;
            this.n = cand.length;
            this.lanes = instance.laneCount();
            this.k = k;
            this.deadline = deadline;
            this.suffixMin = new long[n + 1][lanes];
            Arrays.fill(suffixMin[n], NO_QUOTE);
            for (int i = n - 1; i >= 0; i--) {
                for (int l = 0; l < lanes; l++) {
                    suffixMin[i][l] = Math.min(suffixMin[i + 1][l], cost[cand[i]][l]);
                }
            }
            int levels = Math.min(k, n) + 1;
            this.curMin = new long[levels][lanes];
            this.curArg = new int[levels][lanes];
            Arrays.fill(curMin[0], NO_QUOTE);
            Arrays.fill(curArg[0], -1);
        }

        void offerIncumbent(SolverResult r) {
            int[] used = r.selectedTransporters().stream().mapToInt(Integer::intValue).toArray();
            int[] arg = new int[lanes];
            for (int l = 0; l < lanes; l++) {
                arg[l] = Arrays.binarySearch(instance.transporterIds(), r.assignments().get(l).transporterId());
            }
            bestCost = r.totalCost();
            bestUsed = used;
            bestArg = arg;
        }

        void run() {
            dfs(0, 0);
        }

        private void dfs(int i, int chosen) {
            if (timedOut) {
                return;
            }
            if ((nodes++ & 1023) == 0 && System.nanoTime() - deadline >= 0) {
                timedOut = true;
                return;
            }
            if (i == n || chosen == k) {
                return;
            }
            long[] min = curMin[chosen];
            int[] arg = curArg[chosen];
            long[] suffix = suffixMin[i];

            // Cost lower bound (equal bound is not pruned: ties may improve secondary keys).
            long lb = 0;
            int uncovered = 0;
            for (int l = 0; l < lanes; l++) {
                long m = Math.min(min[l], suffix[l]);
                if (m == NO_QUOTE) {
                    return;
                }
                lb += m;
                if (min[l] == NO_QUOTE) {
                    uncovered++;
                }
            }
            if (bestUsed != null && lb > bestCost) {
                return;
            }

            // Coverage feasibility bound.
            if (uncovered > 0) {
                int maxCov = 0;
                for (int j = i; j < n; j++) {
                    long[] row = cost[cand[j]];
                    int c = 0;
                    for (int l = 0; l < lanes; l++) {
                        if (min[l] == NO_QUOTE && row[l] != NO_QUOTE) {
                            c++;
                        }
                    }
                    maxCov = Math.max(maxCov, c);
                }
                if (maxCov == 0 || (uncovered + maxCov - 1) / maxCov > k - chosen) {
                    return;
                }
            }

            // Include branch: only if candidate i would actually be used.
            int t = cand[i];
            long[] row = cost[t];
            boolean useful = false;
            for (int l = 0; l < lanes && !useful; l++) {
                useful = row[l] < min[l] || (row[l] == min[l] && row[l] != NO_QUOTE && t < arg[l]);
            }
            if (useful) {
                long[] nMin = curMin[chosen + 1];
                int[] nArg = curArg[chosen + 1];
                boolean covered = true;
                for (int l = 0; l < lanes; l++) {
                    long c = row[l];
                    if (c < min[l] || (c == min[l] && c != NO_QUOTE && t < arg[l])) {
                        nMin[l] = c;
                        nArg[l] = t;
                    } else {
                        nMin[l] = min[l];
                        nArg[l] = arg[l];
                    }
                    if (nMin[l] == NO_QUOTE) {
                        covered = false;
                    }
                }
                if (covered) {
                    evaluate(nMin, nArg);
                }
                dfs(i + 1, chosen + 1);
            }

            // Exclude branch.
            dfs(i + 1, chosen);
        }

        private void evaluate(long[] min, int[] arg) {
            long total = 0;
            for (long m : min) {
                total += m;
            }
            if (bestUsed != null && total > bestCost) {
                return;
            }
            int[] used = Arrays.stream(arg).distinct().sorted().map(t -> instance.transporterIds()[t]).toArray();
            if (bestUsed == null || SolutionRanking.compare(total, used, bestCost, bestUsed) < 0) {
                bestCost = total;
                bestUsed = used;
                bestArg = arg.clone();
            }
        }
    }
}
