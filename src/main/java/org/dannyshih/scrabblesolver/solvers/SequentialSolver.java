package org.dannyshih.scrabblesolver.solvers;

import org.dannyshih.scrabblesolver.Progress;

import java.io.IOException;
import java.util.List;
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
        combinations.forEach(combination -> progress.addNumProcessed(Utils.permute(
                combination,
                0,
                m_dictionary,
                minCharacters,
                regex,
                progress,
                isCancellationRequested)));
    }
}
