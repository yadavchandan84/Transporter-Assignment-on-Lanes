package com.freightfox.transporter.solver;

import java.time.Duration;
import java.util.Objects;

/** Solver tuning options. */
public record SolverOptions(Duration timeBudget) {

    public SolverOptions {
        Objects.requireNonNull(timeBudget, "timeBudget");
        if (timeBudget.isNegative()) {
            timeBudget = Duration.ZERO;
        }
    }

    public static SolverOptions defaults() {
        return new SolverOptions(Duration.ofSeconds(5));
    }
}
