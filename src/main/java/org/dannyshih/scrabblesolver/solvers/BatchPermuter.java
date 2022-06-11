package org.dannyshih.scrabblesolver.solvers;

import com.google.common.collect.ImmutableList;
import org.dannyshih.scrabblesolver.Progress;

import java.util.List;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

final class BatchPermuter extends RecursiveAction {
    private static final int THRESHOLD = 256;

    private final List<StringBuilder> m_combinations;
    private final Trie m_dictionary;
    private final int m_minCharacters;
    private final Pattern m_regex;
    private final Progress m_progress;
    private final AtomicBoolean m_isCancellationRequested;

    BatchPermuter(
            List<StringBuilder> combinations,
            Trie dictionary,
            int minCharacters,
            Pattern regex,
            Progress progress,
            AtomicBoolean isCancellationRequested) {
        m_combinations = combinations;
        m_dictionary = dictionary;
        m_minCharacters = minCharacters;
        m_regex = regex;
        m_progress = progress;
        m_isCancellationRequested = isCancellationRequested;
    }

    @Override
    protected void compute() {
        if (m_combinations.size() < THRESHOLD) {
            long numSmallPermsProcessed = 0;
            for (final StringBuilder combination : m_combinations) {
                numSmallPermsProcessed += Utils.permute(
                        combination,
                        0,
                        m_dictionary,
                        m_minCharacters,
                        m_regex,
                        m_progress,
                        m_isCancellationRequested);
            }

            m_progress.addNumProcessed(numSmallPermsProcessed);
        } else {
            final int midIdx = (m_combinations.size() - 1) / 2;
            ForkJoinTask.invokeAll(ImmutableList.of(
                    create(0, midIdx),
                    create(midIdx, m_combinations.size())));
        }
    }

    private BatchPermuter create(int fromInclusive, int toExclusive) {
        return new BatchPermuter(
                m_combinations.subList(fromInclusive, toExclusive),
                m_dictionary,
                m_minCharacters,
                m_regex,
                m_progress,
                m_isCancellationRequested);
    }
}