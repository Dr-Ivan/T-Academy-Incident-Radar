package academy.incidentradar.cli;

import static org.assertj.core.api.Assertions.assertThat;

import academy.incidentradar.Main;
import academy.incidentradar.report.OutputFormat;
import academy.incidentradar.support.CliRunner;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Подключение отчётов к CLI")
class ReportCliIntegrationTest {

    private static final String SOURCE = "samples/logs/reference-sizes.log";

    @TempDir
    private Path directory;

    @ParameterizedTest
    @CsvSource({"JSON, {", "MARKDOWN, #", "ADOC, ="})
    @DisplayName("Выбранный формат выводится через настроенный stdout команды")
    void rendersToConfiguredStdout(OutputFormat format, String prefix) {
        var result = execute("--path", SOURCE, "--output-format", format.name());

        assertThat(result.exitCode()).as("%s", result).isZero();
        assertThat(result.stdout()).startsWith(prefix).endsWith("\n");
    }

    @ParameterizedTest
    @CsvSource({"JSON, {", "MARKDOWN, #", "ADOC, ="})
    @DisplayName("Файл отчёта содержит выбранный формат, stdout пуст")
    void savesSelectedFormat(OutputFormat format, String prefix) throws IOException {
        var outputPath = directory.resolve("отчёт " + format.extension());
        var result = execute(
                "--path", SOURCE,
                "--output-format", format.name(),
                "--output-path", outputPath.toString());

        assertThat(result.exitCode()).as("%s", result).isZero();
        assertThat(result.stdout()).isEmpty();
        assertThat(Files.readString(outputPath, StandardCharsets.UTF_8)).startsWith(prefix);
    }

    @Test
    @DisplayName("По умолчанию выводится Markdown со статистикой запросов")
    void defaultsToMarkdown() {
        var result = execute("--path", SOURCE);

        assertThat(result.exitCode()).as("%s", result).isZero();
        assertThat(result.stdout()).startsWith("# Отчёт Incident Radar\n").contains("| Количество запросов | 5 |");
    }

    @Test
    @DisplayName("Существующий отчёт сохраняется, команда возвращает код 2")
    void existingOutputReturnsTwo() throws IOException {
        var outputPath = directory.resolve("report.md");
        Files.writeString(outputPath, "Исходный отчёт", StandardCharsets.UTF_8);

        var result = execute("--path", SOURCE, "--output-path", outputPath.toString());

        assertThat(result.exitCode()).as("%s", result).isEqualTo(2);
        assertThat(result.stdout()).isEmpty();
        assertThat(result.stderr())
                .contains("уже существует", outputPath.toString())
                .doesNotContain("ReportOutputException");
        assertThat(Files.readString(outputPath, StandardCharsets.UTF_8)).isEqualTo("Исходный отчёт");
    }

    @Test
    @DisplayName("Отсутствующий родительский каталог приводит к коду 2")
    void missingOutputDirectoryReturnsTwo() {
        var outputPath = directory.resolve("missing/report.json");
        var result = execute("--path", SOURCE, "--output-format", "json", "--output-path", outputPath.toString());

        assertThat(result.exitCode()).as("%s", result).isEqualTo(2);
        assertThat(result.stdout()).isEmpty();
        assertThat(result.stderr()).contains("Не удалось записать", outputPath.toString());
        assertThat(outputPath).doesNotExist();
        assertThat(outputPath.getParent()).doesNotExist();
    }

    @Test
    @DisplayName("Ошибка настроенного stdout приводит к коду 2")
    void stdoutFailureReturnsTwo() {
        var output = new PrintWriter(new StringWriter());
        var errors = new StringWriter();
        output.close();

        var exitCode = Main.createCommandLine()
                .setOut(output)
                .setErr(new PrintWriter(errors))
                .execute("--path", SOURCE);

        assertThat(exitCode).isEqualTo(2);
        assertThat(errors.toString()).contains("stdout").doesNotContain("ReportOutputException");
    }

    @Test
    @DisplayName("Ошибка источника завершает команду до создания отчёта")
    void sourceFailureDoesNotCreateOutput() {
        var source = directory.resolve("missing.log");
        var outputPath = directory.resolve("report.json");
        var result = execute(
                "--path", source.toString(),
                "--output-format", "json",
                "--output-path", outputPath.toString());

        assertThat(result.exitCode()).as("%s", result).isEqualTo(2);
        assertThat(result.stdout()).isEmpty();
        assertThat(result.stderr()).contains(source.toString());
        assertThat(outputPath).doesNotExist();
    }

    private static CliRunner.Result execute(String... args) {
        var output = new StringWriter();
        var errors = new StringWriter();
        var exitCode = Main.createCommandLine()
                .setOut(new PrintWriter(output))
                .setErr(new PrintWriter(errors))
                .execute(args);
        return new CliRunner.Result(exitCode, output.toString(), errors.toString());
    }
}
