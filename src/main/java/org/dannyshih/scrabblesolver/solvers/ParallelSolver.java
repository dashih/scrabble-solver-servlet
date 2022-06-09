package org.dannyshih.scrabblesolver.solvers;

import org.dannyshih.scrabblesolver.Progress;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
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
            AtomicBoolean isCancellationRequested,
            ServletContext ctx) {
        ctx.log("ParallelSolver :: parallelism - " + m_pool.getParallelism());
        final List<ForkJoinTask<Void>> tasks = new ArrayList<>();
        combinations.forEach(combination ->
                tasks.add(m_pool.submit(new Permuter(
                        combination,
                        0,
                        m_dictionary,
                        minCharacters,
                        regex,
                        progress,
                        isCancellationRequested))));

        // Wait on all tasks even if canceled.
        // Otherwise, we might tell the user cancellation was successful but still have running zombie tasks.
        AtomicBoolean isCanceled = new AtomicBoolean();
        tasks.forEach(task -> {
            try {
                task.join();
            } catch (CancellationException ignored) {
                isCanceled.set(true);
            }
        });

        if (isCanceled.get()) {
            // The base Solver requires a CE to detect cancellation.
            throw new CancellationException();
        }

        ctx.log("ParallelSolver:: steal count: " + m_pool.getStealCount());
    }
}
