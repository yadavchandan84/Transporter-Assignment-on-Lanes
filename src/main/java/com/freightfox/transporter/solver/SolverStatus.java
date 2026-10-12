package com.freightfox.transporter.solver;

/** Outcome of a solver run. */
public enum SolverStatus {
    /** Proven optimal solution. */
    OPTIMAL,
    /** Best solution found, but optimality was not proven (time budget exhausted or heuristic). */
    FEASIBLE,
    /** At least one lane has no quote from any transporter. */
    INFEASIBLE_UNCOVERED_LANES,
    /** All lanes cannot be covered by at most maxTransporters transporters. */
    INFEASIBLE_TOO_FEW_TRANSPORTERS,
    /** Time budget exhausted before any feasible solution was found. */
    NO_SOLUTION_FOUND
}
