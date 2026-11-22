package academy.incidentradar.report;

/** Формирование текста отчёта. */
@FunctionalInterface
public interface ReportRenderer {

    String render(ReportData report);
}
