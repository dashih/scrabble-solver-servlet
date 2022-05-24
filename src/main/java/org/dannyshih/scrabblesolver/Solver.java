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
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.google.common.math.BigIntegerMath;

public final class Solver {
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    // At what point do we also parallelize permuting. 9 characters including 2 blanks seems to be a good threshold.
    private static final int PERMUTATION_PARALLEL_THRESH = 9;

    private final Set<String> m_dictionary;
    private final boolean m_parallel;
    private final int m_minCharacters;
    private final Pattern m_regex;

    /**
     * ctor
     */
    Solver(boolean parallel, int minCharacters, Pattern regex) throws IOException {
        m_parallel = parallel;
        m_minCharacters = minCharacters;
        m_regex = Preconditions.checkNotNull(regex);

        m_dictionary = new HashSet<>();
        populateDictionary();
    }

    /**
     * Solve
     */
    void solve(String input, Progress progress) {
        Preconditions.checkArgument(StringUtils.isNotBlank(input));

        List<StringBuilder> combinations = new ArrayList<>();
        AtomicLong totalPerms = new AtomicLong(0L);
        getCombinationswithBlanks(new StringBuilder(input), combinations, totalPerms);
        progress.start(totalPerms.get());
        if (m_parallel) {
            combinations.parallelStream().forEach(combination -> {
                if (combination.length() >= PERMUTATION_PARALLEL_THRESH) {
                    List<StringBuilder> permStartPoints = new ArrayList<>();
                    for (int i = 0; i < combination.length(); i++) {
                        swap(combination, 0, i);
                        permStartPoints.add(new StringBuilder(combination));
                        swap(combination, 0, i);
                    }

                    permStartPoints.parallelStream().forEach(
                            startPoint -> permute(startPoint, 1, progress));
                } else {
                    permute(combination, 0, progress);
                }
            });
        } else {
            combinations.forEach(combination -> permute(combination, 0, progress));
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

    private void permute(StringBuilder s, int idx, Progress progress) {
        if (progress.getRunStatus() == Progress.RunStatus.Canceled) {
            throw new CancellationException();
        }

        if (idx == s.length()) {
            String str = s.toString();

            // Check if it's a unique solution that meets the criteria.
            if (m_dictionary.contains(str) && str.length() >= m_minCharacters && m_regex.matcher(str).matches()) {
                progress.addSolution(str);
            }

            progress.increment();
            return;
        }

        for (int i = idx; i < s.length(); i++) {
            swap(s, idx, i);
            permute(s, idx + 1, progress);
            swap(s, idx, i);
        }
    }

    private static void swap(StringBuilder s, int idx0, int idx1) {
        char tmp = s.charAt(idx0);
        s.setCharAt(idx0, s.charAt(idx1));
        s.setCharAt(idx1, tmp);
    }
}
