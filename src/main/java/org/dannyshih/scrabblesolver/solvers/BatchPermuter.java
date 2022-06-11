package org.dannyshih.scrabblesolver.solvers;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeUnit;

final class BatchPermuter extends RecursiveAction {
    private static final int THRESHOLD = 200;

    private final List<StringBuilder> m_combinations;
    private final SolveOperationState m_state;

    BatchPermuter(List<StringBuilder> combinations, SolveOperationState state) {
        m_combinations = combinations;
        m_state = state;
    }

    @Override
    protected void compute() {
        if (m_combinations.size() < THRESHOLD) {
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