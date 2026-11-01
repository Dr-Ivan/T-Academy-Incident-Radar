package academy.incidentradar.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Проверки соответствия формата отчёта и расширения файла. */
@DisplayName("Форматы отчётов")
class OutputFormatTest {

    @ParameterizedTest
    @CsvSource({
        "JSON, report.json, true",
        "JSON, report.JSON, true",
        "MARKDOWN, report.md, true",
        "ADOC, report.adoc, true",
        "JSON, report.md, false",
        "MARKDOWN, report.json, false",
        "ADOC, report.md, false",
        "JSON, report.json.bak, false"
    })
    @DisplayName("Расширение файла соответствует выбранному формату")
    void outputExtensionMatchesSelectedFormat(OutputFormat format, String fileName, boolean expected) {
        assertThat(format.matchesExtension(Path.of(fileName))).isEqualTo(expected);
    }

    @Test
    @DisplayName("Расширение каталога не определяет формат файла")
    void directoryExtensionDoesNotDetermineFileFormat() {
        var path = Path.of("archive.json", "report.md");

        assertThat(OutputFormat.JSON.matchesExtension(path)).isFalse();
        assertThat(OutputFormat.MARKDOWN.matchesExtension(path)).isTrue();
    }

    @Test
    @DisplayName("Корень файловой системы не является файлом отчёта")
    void fileSystemRootIsNotReportFile() {
        var root = Path.of("").toAbsolutePath().getRoot();

        assertThat(root).isNotNull();
        assertThat(OutputFormat.JSON.matchesExtension(root)).isFalse();
    }
}
