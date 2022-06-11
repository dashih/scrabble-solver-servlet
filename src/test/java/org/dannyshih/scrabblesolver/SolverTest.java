package org.dannyshih.scrabblesolver;

import com.google.common.collect.ImmutableList;
import org.dannyshih.scrabblesolver.solvers.ParallelSolver;
import org.dannyshih.scrabblesolver.solvers.SequentialSolver;
import org.dannyshih.scrabblesolver.solvers.Solver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public final class SolverTest {
    @Test
    public void solveSerial() throws IOException {
        solve(new SequentialSolver());
    }

    @Test
    public void solveParallel() throws IOException {
        solve(new ParallelSolver());
    }

    private void solve(Solver solver) {
        final int numExpected = 3;
        final List<String> expected = ImmutableList.of(
                "ABDICATE",
                "ACIDHEAD",
                "ABIDANCE");

        final String input = "*ABCD*EFGHI";
        final int minCharacters = 8;
        final Pattern regex = Pattern.compile("A.+");
        final Progress progress = new Progress();
        solver.solve(input, minCharacters, regex, progress, new AtomicBoolean(), null);

        Assertions.assertEquals(numExpected, progress.toSerializable().solutions.size());
        expected.forEach(expectedSolution ->
                Assertions.assertTrue(progress.toSerializable().solutions.contains(expectedSolution)));
    }
}
