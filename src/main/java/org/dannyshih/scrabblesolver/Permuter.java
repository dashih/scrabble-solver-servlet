package org.dannyshih.scrabblesolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.regex.Pattern;

public final class Permuter extends RecursiveTask<Long> {
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
            return Solver.permute(m_sb, m_idx, m_minCharacters, m_regex, m_progress, m_dictionary);
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
