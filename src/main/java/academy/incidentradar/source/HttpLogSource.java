package academy.incidentradar.source;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

/** Построчное чтение лога по HTTP или HTTPS. */
public final class HttpLogSource implements LogSource {

    private static final int SUCCESS_STATUS_MIN = 200;
    private static final int SUCCESS_STATUS_LIMIT = 300;
    private static final int MAX_PORT = 65_535;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(30);

    private final HttpRequest request;

    public HttpLogSource(URI uri) {
        Objects.requireNonNull(uri, "uri");

        var path = uri.getPath();
        if (path == null || !LogFileNames.hasSupportedExtension(path)) {
            throw new LogSourceException("URL должен указывать на файл .log или .txt: " + uri);
        }

        if (uri.getPort() > MAX_PORT) {
            throw new LogSourceException("Недопустимый порт в URL: " + uri);
        }
        request = createRequest(uri);
    }

    @Override
    public String name() {
        return request.uri().toString();
    }

    @Override
    public void forEachLine(Consumer<String> action) {
        Objects.requireNonNull(action, "action");

        try (var client = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()) {
            var response = client.send(request, HttpResponse.BodyHandlers.ofLines());

            try (var lines = response.body()) {
                var status = response.statusCode();
                if (status < SUCCESS_STATUS_MIN || status >= SUCCESS_STATUS_LIMIT) {
                    throw new LogSourceException("HTTP " + status + " при чтении " + name());
                }
                consumeLines(lines, action);
            }
        } catch (IOException exception) {
            throw new LogSourceException(
                    "Не удалось прочитать HTTP-лог '" + name() + "': " + exception.getMessage(), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Чтение HTTP-лога прервано: " + name(), exception);
        }
    }

    private static HttpRequest createRequest(URI uri) {
        HttpRequest.Builder builder;

        try {
            builder = HttpRequest.newBuilder(uri);
        } catch (IllegalArgumentException exception) {
            throw new LogSourceException("Некорректный HTTP/HTTPS URL: " + uri, exception);
        }
        return builder.timeout(RESPONSE_TIMEOUT).GET().build();
    }

    private static void consumeLines(Stream<String> lines, Consumer<String> action) throws IOException {
        var iterator = lines.iterator();

        while (true) {
            String line;

            try {
                if (!iterator.hasNext()) {
                    return;
                }
                line = iterator.next();
            } catch (UncheckedIOException exception) {
                throw exception.getCause();
            }
            action.accept(line);
        }
    }
}
