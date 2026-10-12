package com.freightfox.transporter.solver;

/** A single lane assigned to a single transporter. */
public record LaneAssignment(int laneId, int transporterId) {
}
