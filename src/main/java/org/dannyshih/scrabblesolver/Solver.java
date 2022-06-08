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
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.google.common.math.BigIntegerMath;

import javax.servlet.ServletContext;

class Solver {
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private final Set<String> m_dictionary;
    private final ForkJoinPool m_pool;

    Solver() throws IOException {
        m_dictionary = new HashSet<>();
        populateDictionary();
        m_pool = ForkJoinPool.commonPool();
    }

    void solve(String input, boolean parallel, int minCharacters, Pattern regex, Progress progress, ServletContext ctx) {
        Preconditions.checkArgument(StringUtils.isNotBlank(input));

        final List<StringBuilder> combinations = new ArrayList<>();
        final AtomicLong totalPermutations = new AtomicLong();
        generateCombinationsWithBlanks(new StringBuilder(input), combination -> {
            combinations.add(combination);
            totalPermutations.addAndGet(BigIntegerMath.factorial(combination.length()).longValueExact());
        });

        progress.start(totalPermutations.get());
        ctx.log("Solver:: generated combinations: " + combinations.size());
        if (parallel) {
            ctx.log("Solver:: parallelism: " + m_pool.getParallelism());

            final List<StringBuilder> smalls = new ArrayList<>();
            final List<StringBuilder> larges = new ArrayList<>();
            combinations.forEach(combination -> {
                if (combination.length() <= 6) {
                    smalls.add(combination);
                } else {
                    larges.add(combination);
                }
            });

            final List<ForkJoinTask<Long>> tasks = new ArrayList<>();
            larges.forEach(combination ->
                tasks.add(m_pool.submit(new Permuter(combination, 0, m_dictionary, minCharacters, regex, progress))));
            ctx.log("Solver :: submitted " + tasks.size() + " tasks to ForkJoin framework for large combinations");

            int numSmallPermsProcessed = 0;
            for (final StringBuilder combination : smalls) {
                numSmallPermsProcessed += permute(combination, 0, progress, permutation -> {
                    if (m_dictionary.contains(permutation) &&
                        permutation.length() >= minCharacters &&
                        regex.matcher(permutation).matches()) {

                        progress.addSolution(permutation);
                    }
                });
            }

            ctx.log("Solver :: processed " + smalls.size() + " small combinations");
            progress.addNumProcessed(numSmallPermsProcessed);

            tasks.forEach(task -> progress.addNumProcessed(task.join()));
            ctx.log("Solver:: steal count: " + m_pool.getStealCount());
        } else {
            combinations.forEach(combination ->
                    permute(combination, 0, progress, permutation -> {
                        if (m_dictionary.contains(permutation) &&
                                permutation.length() >= minCharacters &&
                                regex.matcher(permutation).matches()) {

                            progress.addSolution(permutation);
                        }

                        progress.addNumProcessed(1L);
                    }));
        }

        progress.finish();
    }

    private void populateDictionary() throws IOException {
        InputStream in = Preconditions.checkNotNull(getClass().getResourceAsStream("/dictionary.txt"));
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            br.lines().forEach(m_dictionary::add);
        }
    }

    private static void generateCombinationsWithBlanks(StringBuilder sb, Consumer<StringBuilder> combinationConsumer) {

        for (int i = 0; i < sb.length(); i++) {
            if (sb.charAt(i) == '*') {
                for (char c : ALPHABET.toCharArray()) {
                    sb.setCharAt(i, c);
                    generateCombinationsWithBlanks(sb, combinationConsumer);
                }

                return;
            }
        }

        getCombinations(sb, new StringBuilder(), 0, combinationConsumer);
    }

    private static void getCombinations(
        StringBuilder sb, StringBuilder build, int idx, Consumer<StringBuilder> combinationConsumer) {

        for (int i = idx; i < sb.length(); i++) {
            build.append(sb.charAt(i));

            combinationConsumer.accept(new StringBuilder(build));

            getCombinations(sb, build, i + 1, combinationConsumer);
            build.deleteCharAt(build.length() - 1);
        }
    }

    static long permute(StringBuilder sb, int idx, Progress progress, Consumer<String> permutationConsumer) {
        if (progress.getRunStatus() == Progress.RunStatus.Canceled) {
            throw new CancellationException();
        }

        if (idx == sb.length()) {
            String str = sb.toString();
            permutationConsumer.accept(str);
            return 1L;
        }

        long numProcessed = 0L;
        for (int i = idx; i < sb.length(); i++) {
            swap(sb, idx, i);
            numProcessed += permute(sb, idx + 1, progress, permutationConsumer);
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
