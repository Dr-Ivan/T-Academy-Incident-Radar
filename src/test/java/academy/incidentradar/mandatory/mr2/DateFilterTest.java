package academy.incidentradar.mandatory.mr2;

import static org.assertj.core.api.Assertions.assertThat;

import academy.incidentradar.analysis.DateFilter;
import academy.incidentradar.model.DateRange;
import academy.incidentradar.model.LogRecord;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Обязательные тесты: фильтрация по датам. */
@DisplayName("MR2. Фильтр по датам")
class DateFilterTest {

    private static final LocalDate FROM = LocalDate.of(2025, 3, 1);
    private static final LocalDate TO = LocalDate.of(2025, 3, 2);

    private static final List<LogRecord> RECORDS = List.of(
            record("2025-02-28T23:59:59Z"),
            record("2025-03-01T00:00:00Z"),
            record("2025-03-02T00:00:00Z"),
            record("2025-03-02T23:59:59Z"),
            record("2025-03-03T00:00:00Z"));

    @Test
    @DisplayName("Без --from и --to обрабатываются все записи")
    void noFilterKeepsEverything() {
        var result = filter(new DateRange(null, null));
        assertThat(result).containsExactlyElementsOf(RECORDS);
    }

    @Test
    @DisplayName("Только --from: открытая правая граница")
    void onlyFromIsOpenEnded() {
        var result = filter(new DateRange(FROM, null));

        assertThat(result).containsExactly(RECORDS.get(1), RECORDS.get(2), RECORDS.get(3), RECORDS.get(4));
    }

    @Test
    @DisplayName("Только --to: открытая левая граница")
    void onlyToIsOpenEnded() {
        var result = filter(new DateRange(null, TO));

        assertThat(result).containsExactly(RECORDS.get(0), RECORDS.get(1), RECORDS.get(2), RECORDS.get(3));
    }

    @Test
    @DisplayName("Обе границы: период включительно")
    void bothBoundsAreInclusive() {
        var result = filter(new DateRange(FROM, TO));

        assertThat(result).containsExactly(RECORDS.get(1), RECORDS.get(2), RECORDS.get(3));
    }

    private static List<LogRecord> filter(DateRange dateRange) {
        return RECORDS.stream().filter(new DateFilter(dateRange)).toList();
    }

    private static LogRecord record(String timestamp) {
        return new LogRecord(
                "93.180.71.3", "-", OffsetDateTime.parse(timestamp), "GET", "/health", "HTTP/1.1", 200, 0, "-", "test");
    }
}
