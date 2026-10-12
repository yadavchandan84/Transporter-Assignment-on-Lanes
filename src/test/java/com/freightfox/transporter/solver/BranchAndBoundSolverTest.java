package com.freightfox.transporter.solver;

import static com.freightfox.transporter.solver.SolverAssertions.assertSameResult;
import static com.freightfox.transporter.solver.SolverAssertions.assertValid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BranchAndBoundSolverTest {

    private final BranchAndBoundSolver solver = new BranchAndBoundSolver();
    private final BruteForceSolver bruteForce = new BruteForceSolver();
    private final SolverOptions options = SolverOptions.defaults();

    private static List<LaneAssignment> assignments(int... laneTransporterPairs) {
        LaneAssignment[] out = new LaneAssignment[laneTransporterPairs.length / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = new LaneAssignment(laneTransporterPairs[2 * i], laneTransporterPairs[2 * i + 1]);
        }
        return List.of(out);
    }

    @Test
    void testCase2_max3_cost7500_selected123_lane4ToTransporter2() {
        SolverResult r = solver.solve(SolverTestData.testCase2(), 3, options);
        assertThat(r.status()).isEqualTo(SolverStatus.OPTIMAL);
        assertThat(r.totalCost()).isEqualTo(7500);
        assertThat(r.selectedTransporters()).containsExactly(1, 2, 3);
        assertThat(r.assignments()).isEqualTo(assignments(1, 1, 2, 1, 3, 2, 4, 2, 5, 3));
    }

    @Test
    void testCase2_max4_cost7450_selected1234_lane4To4_lane1To1() {
        SolverResult r = solver.solve(SolverTestData.testCase2(), 4, options);
        assertThat(r.status()).isEqualTo(SolverStatus.OPTIMAL);
        assertThat(r.totalCost()).isEqualTo(7450);
        assertThat(r.selectedTransporters()).containsExactly(1, 2, 3, 4);
        assertThat(r.assignments()).isEqualTo(assignments(1, 1, 2, 1, 3, 2, 4, 4, 5, 3));
    }

    @Test
    void testCase2_max2_infeasible_minRequired3() {
        SolverResult r = solver.solve(SolverTestData.testCase2(), 2, options);
        assertThat(r.status()).isEqualTo(SolverStatus.INFEASIBLE_TOO_FEW_TRANSPORTERS);
        assertThat(r.minTransportersRequired()).isEqualTo(3);
        assertThat(r.isFeasible()).isFalse();
    }

    @Test
    void testCase1_max3_matchesBruteForce_and_is60139_selected145() {
        ProblemInstance tc1 = SolverTestData.testCase1();
        SolverResult r = solver.solve(tc1, 3, options);
        assertSameResult(bruteForce.solve(tc1, 3, options), r);
        assertThat(r.status()).isEqualTo(SolverStatus.OPTIMAL);
        assertThat(r.totalCost()).isEqualTo(60139);
        assertThat(r.selectedTransporters()).containsExactly(1, 4, 5);
        assertThat(r.assignments()).isEqualTo(assignments(1, 5, 2, 1, 3, 4, 4, 4, 5, 4));
    }

    @Test
    void testCase1_allK_1to7_matchBruteForce() {
        ProblemInstance tc1 = SolverTestData.testCase1();
        long[] expected = {96940, 62938, 60139, 59896, 59896, 59896, 59896};
        for (int k = 1; k <= 7; k++) {
            SolverResult r = solver.solve(tc1, k, options);
            assertSameResult(bruteForce.solve(tc1, k, options), r);
            assertThat(r.totalCost()).as("k=%d", k).isEqualTo(expected[k - 1]);
        }
        assertThat(solver.solve(tc1, 1, options).selectedTransporters()).containsExactly(4);
        assertThat(solver.solve(tc1, 2, options).selectedTransporters()).containsExactly(1, 4);
        SolverResult k4 = solver.solve(tc1, 4, options);
        assertThat(k4.selectedTransporters()).containsExactly(1, 3, 4, 5);
        assertThat(k4.assignments()).isEqualTo(assignments(1, 5, 2, 1, 3, 4, 4, 4, 5, 3));
    }

    @Test
    void mainExample_max3_infeasible_minRequired5() {
        SolverResult r = solver.solve(SolverTestData.mainExample(), 3, options);
        assertThat(r.status()).isEqualTo(SolverStatus.INFEASIBLE_TOO_FEW_TRANSPORTERS);
        assertThat(r.minTransportersRequired()).isEqualTo(5);
    }

    @Test
    void mainExample_max5_cost48000_selected12345() {
        SolverResult r = solver.solve(SolverTestData.mainExample(), 5, options);
        assertThat(r.status()).isEqualTo(SolverStatus.OPTIMAL);
        assertThat(r.totalCost()).isEqualTo(48000);
        assertThat(r.selectedTransporters()).containsExactly(1, 2, 3, 4, 5);
        assertThat(r.assignments()).isEqualTo(assignments(1, 1, 2, 1, 3, 2, 4, 2, 5, 3, 6, 3, 7, 4, 8, 4, 9, 5, 10, 5));
    }

    @Test
    void mainExample_max7_cost48000_selected12345() {
        SolverResult r = solver.solve(SolverTestData.mainExample(), 7, options);
        assertThat(r.totalCost()).isEqualTo(48000);
        assertThat(r.selectedTransporters()).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    void laneWithoutQuotes_returnsUncoveredLaneIds() {
        ProblemInstance p = SolverTestData.of(4, Map.of(1, 10L), Map.of(3, 20L));
        SolverResult r = solver.solve(p, 2, options);
        assertThat(r.status()).isEqualTo(SolverStatus.INFEASIBLE_UNCOVERED_LANES);
        assertThat(r.uncoveredLaneIds()).containsExactly(2, 4);
    }

    @Test
    void tieBreak_prefersMoreTransportersAtEqualCost() {
        ProblemInstance p = SolverTestData.of(2, Map.of(1, 5L, 2, 5L), Map.of(1, 5L), Map.of(2, 5L));
        SolverResult r = solver.solve(p, 3, options);
        assertSameResult(bruteForce.solve(p, 3, options), r);
        assertThat(r.totalCost()).isEqualTo(10);
        assertThat(r.selectedTransporters()).containsExactly(2, 3);
        assertThat(r.assignments()).isEqualTo(assignments(1, 2, 2, 3));
    }

    @Test
    void tieBreak_lexicographicallySmallestSetAtEqualCostAndCount() {
        ProblemInstance p = SolverTestData.of(2, Map.of(1, 5L), Map.of(1, 5L), Map.of(2, 7L));
        SolverResult r = solver.solve(p, 2, options);
        assertThat(r.totalCost()).isEqualTo(12);
        assertThat(r.selectedTransporters()).containsExactly(1, 3);
    }

    @Test
    void strictlyDominatedTransporterNeverSelected() {
        // T2 is strictly dominated by T1 (quotes a subset of T1's lanes, each more expensive).
        ProblemInstance p = SolverTestData.of(3, Map.of(1, 5L, 2, 5L), Map.of(1, 6L, 2, 9L), Map.of(3, 4L));
        SolverResult r = solver.solve(p, 3, options);
        assertSameResult(bruteForce.solve(p, 3, options), r);
        assertThat(r.selectedTransporters()).containsExactly(1, 3).doesNotContain(2);
        assertThat(r.totalCost()).isEqualTo(14);
    }

    @Test
    void zeroTimeBudget_returnsHeuristicResult_notOptimal() {
        ProblemInstance p = SolverTestData.random(42, 150, 40, 0.6, 10_000);
        SolverResult r = solver.solve(p, 8, new SolverOptions(Duration.ZERO));
        assertThat(r.status()).isEqualTo(SolverStatus.FEASIBLE);
        assertThat(r.isOptimal()).isFalse();
        assertValid(p, r, 8);
    }

    @Test
    void performance_25Transporters_200Lanes_completesWithinBudget() {
        ProblemInstance p = SolverTestData.random(7, 200, 25, 0.6, 10_000);
        SolverResult heuristic = new HeuristicSolver().solve(p, 6, options);
        long start = System.nanoTime();
        SolverResult r = solver.solve(p, 6, new SolverOptions(Duration.ofSeconds(10)));
        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);
        assertValid(p, r, 6);
        assertThat(elapsed).isLessThan(Duration.ofSeconds(12));
        assertThat(r.totalCost()).isLessThanOrEqualTo(heuristic.totalCost());
    }

    @Test
    void maxTransportersLessThan1_throwsIllegalArgument() {
        assertThatThrownBy(() -> solver.solve(SolverTestData.testCase2(), 0, options))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
