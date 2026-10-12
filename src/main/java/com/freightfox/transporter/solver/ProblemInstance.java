package com.freightfox.transporter.solver;

import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Immutable problem instance.
 *
 * @param laneIds        lane ids sorted ascending
 * @param transporterIds transporter ids sorted ascending
 * @param cost           cost[transporterIndex][laneIndex], {@link #NO_QUOTE} when there is no quote
 */
public record ProblemInstance(int[] laneIds, int[] transporterIds, long[][] cost) {

    public static final long NO_QUOTE = Long.MAX_VALUE;

    /**
     * Builds an instance from lane ids and quotes keyed by transporter id, then lane id.
     * Quotes for unknown lanes are ignored.
     */
    public static ProblemInstance of(List<Integer> laneIds, Map<Integer, Map<Integer, Long>> quotesByTransporterId) {
        int[] lanes = new TreeSet<>(laneIds).stream().mapToInt(Integer::intValue).toArray();
        int[] transporters = new TreeSet<>(quotesByTransporterId.keySet()).stream().mapToInt(Integer::intValue).toArray();
        long[][] cost = new long[transporters.length][lanes.length];
        for (int t = 0; t < transporters.length; t++) {
            Map<Integer, Long> quotes = quotesByTransporterId.get(transporters[t]);
            for (int l = 0; l < lanes.length; l++) {
                Long q = quotes == null ? null : quotes.get(lanes[l]);
                cost[t][l] = q == null ? NO_QUOTE : q;
            }
        }
        return new ProblemInstance(lanes, transporters, cost);
    }

    public int laneCount() {
        return laneIds.length;
    }

    public int transporterCount() {
        return transporterIds.length;
    }
}
