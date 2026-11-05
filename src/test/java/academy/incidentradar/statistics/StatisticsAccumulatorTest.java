package academy.incidentradar.statistics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import academy.incidentradar.analysis.DateFilter;
import academy.incidentradar.model.DateRange;
import academy.incidentradar.model.LogRecord;
import academy.incidentradar.model.ResourceStatistics;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Дополнительные проверки накопления статистики. */
@DisplayName("Накопление статистики")
class StatisticsAccumulatorTest {

    @Test
    @DisplayName("Пустая выборка имеет нулевые счётчики")
    void emptyStatisticsHasZeroCounts() {
        var statistics = new StatisticsAccumulator().snapshot();

        assertThat(statistics.totalRequestsCount()).isZero();
        assertThat(statistics.responseCodes()).isEmpty();
        assertThat(statistics.resources()).isEmpty();
        assertThat(statistics.uniqueProtocols()).isEmpty();
        assertThat(statistics.requestsByWeekday().keySet()).containsExactly(DayOfWeek.values());
        assertThat(statistics.requestsByWeekday().values()).containsOnly(0L);

        var sizes = statistics.responseSizeInBytes();
        assertThat(sizes.average().toPlainString()).isEqualTo("0.00");
        assertThat(sizes.max()).isZero();
        assertThat(sizes.p95()).isZero();
    }

    @Test
    @DisplayName("Дни недели и протоколы считаются по отобранным записям")
    void countsBonusDataForSelectedRecords() {
        var accumulator = new StatisticsAccumulator();
        var filter = new DateFilter(new DateRange(LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 2)));

        var records = List.of(
                record("2025-03-01T00:30:00+03:00", "/a", 500, "HTTP/2.0"),
                record("2025-03-02T23:30:00-03:00", "/b", 200, "HTTP/1.1"),
                record("2025-03-01T12:00:00Z", "/a", 404, "HTTP/1.0"),
                record("2025-03-01T15:00:00Z", "/a", 200, "HTTP/1.1"),
                record("2025-02-28T23:30:00-03:00", "/outside", 500, "HTTP/3.0"));

        records.stream().filter(filter).forEach(accumulator);
        var statistics = accumulator.snapshot();

        assertThat(statistics.totalRequestsCount()).isEqualTo(4);
        assertThat(statistics.requestsByWeekday().get(DayOfWeek.SATURDAY)).isEqualTo(3L);
        assertThat(statistics.requestsByWeekday().get(DayOfWeek.SUNDAY)).isEqualTo(1L);
        assertThat(statistics.requestsByWeekday().get(DayOfWeek.FRIDAY)).isZero();
        assertThat(statistics.uniqueProtocols()).containsExactly("HTTP/1.0", "HTTP/1.1", "HTTP/2.0");
        assertThat(statistics.responseCodes().keySet()).containsExactly(200, 404, 500);
    }

    @Test
    @DisplayName("При равных счётчиках top-10 выбирается по имени ресурса")
    void breaksResourceTiesByName() {
        var accumulator = new StatisticsAccumulator();
        for (var resource : List.of("/l", "/k", "/j", "/i", "/h", "/g", "/f", "/e", "/d", "/c", "/b", "/a")) {
            accumulator.accept(record("2025-03-01T12:00:00Z", resource, 200, "HTTP/1.1"));
        }

        assertThat(accumulator.snapshot().resources())
                .extracting(ResourceStatistics::resource)
                .containsExactly("/a", "/b", "/c", "/d", "/e", "/f", "/g", "/h", "/i", "/j");
    }

    @Test
    @DisplayName("Снимок неизменяем и независим от дальнейшего накопления")
    void snapshotIsImmutableAndIndependent() {
        var accumulator = new StatisticsAccumulator();
        accumulator.accept(record("2025-03-01T12:00:00Z", "/first", 200, "HTTP/1.1"));
        var snapshot = accumulator.snapshot();

        accumulator.accept(record("2025-03-02T12:00:00Z", "/second", 500, "HTTP/2.0"));

        assertThat(snapshot.totalRequestsCount()).isEqualTo(1);
        assertThat(snapshot.responseCodes()).isEqualTo(Map.of(200, 1L));
        assertThat(snapshot.resources()).containsExactly(new ResourceStatistics("/first", 1));
        assertThat(snapshot.requestsByWeekday().get(DayOfWeek.SATURDAY)).isEqualTo(1L);
        assertThat(snapshot.requestsByWeekday().get(DayOfWeek.SUNDAY)).isZero();
        assertThat(snapshot.uniqueProtocols()).containsExactly("HTTP/1.1");
        assertThat(accumulator.snapshot().totalRequestsCount()).isEqualTo(2);

        assertThatThrownBy(() -> snapshot.responseCodes().put(500, 1L))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> snapshot.resources().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> snapshot.requestsByWeekday().put(DayOfWeek.MONDAY, 1L))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> snapshot.uniqueProtocols().add("HTTP/3.0"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Средний размер сохраняет точность при сумме за пределами long")
    void averagesSizesWithoutOverflow() {
        var accumulator = new StatisticsAccumulator();
        for (var size : List.of(0L, 0L, Long.MAX_VALUE, Long.MAX_VALUE)) {
            accumulator.accept(record("2025-03-01T12:00:00Z", "/health", 200, "HTTP/1.1", size));
        }

        var sizes = accumulator.snapshot().responseSizeInBytes();

        assertThat(sizes.average().toPlainString()).isEqualTo("4611686018427387903.50");
        assertThat(sizes.max()).isEqualTo(Long.MAX_VALUE);
        assertThat(sizes.p95()).isEqualTo(Long.MAX_VALUE);
    }

    private static LogRecord record(String timestamp, String resource, int status, String protocol) {
        return record(timestamp, resource, status, protocol, 0);
    }

    private static LogRecord record(String timestamp, String resource, int status, String protocol, long size) {
        return new LogRecord(
                "93.180.71.3",
                "-",
                OffsetDateTime.parse(timestamp),
                "GET",
                resource,
                protocol,
                status,
                size,
                "-",
                "test");
    }
}
