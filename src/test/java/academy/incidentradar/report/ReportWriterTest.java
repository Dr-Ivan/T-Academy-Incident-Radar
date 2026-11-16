package academy.incidentradar.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import academy.incidentradar.model.DateRange;
import academy.incidentradar.statistics.StatisticsAccumulator;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Запись отчёта")
class ReportWriterTest {

    @TempDir
    private Path directory;

    private final ReportWriter writer = new ReportWriter();
    private final StringWriter buffer = new StringWriter();
    private final PrintWriter stdout = new PrintWriter(buffer);

    @ParameterizedTest
    @CsvSource({"JSON, {", "MARKDOWN, #", "ADOC, ="})
    @DisplayName("Выбранный формат записывается в UTF-8-файл")
    void writesSelectedFormat(OutputFormat format, String prefix) throws IOException {
        var data = new ReportData(
                List.of("логи/access.log"),
                new DateRange(null, null),
                new StatisticsAccumulator().snapshot(),
                null,
                null);
        var report = format.createRenderer().render(data);
        var outputPath = directory.resolve("report" + format.extension());

        writer.write(report, outputPath, stdout);

        assertThat(Files.readString(outputPath, StandardCharsets.UTF_8))
                .startsWith(prefix)
                .isEqualTo(report);
        assertThat(buffer.toString()).isEmpty();
    }

    @Test
    @DisplayName("Вывод в stdout сохраняет текст и оставляет поток доступным вызывающему коду")
    void writesToCallerOwnedStdout() {
        writer.write("Отчёт\n", null, stdout);

        assertThat(buffer.toString()).isEqualTo("Отчёт\n");
        stdout.print("Следующая запись");
        stdout.flush();
        assertThat(buffer.toString()).isEqualTo("Отчёт\nСледующая запись");
    }

    @Test
    @DisplayName("Существующий файл сохраняет исходное содержимое")
    void preservesExistingFile() throws IOException {
        var outputPath = directory.resolve("report.md");
        Files.writeString(outputPath, "Исходный отчёт", StandardCharsets.UTF_8);

        assertThatThrownBy(() -> writer.write("Новый отчёт", outputPath, stdout))
                .isInstanceOf(ReportOutputException.class)
                .hasMessageContaining("уже существует");

        assertThat(Files.readString(outputPath, StandardCharsets.UTF_8)).isEqualTo("Исходный отчёт");
        assertThat(buffer.toString()).isEmpty();
    }

    @Test
    @DisplayName("Каталог по пути отчёта сохраняется вместе с содержимым")
    void preservesExistingDirectory() throws IOException {
        var outputPath = Files.createDirectory(directory.resolve("report.md"));
        var marker = outputPath.resolve("keep.txt");
        Files.writeString(marker, "Сохранить", StandardCharsets.UTF_8);

        assertThatThrownBy(() -> writer.write("Отчёт", outputPath, stdout)).isInstanceOf(ReportOutputException.class);

        assertThat(outputPath).isDirectory();
        assertThat(Files.readString(marker, StandardCharsets.UTF_8)).isEqualTo("Сохранить");
    }

    @Test
    @DisplayName("Отсутствующий родительский каталог приводит к ошибке")
    void rejectsMissingParent() {
        var parent = directory.resolve("missing");
        var outputPath = parent.resolve("report.md");

        assertThatThrownBy(() -> writer.write("Отчёт", outputPath, stdout))
                .isInstanceOf(ReportOutputException.class)
                .hasMessageContaining("Не удалось записать");

        assertThat(parent).doesNotExist();
        assertThat(outputPath).doesNotExist();
    }

    @Test
    @DisplayName("Ошибка stdout обнаруживается и передаётся вызывающему коду")
    void detectsStdoutFailure() {
        stdout.close();

        assertThatThrownBy(() -> writer.write("Отчёт", null, stdout))
                .isInstanceOf(ReportOutputException.class)
                .hasMessageContaining("stdout");
    }

    @Test
    @DisplayName("Ошибка кодирования обнаруживается до создания файла")
    void rejectsMalformedTextBeforeCreatingFile() {
        var outputPath = directory.resolve("report.md");

        assertThatThrownBy(() -> writer.write("\uD800", outputPath, stdout))
                .isInstanceOf(ReportOutputException.class)
                .hasCauseInstanceOf(CharacterCodingException.class);

        assertThat(outputPath).doesNotExist();
    }
}
