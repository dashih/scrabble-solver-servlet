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

class Solver {
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

    void solve(String input, boolean parallel, int minCharacters, Pattern regex, Progress progress, ServletContext ctx) {
        Preconditions.checkArgument(StringUtils.isNotBlank(input));

        final List<StringBuilder> combinations = new ArrayList<>();
        final AtomicLong totalPermutations = new AtomicLong();
        generateCombinationsWithBlanks(new StringBuilder(input), totalPermutations, combinations);
        progress.start(totalPermutations.get());
        ctx.log("generated combinations: " + combinations.size());
        if (parallel) {
            final List<ForkJoinTask<Long>> tasks = new ArrayList<>();
            combinations.forEach(combination ->
                tasks.add(m_pool.submit(new Permuter(combination, 0, m_dictionary, minCharacters, regex, progress))));

            tasks.forEach(task -> progress.addNumProcessed(task.join()));
        } else {
            combinations.forEach(combination -> {
                long numJustProcessed = permute(combination, 0, minCharacters, regex, progress, m_dictionary);
                progress.addNumProcessed(numJustProcessed);
            });
        }

        progress.finish();
    }

    private void populateDictionary() throws IOException {
        InputStream in = Preconditions.checkNotNull(getClass().getResourceAsStream("/dictionary.txt"));
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            br.lines().forEach(m_dictionary::add);
        }
    }

    private static void generateCombinationsWithBlanks(
        StringBuilder sb, AtomicLong totalPermutations, List<StringBuilder> combinations) {

        for (int i = 0; i < sb.length(); i++) {
            if (sb.charAt(i) == '*') {
                for (char c : ALPHABET.toCharArray()) {
                    sb.setCharAt(i, c);
                    generateCombinationsWithBlanks(sb, totalPermutations, combinations);
                }

                return;
            }
        }

        getCombinations(sb, new StringBuilder(), 0, totalPermutations, combinations);
    }

    private static void getCombinations(
        StringBuilder sb, StringBuilder build, int idx, AtomicLong totalPermutations, List<StringBuilder> combinations) {

        for (int i = idx; i < sb.length(); i++) {
            build.append(sb.charAt(i));

            combinations.add(new StringBuilder(build));
            totalPermutations.addAndGet(BigIntegerMath.factorial(build.length()).longValueExact());

            getCombinations(sb, build, i + 1, totalPermutations, combinations);
            build.deleteCharAt(build.length() - 1);
        }
    }

    static long permute(StringBuilder sb, int idx, int minCharacters, Pattern regex, Progress progress, Set<String> dictionary) {
        if (progress.getRunStatus() == Progress.RunStatus.Canceled) {
            throw new CancellationException();
        }

        if (idx == sb.length()) {
            String str = sb.toString();

            // Check if it's a unique solution that meets the criteria.
            if (dictionary.contains(str) && str.length() >= minCharacters && regex.matcher(str).matches()) {
                progress.addSolution(str);
            }

            return 1L;
        }

        long numProcessed = 0L;
        for (int i = idx; i < sb.length(); i++) {
            swap(sb, idx, i);
            numProcessed += permute(sb, idx + 1, minCharacters, regex, progress, dictionary);
            swap(sb, idx, i);
        }

        return numProcessed;
    }

    static void swap(StringBuilder sb, int idx0, int idx1) {
        char tmp = sb.charAt(idx0);
        sb.setCharAt(idx0, sb.charAt(idx1));
        sb.setCharAt(idx1, tmp);
    }
}
