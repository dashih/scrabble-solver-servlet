package org.dannyshih.scrabblesolver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.google.common.math.BigIntegerMath;

import javax.servlet.ServletContext;

public final class Solver {
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private final Set<String> m_dictionary;
    private final ForkJoinPool m_pool;

    /**
     * ctor
     */
    Solver() throws IOException {
        m_dictionary = new HashSet<>();
        populateDictionary();
        m_pool = ForkJoinPool.commonPool();
    }

    /**
     * Solve
     */
    void solve(String input, boolean parallel, int minCharacters, Pattern regex, Progress progress, ServletContext ctx) {
        Preconditions.checkArgument(StringUtils.isNotBlank(input));

        final List<StringBuilder> combinations = new ArrayList<>();
        final AtomicLong totalPerms = new AtomicLong(0L);
        getCombinationswithBlanks(new StringBuilder(input), combinations, totalPerms);
        progress.start(totalPerms.get());
        if (parallel) {
            final List<ForkJoinTask<Long>> tasks = new ArrayList<>();
            combinations.forEach(combination -> {
                tasks.add(m_pool.submit(new Permuter(combination, 0, m_dictionary, minCharacters, regex, progress)));
            });

            tasks.forEach(task -> progress.addNumProcessed(task.join()));
        } else {
            combinations.forEach(combination -> permute(combination, 0, minCharacters, regex, progress));
        }

        progress.finish();
    }

    private void populateDictionary() throws IOException {
        InputStream in = Preconditions.checkNotNull(getClass().getResourceAsStream("/dictionary.txt"));
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            br.lines().forEach(m_dictionary::add);
        }
    }

    private static void getCombinationswithBlanks(
        StringBuilder s, List<StringBuilder> combinations, AtomicLong totalPermutations) {

        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '*') {
                for (char c : ALPHABET.toCharArray()) {
                    s.setCharAt(i, c);
                    getCombinationswithBlanks(s, combinations, totalPermutations);
                }

                return;
            }
        }

        getCombinations(s, new StringBuilder(), 0, combinations, totalPermutations);
    }

    private static void getCombinations(
        StringBuilder s, StringBuilder build, int idx, List<StringBuilder> combinations, AtomicLong totalPermutations) {

        for (int i = idx; i < s.length(); i++) {
            build.append(s.charAt(i));

            combinations.add(new StringBuilder(build));
            totalPermutations.addAndGet(BigIntegerMath.factorial(build.length()).longValueExact());

            getCombinations(s, build, i + 1, combinations, totalPermutations);
            build.deleteCharAt(build.length() - 1);
        }
    }

    private void permute(StringBuilder s, int idx, int minCharacters, Pattern regex, Progress progress) {
        if (progress.getRunStatus() == Progress.RunStatus.Canceled) {
            throw new CancellationException();
        }

        if (idx == s.length()) {
            String str = s.toString();

            // Check if it's a unique solution that meets the criteria.
            if (m_dictionary.contains(str) && str.length() >= minCharacters && regex.matcher(str).matches()) {
                progress.addSolution(str);
            }

            progress.addNumProcessed(1L);
            return;
        }

        for (int i = idx; i < s.length(); i++) {
            swap(s, idx, i);
            permute(s, idx + 1, minCharacters, regex, progress);
            swap(s, idx, i);
        }
    }

    private static void swap(StringBuilder s, int idx0, int idx1) {
        char tmp = s.charAt(idx0);
        s.setCharAt(idx0, s.charAt(idx1));
        s.setCharAt(idx1, tmp);
    }
}
