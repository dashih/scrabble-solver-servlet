package org.dannyshih.scrabblesolver;

import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
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
    urlPatterns = { "/api/solve", "/api/getProgress" })
public final class ScrabbleSolverServlet extends HttpServlet {
    private static final int NUM_THREADS = 2;

    private final ExecutorService m_executor;
    private final Gson m_gson;
    private final ConcurrentMap<UUID, Progress> m_operations;

    public ScrabbleSolverServlet() {
        m_executor = Executors.newFixedThreadPool(NUM_THREADS);
        m_gson = new Gson();
        m_operations = new ConcurrentHashMap<>();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestBody = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        SolveParams solveParams = m_gson.fromJson(requestBody, SolveParams.class);
        Pattern regex = Pattern.compile(StringUtils.isBlank(solveParams.regex) ? "[A-Z]+" : solveParams.regex);

        final SolveResponse res = new SolveResponse(UUID.randomUUID());
        m_operations.put(res.id, new Progress());
        m_executor.submit(() -> {
            try {
                new Solver(solveParams.parallelMode, solveParams.minChars, regex).solve(solveParams.input, m_operations.get(res.id));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        PrintWriter out = response.getWriter();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        out.print(m_gson.toJson(res));
        out.flush();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        UUID id = UUID.fromString(request.getParameter("id"));
        Progress progress = m_operations.get(id);
        if (progress != null) {
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

    private static final class SolveParams {
        String password;
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
}
