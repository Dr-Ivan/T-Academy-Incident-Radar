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
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JSON-отчёт")
class JsonReportRendererTest {

    private static final DateRange ALL_DATES = new DateRange(null, null);
    private static final JsonMapper READER = JsonMapper.builder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .build();

    private final ReportRenderer renderer = new JsonReportRenderer();

    @Test
    @DisplayName("Основные сэмплы формируют полный JSON с эталонной статистикой")
    void rendersSampleStatistics() throws IOException {
        var options = new AnalysisOptions("samples/logs/access-*.log", OutputFormat.JSON, null, ALL_DATES);
        var sources = new LogSourceResolver().resolve(options.path());
        var result = new LogAnalysisService().analyze(sources, options);

        var json = READER.readTree(renderer.render(ReportData.from(options, result)));

        assertThat(json.path("files").size()).isEqualTo(2);
        assertThat(json.path("totalRequestsCount").longValue()).isEqualTo(3000);
        assertThat(json.path("responseSizeInBytes").path("average").decimalValue())
                .isEqualByComparingTo("2363.94");
        assertThat(json.path("responseSizeInBytes").path("max").longValue()).isEqualTo(10891);
        assertThat(json.path("responseSizeInBytes").path("p95").longValue()).isEqualTo(5450);
        assertThat(json.path("responseCodes").size()).isEqualTo(10);
        assertThat(json.path("responseCodes").path("200").longValue()).isEqualTo(1767);
        assertThat(json.path("resources").size()).isEqualTo(10);
        assertThat(json.path("resources").path(0).path("resource").asText()).isEqualTo("/api/v1/accounts");
        assertThat(json.path("resources").path(0).path("totalRequestsCount").longValue())
                .isEqualTo(581);
        assertThat(json.path("requestsByWeekday").size()).isEqualTo(7);
        assertThat(json.path("requestsByWeekday").path("SATURDAY").longValue()).isEqualTo(1500);
        assertThat(json.path("requestsByWeekday").path("SUNDAY").longValue()).isEqualTo(1500);
        assertThat(json.path("requestsByWeekday").path("MONDAY").longValue()).isZero();
        assertThat(json.path("uniqueProtocols").size()).isEqualTo(2);
        assertThat(json.path("uniqueProtocols").path(0).asText()).isEqualTo("HTTP/1.1");
        assertThat(json.path("uniqueProtocols").path(1).asText()).isEqualTo("HTTP/2.0");
        assertThat(json.path("period").path("from").isNull()).isTrue();
        assertThat(json.path("period").path("to").isNull()).isTrue();
        assertThat(json.has("comparison")).isFalse();
    }

    @Test
    @DisplayName("Пустая выборка сохраняет нулевые метрики и два знака среднего")
    void rendersEmptyStatistics() throws IOException {
        var report =
                new ReportData(List.of("empty.log"), ALL_DATES, new StatisticsAccumulator().snapshot(), null, null);

        var rendered = renderer.render(report);
        var json = READER.readTree(rendered);

        assertThat(json.path("files").path(0).asText()).isEqualTo("empty.log");
        assertThat(json.path("totalRequestsCount").longValue()).isZero();
        assertThat(json.path("responseSizeInBytes").path("max").longValue()).isZero();
        assertThat(json.path("responseSizeInBytes").path("p95").longValue()).isZero();
        assertThat(json.path("responseCodes").isEmpty()).isTrue();
        assertThat(json.path("resources").isEmpty()).isTrue();
        assertThat(json.path("uniqueProtocols").isEmpty()).isTrue();
        assertThat(json.path("requestsByWeekday").size()).isEqualTo(7);
        assertThat(rendered).contains("\"average\" : 0.00").endsWith("\n").doesNotContain("\r");
    }

    @Test
    @DisplayName("Сравнение содержит даты, статистику базы и изменения всех четырёх метрик")
    void rendersComparison() throws IOException {
        var primary = statistics(2, "150.00", 200);
        var reference = statistics(4, "200.00", 400);
        var report = new ReportData(
                List.of("access.log"),
                new DateRange(LocalDate.of(2025, 3, 2), LocalDate.of(2025, 3, 3)),
                primary,
                new DateRange(LocalDate.of(2025, 2, 28), LocalDate.of(2025, 3, 1)),
                PeriodComparison.between(primary, reference));

        var json = READER.readTree(renderer.render(report));
        var comparison = json.path("comparison");

        assertThat(json.path("period").path("from").asText()).isEqualTo("2025-03-02");
        assertThat(json.path("period").path("to").asText()).isEqualTo("2025-03-03");
        assertThat(comparison.path("period").path("from").asText()).isEqualTo("2025-02-28");
        assertThat(comparison.path("period").path("to").asText()).isEqualTo("2025-03-01");
        assertThat(comparison
                        .path("referenceStatistics")
                        .path("totalRequestsCount")
                        .longValue())
                .isEqualTo(4);
        assertThat(comparison.path("requestsChange").path("difference").decimalValue())
                .isEqualByComparingTo("-2");
        assertThat(comparison.path("requestsChange").path("percentage").decimalValue())
                .isEqualByComparingTo("-50.00");
        assertThat(comparison.path("averageSizeChange").path("difference").decimalValue())
                .isEqualByComparingTo("-50.00");
        assertThat(comparison.path("averageSizeChange").path("percentage").decimalValue())
                .isEqualByComparingTo("-25.00");
        assertThat(comparison.path("maxSizeChange").path("difference").decimalValue())
                .isEqualByComparingTo("-200");
        assertThat(comparison.path("p95SizeChange").path("difference").decimalValue())
                .isEqualByComparingTo("-200");
    }

    @Test
    @DisplayName("Нулевая база сохраняет null вместо процента")
    void rendersUndefinedPercentages() throws IOException {
        var primary = statistics(2, "150.00", 200);
        var report = new ReportData(
                List.of("access.log"),
                new DateRange(LocalDate.of(2025, 3, 2), LocalDate.of(2025, 3, 3)),
                primary,
                new DateRange(LocalDate.of(2025, 2, 28), LocalDate.of(2025, 3, 1)),
                PeriodComparison.between(primary, new StatisticsAccumulator().snapshot()));

        var comparison = READER.readTree(renderer.render(report)).path("comparison");

        assertThat(comparison.path("requestsChange").path("percentage").isNull())
                .isTrue();
        assertThat(comparison.path("averageSizeChange").path("percentage").isNull())
                .isTrue();
        assertThat(comparison.path("maxSizeChange").path("percentage").isNull()).isTrue();
        assertThat(comparison.path("p95SizeChange").path("percentage").isNull()).isTrue();
        assertThat(comparison.path("requestsChange").path("difference").longValue())
                .isEqualTo(2);
    }

    @Test
    @DisplayName("Строки экранируются, большие числа и десятичная точность сохраняются")
    void preservesStringsAndExactNumbers() throws IOException {
        var file = "логи/день \"1\".log";
        var resource = "/путь\\раздел?q=\"текст\"\t\n";
        var large = 9_007_199_254_740_993L;
        var statistics = new LogStatistics(
                large,
                new ResponseSizeStatistics(new BigDecimal("9007199254740993.00"), large, large),
                Map.of(200, large),
                List.of(new ResourceStatistics(resource, large)),
                Map.of(DayOfWeek.SATURDAY, large),
                List.of("HTTP/1.1"));
        var report = new ReportData(List.of(file), ALL_DATES, statistics, null, null);

        var rendered = renderer.render(report);
        var json = READER.readTree(rendered);

        assertThat(json.path("files").path(0).asText()).isEqualTo(file);
        assertThat(json.path("resources").path(0).path("resource").asText()).isEqualTo(resource);
        assertThat(json.path("totalRequestsCount").isIntegralNumber()).isTrue();
        assertThat(json.path("totalRequestsCount").longValue()).isEqualTo(large);
        assertThat(json.path("responseCodes").path("200").longValue()).isEqualTo(large);
        assertThat(json.path("responseSizeInBytes").path("average").decimalValue())
                .isEqualByComparingTo("9007199254740993.00");
        assertThat(rendered).contains("\"average\" : 9007199254740993.00");
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
