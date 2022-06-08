package org.dannyshih.scrabblesolver;

import com.google.common.base.Preconditions;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.dannyshih.scrabblesolver.solvers.ParallelSolver;
import org.dannyshih.scrabblesolver.solvers.SequentialSolver;
import org.dannyshih.scrabblesolver.solvers.Solver;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@WebServlet(
    name = "Scrabble Solver",
    description = "A Scrabble solving servlet",
    urlPatterns = {
        "/api/solve",
        "/api/getProgress",
        "/api/getVersions",
        "/api/getCurrentlyRunning",
        "/api/cancel"
    })
public final class ScrabbleSolverServlet extends HttpServlet {
    private static final int NUM_THREADS = 4;
    private static final String VERSION_RESOURCE = "/version.txt";
    private static final String PASSWORD_RESOURCE = "/password.txt";
    private static final long REAP_PERIOD = 1; // minute
    private static final long DONE_KEEP_DAYS = 7; // days

    private final Solver m_sequentialSolver;
    private final Solver m_parallelSolver;
    private final ExecutorService m_executor;
    private final Gson m_gson;
    private final ConcurrentMap<UUID, Operation> m_operations;
    private final String m_passwordHash;

    public ScrabbleSolverServlet() throws IOException {
        m_sequentialSolver = new SequentialSolver();
        m_parallelSolver = new ParallelSolver();
        m_executor = Executors.newFixedThreadPool(NUM_THREADS);
        m_gson = new Gson();
        m_operations = new ConcurrentHashMap<>();
        InputStream passwordResource = Preconditions.checkNotNull(getClass().getResourceAsStream(PASSWORD_RESOURCE));
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(passwordResource))) {
            String password = reader.readLine();
            m_passwordHash = Hashing.sha256().hashString(password, StandardCharsets.UTF_8).toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read credentials file.", e);
        }

        ScheduledExecutorService reaper = Executors.newSingleThreadScheduledExecutor();
        reaper.scheduleAtFixedRate(() -> m_operations.keySet().forEach(id -> {
            final Operation op = m_operations.get(id);
            switch (op.progress.getRunStatus()) {
                case Canceled:
                case Failed:
                    m_operations.remove(id);
                    break;
                case Done:
                    final long ageMs = new Date().getTime() - op.progress.getFinishedDate().getTime();
                    final long ageDays = TimeUnit.DAYS.convert(ageMs, TimeUnit.MILLISECONDS);
                    if (ageDays > DONE_KEEP_DAYS) {
                        m_operations.remove(id);
                    }
                    break;
            }
        }), REAP_PERIOD, REAP_PERIOD, TimeUnit.MINUTES);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        switch (request.getServletPath()) {
            case "/api/getVersions":
                getVersions(request, response);
                break;
            case "/api/getCurrentlyRunning":
                getCurrentlyRunning(request, response);
                break;
            case "/api/solve":
                solve(request, response);
                break;
            case "/api/getProgress":
                getProgress(request, response);
                break;
            case "/api/cancel":
                cancel(request, response);
                break;
            default:
                throw new ServletException("Unexpected path.");
        }
    }

    private void solve(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestBody = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        SolveParams solveParams = m_gson.fromJson(requestBody, SolveParams.class);
        Preconditions.checkNotNull(solveParams);
        if (!m_passwordHash.equals(solveParams.passwordHash)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Wrong password.");
            return;
        }

        final SolveResponse res = new SolveResponse(UUID.randomUUID());
        final Operation op = new Operation();
        op.params = solveParams;
        op.progress = new Progress();
        m_operations.put(res.id, op);
        m_executor.submit(() -> {
            try {
                final Pattern regex = Pattern.compile(StringUtils.isBlank(solveParams.regex) ? "[A-Z]+" : solveParams.regex);
                (solveParams.parallelMode ? m_parallelSolver : m_sequentialSolver).solve(
                        solveParams.input,
                        solveParams.minChars,
                        regex,
                        m_operations.get(res.id).progress,
                        getServletContext());

            } catch (CancellationException ce) {
                // Catch so status does not get set to Failed.
            } catch (Exception e) {
                log(e.toString());
                m_operations.get(res.id).progress.finish(e);
            }
        });

        respond(response, res);
    }

    private void getProgress(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final String requestBody = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        final SolveResponse solveResponse = m_gson.fromJson(requestBody, SolveResponse.class);
        Preconditions.checkNotNull(solveResponse);
        UUID id = solveResponse.id;
        Operation op = m_operations.get(id);
        if (op != null) {
            Progress progress = op.progress;
            if (progress.getRunStatus() == Progress.RunStatus.Canceled) {
                response.sendError(HttpServletResponse.SC_GONE, "Operation canceled");
                return;
            }

            if (progress.getRunStatus() == Progress.RunStatus.Failed) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, progress.getError().getMessage());
                return;
            }

            GetResponse res = new GetResponse();
            res.params = op.params;
            res.progress = progress.toSerializable();

            respond(response, res);
        } else {
            log("didn't find running operation");
        }
    }

    private void cancel(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final String requestBody = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        final SolveResponse solveResponse = m_gson.fromJson(requestBody, SolveResponse.class);
        Preconditions.checkNotNull(solveResponse);
        Operation op = m_operations.get(solveResponse.id);
        if (op == null) {
            log("didn't find operation to cancel");
        } else {
            op.progress.cancel();
        }
    }

    private void getVersions(HttpServletRequest request, HttpServletResponse response) throws IOException {
        VersionsResponse v = new VersionsResponse();

        InputStream versionResource = Preconditions.checkNotNull(getClass().getResourceAsStream(VERSION_RESOURCE));
        String appVersion;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(versionResource))) {
            appVersion = reader.readLine();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read version file.", e);
        }

        v.app = appVersion;
        v.tomcat = request.getServletContext().getServerInfo();
        v.servletApi = String.format("%s.%s",
            request.getServletContext().getEffectiveMajorVersion(),
            request.getServletContext().getEffectiveMinorVersion());
        v.java = System.getProperty("java.version");

        respond(response, v);
    }

    private void getCurrentlyRunning(HttpServletRequest request, HttpServletResponse response) throws IOException {
        CurrentlyRunningResponse res = new CurrentlyRunningResponse();
        res.operations = new HashMap<>();
        m_operations.forEach((key, value) -> res.operations.put(key, value.params.input));
        respond(response, res);
    }

    private void respond(HttpServletResponse response, Object obj) throws IOException {
        PrintWriter out = response.getWriter();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        out.print(m_gson.toJson(obj));
        out.flush();
    }

    private static final class Operation {
        SolveParams params;
        Progress progress;
    }

    private static final class SolveParams {
        String passwordHash;
        boolean parallelMode;
        String input;
        String regex;
        int minChars;
    }

    private static final class SolveResponse {
        UUID id;

        SolveResponse(UUID id) {
            this.id = id;
        }
    }

    private static final class GetResponse {
        SolveParams params;
        Progress.SerializableProgress progress;
    }

    private static final class VersionsResponse {
        String app;
        String tomcat;
        String servletApi;
        String java;
    }

    private static final class CurrentlyRunningResponse {
        Map<UUID, String> operations;
    }
}
