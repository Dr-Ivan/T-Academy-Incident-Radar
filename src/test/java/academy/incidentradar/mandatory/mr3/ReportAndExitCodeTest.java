package academy.incidentradar.mandatory.mr3;

import static org.assertj.core.api.Assertions.assertThat;

import academy.incidentradar.support.CliRunner;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Обязательные тесты: отчёты и коды возврата — blackbox-проверки CLI. */
@DisplayName("MR3. Отчёты и коды возврата")
class ReportAndExitCodeTest {

    private static final String SOURCE = "samples/logs/reference-sizes.log";

    @TempDir
    private Path directory;

    @Test
    @DisplayName("JSON-отчёт валиден и содержит все обязательные поля")
    void jsonReportIsValidAndComplete() throws IOException {
        var outputPath = directory.resolve("report.json");
        var result = CliRunner.run("--path", SOURCE, "--output-format", "json", "--output-path", outputPath.toString());

        assertThat(result.exitCode()).as("%s", result).isZero();
        assertThat(result.stdout()).isEmpty();
        assertThat(outputPath).isRegularFile();

        var mapper = JsonMapper.builder()
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .build();
        var json = mapper.readTree(Files.readString(outputPath, StandardCharsets.UTF_8));

        assertThat(json.isObject()).isTrue();
        assertThat(json.path("files").isArray()).isTrue();
        assertThat(json.path("files").size()).isEqualTo(1);
        var reportedFile = Path.of(json.path("files").path(0).asText());
        assertThat(reportedFile.toAbsolutePath().normalize())
                .isEqualTo(Path.of(SOURCE).toAbsolutePath().normalize());
        assertThat(json.path("totalRequestsCount").isIntegralNumber()).isTrue();
        assertThat(json.path("totalRequestsCount").longValue()).isEqualTo(5);

        var sizes = json.path("responseSizeInBytes");
        assertThat(sizes.isObject()).isTrue();
        assertThat(sizes.path("average").isNumber()).isTrue();
        assertThat(sizes.path("average").decimalValue()).isEqualByComparingTo("300.00");
        assertThat(sizes.path("max").isIntegralNumber()).isTrue();
        assertThat(sizes.path("max").longValue()).isEqualTo(500);
        assertThat(sizes.path("p95").isIntegralNumber()).isTrue();
        assertThat(sizes.path("p95").longValue()).isEqualTo(500);

        assertThat(json.path("resources")).isEqualTo(mapper.readTree("""
                        [{"resource": "/api/v1/cards", "totalRequestsCount": 5}]
                        """));
        assertThat(json.path("responseCodes")).isEqualTo(mapper.readTree("{\"200\": 5}"));
    }

    @Test
    @DisplayName("Markdown-отчёт содержит заголовок и таблицы")
    void markdownReportHasHeaderAndTables() throws IOException {
        var outputPath = directory.resolve("report.md");
        var result =
                CliRunner.run("--path", SOURCE, "--output-format", "markdown", "--output-path", outputPath.toString());

        assertThat(result.exitCode()).as("%s", result).isZero();
        assertThat(result.stdout()).isEmpty();

        var markdown = Files.readString(outputPath, StandardCharsets.UTF_8);
        assertThat(markdown)
                .startsWith("# Отчёт Incident Radar\n\n")
                .contains(
                        "## Источники\n\n| Источник |\n| --- |\n",
                        "reference-sizes.log",
                        "### Общая статистика\n\n| Метрика | Значение |\n| --- | --- |\n",
                        "| Количество запросов | 5 |",
                        "| Средний размер ответа, байт | 300.00 |",
                        "| Максимальный размер ответа, байт | 500 |",
                        "| p95, байт | 500 |",
                        "### Топ-10 ресурсов\n\n| Ресурс | Запросов |\n| --- | --- |\n",
                        "| /api/v1/cards | 5 |",
                        "### Коды ответа\n\n| Код | Запросов |\n| --- | --- |\n",
                        "| 200");
    }

    @Test
    @DisplayName("Существующий файл отчёта не перезаписывается -> ошибка")
    void existingOutputFileIsNotOverwritten() throws IOException {
        var outputPath = directory.resolve("report.json");
        var original = "Исходное содержимое отчёта\n".getBytes(StandardCharsets.UTF_8);
        Files.write(outputPath, original);

        var result = CliRunner.run("--path", SOURCE, "--output-format", "json", "--output-path", outputPath.toString());

        assertThat(result.exitCode()).as("%s", result).isEqualTo(2);
        assertThat(result.stdout()).isEmpty();
        assertThat(result.stderr()).contains("уже существует", outputPath.toString());
        assertThat(Files.readAllBytes(outputPath)).containsExactly(original);
    }

    @Test
    @DisplayName("Успешный запуск -> exit code 0")
    void successfulRunExitsWithZero() {
        var result = CliRunner.run("--path", SOURCE);

        assertThat(result.exitCode()).as("%s", result).isZero();
        assertThat(result.stdout()).startsWith("# Отчёт Incident Radar\n").contains("| Количество запросов | 5 |");
    }

    @Test
    @DisplayName("Некорректное использование -> exit code 2, а не 1")
    void misuseExitsWithTwoNotOne() {
        var result = CliRunner.run();

        assertThat(result.exitCode()).as("%s", result).isEqualTo(2);
        assertThat(result.stdout()).isEmpty();
        assertThat(result.stderr()).contains("--path");
    }
}
