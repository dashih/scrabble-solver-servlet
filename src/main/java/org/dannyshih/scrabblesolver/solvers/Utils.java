package org.dannyshih.scrabblesolver.solvers;

import com.google.common.math.BigIntegerMath;

import java.util.concurrent.CancellationException;

final class Utils {
    static long permute(StringBuilder sb, int idx, SolveOperationState state) {
        if (state.isCancellationRequested.get()) {
            throw new CancellationException();
        }

        if (idx == sb.length()) {
            final String s = sb.toString();
            if (state.dictionary.isWord(s) && s.length() >= state.minCharacters && state.regex.matcher(s).matches()) {
                state.progress.addSolution(s);
            }

            return 1L;
        }

        if (idx > 0 && !state.dictionary.beginsWord(sb.substring(0, idx))) {
            return BigIntegerMath.factorial(sb.length() - idx).longValueExact();
        }

        long numProcessed = 0L;
        for (int i = idx; i < sb.length(); i++) {
            Utils.swap(sb, idx, i);
            numProcessed += permute(sb, idx + 1, state);
            Utils.swap(sb, idx, i);
        }

        return numProcessed;
    }

    static void swap(StringBuilder sb, int idx0, int idx1) {
        char tmp = sb.charAt(idx0);
        sb.setCharAt(idx0, sb.charAt(idx1));
        sb.setCharAt(idx1, tmp);
    }
}
