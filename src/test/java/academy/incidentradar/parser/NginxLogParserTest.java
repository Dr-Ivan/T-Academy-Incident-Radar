package academy.incidentradar.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Дополнительные проверки разбора NGINX-логов. */
@DisplayName("Парсер NGINX-логов")
class NginxLogParserTest {

    private static final String REFERENCE_LINE = "93.180.71.3 - - [17/May/2015:08:05:32 +0000] "
            + "\"GET /downloads/product_1 HTTP/1.1\" 304 0 \"-\" \"Debian APT-HTTP/1.3\"";

    private final NginxLogParser parser = new NginxLogParser();

    @ParameterizedTest
    @MethodSource("malformedLines")
    @DisplayName("Некорректная строка даёт пустой результат")
    void rejectsMalformedLine(String line) {
        assertThat(parser.parse(line)).isEmpty();
    }

    @Test
    @DisplayName("Сохраняются смещение, большой размер ответа и пустые поля в кавычках")
    void preservesOffsetAndExtendedValues() {
        var line = "2001:db8::1 - ivanov [01/Mar/2025:00:30:00 +0300] "
                + "\"GET /api/v1/accounts?limit=10 HTTP/2.0\" 200 2147483648 \"\" \"\"";

        var record = parser.parse(line).orElseThrow();

        assertThat(record.remoteAddress()).isEqualTo("2001:db8::1");
        assertThat(record.remoteUser()).isEqualTo("ivanov");
        assertThat(record.timestamp()).isEqualTo(OffsetDateTime.parse("2025-03-01T00:30:00+03:00"));
        assertThat(record.resource()).isEqualTo("/api/v1/accounts?limit=10");
        assertThat(record.bodyBytesSent()).isEqualTo(2_147_483_648L);
        assertThat(record.referer()).isEmpty();
        assertThat(record.userAgent()).isEmpty();
    }

    @Test
    @DisplayName("В смешанном сэмпле разбираются семь корректных записей")
    void parsesValidRecordsFromMixedSample() throws IOException {
        var path = Path.of("samples", "logs", "malformed.log");

        try (var lines = Files.lines(path, StandardCharsets.UTF_8)) {
            var parsedCount =
                    lines.map(parser::parse).filter(Optional::isPresent).count();
            assertThat(parsedCount).isEqualTo(7);
        }
    }

    private static Stream<String> malformedLines() {
        return Stream.of(
                "",
                "это вообще не лог",
                REFERENCE_LINE.replace("17/May/2015", "31/Feb/2015"),
                REFERENCE_LINE.replace("304 0", "099 0"),
                REFERENCE_LINE.replace("304 0", "600 0"),
                REFERENCE_LINE.replace("304 0", "304 -1"),
                REFERENCE_LINE.replace("304 0", "304 9223372036854775808"),
                REFERENCE_LINE.replace("\"GET /downloads/product_1 HTTP/1.1\"", "\"\""),
                REFERENCE_LINE + " trailing");
    }
}
