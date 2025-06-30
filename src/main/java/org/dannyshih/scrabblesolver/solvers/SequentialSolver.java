package org.dannyshih.scrabblesolver.solvers;

import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SequentialSolver extends Solver {
    private static final Logger S_LOGGER = LoggerFactory.getLogger(SequentialSolver.class);
    public SequentialSolver() throws IOException {
        super();
    }

    @Override
    protected void doSolve(List<StringBuilder> combinations, SolveOperationState state) {
        S_LOGGER.info("SequentialSolver :: solving...");
        combinations.forEach(combination -> state.progress.addNumProcessed(Utils.permute(
                combination, 0, state)));
    }
}
