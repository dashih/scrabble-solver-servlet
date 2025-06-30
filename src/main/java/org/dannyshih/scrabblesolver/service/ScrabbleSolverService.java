package org.dannyshih.scrabblesolver.service;

import com.google.common.base.Preconditions;
import org.dannyshih.scrabblesolver.Progress;
import org.dannyshih.scrabblesolver.config.ScrabbleSolverConfig;
import org.dannyshih.scrabblesolver.dto.SolveRequest;
import org.dannyshih.scrabblesolver.solvers.ParallelSolver;
import org.dannyshih.scrabblesolver.solvers.SequentialSolver;
import org.dannyshih.scrabblesolver.solvers.Solver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

@Service
public class ScrabbleSolverService {
    private static final Logger S_LOGGER = LoggerFactory.getLogger(ScrabbleSolverService.class);
    private static final String VERSION_RESOURCE = "/version.txt";
    private static final long REAP_PERIOD = 1; // minute
    private static final long DONE_KEEP_DAYS = 7; // days

    private final Solver sequentialSolver;
    private final Solver parallelSolver;
    private final ExecutorService executor;
    private final ConcurrentMap<UUID, Operation> operations;

    @Autowired
    public ScrabbleSolverService(ScrabbleSolverConfig config) throws IOException {
        S_LOGGER.info("{}", config);
        this.sequentialSolver = new SequentialSolver();
        this.parallelSolver = new ParallelSolver();
        this.operations = new ConcurrentHashMap<>();
        this.executor = Executors.newFixedThreadPool(config.getMaxConcurrentOperations());

        ScheduledExecutorService reaper = Executors.newSingleThreadScheduledExecutor();
        reaper.scheduleAtFixedRate(() -> operations.keySet().forEach(id -> {
            final Operation op = operations.get(id);
            if (op != null) {
                switch (op.progress.getRunStatus()) {
                    case Canceled:
                    case Failed:
                        operations.remove(id);
                        break;
                    case Done:
                        final long ageMs = new Date().getTime() - op.progress.getFinishedDate().getTime();
                        final long ageDays = TimeUnit.DAYS.convert(ageMs, TimeUnit.MILLISECONDS);
                        if (ageDays > DONE_KEEP_DAYS) {
                            operations.remove(id);
                        }
                        break;
                }
            }
        }), REAP_PERIOD, REAP_PERIOD, TimeUnit.MINUTES);
    }

    public UUID startSolve(SolveRequest solveRequest) {
        Preconditions.checkNotNull(solveRequest);

        final UUID operationId = UUID.randomUUID();
        final Operation op = new Operation();
        op.params = solveRequest;
        op.progress = new Progress();
        op.isCancellationRequested = new AtomicBoolean();
        operations.put(operationId, op);

        executor.submit(() -> {
            try {
                final Pattern regex = Pattern.compile(solveRequest.getRegex());
                (solveRequest.isParallelMode() ? parallelSolver : sequentialSolver).solve(
                        solveRequest.getInput(),
                        solveRequest.getMinChars(),
                        regex,
                        op.progress,
                        op.isCancellationRequested);

            } catch (Exception e) {
                S_LOGGER.error("Error during solve operation: {}", e.getMessage(), e);
                if (operations.get(operationId) != null) {
                    operations.get(operationId).progress.finish(e);
                }
            }
        });

        return operationId;
    }

    public Operation getOperation(UUID operationId) {
        return operations.get(operationId);
    }

    public boolean cancelOperation(UUID operationId) {
        Operation op = operations.get(operationId);
        if (op != null) {
            op.isCancellationRequested.set(true);
            return true;
        }
        return false;
    }

    public Map<UUID, String> getCurrentlyRunningOperations() {
        Map<UUID, String> result = new HashMap<>();
        operations.forEach((key, value) -> result.put(key, value.params.getInput()));
        return result;
    }

    public String getAppVersion() {
        InputStream versionResource = Preconditions.checkNotNull(getClass().getResourceAsStream(VERSION_RESOURCE));
        try (InputStreamReader reader = new InputStreamReader(versionResource)) {
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[1024];
            int bytesRead;
            while ((bytesRead = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, bytesRead);
            }
            return sb.toString().trim();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read version file.", e);
        }
    }

    public static final class Operation {
        public SolveRequest params;
        public Progress progress;
        public AtomicBoolean isCancellationRequested;
    }
} 