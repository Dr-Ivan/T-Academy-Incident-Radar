package academy.incidentradar.support;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/** Локальный HTTP-сервер с заданным ответом для тестов. */
public final class TestHttpServer implements AutoCloseable {

    private final HttpServer server;

    public TestHttpServer(int status, String body) throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.ofLiteral("127.0.0.1"), 0), 0);
        var bytes = body.getBytes(StandardCharsets.UTF_8);

        server.createContext("/access.log", exchange -> {
            try (exchange) {
                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
        });
        server.start();
    }

    public URI uri() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/access.log");
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
