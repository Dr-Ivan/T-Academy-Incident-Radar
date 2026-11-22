package academy.incidentradar.report;

import academy.incidentradar.model.AnalysisOptions;
import academy.incidentradar.model.AnalysisResult;
import academy.incidentradar.model.DateRange;
import academy.incidentradar.model.LogStatistics;
import academy.incidentradar.model.PeriodComparison;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Данные и периоды анализа для формирования отчёта. */
public record ReportData(
        List<String> files,
        DateRange period,
        LogStatistics statistics,
        @Nullable DateRange comparisonPeriod,
        @Nullable PeriodComparison comparison) {

    public ReportData {
        files = List.copyOf(files);
        Objects.requireNonNull(period, "period");
        Objects.requireNonNull(statistics, "statistics");

        if ((comparisonPeriod == null) != (comparison == null)) {
            throw new IllegalArgumentException("Период и результат сравнения должны присутствовать вместе");
        }
    }

    public static ReportData from(AnalysisOptions options, AnalysisResult result) {
        Objects.requireNonNull(options, "options");
        Objects.requireNonNull(result, "result");

        return new ReportData(
                result.readResult().files(),
                options.dateRange(),
                result.statistics(),
                options.comparisonDateRange(),
                result.comparison());
    }
}
