package io.github.zhancm.repoonboard.web;

import io.github.zhancm.repoonboard.core.model.AnalysisReport;
import java.awt.Desktop;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

/** Coordinates server, browser, and shutdown behavior for a local UI session. */
public final class LocalUiApplication implements LocalUiLauncher {
    private final BrowserLauncher browserLauncher;
    private final ShutdownAwaiter shutdownAwaiter;

    public LocalUiApplication() {
        this(LocalUiApplication::browse, LocalUiApplication::awaitShutdown);
    }

    LocalUiApplication(BrowserLauncher browserLauncher, ShutdownAwaiter shutdownAwaiter) {
        this.browserLauncher = Objects.requireNonNull(browserLauncher, "browserLauncher");
        this.shutdownAwaiter = Objects.requireNonNull(shutdownAwaiter, "shutdownAwaiter");
    }

    @Override
    public void launch(
            AnalysisReport report,
            boolean noOpen,
            PrintWriter out,
            PrintWriter err) throws IOException, InterruptedException {
        Objects.requireNonNull(out, "out");
        Objects.requireNonNull(err, "err");
        try (LocalUiServer server = LocalUiServer.start(report)) {
            URI uri = server.uri();
            out.printf("Local UI: %s%n", uri);
            if (noOpen) {
                out.println("Automatic browser opening disabled (--no-open).");
            } else {
                try {
                    browserLauncher.open(uri);
                } catch (IOException | RuntimeException exception) {
                    err.printf("Warning: could not open the browser automatically: %s%n",
                            message(exception));
                    err.printf("Open the local UI manually: %s%n", uri);
                }
            }
            out.println("Press Ctrl+C to stop RepoOnboard.");
            shutdownAwaiter.await(server);
        }
    }

    private static void browse(URI uri) throws IOException {
        if (!Desktop.isDesktopSupported()
                || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            throw new IOException("desktop browsing is not supported in this environment");
        }
        Desktop.getDesktop().browse(uri);
    }

    private static void awaitShutdown(LocalUiServer server) throws InterruptedException {
        CountDownLatch shutdown = new CountDownLatch(1);
        Thread hook = new Thread(() -> {
            server.close();
            shutdown.countDown();
        }, "repoonboard-shutdown");
        Runtime runtime = Runtime.getRuntime();
        runtime.addShutdownHook(hook);
        try {
            shutdown.await();
        } finally {
            try {
                runtime.removeShutdownHook(hook);
            } catch (IllegalStateException ignored) {
                // The JVM is already shutting down and owns hook execution.
            }
        }
    }

    private static String message(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    @FunctionalInterface
    interface BrowserLauncher {
        void open(URI uri) throws IOException;
    }

    @FunctionalInterface
    interface ShutdownAwaiter {
        void await(LocalUiServer server) throws InterruptedException;
    }
}
