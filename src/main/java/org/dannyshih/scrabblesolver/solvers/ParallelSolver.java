package org.dannyshih.scrabblesolver.solvers;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

/**
 * This class parallelizes permutation of all strings in a collection of variable-length strings.
 *
 * @author dshih
 */
public final class ParallelSolver extends Solver {
    private final ForkJoinPool m_pool;

    public ParallelSolver() throws IOException {
        super();
        m_pool = ForkJoinPool.commonPool();
    }

    @Override
    protected void doSolve(List<StringBuilder> combinations, SolveOperationState state) {
        state.logger.log("ParallelSolver :: parallelism - " + m_pool.getParallelism());

        // Randomize to produce evenly distributed batches
        Collections.shuffle(combinations);
        m_pool.submit(new BatchPermuter(combinations, state)).join();

        state.logger.log("ParallelSolver :: steal count: " + m_pool.getStealCount());
    }
}
