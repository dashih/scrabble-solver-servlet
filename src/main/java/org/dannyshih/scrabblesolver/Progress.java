package org.dannyshih.scrabblesolver;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public final class Progress {
    private final ConcurrentMap<String, Boolean> m_solutions;
    private final Stopwatch m_stopwatch;

    private long m_total;
    private long m_numProcessed;
    private Exception m_exception;
    private RunStatus m_runStatus;
    private Date m_finished;

    public Progress() {
        m_solutions = new ConcurrentHashMap<>();
        m_total = 0L;
        m_numProcessed = 0L;
        m_stopwatch = Stopwatch.createUnstarted();
        m_runStatus = RunStatus.Starting;
    }

    public void start(long goal) {
        m_total = goal;
        m_stopwatch.start();
        m_runStatus = RunStatus.Running;
    }

    public RunStatus getRunStatus() {
        return m_runStatus;
    }

    public Exception getError() {
        return m_exception;
    }

    public Date getFinishedDate() {
        return m_finished;
    }

    public void addNumProcessed(long numProcessed) {
        m_numProcessed += numProcessed;
    }

    public void addSolution(String solution) {
        m_solutions.putIfAbsent(solution, true);
    }

    public void cancel() {
        m_runStatus = RunStatus.Canceled;
        m_finished = new Date();
    }

    public void finish(Exception exception) {
        m_runStatus = RunStatus.Failed;
        m_exception = exception;
        m_finished = new Date();
    }

    public void finish() {
        m_stopwatch.stop();
        m_runStatus = RunStatus.Done;
        m_finished = new Date();
    }

    public SerializableProgress toSerializable() {
        SerializableProgress sp = new SerializableProgress();
        sp.runStatus = m_runStatus;
        if (m_runStatus == RunStatus.Starting) {
            sp.solutions = ImmutableList.of();
            sp.percentDone = 0.0f;
        } else {
            sp.solutions = new ArrayList<>(m_solutions.keySet());
            sp.solutions.sort((word0, word1) -> word1.length() - word0.length());
            sp.total = m_total;
            sp.percentDone = ((float)m_numProcessed / m_total) * 100.0f;
            sp.elapsed = m_stopwatch.elapsed(TimeUnit.MILLISECONDS);
        }

        return sp;
    }

    public enum RunStatus {
        Starting,
        Running,
        Canceled,
        Failed,
        Done
    }

    static final class SerializableProgress {
        RunStatus runStatus;
        List<String> solutions;
        long total;
        float percentDone;
        long elapsed;
    }
}
