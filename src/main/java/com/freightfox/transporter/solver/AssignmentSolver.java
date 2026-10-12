package com.freightfox.transporter.solver;

/** Chooses at most {@code maxTransporters} transporters covering all lanes at minimum total cost. */
public interface AssignmentSolver {

    SolverResult solve(ProblemInstance instance, int maxTransporters, SolverOptions options);
}
