package academy.incidentradar.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import academy.incidentradar.model.AnalysisOptions;
import academy.incidentradar.model.AnalysisResult;
import academy.incidentradar.model.DateRange;
import academy.incidentradar.model.LogReadResult;
import academy.incidentradar.model.LogStatistics;
import academy.incidentradar.model.PeriodComparison;
import academy.incidentradar.model.ResourceStatistics;
import academy.incidentradar.model.ResponseSizeStatistics;
import academy.incidentradar.statistics.StatisticsAccumulator;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Данные отчёта")
class ReportDataTest {

    private static final DateRange PERIOD = new DateRange(LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 2));
    private static final DateRange COMPARISON_PERIOD =
            new DateRange(LocalDate.of(2025, 2, 27), LocalDate.of(2025, 2, 28));

    @Test
    @DisplayName("В отчёт передаются источники и статистика отобранных запросов")
    void keepsSelectedStatisticsAndSources() {
        var statistics = statistics();
        var result = new AnalysisResult(new LogReadResult(List.of("first.log", "second.log"), 3), 1, statistics, null);
        var options = new AnalysisOptions("*.log", OutputFormat.JSON, null, PERIOD);

        var report = ReportData.from(options, result);

        assertThat(report.files()).containsExactly("first.log", "second.log");
        assertThat(report.period()).isEqualTo(PERIOD);
        assertThat(report.statistics()).isEqualTo(statistics);
        assertThat(report.statistics().totalRequestsCount()).isEqualTo(2);
        assertThat(report.comparisonPeriod()).isNull();
        assertThat(report.comparison()).isNull();
    }

    @Test
    @DisplayName("Сравнение с пустой базой сохраняет период и отсутствующие проценты")
    void keepsComparisonWithEmptyReference() {
        var statistics = statistics();
        var comparison = PeriodComparison.between(statistics, new StatisticsAccumulator().snapshot());
        var result = new AnalysisResult(new LogReadResult(List.of("access.log"), 2), 0, statistics, comparison);
        var options = new AnalysisOptions("access.log", OutputFormat.JSON, null, PERIOD, COMPARISON_PERIOD);

        var report = ReportData.from(options, result);

        assertThat(report.comparisonPeriod()).isEqualTo(COMPARISON_PERIOD);
        assertThat(report.comparison()).isEqualTo(comparison);
        assertThat(report.comparison().requestsChange().percentage()).isNull();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Режим сравнения в настройках и результате должен совпадать")
    void rejectsComparisonMismatch(boolean comparisonRequested) {
        var statistics = statistics();
        var comparison = PeriodComparison.between(statistics, new StatisticsAccumulator().snapshot());
        var options = new AnalysisOptions(
                "access.log", OutputFormat.JSON, null, PERIOD, comparisonRequested ? COMPARISON_PERIOD : null);
        var result = new AnalysisResult(
                new LogReadResult(List.of("access.log"), 2), 0, statistics, comparisonRequested ? null : comparison);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> ReportData.from(options, result))
                .withMessageContaining("Период и результат сравнения");
    }

    @Test
    @DisplayName("Источники отчёта являются неизменяемым независимым списком")
    void protectsSources() {
        var files = new ArrayList<>(List.of("first.log", "second.log"));
        var report = new ReportData(files, PERIOD, statistics(), null, null);

        files.clear();

        assertThat(report.files()).containsExactly("first.log", "second.log");
        assertThatThrownBy(() -> report.files().add("third.log")).isInstanceOf(UnsupportedOperationException.class);
    }

    private static LogStatistics statistics() {
        return new LogStatistics(
                2,
                new ResponseSizeStatistics(new BigDecimal("150.00"), 200, 200),
                Map.of(200, 2L),
                List.of(new ResourceStatistics("/health", 2)),
                Map.of(DayOfWeek.SATURDAY, 2L),
                List.of("HTTP/1.1"));
    }
}
