package org.dannyshih.scrabblesolver.solvers;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeUnit;

/**
 * A RecursiveAction class that permutes a collection of strings.
 * If the collection size is less than a threshold, permute the collection directly.
 * Otherwise, divide the collection in half and process in parallel.
 *
 * @author dshih
 */
final class BatchPermuter extends RecursiveAction {
    private static final String THRESHOLD_PROP = "SCABBLE_SOLVER_PERMUTATION_BATCH_THRESHOLD";
    private static final int DEFAULT_THRESHOLD = 200;

    private final List<StringBuilder> m_combinations;
    private final SolveOperationState m_state;
    private final int m_threshold;

    BatchPermuter(List<StringBuilder> combinations, SolveOperationState state) {
        m_combinations = combinations;
        m_state = state;
        m_threshold = System.getenv(THRESHOLD_PROP) == null ?
                DEFAULT_THRESHOLD : Integer.parseInt(System.getenv(THRESHOLD_PROP));
    }

    @Override
    protected void compute() {
        if (m_combinations.size() < m_threshold) {
            final Stopwatch sw = Stopwatch.createStarted();
            long numSmallPermsProcessed = 0;
            for (final StringBuilder combination : m_combinations) {
                numSmallPermsProcessed += Utils.permute(combination, 0, m_state);
            }

            m_state.progress.addNumProcessed(numSmallPermsProcessed);
            m_state.opTimes.add(sw.stop().elapsed(TimeUnit.MILLISECONDS));
        } else {
            final int midIdx = (m_combinations.size() - 1) / 2;
            ForkJoinTask.invokeAll(ImmutableList.of(
                    new BatchPermuter(m_combinations.subList(0, midIdx), m_state),
                    new BatchPermuter(m_combinations.subList(midIdx, m_combinations.size()), m_state)));
        }
    }
}