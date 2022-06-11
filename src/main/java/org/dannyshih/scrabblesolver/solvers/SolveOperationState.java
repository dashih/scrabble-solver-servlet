package org.dannyshih.scrabblesolver.solvers;

import org.dannyshih.scrabblesolver.Progress;

import javax.servlet.ServletContext;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

final class SolveOperationState {
    final Trie dictionary;
    final int minCharacters;
    final Pattern regex;
    final Progress progress;
    final AtomicBoolean isCancellationRequested;

    private final ServletContext servletContext;

    SolveOperationState(
            Trie dictionary,
            int minCharacters,
            Pattern regex,
            Progress progress,
            AtomicBoolean isCancellationRequested,
            ServletContext servletContext) {
        this.dictionary = dictionary;
        this.minCharacters = minCharacters;
        this.regex = regex;
        this.progress = progress;
        this.isCancellationRequested = isCancellationRequested;
        this.servletContext = servletContext;
    }

    void log(String msg) {
        servletContext.log(msg);
    }
}
