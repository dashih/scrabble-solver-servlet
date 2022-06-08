package org.dannyshih.scrabblesolver.solvers;

import org.dannyshih.scrabblesolver.Progress;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.regex.Pattern;

/**
 * This class parallelizes permutation of all strings in a collection of variable-length strings.
 * Two strategies are used for small and large strings. There is an assumption that there will be many more small
 * strings than large ones, due to the domain.
 *
 * Small strings are processed using BulkPermutor.
 * Large strings are processed using Permuter.
 *
 * @author dshih
 */
public final class ParallelSolver extends Solver {
    private static final int SMALL_COMBINATION_THRESHOLD = 8;

    private final ForkJoinPool m_pool;

    public ParallelSolver() throws IOException {
        super();
        m_pool = ForkJoinPool.commonPool();
    }

    @Override
    protected void doSolve(
            List<StringBuilder> combinations, int minCharacters, Pattern regex, Progress progress, ServletContext ctx) {
        ctx.log("ParallelSolver :: parallelism - " + m_pool.getParallelism());

        // Separate the list of combinations into smalls (8-characters or less) and larges.
        final List<StringBuilder> smalls = new ArrayList<>();
        final List<StringBuilder> larges = new ArrayList<>();
        combinations.forEach(combination -> {
            if (combination.length() <= SMALL_COMBINATION_THRESHOLD) {
                smalls.add(combination);
            } else {
                larges.add(combination);
            }
        });

        // Submit a single BulkPermutor task to process the smalls.
        final List<ForkJoinTask<Long>> tasks = new ArrayList<>();
        tasks.add(m_pool.submit(new BulkPermuter(smalls, m_dictionary, minCharacters, regex, progress)));
        ctx.log(String.format("ParallelSolver :: submitted a BulkPermutor to process %d small combinations", smalls.size()));

        // Submit Permutor tasks for the larges.
        larges.forEach(combination ->
                tasks.add(m_pool.submit(new Permuter(combination, 0, m_dictionary, minCharacters, regex, progress))));
        ctx.log(String.format("ParallelSolver :: submitted %d Permuters to process %d large combinations",
                tasks.size(), larges.size()));

        // Shuffle the tasks so there's a better chance of responsive status reporting.
        Collections.shuffle(tasks);
        tasks.forEach(task -> progress.addNumProcessed(task.join()));
        ctx.log("ParallelSolver:: steal count: " + m_pool.getStealCount());
    }
}
