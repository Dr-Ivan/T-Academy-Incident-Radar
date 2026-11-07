package academy.incidentradar.mandatory.mr2;

import static org.assertj.core.api.Assertions.assertThat;

import academy.incidentradar.model.LogRecord;
import academy.incidentradar.parser.NginxLogParser;
import academy.incidentradar.support.CliRunner;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Обязательные тесты: разбор строки лога. */
@DisplayName("MR2. Парсер строки лога")
class LogParsingTest {

    private final NginxLogParser parser = new NginxLogParser();

    @Test
    @DisplayName("Эталонная строка разбирается на корректные поля")
    void referenceLineIsParsedCorrectly() {
        var line = "93.180.71.3 - - [17/May/2015:08:05:32 +0000] "
                + "\"GET /downloads/product_1 HTTP/1.1\" 304 0 \"-\" \"Debian APT-HTTP/1.3\"";

        var record = parser.parse(line).orElseThrow();

        assertThat(record)
                .isEqualTo(new LogRecord(
                        "93.180.71.3",
                        "-",
                        OffsetDateTime.parse("2015-05-17T08:05:32Z"),
                        "GET",
                        "/downloads/product_1",
                        "HTTP/1.1",
                        304,
                        0,
                        "-",
                        "Debian APT-HTTP/1.3"));
    }

    @Test
    @DisplayName("Метод, ресурс и протокол разбираются в три отдельных поля")
    void requestIsSplitIntoThreeFields() {
        var line = "93.180.71.3 - - [01/Mar/2025:12:00:00 +0000] "
                + "\"POST /api/v1/payments?currency=RUB HTTP/2.0\" 201 512 \"-\" \"curl/8.7.1\"";

        var record = parser.parse(line).orElseThrow();

        assertThat(record.method()).isEqualTo("POST");
        assertThat(record.resource()).isEqualTo("/api/v1/payments?currency=RUB");
        assertThat(record.protocol()).isEqualTo("HTTP/2.0");
    }

    @Test
    @DisplayName("Битая строка пропускается с warning'ом, программа не падает")
    void malformedLineIsSkippedWithWarning() {
        var result = CliRunner.run("--path", "samples/logs/malformed.log");
        var warnings = result.stderr()
                .lines()
                .filter(line -> line.contains("WARN") && line.contains("skipped malformed log line"))
                .count();

        assertThat(result.exitCode()).as("%s", result).isZero();
        assertThat(warnings).isEqualTo(6);
        assertThat(result.stderr()).contains("Некорректных строк: 6, запросов в основном периоде: 7");
    }
}
