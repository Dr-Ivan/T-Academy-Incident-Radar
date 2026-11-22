package academy.incidentradar.report;

import static org.assertj.core.api.Assertions.assertThat;

import academy.incidentradar.analysis.LogAnalysisService;
import academy.incidentradar.model.AnalysisOptions;
import academy.incidentradar.model.DateRange;
import academy.incidentradar.model.LogStatistics;
import academy.incidentradar.model.PeriodComparison;
import academy.incidentradar.model.ResourceStatistics;
import academy.incidentradar.model.ResponseSizeStatistics;
import academy.incidentradar.source.LogSourceResolver;
import academy.incidentradar.statistics.StatisticsAccumulator;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AsciiDoc-отчёт")
class AsciiDocReportRendererTest {

    private static final DateRange ALL_DATES = new DateRange(null, null);
    private static final DateRange PERIOD = new DateRange(LocalDate.of(2025, 3, 2), LocalDate.of(2025, 3, 3));
    private static final DateRange REFERENCE_PERIOD =
            new DateRange(LocalDate.of(2025, 2, 28), LocalDate.of(2025, 3, 1));

    private final ReportRenderer renderer = new AsciiDocReportRenderer();

    @Test
    @DisplayName("Основные сэмплы формируют AsciiDoc-таблицы со статистикой и бонусами")
    void rendersSampleStatistics() {
        var options = new AnalysisOptions("samples/logs/access-*.log", OutputFormat.ADOC, null, ALL_DATES);
        var sources = new LogSourceResolver().resolve(options.path());
        var result = new LogAnalysisService().analyze(sources, options);

        var rendered = renderer.render(ReportData.from(options, result));

        assertThat(rendered)
                .startsWith("= Отчёт Incident Radar\n\n")
                .contains(
                        "[cols=\"1*\",options=\"header\"]\n|===\n",
                        "[cols=\"2*\",options=\"header\"]\n|===\n",
                        "access-2025-03-01.log",
                        "access-2025-03-02.log",
                        "|+++Количество запросов+++ |+++3000+++",
                        "|+++Средний размер ответа, байт+++ |+++2363.94+++",
                        "|+++Максимальный размер ответа, байт+++ |+++10891+++",
                        "|+++p95, байт+++ |+++5450+++",
                        "|+++/api/v1/accounts+++ |+++581+++",
                        "|+++200 OK+++ |+++1767+++",
                        "|+++429 Too Many Requests+++ |+++60+++",
                        "|+++суббота+++ |+++1500+++",
                        "|+++воскресенье+++ |+++1500+++",
                        "|+++понедельник+++ |+++0+++",
                        "|+++HTTP/1.1+++",
                        "|+++HTTP/2.0+++")
                .doesNotContain("== Период сравнения")
                .endsWith("|===\n\n")
                .doesNotContain("\r");
        assertThat(rendered.lines().filter("|==="::equals).count()).isEqualTo(12);
    }

    @Test
    @DisplayName("Сравнение включает оба периода и изменения четырёх метрик")
    void rendersComparison() {
        var primary = statistics(2, "150.00", 200);
        var reference = statistics(4, "200.00", 400);
        var report = new ReportData(
                List.of("access.log"), PERIOD, primary, REFERENCE_PERIOD, PeriodComparison.between(primary, reference));

        var rendered = renderer.render(report);

        assertThat(rendered)
                .contains(
                        "== Основной период",
                        "== Период сравнения",
                        "[cols=\"3*\",options=\"header\"]",
                        "|+++Начальная дата+++ |+++2025-03-02+++",
                        "|+++Конечная дата+++ |+++2025-03-03+++",
                        "|+++Начальная дата+++ |+++2025-02-28+++",
                        "|+++Конечная дата+++ |+++2025-03-01+++",
                        "|+++Количество запросов+++ |+++2+++",
                        "|+++Количество запросов+++ |+++4+++",
                        "|+++Количество запросов+++ |+++-2+++ |+++-50.00%+++",
                        "|+++Средний размер ответа, байт+++ |+++-50.00+++ |+++-25.00%+++",
                        "|+++Максимальный размер ответа, байт+++ |+++-200+++ |+++-50.00%+++",
                        "|+++p95, байт+++ |+++-200+++ |+++-50.00%+++");
        assertThat(rendered.lines().filter("|==="::equals).count()).isEqualTo(24);
    }

    @Test
    @DisplayName("Нулевая база выводит н/д вместо неопределённого процента")
    void rendersUndefinedPercentages() {
        var primary = statistics(2, "150.00", 200);
        var report = new ReportData(
                List.of("access.log"),
                PERIOD,
                primary,
                REFERENCE_PERIOD,
                PeriodComparison.between(primary, new StatisticsAccumulator().snapshot()));

        assertThat(renderer.render(report))
                .contains(
                        "|+++Количество запросов+++ |+++2+++ |+++н/д+++",
                        "|+++Средний размер ответа, байт+++ |+++150.00+++ |+++н/д+++",
                        "|+++Максимальный размер ответа, байт+++ |+++200+++ |+++н/д+++",
                        "|+++p95, байт+++ |+++200+++ |+++н/д+++");
    }

    @Test
    @DisplayName("Пустая выборка сохраняет нули и заполнители пустых таблиц")
    void rendersEmptyStatistics() {
        var report =
                new ReportData(List.of("empty.log"), ALL_DATES, new StatisticsAccumulator().snapshot(), null, null);

        assertThat(renderer.render(report))
                .contains(
                        "|+++Начальная дата+++ |+++Не ограничено+++",
                        "|+++Конечная дата+++ |+++Не ограничено+++",
                        "|+++Количество запросов+++ |+++0+++",
                        "|+++Средний размер ответа, байт+++ |+++0.00+++",
                        "|+++Максимальный размер ответа, байт+++ |+++0+++",
                        "|+++p95, байт+++ |+++0+++",
                        "|+++понедельник+++ |+++0+++",
                        "|+++—+++ |+++—+++");
    }

    @Test
    @DisplayName("Разметка, разделители таблиц и управляющие символы остаются данными")
    void escapesCells() {
        var value = "a\\b|<tag>&*x*{name}[y]+++pass:[z]\r\n\t|===";
        var escaped = "a\\b&#124;&lt;tag&gt;&amp;*x*{name}[y]&#43;&#43;&#43;pass:[z]&#13;&#10;&#9;&#124;===";
        var statistics = new LogStatistics(
                1,
                new ResponseSizeStatistics(new BigDecimal("1.00"), 1, 1),
                Map.of(200, 1L),
                List.of(new ResourceStatistics(value, 1)),
                Map.of(DayOfWeek.SATURDAY, 1L),
                List.of("HTTP/1.1"));
        var report = new ReportData(List.of(value), ALL_DATES, statistics, null, null);

        var rendered = renderer.render(report);

        assertThat(rendered)
                .contains("|+++" + escaped + "+++", "|+++" + escaped + "+++ |+++1+++")
                .doesNotContain("<tag>", "\r", "\t", "\n\t|===");
        assertThat(rendered.lines().filter("|==="::equals).count()).isEqualTo(12);
    }

    private static LogStatistics statistics(long requests, String average, long max) {
        return new LogStatistics(
                requests,
                new ResponseSizeStatistics(new BigDecimal(average), max, max),
                Map.of(200, requests),
                List.of(new ResourceStatistics("/health", requests)),
                Map.of(DayOfWeek.SATURDAY, requests),
                List.of("HTTP/1.1"));
    }
}
