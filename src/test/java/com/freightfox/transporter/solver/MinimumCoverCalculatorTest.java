package com.freightfox.transporter.solver;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.OptionalInt;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class MinimumCoverCalculatorTest {

    private static OptionalInt minCover(ProblemInstance p) {
        return MinimumCoverCalculator.minimumCover(p.cost(), p.laneCount(),
                System.nanoTime() + TimeUnit.SECONDS.toNanos(5));
    }

    @Test
    void mainExample_needs5() {
        assertThat(minCover(SolverTestData.mainExample())).hasValue(5);
    }

    @Test
    void testCase2_needs3() {
        assertThat(minCover(SolverTestData.testCase2())).hasValue(3);
    }

    @Test
    void testCase1_needs1() {
        assertThat(minCover(SolverTestData.testCase1())).hasValue(1);
    }

    @Test
    void randomInstances_matchBruteForce() {
        BruteForceSolver bruteForce = new BruteForceSolver();
        for (long seed = 1; seed <= 100; seed++) {
            ProblemInstance p = SolverTestData.random(seed, 1 + (int) (seed % 10), 2 + (int) (seed % 11), 0.35, 10);
            // k = 1 makes brute force report the true minimum cover size whenever k is too small.
            SolverResult b = bruteForce.solve(p, 1, SolverOptions.defaults());
            OptionalInt m = minCover(p);
            switch (b.status()) {
                case INFEASIBLE_UNCOVERED_LANES -> assertThat(m).as("seed %d", seed).isEmpty();
                case INFEASIBLE_TOO_FEW_TRANSPORTERS ->
                        assertThat(m).as("seed %d", seed).hasValue(b.minTransportersRequired());
                default -> assertThat(m).as("seed %d", seed).hasValue(1);
            }
        }
    }
}
