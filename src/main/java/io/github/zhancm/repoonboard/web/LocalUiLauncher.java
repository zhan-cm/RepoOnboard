package io.github.zhancm.repoonboard.web;

import io.github.zhancm.repoonboard.core.model.AnalysisReport;
import java.io.IOException;
import java.io.PrintWriter;

/** Starts the local presentation lifecycle for one completed analysis report. */
@FunctionalInterface
public interface LocalUiLauncher {
    void launch(AnalysisReport report, boolean noOpen, PrintWriter out, PrintWriter err)
            throws IOException, InterruptedException;
}
