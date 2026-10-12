package com.freightfox.transporter.solver;

import static com.freightfox.transporter.solver.SolverAssertions.assertSameResult;
import static com.freightfox.transporter.solver.SolverAssertions.assertValid;

import java.util.Random;
import java.util.stream.LongStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Property tests: the exact solver must agree with brute force on every field. */
class BranchAndBoundRandomizedTest {

    private final BranchAndBoundSolver solver = new BranchAndBoundSolver();
    private final BruteForceSolver bruteForce = new BruteForceSolver();

    static LongStream seeds() {
        return LongStream.rangeClosed(1, 300);
    }

    static LongStream heavyTieSeeds() {
        return LongStream.rangeClosed(1001, 1100);
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("seeds")
    void matchesBruteForce(long seed) {
        check(seed, 20);
    }

    @ParameterizedTest(name = "seed {0}")
    @MethodSource("heavyTieSeeds")
    void matchesBruteForce_heavyTies(long seed) {
        check(seed, 3);
    }

    private void check(long seed, int maxQuote) {
        Random rnd = new Random(seed * 31 + 17);
        int lanes = 1 + rnd.nextInt(8);
        int transporters = 1 + rnd.nextInt(9);
        int k = 1 + rnd.nextInt(transporters);
        ProblemInstance p = SolverTestData.random(seed, lanes, transporters, 0.45, maxQuote);
        SolverResult expected = bruteForce.solve(p, k, SolverOptions.defaults());
        SolverResult actual = solver.solve(p, k, SolverOptions.defaults());
        assertSameResult(expected, actual);
        if (actual.isFeasible()) {
            assertValid(p, actual, k);
        }
    }
}
