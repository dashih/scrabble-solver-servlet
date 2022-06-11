package org.dannyshih.scrabblesolver.solvers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
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

    protected final Trie m_dictionary;
    private ServletContext m_servletContext;

    public Solver() throws IOException {
        m_dictionary = new Trie();
        populateDictionary();
    }

    protected abstract void doSolve(
            List<StringBuilder> combinations,
            int minCharacters,
            Pattern regex,
            Progress progress,
            AtomicBoolean isCancellationRequested);

    protected void log(String msg) {
        if (m_servletContext != null) {
            m_servletContext.log(msg);
        }
    }

    public void solve(
            String input,
            int minCharacters,
            Pattern regex,
            Progress progress,
            AtomicBoolean isCancellationRequested,
            ServletContext servletContext) {
        Preconditions.checkArgument(StringUtils.isNotBlank(input));
        m_servletContext = servletContext;

        final List<StringBuilder> combinations = new ArrayList<>();
        final AtomicLong totalPermutations = new AtomicLong();
        generateCombinationsWithBlanks(new StringBuilder(input), combination -> {
            combinations.add(combination);
            totalPermutations.addAndGet(BigIntegerMath.factorial(combination.length()).longValueExact());
        });

        progress.start(totalPermutations.get());
        log("Solver :: generated combinations: " + combinations.size());

        try {
            doSolve(combinations, minCharacters, regex, progress, isCancellationRequested);
            progress.finish();
        } catch (CancellationException ce) {
            log("Solver :: canceled!");
            progress.cancel();
        } finally {
            m_servletContext = null;
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
