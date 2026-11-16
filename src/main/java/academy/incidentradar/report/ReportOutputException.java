package academy.incidentradar.report;

/** Ошибка назначения или записи отчёта. */
public final class ReportOutputException extends RuntimeException {

    public ReportOutputException(String message) {
        super(message);
    }

    public ReportOutputException(String message, Throwable cause) {
        super(message, cause);
    }
}
