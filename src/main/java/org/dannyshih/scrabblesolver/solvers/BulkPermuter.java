package org.dannyshih.scrabblesolver.solvers;

import com.google.common.collect.ImmutableList;
import org.dannyshih.scrabblesolver.Progress;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.regex.Pattern;

/**
 * This class is designed to efficiently permute small strings - 8 characters or less. On modern hardware (2022), an
 * 8 character string requires about 20 ms to permute. 3-7 character strings require 6-10 ms.
 *
 * It is inefficient to permute these strings individually, so the strategy is to process them in bulk.
 * The goal is to chunk the collection such that each chunk requires about 200 ms to process. This strikes a balance
 * between allowing work-stealing and not over-coordinating. Accordingly, the threshold is set to 25.
 *
 * @author dshih
 */
final class BulkPermuter extends RecursiveTask<Long> {
    private static final int THRESHOLD = 25;

    private final List<StringBuilder> m_smallCombinations;
    private final Set<String> m_dictionary;
    private final int m_minCharacters;
    private final Pattern m_regex;
    private final Progress m_progress;

    BulkPermuter(
            List<StringBuilder> smallCombinations,
            Set<String> dictionary,
            int minCharacters,
            Pattern regex,
            Progress progress) {
        m_smallCombinations = smallCombinations;
        m_dictionary = dictionary;
        m_minCharacters = minCharacters;
        m_regex = regex;
        m_progress = progress;
    }

    @Override
    protected Long compute() {
        if (m_smallCombinations.size() >= THRESHOLD) {
            long numSmallPermsProcessed = 0;
            for (final StringBuilder combination : m_smallCombinations) {
                numSmallPermsProcessed += Solver.permute(combination, 0, m_progress, permutation -> {
                    if (m_dictionary.contains(permutation) &&
                            permutation.length() >= m_minCharacters &&
                            m_regex.matcher(permutation).matches()) {

                        m_progress.addSolution(permutation);
                    }
                });
            }

            return numSmallPermsProcessed;
        } else {
            return ForkJoinTask.invokeAll(createSubtasks()).stream().mapToLong(ForkJoinTask::join).sum();
        }
    }

    private List<BulkPermuter> createSubtasks() {
        final int midIdx = (m_smallCombinations.size() - 1) / 2;
        return ImmutableList.of(
                new BulkPermuter(
                        m_smallCombinations.subList(0, midIdx), m_dictionary, m_minCharacters, m_regex, m_progress),
                new BulkPermuter(
                        m_smallCombinations.subList(
                                midIdx, m_smallCombinations.size()), m_dictionary, m_minCharacters, m_regex, m_progress));
    }
}
