package com.freightfox.transporter.solver;

import static com.freightfox.transporter.solver.SolverAssertions.assertValid;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;
import org.junit.jupiter.api.Test;

class HeuristicSolverTest {

    private final HeuristicSolver heuristic = new HeuristicSolver();
    private final BranchAndBoundSolver exact = new BranchAndBoundSolver();
    private final BruteForceSolver bruteForce = new BruteForceSolver();
    private final SolverOptions options = SolverOptions.defaults();

    @Test
    void testCase2_max3_equalsExact() {
        ProblemInstance p = SolverTestData.testCase2();
        SolverResult h = heuristic.solve(p, 3, options);
        SolverResult e = exact.solve(p, 3, options);
        assertThat(h.status()).isEqualTo(SolverStatus.FEASIBLE);
        assertThat(h.totalCost()).isEqualTo(e.totalCost()).isEqualTo(7500);
        assertThat(h.selectedTransporters()).isEqualTo(e.selectedTransporters());
        assertThat(h.assignments()).isEqualTo(e.assignments());
    }

    @Test
    void testCase2_max4_equalsExact() {
        ProblemInstance p = SolverTestData.testCase2();
        SolverResult h = heuristic.solve(p, 4, options);
        SolverResult e = exact.solve(p, 4, options);
        assertThat(h.totalCost()).isEqualTo(e.totalCost()).isEqualTo(7450);
        assertThat(h.selectedTransporters()).isEqualTo(e.selectedTransporters());
        assertThat(h.assignments()).isEqualTo(e.assignments());
    }

    @Test
    void randomInstances_validAndNeverBetterThanBruteForce() {
        for (long seed = 1; seed <= 100; seed++) {
            Random rnd = new Random(seed);
            int lanes = 1 + rnd.nextInt(8);
            int transporters = 1 + rnd.nextInt(9);
            int k = 1 + rnd.nextInt(transporters);
            ProblemInstance p = SolverTestData.random(seed, lanes, transporters, 0.5, 20);
            SolverResult h = heuristic.solve(p, k, options);
            SolverResult b = bruteForce.solve(p, k, options);
            if (h.isFeasible()) {
                assertValid(p, h, k);
                assertThat(b.isFeasible()).as("seed %d", seed).isTrue();
                assertThat(SolutionRanking.compare(
                        h.totalCost(), ids(h), b.totalCost(), ids(b))).as("seed %d", seed).isGreaterThanOrEqualTo(0);
            } else if (h.status() == SolverStatus.INFEASIBLE_UNCOVERED_LANES) {
                assertThat(b.status()).isEqualTo(SolverStatus.INFEASIBLE_UNCOVERED_LANES);
                assertThat(h.uncoveredLaneIds()).isEqualTo(b.uncoveredLaneIds());
            } else {
                assertThat(h.status()).isEqualTo(SolverStatus.NO_SOLUTION_FOUND);
            }
        }
    }

    private static int[] ids(SolverResult r) {
        return r.selectedTransporters().stream().mapToInt(Integer::intValue).toArray();
    }
}
