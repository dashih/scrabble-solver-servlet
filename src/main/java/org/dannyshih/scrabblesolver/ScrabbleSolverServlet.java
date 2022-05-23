package org.dannyshih.scrabblesolver;

import com.google.common.base.Preconditions;
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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@WebServlet(
    name = "Scrabble Solver",
    description = "A Scrabble solving servlet",
    urlPatterns = { "/api/solve", "/api/getProgress", "/api/getVersions" })
public final class ScrabbleSolverServlet extends HttpServlet {
    private static final int NUM_THREADS = 2;
    private static final String VERSION_RESOURCE = "/version.txt";
    private static final String PASSWORD_RESOURCE = "/password.txt";

    private final ExecutorService m_executor;
    private final Gson m_gson;
    private final ConcurrentMap<UUID, Progress> m_operations;
    private final String m_passwordHash;

    public ScrabbleSolverServlet() {
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
            case "/api/solve":
                solve(request, response);
                break;
            case "/api/getProgress":
                getProgress(request, response);
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
        m_operations.put(res.id, new Progress());
        m_executor.submit(() -> {
            try {
                Pattern regex = Pattern.compile(StringUtils.isBlank(solveParams.regex) ? "[A-Z]+" : solveParams.regex);
                new Solver(solveParams.parallelMode, solveParams.minChars, regex).solve(solveParams.input, m_operations.get(res.id));
            } catch (Exception e) {
                m_operations.get(res.id).finish(e);
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
        Progress progress = m_operations.get(id);
        if (progress != null) {
            if (progress.getRunStatus() == Progress.RunStatus.Failed) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, progress.getError().getMessage());
                m_operations.remove(id);
                return;
            }

            if (progress.getRunStatus() == Progress.RunStatus.Done) {
                m_operations.remove(id);
            }

            PrintWriter out = response.getWriter();
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            out.print(progress.toJson());
            out.flush();
        } else {
            log("didn't find running operation");
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

    private static final class VersionsResponse {
        String app;
        String tomcat;
        String servletApi;
        String java;
    }
}
