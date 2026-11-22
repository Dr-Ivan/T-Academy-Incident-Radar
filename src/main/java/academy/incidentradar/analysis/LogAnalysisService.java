package academy.incidentradar.analysis;

import academy.incidentradar.model.AnalysisOptions;
import academy.incidentradar.model.AnalysisResult;
import academy.incidentradar.model.DateRange;
import academy.incidentradar.model.LogRecord;
import academy.incidentradar.model.PeriodComparison;
import academy.incidentradar.parser.NginxLogParser;
import academy.incidentradar.source.LogReader;
import academy.incidentradar.source.LogSource;
import academy.incidentradar.statistics.StatisticsAccumulator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Разбирает строки и собирает статистику периодов за одно чтение. */
public final class LogAnalysisService {

    private static final Logger LOG = LoggerFactory.getLogger(LogAnalysisService.class);

    private final LogReader reader = new LogReader();
    private final NginxLogParser parser = new NginxLogParser();

    public AnalysisResult analyze(List<? extends LogSource> sources, AnalysisOptions options) {
        Objects.requireNonNull(options, "options");

        var primary = new PeriodAccumulator(options.dateRange());
        var comparisonRange = options.comparisonDateRange();
        var reference = comparisonRange == null ? null : new PeriodAccumulator(comparisonRange);
        var malformedLines = new AtomicLong();
        Runnable onMalformedLine = () -> {
            malformedLines.incrementAndGet();
            LOG.warn("skipped malformed log line");
        };

        var readResult = reader.read(
                sources,
                line -> {
                    var parsed = parser.parse(line);
                    if (parsed.isEmpty()) {
                        onMalformedLine.run();
                        return;
                    }

                    var record = parsed.orElseThrow();
                    primary.accept(record);
                    if (reference != null) {
                        reference.accept(record);
                    }
                },
                onMalformedLine);

        var statistics = primary.accumulator().snapshot();
        var comparison = reference == null
                ? null
                : PeriodComparison.between(statistics, reference.accumulator().snapshot());
        return new AnalysisResult(readResult, malformedLines.get(), statistics, comparison);
    }

    private record PeriodAccumulator(DateFilter filter, StatisticsAccumulator accumulator) {

        private PeriodAccumulator(DateRange range) {
            this(new DateFilter(range), new StatisticsAccumulator());
        }

        private void accept(LogRecord record) {
            if (filter.test(record)) {
                accumulator.accept(record);
            }
        }
    }
}
