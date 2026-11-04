package academy.incidentradar.mandatory.mr1;

import static org.assertj.core.api.Assertions.assertThat;

import academy.incidentradar.Main;
import academy.incidentradar.support.CliRunner;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Обязательные тесты: валидация аргументов командной строки. */
@DisplayName("MR1. Валидация аргументов CLI")
class CliValidationTest {

    @TempDir
    private Path tempDir;

    private Path logPath;

    @BeforeEach
    void createLogFile() throws IOException {
        logPath = tempDir.resolve("access.log");
        Files.writeString(logPath, "строка\n", StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("Не передан обязательный --path -> exit code 2")
    void missingPathExitsWithTwo() {
        var result = executeInProcess();

        assertThat(result.exitCode()).as("%s", result).isEqualTo(2);
        assertThat(result.stderr()).contains("--path");
        assertThat(result.stdout()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"xml", "csv", "txt"})
    @DisplayName("Неподдерживаемый --output-format \"{0}\" -> exit code 2")
    void unsupportedOutputFormatExitsWithTwo(String format) {
        var result = executeInProcess("--path", logPath.toString(), "--output-format", format);

        assertThat(result.exitCode()).as("%s", result).isEqualTo(2);
        assertThat(result.stderr()).contains("--output-format");
        assertThat(result.stdout()).isEmpty();
    }

    @Test
    @DisplayName("--from больше или равен --to -> exit code 2")
    void fromNotBeforeToExitsWithTwo() {
        for (var to : List.of("2025-03-01", "2025-02-28")) {
            var result = executeInProcess("--path", logPath.toString(), "--from", "2025-03-01", "--to", to);

            assertThat(result.exitCode())
                    .as("Конечная дата: %s%n%s", to, result)
                    .isEqualTo(2);
            assertThat(result.stderr()).contains("--from", "--to");
            assertThat(result.stdout()).isEmpty();
        }
    }

    @Test
    @DisplayName("Несуществующий локальный файл -> exit code 2")
    void missingLocalFileExitsWithTwo() {
        var missingPath = tempDir.resolve("missing.log");

        var result = CliRunner.run("--path", missingPath.toString());

        assertThat(result.exitCode()).as("%s", result).isEqualTo(2);
        assertThat(result.stderr()).contains(missingPath.toString());
        assertThat(result.stdout()).isEmpty();
    }

    @Test
    @DisplayName("Расширение файла отчёта не соответствует формату -> exit code 2")
    void outputExtensionMismatchExitsWithTwo() {
        var outputPath = tempDir.resolve("report.md");

        var result = executeInProcess(
                "--path", logPath.toString(),
                "--output-format", "json",
                "--output-path", outputPath.toString());

        assertThat(result.exitCode()).as("%s", result).isEqualTo(2);
        assertThat(result.stderr()).contains("--output-path", ".json");
        assertThat(result.stdout()).isEmpty();
        assertThat(outputPath).doesNotExist();
    }

    private CliRunner.Result executeInProcess(String... args) {
        var stdout = new StringWriter();
        var stderr = new StringWriter();

        var exitCode = Main.createCommandLine()
                .setOut(new PrintWriter(stdout))
                .setErr(new PrintWriter(stderr))
                .execute(args);

        return new CliRunner.Result(exitCode, stdout.toString(), stderr.toString());
    }
}
