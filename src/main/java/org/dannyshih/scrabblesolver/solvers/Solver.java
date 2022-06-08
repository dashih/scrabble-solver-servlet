package org.dannyshih.scrabblesolver.solvers;

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
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.google.common.math.BigIntegerMath;
import org.dannyshih.scrabblesolver.Progress;

import javax.servlet.ServletContext;

public abstract class Solver {
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    protected final Set<String> m_dictionary;

    public Solver() throws IOException {
        m_dictionary = new HashSet<>();
        populateDictionary();
    }

    protected abstract void doSolve(
            List<StringBuilder> combinations, int minCharacters, Pattern regex, Progress progress, ServletContext ctx);

    public void solve(String input, int minCharacters, Pattern regex, Progress progress, ServletContext ctx) {
        Preconditions.checkArgument(StringUtils.isNotBlank(input));

        final List<StringBuilder> combinations = new ArrayList<>();
        final AtomicLong totalPermutations = new AtomicLong();
        generateCombinationsWithBlanks(new StringBuilder(input), combination -> {
            combinations.add(combination);
            totalPermutations.addAndGet(BigIntegerMath.factorial(combination.length()).longValueExact());
        });

        progress.start(totalPermutations.get());
        ctx.log("Solver:: generated combinations: " + combinations.size());
        doSolve(combinations, minCharacters, regex, progress, ctx);

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
