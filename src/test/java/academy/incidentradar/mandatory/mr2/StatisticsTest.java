package academy.incidentradar.mandatory.mr2;

import static org.assertj.core.api.Assertions.assertThat;

import academy.incidentradar.model.LogRecord;
import academy.incidentradar.model.ResourceStatistics;
import academy.incidentradar.model.ResponseSizeStatistics;
import academy.incidentradar.statistics.StatisticsAccumulator;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Обязательные тесты: подсчёт статистики. */
@DisplayName("MR2. Статистика")
class StatisticsTest {

    @Test
    @DisplayName("Распределение кодов: 200,200,404,500,500,500 -> 200:2, 404:1, 500:3")
    void responseCodesAreCounted() {
        var accumulator = new StatisticsAccumulator();
        for (var status : List.of(200, 200, 404, 500, 500, 500)) {
            accumulator.accept(record("/health", status));
        }

        var statistics = accumulator.snapshot();

        assertThat(statistics.totalRequestsCount()).isEqualTo(6);
        assertThat(statistics.responseCodes()).isEqualTo(Map.of(200, 2L, 404, 1L, 500, 3L));
    }

    @Test
    @DisplayName("Top-10 ресурсов отсортирован по убыванию количества")
    void topResourcesAreSortedDescending() {
        var accumulator = new StatisticsAccumulator();
        for (var count = 1; count <= 12; count++) {
            for (var index = 0; index < count; index++) {
                accumulator.accept(record("/resource-" + count, 200));
            }
        }

        assertThat(accumulator.snapshot().resources())
                .containsExactly(
                        new ResourceStatistics("/resource-12", 12),
                        new ResourceStatistics("/resource-11", 11),
                        new ResourceStatistics("/resource-10", 10),
                        new ResourceStatistics("/resource-9", 9),
                        new ResourceStatistics("/resource-8", 8),
                        new ResourceStatistics("/resource-7", 7),
                        new ResourceStatistics("/resource-6", 6),
                        new ResourceStatistics("/resource-5", 5),
                        new ResourceStatistics("/resource-4", 4),
                        new ResourceStatistics("/resource-3", 3));
    }

    @Test
    @DisplayName("Размеры 100..500: average 300, max 500, p95 500")
    void responseSizeStatistics() {
        var sizes = responseSizes(List.of(100L, 200L, 300L, 400L, 500L));
        assertThat(sizes.average().toPlainString()).isEqualTo("300.00");
        assertThat(sizes.max()).isEqualTo(500);
        assertThat(sizes.p95()).isEqualTo(500);
    }

    @Test
    @DisplayName("Дробные значения округляются до 2 знаков после запятой")
    void fractionalValuesAreRoundedToTwoDecimals() {
        var sizes = responseSizes(List.of(0L, 0L, 0L, 0L, 0L, 0L, 0L, 1L));
        assertThat(sizes.average().toPlainString()).isEqualTo("0.13");
    }

    /** Nearest rank берёт значение на позиции ceil(0.95 * n) при счёте с единицы. */
    @Test
    @DisplayName("Выбранный метод расчёта p95 зафиксирован и воспроизводим")
    void p95MethodIsDocumentedAndStable() {
        var sizes = new ArrayList<>(Collections.nCopies(18, 100L));
        sizes.add(200L);
        sizes.add(300L);

        var statistics = responseSizes(sizes);

        assertThat(statistics.p95()).isEqualTo(200);
        assertThat(responseSizes(sizes.reversed())).isEqualTo(statistics);
    }

    private static ResponseSizeStatistics responseSizes(List<Long> sizes) {
        var accumulator = new StatisticsAccumulator();
        for (var size : sizes) {
            accumulator.accept(record("/health", 200, size));
        }
        return accumulator.snapshot().responseSizeInBytes();
    }

    private static LogRecord record(String resource, int status) {
        return record(resource, status, 0);
    }

    private static LogRecord record(String resource, int status, long size) {
        return new LogRecord(
                "93.180.71.3",
                "-",
                OffsetDateTime.parse("2025-03-01T12:00:00Z"),
                "GET",
                resource,
                "HTTP/1.1",
                status,
                size,
                "-",
                "test");
    }
}
