package com.freightfox.transporter.service;

import com.freightfox.transporter.config.SolverProperties;
import com.freightfox.transporter.dto.AssignmentResponse;
import com.freightfox.transporter.dto.LaneAssignmentDto;
import com.freightfox.transporter.entity.LaneEntity;
import com.freightfox.transporter.entity.LaneQuoteEntity;
import com.freightfox.transporter.entity.TransporterEntity;
import com.freightfox.transporter.exception.InfeasibleAssignmentException;
import com.freightfox.transporter.exception.NoInputDataException;
import com.freightfox.transporter.repository.LaneRepository;
import com.freightfox.transporter.repository.TransporterRepository;
import com.freightfox.transporter.solver.AssignmentSolver;
import com.freightfox.transporter.solver.ProblemInstance;
import com.freightfox.transporter.solver.SolverOptions;
import com.freightfox.transporter.solver.SolverResult;
import com.freightfox.transporter.solver.SolverStatus;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Loads the stored dataset, runs the solver and maps the outcome to the API. */
@Service
public class AssignmentService {

    private final LaneRepository laneRepository;
    private final TransporterRepository transporterRepository;
    private final AssignmentSolver solver;
    private final SolverProperties solverProperties;
    private final TransactionTemplate readOnlyTx;

    public AssignmentService(LaneRepository laneRepository, TransporterRepository transporterRepository,
                             AssignmentSolver solver, SolverProperties solverProperties,
                             PlatformTransactionManager transactionManager) {
        this.laneRepository = laneRepository;
        this.transporterRepository = transporterRepository;
        this.solver = solver;
        this.solverProperties = solverProperties;
        this.readOnlyTx = new TransactionTemplate(transactionManager);
        this.readOnlyTx.setReadOnly(true);
    }

    public AssignmentResponse assign(int maxTransporters) {
        // Load inside a short read-only transaction; the solver then runs without holding a DB connection.
        LoadedData data = readOnlyTx.execute(status -> load());
        List<LaneEntity> lanes = data.lanes();
        ProblemInstance instance = ProblemInstance.of(lanes.stream().map(LaneEntity::getId).toList(), data.quotes());
        SolverResult result = solver.solve(instance, maxTransporters, new SolverOptions(solverProperties.timeBudget()));

        return switch (result.status()) {
            case OPTIMAL, FEASIBLE -> new AssignmentResponse(
                    "success",
                    result.totalCost(),
                    result.assignments().stream()
                            .map(a -> new LaneAssignmentDto(a.laneId(), a.transporterId()))
                            .toList(),
                    result.selectedTransporters(),
                    result.status() == SolverStatus.OPTIMAL);
            case INFEASIBLE_UNCOVERED_LANES -> {
                Map<Integer, LaneEntity> byId = new HashMap<>();
                lanes.forEach(l -> byId.put(l.getId(), l));
                throw new InfeasibleAssignmentException(
                        "Some lanes have no quotes from any transporter, so no assignment can cover all lanes.",
                        result.uncoveredLaneIds().stream().map(id -> {
                            LaneEntity l = byId.get(id);
                            return "Lane %d (%s -> %s) has no transporter quotes."
                                    .formatted(id, l.getOrigin(), l.getDestination());
                        }).toList());
            }
            case INFEASIBLE_TOO_FEW_TRANSPORTERS -> throw new InfeasibleAssignmentException(
                    "Cannot cover all %d lanes with at most %d transporter(s).".formatted(lanes.size(), maxTransporters),
                    List.of("At least %d transporters are required to cover all lanes, but maxTransporters is %d."
                            .formatted(result.minTransportersRequired(), maxTransporters)));
            case NO_SOLUTION_FOUND -> throw new InfeasibleAssignmentException(
                    "No feasible assignment was found within the solver time budget.",
                    List.of("Try increasing maxTransporters."));
        };
    }

    private LoadedData load() {
        List<LaneEntity> lanes = laneRepository.findAll(Sort.by("id"));
        if (lanes.isEmpty()) {
            throw new NoInputDataException();
        }
        Map<Integer, Map<Integer, Long>> quotes = new LinkedHashMap<>();
        for (TransporterEntity t : transporterRepository.findAllWithQuotes()) {
            Map<Integer, Long> row = new HashMap<>();
            for (LaneQuoteEntity q : t.getLaneQuotes()) {
                row.put(q.getLane().getId(), q.getQuote());
            }
            quotes.put(t.getId(), row);
        }
        return new LoadedData(lanes, quotes);
    }

    private record LoadedData(List<LaneEntity> lanes, Map<Integer, Map<Integer, Long>> quotes) {
    }
}
