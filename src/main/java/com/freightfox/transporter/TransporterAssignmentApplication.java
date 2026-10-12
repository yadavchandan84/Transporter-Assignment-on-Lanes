package com.freightfox.transporter;

import com.freightfox.transporter.solver.AssignmentSolver;
import com.freightfox.transporter.solver.BranchAndBoundSolver;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TransporterAssignmentApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransporterAssignmentApplication.class, args);
    }

    /** Exact branch-and-bound solver with heuristic fallback when the time budget is exhausted. */
    @Bean
    public AssignmentSolver assignmentSolver() {
        return new BranchAndBoundSolver();
    }
}
