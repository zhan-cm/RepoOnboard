package io.github.zhancm.repoonboard.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.zhancm.repoonboard.core.model.AnalysisReport;
import io.github.zhancm.repoonboard.core.model.AnalysisStatus;
import io.github.zhancm.repoonboard.core.model.BuildSystem;
import io.github.zhancm.repoonboard.core.model.Evidence;
import io.github.zhancm.repoonboard.core.model.Project;
import io.github.zhancm.repoonboard.core.model.SourceLocation;
import io.github.zhancm.repoonboard.serialization.AnalysisReportJson;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class LocalUiServerTest {
    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void servesThePackagedShellAndImmutableReportOnLoopback() throws Exception {
        AnalysisReport report = report();
        try (LocalUiServer server = LocalUiServer.start(report)) {
            assertEquals("127.0.0.1", server.uri().getHost());
            assertTrue(server.port() > 0);

            HttpResponse<String> index = get(server.uri());
            assertEquals(200, index.statusCode());
            assertTrue(index.headers().firstValue("Content-Type").orElseThrow()
                    .startsWith("text/html"));
            assertTrue(index.body().contains("<div id=\"app\"></div>"));
            assertTrue(index.body().contains("/assets/app.js"));

            HttpResponse<String> script = get(server.uri().resolve("assets/app.js"));
            assertEquals(200, script.statusCode());
            assertTrue(script.body().contains("RepoOnboard"));

            HttpResponse<String> style = get(server.uri().resolve("assets/app.css"));
            assertEquals(200, style.statusCode());
            assertTrue(style.body().contains("app-shell"));

            HttpResponse<String> response = get(server.uri().resolve("api/report"));
            assertEquals(200, response.statusCode());
            assertEquals("no-store", response.headers().firstValue("Cache-Control").orElseThrow());
            assertEquals("nosniff", response.headers()
                    .firstValue("X-Content-Type-Options").orElseThrow());
            assertEquals("DENY", response.headers().firstValue("X-Frame-Options").orElseThrow());
            assertEquals("same-origin", response.headers()
                    .firstValue("Cross-Origin-Resource-Policy").orElseThrow());
            assertEquals("no-referrer", response.headers()
                    .firstValue("Referrer-Policy").orElseThrow());
            assertTrue(response.headers().firstValue("Content-Security-Policy").orElseThrow()
                    .contains("default-src 'none'"));
            assertTrue(response.headers().firstValue("Content-Security-Policy").orElseThrow()
                    .contains("connect-src 'self'"));
            assertFalse(response.headers().map().containsKey("access-control-allow-origin"));
            assertEquals(report, new AnalysisReportJson().deserialize(response.body()));
        }
    }

    @Test
    void exposesOnlyFixedReadOnlyRoutes() throws Exception {
        try (LocalUiServer server = LocalUiServer.start(report())) {
            assertEquals(404, get(server.uri().resolve("pom.xml")).statusCode());
            assertEquals(404, get(server.uri().resolve("api/report/extra")).statusCode());

            HttpRequest head = HttpRequest.newBuilder(server.uri().resolve("api/report"))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> headResponse = client.send(
                    head, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, headResponse.statusCode());
            assertEquals("", headResponse.body());

            HttpRequest post = HttpRequest.newBuilder(server.uri().resolve("api/report"))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> response = client.send(post, HttpResponse.BodyHandlers.ofString());
            assertEquals(405, response.statusCode());
            assertEquals("GET, HEAD", response.headers().firstValue("Allow").orElseThrow());
        }
    }

    @Test
    void rejectsTraversalNonCanonicalPathsAndForeignHostOrOrigin() throws Exception {
        try (LocalUiServer server = LocalUiServer.start(report())) {
            String validHost = "127.0.0.1:" + server.port();

            assertTrue(rawRequest(server.port(), "GET /../pom.xml HTTP/1.1\r\n"
                    + "Host: " + validHost + "\r\nConnection: close\r\n\r\n")
                    .startsWith("HTTP/1.1 404"));
            assertTrue(rawRequest(server.port(), "GET /assets/%2e%2e/%2e%2e/pom.xml HTTP/1.1\r\n"
                    + "Host: " + validHost + "\r\nConnection: close\r\n\r\n")
                    .startsWith("HTTP/1.1 404"));
            assertTrue(rawRequest(server.port(), "GET /api/report?file=pom.xml HTTP/1.1\r\n"
                    + "Host: " + validHost + "\r\nConnection: close\r\n\r\n")
                    .startsWith("HTTP/1.1 404"));
            assertTrue(rawRequest(server.port(), "GET / HTTP/1.1\r\n"
                    + "Host: attacker.example\r\nConnection: close\r\n\r\n")
                    .startsWith("HTTP/1.1 403"));
            assertTrue(rawRequest(server.port(), "GET /api/report HTTP/1.1\r\n"
                    + "Host: " + validHost + "\r\n"
                    + "Origin: https://attacker.example\r\nConnection: close\r\n\r\n")
                    .startsWith("HTTP/1.1 403"));

            HttpRequest sameOrigin = HttpRequest.newBuilder(server.uri().resolve("api/report"))
                    .header("Origin", "http://" + validHost)
                    .GET()
                    .build();
            assertEquals(200, client.send(sameOrigin, HttpResponse.BodyHandlers.ofString())
                    .statusCode());
        }
    }

    @Test
    void servesConcurrentReadRequestsWithoutChangingTheReport() throws Exception {
        try (LocalUiServer server = LocalUiServer.start(report())) {
            List<CompletableFuture<HttpResponse<String>>> requests = java.util.stream.IntStream
                    .range(0, 12)
                    .mapToObj(index -> client.sendAsync(
                            HttpRequest.newBuilder(server.uri().resolve("api/report")).GET().build(),
                            HttpResponse.BodyHandlers.ofString()))
                    .toList();

            for (CompletableFuture<HttpResponse<String>> request : requests) {
                HttpResponse<String> response = request.join();
                assertEquals(200, response.statusCode());
                assertEquals(report(), new AnalysisReportJson().deserialize(response.body()));
            }
        }
    }

    @Test
    void rejectsNonLoopbackBinding() throws Exception {
        InetSocketAddress wildcard = new InetSocketAddress(InetAddress.getByName("0.0.0.0"), 0);

        assertThrows(IllegalArgumentException.class, () -> LocalUiServer.start(report(), wildcard));
    }

    @Test
    void reportsOccupiedPortsAndReleasesItsPortOnClose() throws Exception {
        InetAddress loopback = InetAddress.getByName("127.0.0.1");
        try (ServerSocket occupied = new ServerSocket()) {
            occupied.bind(new InetSocketAddress(loopback, 0));
            int port = occupied.getLocalPort();
            assertThrows(IOException.class, () -> LocalUiServer.start(
                    report(), new InetSocketAddress(loopback, port)));
        }

        int releasedPort;
        LocalUiServer server = LocalUiServer.start(report());
        releasedPort = server.port();
        server.close();
        try (ServerSocket rebound = new ServerSocket()) {
            rebound.setReuseAddress(true);
            rebound.bind(new InetSocketAddress(loopback, releasedPort));
            assertEquals(releasedPort, rebound.getLocalPort());
        }
    }

    private HttpResponse<String> get(URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String rawRequest(int port, String request) throws IOException {
        try (Socket socket = new Socket(InetAddress.getByName("127.0.0.1"), port)) {
            socket.getOutputStream().write(request.getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().flush();
            return new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    static AnalysisReport report() {
        SourceLocation pom = SourceLocation.file("pom.xml");
        Evidence evidence = new Evidence("MAVEN_PROJECT", pom, List.of(), "maven.project.root");
        return new AnalysisReport(
                new Project("project:pom.xml", "demo-project", BuildSystem.MAVEN, List.of(evidence)),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                AnalysisStatus.SUCCESS,
                List.of());
    }
}
