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
            return permute(m_sb, m_idx);
        } else {
            return ForkJoinTask.invokeAll(createSubtasks()).stream().mapToLong(ForkJoinTask::join).sum();
        }
    }

    private List<Permuter> createSubtasks() {
        final List<Permuter> subtasks = new ArrayList<>();
        for (int i = m_idx; i < m_sb.length(); i++) {
            swap(m_sb, m_idx, i);
            subtasks.add(new Permuter(new StringBuilder(m_sb), m_idx + 1, m_dictionary, m_minCharacters, m_regex, m_progress));
            swap(m_sb, m_idx, i);
        }

        return subtasks;
    }

    private long permute(StringBuilder s, int idx) {
        if (idx == s.length()) {
            String str = s.toString();
            if (m_dictionary.contains(str) && str.length() >= m_minCharacters &&  m_regex.matcher(str).matches()) {
                m_progress.addSolution(str);
            }

            return 1L;
        }

        long res = 0L;
        for (int i = idx; i < s.length(); i++) {
            swap(s, idx, i);
            res += permute(s, idx + 1);
            swap(s, idx, i);
        }

        return res;
    }

    private static void swap(StringBuilder s, int idx0, int idx1) {
        char tmp = s.charAt(idx0);
        s.setCharAt(idx0, s.charAt(idx1));
        s.setCharAt(idx1, tmp);
    }
}
