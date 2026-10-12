package com.freightfox.transporter.solver;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Shared structural checks for solver results. */
final class SolverAssertions {

    private SolverAssertions() {
    }

    /** Every lane assigned once, to a quoting selected transporter; |selected| <= k; cost matches. */
    static void assertValid(ProblemInstance instance, SolverResult r, int k) {
        assertThat(r.isFeasible()).isTrue();
        assertThat(r.selectedTransporters()).isSorted().doesNotHaveDuplicates();
        assertThat(r.selectedTransporters().size()).isLessThanOrEqualTo(k);
        assertThat(r.assignments()).hasSize(instance.laneCount());
        Set<Integer> used = new HashSet<>();
        long total = 0;
        for (int l = 0; l < instance.laneCount(); l++) {
            LaneAssignment a = r.assignments().get(l);
            assertThat(a.laneId()).isEqualTo(instance.laneIds()[l]);
            assertThat(r.selectedTransporters()).contains(a.transporterId());
            int t = Arrays.binarySearch(instance.transporterIds(), a.transporterId());
            long quote = instance.cost()[t][l];
            assertThat(quote).isNotEqualTo(ProblemInstance.NO_QUOTE);
            total += quote;
            used.add(a.transporterId());
        }
        assertThat(used).containsExactlyInAnyOrderElementsOf(r.selectedTransporters());
        assertThat(r.totalCost()).isEqualTo(total);
    }

    /** Status and every reported field equal. */
    static void assertSameResult(SolverResult expected, SolverResult actual) {
        assertThat(actual.status()).isEqualTo(expected.status());
        assertThat(actual.totalCost()).isEqualTo(expected.totalCost());
        assertThat(actual.assignments()).isEqualTo(expected.assignments());
        assertThat(actual.selectedTransporters()).isEqualTo(expected.selectedTransporters());
        assertThat(actual.uncoveredLaneIds()).isEqualTo(expected.uncoveredLaneIds());
        assertThat(actual.minTransportersRequired()).isEqualTo(expected.minTransportersRequired());
    }
}
