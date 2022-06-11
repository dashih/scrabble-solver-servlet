package org.dannyshih.scrabblesolver.solvers;

import com.google.common.math.BigIntegerMath;
import org.dannyshih.scrabblesolver.Progress;

import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

final class Utils {
    static long permute(
            StringBuilder sb,
            int idx,
            Trie dictionary,
            int minCharacters,
            Pattern regex,
            Progress progress,
            AtomicBoolean isCancellationRequested) {
        if (isCancellationRequested.get()) {
            throw new CancellationException();
        }

        if (idx == sb.length()) {
            final String s = sb.toString();
            if (dictionary.isWord(s) && s.length() >= minCharacters && regex.matcher(s).matches()) {
                progress.addSolution(s);
            }

            return 1L;
        }

        if (idx > 0 && !dictionary.beginsWord(sb.substring(0, idx))) {
            return BigIntegerMath.factorial(sb.length() - idx).longValueExact();
        }

        long numProcessed = 0L;
        for (int i = idx; i < sb.length(); i++) {
            Utils.swap(sb, idx, i);
            numProcessed += permute(sb, idx + 1, dictionary, minCharacters, regex, progress, isCancellationRequested);
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
