package academy.incidentradar.model;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Итог одного прохода по источникам логов. */
public record AnalysisResult(
        LogReadResult readResult,
        long malformedLines,
        LogStatistics statistics,
        @Nullable PeriodComparison comparison) {

    public AnalysisResult {
        Objects.requireNonNull(readResult, "readResult");
        Objects.requireNonNull(statistics, "statistics");

        if (malformedLines < 0 || malformedLines > readResult.linesRead()) {
            throw new IllegalArgumentException("Некорректное число битых строк");
        }
        var validLines = readResult.linesRead() - malformedLines;
        if (statistics.totalRequestsCount() > validLines
                || (comparison != null && comparison.referenceStatistics().totalRequestsCount() > validLines)) {
            throw new IllegalArgumentException("В периоде больше запросов, чем прочитано корректных строк");
        }
    }
}
