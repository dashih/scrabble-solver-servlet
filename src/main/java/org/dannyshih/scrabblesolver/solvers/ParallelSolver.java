package org.dannyshih.scrabblesolver.solvers;

import org.dannyshih.scrabblesolver.Progress;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

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
    protected void doSolve(
            List<StringBuilder> combinations,
            int minCharacters,
            Pattern regex,
            Progress progress,
            AtomicBoolean isCancellationRequested) {
        log("ParallelSolver :: parallelism - " + m_pool.getParallelism());

        m_pool.submit(new BatchPermuter(
                combinations,
                m_dictionary,
                minCharacters,
                regex,
                progress,
                isCancellationRequested)).join();

        log("ParallelSolver:: steal count: " + m_pool.getStealCount());
    }
}
