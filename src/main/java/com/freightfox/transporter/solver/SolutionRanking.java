package com.freightfox.transporter.solver;

/**
 * Single source of truth for comparing two solutions. Lower is better:
 * <ol>
 *   <li>total cost ascending;</li>
 *   <li>number of distinct transporters actually used, descending;</li>
 *   <li>sorted list of used transporter ids, lexicographically ascending.</li>
 * </ol>
 */
public final class SolutionRanking {

    private SolutionRanking() {
    }

    /**
     * @param usedA sorted ascending ids of transporters used by solution A
     * @param usedB sorted ascending ids of transporters used by solution B
     * @return negative if A is better, positive if B is better, 0 if equivalent
     */
    public static int compare(long costA, int[] usedA, long costB, int[] usedB) {
        int c = Long.compare(costA, costB);
        if (c != 0) {
            return c;
        }
        c = Integer.compare(usedB.length, usedA.length);
        if (c != 0) {
            return c;
        }
        for (int i = 0; i < usedA.length; i++) {
            c = Integer.compare(usedA[i], usedB[i]);
            if (c != 0) {
                return c;
            }
        }
        return 0;
    }
}
