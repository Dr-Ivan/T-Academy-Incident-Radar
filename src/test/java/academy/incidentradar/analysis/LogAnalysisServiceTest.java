package academy.incidentradar.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import academy.incidentradar.model.AnalysisOptions;
import academy.incidentradar.model.DateRange;
import academy.incidentradar.model.PeriodComparison;
import academy.incidentradar.parser.NginxLogParser;
import academy.incidentradar.report.OutputFormat;
import academy.incidentradar.source.LogSource;
import academy.incidentradar.statistics.StatisticsAccumulator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Проверки анализа двух периодов. */
@DisplayName("Сравнение периодов")
class LogAnalysisServiceTest {

    @Test
    @DisplayName("Пересекающиеся периоды рассчитываются за одно чтение с учётом смещения")
    void countsOverlappingPeriodsInOnePass() {
        var lines = List.of(
                line("01/Mar/2025:00:15:00 +0300", 100),
                line("02/Mar/2025:00:30:00 +0300", 200),
                line("03/Mar/2025:10:00:00 +0300", 400),
                line("03/Mar/2025:11:00:00 +0300", 600),
                line("04/Mar/2025:12:00:00 +0300", 800));
        var reads = new AtomicInteger();
        var source = new LogSource() {
            @Override
            public String name() {
                return "example.log";
            }

            @Override
            public void forEachLine(Consumer<String> action) {
                reads.incrementAndGet();
                lines.forEach(action);
            }
        };

        var first = LocalDate.of(2025, 3, 1);
        var middle = first.plusDays(1);
        var last = middle.plusDays(1);
        var options = new AnalysisOptions(
                source.name(), OutputFormat.MARKDOWN, null, new DateRange(middle, last), new DateRange(first, middle));

        var result = new LogAnalysisService().analyze(List.of(source), options);

        assertThat(reads.get()).isEqualTo(1);
        assertThat(result.readResult().linesRead()).isEqualTo(5);
        assertThat(result.malformedLines()).isZero();
        assertThat(result.statistics().totalRequestsCount()).isEqualTo(3);

        var comparison = result.comparison();
        assertThat(comparison).isNotNull();
        assertThat(comparison.referenceStatistics().totalRequestsCount()).isEqualTo(2);
        assertThat(comparison.requestsChange().difference()).isEqualTo(BigDecimal.ONE);
        assertThat(comparison.requestsChange().percentage()).isEqualTo(new BigDecimal("50.00"));
        assertThat(comparison.averageSizeChange().difference()).isEqualTo(new BigDecimal("250.00"));
        assertThat(comparison.averageSizeChange().percentage()).isEqualTo(new BigDecimal("166.67"));
        assertThat(comparison.maxSizeChange().difference()).isEqualTo(new BigDecimal("400"));
        assertThat(comparison.p95SizeChange().difference()).isEqualTo(new BigDecimal("400"));

        var reverse = PeriodComparison.between(comparison.referenceStatistics(), result.statistics());
        assertThat(reverse.requestsChange().difference()).isEqualTo(new BigDecimal("-1"));
        assertThat(reverse.requestsChange().percentage()).isEqualTo(new BigDecimal("-33.33"));
    }

    @Test
    @DisplayName("При пустом периоде сравнения сохраняются разности, а проценты отсутствуют")
    void leavesPercentageUndefinedForZeroBaseline() {
        var primary = new StatisticsAccumulator();
        var parsed = new NginxLogParser().parse(line("01/Mar/2025:12:00:00 +0000", 100));
        primary.accept(parsed.orElseThrow());

        var result = PeriodComparison.between(primary.snapshot(), new StatisticsAccumulator().snapshot());

        assertThat(result.requestsChange().difference()).isEqualTo(BigDecimal.ONE);
        assertThat(result.requestsChange().percentage()).isNull();
        assertThat(result.averageSizeChange().difference()).isEqualTo(new BigDecimal("100.00"));
        assertThat(result.averageSizeChange().percentage()).isNull();
        assertThat(result.maxSizeChange().percentage()).isNull();
        assertThat(result.p95SizeChange().percentage()).isNull();
    }

    private static String line(String timestamp, long size) {
        return "93.180.71.3 - - [" + timestamp + "] \"GET /health HTTP/1.1\" 200 " + size + " \"-\" \"test\"";
    }
}
