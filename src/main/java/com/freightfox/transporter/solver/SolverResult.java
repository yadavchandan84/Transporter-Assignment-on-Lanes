package com.freightfox.transporter.solver;

import java.util.List;

/**
 * Result of a solver run.
 *
 * @param status                  outcome
 * @param totalCost               total cost of the assignment (0 when infeasible)
 * @param assignments             lane assignments sorted by lane id (empty when infeasible)
 * @param selectedTransporters    transporters actually used, ascending (empty when infeasible)
 * @param uncoveredLaneIds        lanes without any quote (only for INFEASIBLE_UNCOVERED_LANES)
 * @param minTransportersRequired minimum number of transporters needed (only for INFEASIBLE_TOO_FEW_TRANSPORTERS)
 * @param nodesExplored           search nodes explored (diagnostics)
 */
public record SolverResult(
        SolverStatus status,
        long totalCost,
        List<LaneAssignment> assignments,
        List<Integer> selectedTransporters,
        List<Integer> uncoveredLaneIds,
        Integer minTransportersRequired,
        long nodesExplored) {

    public SolverResult {
        assignments = List.copyOf(assignments);
        selectedTransporters = List.copyOf(selectedTransporters);
        uncoveredLaneIds = List.copyOf(uncoveredLaneIds);
    }

    public boolean isFeasible() {
        return status == SolverStatus.OPTIMAL || status == SolverStatus.FEASIBLE;
    }

    public boolean isOptimal() {
        return status == SolverStatus.OPTIMAL;
    }

    public static SolverResult optimal(long totalCost, List<LaneAssignment> assignments,
                                       List<Integer> selected, long nodes) {
        return new SolverResult(SolverStatus.OPTIMAL, totalCost, assignments, selected, List.of(), null, nodes);
    }

    public static SolverResult feasible(long totalCost, List<LaneAssignment> assignments,
                                        List<Integer> selected, long nodes) {
        return new SolverResult(SolverStatus.FEASIBLE, totalCost, assignments, selected, List.of(), null, nodes);
    }

    public static SolverResult uncovered(List<Integer> uncoveredLaneIds) {
        return new SolverResult(SolverStatus.INFEASIBLE_UNCOVERED_LANES, 0, List.of(), List.of(),
                uncoveredLaneIds, null, 0);
    }

    public static SolverResult tooFew(int minRequired) {
        return new SolverResult(SolverStatus.INFEASIBLE_TOO_FEW_TRANSPORTERS, 0, List.of(), List.of(),
                List.of(), minRequired, 0);
    }

    public static SolverResult noSolution() {
        return new SolverResult(SolverStatus.NO_SOLUTION_FOUND, 0, List.of(), List.of(), List.of(), null, 0);
    }
}
