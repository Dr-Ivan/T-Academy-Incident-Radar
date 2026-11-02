package academy.incidentradar.model;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import academy.incidentradar.report.OutputFormat;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Проверки согласованности настроек запуска. */
@DisplayName("Настройки запуска анализа")
class AnalysisOptionsTest {

    private static final String SOURCE = "access.log";
    private static final DateRange ALL_DATES = new DateRange(null, null);

    @Test
    @DisplayName("Путь вывода может отсутствовать")
    void acceptsStdoutDestination() {
        assertThatCode(() -> new AnalysisOptions(SOURCE, OutputFormat.MARKDOWN, null, ALL_DATES))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({"JSON, report.json", "MARKDOWN, report.md", "ADOC, report.adoc"})
    @DisplayName("Допускается выходной файл с соответствующим расширением")
    void acceptsMatchingOutputExtension(OutputFormat format, String fileName) {
        var outputPath = Path.of(fileName);

        assertThatCode(() -> new AnalysisOptions(SOURCE, format, outputPath, ALL_DATES))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Расширение выходного файла должно соответствовать формату")
    void rejectsMismatchedOutputExtension() {
        var outputPath = Path.of("report.md");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new AnalysisOptions(SOURCE, OutputFormat.JSON, outputPath, ALL_DATES))
                .withMessageContaining("--output-path")
                .withMessageContaining(".json");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    @DisplayName("Источник не может быть пустым или состоять из пробелов")
    void rejectsBlankSource(String path) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new AnalysisOptions(path, OutputFormat.MARKDOWN, null, ALL_DATES))
                .withMessageContaining("--path");
    }
}
