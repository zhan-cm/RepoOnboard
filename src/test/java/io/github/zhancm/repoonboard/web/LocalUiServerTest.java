package io.github.zhancm.repoonboard.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
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
            assertTrue(style.body().contains("app-frame"));

            HttpResponse<String> response = get(server.uri().resolve("api/report"));
            assertEquals(200, response.statusCode());
            assertEquals("no-store", response.headers().firstValue("Cache-Control").orElseThrow());
            assertEquals(report, new AnalysisReportJson().deserialize(response.body()));
        }
    }

    @Test
    void exposesOnlyFixedReadOnlyRoutes() throws Exception {
        try (LocalUiServer server = LocalUiServer.start(report())) {
            assertEquals(404, get(server.uri().resolve("pom.xml")).statusCode());
            assertEquals(404, get(server.uri().resolve("api/report/extra")).statusCode());

            HttpRequest post = HttpRequest.newBuilder(server.uri().resolve("api/report"))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> response = client.send(post, HttpResponse.BodyHandlers.ofString());
            assertEquals(405, response.statusCode());
            assertEquals("GET, HEAD", response.headers().firstValue("Allow").orElseThrow());
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
