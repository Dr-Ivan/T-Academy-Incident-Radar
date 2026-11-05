package academy.incidentradar.model;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Проверки инвариантов разобранной записи лога. */
@DisplayName("Запись NGINX-лога")
class LogRecordTest {

    private static final OffsetDateTime TIMESTAMP = OffsetDateTime.parse("2025-03-01T00:30:00+03:00");

    @ParameterizedTest
    @ValueSource(ints = {100, 444, 599})
    @DisplayName("Допустимые HTTP-коды принимаются")
    void acceptsValidStatus(int status) {
        assertThatCode(() -> record("GET", status, 0)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(ints = {99, 600})
    @DisplayName("HTTP-код за пределами допустимого диапазона отклоняется")
    void rejectsInvalidStatus(int status) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> record("GET", status, 0))
                .withMessageContaining("HTTP-код");
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 2_147_483_648L, Long.MAX_VALUE})
    @DisplayName("Допускаются нулевой размер и значения за пределами int")
    void acceptsNonNegativeSize(long size) {
        assertThatCode(() -> record("GET", 200, size)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Отрицательный размер ответа отклоняется")
    void rejectsNegativeSize() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> record("GET", 200, -1))
                .withMessageContaining("Размер ответа");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    @DisplayName("Пустой метод запроса отклоняется")
    void rejectsBlankMethod(String method) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> record(method, 200, 0))
                .withMessageContaining("method");
    }

    @Test
    @DisplayName("Пустые referer и userAgent допустимы")
    void acceptsEmptyQuotedFields() {
        assertThatCode(() -> new LogRecord(
                        "93.180.71.3", "-", TIMESTAMP, "GET", "/downloads/product_1", "HTTP/1.1", 200, 0, "", ""))
                .doesNotThrowAnyException();
    }

    private static LogRecord record(String method, int status, long size) {
        return new LogRecord(
                "93.180.71.3",
                "-",
                TIMESTAMP,
                method,
                "/downloads/product_1",
                "HTTP/1.1",
                status,
                size,
                "-",
                "curl/8.7.1");
    }
}
