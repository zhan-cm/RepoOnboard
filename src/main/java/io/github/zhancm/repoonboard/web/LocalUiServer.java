package io.github.zhancm.repoonboard.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.github.zhancm.repoonboard.core.model.AnalysisReport;
import io.github.zhancm.repoonboard.serialization.AnalysisReportJson;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** A loopback-only server exposing one immutable report and packaged UI resources. */
final class LocalUiServer implements AutoCloseable {
    private static final String RESOURCE_ROOT = "/io/github/zhancm/repoonboard/web/ui/";

    private final HttpServer server;
    private final ExecutorService executor;
    private final URI uri;
    private final AtomicBoolean closed = new AtomicBoolean();

    private LocalUiServer(HttpServer server, ExecutorService executor) {
        this.server = server;
        this.executor = executor;
        this.uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/");
    }

    static LocalUiServer start(AnalysisReport report) throws IOException {
        InetAddress loopback = InetAddress.getByName("127.0.0.1");
        return start(report, new InetSocketAddress(loopback, 0));
    }

    static LocalUiServer start(AnalysisReport report, InetSocketAddress address) throws IOException {
        Objects.requireNonNull(report, "report");
        Objects.requireNonNull(address, "address");
        if (address.getAddress() == null || !address.getAddress().isLoopbackAddress()) {
            throw new IllegalArgumentException("Local UI address must be resolved loopback");
        }

        byte[] reportJson = new AnalysisReportJson().serialize(report)
                .getBytes(StandardCharsets.UTF_8);
        Map<String, Asset> assets = Map.of(
                "/", loadAsset("index.html", "text/html; charset=utf-8"),
                "/index.html", loadAsset("index.html", "text/html; charset=utf-8"),
                "/assets/app.js", loadAsset("assets/app.js", "text/javascript; charset=utf-8"),
                "/assets/app.css", loadAsset("assets/app.css", "text/css; charset=utf-8"));

        HttpServer httpServer = HttpServer.create(address, 0);
        ExecutorService executor = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "repoonboard-http");
            thread.setDaemon(false);
            return thread;
        });
        try {
            httpServer.setExecutor(executor);
            httpServer.createContext("/api/report", new ExactResponseHandler(
                    "/api/report", "application/json; charset=utf-8", reportJson));
            httpServer.createContext("/", exchange -> {
                Asset asset = assets.get(exchange.getRequestURI().getPath());
                if (asset == null) {
                    send(exchange, 404, "text/plain; charset=utf-8", bytes("Not found"));
                    return;
                }
                respondToRead(exchange, asset.contentType(), asset.body());
            });
            httpServer.start();
            return new LocalUiServer(httpServer, executor);
        } catch (RuntimeException exception) {
            httpServer.stop(0);
            executor.shutdownNow();
            throw exception;
        }
    }

    URI uri() {
        return uri;
    }

    int port() {
        return server.getAddress().getPort();
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            server.stop(0);
            executor.shutdownNow();
        }
    }

    private static Asset loadAsset(String name, String contentType) throws IOException {
        try (InputStream input = LocalUiServer.class.getResourceAsStream(RESOURCE_ROOT + name)) {
            if (input == null) {
                throw new IOException("Packaged UI resource is missing: " + name);
            }
            return new Asset(contentType, input.readAllBytes());
        }
    }

    private static void respondToRead(HttpExchange exchange, String contentType, byte[] body)
            throws IOException {
        String method = exchange.getRequestMethod();
        if (!method.equals("GET") && !method.equals("HEAD")) {
            exchange.getResponseHeaders().set("Allow", "GET, HEAD");
            send(exchange, 405, "text/plain; charset=utf-8", bytes("Method not allowed"));
            return;
        }
        send(exchange, 200, contentType, body);
    }

    private static void send(HttpExchange exchange, int status, String contentType, byte[] body)
            throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        if (exchange.getRequestMethod().equals("HEAD")) {
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
            return;
        }
        exchange.sendResponseHeaders(status, body.length);
        try (var output = exchange.getResponseBody()) {
            output.write(body);
        }
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private record Asset(String contentType, byte[] body) {
        private Asset {
            Objects.requireNonNull(contentType, "contentType");
            body = Objects.requireNonNull(body, "body").clone();
        }

        @Override
        public byte[] body() {
            return body.clone();
        }
    }

    private record ExactResponseHandler(String path, String contentType, byte[] body)
            implements HttpHandler {
        private ExactResponseHandler {
            Objects.requireNonNull(path, "path");
            Objects.requireNonNull(contentType, "contentType");
            body = Objects.requireNonNull(body, "body").clone();
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestURI().getPath().equals(path)) {
                send(exchange, 404, "text/plain; charset=utf-8", bytes("Not found"));
                return;
            }
            respondToRead(exchange, contentType, body);
        }
    }
}
