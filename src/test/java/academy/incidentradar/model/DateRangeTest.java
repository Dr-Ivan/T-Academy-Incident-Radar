package academy.incidentradar.model;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Проверки допустимых границ периода. */
@DisplayName("Границы периода")
class DateRangeTest {

    private static final LocalDate START = LocalDate.of(2025, 3, 1);

    @ParameterizedTest
    @MethodSource("validRanges")
    @DisplayName("Допускаются открытые границы и возрастающий период")
    void acceptsValidBounds(LocalDate from, LocalDate to) {
        assertThatCode(() -> new DateRange(from, to)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"2025-03-01", "2025-02-28"})
    @DisplayName("Конечная дата должна быть строго позже начальной")
    void rejectsNonIncreasingBounds(String to) {
        var end = LocalDate.parse(to);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DateRange(START, end))
                .withMessageContaining("--from")
                .withMessageContaining("--to");
    }

    private static Stream<Arguments> validRanges() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of(START, null),
                Arguments.of(null, START),
                Arguments.of(START, START.plusDays(1)));
    }
}
