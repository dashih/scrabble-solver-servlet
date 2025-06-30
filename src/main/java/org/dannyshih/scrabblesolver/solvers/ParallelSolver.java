package org.dannyshih.scrabblesolver.solvers;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class parallelizes permutation of all strings in a collection of variable-length strings.
 *
 * @author dshih
 */
public final class ParallelSolver extends Solver {
    private static final Logger S_LOGGER = LoggerFactory.getLogger(ParallelSolver.class);
    private final ForkJoinPool m_pool;

    public ParallelSolver() throws IOException {
        super();
        m_pool = ForkJoinPool.commonPool();
    }

    @Override
    protected void doSolve(List<StringBuilder> combinations, SolveOperationState state) {
        S_LOGGER.info("ParallelSolver :: parallelism - {}", m_pool.getParallelism());

        // Randomize to produce evenly distributed batches
        Collections.shuffle(combinations);
        m_pool.submit(new BatchPermuter(combinations, state)).join();

        S_LOGGER.info("ParallelSolver :: steal count: {}", m_pool.getStealCount());
    }
}
