package org.dannyshih.scrabblesolver.solvers;

import java.io.IOException;
import java.util.List;

public final class SequentialSolver extends Solver {
    public SequentialSolver() throws IOException {
        super();
    }

    @Override
    protected void doSolve(List<StringBuilder> combinations, SolveOperationState state) {
        state.log("SequentialSolver :: solving...");
        combinations.forEach(combination -> state.progress.addNumProcessed(Utils.permute(
                combination, 0, state)));
    }
}
