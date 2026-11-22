package academy.incidentradar.cli;

import static org.assertj.core.api.Assertions.assertThat;

import academy.incidentradar.model.AnalysisOptions;
import academy.incidentradar.model.DateRange;
import academy.incidentradar.report.OutputFormat;
import academy.incidentradar.source.LogSourceException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;

/** Проверки разбора аргументов и запуска обработчика. */
@DisplayName("Команда Incident Radar")
class IncidentRadarCommandTest {

    private final List<AnalysisOptions> receivedOptions = new ArrayList<>();
    private final StringWriter stdout = new StringWriter();
    private final StringWriter stderr = new StringWriter();

    private final CommandLine commandLine = IncidentRadarCommand.createCommandLine(receivedOptions::add)
            .setOut(new PrintWriter(stdout))
            .setErr(new PrintWriter(stderr));

    @Test
    @DisplayName("По умолчанию выбираются Markdown, stdout и отсутствие фильтра")
    void usesDefaultOptions() {
        var exitCode = commandLine.execute("--path", "access.log");

        assertThat(exitCode).isZero();
        assertThat(receivedOptions)
                .containsExactly(
                        new AnalysisOptions("access.log", OutputFormat.MARKDOWN, null, new DateRange(null, null)));
        assertThat(stdout.toString()).isEmpty();
        assertThat(stderr.toString()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"json, JSON", "JsOn, JSON", "adoc, ADOC"})
    @DisplayName("Формат принимается без учёта регистра")
    void acceptsOutputFormatIgnoringCase(String value, OutputFormat expected) {
        var exitCode = commandLine.execute("--path", "access.log", "--output-format", value);

        assertThat(exitCode).isZero();
        assertThat(receivedOptions)
                .containsExactly(new AnalysisOptions("access.log", expected, null, new DateRange(null, null)));
    }

    @Test
    @DisplayName("Короткие параметры и границы периода передаются обработчику")
    void passesExplicitOptionsToAction() {
        var exitCode = commandLine.execute(
                "-p", "logs/archive 2025/*.log",
                "-f", "json",
                "-o", "reports/report.json",
                "--from", "2025-03-01",
                "--to", "2025-03-02");

        assertThat(exitCode).isZero();
        assertThat(receivedOptions)
                .containsExactly(new AnalysisOptions(
                        "logs/archive 2025/*.log",
                        OutputFormat.JSON,
                        Path.of("reports/report.json"),
                        new DateRange(LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 2))));
    }

    @Test
    @DisplayName("Справка работает без --path и не запускает обработчик")
    void helpDoesNotRunAction() {
        var exitCode = commandLine.execute("--help");

        assertThat(exitCode).isZero();
        assertThat(stdout.toString()).contains("--path", "--from", "--to");
        assertThat(stderr.toString()).isEmpty();
        assertThat(receivedOptions).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"2025-02-30", "2025-3-01", "2025-03-01T00:00:00", "2025-03-01garbage"})
    @DisplayName("Некорректная дата отклоняется до запуска обработчика")
    void rejectsInvalidDate(String date) {
        var exitCode = commandLine.execute("--path", "access.log", "--from", date);

        assertThat(exitCode).isEqualTo(2);
        assertThat(stderr.toString()).contains("--from");
        assertThat(stdout.toString()).isEmpty();
        assertThat(receivedOptions).isEmpty();
    }

    @Test
    @DisplayName("Ошибка настроек возвращает код 2 и не запускает обработчик")
    void invalidOptionsDoNotRunAction() {
        var exitCode = commandLine.execute("--path", " ");

        assertThat(exitCode).isEqualTo(2);
        assertThat(stderr.toString()).contains("--path");
        assertThat(receivedOptions).isEmpty();
    }

    @Test
    @DisplayName("Непредвиденная ошибка обработчика возвращает код 1")
    void actionFailureReturnsOne() {
        var failingCommand = IncidentRadarCommand.createCommandLine(options -> {
                    throw new IllegalStateException("Тестовый сбой обработчика");
                })
                .setOut(new PrintWriter(stdout))
                .setErr(new PrintWriter(stderr));

        var exitCode = failingCommand.execute("--path", "access.log");

        assertThat(exitCode).isEqualTo(1);
        assertThat(stderr.toString()).contains("Тестовый сбой обработчика");
        assertThat(stdout.toString()).isEmpty();
    }

    @Test
    @DisplayName("Ошибка источника возвращает код 2 без вывода отчёта")
    void sourceFailureReturnsTwo() {
        var failingCommand = IncidentRadarCommand.createCommandLine(options -> {
                    throw new LogSourceException("Тестовая ошибка источника");
                })
                .setOut(new PrintWriter(stdout))
                .setErr(new PrintWriter(stderr));

        var exitCode = failingCommand.execute("--path", "access.log");

        assertThat(exitCode).isEqualTo(2);
        assertThat(stderr.toString()).contains("Тестовая ошибка источника").doesNotContain("LogSourceException");
        assertThat(stdout.toString()).isEmpty();
    }
}
