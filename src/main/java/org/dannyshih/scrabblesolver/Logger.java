package org.dannyshih.scrabblesolver;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import javax.servlet.ServletContext;

public final class Logger {
    private static final String STDOUT_LOGS_PROP = "SCRABBLE_SOLVER_LOGS_STDOUT";
    private final ServletContext m_servletContext;

    Logger(ServletContext servletContext) {
        m_servletContext = Preconditions.checkNotNull(servletContext);
    }

    @VisibleForTesting
    Logger() {
        m_servletContext = null;
    }

    public void log(String msg) {
        if (Boolean.parseBoolean(System.getenv(STDOUT_LOGS_PROP))) {
            System.out.println(msg);
        } else {
            if (m_servletContext != null) {
                m_servletContext.log(msg);
            }
        }
    }
}
