package org.dannyshih.scrabblesolver.solvers;

import org.dannyshih.scrabblesolver.Progress;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * A RecursiveTask to permute a string.
 *
 * If the permutation range is less than or equal to the threshold, permute it directly.
 *
 * Otherwise, divide and conqueror. Take each character in the permutation range and create a child string that starts
 * with that character. These child strings can be permuted from the next character onwards to produce the same results
 * as permuting the parent string. The child string permutations can be done in parallel, and the permutation range
 * is reduced by one.
 *
 * @author dshih
 */
final class Permuter extends RecursiveAction {
    // Experimentally tuned. See README.
    private static final int THRESHOLD = 6;

    private final StringBuilder m_sb;
    private final int m_idx;
    private final Set<String> m_dictionary;
    private final int m_minCharacters;
    private final Pattern m_regex;
    private final Progress m_progress;
    private final AtomicBoolean m_isCanceled;

    Permuter(
            StringBuilder sb,
            int idx, Set<String> dictionary,
            int minCharacters,
            Pattern regex,
            Progress progress,
            AtomicBoolean isCanceled) {
        m_sb = sb;
        m_idx = idx;
        m_dictionary = dictionary;
        m_minCharacters = minCharacters;
        m_regex = regex;
        m_progress = progress;
        m_isCanceled = isCanceled;
    }

    @Override
    protected void compute() {
        if (m_sb.length() - m_idx <= THRESHOLD) {
            final long numProcessed = Solver.permute(m_sb, m_idx, m_isCanceled, permutation -> {
                if (m_dictionary.contains(permutation) &&
                    permutation.length() >= m_minCharacters &&
                    m_regex.matcher(permutation).matches()) {

                    m_progress.addSolution(permutation);
                }
            });

            // We can get away with updating progress on task completion instead of every permutation, because the
            // biggest tasks only require 200 ms to complete (so that's the maximum amount of time between status
            // updates). The serial solver must update on every permutation, because otherwise, it might get stuck
            // processing a large string and not provide a status update for hours.
            m_progress.addNumProcessed(numProcessed);
        } else {
            ForkJoinTask.invokeAll(createSubtasks());
        }
    }

    private List<Permuter> createSubtasks() {
        final List<Permuter> subtasks = new ArrayList<>();
        for (int i = m_idx; i < m_sb.length(); i++) {
            Solver.swap(m_sb, m_idx, i);
            subtasks.add(new Permuter(
                    new StringBuilder(m_sb),
                    m_idx + 1,
                    m_dictionary,
                    m_minCharacters,
                    m_regex,
                    m_progress,
                    m_isCanceled));
            Solver.swap(m_sb, m_idx, i);
        }

        return subtasks;
    }
}
