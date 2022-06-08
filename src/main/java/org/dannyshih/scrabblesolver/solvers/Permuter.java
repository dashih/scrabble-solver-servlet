package org.dannyshih.scrabblesolver.solvers;

import org.dannyshih.scrabblesolver.Progress;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.regex.Pattern;

/**
 * This class is designed to permute large strings - 9 characters or more. These strings quickly become expensive.
 *
 * The standard method to parallelize permutation of a large string is to take each character and produce strings that
 * start with those characters. These strings can then be permuted in parallel from the second character onwards.
 * This process can be recursed until the desired permutation range is attained.
 *
 * On modern hardware (2022), an 11 character strings requires 1500 ms to permute, and a 10 character string requires
 * 200 ms. This class uses the above strategy to break larger strings down to units that require no more than 200 ms
 * to process.
 *
 * @author dshih
 */
final class Permuter extends RecursiveTask<Long> {
    private static final int THRESHOLD = 11;

    private final StringBuilder m_sb;
    private final int m_idx;
    private final Set<String> m_dictionary;
    private final int m_minCharacters;
    private final Pattern m_regex;
    private final Progress m_progress;

    Permuter(StringBuilder sb, int idx, Set<String> dictionary, int minCharacters, Pattern regex, Progress progress) {
        m_sb = sb;
        m_idx = idx;
        m_dictionary = dictionary;
        m_minCharacters = minCharacters;
        m_regex = regex;
        m_progress = progress;
    }

    @Override
    protected Long compute() {
        if (m_sb.length() <= THRESHOLD) {
            return Solver.permute(m_sb, m_idx, m_progress, permutation -> {
                if (m_dictionary.contains(permutation) &&
                    permutation.length() >= m_minCharacters &&
                    m_regex.matcher(permutation).matches()) {

                    m_progress.addSolution(permutation);
                }
            });
        } else {
            return ForkJoinTask.invokeAll(createSubtasks()).stream().mapToLong(ForkJoinTask::join).sum();
        }
    }

    private List<Permuter> createSubtasks() {
        final List<Permuter> subtasks = new ArrayList<>();
        for (int i = m_idx; i < m_sb.length(); i++) {
            Solver.swap(m_sb, m_idx, i);
            subtasks.add(new Permuter(new StringBuilder(m_sb), m_idx + 1, m_dictionary, m_minCharacters, m_regex, m_progress));
            Solver.swap(m_sb, m_idx, i);
        }

        return subtasks;
    }
}
