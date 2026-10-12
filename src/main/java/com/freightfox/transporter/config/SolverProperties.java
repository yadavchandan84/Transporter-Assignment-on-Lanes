package com.freightfox.transporter.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Solver configuration ({@code solver.time-budget}). */
@ConfigurationProperties("solver")
public record SolverProperties(@DefaultValue("5s") Duration timeBudget) {
}
