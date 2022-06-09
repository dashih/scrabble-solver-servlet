package org.dannyshih.scrabblesolver.solvers;

import org.dannyshih.scrabblesolver.Progress;

import javax.servlet.ServletContext;
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
            AtomicBoolean isCanceled,
            ServletContext ctx) {
        ctx.log("SequentialSolver :: solving...");
        combinations.forEach(combination ->
                permute(combination, 0, isCanceled, permutation -> {
                    if (m_dictionary.contains(permutation) &&
                            permutation.length() >= minCharacters &&
                            regex.matcher(permutation).matches()) {

                        progress.addSolution(permutation);
                    }

                    // Increment for every permutation to provide timely progress updates.
                    progress.addNumProcessed(1L);
                }));
    }
}
