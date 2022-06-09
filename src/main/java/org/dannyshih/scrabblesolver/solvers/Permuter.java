package org.dannyshih.scrabblesolver.solvers;

import org.dannyshih.scrabblesolver.Progress;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * A RecursiveTask to permute a string.
 *
 * The standard method to parallelize permutation of a large string is to take each character and produce strings that
 * start with those characters. These strings can then be permuted in parallel from the second character onwards.
 * This process can be recursed until the desired permutation range is attained.
 *
 * On modern hardware (2019 Macbook Pro, 2.4 GHz Intel Core i9), an 11 character string requires 1500 ms to permute,
 * and a 10 character string requires 200 ms. Subtasks that require a maximum of 200 ms to process strike a good
 * balance between optimizing core utilization through work-stealing and minimizing coordination overhead.
 *
 * @author dshih
 */
final class Permuter extends RecursiveAction {
    private static final int THRESHOLD = 11;

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
        if (m_isCanceled.get()) {
            throw new CancellationException();
        }

        if (m_sb.length() <= THRESHOLD) {
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
