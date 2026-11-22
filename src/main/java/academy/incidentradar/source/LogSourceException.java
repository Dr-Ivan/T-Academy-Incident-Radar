package academy.incidentradar.source;

/** Ошибка выбора или чтения источника логов. */
public final class LogSourceException extends RuntimeException {

    public LogSourceException(String message) {
        super(message);
    }

    public LogSourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
