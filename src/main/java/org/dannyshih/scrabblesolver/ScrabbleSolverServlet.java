package org.dannyshih.scrabblesolver;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
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
    private static final int NUM_THREADS = 2;
    private static final String VERSION_RESOURCE = "/version.txt";
    private static final String PASSWORD_RESOURCE = "/password.txt";

    private final Solver m_solver;
    private final ExecutorService m_executor;
    private final Gson m_gson;
    private final ConcurrentMap<UUID, Operation> m_operations;
    private final String m_passwordHash;

    public ScrabbleSolverServlet() throws IOException {
        m_solver = new Solver();
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
                Pattern regex = Pattern.compile(StringUtils.isBlank(solveParams.regex) ? "[A-Z]+" : solveParams.regex);
                m_solver.solve(
                    solveParams.input,
                    solveParams.parallelMode,
                    solveParams.minChars,
                    regex,
                    m_operations.get(res.id).progress);
            } catch (CancellationException ce) {
                // Catch so status does not get set to Failed.
            } catch (Exception e) {
                m_operations.get(res.id).progress.finish(e);
            }
        });

        PrintWriter out = response.getWriter();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        out.print(m_gson.toJson(res));
        out.flush();
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

            PrintWriter out = response.getWriter();
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            out.print(m_gson.toJson(res));
            out.flush();
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
        String appVersion = null;
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

        PrintWriter out = response.getWriter();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        out.print(m_gson.toJson(v));
        out.flush();
    }

    private void getCurrentlyRunning(HttpServletRequest request, HttpServletResponse response) throws IOException {
        CurrentlyRunningResponse res = new CurrentlyRunningResponse();
        res.ids = ImmutableList.copyOf(m_operations.keySet());

        PrintWriter out = response.getWriter();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        out.print(m_gson.toJson(res));
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
        List<UUID> ids;
    }
}
