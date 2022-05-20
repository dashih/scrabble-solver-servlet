package org.dannyshih.scrabblesolver;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(
    name = "Scrabble Solver",
    description = "A Scrabble solving servlet",
    urlPatterns = { "/solve" })
public final class ScrabbleSolverServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        PrintWriter output = response.getWriter();
        output.println(request.getParameter("minCharsForMatches"));
        output.println(request.getParameter("input"));
        output.println(request.getParameter("regex"));
    }
}
