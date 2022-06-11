package org.dannyshih.scrabblesolver.solvers;

import com.google.common.math.BigIntegerMath;
import org.dannyshih.scrabblesolver.Progress;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
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
    private final Trie m_dictionary;
    private final int m_minCharacters;
    private final Pattern m_regex;
    private final Progress m_progress;
    private final AtomicBoolean m_isCancellationRequested;

    Permuter(
            StringBuilder sb,
            int idx,
            Trie dictionary,
            int minCharacters,
            Pattern regex,
            Progress progress,
            AtomicBoolean isCancellationRequested) {
        m_sb = sb;
        m_idx = idx;
        m_dictionary = dictionary;
        m_minCharacters = minCharacters;
        m_regex = regex;
        m_progress = progress;
        m_isCancellationRequested = isCancellationRequested;
    }

    @Override
    protected void compute() {
        if (m_sb.length() - m_idx <= THRESHOLD) {
            // We can get away with updating progress on task completion instead of every permutation, because the
            // parallel solver breaks down the work into very small tasks. So tasks will always be completing at a high
            // clip, and UI updates will happen at the same rate.
            // The serial solver must update on every permutation, because otherwise, it might get stuck
            // processing a large string and not provide a status update for a long time.
            m_progress.addNumProcessed(permute(m_sb, m_idx));
        } else {
            // Prune before even submitting subtasks.
            if (m_idx > 0 && !m_dictionary.beginsWord(m_sb.substring(0, m_idx))) {
                m_progress.addNumProcessed(BigIntegerMath.factorial(m_sb.length() - m_idx).longValueExact());
            } else {
                ForkJoinTask.invokeAll(createSubtasks());
            }
        }
    }

    private List<Permuter> createSubtasks() {
        final List<Permuter> subtasks = new ArrayList<>();
        for (int i = m_idx; i < m_sb.length(); i++) {
            Utils.swap(m_sb, m_idx, i);
            subtasks.add(new Permuter(
                    new StringBuilder(m_sb),
                    m_idx + 1,
                    m_dictionary,
                    m_minCharacters,
                    m_regex,
                    m_progress,
                    m_isCancellationRequested));
            Utils.swap(m_sb, m_idx, i);
        }

        return subtasks;
    }

    private long permute(StringBuilder sb, int idx) {
        if (m_isCancellationRequested.get()) {
            throw new CancellationException();
        }

        if (idx == sb.length()) {
            final String s = sb.toString();
            if (m_dictionary.isWord(s) && s.length() >= m_minCharacters && m_regex.matcher(s).matches()) {
                m_progress.addSolution(s);
            }

            return 1L;
        }

        if (idx > 0 && !m_dictionary.beginsWord(sb.substring(0, idx))) {
            return BigIntegerMath.factorial(sb.length() - idx).longValueExact();
        }

        long numProcessed = 0L;
        for (int i = idx; i < sb.length(); i++) {
            Utils.swap(sb, idx, i);
            numProcessed += permute(sb, idx + 1);
            Utils.swap(sb, idx, i);
        }

        return numProcessed;
    }
}
