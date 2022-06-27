package org.dannyshih.scrabblesolver.solvers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.google.common.math.BigIntegerMath;
import org.dannyshih.scrabblesolver.Logger;
import org.dannyshih.scrabblesolver.Progress;

public abstract class Solver {
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    protected final Trie m_dictionary;

    public Solver() throws IOException {
        m_dictionary = new Trie();
        populateDictionary();
    }

    protected abstract void doSolve(List<StringBuilder> combinations, SolveOperationState state);

    public void solve(
            String input,
            int minCharacters,
            Pattern regex,
            Progress progress,
            AtomicBoolean isCancellationRequested,
            Logger logger) {
        Preconditions.checkArgument(StringUtils.isNotBlank(input));
        logger.log(String.format("Solver :: solving %s (%d chars, %d blanks), %d minimum, matching %s",
                input, input.length(), input.chars().filter(c -> c == '*').count(), minCharacters, regex.toString()));

        final List<StringBuilder> combinations = new ArrayList<>();
        final AtomicLong totalPermutations = new AtomicLong();
        generateCombinationsWithBlanks(new StringBuilder(input), combination -> {
            combinations.add(combination);
            totalPermutations.addAndGet(BigIntegerMath.factorial(combination.length()).longValueExact());
        });

        progress.start(totalPermutations.get());
        logger.log("Solver :: generated combinations: " + combinations.size());

        try {
            final SolveOperationState opState = new SolveOperationState(
                    m_dictionary, minCharacters, regex, progress, isCancellationRequested, logger, new ConcurrentLinkedQueue<>());

            doSolve(combinations, opState);

            opState.opTimes.stream().mapToDouble(d -> d).average().ifPresent(
                    d -> logger.log("Solver :: avg op - " + d + " ms"));
            opState.opTimes.stream().mapToDouble(d -> d).min().ifPresent(
                    d -> logger.log("Solver :: min op - " + d + " ms"));
            opState.opTimes.stream().mapToDouble(d -> d).max().ifPresent(
                    d -> logger.log("Solver :: max op - " + d + " ms"));
            progress.finish();
        } catch (CancellationException ce) {
            logger.log("Solver :: canceled!");
            progress.cancel();
        }
    }

    private void populateDictionary() throws IOException {
        InputStream in = Preconditions.checkNotNull(getClass().getResourceAsStream("/dictionary.txt"));
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            br.lines().forEach(m_dictionary::addWord);
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
}
