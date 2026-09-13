package io.github.zhancm.repoonboard.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class LocalUiApplicationTest {

    @Test
    void noOpenPrintsTheAddressWithoutCallingTheBrowser() throws Exception {
        AtomicBoolean browserCalled = new AtomicBoolean();
        LocalUiApplication application = new LocalUiApplication(
                uri -> browserCalled.set(true),
                server -> { });
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();

        application.launch(
                LocalUiServerTest.report(),
                true,
                new PrintWriter(out, true),
                new PrintWriter(err, true));

        assertFalse(browserCalled.get());
        assertTrue(out.toString().contains("Local UI: http://127.0.0.1:"));
        assertTrue(out.toString().contains("Automatic browser opening disabled (--no-open)."));
        assertTrue(out.toString().contains("Press Ctrl+C to stop RepoOnboard."));
        assertTrue(err.toString().isEmpty());
    }

    @Test
    void browserFailureKeepsTheSessionAvailableAndPrintsAManualAddress() throws Exception {
        LocalUiApplication application = new LocalUiApplication(
                uri -> {
                    throw new IOException("browser unavailable");
                },
                server -> { });
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();

        application.launch(
                LocalUiServerTest.report(),
                false,
                new PrintWriter(out, true),
                new PrintWriter(err, true));

        assertTrue(out.toString().contains("Local UI: http://127.0.0.1:"));
        assertTrue(err.toString().contains("WARNING [BROWSER_OPEN_FAILED] stage=LOCAL_UI"));
        assertTrue(err.toString().contains("Open the local UI manually: http://127.0.0.1:"));
        assertFalse(err.toString().contains("browser unavailable"));
    }

    @Test
    void interruptionClosesTheServerAndPreservesTheSignal() throws Exception {
        AtomicInteger port = new AtomicInteger();
        LocalUiApplication application = new LocalUiApplication(
                uri -> { },
                server -> {
                    port.set(server.port());
                    throw new InterruptedException("stop");
                });

        assertThrows(InterruptedException.class, () -> application.launch(
                LocalUiServerTest.report(),
                false,
                new PrintWriter(new StringWriter(), true),
                new PrintWriter(new StringWriter(), true)));

        try (ServerSocket rebound = new ServerSocket()) {
            rebound.setReuseAddress(true);
            rebound.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port.get()));
            assertTrue(rebound.isBound());
        }
    }
}
