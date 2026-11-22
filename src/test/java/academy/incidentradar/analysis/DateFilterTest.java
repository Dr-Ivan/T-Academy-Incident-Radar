package academy.incidentradar.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import academy.incidentradar.model.DateRange;
import academy.incidentradar.model.LogRecord;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Проверки календарной даты записи при разных часовых смещениях. */
@DisplayName("Фильтрация с учётом смещения записи")
class DateFilterTest {

    private final DateFilter filter = new DateFilter(new DateRange(LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 2)));

    @ParameterizedTest
    @CsvSource({
        "2025-03-01T00:30:00+03:00, true",
        "2025-02-28T23:30:00-03:00, false",
        "2025-03-02T23:30:00-03:00, true",
        "2025-03-03T00:30:00+03:00, false"
    })
    @DisplayName("Используется дата в смещении записи")
    void usesDateInRecordOffset(String timestamp, boolean expected) {
        var record = new LogRecord(
                "93.180.71.3", "-", OffsetDateTime.parse(timestamp), "GET", "/health", "HTTP/1.1", 200, 0, "-", "test");

        assertThat(filter.test(record)).isEqualTo(expected);
    }
}
