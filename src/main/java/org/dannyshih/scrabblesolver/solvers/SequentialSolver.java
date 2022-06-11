package org.dannyshih.scrabblesolver.solvers;

import com.google.common.math.BigIntegerMath;
import org.dannyshih.scrabblesolver.Progress;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public final class SequentialSolver extends Solver {
    public SequentialSolver() throws IOException {
        super();
    }

    @Override
    protected void doSolve(
            List<StringBuilder> combinations,
            int minCharacters,
            Pattern regex,
            Progress progress,
            AtomicBoolean isCancellationRequested) {
        log("SequentialSolver :: solving...");
        combinations.forEach(combination ->
                permute(combination, 0, isCancellationRequested, minCharacters, regex, progress));
    }

    private void permute(
            StringBuilder sb,
            int idx,
            AtomicBoolean isCancellationRequested,
            int minCharacters,
            Pattern regex,
            Progress progress) {
        if (isCancellationRequested.get()) {
            throw new CancellationException();
        }

        if (idx == sb.length()) {
            final String s = sb.toString();
            if (m_dictionary.isWord(s) && s.length() >= minCharacters && regex.matcher(s).matches()) {
                progress.addSolution(s);
            }

            // Increment for every permutation to provide timely progress updates.
            progress.addNumProcessed(1L);
        }

        // The god-like optimization that took me 15 years to realize =(
        if (idx > 0 && !m_dictionary.beginsWord(sb.substring(0, idx))) {
            progress.addNumProcessed(BigIntegerMath.factorial(sb.length() - idx).longValueExact());
            return;
        }

        for (int i = idx; i < sb.length(); i++) {
            Utils.swap(sb, idx, i);
            permute(sb, idx + 1, isCancellationRequested, minCharacters, regex, progress);
            Utils.swap(sb, idx, i);
        }
    }
}
