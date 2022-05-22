package org.dannyshih.scrabblesolver;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

final class Progress {
    private final ConcurrentMap<String, Boolean> m_solutions;
    private final AtomicLong m_total;
    private final AtomicLong m_processed;
    private final Gson m_gson;
    private final Stopwatch m_stopwatch;

    private Exception m_exception;

    private RunStatus m_runStatus;

    Progress() {
        m_solutions = new ConcurrentHashMap<>();
        m_total = new AtomicLong();
        m_processed = new AtomicLong();
        m_gson = new Gson();
        m_stopwatch = Stopwatch.createUnstarted();
        m_runStatus = RunStatus.Starting;
    }

    void start(long goal) {
        m_total.set(goal);
        m_stopwatch.start();
        m_runStatus = RunStatus.Running;
    }

    RunStatus getRunStatus() {
        return m_runStatus;
    }

    Exception getError() {
        return m_exception;
    }

    void increment() {
        m_processed.incrementAndGet();
    }

    void addSolution(String solution) {
        m_solutions.putIfAbsent(solution, true);
    }

    void finish(Exception exception) {
        m_runStatus = RunStatus.Failed;
        m_exception = exception;
    }

    void finish() {
        m_stopwatch.stop();
        m_runStatus = RunStatus.Done;
    }

    String toJson() {
        SerializableProgress sp = new SerializableProgress();
        sp.runStatus = m_runStatus;
        if (m_runStatus == RunStatus.Starting) {
            sp.solutions = ImmutableList.of();
            sp.percentDone = 0.0f;
        } else {
            sp.solutions = ImmutableList.copyOf(m_solutions.keySet());
            sp.total = m_total.get();
            sp.percentDone = ((float)m_processed.get() / m_total.get()) * 100.0f;
            sp.elapsed = m_stopwatch.elapsed(TimeUnit.MILLISECONDS);
        }

        return m_gson.toJson(sp);
    }

    enum RunStatus {
        Starting,
        Running,
        Failed,
        Done
    }

    private static final class SerializableProgress {
        RunStatus runStatus;
        List<String> solutions;
        long total;
        float percentDone;
        long elapsed;
    }
}
