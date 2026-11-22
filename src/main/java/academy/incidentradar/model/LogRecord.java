package academy.incidentradar.model;

import java.time.OffsetDateTime;
import java.util.Objects;

/** Разобранная запись NGINX-лога. */
public record LogRecord(
        String remoteAddress,
        String remoteUser,
        OffsetDateTime timestamp,
        String method,
        String resource,
        String protocol,
        int status,
        long bodyBytesSent,
        String referer,
        String userAgent) {

    private static final int MIN_STATUS = 100;
    private static final int MAX_STATUS = 599;

    public LogRecord {
        requireNonBlank(remoteAddress, "remoteAddress");
        requireNonBlank(remoteUser, "remoteUser");
        Objects.requireNonNull(timestamp, "timestamp");
        requireNonBlank(method, "method");
        requireNonBlank(resource, "resource");
        requireNonBlank(protocol, "protocol");
        Objects.requireNonNull(referer, "referer");
        Objects.requireNonNull(userAgent, "userAgent");

        if (status < MIN_STATUS || status > MAX_STATUS) {
            throw new IllegalArgumentException("HTTP-код должен находиться в диапазоне 100..599: " + status);
        }

        if (bodyBytesSent < 0) {
            throw new IllegalArgumentException("Размер ответа не может быть отрицательным: " + bodyBytesSent);
        }
    }

    private static void requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " не должен быть пустым");
        }
    }
}
