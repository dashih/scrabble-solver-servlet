package org.dannyshih.scrabblesolver.solvers;

import org.dannyshih.scrabblesolver.Logger;
import org.dannyshih.scrabblesolver.Progress;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

final class SolveOperationState {
    final Trie dictionary;
    final int minCharacters;
    final Pattern regex;
    final Progress progress;
    final AtomicBoolean isCancellationRequested;
    final Logger logger;

    SolveOperationState(
            Trie dictionary,
            int minCharacters,
            Pattern regex,
            Progress progress,
            AtomicBoolean isCancellationRequested,
            Logger logger) {
        this.dictionary = dictionary;
        this.minCharacters = minCharacters;
        this.regex = regex;
        this.progress = progress;
        this.isCancellationRequested = isCancellationRequested;
        this.logger = logger;
    }
}
